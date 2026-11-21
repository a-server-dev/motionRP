#!/usr/bin/env python3
"""Generate the actual BetterModel resource pack in an isolated loopback-only Paper server."""
import argparse,hashlib,os,shutil,subprocess,time,urllib.request,zipfile,sys,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ARTIFACTS={
 'paper.jar':('https://fill-data.papermc.io/v1/objects/f9df8e19f4bdee97dbded48095391ea69c64342a4e19624db4f5e0d0136dfe0b/paper-26.3-253.jar','f9df8e19f4bdee97dbded48095391ea69c64342a4e19624db4f5e0d0136dfe0b'),
 'BetterModel.jar':('https://github.com/toxicity188/BetterModel/releases/download/3.6.0/bettermodel-3.6.0-paper.jar','66b2acc679ffcb81d9d7de686092d3e1daa38dc154da4cdaa9aaabc048a06aba')}
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--jar',required=True,type=Path);ap.add_argument('--output',type=Path,default=ROOT/'build/studio-artifacts');ap.add_argument('--runtime',type=Path,default=ROOT/'build/studio-paper');ap.add_argument('--fixture',type=Path);args=ap.parse_args();args.jar=args.jar.resolve();work=args.runtime.resolve();out=args.output.resolve();work.mkdir(parents=True,exist_ok=True);out.mkdir(parents=True,exist_ok=True);(work/'plugins').mkdir(exist_ok=True)
 for name,(url,sha)in ARTIFACTS.items():
  target=work/('plugins/'+name if name=='BetterModel.jar'else name)
  if args.fixture:
   source=args.fixture/('plugins/BetterModel.jar'if name=='BetterModel.jar'else'paper-26.3-253.jar')
   if not source.exists()and name=='BetterModel.jar':source=args.fixture/'plugins/bettermodel-3.6.0-paper.jar'
   shutil.copyfile(source,target)
  elif not target.exists():
   print('Downloading',name,flush=True);req=urllib.request.Request(url,headers={'User-Agent':'MotionRP-Studio/1.0 (a-server-dev/motionRP)'})
   with urllib.request.urlopen(req,timeout=180)as r,target.open('wb')as f:shutil.copyfileobj(r,f)
  if hashlib.sha256(target.read_bytes()).hexdigest()!=sha:raise ValueError('Runtime checksum mismatch: '+name)
 if args.fixture:
  for name in ['libraries','cache','versions']:
   if not(work/name).exists():shutil.copytree(args.fixture/name,work/name)
  if (args.fixture/'plugins/BetterModel').exists():shutil.copytree(args.fixture/'plugins/BetterModel',work/'plugins/BetterModel',dirs_exist_ok=True)
 shutil.copyfile(args.jar,work/'plugins/MotionRP.jar');(work/'eula.txt').write_text('eula=true\n');(work/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25587\nonline-mode=false\nlevel-type=minecraft:flat\nview-distance=2\nsimulation-distance=2\nmax-players=1\nspawn-protection=0\nresource-pack=\n')
 # Never publish to GitHub or offer players a pack from this build sandbox.
 (work/'plugins/MotionRP').mkdir(exist_ok=True);(work/'plugins/MotionRP/config.yml').write_text('resource-pack:\n  enabled: false\n')
 log=out/'pack-build.log';process=None
 try:
  with log.open('w')as stream:
   java=str(Path(os.environ['JAVA_HOME'])/'bin/java')if'JAVA_HOME'in os.environ else'java'
   process=subprocess.Popen([java,'-XX:ActiveProcessorCount=2','-Xms512M','-Xmx2G','-jar','paper.jar','--nogui'],cwd=work,stdin=subprocess.PIPE,stdout=stream,stderr=subprocess.STDOUT,text=True,env={**os.environ,'MOTIONRP_GITHUB_TOKEN':''})
   deadline=time.monotonic()+300;ready=False;pack=work/'plugins/BetterModel/build.zip'
   while time.monotonic()<deadline:
    if process.poll()is not None:raise RuntimeError('Paper stopped; see pack-build.log')
    text=log.read_text(errors='replace')
    if 'Bundled Rasengan models loaded' in text or ('Updated bundled model'not in text and 'MotionRP enabled:'in text and 'Done ('in text and pack.exists()and time.time()-pack.stat().st_mtime>15 and time.monotonic()>deadline-230):
     if pack.exists()and time.time()-pack.stat().st_mtime>4:ready=True;break
    time.sleep(1)
   if not ready:raise TimeoutError('BetterModel pack generation timed out; see pack-build.log')
   process.stdin.write('stop\n');process.stdin.flush();process.wait(timeout=45)
   if process.returncode!=0:raise RuntimeError('Paper shutdown failed')
  with zipfile.ZipFile(pack)as z:
   if 'pack.mcmeta'not in z.namelist():raise ValueError('Invalid resource pack')
  with zipfile.ZipFile(args.jar)as z:index=json.loads(z.read('studio/index.json'))
  with zipfile.ZipFile(pack)as z:
   names='\n'.join(z.namelist())
   for p in index['projects']:
    for m in p['models']:
     if m['asset']not in names:raise ValueError('Compiled model missing from pack: '+m['asset'])
  shutil.copyfile(pack,out/'MotionRP-resource-pack.zip');shutil.copyfile(args.jar,out/'MotionRP.jar');(out/'SHA256SUMS').write_text('\n'.join(hashlib.sha256((out/f).read_bytes()).hexdigest()+'  '+f for f in ['MotionRP.jar','MotionRP-resource-pack.zip'])+'\n')
  (out/'INSTALL.txt').write_text('Paper 26.3 / Java 25 / BetterModel 3.6.0\nInstall MotionRP.jar and BetterModel on Paper; restart.\nApply MotionRP-resource-pack.zip to each client.\nCustom projects: /motionrp give motion studio_<project-id>\nReview projects before merging. No arbitrary scripts are executed by project.json.\n')
  print('Studio JAR and resource pack:',out)
 finally:
  if process and process.poll()is None:
   try:process.stdin.write('stop\n');process.stdin.flush();process.wait(timeout=30)
   except Exception:process.kill();process.wait()
if __name__=='__main__':
 try:main()
 except Exception as e:print('Pack build failed:',str(e),file=sys.stderr);sys.exit(1)
