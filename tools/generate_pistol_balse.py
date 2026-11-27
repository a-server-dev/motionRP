"""Gear-three inflated fist and consensual two-player spell, editable BetterModel assets."""
import sys,json,math,hashlib
from generate_perfect_susanoo import direction_rotation,normalized,transpose,continuous
from hollow_kinematics import mul,euler,matrix,vector
from generate_summons import ident
sys.dont_write_bytecode=True
from pathlib import Path
from generate_summons import Model,player,save,animation,animator,RIG_SHA

def generate(rig):
 player(rig,'gigant_pistol',7.5,{
 'pc_chest':[(0,(0,0,0)),(.6,(-5,-12,0)),(2,(-8,-22,0)),(3.2,(-12,-38,0)),(3.6,(24,18,0)),(4.6,(24,18,0)),(6.2,(5,5,0)),(7.5,(0,0,0))],
 'pra_right_arm':[(0,(0,0,0)),(.5,(100,-25,-10)),(1.6,(100,-25,-10)),(2.5,(-20,-20,15)),(3.2,(-48,-25,18)),(3.6,(90,0,0)),(4.7,(90,0,0)),(6.4,(12,0,4)),(7.5,(0,0,0))],
 'prfa_right_forearm':[(0,(0,0,0)),(.5,(90,0,0)),(1.6,(90,0,0)),(2.6,(45,0,0)),(3.2,(55,0,0)),(3.6,(0,0,0)),(5,(0,0,0)),(7.5,(0,0,0))],
 'pla_left_arm':[(0,(0,0,0)),(.6,(80,15,12)),(1.6,(80,15,12)),(2.8,(20,0,-25)),(3.6,(-35,0,-20)),(5,(-35,0,-20)),(7.5,(0,0,0))],
 'plfa_left_forearm':[(0,(0,0,0)),(.6,(65,0,0)),(1.6,(65,0,0)),(3.6,(40,0,0)),(7.5,(0,0,0))],
 'prl_right_leg':[(0,(0,0,0)),(2,(-16,0,10)),(3.2,(-25,0,12)),(3.6,(-18,0,12)),(6,(-18,0,12)),(7.5,(0,0,0))],
 'pll_left_leg':[(0,(0,0,0)),(2,(25,0,-12)),(3.6,(38,0,-12)),(6,(38,0,-12)),(7.5,(0,0,0))],
 'plfl_left_foreleg':[(0,(0,0,0)),(2,(-25,0,0)),(3.6,(-50,0,0)),(6,(-50,0,0)),(7.5,(0,0,0))]})
 skin=[(226,159,112,255),(255,211,164,255),(151,86,57,255),(246,185,133,255),(116,65,46,255),(255,232,193,255),(192,122,82,255),(233,170,121,255)]
 m=Model('motionrp_gigant_fist',False,skin);parts=[]
 # Rounded barrel forearm, palm, four curled fingers, thumb and creases. Forward=-Z.
 for j in range(12):
  z=6-j*1.7;r=4.2+2.5*math.sin(j/11*math.pi*.8)
  parts.append(m.cube('inflated_forearm_'+str(j),(0,0,z),(r*2,r*1.75,2),3 if j%3 else 0))
  for side in [-1,1]:parts.append(m.cube(f'forearm_highlight_{j}_{side}',(side*r*.6,r*.7,z),(r*.55,.5,1.9),1))
 parts.append(m.cube('palm',(0,0,-16),(16,13,10),0))
 for finger in range(4):
  x=(finger-1.5)*4
  parts.append(m.cube('curled_finger_'+str(finger),(x,-.2,-22),(3.8,11.5,6),3))
  parts.append(m.cube('knuckle_'+str(finger),(x,4,-23),(3.7,4.8,5.5),1))
  parts.append(m.cube('finger_crease_'+str(finger),(x,-3.2,-25.1),(3,.35,.3),2))
 parts.append(m.cube('folded_thumb',(7,-5,-18),(5,5,10),0,[0,0,-22.5]))
 parts.append(m.cube('thumb_nail',(9.6,-4.8,-21),(.3,2.6,3),1))
 m.group('inflated_fist',parts);m.save()
 for left in [True,False]:
  sg=1 if left else -1;arm='pra_right_arm'if left else'pla_left_arm';fore='prfa_right_forearm'if left else'plfa_left_forearm'
  player(rig,'balse_'+('left'if left else'right'),7,{
   arm:[(0,(0,0,sg*5)),(1,(58,sg*25,sg*30)),(2,(72,sg*30,sg*38)),(5.5,(72,sg*30,sg*38)),(6.5,(15,0,sg*5)),(7,(0,0,0))],
   fore:[(0,(0,0,0)),(1,(32,0,0)),(2,(28,0,0)),(5.5,(28,0,0)),(7,(0,0,0))],
   'h_ph_head':[(0,(0,0,0)),(1,(18,sg*15,0)),(2,(12,sg*8,0)),(3.5,(-10,0,0)),(5.5,(-10,0,0)),(7,(0,0,0))],
   'pc_chest':[(0,(0,0,0)),(1,(0,sg*8,0)),(3.5,(-5,sg*8,0)),(6.5,(0,0,0))]})
 blue=[(47,162,255,255),(190,255,255,255),(14,59,151,255),(255,255,255,255),(66,223,255,255),(128,221,255,255),(19,104,217,255),(229,255,255,255)]
 m=Model('motionrp_balse_crystal',False,blue);parts=[]
 for j in range(5):
  w=3.8-abs(j-2)*.85;parts.append(m.cube('crystal_facet_'+str(j),(0,(j-2)*.85,0),(w,.95,2.4),j%2))
 for j in range(3):parts.append(m.cube('ancient_sigil_'+str(j),(0,(j-1)*.6,-1.27),(1.2 if j==1 else .5,.18,.14),3))
 m.group('flying_stone',parts);m.save()
 m=Model('motionrp_balse_light',False,blue);parts=[]
 for axis in range(3):
  for side in [-1,1]:
   for j in range(10):
    center=[0.,0.,0.];center[axis]=side*(j*2+1);size=[.6,.6,.6];size[axis]=2.1
    parts.append(m.cube(f'white_ray_{axis}_{side}_{j}',center,size,3))
 for j in range(32):
  q=math.tau*j/32;parts.append(m.cube('halo_'+str(j),(9*math.cos(q),9*math.sin(q),0),(1,1,.5),1))
 g=m.group('radiant_halo',parts);a=animation('radiance',2,{});a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1,(0,0,180)),(2,(0,0,360))]);m.data['animations']=[a];m.save()
