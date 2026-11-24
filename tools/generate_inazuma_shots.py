"""Reference-based football shots with a shared two-foot brush and rightward landing turn.
Original editable meshes; pinned BetterModel Steve rig, no video pixels embedded.
"""
import sys,json,math,hashlib,copy
from pathlib import Path
sys.dont_write_bytecode=True
from generate_summons import player,animator,save,ident,RIG_SHA,animation
from generate_energy import mesh_model,add_mesh
from generate_battle_visuals import oval
COLORS=[(8,5,22,255),(22,26,94,255),(27,80,255,255),(66,195,255,255),(235,252,255,255),(152,231,255,255),(205,8,35,255),(255,69,81,255)]
SHOTS={'death_sword':(129,74),'eternal_blizzard':(278,238),'death_spear':(132,108)}
def ice_tick(reference):
 # Remove the 32-tick intro; pre-turn motion runs at 1.5x, subsequent motion at 1x.
 return int(math.floor((max(0,reference-32)/1.5 if reference<=136 else 104/1.5+reference-136)+.5))

def sword_tick(reference):
 return int(math.floor((reference/2 if reference<=78 else reference-39)+.5))

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
  # Scoop into a free arc, then strike high with a grounded, straight-legged front kick.
  thigh=[(0,(0,0,0)),(8,(0,0,0)),(16,(45,0,0)),(22,(45,0,0)),(30,(65,0,0)),(40,(40,0,0)),(52,(-20,0,0)),(64,(-60,0,0)),(68,(-60,0,0)),(72,(110,0,0)),(78,(110,0,0)),(82,(80,0,0)),(88,(65,0,0)),(end,(65,0,0))]
  knee=[(0,(0,0,0)),(22,(0,0,0)),(30,(-30,0,0)),(40,(-65,0,0)),(52,(-95,0,0)),(64,(-105,0,0)),(68,(-105,0,0)),(72,(0,0,0)),(78,(0,0,0)),(82,(-40,0,0)),(88,(-120,0,0)),(end,(-120,0,0))]
  chest=[(0,(0,0,0)),(18,(-8,0,0)),(44,(-8,0,0)),(60,(35,-15,0)),(70,(42,-25,-4)),(kick,(-12,15,0)),(80,(-4,-4,0)),(88,(0,-12,3)),(94,(0,-12,3)),(96,(-10,-4,1)),(100,(-16,8,-2)),(104,(-20,15,-3)),(106,(-18,15,-3)),(110,(-20,15,0)),(114,(-15,15,5)),(end,(-15,15,5))]
  root=[(0,(0,0,0)),(end,(0,0,0))]
 elif name=='eternal_blizzard':
  # 13.9-second anime reference: focus, scoop/scissor, planted freeze, rise/turn, loaded aerial kick.
  thigh=[(0,(0,0,0)),(32,(0,0,0)),(36,(-12,0,5)),(40,(30,0,5)),(44,(85,0,6)),(50,(85,20,10)),(54,(45,0,0)),(64,(45,0,0)),(70,(45,0,0)),(86,(60,0,55)),(134,(60,0,55)),(148,(30,0,30)),(158,(35,0,20)),(168,(45,0,25)),(180,(-40,0,20)),(194,(-65,0,15)),(208,(-85,0,10)),(222,(-85,0,10)),(226,(-65,0,8)),(232,(80,0,10)),(kick,(125,0,8)),(248,(140,0,12)),(260,(60,0,8)),(end,(0,0,0))]
  knee=[(0,(0,0,0)),(32,(0,0,0)),(36,(-15,0,0)),(40,(-25,0,0)),(44,(-20,0,0)),(50,(-20,0,0)),(54,(-45,0,0)),(64,(-45,0,0)),(70,(-45,0,0)),(86,(-105,0,0)),(134,(-105,0,0)),(148,(-65,0,0)),(158,(-55,0,0)),(168,(-90,0,0)),(194,(-105,0,0)),(208,(-115,0,0)),(222,(-115,0,0)),(226,(-80,0,0)),(232,(-20,0,0)),(kick,(0,0,0)),(248,(0,0,0)),(260,(-60,0,0)),(end,(0,0,0))]
  chest=[(0,(-4,0,0)),(32,(-4,0,0)),(44,(-5,0,0)),(54,(-5,20,-8)),(64,(-5,-20,8)),(70,(-10,-10,0)),(86,(-40,0,0)),(134,(-40,0,0)),(148,(-18,-15,-5)),(158,(-10,-30,-10)),(180,(-15,-20,-8)),(194,(-18,-25,-10)),(208,(-18,-35,-12)),(222,(-18,-35,-12)),(226,(-10,-10,0)),(232,(15,25,5)),(kick,(20,30,8)),(248,(-25,10,8)),(end,(0,0,0))]
  root=[(0,(0,0,0)),(44,(0,0,0)),(54,(0,0,0)),(58,(0,0,0)),(62,(0,0,0)),(66,(0,0,0)),(70,(0,0,0)),(86,(0,0,0)),(134,(0,0,0)),(148,(-10,150,-8)),(158,(-15,270,-12)),(168,(-25,450,-25)),(180,(-30,630,-25)),(194,(-20,720,-15)),(208,(-15,720,-8)),(222,(-15,720,-8)),(226,(-12,720,0)),(kick,(0,720,0)),(250,(15,720,5)),(end,(0,720,0))]

 else:
  thigh=[(0,(0,0,0)),(12,(80,0,15)),(30,(60,0,20)),(44,(65,0,20)),(kick-10,(-55,0,12)),(kick-4,(-55,0,12)),(kick,(125,0,5)),(kick+7,(140,0,5)),(end,(0,0,0))]
  knee=[(0,(0,0,0)),(20,(-80,0,0)),(44,(-70,0,0)),(kick-10,(-105,0,0)),(kick-4,(-105,0,0)),(kick,(0,0,0)),(kick+7,(0,0,0)),(end,(0,0,0))]
  chest=[(0,(-10,0,0)),(26,(-30,-20,-10)),(kick-10,(-25,-35,-15)),(kick,(35,30,15)),(kick+10,(25,25,12)),(end,(0,0,0))]
  root=[(0,(0,0,0)),(30,(0,0,-12)),(kick-4,(0,0,-12)),(kick,(0,0,8)),(end,(0,0,0))]
 track('player_root',root);track('pc_chest',chest);track('pw_waist',[(0,(0,0,0)),(54,(0,25,0)),(64,(0,-25,0)),(70,(0,-10,0)),(86,(-30,0,0)),(134,(-30,0,0)),(148,(-12,-20,0)),(158,(0,-15,0)),(194,(-8,-20,0)),(222,(-8,-20,0)),(232,(15,20,0)),(244,(-10,5,0)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(0,0,0)),(kick-10,(-12,-18,0)),(kick,(10,18,0)),(end,(0,0,0))]if name!='death_sword'else[(0,(0,0,0)),(kick,(-5,0,0)),(80,(-8,-10,0)),(88,(0,-18,0)),(94,(0,-18,0)),(96,(-16,-12,-2)),(100,(-35,2,-4)),(104,(-48,14,-4)),(106,(-52,18,-4)),(110,(-45,20,0)),(114,(-45,25,8)),(end,(-45,25,8))])
 track('prl_right_leg',thigh);track('prfl_right_foreleg',knee)
 track('pll_left_leg',[(0,(0,0,0)),(78,(0,0,0)),(88,(65,0,0)),(end,(65,0,0))]if name=='death_sword'else[(0,(0,0,0)),(44,(-15,0,-8)),(54,(-40,-25,-10)),(64,(85,30,-15)),(70,(25,15,-8)),(86,(60,0,-55)),(134,(60,0,-55)),(148,(30,0,-30)),(158,(45,0,-12)),(180,(65,0,-15)),(208,(45,0,-15)),(222,(45,0,-15)),(kick,(-45,0,-12)),(248,(-55,0,-12)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(0,0,0)),(32,(-12,0,-8)),(kick-8,(35,0,-15)),(kick,(-30,0,-12)),(end,(0,0,0))])
 track('plfl_left_foreleg',[(0,(0,0,0)),(78,(0,0,0)),(88,(-120,0,0)),(end,(-120,0,0))]if name=='death_sword'else[(0,(0,0,0)),(44,(-25,0,0)),(54,(-65,0,0)),(64,(-55,0,0)),(70,(-40,0,0)),(86,(-105,0,0)),(134,(-105,0,0)),(148,(-65,0,0)),(158,(-60,0,0)),(180,(-45,0,0)),(208,(-40,0,0)),(222,(-40,0,0)),(kick,(-75,0,0)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(0,0,0)),(40,(-65,0,0)),(kick,(-75,0,0)),(end,(0,0,0))])
 for right in [True,False]:
  sign=1 if right else -1
  if name=='death_sword':
   if right:arm=[(0,(-15,0,8)),(16,(-20,0,8)),(44,(-20,0,8)),(60,(-40,0,5)),(kick,(-35,0,8)),(82,(25,-30,30)),(84,(90,-30,40)),(86,(150,-30,35)),(88,(185,-30,15)),(94,(185,-30,15)),(96,(185,-35,5)),(98,(175,-40,-20)),(100,(150,-45,-30)),(104,(100,-45,-25)),(106,(80,-40,-20)),(110,(70,-30,-15)),(114,(105,-25,20)),(end,(105,-25,20))]

   else:arm=[(0,(-15,0,-8)),(44,(-20,0,-8)),(60,(-40,0,-5)),(82,(-15,-15,-20)),(94,(-15,-15,-20)),(100,(-35,-10,-25)),(106,(-55,0,-20)),(110,(-65,-15,-40)),(114,(-55,-25,-45)),(end,(-55,-25,-45))]

  elif name=='eternal_blizzard':
   arm=[(0,(15,0,sign*10)),(32,(15,0,sign*10)),(44,(-25 if right else 25,0,sign*35)),(64,(-40 if right else -20,0,sign*70)),(86,(70 if right else -65,15 if right else -25,5 if right else -35)),(134,(70 if right else -65,15 if right else -25,5 if right else -35)),(148,(60 if right else -65,-10 if right else 10,sign*35)),(158,(75 if right else -25,0,sign*55)),(180,(-40 if right else 65,0,sign*65)),(194,(100 if right else 65,-35 if right else 25,-30 if right else 25)),(208,(120 if right else 70,-40 if right else 30,-40 if right else 30)),(222,(120 if right else 70,-40 if right else 30,-40 if right else 30)),(232,(65 if right else -45,0,sign*35)),(kick,(75 if right else -55,0,sign*25)),(250,(80 if right else -50,0,sign*25)),(end,(0,0,0))]
  else:arm=[(0,(0,0,sign*8)),(30,(-20,0,sign*70)),(kick-12,(-50 if right else 80,0,sign*65)),(kick,(75 if right else -25,0,sign*45)),(kick+10,(30 if right else -20,0,sign*35)),(end,(0,0,0))]
  track('pra_right_arm'if right else'pla_left_arm',arm)
  forearm=[(0,(0,0,0)),(30,(30,0,0)),(kick,(45,0,0)),(end,(0,0,0))]
  if name=='eternal_blizzard':forearm=[(0,(65,0,0)),(32,(65,0,0)),(44,(25,0,0)),(64,(30,0,0)),(86,(0,0,0)),(134,(0,0,0)),(158,(35,0,0)),(180,(40,0,0)),(194,(85 if right else 65,0,0)),(222,(85 if right else 65,0,0)),(kick,(55 if right else 25,0,0)),(end,(0,0,0))]
  if name=='death_sword'and right:forearm=[(0,(0,0,0)),(30,(55,0,0)),(kick,(15,0,0)),(82,(10,0,0)),(88,(0,0,0)),(104,(0,0,0)),(110,(0,0,0)),(114,(10,0,0)),(end,(10,0,0))]
  if name=='death_sword'and not right:forearm=[(0,(0,0,0)),(30,(30,0,0)),(kick,(45,0,0)),(106,(0,0,0)),(114,(10,0,0)),(end,(10,0,0))]
  track('prfa_right_forearm'if right else'plfa_left_forearm',forearm)
 track('h_ph_head',[(0,(20,0,0)),(kick-12,(15,-15,0)),(kick,(10,10,0)),(94,(10,20,0)),(110,(15,-10,0)),(114,(45,-10,0)),(end,(45,-10,0))]if name=='death_sword'else[(0,(0,-8,0)),(32,(0,-8,0)),(44,(30,0,0)),(70,(35,0,0)),(86,(60,0,0)),(134,(60,0,0)),(148,(25,15,0)),(180,(10,0,0)),(208,(-15,20,0)),(222,(-15,20,0)),(kick,(-5,0,0)),(end,(0,0,0))]if name=='eternal_blizzard'else[(0,(20,0,0)),(kick-12,(15,-15,0)),(kick,(10,10,0)),(end,(0,0,0))])
 if name=='eternal_blizzard':
  g=next(g for g in m['groups']if g['name']=='pw_waist')
  m['animations'][0]['animators'][g['uuid']]['keyframes']+=animator(g,[(0,(0,0,0)),(72/20,(0,0,0)),(88/20,(0,-2,0)),(136/20,(0,-2,0)),(150/20,(0,0,0)),(end/20,(0,0,0))],'position')['keyframes']
 if name=='death_sword':
  # Spread the planted feet after impact, then bend both knees and lower the actual pelvis.
  for bone,z in [('prl_right_leg',-5),('pll_left_leg',5)]:
   g=next(g for g in m['groups']if g['name']==bone)
   entry=m['animations'][0]['animators'][g['uuid']]
   keys=[(0,(0,0,0)),(80/20,(0,0,0)),(94/20,(0,0,z)),(end/20,(0,0,z))]
   entry['keyframes']+=animator(g,keys,'position')['keyframes']

 attachments=[('shot_hip','phip_hip',[0,11.25,0]),('shot_knife_edge' ,'prfa_right_forearm',[3.75,11.25,-1.875]),('shot_support_boot','plfl_left_foreleg',[-1.875,.65,-1.8]),('shot_upper','pc_chest',[0,22,0]),('shot_trail_hand','plfa_left_forearm',[-5.625,11.25,0])]if name=='death_sword'else[('ice_other_boot','plfl_left_foreleg',[-1.875,.65,-1.8]),('ice_ground_hand','prfa_right_forearm',[5.625,11.25,0]),('ice_upper','pc_chest',[0,22,0]),('ice_trail_hand','plfa_left_forearm',[-5.625,11.25,0]),('ice_left_shoulder','pla_left_arm',[-5,22,0])]if name=='eternal_blizzard'else[]
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
 if name in {'eternal_blizzard','death_sword'}:
  from generate_rasengan import hand_path
  right=[list(map(float,l.split(',')))for l in hand_path(m,end,'shot_boot').splitlines()]
  left=[list(map(float,l.split(',')))for l in hand_path(m,end,'ice_other_boot'if name=='eternal_blizzard'else'shot_support_boot').splitlines()]
  base=right[0][2]
  first,last=(72,148)if name=='eternal_blizzard'else(80,end-1)
  # Match both soles to the floor while the hips lower, without moving the real pilot.
  root=next(g for g in m['groups']if g['name']=='player_root')
  root_keys=[(0,(0,0,0))]+[(first/20,(0,0,0))]
  leg_keys=[[(0,(0,0,0)),(first/20,(0,0,0))]for _ in range(2)]
  for t in range(first+1,last+1):
   # The sword's striking foot returns while the other remains planted.
   drop=(left[t][2]-base)if name=='death_sword'and t<94 else min(right[t][2],left[t][2])-base
   root_keys.append((t/20,(0,-drop*16,0)))
   for i,(bone,path)in enumerate(zip(['prl_right_leg','pll_left_leg'],[right,left])):
    z=0 if name=='eternal_blizzard'else(-5 if i==0 else 5)*min(1,max(0,(t-80)/14))
    lift=(base+drop-path[t][2])*16 if name=='death_sword'and i==1 and t<94 else 0
    leg_keys[i].append((t/20,(0,lift,z)))
  if name=='eternal_blizzard':root_keys.extend([(162/20,(0,0,0)),(end/20,(0,0,0))])
  else:root_keys.append((end/20,root_keys[-1][1]))
  m['animations'][0]['animators'][root['uuid']]['keyframes']=[k for k in m['animations'][0]['animators'][root['uuid']]['keyframes']if k['channel']!='position']+animator(root,root_keys,'position')['keyframes']
  if name=='death_sword':
   for bone,keys in zip(['prl_right_leg','pll_left_leg'],leg_keys):
    g=next(g for g in m['groups']if g['name']==bone);keys.append((end/20,keys[-1][1]))
    m['animations'][0]['animators'][g['uuid']]['keyframes']=[k for k in m['animations'][0]['animators'][g['uuid']]['keyframes']if k['channel']!='position']+animator(g,keys,'position')['keyframes']
 if name in {'eternal_blizzard','death_sword'}:
  # Compress only the requested phase; the renderer lead and post-kick timing remain intact.
  retime=ice_tick if name=='eternal_blizzard'else sword_tick
  new_end=retime(end)
  for entry in m['animations'][0]['animators'].values():
   unique={}
   for k in entry['keyframes']:
    old=k['time']*20
    t=(max(0,min(new_end,retime(old+2)-2))if name=='death_sword'else min(new_end,retime(max(0,old-2))+2))if old else 0
    k['time']=t/20;unique[(k['channel'],t)]=k
   entry['keyframes']=list(unique.values())
  end=new_end;m['animations'][0]['length']=end/20
 if name=='death_sword':
  # End on the completed hand chop; remove the extra raised-arm flourish after it.
  for entry in m['animations'][0]['animators'].values():
   keys=entry['keyframes'];cut=sword_tick(110)+2
   rot=[k for k in keys if k['channel']=='rotation'and k['time']<=cut/20]
   if rot:
    held=copy.deepcopy(max(rot,key=lambda k:k['time']));held['time']=end/20
    entry['keyframes']=[k for k in keys if k['channel']!='rotation']+rot+[held]
 if name=='eternal_blizzard':
  from generate_rasengan import hand_path
  def smooth(x,a,b):
   q=max(0,min(1,(x-a)/(b-a)));return q*q*(3-2*q)
  # The right foot approaches from the front and the left from behind, brushing opposite sides.
  for right in [True,False]:
   leg='prl_right_leg'if right else'pll_left_leg';knee='prfl_right_foreleg'if right else'plfl_left_foreleg'
   hips=[list(map(float,l.split(',')))for l in hand_path(m,end,leg).splitlines()]
   upper=5.625/16;lower=math.hypot(4.975,1.8)/16;rest=math.atan2(1.8,4.975)
   thigh_keys=[];knee_keys=[]
   for t in range(8,17):
    p=(t-8)/8;clock=t+2;pilot=.85*smooth(clock,2,12)*(1-smooth(clock,24,39))
    dx=(.34 if right else -.34)-hips[t][0]
    target_f=.32+(.24*(1-2*p)if right else -.24*(1-2*p))
    df=target_f-(hips[t][1]-.05);dy=1.35-pilot-(hips[t][2]-.03)
    reach=math.sqrt(dx*dx+df*df+dy*dy);assert abs(upper-lower)<reach<upper+lower,(right,t,reach)
    a=math.atan2(math.hypot(dx,df),-dy)+math.acos((upper*upper+reach*reach-lower*lower)/(2*upper*reach))
    bend=math.acos((reach*reach-upper*upper-lower*lower)/(2*upper*lower))
    thigh_keys.append((t/20,(math.degrees(a),-math.degrees(math.atan2(dx,df)),0)));knee_keys.append((t/20,(-math.degrees(bend+rest),0,0)))
   thigh_keys.extend([(20/20,(30 if right else 70,0,10 if right else -15)),(28/20,(45 if right else 65,0,35 if right else -40)),(38/20,(60,0,55 if right else -55))])
   knee_keys.extend([(20/20,(-75 if right else -100,0,0)),(28/20,(-100 if right else -105,0,0)),(38/20,(-105,0,0))])
   for bone,keys in [(leg,thigh_keys),(knee,knee_keys)]:
    g=next(g for g in m['groups']if g['name']==bone);entry=m['animations'][0]['animators'][g['uuid']]
    entry['keyframes']=[k for k in entry['keyframes']if k['channel']!='rotation'or not 8/20<=k['time']<=38/20]+animator(g,keys,'rotation')['keyframes']
  # Rightward body turn follows the two-foot rub and finishes in the planted crouch.
  root=next(g for g in m['groups']if g['name']=='player_root');entry=m['animations'][0]['animators'][root['uuid']]
  kept=[]
  for k in entry['keyframes']:
   if k['channel']=='rotation':
    if k['time']<69/20:continue
    k['data_points'][0]['y']=str(float(k['data_points'][0]['y'])-360)
   kept.append(k)
  turn=[(0,(0,0,0)),(18/20,(0,0,0)),(22/20,(0,-90,0)),(26/20,(0,-180,0)),(30/20,(0,-270,0)),(34/20,(0,-360,0)),(39/20,(0,-360,0))]
  entry['keyframes']=kept+animator(root,turn,'rotation')['keyframes']
  # Floor clearance remains reversible throughout the descending turn.
  r=[list(map(float,l.split(',')))for l in hand_path(m,end,'shot_boot').splitlines()];l=[list(map(float,v.split(',')))for v in hand_path(m,end,'ice_other_boot').splitlines()]
  pos=[k for k in entry['keyframes']if k['channel']=='position'];fixed=[]
  for t in range(18,39):
   a=max((k for k in pos if k['time']<=t/20),key=lambda k:k['time'],default=pos[0]);b=min((k for k in pos if k['time']>=t/20),key=lambda k:k['time'],default=pos[-1]);q=(t/20-a['time'])/(b['time']-a['time'])if b['time']!=a['time']else 0
   x,y,z=[float(a['data_points'][0][axis])*(1-q)+float(b['data_points'][0][axis])*q for axis in'xyz'];clock=t+2;pilot=.85*smooth(clock,2,12)*(1-smooth(clock,24,39));lift=max(0,.070625-min(r[t][2],l[t][2])-pilot)
   fixed.append((t/20,(x,y+lift*16,z)))
  entry['keyframes']=[k for k in entry['keyframes']if k['channel']!='position'or not 18/20<=k['time']<=38/20]+animator(root,fixed,'position')['keyframes']

 save('players/motionrp_'+name+'.bbmodel',m)
 if name=='eternal_blizzard':
  from generate_rasengan import hand_path
  from generate_summons import ROOT
  (ROOT/'eternal_blizzard-boot.csv').write_text(hand_path(m,end,'shot_boot'))
  (ROOT/'eternal_blizzard-other.csv').write_text(hand_path(m,end,'ice_other_boot'))
  (ROOT/'eternal_blizzard-ground.csv').write_text(hand_path(m,end,'ice_ground_hand'))
  (ROOT/'eternal_blizzard-trail.csv').write_text(hand_path(m,end,'ice_trail_hand'))
  (ROOT/'eternal_blizzard-shoulder.csv').write_text(hand_path(m,end,'ice_left_shoulder'))
 if name=='death_sword':
  from generate_rasengan import hand_path
  from generate_summons import ROOT
  (ROOT/'death_sword-knife.csv').write_text(hand_path(m,end,'shot_knife_edge'))
  (ROOT/'death_sword-support.csv').write_text(hand_path(m,end,'shot_support_boot'))
  (ROOT/'death_sword-right.csv').write_text(hand_path(m,end,'shot_boot'))
  (ROOT/'death_sword-hip.csv').write_text(hand_path(m,end,'phip_hip'))
 return m

def spear_from_scissor(blizzard):
 from generate_rasengan import hand_path
 from generate_summons import ROOT
 m=copy.deepcopy(blizzard);m['name']=m['model_identifier']='motionrp_death_spear';clip=m['animations'][0];clip['name']='death_spear';clip['length']=132/20
 for entry in clip['animators'].values():
  unique={}
  for k in entry['keyframes']:
   t=max(0,min(132,int(math.floor((k['time']*20+2)*132/211+.5))-2));k['time']=t/20;unique[(k['channel'],t)]=k
  entry['keyframes']=list(unique.values())
 save('players/motionrp_death_spear.bbmodel',m)
 for suffix,bone in [('boot','shot_boot'),('other','ice_other_boot'),('ground','ice_ground_hand')]:
  (ROOT/('death_spear-'+suffix+'.csv')).write_text(hand_path(m,132,bone))

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
 built={name:actor(json.loads(raw),name,end,kick)for name,(end,kick)in SHOTS.items()if name!='death_spear'}
 spear_from_scissor(built['eternal_blizzard'])
 effects()
