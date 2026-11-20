"""Original two-player rock-slam poses, cosmetic stone slab and cracked impact ring."""
import sys
sys.dont_write_bytecode=True
import json,math,hashlib
from pathlib import Path
from generate_summons import ROOT,RIG_SHA,player,save,animation,animator
from generate_epic_revision import put_track
from generate_energy import mesh_model,add_mesh
from generate_battle_visuals import material_texture

def actors(rig):
 for attack in [True,False]:
  name='broly_attacker'if attack else'broly_victim';m=player(rig,name,7.2,{})
  # This clock bone is intentionally never overridden by phase animations.
  put_track(m,'player_root',[(0,(0,0,0)),(7.2,(0,0,0))],'position');m['animations'][0]['override']=True
  groups={g['name']:g for g in m['groups']}
  poses={
   'grapple':{'pc_chest':(-12,0,0)if attack else(20,0,0),'pw_waist':(0,0,0)if attack else(-10,0,0),'pra_right_arm':(120,-10,0)if attack else(65,-10,20),'prfa_right_forearm':(8,0,0)if attack else(75,0,0),'pla_left_arm':(40,0,-15)if attack else(75,10,-20),'plfa_left_forearm':(80,0,0),'h_ph_head':(0,0,0)if attack else(-25,0,0),'prl_right_leg':(15,0,8),'pll_left_leg':(-15,0,-8)},
   'flight':{'pc_chest':(25,0,0)if attack else(-50,0,0),'pra_right_arm':(100,0,0)if attack else(135,0,35),'prfa_right_forearm':(5,0,0)if attack else(35,0,0),'pla_left_arm':(55,0,-12)if attack else(120,0,-35),'plfa_left_forearm':(70,0,0),'prl_right_leg':(-25,0,10),'prfl_right_foreleg':(-50,0,0),'pll_left_leg':(30,0,-10),'plfl_left_foreleg':(-65,0,0),'h_ph_head':(5,0,0)if attack else(-25,0,0)},
   'impact':{'pc_chest':(30,0,0)if attack else(-35,0,0),'pra_right_arm':(110,0,0)if attack else(100,0,30),'prfa_right_forearm':(0,0,0)if attack else(80,0,0),'pla_left_arm':(65,0,-20)if attack else(95,0,-30),'plfa_left_forearm':(70,0,0),'h_ph_head':(5,0,0)if attack else(-40,0,0),'prl_right_leg':(10,0,12),'pll_left_leg':(-15,0,-12)},
   'recover':{'pc_chest':(0,0,0)if attack else(30,0,0),'pw_waist':(0,0,0)if attack else(12,0,0),'pra_right_arm':(0,0,5)if attack else(15,0,15),'pla_left_arm':(0,0,-5)if attack else(15,0,-15),'prfa_right_forearm':(0,0,0),'plfa_left_forearm':(0,0,0),'h_ph_head':(0,0,0)if attack else(35,0,0),'prl_right_leg':(0,0,0)if attack else(30,0,12),'pll_left_leg':(0,0,0)if attack else(25,0,-12),'prfl_right_foreleg':(0,0,0)if attack else(-50,0,0),'plfl_left_foreleg':(0,0,0)if attack else(-45,0,0)}
  }
  poses['stand']={b:(0,0,0)for b in ['pc_chest','pw_waist','pra_right_arm','prfa_right_forearm','pla_left_arm','plfa_left_forearm','h_ph_head','prl_right_leg','pll_left_leg','prfl_right_foreleg','plfl_left_foreleg']}
  poses['stand'].update({'pra_right_arm':(12,0,12),'pla_left_arm':(12,0,-12),'prfa_right_forearm':(25,0,0),'plfa_left_forearm':(25,0,0),'prl_right_leg':(10,0,8),'pll_left_leg':(-8,0,-8),'prfl_right_foreleg':(-25,0,0),'plfl_left_foreleg':(-20,0,0)})
  poses['charge']={'pc_chest':(40,0,0),'pra_right_arm':(105,0,0),'prfa_right_forearm':(5,0,0),'pla_left_arm':(-35,0,-15),'plfa_left_forearm':(70,0,0),'prl_right_leg':(-30,0,10),'pll_left_leg':(35,0,-10),'prfl_right_foreleg':(-65,0,0),'plfl_left_foreleg':(-35,0,0),'h_ph_head':(-15,0,0)}
  poses['fright']={'pc_chest':(-15,0,0),'pra_right_arm':(65,0,28),'pla_left_arm':(65,0,-28),'prfa_right_forearm':(80,0,0),'plfa_left_forearm':(80,0,0),'h_ph_head':(-12,0,0),'prl_right_leg':(15,0,12),'pll_left_leg':(-12,0,-12)}
  for phase,pose in poses.items():
   a=animation(phase,.5 if phase=='flight' else 1,{});a['override']=True
   for bone,value in pose.items():a['animators'][groups[bone]['uuid']]=animator(groups[bone],[(0,value),(.5 if phase=='flight' else 1,value)])
   if phase=='flight':a['loop']='loop'
   m['animations'].append(a)
  run=animation('flee',.8,{});run['override']=True;run['loop']='loop'
  flying={'pc_chest':(38,0,0),'h_ph_head':(-20,0,0),'pra_right_arm':(-20,0,12),'pla_left_arm':(-20,0,-12),'prfa_right_forearm':(40,0,0),'plfa_left_forearm':(40,0,0),'prl_right_leg':(-18,0,8),'pll_left_leg':(-12,0,-8),'prfl_right_foreleg':(-35,0,0),'plfl_left_foreleg':(-28,0,0)}
  for bone,value in flying.items():
   keys=[(t,(value[0]+(2*math.sin(t*math.tau/.8)if bone in ['pc_chest','prl_right_leg','pll_left_leg']else 0),value[1],value[2]))for t in [0,.2,.4,.6,.8]]
   run['animators'][groups[bone]['uuid']]=animator(groups[bone],keys)
  turn=animation('turn',.6,{});turn['override']=True
  for bone,keys in {'h_ph_head':[(0,(-10,0,0)),(.15,(-5,65,0)),(.35,(-5,40,0)),(.6,(-12,0,0))],'pc_chest':[(0,(38,0,0)),(.3,(0,20,0)),(.6,(-12,0,0))],'pra_right_arm':[(0,(0,0,0)),(.6,(65,0,28))],'pla_left_arm':[(0,(0,0,0)),(.6,(65,0,-28))],'prfa_right_forearm':[(0,(65,0,0)),(.6,(80,0,0))],'plfa_left_forearm':[(0,(65,0,0)),(.6,(80,0,0))],'prl_right_leg':[(0,(10,0,10)),(.6,(15,0,12))],'pll_left_leg':[(0,(-10,0,-10)),(.6,(-12,0,-12))],'prfl_right_foreleg':[(0,(-35,0,0)),(.6,(-20,0,0))],'plfl_left_foreleg':[(0,(-28,0,0)),(.6,(-20,0,0))]}.items():turn['animators'][groups[bone]['uuid']]=animator(groups[bone],keys)
  m['animations'].extend([run,turn])
  save('players/motionrp_'+name+'.bbmodel',m)

