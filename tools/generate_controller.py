"""Generate the original MotionRP handheld remote model and its palette texture."""
import json
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/controller/assets/motionrp'
COLORS=['#172435','#293c50','#40576a','#101a28','#57ebf4','#b1fcff','#f6c46b','#ad783c','#ec627c','#e5edf3','#33878f','#6d8695']
image=Image.new('RGBA',(16,16))
for i,c in enumerate(COLORS):
 for x in range(i%4*4,i%4*4+4):
  for y in range(i//4*4,i//4*4+4):image.putpixel((x,y),tuple(bytes.fromhex(c[1:]))+(255,))
(OUT/'textures/item').mkdir(parents=True,exist_ok=True);image.save(OUT/'textures/item/controller.png')
elements=[]
def cube(name,a,b,color):
 x=color%4*4;y=color//4*4
 elements.append({'name':name,'from':a,'to':b,'faces':{f:{'uv':[x+.5,y+.5,x+3.5,y+3.5],'texture':'#0'}for f in ['north','south','east','west','up','down']}})
cube('shell',[5,3,7],[11,13.5,9],0)
cube('back inset',[5.5,3.4,9],[10.5,13,9.25],1)
cube('face',[5.35,3.5,6.65],[10.65,13,7],1)
cube('left bevel',[4.8,3.5,7.2],[5.25,13,8.7],2)
cube('right bevel',[10.75,3.5,7.2],[11.2,13,8.7],2)
cube('top gold trim',[5.3,13,6.55],[10.7,13.45,7.1],6)
cube('screen cyan frame',[5.8,8.4,6.3],[10.2,12.3,6.65],10)
cube('screen glass',[6.05,8.65,6.05],[9.95,12.05,6.3],3)
for row,width in enumerate([.5,1.1,1.7,1.1,.5]):
 y=9.15+row*.45
 cube('play glyph '+str(row),[7,y,5.95],[7+width,y+.43,6.05],4)
cube('status led',[9.1,11.55,5.94],[9.55,11.85,6.05],5)
cube('dpad vertical',[6.45,4.7,6.12],[7.15,7.35,6.65],6)
cube('dpad horizontal',[5.48,5.65,6.12],[8.1,6.4,6.65],6)
cube('dpad center',[6.5,5.7,5.96],[7.1,6.3,6.12],7)
cube('start button',[8.85,6.35,6.1],[9.85,7.35,6.65],4)
cube('stop button',[8.85,4.85,6.1],[9.85,5.85,6.65],8)
cube('bottom trim',[5.3,3.2,6.6],[10.7,3.6,7],7)
cube('antenna base',[9,13.5,7.35],[10.2,14.25,8.65],2)
cube('antenna',[9.35,14.25,7.7],[9.85,15.65,8.3],0)
cube('antenna light',[9.25,15.45,7.6],[9.95,16,8.4],4)
model={'credit':'Original MotionRP controller','gui_light':'front','textures':{'0':'motionrp:item/controller','particle':'motionrp:item/controller'},'elements':elements,'display':{
'gui':{'rotation':[20,210,0],'translation':[0,-.5,0],'scale':[1.05,1.05,1.05]},
'firstperson_righthand':{'rotation':[0,165,0],'translation':[1,1,-1],'scale':[.7,.7,.7]},
'firstperson_lefthand':{'rotation':[0,-165,0],'translation':[1,1,-1],'scale':[.7,.7,.7]},
 'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,1,0],'scale':[.7,.7,.7]},
 'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,1,0],'scale':[.7,.7,.7]},
 'ground':{'translation':[0,2,0],'scale':[.6,.6,.6]},'fixed':{'rotation':[0,180,0]}}}
for name,data in [('models/item/controller.json',model),('items/controller.json',{'model':{'type':'minecraft:model','model':'motionrp:item/controller'}})]:
 p=OUT/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
# Render the actual cuboids as a reviewable preview; no hand-drawn placeholder.
if __name__=='__main__':
 import numpy as np
 w,h=540,660
 eye=np.array([20.,-35.,21.]);target=np.array([8.,7.5,9.5]);forward=target-eye;forward/=np.linalg.norm(forward)
 right=np.cross(forward,[0,0,1]);right/=np.linalg.norm(right);up=np.cross(right,forward)
 xx,yy=np.meshgrid((np.arange(w)-w/2)/32.,(h/2-np.arange(h))/32.)
 origins=eye+xx[...,None]*right+yy[...,None]*up
 depth=np.full((h,w),np.inf);pixels=np.zeros((h,w,3),dtype=np.uint8);pixels[:]=[14,24,37]
 for element in elements:
  lo=np.array(element['from'])[[0,2,1]];hi=np.array(element['to'])[[0,2,1]]
  ta=(lo-origins)/forward;tb=(hi-origins)/forward
  near=np.minimum(ta,tb);far=np.maximum(ta,tb);t=near.max(axis=2)
  valid=(t<=far.min(axis=2))&(t>0)&(t<depth)
  axis=near.argmax(axis=2);shade=np.choose(axis,[.82,1.,.93])
  uv=element['faces']['north']['uv'];idx=int(uv[1]//4)*4+int(uv[0]//4)
  color=np.array(list(bytes.fromhex(COLORS[idx][1:])))
  pixels[valid]=(shade[...,None]*color)[valid].astype(np.uint8);depth[valid]=t[valid]
 p=ROOT/'docs/previews/controller-v082.png';p.parent.mkdir(parents=True,exist_ok=True);Image.fromarray(pixels).save(p)
