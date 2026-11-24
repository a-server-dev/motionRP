"""Three reference-based football shots, at 1.2x the source effect duration.
Original editable meshes; pinned BetterModel Steve rig, no video pixels embedded.
"""
import sys,json,math,hashlib
from pathlib import Path
sys.dont_write_bytecode=True
from generate_summons import player,animator,save,ident,RIG_SHA,animation
from generate_energy import mesh_model,add_mesh
from generate_battle_visuals import oval
COLORS=[(8,5,22,255),(22,26,94,255),(27,80,255,255),(66,195,255,255),(235,252,255,255),(152,231,255,255),(205,8,35,255),(255,69,81,255)]
SHOTS={'death_sword':(129,74),'eternal_blizzard':(158,110),'death_spear':(132,72)}
def actor(rig,name,end,kick):
 m=player(rig,name,end/20,{})
 def track(b,keys,channel='rotation'):
  g=next(g for g in m['groups']if g['name']==b)
  keys=[(min(end,t+(2 if name in {'death_sword','eternal_blizzard'}else 4))if t else 0,v)for t,v in keys]
  # A terminal pose can coincide after the renderer lead correction.
  keys=list({t:v for t,v in keys}.items())
  m['animations'][0]['animators'][g['uuid']]=animator(g,[(t/20,v)for t,v in keys],channel)
 track('player_root',[(0,(0,0,0)),(end,(0,0,0))],'position')
 if name=='death_sword':
  # The ball stays on the ground; a single front kick approaches it.
  thigh=[(0,(0,0,0)),(40,(0,0,0)),(52,(-20,0,0)),(62,(10,0,0)),(68,(45,0,0)),(72,(50,0,0)),(78,(50,0,0)),(82,(65,0,0)),(88,(20,0,0)),(end,(20,0,0))]
  knee=[(0,(0,0,0)),(40,(0,0,0)),(52,(-55,0,0)),(62,(-75,0,0)),(68,(-55,0,0)),(72,(0,0,0)),(78,(0,0,0)),(82,(0,0,0)),(88,(-20,0,0)),(end,(-20,0,0))]
  chest=[(0,(0,0,0)),(18,(-8,0,0)),(44,(-8,0,0)),(60,(35,-15,0)),(70,(42,-25,-4)),(kick,(-12,15,0)),(80,(-4,-4,0)),(88,(0,-12,3)),(94,(0,-12,3)),(96,(-10,-4,1)),(100,(-16,8,-2)),(104,(-20,15,-3)),(106,(-18,15,-3)),(110,(-20,15,0)),(114,(-20,15,7)),(end,(-20,15,7))]
  root=[(0,(0,0,0)),(end,(0,0,0))]
 elif name=='eternal_blizzard':
  # One front/back exchange, then a wide, one-hand-down crouch for a full second.
  thigh=[(0,(0,0,0)),(6,(-12,0,5)),(12,(20,0,5)),(18,(55,0,6)),(22,(85,0,6)),(24,(65,25,10)),(28,(-45,-30,15)),(32,(-20,-15,8)),(40,(60,0,55)),(60,(60,0,55)),(64,(25,0,15)),(74,(45,0,25)),(84,(-40,0,20)),(96,(-85,0,10)),(104,(-85,0,10)),(106,(-65,0,8)),(108,(115,0,5)),(kick,(115,0,5)),(kick+4,(135,0,8)),(end,(0,0,0))]
  knee=[(0,(0,0,0)),(6,(-15,0,0)),(12,(-20,0,0)),(18,(-40,0,0)),(22,(-20,0,0)),(24,(-65,0,0)),(28,(-65,0,0)),(32,(-25,0,0)),(40,(-105,0,0)),(60,(-105,0,0)),(64,(-40,0,0)),(74,(-90,0,0)),(96,(-115,0,0)),(104,(-115,0,0)),(106,(-80,0,0)),(108,(0,0,0)),(kick+8,(0,0,0)),(end,(0,0,0))]
  chest=[(0,(0,0,0)),(22,(-5,0,0)),(24,(-5,20,-8)),(28,(-5,-20,8)),(32,(-10,-10,0)),(40,(-40,0,0)),(60,(-40,0,0)),(64,(-15,0,0)),(84,(-20,-10,-8)),(96,(-18,-25,-10)),(104,(-18,-25,-10)),(108,(15,25,5)),(kick,(20,30,8)),(kick+8,(-25,10,8)),(end,(0,0,0))]
  root=[(0,(0,0,0)),(22,(0,0,0)),(28,(-10,0,-12)),(32,(0,0,0)),(40,(0,0,0)),(60,(0,0,0)),(64,(-15,0,-12)),(74,(-25,90,-25)),(84,(-35,180,-30)),(90,(-30,270,-25)),(96,(-20,300,-15)),(100,(-15,330,-8)),(106,(-12,345,0)),(kick,(0,360,0)),(118,(15,360,5)),(end,(0,360,0))]

 else:
  thigh=[(0,(0,0,0)),(12,(80,0,15)),(30,(60,0,20)),(44,(65,0,20)),(kick-10,(-55,0,12)),(kick-4,(-55,0,12)),(kick,(125,0,5)),(kick+7,(140,0,5)),(end,(0,0,0))]
  knee=[(0,(0,0,0)),(20,(-80,0,0)),(44,(-70,0,0)),(kick-10,(-105,0,0)),(kick-4,(-105,0,0)),(kick,(0,0,0)),(kick+7,(0,0,0)),(end,(0,0,0))]
  chest=[(0,(-10,0,0)),(26,(-30,-20,-10)),(kick-10,(-25,-35,-15)),(kick,(35,30,15)),(kick+10,(25,25,12)),(end,(0,0,0))]
  root=[(0,(0,0,0)),(30,(0,0,-12)),(kick-4,(0,0,-12)),(kick,(0,0,8)),(end,(0,0,0))]
 track('player_root',root);track('pc_chest',chest);track('pw_waist',[(0,(0,0,0)),(24,(0,25,0)),(28,(0,-25,0)),(32,(0,-10,0)),(40,(-30,0,0)),(60,(-30,0,0)),(64,(0,0,0)),(90,(-8,-20,0)),(104,(-8,-20,0)),(108,(15,20,0)),(114,(-10,5,0)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(0,0,0)),(kick-10,(-12,-18,0)),(kick,(10,18,0)),(end,(0,0,0))]if name!='death_sword'else[(0,(0,0,0)),(kick,(-5,0,0)),(80,(-8,-10,0)),(88,(0,-18,0)),(94,(0,-18,0)),(96,(-16,-12,-2)),(100,(-35,2,-4)),(104,(-48,14,-4)),(106,(-52,18,-4)),(110,(-45,20,0)),(114,(-45,25,8)),(end,(-45,25,8))])
 track('prl_right_leg',thigh);track('prfl_right_foreleg',knee)
 track('pll_left_leg',[(0,(0,0,0)),(78,(0,0,0)),(92,(5,0,0)),(end,(5,0,0))]if name=='death_sword'else[(0,(0,0,0)),(22,(-15,0,-8)),(24,(-40,-25,-10)),(28,(85,30,-15)),(32,(25,15,-8)),(40,(60,0,-55)),(60,(60,0,-55)),(64,(45,0,-12)),(84,(65,0,-15)),(96,(45,0,-15)),(kick,(-45,0,-12)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(0,0,0)),(32,(-12,0,-8)),(kick-8,(35,0,-15)),(kick,(-30,0,-12)),(end,(0,0,0))])
 track('plfl_left_foreleg',[(0,(0,0,0)),(78,(0,0,0)),(92,(-5,0,0)),(end,(-5,0,0))]if name=='death_sword'else[(0,(0,0,0)),(22,(-25,0,0)),(24,(-65,0,0)),(28,(-55,0,0)),(32,(-40,0,0)),(40,(-105,0,0)),(60,(-105,0,0)),(64,(-60,0,0)),(84,(-45,0,0)),(96,(-40,0,0)),(kick,(-75,0,0)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(0,0,0)),(40,(-65,0,0)),(kick,(-75,0,0)),(end,(0,0,0))])
 for right in [True,False]:
  sign=1 if right else -1
  if name=='death_sword':
   if right:arm=[(0,(-15,0,8)),(16,(-20,0,8)),(44,(-20,0,8)),(60,(-40,0,5)),(kick,(-35,0,8)),(82,(25,-30,30)),(84,(90,-30,40)),(86,(150,-30,35)),(88,(185,-30,15)),(94,(185,-30,15)),(96,(185,-35,5)),(98,(175,-40,-20)),(100,(150,-45,-30)),(104,(100,-45,-25)),(106,(80,-40,-20)),(110,(70,-30,-15)),(114,(70,-15,-5)),(end,(70,-15,-5))]

   else:arm=[(0,(-15,0,-8)),(44,(-20,0,-8)),(60,(-40,0,-5)),(82,(-15,-15,-20)),(94,(-15,-15,-20)),(100,(-35,-10,-25)),(106,(-55,0,-20)),(110,(-70,-10,-40)),(114,(-85,-15,-50)),(end,(-85,-15,-50))]

  elif name=='eternal_blizzard':
   arm=[(0,(0,0,sign*8)),(22,(-25 if right else 25,0,sign*35)),(28,(-40 if right else -20,0,sign*70)),(40,(70 if right else -100,15 if right else -15,5 if right else -35)),(60,(70 if right else -100,15 if right else -15,5 if right else -35)),(64,(-40 if right else -20,0,sign*70)),(84,(-60 if right else 30,0,sign*65)),(96,(-70 if right else 110,0,sign*45)),(104,(-70 if right else 110,0,sign*45)),(108,(45 if right else -35,-10 if right else 10,sign*35)),(114,(65 if right else -55,0,sign*25)),(end,(0,0,0))]
  else:arm=[(0,(0,0,sign*8)),(30,(-20,0,sign*70)),(kick-12,(-50 if right else 80,0,sign*65)),(kick,(75 if right else -25,0,sign*45)),(kick+10,(30 if right else -20,0,sign*35)),(end,(0,0,0))]
  track('pra_right_arm'if right else'pla_left_arm',arm)
  forearm=[(0,(0,0,0)),(30,(30,0,0)),(kick,(45,0,0)),(end,(0,0,0))]
  if name=='eternal_blizzard':forearm=[(0,(0,0,0)),(28,(30,0,0)),(40,(0,0,0)),(60,(0,0,0)),(84,(30,0,0)),(kick,(45,0,0)),(end,(0,0,0))]
  if name=='death_sword'and right:forearm=[(0,(0,0,0)),(30,(55,0,0)),(kick,(15,0,0)),(82,(10,0,0)),(88,(0,0,0)),(104,(0,0,0)),(end,(0,0,0))]
  if name=='death_sword'and not right:forearm=[(0,(0,0,0)),(30,(30,0,0)),(kick,(45,0,0)),(106,(0,0,0)),(end,(0,0,0))]
  track('prfa_right_forearm'if right else'plfa_left_forearm',forearm)
 track('h_ph_head',[(0,(20,0,0)),(kick-12,(15,-15,0)),(kick,(10,10,0)),(94,(10,20,0)),(110,(15,-10,0)),(114,(20,-15,0)),(end,(20,-15,0))]if name=='death_sword'else[(0,(-20,0,0)),(12,(-25,0,0)),(22,(30,0,0)),(32,(35,0,0)),(40,(60,0,0)),(60,(60,0,0)),(84,(15,0,0)),(96,(-15,20,0)),(104,(-15,20,0)),(kick,(-5,0,0)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(20,0,0)),(kick-12,(15,-15,0)),(kick,(10,10,0)),(end,(0,0,0))])
 if name=='eternal_blizzard':
  g=next(g for g in m['groups']if g['name']=='pw_waist')
  m['animations'][0]['animators'][g['uuid']]['keyframes']+=animator(g,[(0,(0,0,0)),(34/20,(0,0,0)),(42/20,(0,-2,0)),(62/20,(0,-2,0)),(66/20,(0,0,0)),(end/20,(0,0,0))],'position')['keyframes']
 if name=='death_sword':
  # Set the right foot forward as it settles after impact, not at the very end.
  for bone,angle,z in [('prl_right_leg',20,-3),('pll_left_leg',5,3)]:
   g=next(g for g in m['groups']if g['name']==bone)
   fore=next(g for g in m['groups']if g['name']==('prfl_right_foreleg'if bone=='prl_right_leg'else'plfl_left_foreleg'))
   entry=m['animations'][0]['animators'][g['uuid']]
   keys=[(0,(0,0,0)),(80/20,(0,0,0))]
   for t in range(81,95):
    p=(t-80)/14
    bend=20 if bone=='prl_right_leg'else 5*p
    # Ground compensation begins only after the kicking leg returns.
    drop=-(g['origin'][1]-fore['origin'][1])*(1-math.cos(math.radians(bend)))
    keys.append((t/20,(0,drop if bone=='pll_left_leg'or t>=90 else drop*(t-80)/10,z*p)))
   keys.append((end/20,keys[-1][1]))
   entry['keyframes']+=animator(g,keys,'position')['keyframes']

 attachments=[('shot_knife_edge' ,'prfa_right_forearm',[3.75,11.25,-1.875]),('shot_support_boot','plfl_left_foreleg',[-1.875,.65,-1.8]),('shot_upper','pc_chest',[0,22,0]),('shot_trail_hand','plfa_left_forearm',[-5.625,11.25,0])]if name=='death_sword'else[('ice_other_boot','plfl_left_foreleg',[-1.875,.65,-1.8]),('ice_ground_hand','prfa_right_forearm',[5.625,11.25,0]),('ice_upper','pc_chest',[0,22,0])]if name=='eternal_blizzard'else[]
 for label,parent,origin in attachments+[('shot_boot','prfl_right_foreleg',[1.875,.65,-1.8]),('shot_torso','pc_chest',[0,17,0])]:
  g={'name':label,'uuid':ident(name+'/'+label),'origin':origin,'rotation':[0,0,0],'export':True};m['groups'].append(g)
  uid=next(x['uuid']for x in m['groups']if x['name']==parent)
  def add(ns):
   for n in ns:
    if isinstance(n,dict):
     if n['uuid']==uid:n['children'].append({'uuid':g['uuid'],'children':[]});return True
     if add(n['children']):return True
   return False
  assert add(m['outliner'])
 if name=='eternal_blizzard':
  from generate_rasengan import hand_path
  rows=[list(map(float,l.split(',')))for l in hand_path(m,end,'shot_boot').splitlines()]
  g=next(g for g in m['groups']if g['name']=='player_root')
  base=rows[0][2]
  keys=[(0,(0,0,0)),(32/20,(0,0,0))]+[(t/20,(0,(base-rows[t][2])*16,0))for t in range(34,73)]+[(80/20,(0,0,0)),(end/20,(0,0,0))]
  m['animations'][0]['animators'][g['uuid']]['keyframes']+=animator(g,keys,'position')['keyframes']
 save('players/motionrp_'+name+'.bbmodel',m)
 if name=='eternal_blizzard':
  from generate_rasengan import hand_path
  from generate_summons import ROOT
  (ROOT/'eternal_blizzard-boot.csv').write_text(hand_path(m,end,'shot_boot'))
  (ROOT/'eternal_blizzard-other.csv').write_text(hand_path(m,end,'ice_other_boot'))
  (ROOT/'eternal_blizzard-ground.csv').write_text(hand_path(m,end,'ice_ground_hand'))
 if name=='death_sword':
  from generate_rasengan import hand_path
  from generate_summons import ROOT
  (ROOT/'death_sword-knife.csv').write_text(hand_path(m,end,'shot_knife_edge'))
  (ROOT/'death_sword-support.csv').write_text(hand_path(m,end,'shot_support_boot'))
  (ROOT/'death_sword-right.csv').write_text(hand_path(m,end,'shot_boot'))
