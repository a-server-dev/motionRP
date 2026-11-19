"""Original editable chakra avatars and the two-person Valley of the End sequence.
Visual references: official Naruto Shippuden episode pages 01_920 / 01_921.
"""
import sys
sys.dont_write_bytecode=True
import math,copy
from generate_summons import Model,ROOT,save,animation,animator
from generate_epic_revision import palette_model,put_track
from generate_rasengan import hand_path
from generate_summons import player
from generate_energy import mesh_model,add_mesh,ribbon
GOLD=[(255,164,25,255),(255,220,72,255),(255,244,144,255),(70,38,17,255),(255,116,17,255),(255,253,218,255),(255,194,44,255),(255,255,250,255)]
PURPLE=[(68,48,142,255),(117,86,193,255),(161,136,228,255),(35,24,79,255),(96,65,167,255),(218,204,255,255),(128,104,207,255),(248,237,255,255)]
INDRA=[(83,64,157,255),(140,112,205,255),(178,159,233,255),(41,34,92,255),(91,188,243,255),(208,249,255,255),(130,144,234,255),(250,255,255,255)]
CYAN=[(38,139,229,255),(91,215,255,255),(139,233,255,255),(192,249,255,255),(98,174,239,255),(219,254,255,255),(67,190,250,255),(255,255,255,255)]
DARK=[(19,7,38,255),(37,14,60,255),(68,28,91,255),(130,70,162,255),(189,129,218,255),(235,211,255,255),(150,108,192,255),(255,255,255,255)]

def fox_head(a,x,y,z,size,label):
 c=a.cube
 for j in range(6):
  w=size*(1-j*.10);c(label+'_fox_skull_'+str(j),(x,y+(j-2)*size*.12,z),(w,size*.16,size*.65),0 if j<4 else 1)
 for j in range(4):c(label+'_long_muzzle_'+str(j),(x,y-size*.17,z-size*(.35+j*.13)),(size*(.55-j*.10),size*(.27-j*.04),size*.25),1)
 c(label+'_nose',(x,y-size*.13,z-size*.89),(size*.16,size*.12,size*.12),3)
 for side in [-1,1]:
  for j in range(6):c(label+'_ear_'+str(side)+'_'+str(j),(x+side*size*(.34+j*.035),y+size*(.4+j*.14),z+size*.06),(size*(.26-j*.035),size*.19,size*(.25-j*.025)),3 if j>3 else 0)
  c(label+'_eye_outline_'+str(side),(x+side*size*.29,y+size*.05,z-size*.36),(size*.29,size*.11,size*.1),3,(0,0,-side*20))
  c(label+'_eye_'+str(side),(x+side*size*.28,y+size*.07,z-size*.42),(size*.17,size*.05,size*.05),7,(0,0,-side*20))
  for j in range(3):c(label+'_whisker_'+str(side)+'_'+str(j),(x+side*size*.39,y-size*(.10+j*.09),z-size*.36),(size*.28,size*.035,size*.08),3,(0,0,side*(j*8-9)))
 return a.joint(label,(x,y-size*.35,z))

