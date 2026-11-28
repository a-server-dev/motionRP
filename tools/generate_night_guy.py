"""Continuous sprint -> left-foot takeoff -> right knee drive -> heel-first kick."""
import sys,json,hashlib,math,copy
from pathlib import Path
sys.dont_write_bytecode=True
from generate_summons import player,save,RIG_SHA,animator,ident
from generate_rasengan import hand_path

def generate(rig):
 m=player(rig,'night_guy',8,{})
 groups={g['name']:g for g in m['groups']};clip=m['animations'][0];clip['override']=True
 # Source-space rotations. Preserve both root channels: a position track must not replace rotation.
 prefix={'player_root':(0,0,0),'pw_waist':(-35,-12,0),'pc_chest':(-18,12,0),'h_ph_head':(28,0,0),
 'pra_right_arm':(35,0,22),'pla_left_arm':(45,0,-25),'prfa_right_forearm':(100,0,0),'plfa_left_forearm':(110,0,0),
 'prl_right_leg':(55,0,18),'pll_left_leg':(65,0,-18),'prfl_right_foreleg':(-100,0,0),'plfl_left_foreleg':(-110,0,0)}
 ticks=[115,116,119,122,126,130,138,140,144,148,154,160]
 xs={
 'player_root':[-5,-5,0,10,20,28,28,20,8,0,0,0],
 'pw_waist':[-30,-25,-15,-8,-4,0,0,-2,-10,-15,0,0],
 'pc_chest':[-8,-5,-4,0,0,0,0,0,0,-3,0,0],
 'h_ph_head':[30,30,19,-2,-16,-28,-28,-18,2,18,0,0],
 'pra_right_arm':[50,20,-5,-15,-20,-25,-25,-10,20,25,0,0],
 'pla_left_arm':[-50,-25,-20,-25,-30,-35,-35,-15,20,25,0,0],
 'prfa_right_forearm':[65,80,90,60,40,35,35,60,80,75,0,0],
 'plfa_left_forearm':[105,90,100,70,55,50,50,70,80,75,0,0],
 'prl_right_leg':[-50,-35,50,100,90,62,62,60,50,25,0,0],
 'pll_left_leg':[50,30,-10,-20,-25,-25,-25,0,35,30,0,0],
 'prfl_right_foreleg':[-100,-105,-120,-105,-50,-5,-5,-25,-70,-45,0,0],
 'plfl_left_foreleg':[-40,-30,-10,-65,-95,-100,-100,-80,-65,-55,0,0]}
 for name in prefix:
  keys=[(0,(0,0,0)),(.6,prefix[name]),(3.8,prefix[name])]
  for tick in range(96,115,2):
   phase=(tick-96)*math.pi/2;side=-1 if name in ['pll_left_leg','plfl_left_foreleg','pla_left_arm','plfa_left_forearm']else 1
   if name=='player_root':v=(-5,0,0)
   elif name in ['prl_right_leg','pll_left_leg']:v=(side*65*math.cos(phase),0,side*6)
   elif name in ['prfl_right_foreleg','plfl_left_foreleg']:v=(-45-45*max(0,-side*math.cos(phase)),0,0)
   elif name in ['pra_right_arm','pla_left_arm']:v=(-side*60*math.cos(phase),0,side*12)
   elif name in ['prfa_right_forearm','plfa_left_forearm']:v=(85+side*20*math.cos(phase),0,0)
   elif name=='pw_waist':v=(-30,0,0)
   elif name=='pc_chest':v=(-8,5*math.cos(phase),0)
   else:v=(30,0,0)
   keys.append((tick/20.,v))
  for j,tick in enumerate(ticks):
   side=-1 if name.startswith('pl')else 1
   z=(side*(12 if tick<119 else 24 if 'arm' in name else 8)) if name in ['pra_right_arm','pla_left_arm','prl_right_leg','pll_left_leg']else 0
   if tick>=154:z=0
   keys.append((tick/20.,(xs[name][j],0,z)))
  clip['animators'][groups[name]['uuid']]=animator(groups[name],keys)
 root=clip['animators'][groups['player_root']['uuid']]
 root['keyframes'].extend(animator(groups['player_root'],[(0,(0,0,0)),(5.8,(0,0,0)),(5.95,(0,0,0)),(7.2,(0,0,0)),(7.4,(0,0,0)),(8,(0,0,0))],'position')['keyframes'])
 root['keyframes'].sort(key=lambda k:k['time'])
 # Ground contact through the last push-off frame and again from touchdown onward.
 probe=copy.deepcopy(m);samples=[]
 def attach(nodes,uid,marker):
  for n in nodes:
   if isinstance(n,dict):
    if n['uuid']==uid:n['children'].append(marker);return True
    if attach(n['children'],uid,marker):return True
  return False
 for side,name in [(1,'prfl_right_foreleg'),(-1,'plfl_left_foreleg')]:
  for x in ([0,3.75]if side==1 else[-3.75,0]):
   for y in [0,5.625]:
    for z in [-1.875,1.875]:
     label=f'night_sole_{side}_{x}_{y}_{z}';g={'name':label,'uuid':ident(label),'origin':[x,y,z],'rotation':[0,0,0]}
     probe['groups'].append(g);attach(probe['outliner'],groups[name]['uuid'],{'uuid':g['uuid'],'children':[]})
     samples.append([[float(v)for v in row.split(',')]for row in hand_path(probe,161,label).splitlines()])
 contact=[(tick/20.,(0,-min((rows[tick][2]-.03)*16 for rows in samples),0))for tick in list(range(117))+list(range(148,161))]
 root['keyframes']=[k for k in root['keyframes']if k['channel']!='position'or(5.8<k['time']<7.4)]
 root['keyframes'].extend(animator(groups['player_root'],contact,'position')['keyframes']);root['keyframes'].sort(key=lambda k:k['time'])
 for label,parent,origin in [('night_heel','prfl_right_foreleg',[1.875,.3,1.875]),('night_head','h_ph_head',[0,22.5,0]),('night_hip','phip_hip',[0,11.25,0]),('night_up','pc_chest',[0,22,0]),('night_knee','prfl_right_foreleg',[1.875,5.625,0])]:
  g={'name':label,'uuid':ident('night_guy/'+label),'origin':origin,'rotation':[0,0,0],'export':True};m['groups'].append(g);attach(m['outliner'],groups[parent]['uuid'],{'uuid':g['uuid'],'children':[]})
 save('players/motionrp_night_guy.bbmodel',m)
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes();assert hashlib.sha256(raw).hexdigest()==RIG_SHA;generate(json.loads(raw))
