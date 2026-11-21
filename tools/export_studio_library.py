"""Publish only validated creator models and manifests, never arbitrary repository files."""
import json,shutil
from pathlib import Path
from compile_studio_projects import ROOT,compile_project
out=ROOT/'editor/public/community';shutil.rmtree(out,ignore_errors=True);out.mkdir(parents=True);items=[]
for f in sorted((ROOT/'projects').glob('*/project.json')):
 compiled,_=compile_project(f.parent)
 if not compiled.get('enabled',True):continue
 p=json.loads(f.read_text());target=out/p['id'];(target/'models').mkdir(parents=True);(target/'project.json').write_text(json.dumps(p,ensure_ascii=False))
 for m in p['models']:shutil.copyfile(f.parent/m['file'],target/m['file'])
 items.append({'id':p['id'],'name':p['name'],'duration':p['duration'],'models':len(p['models']),'events':len(p['events'])})
(ROOT/'editor/public/library.json').write_text(json.dumps(items,ensure_ascii=False))
print('Published library projects:',len(items))