def kurama(asura=False):
 a=palette_model('motionrp_duel_asura_kurama'if asura else'motionrp_duel_kurama',GOLD);c=a.cube;tails=[]
 for side in [-1,1]:
  x=side*9
  for j in range(8):c('fox_leg_'+str(side)+'_'+str(j),(x,5+j*4.8,0),(9+math.sin(j*.5)*2,5.2,11),0 if j%3 else 1)
  c('fox_paw_'+str(side),(x,3,-4),(12,6,18),1)
  for toe in range(3):c('fox_claw_'+str(side)+'_'+str(toe),(x-4+toe*4,2,-13),(2,3,4),3)
  a.joint('fox_leg_'+str(side),(x,44,0))
 for j in range(12):
  y=43+j*3.5;w=21+7*math.sin(j*math.pi/11);c('chakra_torso_'+str(j),(0,y,0),(w,4.1,13),1 if j%4 else 2)
 for j in range(24):
  q=j*math.tau/24;c('belly_seal_'+str(j),(6*math.cos(q),51+6*math.sin(q),-7.2),(1.6,1.6,.8),3)
 for side in [-1,1]:
  for j in range(4):c('chest_marking_'+str(side)+'_'+str(j),(side*8,64+j*3.2,-7.4),(7,1.2,.8),3)
  for j in range(3):c('neck_magatama_'+str(side)+'_'+str(j),(side*(3+j*3),80-j*1.2,-7.8),(2.2,3.5,1.2),3,(0,0,side*22.5))
 a.joint('chakra_body',(0,42,0))
 heads=[]
 if asura:
  for i,(x,y,z,size)in enumerate([(-16,88,3,14),(0,95,-2,18),(16,88,3,14)]):heads.append(fox_head(a,x,y,z,size,'fox_head_'+str(i)))
 else:heads.append(fox_head(a,0,90,0,20,'fox_head_0'))
 for i in range(9):
  angle=(i-4)*math.pi/6
  for j in range(13):
   t=j/12;r=6+52*t;w=13*(1-t)**.65+1.1;xx=math.sin(angle)*r;yy=40+math.cos(angle)*r;zz=12+math.sin(math.pi*t)*13+t*5
   c('nine_tail_'+str(i)+'_'+str(j),(xx,yy,zz),(w,w*.9,6.1),0 if j>9 else 1)
   if j:
    tt=(j-1)/12;rr=6+52*tt;prev=(math.sin(angle)*rr,40+math.cos(angle)*rr,12+math.sin(math.pi*tt)*13+tt*5);cur=(xx,yy,zz);delta=[x-y for x,y in zip(cur,prev)];length=math.sqrt(sum(x*x for x in delta));rotation=(math.degrees(math.asin(delta[2]/length)),0,math.degrees(math.atan2(-delta[0],delta[1])))
    c('continuous_tail_'+str(i)+'_'+str(j),tuple((x+y)/2 for x,y in zip(cur,prev)),(w*.95,length+1,w*.95),0 if j>9 else 1,rotation)
   if j<11:c('tail_seal_line_'+str(i)+'_'+str(j),(xx,yy,zz-w*.53),(w*.17,w*.65,.8),3)
  tails.append(a.joint('nine_tail_'+str(i),(0,40,12)))
 arms=[]
 for row in range(3 if asura else 1):
  for side in [-1,1]:
   x=side*(19+row*1.7);y=81-row*13
   c('chakra_upper_arm_'+str(row)+'_'+str(side),(x,y-10,0),(8,20,9),1)
   c('arm_seal_'+str(row)+'_'+str(side),(x,y-7,-5),(6,1.2,.8),3)
   upper=a.joint('fox_upper_'+str(row)+'_'+str(side),(x,y,0));u=a.m.data['outliner'].pop()
   c('chakra_forearm_'+str(row)+'_'+str(side),(x,y-29,0),(8.6,19,9),1);c('chakra_fist_'+str(row)+'_'+str(side),(x,y-40,-1),(12,10,12),0)
   for f in range(3):c('fox_finger_'+str(row)+'_'+str(side)+'_'+str(f),(x-4+f*4,y-40,-7),(3,8,1.5),3)
   fore=a.joint('fox_fore_'+str(row)+'_'+str(side),(x,y-20,0));u['children'].append(a.m.data['outliner'].pop());a.m.data['outliner'].append(u);arms.append((row,side,upper,fore))
 idle=animation('idle',2,{});idle['loop']='loop'
 for i,g in enumerate(tails):idle['animators'][g['uuid']]=animator(g,[(j*.5,(math.sin(j*math.pi/2+i)*4,math.cos(j*math.pi/2+i)*3,math.sin(j*math.pi/2+i)*5))for j in range(5)])
 charge=animation('charge',8,{});exchange=animation('exchange',4,{})
 for row,side,u,f in arms:
  target=165 if row==0 else (55 if row==1 else 25);bend=-40 if row==0 else 35
  charge['animators'][u['uuid']]=animator(u,[(0,(0,0,0)),(2,(target,0,-side*12)),(7,(target,0,-side*12)),(8,(75,0,-side*8))])
  charge['animators'][f['uuid']]=animator(f,[(0,(10,0,0)),(2,(bend,0,0)),(7,(bend,0,0)),(8,(10,0,0))])
  exchange['animators'][u['uuid']]=animator(u,[(0,(0,0,0)),(.8,(110,side*15,0)),(1.2,(85,-side*15,0)),(2.4,(40,0,0)),(4,(0,0,0))])
 a.m.data['animations']=[idle,charge,exchange];a.m.save()