GRAY=[(56,60,69,255),(82,87,98,255),(111,116,128,255),(141,146,158,255),(30,33,40,255),(172,177,187,255),(96,102,114,255),(198,202,211,255)]

def bowl_z(r):
 if r<20:return -12*max(0,1-(r/20)**2)**.7
 if r<23:return 1.3*math.sin((r-20)/3*math.pi)
 return 0

def mesh_group(m,name,ids):return m.group(name,ids,origin=(0,0,0))

def deform_clips(m,wall,cracks=None):
 flat=animation('flat',1,{});flat['override']=True;flat['loop']='loop';dent=animation('dent',.4,{});dent['override']=True
 flat['animators'][wall['uuid']]=animator(wall,[(0,(1,1,.001)),(1,(1,1,.001))],'scale')
 dent['animators'][wall['uuid']]=animator(wall,[(0,(1,1,.001)),(.12,(1,1,.35)),(.3,(1,1,1)),(.4,(1,1,1))],'scale')
 if cracks:
  flat['animators'][cracks['uuid']]=animator(cracks,[(0,(.001,.001,.001)),(1,(.001,.001,.001))],'scale')
  dent['animators'][cracks['uuid']]=animator(cracks,[(0,(.001,.001,.001)),(.12,(.4,.4,.5)),(.28,(1,1,1)),(.4,(1,1,1))],'scale')
 m.data['animations']=[flat,dent];material_texture(m,GRAY)

