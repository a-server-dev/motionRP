"""Render actual animated rig/weapon geometry for attachment and pose review."""
import sys,json,math
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw
sys.path.insert(0,str(Path(__file__).resolve().parent))
from hollow_kinematics import matrix
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'src/main/resources/bettermodel'
actor=json.loads((ASSETS/'players/motionrp_getsuga_tensho.bbmodel').read_text());weapon=json.loads((ASSETS/'models/motionrp_getsuga_tensa.bbmodel').read_text());mask=json.loads((ASSETS/'models/motionrp_hollow_mask.bbmodel').read_text())
blade=np.loadtxt(ASSETS/'getsuga_tensho-blade-path.csv',delimiter=',');head=np.loadtxt(ASSETS/'getsuga_tensho-head-path.csv',delimiter=',')
CORNERS=np.array([[x,y,z]for x in [-1,1]for y in [-1,1]for z in [-1,1]])
FACES=[[0,1,3,2],[4,6,7,5],[0,4,5,1],[2,3,7,6],[0,2,6,4],[1,5,7,3]]
def rig(model,t):
 groups={g['uuid']:g for g in model['groups']};parents={};leaves={};tracks=model['animations'][0]['animators']
 def walk(nodes,parent=None):
  for node in nodes:
   if isinstance(node,dict):parents[node['uuid']]=parent;walk(node['children'],node['uuid'])
   else:leaves[node]=parent
 walk(model['outliner'])
 def sample(id,channel):
  frames=[k for k in tracks.get(id,{}).get('keyframes',[])if k['channel']==channel]
  if not frames:return np.zeros(3)
  lo=max((k for k in frames if k['time']<=t),key=lambda k:k['time'],default=frames[0]);hi=min((k for k in frames if k['time']>=t),key=lambda k:k['time'],default=frames[-1]);f=(t-lo['time'])/(hi['time']-lo['time'])if hi['time']!=lo['time']else 0
  return np.array([float(lo['data_points'][0][c])*(1-f)+float(hi['data_points'][0][c])*f for c in 'xyz'])
 def transform(points,id):
  points=np.array(points,dtype=float)
  while id:
   origin=np.array(groups[id]['origin']);rot=np.array(matrix(*sample(id,'rotation')))
   points=(points-origin)@rot.T+origin+sample(id,'position');id=parents.get(id)
  return points
 return leaves,groups,transform
right=np.array([-.87,0,.5]);right/=np.linalg.norm(right);forward=np.array([.5,-.13,.87]);forward/=np.linalg.norm(forward);up=np.cross(right,forward);up/=np.linalg.norm(up)
w,h=600,560;scale=8.0

def frame(tick):
 faces=[]
 def add(points,color):
  points=np.array(points);q=[(w/2+np.dot(p,right)*scale,h*.84-np.dot(p,up)*scale)for p in points]
  faces.append((np.mean(points@forward),q,color))
 for model,t,kind in [(actor,tick/20,'actor'),(weapon,tick/20,'weapon'),(mask,max(0,(tick-12)/20),'mask')]:
  if kind=='mask'and tick<12:continue
  leaves,groups,transform=rig(model,t)
  for e in model['elements']:
   parent=leaves.get(e['uuid']);name=groups.get(parent,{}).get('name','')
   if kind=='actor'and(e['name']!='skin' or name in ['shadow','cape_cape','pri_right_item','pli_left_item']):continue
   if kind=='mask':
    band=int(e['name'].rsplit('_',1)[1]);
    if tick<12+band*2:continue
    for f in e['faces'].values():
     pts=transform([e['vertices'][v]for v in f['vertices']],parent)
     pts+=np.array([head[tick,0]*16,head[tick,2]*16,-head[tick,1]*16])
     uv=f['uv'][f['vertices'][0]];c=int(uv[0]//2);color=[(236,235,239),(180,9,39),(18,17,24),(244,187,45),(215,215,220),(113,113,120),(220,220,220),(250,250,250)][min(7,c)]
     add(pts,color)
    continue
   lo=np.array(e['from']);hi=np.array(e['to']);pts=(lo+hi)/2+CORNERS*(hi-lo)/2
   origin=np.array(e.get('origin',[0,0,0]));pts=(pts-origin)@np.array(matrix(*e.get('rotation',[0,0,0]))).T+origin
   pts=transform(pts,parent)
   if kind=='weapon':
    pts*=.65;pts+=np.array([blade[tick,0]*16,blade[tick,2]*16,-blade[tick,1]*16]);color=(26,23,34)if not e['name'].startswith('white_')else(208,210,224)
   else:
    color=(190,139,105)if name in ['h_ph_head','prfa_right_forearm','plfa_left_forearm']else(55,66,137)if 'leg'in name else(45,151,168)
   for i,indices in enumerate(FACES):add(pts[indices],tuple(int(c*[.85,.94,.8,1,.92,.96][i])for c in color))
 im=Image.new('RGB',(w,h),(174,188,207));d=ImageDraw.Draw(im);d.line([(0,h*.84),(w,h*.84)],fill=(105,123,144),width=2)
 for _,pts,c in sorted(faces,key=lambda f:f[0],reverse=True):d.polygon(pts,fill=c,outline=tuple(max(0,int(v*.8))for v in c))
 phase='MASK'if tick<44 else'ONE-HAND SWEEP'if tick<68 else'RAISE BOTH HANDS'if tick<96 else'CHARGE'if tick<124 else'RELEASE / FOLLOW-THROUGH'
 d.text((16,16),f'Getsuga | {tick/20:.1f}s | {phase}',fill=(30,30,48));d.text((16,h-24),'Actual model geometry preview; not an in-game capture',fill=(52,63,78))
 return im
if __name__=='__main__':
 out=ROOT/'docs/previews';out.mkdir(parents=True,exist_ok=True)
 frames=[frame(t)for t in range(0,160,2)];frames[0].save(out/'getsuga-v086.gif',save_all=True,append_images=frames[1:],duration=100,loop=0)
 sheet=Image.new('RGB',(w*3,h*2))
 for i,t in enumerate([26,44,52,60,96,124]):sheet.paste(frame(t),(i%3*w,i//3*h))
 sheet.save(out/'getsuga-v086.jpg');print(out/'getsuga-v086.gif')