def susanoo(indra=False):
 a=palette_model('motionrp_duel_indra_susanoo'if indra else'motionrp_duel_susanoo',INDRA if indra else PURPLE);c=a.cube
 for side in [-1,1]:
  x=side*10
  for row in range(10):
   c('leg_armor_'+str(side)+'_'+str(row),(x,5+row*4.4,0),(11+row*.15,4.8,13),1 if row%2 else 4)
   c('leg_chakra_trim_'+str(side)+'_'+str(row),(x,6+row*4.4,-7),(10,.7,1),5)
  c('cloven_foot_'+str(side),(x,2,-4),(13,4,22),1)
  for k in range(3):c('talon_'+str(side)+'_'+str(k),(x-4+k*4,1,-15),(2.5,2,5),5)
  a.joint('susanoo_leg_'+str(side),(x,50,0))
 for j in range(8):
  c('armored_hakama_'+str(j),((j-3.5)*5,46,0),(5.5,19,22),1 if j%2 else 4)
  c('hakama_outline_'+str(j),((j-3.5)*5,40,-11.5),(4.6,.8,1),5)
 a.joint('susanoo_waist',(0,54,0))
 for j in range(11):
  y=56+j*2.7;w=29+8*math.sin(j*math.pi/10);c('violet_breastplate_'+str(j),(0,y,0),(w,3.4,17),1)
  for side in [-1,1]:c('chakra_rib_'+str(side)+'_'+str(j),(side*(7+j*.3),y,-9),(9,.8,.8),5 if indra else 2,(0,0,side*22.5))
 c('chest_core',(0,73,-9.4),(7,15,1.5),7 if indra else 3)
 a.joint('susanoo_chest',(0,54,0))
 c('avian_mask',(0,97,-1),(20,20,17),1)
 for j in range(6):c('beaked_mask_'+str(j),(0,96-j, -10-j*.9),(8-j*.9,2.8,3),2)
 for side in [-1,1]:
  c('mask_eye_'+str(side),(side*5,99,-10),(5,1,1),7,(0,0,-side*15))
  for j in range(8):c('crown_horn_'+str(side)+'_'+str(j),(side*(8+j*.8),104+j*1.8,1+j*.4),(4-j*.35,3,5-j*.35),2)
  for j in range(6):c('cheek_plate_'+str(side)+'_'+str(j),(side*(9+j*.3),97-j*2,1),(4,2.8,16),1)
 for side in [-1,1]:
  c('fierce_mask_brow_'+str(side),(side*5,101,-10.8),(7,2,1.2),3,(0,0,-side*18))
  c('luminous_mask_eye_'+str(side),(side*5,99.4,-11.6),(4.6,1.3,.7),7,(0,0,-side*18))
  c('angular_mask_cheek_'+str(side),(side*8,92,-8),(4,9,9),2,(0,0,side*15))
 for j in range(5):c('crown_ridge_'+str(j),(0,105+j*1.7,-2),(2.3,2.3,5),2)
 a.joint('sasuke_mask',(0,87,0))
 wings=[]
 for side in [-1,1]:
  for j in range(18):
   x=side*(22+j*3.2);y=106-j*2.8
   c('wing_feather_'+str(side)+'_'+str(j),(x,y,17),(5.8,23+j*.6,3.2),1 if j%2 else 4,(0,0,-side*22.5))
   c('wing_light_edge_'+str(side)+'_'+str(j),(x+side*2,y,15),(1.1,25+j*.6,1),5)
  for j in range(16):
   t=j/15;c('wing_spine_'+str(side)+'_'+str(j),(side*(17+61*t),83+28*math.sin(math.pi*t)-5*t,14+4*t),(6.5,6.5,6),2)
  wings.append(a.joint('susanoo_wing_'+str(side),(side*17,83,13)))
 arms=[]
 for side in [-1,1]:
  x=side*26;c('pauldron_'+str(side),(x,81,0),(18,12,19),2)
  for j in range(5):c('pauldron_lamella_'+str(side)+'_'+str(j),(x,83-j*2,0),(19+j,2,20),1 if j%2 else 4)
  c('upper_arm_'+str(side),(x,70,0),(11,22,12),1);u=a.joint('susanoo_arm_'+str(side),(x,81,0));node=a.m.data['outliner'].pop()
  for j in range(6):c('forearm_plate_'+str(side)+'_'+str(j),(x,42+j*3.2,0),(13-j*.6,3.8,14-j*.5),1 if j%2 else 4)
  c('gauntlet_fist_'+str(side),(x,37,-1),(14,11,15),1);f=a.joint('susanoo_forearm_'+str(side),(x,59,0));node['children'].append(a.m.data['outliner'].pop());a.m.data['outliner'].append(node);arms.append((side,u,f))
 for j in range(31):
  q=math.radians(-100+j*200/30);c('indra_bow_limb_'+str(j),(-26,81+46*math.sin(q),-30-14*math.cos(q)),(4,4.4,4),5 if indra else 2)
 for segment,(p,q)in enumerate([((-26,126,-28),(-18,81,0)),((-18,81,0),(-26,36,-28))]):
  for j in range(20):
   t=j/19;c('taut_bowstring_'+str(segment)+'_'+str(j),tuple(x*(1-t)+y*t for x,y in zip(p,q)),(.55,2,.55),7)
 bow=a.joint('indra_bow',(0,0,0));idle=animation('idle',2,{});idle['loop']='loop';idle['animators'][bow['uuid']]=animator(bow,[(0,(.001,.001,.001)),(2,(.001,.001,.001))],'scale')
 for side,g in zip([-1,1],wings):idle['animators'][g['uuid']]=animator(g,[(0,(0,0,side*4)),(1,(0,side*4,side*8)),(2,(0,0,side*4))])
 draw=animation('charge',8,{});draw['animators'][bow['uuid']]=animator(bow,[(0,(.001,.001,.001)),(1.5,(1,1,1)),(8,(1,1,1))],'scale');exchange=animation('exchange',4,{})
 for side,u,f in arms:
  draw['animators'][u['uuid']]=animator(u,[(0,(0,0,0)),(2,(90,90 if side==1 else 0,0)),(7,(90,90 if side==1 else 0,0)),(8,(75,0,0))]);draw['animators'][f['uuid']]=animator(f,[(0,(0,0,0)),(8,(0,0,0))])
  exchange['animators'][u['uuid']]=animator(u,[(0,(0,0,0)),(.8,(90,-side*20,0)),(1.2,(110,side*15,0)),(2.4,(40,0,0)),(4,(0,0,0))])
 a.m.data['animations']=[idle,draw,exchange];a.m.save()

