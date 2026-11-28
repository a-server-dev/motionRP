"""Low Eight-Gates stance, launch, heel-first right flying kick and balanced recovery."""
import sys,json,hashlib,math,copy
from pathlib import Path
sys.dont_write_bytecode=True
from generate_summons import player,save,RIG_SHA,animator,ident
from generate_epic_revision import put_track

def generate(rig):
 m=player(rig,'night_guy',11.8,{})
 poses={
 'player_root':[(0,(0,0,0)),(1.6,(0,0,0)),(4.8,(0,0,0)),(5.6,(-20,0,0)),(6.35,(55,0,0)),(6.7,(55,0,0)),(7.3,(20,0,0)),(7.9,(-10,0,0)),(9.7,(0,0,0)),(11.8,(0,0,0))],
 'pw_waist':[(0,(0,0,0)),(.6,(-28,-12,0)),(1.6,(-35,-12,0)),(3.8,(-35,-12,0)),(4.8,(-42,-8,0)),(5.6,(-8,0,0)),(6.4,(0,0,0)),(7.4,(0,0,0)),(9.7,(-10,0,0)),(11.2,(0,0,0))],
 'pc_chest':[(0,(0,0,0)),(.6,(-18,12,0)),(4.8,(-18,12,0)),(5.6,(0,-8,0)),(6.7,(0,-8,0)),(7.9,(0,0,0)),(11.8,(0,0,0))],
 'h_ph_head':[(0,(0,0,0)),(.6,(28,0,0)),(4.8,(28,0,0)),(5.6,(35,0,0)),(6.35,(-55,0,0)),(6.7,(-55,0,0)),(7.9,(0,0,0)),(11.8,(0,0,0))],
 'pra_right_arm':[(0,(0,0,0)),(.6,(35,0,22)),(3.8,(35,0,22)),(4.8,(65,-15,28)),(5.6,(-25,0,35)),(6.7,(-25,0,35)),(7.9,(35,0,25)),(10.8,(0,0,0))],
 'pla_left_arm':[(0,(0,0,0)),(.6,(45,0,-25)),(4.8,(75,10,-25)),(5.6,(-35,0,-35)),(6.7,(-35,0,-35)),(7.9,(35,0,-25)),(10.8,(0,0,0))],
 'prfa_right_forearm':[(0,(0,0,0)),(.6,(100,0,0)),(4.8,(100,0,0)),(5.6,(35,0,0)),(6.7,(35,0,0)),(7.9,(80,0,0)),(10.8,(0,0,0))],
 'plfa_left_forearm':[(0,(0,0,0)),(.6,(110,0,0)),(4.8,(110,0,0)),(5.6,(45,0,0)),(6.7,(45,0,0)),(7.9,(80,0,0)),(10.8,(0,0,0))],
 'prl_right_leg':[(0,(0,0,0)),(.6,(55,0,18)),(4.8,(55,0,18)),(5.4,(95,0,8)),(6.35,(75,0,3)),(6.7,(75,0,3)),(7.3,(90,0,10)),(7.9,(45,0,15)),(9.7,(25,0,15)),(11.2,(0,0,0))],
 'pll_left_leg':[(0,(0,0,0)),(.6,(65,0,-18)),(4.8,(65,0,-18)),(5.4,(-30,0,-15)),(6.35,(-20,0,-12)),(6.7,(-20,0,-12)),(7.9,(45,0,-15)),(9.7,(25,0,-15)),(11.2,(0,0,0))],
 'prfl_right_foreleg':[(0,(0,0,0)),(.6,(-100,0,0)),(4.8,(-100,0,0)),(5.4,(-95,0,0)),(6.35,(-3,0,0)),(6.7,(-3,0,0)),(7.3,(-70,0,0)),(9.7,(-45,0,0)),(11.2,(0,0,0))],
 'plfl_left_foreleg':[(0,(0,0,0)),(.6,(-110,0,0)),(4.8,(-110,0,0)),(5.4,(-80,0,0)),(6.35,(-90,0,0)),(6.7,(-90,0,0)),(7.9,(-85,0,0)),(9.7,(-45,0,0)),(11.2,(0,0,0))]}
 for name,keys in poses.items():put_track(m,name,keys)
 put_track(m,'player_root',[(0,(0,0,0)),(.6,(0,-1.7,0)),(4.5,(0,-1.7,0)),(4.8,(0,-2.4,0)),(5.6,(0,0,0)),(7.4,(0,0,0)),(9.7,(0,-.6,0)),(10.1,(0,-1,0)),(11.2,(0,0,0))],'position')
 # Insert twenty ticks of sprint before the existing jump and remove the retreat section.
 clip=m['animations'][0];clip['length']=11.3
 for track in clip['animators'].values():
  keep=[]
  for key in track['keyframes']:
   if key['time']>9.7:continue
   if key['time']>=4.8:key['time']+=1
   keep.append(key)
  channels={k['channel'] for k in keep}
  group=next(g for g in m['groups']if g['name']==track['name'])
  for channel in channels:keep.extend(animator(group,[(11.3,(0,0,0))],channel)['keyframes'])
  track['keyframes']=sorted(keep,key=lambda k:k['time'])
 groups={g['name']:g for g in m['groups']}
 for name in ['prl_right_leg','pll_left_leg','prfl_right_foreleg','plfl_left_foreleg','pra_right_arm','pla_left_arm','prfa_right_forearm','plfa_left_forearm','pw_waist','pc_chest','h_ph_head']:
  keys=[]
  for tick in range(96,116,2):
   phase=(tick-96)*math.pi/2
   side=-1 if name in ['pll_left_leg','plfl_left_foreleg','pla_left_arm','plfa_left_forearm']else 1
   if name in ['prl_right_leg','pll_left_leg']:value=(side*65*math.cos(phase),0,side*6)
   elif name in ['prfl_right_foreleg','plfl_left_foreleg']:value=(-45-45*max(0,-side*math.cos(phase)),0,0)
   elif name in ['pra_right_arm','pla_left_arm']:value=(-side*60*math.cos(phase),0,side*12)
   elif name in ['prfa_right_forearm','plfa_left_forearm']:value=(85+side*20*math.cos(phase),0,0)
   elif name=='pw_waist':value=(-30,0,0)
   elif name=='pc_chest':value=(-8,side*5*math.cos(phase),0)
   else:value=(30,0,0)
   keys.append((tick/20.,value))
  track=clip['animators'][groups[name]['uuid']]
  track['keyframes']=[k for k in track['keyframes']if not(4.8<=k['time']<5.8)]
  track['keyframes'].extend(animator(groups[name],keys)['keyframes']);track['keyframes'].sort(key=lambda k:k['time'])
 # Bake foot contact during the grounded sprint using the authored hierarchy itself.
 from generate_rasengan import hand_path
 probe=copy.deepcopy(m);samples=[]
 def attach(nodes,uid,marker):
  for node in nodes:
   if isinstance(node,dict):
    if node['uuid']==uid:node['children'].append(marker);return True
    if attach(node['children'],uid,marker):return True
  return False
 for side,name in [(1,'prfl_right_foreleg'),(-1,'plfl_left_foreleg')]:
  for x in ([0,3.75]if side==1 else[-3.75,0]):
   for y in [0,5.625]:
    for z in [-1.875,1.875]:
     label=f'sprint_sole_{side}_{x}_{y}_{z}';g={'name':label,'uuid':ident(label),'origin':[x,y,z],'rotation':[0,0,0]}
     probe['groups'].append(g);attach(probe['outliner'],groups[name]['uuid'],{'uuid':g['uuid'],'children':[]})
     samples.append([[float(v)for v in row.split(',')]for row in hand_path(probe,226,label).splitlines()])
 root=clip['animators'][groups['player_root']['uuid']];positions=[k for k in root['keyframes']if k['channel']=='position']
 def old_y(t):
  lo=max((k for k in positions if k['time']<=t),key=lambda k:k['time'],default=positions[0]);hi=min((k for k in positions if k['time']>=t),key=lambda k:k['time'],default=positions[-1]);f=(t-lo['time'])/(hi['time']-lo['time'])if hi['time']!=lo['time']else 0
  return float(lo['data_points'][0]['y'])*(1-f)+float(hi['data_points'][0]['y'])*f
 contact=[]
 for tick in range(96,116):contact.append((tick/20.,(0,old_y(tick/20.)-min((rows[tick][2]-.03)*16 for rows in samples),0)))
 root['keyframes']=[k for k in root['keyframes']if k['channel']!='position'or not(4.8<=k['time']<5.8)]
 root['keyframes'].extend(animator(groups['player_root'],contact,'position')['keyframes']);root['keyframes'].sort(key=lambda k:k['time'])
 clip['override']=True;save('players/motionrp_night_guy.bbmodel',m)
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes();assert hashlib.sha256(raw).hexdigest()==RIG_SHA;generate(json.loads(raw))