def stones():
 m=mesh_model('motionrp_broly_dented_wall',GRAY);vertices={};faces=[];sides=64;rings=[]
 # Continuous bowl and a broad rectangular cliff surround. Local -Z is into the wall.
 for row in range(15):
  radii=[]
  for k in range(sides):
   q=k*math.tau/sides;outer=min(36/max(.0001,abs(math.cos(q))),28/max(.0001,abs(math.sin(q))))
   r=row*2.5 if row<=8 else 20+(outer-20)*(row-8)/6
   radii.append(r);noise=(.22*math.sin(k*1.7+row*2.3)+.13*math.sin(k*.7-row))*(r/20 if r<20 else 1)
   z=bowl_z(r)+noise;vertices[f'{row}_{k}']=[r*math.cos(q),r*math.sin(q),z]
  rings.append(radii)
 for row in range(14):
  for k in range(sides):
   ids=[f'{row}_{k}',f'{row+1}_{k}',f'{row+1}_{(k+1)%sides}',f'{row}_{(k+1)%sides}'];color=(0 if row<2 else 1 if row<5 else 2)
   if row==0:ids=[ids[0],ids[1],ids[2]]
   faces.append((ids,color))
 # The slab has substantial sides and back; the front is not filled by cubes.
 for k in range(sides):
  p=vertices[f'14_{k}'];vertices['back_'+str(k)]=[p[0],p[1],-20]
 vertices['back_center']=[0,0,-20]
 for k in range(sides):
  j=(k+1)%sides;faces.extend([([f'14_{k}','back_'+str(k),'back_'+str(j),f'14_{j}'],1),(['back_center','back_'+str(j),'back_'+str(k)],1)])
 uid=add_mesh(m,'continuous_grey_cliff_and_recessed_bowl',vertices,faces);wall=mesh_group(m,'deformable_rock_wall',[uid]);crack_ids=[]
 for crack in range(19):
  angle=crack*2.399963;points=[]
  for j in range(11):
   r=3+j*2.2;q=angle+.045*math.sin(j*2.1+crack);points.append((r*math.cos(q),r*math.sin(q),bowl_z(r)+.32))
  crack_ids.append(stroke(m,'radial_fracture_'+str(crack),points,.20,4))
  r0=10+(crack%3)*3;points=[]
  for j in range(5):
   r=r0+j*1.6;q=angle+j*.075;points.append((r*math.cos(q),r*math.sin(q),bowl_z(r)+.34))
  crack_ids.append(stroke(m,'branch_fracture_'+str(crack),points,.12,4))
 cracks=mesh_group(m,'growing_fractures',crack_ids);deform_clips(m,wall,cracks);m.save()
 # A separate unbroken cliff keeps the pre-impact face plain, with no dark target rings.
 flat=mesh_model('motionrp_broly_wall',GRAY);fv={}
 for key,p in vertices.items():
  fv[key]=list(p)
  if not key.startswith('back'):fv[key][2]=.16*math.sin(p[0]*.37+p[1]*.21)
 ff=[(ids,1 if any(k.startswith('back')for k in ids)else 2)for ids,color in faces]
 uid=add_mesh(flat,'unbroken_grey_cliff_face',fv,ff);g=mesh_group(flat,'stone_wall',[uid]);a=animation('flat',1,{});a['loop']='loop';a['override']=True;a['animators'][g['uuid']]=animator(g,[(0,(1,1,1)),(1,(1,1,1))],'scale');flat.data['animations']=[a];material_texture(flat,GRAY);flat.save()
 # Continuous raised circular lip, matching the wall's 8.5-block impact opening.
 m=mesh_model('motionrp_broly_crater',GRAY);v={};f=[]
 for row,(radius,z)in enumerate([(19.4,.05),(20.4,1.55),(21.7,.8),(23,0)]):
  for k in range(sides):
   q=k*math.tau/sides;r=radius+.25*math.sin(k*1.4);v[f'{row}_{k}']=[r*math.cos(q),r*math.sin(q),z+.12*math.sin(k*2.3)]
 for row in range(3):
  for k in range(sides):f.append(([f'{row}_{k}',f'{row+1}_{k}',f'{row+1}_{(k+1)%sides}',f'{row}_{(k+1)%sides}'],3 if row==0 else 2))
 uid=add_mesh(m,'raised_circular_crater_lip',v,f);g=mesh_group(m,'impact_ring',[uid]);deform_clips(m,g);m.save()
 m=mesh_model('motionrp_broly_debris',GRAY)
 from generate_battle_visuals import oval
 uid=oval(m,'grey_rock_fragment',(0,0,0),(5.5,4.7,4.3),2,rows=4,sides=7);mesh_group(m,'fragment',[uid]);material_texture(m,GRAY);m.save()

def stroke(m,name,points,width,color):
 # Fractures lie in the XY face, following its varying depth.
 v={};f=[]
 for j,p in enumerate(points):
  before=points[max(0,j-1)];after=points[min(len(points)-1,j+1)];dx,dy=after[0]-before[0],after[1]-before[1];length=max(.001,math.hypot(dx,dy));nx,ny=-dy/length*width/2,dx/length*width/2
  v['a'+str(j)]=[p[0]-nx,p[1]-ny,p[2]];v['b'+str(j)]=[p[0]+nx,p[1]+ny,p[2]]
  if j:f.append((['a'+str(j-1),'a'+str(j),'b'+str(j),'b'+str(j-1)],color))
 return add_mesh(m,name,v,f)

if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes()
 if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise SystemExit('Expected pinned Steve rig')
 actors(json.loads(raw));stones()