def round_mesh(m,name,radius,palette_mode='bright'):
 vs={};fs=[];lat,lon=14,28
 for j in range(lat+1):
  p=math.pi*j/lat
  for k in range(lon):
   q=k*math.tau/lon;vs[f'{j}_{k}']=[radius*math.sin(p)*math.cos(q),radius*math.cos(p),radius*math.sin(p)*math.sin(q)]
 for j in range(lat):
  for k in range(lon):
   ids=[f'{j}_{k}',f'{j}_{(k+1)%lon}',f'{j+1}_{(k+1)%lon}',f'{j+1}_{k}'];color=(7 if (k+j*3)%9<2 else 2 if (k-j)%5<2 else 1)if palette_mode=='bright'else(0 if (k+j)%7>1 else 4)
   if j==0:ids=[ids[0],ids[2],ids[3]]
   elif j==lat-1:ids=[ids[0],ids[1],ids[2]]
   fs.append((ids,color))
 return add_mesh(m,name,vs,fs)

def shuriken(dark=False):
 m=mesh_model('motionrp_duel_dark_rasenshuriken'if dark else'motionrp_duel_gold_rasenshuriken',DARK if dark else GOLD)
 uid=round_mesh(m,'rotating_chakra_core',12,'dark'if dark else'bright');core=m.group('chakra_core',[uid]);ids=[]
 for side in range(4):
  a=side*math.pi/2
  pts=[(11,-2),(27,0),(14,6),(9,3)]
  vs={str(i):[r*math.cos(a)-t*math.sin(a),0,r*math.sin(a)+t*math.cos(a)]for i,(r,t)in enumerate(pts)}
  ids.append(add_mesh(m,'wind_blade_'+str(side),vs,[(['0','1','2','3'],7),(['3','2','1','0'],7)]))
 blades=m.group('four_wind_blades',ids);spin=animation('spin',.4,{});spin['loop']='loop'
 for g in [core,blades]:spin['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.2,(0,180,0)),(.4,(0,360,0))])
 for strand in range(2):
  pts=[]
  for j in range(49):
   q=j*math.tau/48;p=math.pi/2+.5*math.sin(3*q+strand*2);pts.append((12.3*math.sin(p)*math.cos(q),12.3*math.cos(p),12.3*math.sin(p)*math.sin(q)))
  uid=ribbon(m,'swirling_current_'+str(strand),pts,.3,7);g=m.group('current_'+str(strand),[uid]);spin['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.4,(0,-360,0))])
 m.data['animations']=[spin];m.save()

def arrow():
 a=palette_model('motionrp_duel_indra_arrow',CYAN);c=a.cube
 for j in range(24):c('lightning_shaft_'+str(j),(0,0,-j*2.5),(2.2,2.2,3),7)
 for axis in range(2):
  for j in range(10):
   z=-43-j*2;w=18-j*1.7;c('arrowhead_'+str(axis)+'_'+str(j),(0,0,z),(w,1.3,2.4)if axis==0 else(1.3,w,2.4),7 if j%2 else 3)
 for strand in range(4):
  for j in range(23):
   q=j*.75+strand*math.pi/2;c('lightning_filament_'+str(strand)+'_'+str(j),(math.cos(q)*3.8,math.sin(q)*3.8,8-j*3),(.8,.8,3.5),1)
 g=a.joint('indra_lightning_arrow',(0,0,0));clip=animation('surge',.4,{});clip['loop']='loop';clip['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.4,(0,0,360))]);a.m.data['animations']=[clip];a.m.save()

def clash_orb():
 m=mesh_model('motionrp_duel_clash_orb',CYAN);uid=round_mesh(m,'radiant_collision',16);g=m.group('collision_sphere',[uid]);a=animation('surge',1,{});a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1,(90,180,90))]);m.data['animations']=[a];m.save()
 m=mesh_model('motionrp_duel_chakra_spark',GOLD);uid=round_mesh(m,'gathered_bijuu_chakra',5);m.group('chakra_spark',[uid]);m.save()

