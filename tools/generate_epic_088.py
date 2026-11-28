"""Reference armor refinement and grounded shoulder tackle; preserves the exact Titan skin."""
import sys,copy,json,math
sys.dont_write_bytecode=True
from generate_summons import ROOT,Model,animation,animator,save
from generate_epic_refinement import myoo,BROWN
from generate_energy import add_mesh
from generate_rasengan import hand_path
from generate_epic_revision import put_track

def refine_myoo():
 myoo();p=ROOT/'models/motionrp_tengen_myoo.bbmodel';m=Model('motionrp_tengen_myoo',False,BROWN);m.data=json.loads(p.read_text())
 removed={e['uuid'] for e in m.data['elements'] if e['name'].startswith(('rounded_pauldron_','closed_helmet_shell_','curved_helmet_crown_','breastplate_stratum_','overlapping_sternum_','giant_sword_blade_','blade_edge_','metal_visor_grille_','closed_visor_slit_'))}
 m.data['elements']=[e for e in m.data['elements'] if e['uuid'] not in removed]
 def prune(nodes):
  nodes[:]=[n for n in nodes if not isinstance(n,str) or n not in removed]
  for n in nodes:
   if isinstance(n,dict):prune(n['children'])
 prune(m.data['outliner'])
 def node(name,nodes=None):
  for n in m.data['outliner'] if nodes is None else nodes:
   if isinstance(n,dict):
    if n['uuid']==next(g['uuid'] for g in m.data['groups'] if g['name']==name):return n
    child=node(name,n['children'])
    if child:return child
 def mesh(name,parent,verts,faces):
  uid=add_mesh(m,name,verts,faces)
  # This armor retains its original 8x1 palette, unlike energy mesh textures.
  for face in m.data['elements'][-1]['faces'].values():
   color=int(next(iter(face['uv'].values()))[0]//2)
   face['uv']={v:[color+.5,.5] for v in face['vertices']}
  node(parent)['children'].append(uid)
 def shell(name,parent,center,radii,color):
  v={};f=[];lat,lon=12,24
  for j in range(lat+1):
   phi=.03+(math.pi-.06)*j/lat
   for i in range(lon):
    t=math.tau*i/lon;v[f'{j}_{i}']=[center[0]+radii[0]*math.sin(phi)*math.cos(t),center[1]+radii[1]*math.cos(phi),center[2]+radii[2]*math.sin(phi)*math.sin(t)]
  for j in range(lat):
   for i in range(lon):f.append(([f'{j}_{i}',f'{j}_{(i+1)%lon}',f'{j+1}_{(i+1)%lon}',f'{j+1}_{i}'],color if j%4 else 2))
  mesh(name,parent,v,f)
 shell('forged_curved_breastplate','heavy_breastplate',(0,18,0),(21,17,11),1)
 shell('domed_kabuto','closed_helmet_and_veil',(0,48,1),(12.5,14,11.5),1)
 for side in [-1,1]:shell('rounded_pauldron_shell_'+str(side),'massive_pauldron_'+str(side),(side*31,29,0),(14,14,13),1)
 # Narrow eye openings replace the cage-like grille; the crimson veil stays visible below.
 for side in [-1,1]:
  uid=m.cube('closed_visor_slit_eye_'+str(side),(side*4.5,46,-12.4),(5.5,1.5,1.2),3)
  node('closed_helmet_and_veil')['children'].append(uid)
 # Broad forged blade, central ridge, separate polished edge and angled kissaki.
 v={};sections=[(31,3.4),(8,3.25),(-17,2.8),(-26,2.4),(-30,0)]
 for j,(y,w) in enumerate(sections):
  for k,(x,z) in enumerate([(-w,0),(0,-1),(w,0),(0,1)]):v[f'{j}_{k}']=[29+x,y-54,-3+z]
 faces=[]
 for j in range(4):
  for k in range(4):faces.append(([f'{j}_{k}',f'{j}_{(k+1)%4}',f'{j+1}_{(k+1)%4}',f'{j+1}_{k}'],6 if k%2 else 7))
 mesh('giant_sword_blade_forged','forearm_1',v,faces)
 slam=next(a for a in m.data['animations'] if a['name']=='slam');groups={g['name']:g for g in m.data['groups']}
 for side in [-1,1]:
  upper=groups['sword_arm'if side==1 else'guard_arm'];fore=groups['forearm_'+str(side)]
  keys=([(0,(0,0,12)),(1.7,(125,-30,23)),(3.2,(125,-30,23)),(3.5,(80,-30,32)),(4.3,(70,40,40)),(4.8,(55,38,32)),(8.3,(55,38,32))] if side==1 else [(0,(0,0,-12)),(1.7,(25,0,-18)),(3.2,(25,0,-18)),(4.3,(20,-15,-28)),(8.3,(20,-15,-28))])
  slam['animators'][upper['uuid']]=animator(upper,keys)
  slam['animators'][fore['uuid']]=animator(fore,[(0,(15,0,0)),(1.7,(20,0,0)),(3.2,(20,0,0)),(4.3,(12,0,0)),(8.3,(12,0,0))])
 idle=next(a for a in m.data['animations'] if a['name']=='idle')
 for side in [-1,1]:
  upper=groups['sword_arm'if side==1 else'guard_arm'];fore=groups['forearm_'+str(side)]
  idle['animators'][upper['uuid']]=animator(upper,[(0,(0,0,side*12)),(1,(0,0,side*12))]);idle['animators'][fore['uuid']]=animator(fore,[(0,(15,0,0)),(1,(15,0,0))])
 m.save()
 # Player mirrors exactly the same upper-body timeline (slam starts at tick54).
 p=ROOT/'players/motionrp_kokujo_tengen_myoo.bbmodel';actor=json.loads(p.read_text());clip=actor['animations'][0];actor_groups={g['name']:g for g in actor['groups']}
 for side in [-1,1]:
  for src,dst in [('sword_arm'if side==1 else'guard_arm','pra_right_arm'if side==1 else'pla_left_arm'),('forearm_'+str(side),'prfa_right_forearm'if side==1 else'plfa_left_forearm')]:
   t=copy.deepcopy(slam['animators'][groups[src]['uuid']]);t['name']=dst
   for k in t['keyframes']:k['time']+=2.7
   first=copy.deepcopy(t['keyframes'][0]);first['time']=0;t['keyframes'].insert(0,first);clip['animators'][actor_groups[dst]['uuid']]=t
 save('players/motionrp_kokujo_tengen_myoo.bbmodel',actor);(ROOT/'kokujo_tengen_myoo-hand-path.csv').write_text(hand_path(actor,220))

def titan():
 p=ROOT/'players/motionrp_titan_transform.bbmodel';m=json.loads(p.read_text());groups={g['name']:g for g in m['groups']}
 a=animation('titan_tackle',5.4,{});a['override']=True
 # Actual Blockbench rotations: torso folds forward while knees bend into a low drive.
 poses={'player_root':[(0,(0,0,0)),(.15,(-10,25,-4)),(.35,(-25,70,-12)),(.5,(-28,70,-12)),(5.4,(-28,70,-12))],
 'pw_waist':[(0,(0,0,0)),(.35,(-8,0,0)),(5.4,(-8,0,0))],
 'pc_chest':[(0,(0,0,0)),(.35,(-5,10,0)),(5.4,(-5,10,0))],
 'h_ph_head':[(0,(-10,0,0)),(.35,(28,-65,0)),(5.4,(28,-65,0))],
 'pra_right_arm':[(0,(5,0,15)),(.35,(35,0,-12)),(5.4,(35,0,-12))],
 'pla_left_arm':[(0,(5,0,-15)),(.35,(-20,0,-12)),(5.4,(-20,0,-12))],
 'prfa_right_forearm':[(0,(0,0,0)),(.35,(120,0,0)),(5.4,(120,0,0))],
 'plfa_left_forearm':[(0,(0,0,0)),(.35,(65,0,0)),(5.4,(65,0,0))],
 'prl_right_leg':[(0,(0,0,0)),(.15,(40,0,12)),(.35,(80,0,14)),(5.4,(80,0,14))],
 'pll_left_leg':[(0,(0,0,0)),(.15,(45,0,-12)),(.35,(90,0,-14)),(5.4,(90,0,-14))],
 'prfl_right_foreleg':[(0,(0,0,0)),(.15,(-80,0,0)),(.35,(-100,0,0)),(5.4,(-100,0,0))],
 'plfl_left_foreleg':[(0,(0,0,0)),(.15,(-80,0,0)),(.35,(-115,0,0)),(5.4,(-115,0,0))]}
 sprint_end={'player_root':(-8,0,0),'pw_waist':(-18,0,0),'pc_chest':(-8,-4,0),'h_ph_head':(25,0,0),'pra_right_arm':(50,0,12),'pla_left_arm':(-50,0,-12),'prfa_right_forearm':(60,0,0),'plfa_left_forearm':(100,0,0),'prl_right_leg':(-55,0,8),'pll_left_leg':(55,0,-8),'prfl_right_foreleg':(-90,0,0),'plfl_left_foreleg':(-40,0,0)}
 for name,keys in poses.items():
  keys[0]=(0,sprint_end[name]);a['animators'][groups[name]['uuid']]=animator(groups[name],keys)
 m['animations']=[x for x in m['animations'] if x['name']not in ('titan_tackle','titan_sprint')]+[a];save('players/motionrp_titan_transform.bbmodel',m)
 body=json.loads((ROOT/'models/motionrp_titan_body.bbmodel').read_text());b=copy.deepcopy(a);bg={g['uuid']:g for g in body['groups']}
 for uid,t in b['animators'].items():t['name']=bg[uid]['name']
 body['animations']=[x for x in body['animations'] if x['name']not in ('titan_tackle','titan_sprint')]+[b];save('models/motionrp_titan_body.bbmodel',body)
 # Ground the bent knees by lowering the whole rig, then bake the actual nape position.
 from hollow_kinematics import matrix,vector
 parents={}
 def walk(nodes,parent=None):
  for n in nodes:
   if isinstance(n,dict):parents[n['uuid']]=parent;walk(n['children'],n['uuid'])
 walk(m['outliner']);byid={g['uuid']:g for g in m['groups']}
 def sample(uid,channel,t):
  frames=[k for k in a['animators'].get(uid,{}).get('keyframes',[]) if k['channel']==channel]
  if not frames:return [0,0,0]
  lo=max((k for k in frames if k['time']<=t),key=lambda k:k['time'],default=frames[0]);hi=min((k for k in frames if k['time']>=t),key=lambda k:k['time'],default=frames[-1]);f=(t-lo['time'])/(hi['time']-lo['time']) if hi['time']!=lo['time']else 0
  return [float(lo['data_points'][0][c])*(1-f)+float(hi['data_points'][0][c])*f for c in 'xyz']
 def point(name,p,t):
  uid=groups[name]['uuid']
  while uid:
   o=byid[uid]['origin'];r=vector(matrix(*sample(uid,'rotation',t)),[p[i]-o[i] for i in range(3)]);shift=sample(uid,'position',t);p=[o[i]+r[i]+shift[i] for i in range(3)];uid=parents[uid]
  return p
 root=groups['player_root'];keys=[]
 for tick in range(109):
  t=tick/20
  low=min(point(name,[x,y,z],t)[1] for name,xs in [('prfl_right_foreleg',[0,3.75]),('plfl_left_foreleg',[-3.75,0])] for x in xs for y in [0,5.625] for z in [-1.875,1.875])
  keys.append((t,(0,-low,0)))
 a['animators'][root['uuid']]['keyframes'].extend(animator(root,keys,'position')['keyframes']);a['animators'][root['uuid']]['keyframes'].sort(key=lambda k:k['time'])
 rows=[]
 for tick in range(109):
  p=point('h_ph_head',[0,23,4.2],tick/20);rows.append(','.join(str(v*10/16) for v in [p[0],-p[2],p[1]]))
 (ROOT/'titan-tackle-nape.csv').write_text('\n'.join(rows)+'\n')
 m['animations'][-1]=a;save('players/motionrp_titan_transform.bbmodel',m)
 b=copy.deepcopy(a)
 for uid,t in b['animators'].items():t['name']=bg[uid]['name']
 body['animations'][-1]=b;save('models/motionrp_titan_body.bbmodel',body)
 from generate_titan_sprint import generate
 generate()

if __name__=='__main__':refine_myoo();titan()