def finish(m,ids,spin=False):
 g=m.group('shot_effect',ids)
 if spin:
  a=animation('spin',.4,{g['uuid']:animator(g,[(i*.1,(0,0,i*90))for i in range(5)])});a['loop']='loop';m.data['animations']=[a]
 for e in m.data['elements']:e['light_emission']=15
 m.save()
def effects():
 m=mesh_model('motionrp_death_sword_edge',COLORS);ids=[]
 # The ball leads; the black blade-like wake flares backwards, not ahead of it.
 for i in range(7):
  q=i*math.tau/7;c,s=math.cos(q),math.sin(q);length=50+(i%3)*8
  def p(r,w,z):return[r*c-w*s,r*s+w*c,z]
  for outer,width,color in [(True,5,2),(False,3.1,0)]:
   v={'a':p(2,-width,3),'b':p(2,width,3),'c':p(12+(i%2)*4,width,-19),'d':p(22,0,-length),'e':p(9,-width,-31)}
   ids.append(add_mesh(m,f'jagged_wake_{i}_{outer}',v,[(['a','b','c'],color),(['a','c','e'],color),(['c','d','e'],3 if outer else 0)]))
 for i in range(14):
  a=i*math.tau/14;b=(i+1)*math.tau/14
  ids.append(add_mesh(m,'blue_corona_'+str(i),{'a':[5*math.cos(a),5*math.sin(a),1],'b':[5*math.cos(b),5*math.sin(b),1],'tip':[(13+i%3*3)*math.cos((a+b)/2),(13+i%3*3)*math.sin((a+b)/2),-3]},[(['a','b','tip'],2 if i%2 else 3)]))
 finish(m,ids)
 m=mesh_model('motionrp_death_sword_core',COLORS);ids=[oval(m,'black_ball',(0,0,0),(4.3,4.3,4.3),0,rows=10,sides=16)]
 for k in range(4):
  v={};f=[]
  for j in range(33):
   a=j*math.tau/32
   for side in [0,1]:
    r=4.8+side*.45;x=r*math.cos(a);y=r*math.sin(a);tilt=k*math.pi/4;v[f'{j}_{side}']=[x,y*math.cos(tilt),y*math.sin(tilt)]
   if j:f.append(([f'{j-1}_0',f'{j-1}_1',f'{j}_1',f'{j}_0'],2))
  ids.append(add_mesh(m,'blue_core_edge_'+str(k),v,f))
 finish(m,ids)
 m=mesh_model('motionrp_death_spear_drill',COLORS);ids=[]
 for i in range(12):
  a=i*math.tau/12;b=(i+1)*math.tau/12
  ids.append(add_mesh(m,'spear_facet_'+str(i),{'a':[12*math.cos(a),12*math.sin(a),-16],'b':[12*math.cos(b),12*math.sin(b),-16],'tip':[0,0,60]},[(['a','b','tip'],0 if i%2 else 6)]))
 for strand in range(3):
  v={};f=[]
  for j in range(33):
   z=-24+j*2.5;r=14*(1-j/34);a=j*.6+strand*math.tau/3
   for side in [0,1]:v[f'{j}_{side}']=[r*math.cos(a+side*.22),r*math.sin(a+side*.22),z]
   if j:f.append(([f'{j-1}_0',f'{j-1}_1',f'{j}_1',f'{j}_0'],7 if strand==0 else 6))
  ids.append(add_mesh(m,'red_drill_helix_'+str(strand),v,f))
 finish(m,ids,True)
 m=mesh_model('motionrp_blizzard_orb',COLORS);ids=[oval(m,'icy_core',(0,0,0),(8.5,9.5,8.5),5,rows=8,sides=12)]
 for k in range(4):
  v={};f=[]
  for j in range(41):
   a=j*math.tau/40;r=11.5;tilt=k*math.pi/4
   for side in [0,1]:
    x=(r+side*3.2)*math.cos(a);y=(r+side*3.2)*math.sin(a);v[f'{j}_{side}']=[x,y*math.cos(tilt),y*math.sin(tilt)]
   if j:f.append(([f'{j-1}_0',f'{j-1}_1',f'{j}_1',f'{j}_0'],4 if k%2 else 3))
  ids.append(add_mesh(m,'white_ice_ribbon_'+str(k),v,f))
 finish(m,ids,True)
 for wake in [False,True]:
  m=mesh_model('motionrp_blizzard_wake'if wake else'motionrp_blizzard_wind',COLORS);ids=[]
  for strand in range(3):
   for edge in [False,True]:
    v={};f=[]
    for j in range(65):
     p=j/64;y=p*64;r=(14+18*p)if not wake else(4+22*p);a=p*math.tau*1.6+strand*math.tau/3
     width=.045 if edge else .15
     for side in [0,1]:
      q=a+side*width;x=r*math.cos(q);z=r*math.sin(q);v[f'{j}_{side}']=[x,z,-y]if wake else[x,y,z]
     if j and j%12!=0:f.append(([f'{j-1}_0',f'{j-1}_1',f'{j}_1',f'{j}_0'],3 if edge else 4))
    ids.append(add_mesh(m,f'wind_{strand}_{edge}',v,f))
  g=m.group('winter_wind',ids);axis=(lambda i:(0,0,i*90))if wake else(lambda i:(0,i*90,0))
  a=animation('spin',.8,{g['uuid']:animator(g,[(i*.2,axis(i))for i in range(5)])});a['loop']='loop';m.data['animations']=[a]
  for e in m.data['elements']:e['light_emission']=15
  m.save()
 m=mesh_model('motionrp_blizzard_flake',COLORS);ids=[]
 for k in range(6):
  a=k*math.tau/6
  def point(x,y):return[x*math.cos(a)-y*math.sin(a),x*math.sin(a)+y*math.cos(a),0]
  segments=[((0,0),(0,12))]+[((0,y),(s*3,y+3))for y in [5,8]for s in [-1,1]]
  for j,(p,q)in enumerate(segments):
   ids.append(add_mesh(m,f'crystal_{k}_{j}',{'a':point(p[0]-.55,p[1]),'b':point(p[0]+.55,p[1]),'c':point(q[0]+.55,q[1]),'d':point(q[0]-.55,q[1])},[(['a','b','c','d'],4 if j else 5)]))
 finish(m,ids,True)
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes();assert hashlib.sha256(raw).hexdigest()==RIG_SHA
 for name,(end,kick)in SHOTS.items():actor(json.loads(raw),name,end,kick)
 effects()