def actors(rig):
 for naruto in [True,False]:
  name='final_battle_naruto'if naruto else'final_battle_sasuke';m=player(rig,name,44,{})
  for side,upper,fore in [(1,'pra_right_arm','prfa_right_forearm'),(-1,'pla_left_arm','plfa_left_forearm')]:
   ready=(165,0,-side*12)if naruto else(90,90 if side==1 else 0,0)
   put_track(m,upper,[(0,(0,0,0)),(2,(0,0,0)),(7,(0,0,0)),(8,(110,side*15,0)),(9,(85,-side*15,0)),(12,(0,0,0)),(20,(0,0,0)),(22,ready),(27.8,ready),(28.3,(75,0,0)),(36,(20,0,0)),(40,(0,0,0)),(44,(0,0,0))])
   put_track(m,fore,[(0,(0,0,0)),(20,(0,0,0)),(22,(-40 if naruto else 0,0,0)),(27.8,(-40 if naruto else 0,0,0)),(28.3,(10,0,0)),(44,(0,0,0))])
  save('players/motionrp_'+name+'.bbmodel',m)

def generate(rig):
 kurama();kurama(True);susanoo();susanoo(True);shuriken();shuriken(True);arrow();clash_orb();actors(rig)
if __name__=='__main__':
 import json,hashlib
 from pathlib import Path
 from generate_summons import RIG_SHA
 raw=Path(sys.argv[1]).read_bytes()
 if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise SystemExit('Expected the pinned BetterModel Steve rig')
 generate(json.loads(raw))
