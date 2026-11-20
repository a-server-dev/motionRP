"""Perspective preview of the actual editable cube geometry (not a Minecraft screenshot)."""
import json,math,base64,io
from pathlib import Path
from PIL import Image,ImageDraw
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
m=json.load(open(ROOT/'src/main/resources/bettermodel/models/motionrp_castle_chamber.bbmodel'))
palette=Image.open(io.BytesIO(base64.b64decode(m['textures'][0]['source'].split(',')[1]))).convert('RGB')
w,h=1100,750;eye=np.array([0,320,0]);yaw=.32;pitch=-.20
forward=np.array([math.sin(yaw)*math.cos(pitch),math.sin(pitch),math.cos(yaw)*math.cos(pitch)])
right=np.cross(np.array([0,1,0]),forward);right/=np.linalg.norm(right);up=np.cross(forward,right)
faces=[];corners=np.array([[x,y,z]for x in [-1,1]for y in [-1,1]for z in [-1,1]])
indices=[[0,1,3,2],[4,6,7,5],[0,4,5,1],[2,3,7,6],[0,2,6,4],[1,5,7,3]]
for e in m['elements']:
 lo=np.array(e['from']);hi=np.array(e['to']);pts=(lo+hi)/2+corners*(hi-lo)/2;origin=np.array(e['origin']);a=math.radians(e.get('rotation',[0,0,0])[1]);rot=np.array([[math.cos(a),0,math.sin(a)],[0,1,0],[-math.sin(a),0,math.cos(a)]])
 pts=(pts-origin)@rot.T+origin-eye;xyz=np.stack([pts@right,pts@up,pts@forward],axis=1)
 color=palette.getpixel((min(7,int(next(iter(e['faces'].values()))['uv'][0])),0))
 for i,ids in enumerate(indices):
  p=xyz[ids]
  # Clip faces crossing the eye to retain room enclosure near the camera.
  out=[]
  for j in range(4):
   a,b=p[j],p[(j+1)%4];ain,bin=a[2]>3,b[2]>3
   if ain:out.append(a)
   if ain!=bin:out.append(a+(b-a)*((3-a[2])/(b[2]-a[2])))
  if len(out)<3:continue
  p=np.array(out);q=np.stack([w/2+p[:,0]/p[:,2]*650,h/2-p[:,1]/p[:,2]*650],axis=1)
  if q[:,0].max()<0 or q[:,0].min()>w or q[:,1].max()<0 or q[:,1].min()>h:continue
  shade=[.76,.85,.6,1,.68,.9][i];fog=min(.48,float(p[:,2].mean())/1800);c=tuple(int(v*shade*(1-fog)+9*fog)for v in color)
  faces.append((float(p[:,2].mean()),[tuple(x)for x in q],c))
im=Image.new('RGB',(w,h),(9,6,10));d=ImageDraw.Draw(im)
for _,points,c in sorted(faces,reverse=True,key=lambda f:f[0]):d.polygon(points,fill=c)
d.rectangle((0,h-34,w,h),fill=(12,9,15));d.text((15,h-24),'Infinity Castle | actual model geometry preview | not an in-game screenshot',fill=(240,214,173))
p=ROOT/'docs/images/infinity-castle-037.png';im.save(p);print(p)
