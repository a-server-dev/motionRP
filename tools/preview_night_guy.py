import sys,json,math,numpy as np
from pathlib import Path
from PIL import Image,ImageDraw
sys.path.insert(0,str(Path(__file__).resolve().parent));from preview_getsuga import rig,CORNERS,FACES
ROOT=Path(__file__).resolve().parents[1]
m=json.loads((ROOT/'src/main/resources/bettermodel/players/motionrp_night_guy.bbmodel').read_text())
right=np.array([0,0,-1]);depth=np.array([1,.04,0]);up=np.cross(depth,right);up/=np.linalg.norm(up)
def frame(tick):
 w,h=260,520;scale=5.1;im=Image.new('RGB',(w,h),(218,225,233));d=ImageDraw.Draw(im);d.line((0,490,w,490),fill=(120,130,140));d.text((10,12),f'tick {tick}',fill=(30,35,40));leaves,groups,tf=rig(m,tick/20.);faces=[]
 t=max(0,min(1,(tick-116)/32));height=12.8*t*(1-t)*16
 def project(p):return (w/2+p@right*scale,490-(p@up+height)*scale)
 for e in m['elements']:
  if e['name']!='skin':continue
  parent=leaves[e['uuid']];name=groups[parent]['name'];color=(172,153,120)
  if 'right_leg'in name or 'right_foreleg'in name:color=(200,70,65)
  elif 'left_leg'in name or 'left_foreleg'in name:color=(80,110,170)
  lo=np.array(e['from']);hi=np.array(e['to']);ps=tf((lo+hi)/2+CORNERS*(hi-lo)/2,parent)
  for j,f in enumerate(FACES):faces.append((np.mean(ps[f]@depth),[project(p)for p in ps[f]],tuple(int(c*(.75+j*.035))for c in color)))
 for _,q,c in sorted(faces,key=lambda x:x[0],reverse=True):d.polygon(q,fill=c,outline=(80,75,70))
 return im
if __name__=='__main__':
 frames=[]
 for tick in range(110,156):
  canvas=Image.new('RGB',(520,520))
  for column in range(2):
   right=np.array([0,0,-1])if column==0 else np.array([.75,0,-.66]);depth=np.array([1,.04,0])if column==0 else np.array([.66,.08,.75]);up=np.cross(depth,right);up/=np.linalg.norm(up)
   im=frame(tick);draw=ImageDraw.Draw(im);draw.text((10,28),'Pose preview / right leg red',fill=(30,35,40));draw.text((10,44),'Camera follows runner',fill=(30,35,40));canvas.paste(im,(column*260,0))
  frames.append(canvas)
 target=ROOT/'docs/previews/night-guy-v093.gif';target.parent.mkdir(parents=True,exist_ok=True)
 frames[0].save(target,save_all=True,append_images=frames[1:]+[frames[-1]]*10,duration=50,loop=0,optimize=True)
 print(target)