def corrected_actors():
 from generate_summons import ROOT
 # Undo the legacy pose-table X conversion for these newly authored arms.
 for name in ['gigant_pistol','balse_left','balse_right']:
  p=ROOT/('players/motionrp_'+name+'.bbmodel');m=json.loads(p.read_text());groups={g['name']:g for g in m['groups']};clip=m['animations'][0]
  for entry in clip['animators'].values():
   for k in entry['keyframes']:
    if k['channel']=='rotation':k['data_points'][0]['x']=str(-float(k['data_points'][0]['x']))
  def track(bone,keys,ch='rotation'):
   g=groups[bone];part=animator(g,keys,ch);entry=clip['animators'].setdefault(g['uuid'],{'name':bone,'type':'bone','keyframes':[]});entry['keyframes']=[k for k in entry['keyframes']if k['channel']!=ch]+part['keyframes']
  def probe(label,parent,point):
   g={'name':label,'uuid':ident(name+'/'+label),'origin':list(point),'rotation':[0,0,0],'export':True};m['groups'].append(g)
   def bind(nodes):
    for n in nodes:
     if isinstance(n,dict):
      if n['uuid']==groups[parent]['uuid']:n['children'].append({'uuid':g['uuid'],'children':[]});return True
      if bind(n['children']):return True
    return False
   assert bind(m['outliner'])
  if name=='gigant_pistol':
   # Enlarge actual player skin bones, with child scales relative to their parents.
   track('pra_right_arm',[(0,(1,1,1)),(.9,(1,1,1)),(2.4,(8,5,8)),(5.2,(8,5,8)),(6.7,(1,1,1)),(7.5,(1,1,1))],'scale')
   track('prfa_right_forearm',[(0,(1,1,1)),(.9,(1,1,1)),(2.4,(1.35,1,1.35)),(3.2,(1.35,1,1.35)),(3.7,(1.35,4,1.35)),(5.2,(1.35,4,1.35)),(6.7,(1,1,1)),(7.5,(1,1,1))],'scale')
   # Fixed shoulder and elbow positions: no detached or independently flying fist.
   track('pra_right_arm',[(0,(0,0,0)),(.5,(100,0,-12)),(.9,(100,0,-12)),(2.4,(75,-35,20)),(3.2,(80,-75,25)),(3.7,(90,0,0)),(5,(90,0,0)),(6.3,(90,0,0)),(6.7,(0,0,0)),(7.5,(0,0,0))])
   track('prfa_right_forearm',[(0,(0,0,0)),(.5,(65,0,0)),(.9,(65,0,0)),(2.4,(30,0,0)),(3.2,(55,0,0)),(3.7,(0,0,0)),(5,(0,0,0)),(6.7,(0,0,0)),(7.5,(0,0,0))])
   # Keep the giant shoulder outside the torso by bracing rather than crossing it.
   track('pc_chest',[(0,(0,0,0)),(2.4,(-5,-12,0)),(3.2,(-5,-20,0)),(3.7,(10,0,0)),(5,(10,0,0)),(7.5,(0,0,0))])
   for label,parent,point in [('pistol_shoulder','pra_right_arm',(5,22,0)),('pistol_elbow','prfa_right_forearm',(5,16.5,0)),('pistol_palm','prfa_right_forearm',(5,10,0)),('pistol_width_a','prfa_right_forearm',(3,12,0)),('pistol_width_b','prfa_right_forearm',(7,12,0))]:probe(label,parent,point)
  else:
   side=1 if name.endswith('left')else -1;upper='pra_right_arm'if side==1 else'pla_left_arm';fore='prfa_right_forearm'if side==1 else'plfa_left_forearm'
   shoulder=[side*5,22,0];hand=[side*8.8,21.6,-10.4];delta=[hand[i]-shoulder[i]for i in range(3)];dist=math.sqrt(sum(v*v for v in delta));aim=normalized(delta)
   along=(5.5**2-6.5**2+dist**2)/(2*dist);height=math.sqrt(5.5**2-along**2);ref=[side*.8,-1,0];dot=sum(ref[i]*aim[i]for i in range(3));bend=normalized([ref[i]-dot*aim[i]for i in range(3)])
   elbow=[shoulder[i]+aim[i]*along+bend[i]*height for i in range(3)];a=direction_rotation([elbow[i]-shoulder[i]for i in range(3)],True);b=direction_rotation([hand[i]-elbow[i]for i in range(3)],True)
   track(upper,[(0,(0,0,side*4)),(1.7,euler(a)),(5.5,euler(a)),(6.7,(0,0,side*4)),(7,(0,0,0))]);track(fore,[(0,(0,0,0)),(1.7,euler(mul(transpose(a),b))),(5.5,euler(mul(transpose(a),b))),(7,(0,0,0))]);track('pc_chest',[(0,(0,0,0)),(7,(0,0,0))])
   probe('balse_palm',fore,(side*5,10,0))
  if name=='gigant_pistol':
   parents={}
   def walk(nodes,parent=None):
    for node in nodes:
     if isinstance(node,dict):parents[node['uuid']]=parent;walk(node['children'],node['uuid'])
   walk(m['outliner']);byid={g['uuid']:g for g in m['groups']}
   def sample(uid,ch,t):
    keys=[k for k in clip['animators'].get(uid,{}).get('keyframes',[])if k['channel']==ch]
    if not keys:return [1,1,1]if ch=='scale'else[0,0,0]
    a=max((k for k in keys if k['time']<=t),key=lambda k:k['time'],default=keys[0]);b=min((k for k in keys if k['time']>=t),key=lambda k:k['time'],default=keys[-1]);q=(t-a['time'])/(b['time']-a['time'])if a['time']!=b['time']else 0
    return [float(a['data_points'][0][c])*(1-q)+float(b['data_points'][0][c])*q for c in 'xyz']
   def point(label,t):
    g=next(g for g in m['groups']if g['name']==label);p=g['origin'][:];uid=g['uuid']
    while uid is not None:
     o=byid[uid]['origin'];sc=sample(uid,'scale',t);delta=[(p[i]-o[i])*sc[i]for i in range(3)];v=vector(matrix(*sample(uid,'rotation',t)),delta);pos=sample(uid,'position',t);p=[o[i]+v[i]+pos[i]for i in range(3)];uid=parents[uid]
    return p
   def dist(a,b):return math.sqrt(sum((x-y)**2 for x,y in zip(a,b)))/16
   rows=[]
   for tick in range(150):
    t=tick/20;p=point('pistol_palm',t);width=dist(point('pistol_width_a',t),point('pistol_width_b',t));reach=dist(point('pistol_shoulder',t),p);rows.append(','.join(f'{v:.8f}'for v in [p[0]/16,-p[2]/16,p[1]/16,width,reach]))
   (ROOT/'pistol-palm.csv').write_text('\n'.join(rows)+'\n')
  save('players/motionrp_'+name+'.bbmodel',m)

if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes();assert hashlib.sha256(raw).hexdigest()==RIG_SHA;generate(json.loads(raw));corrected_actors()
