#!/usr/bin/env python3
"""Editable first-pass World/Road Roller, Black Dragon, Myoo, gravity moon and Fusion."""
import sys
sys.dont_write_bytecode=True
import json,math,hashlib
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player,save,animation,animator
from generate_perfect_susanoo import Armor
from generate_rasengan import hand_path
GOLD=[(224,173,37,255),(255,236,139,255),(78,69,43,255),(36,154,130,255),(197,151,40,255),(251,207,69,255),(49,48,44,255),(255,255,218,255)]
DARK=[(10,5,16,255),(50,16,73,255),(104,22,154,255),(197,86,255,255),(231,30,68,255),(240,222,245,255),(30,12,42,255),(165,53,235,255)]
SAMURAI=[(28,24,27,255),(70,59,55,255),(117,92,72,255),(170,133,104,255),(124,21,27,255),(224,197,124,255),(179,184,184,255),(242,234,208,255)]
ROCK=[(67,59,53,255),(105,89,73,255),(138,120,95,255),(74,75,77,255),(40,36,37,255),(166,153,131,255),(107,109,111,255),(196,180,152,255)]
def solid(name,colors):
 a=Armor(name,False);a.m=Model(name,False,colors);return a

def humanoid(name,colors,stand=False):
 a=solid(name,colors);c=a.cube
 for side in [-1,1]:
  for row in range(6):
   c(f'leg_plate_{side}_{row}',[side*5,3+row*2.2,0],[6,2.5,7],0 if row%2 else 4)
   c(f'greave_trim_{side}_{row}',[side*5,4+row*2.2,-3.8],[6.5,.5,.7],1)
  c(f'boot_{side}',[side*5,1,-2],[7,3,11],2)
  a.joint(f'leg_{side}',(side*5,16,0))
 for row in range(6):
  c(f'torso_lamella_{row}',[0,17+row*2.4,0],[16+row*.8,2.8,10],0)
  for side in [-1,1]:
   c(f'chest_inlay_{side}_{row}',[side*5,18+row*2.4,-5.4],[4,.7,.7],1)
   if not stand:c(f'rope_lacing_{side}_{row}',[side*7,17+row*2.4,-6],[1.2,1,1.3],4)
 c('abdomen_core',[0,19,-5.8],[3,5,1],3 if stand else 4)
 a.joint('chest',(0,16,0))
 c('face',[0,37,-.5],[10,10,9],0 if stand else 3)
 c('helmet_top',[0,42,0],[13,3,11],0)
 for side in [-1,1]:
  c(f'eye_{side}',[side*2.5,38,-5.3],[2.4,.8,.7],3 if stand else 5)
  c(f'cheek_{side}',[side*4,35.5,-4.5],[2,5,2],1 if stand else 1)
  c(f'helmet_ear_{side}',[side*6,38,0],[2,8,7],2 if stand else 0)
  if stand:
   for j in range(4):c(f'head_tube_{side}_{j}',[side*5,40+j,-3+j],[2,2,2],1)
  else:
   for j in range(5):c(f'helmet_neck_plate_{side}_{j}',[side*(5+j*.3),36-j*1.6,2],[3,2,10],0)
   c(f'helmet_horn_{side}',[side*5,45,0],[2,9,2],5,[0,0,-side*22.5])
 c('mouth',[0,33.5,-5.2],[5,1,.7],2 if stand else 0)
 if stand:
  c('forehead_heart_a',[ -.9,41,-5],[2.2,2.2,1.2],3,[0,0,-45]);c('forehead_heart_b',[.9,41,-5],[2.2,2.2,1.2],3,[0,0,45])
 else:
  c('nose',[0,36,-6],[2,3,3],3)
  for side in [-1,1]:c(f'moustache_{side}',[side*2,34.2,-6.1],[4,.9,1],0,[0,0,side*22.5])
 a.joint('head',(0,31,0))
 arms=[]
 for side in [-1,1]:
  c(f'pauldron_{side}',[side*13,29,0],[10,8,11],0)
  for j in range(4):
   c(f'arm_scale_{side}_{j}',[side*13,17+j*3,-.5],[7.5,3.5,8],0 if j%2 else 4)
   c(f'bracer_trim_{side}_{j}',[side*13,18+j*3,-5],[8,.8,.8],1)
  c(f'fist_{side}',[side*13,14,-1],[8,7,8],0 if stand else 3)
  for j in range(4):c(f'knuckle_{side}_{j}',[side*13-3+j*2,14,-5.2],[1.4,2.8,1],1)
  if not stand and side==1:
   c('giant_sword_hilt',[13,14,-7],[2,2,12],1)
   c('giant_sword_guard',[13,14,-13],[10,2,2],5)
   for j in range(16):
    c(f'giant_sword_blade_{j}',[13,14,-16-j*3],[3.6-j*.15,1,3.4],6)
    c(f'giant_sword_edge_{j}',[14.6-j*.075,14,-16-j*3],[.5,1.2,3.4],7)
  arms.append(a.joint(f'arm_{side}',(side*10,30,0)))
 if stand:
  clip=animation('barrage',.4,{});clip['loop']='loop'
  for side,g in zip([-1,1],arms):clip['animators'][g['uuid']]=animator(g,[(0,(-65,0,side*10)),(.1,(-120,side*20,side*5)),(.2,(-65,0,side*10)),(.3,(-120,-side*20,side*5)),(.4,(-65,0,side*10))])
 else:
  clip=animation('slam',7,{})
  for side,g in zip([-1,1],arms):clip['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1,(side*10,0,side*10)),(2.6,(105,-10,-25) if side==1 else (10,0,-15)),(3.2,(105,-10,-25) if side==1 else (10,0,-15)),(3.5,(-65,20,30) if side==1 else (5,0,-15)),(6,(-65,20,30) if side==1 else (5,0,-15)),(7,(0,0,0))])
 a.m.data['animations']=[clip];a.m.save()

def roller():
 a=solid('motionrp_road_roller',GOLD);c=a.cube
 c('chassis',[0,14,0],[42,9,64],0);c('top_deck',[0,20,0],[44,3,66],5)
 for end in [-1,1]:
  for j in range(20):
   t=j*math.tau/20;c(f'drum_{end}_{j}',[0,9+8*math.cos(t),end*23+8*math.sin(t)],[46,3,3],6)
  for side in [-1,1]:
   c(f'drum_hub_{end}_{side}',[side*24,9,end*23],[2,6,6],2)
   c(f'wheel_fork_{end}_{side}',[side*24,14,end*19],[3,12,4],0)
 c('engine_cover',[0,26,-10],[30,11,25],0)
 for j in range(9):c(f'radiator_grille_{j}',[-12+j*3,25,-23],[1,6,1],6)
 c('driver_seat',[0,25,13],[13,4,13],6);c('seat_back',[0,30,19],[13,10,3],6)
 c('steering_column',[0,27,2],[2,10,2],2)
 for side in [-1,1]:
  c(f'cab_pillar_{side}',[side*17,33,9],[2,24,2],0)
  for j in range(6):c(f'hood_bolt_{side}_{j}',[side*15,32,-19+j*4],[1,1,1],1)
  c(f'headlight_{side}',[side*15,25,-24],[6,4,2],7)
 c('cab_roof',[0,45,9],[39,3,34],5);c('exhaust_stack',[12,35,-8],[3,15,3],2)
 a.joint('roller',(0,0,0));a.m.save()

def dragon():
 for head in [True,False]:
  a=solid('motionrp_black_dragon_head' if head else 'motionrp_black_dragon_body',DARK);c=a.cube
  if head:
   c('dragon_skull',[0,0,0],[10,8,12],0);c('elongated_snout',[0,-1,-9],[8,4,9],0);c('jaw',[0,-4,-8],[7,2,10],1)
   for side in [-1,1]:
    c(f'dragon_eye_{side}',[side*4.2,1,-5.5],[1.8,1.1,2],4)
    c(f'eye_brow_{side}',[side*4.5,2,-5],[2,2,5],2,[0,0,-side*22.5])
    for j in range(6):
     c(f'swept_horn_{side}_{j}',[side*(4+j*.7),4+j*.8,2+j],[2-j*.2,2,3],2)
     if j<4:c(f'fang_{side}_{j}',[side*3,-3,-7-j*2],[.6,2.5,.8],5)
   c('red_mouth',[0,-2.8,-8],[6,.6,8],4)
  else:
   c('serpent_segment',[0,0,0],[7,7,9],0);c('violet_belly',[0,-3.5,-.5],[5,.7,8],2)
   for side in [-1,1]:c(f'side_flame_{side}',[side*4,1,0],[2,5,4],1,[0,0,-side*22.5])
   c('dorsal_spine',[0,5,1],[2,5,5],3,[22.5,0,0])
  a.joint('dragon',(0,0,0));a.m.save()

def sphere(name,colors,crag=False):
 m=Model(name,False,colors);verts={};faces={};lat=14;lon=28
 for i in range(lat+1):
  v=math.pi*i/lat
  for j in range(lon):
   a=j*math.tau/lon;r=16*(1+(.045*math.sin(j*4+i*7) if crag else 0));verts[f'{i}_{j}']=[r*math.sin(v)*math.cos(a),r*math.cos(v),r*math.sin(v)*math.sin(a)]
 for i in range(lat):
  for j in range(lon):
   ids=[f'{i}_{j}',f'{i+1}_{j}',f'{i+1}_{(j+1)%lon}',f'{i}_{(j+1)%lon}'];color=(i*3+j*7)%len(colors) if crag else 0
   for tri in [[ids[0],ids[1],ids[2]],[ids[0],ids[2],ids[3]]]:
    for order in [tri,list(reversed(tri))]:faces[str(len(faces))]={'vertices':order,'uv':{tri[0]:[(color+.15)*2,2],tri[1]:[(color+.15)*2,14],tri[2]:[(color+.85)*2,14]},'texture':0}
 uid=name+'_surface';m.data['elements']=[{'name':'surface','uuid':__import__('generate_summons').ident(uid),'type':'mesh','origin':[0,0,0],'vertices':verts,'faces':faces}];m.group('sphere',[m.data['elements'][0]['uuid']]);m.save()
 a=solid('motionrp_gravity_rock',ROCK);c=a.cube
 c('rock_core',[0,0,0],[10,9,11],1);c('rock_stratum',[1,2,-1],[12,2,10],3);c('rock_chip',[-2,4,1],[7,3,6],2,[0,0,22.5]);a.joint('rock',(0,0,0));a.m.save()

def fused():
 colors=[(248,205,145,255),(32,27,32,255),(252,205,50,255),(250,250,244,255),(32,96,160,255),(132,45,30,255),(43,38,43,255),(255,235,138,255)]
 a=solid('motionrp_fusion_warrior',colors);c=a.cube
 for side in [-1,1]:
  c(f'trouser_{side}',[side*2.2,6,0],[4.3,11,5],3);c(f'boot_{side}',[side*2.2,1,-.4],[4.4,3,6],1)
  c(f'vest_{side}',[side*3,19,0],[3,8,6],1);c(f'vest_gold_trim_{side}',[side*2.1,21,-3.2],[2,5,1.5],2)
  c(f'arm_{side}',[side*6.5,17,0],[3.5,10,4],0);c(f'wristband_{side}',[side*6.5,13,0],[3.8,2.5,4.3],4)
 c('torso',[0,17,0],[8,12,5],0);c('blue_sash',[0,11,0],[9,2.5,6],4);c('sash_tail',[5,7,1],[2,8,1],4,[0,0,22.5])
 c('face',[0,27,-.5],[7,7,7],0);c('hair_cap',[0,31,0],[8,3,8],1)
 for j in range(7):c(f'hair_spike_{j}',[-4+j*1.4,33+(j%3),0],[2,5,5],1,[0,0,-22.5 if j<3 else 0 if j==3 else 22.5])
 for side in [-1,1]:c(f'eye_{side}',[side*1.8,27.5,-4.2],[1.5,.5,.5],1)
 a.joint('warrior',(0,0,0));a.m.save()

def actors(rig):
 lengths={'road_roller':220,'kokuryuha':164,'kokujo_tengen_myoo':220,'chibaku_tensei':220}
 for name,end in lengths.items():
  if name=='road_roller':poses={'pra_right_arm':[(0,(0,0,0)),(1,(-160,0,-15)),(3,(-160,0,-15)),(4.5,(-80,0,-10)),(7.5,(-80,0,-10)),(10,(0,0,0))],'pla_left_arm':[(0,(0,0,0)),(1,(-160,0,15)),(3,(-160,0,15)),(4.5,(-80,0,10)),(7.5,(-80,0,10)),(10,(0,0,0))]}
  elif name=='kokuryuha':poses={'pra_right_arm':[(0,(0,0,0)),(.6,(-65,-35,-35)),(2,(-65,-35,-35)),(2.5,(-95,0,-5)),(5,(-95,0,-5)),(7.8,(0,0,0))],'prfa_right_forearm':[(0,(0,0,0)),(.6,(-100,0,0)),(2,(-100,0,0)),(2.5,(0,0,0)),(7.8,(0,0,0))],'pc_chest':[(0,(0,0,0)),(2,(0,-20,0)),(2.5,(10,20,0)),(7.8,(0,0,0))]}
  elif name=='kokujo_tengen_myoo':poses={'pra_right_arm':[(0,(0,0,0)),(2,(-160,0,-10)),(5.6,(-160,0,-10)),(6.1,(-45,20,-35)),(9.8,(-45,20,-35)),(11,(0,0,0))],'pla_left_arm':[(0,(0,0,0)),(2,(-150,0,10)),(5.6,(-150,0,10)),(6.1,(-40,-10,-25)),(9.8,(-40,-10,-25)),(11,(0,0,0))],'pc_chest':[(0,(0,0,0)),(5.6,(-5,-10,0)),(6.1,(15,15,0)),(11,(0,0,0))]}
  else:poses={'pra_right_arm':[(0,(0,0,0)),(.8,(-80,-20,-15)),(7.5,(-80,-20,-15)),(10.5,(0,0,0))],'pla_left_arm':[(0,(0,0,0)),(.8,(-80,20,15)),(7.5,(-80,20,15)),(10.5,(0,0,0))],'prfa_right_forearm':[(0,(0,0,0)),(.8,(-65,0,0)),(7.5,(-65,0,0)),(10.5,(0,0,0))],'plfa_left_forearm':[(0,(0,0,0)),(.8,(-65,0,0)),(7.5,(-65,0,0)),(10.5,(0,0,0))]}
  m=player(rig,name,end/20,poses);(ROOT/(name+'-hand-path.csv')).write_text(hand_path(m,end))

def fusion(rig):
 for role in [-1,1]:
  name='fusion_left' if role==1 else 'fusion_right';waist=-role*35
  m=player(rig,name,8,{'pw_waist':[(0,(0,0,0)),(3.4,(0,0,0)),(4.8,(0,0,waist)),(7.5,(0,0,waist)),(8,(0,0,0))],
   'pra_right_arm':[(0,(0,0,0)),(1,(-90,0,-80*role)),(2,(-90,0,80*role)),(3.4,(-90,0,60*role))],
   'pla_left_arm':[(0,(0,0,0)),(1,(-90,0,-80*role)),(2,(-90,0,80*role)),(3.4,(-90,0,60*role))],
   'prl_right_leg':[(0,(0,0,0)),(1.5,(0,0,0)),(2,(-35,0,0)),(2.5,(0,0,0)),(3,(-35,0,0)),(3.4,(0,0,0))],
   'pll_left_leg':[(0,(0,0,0)),(1.5,(0,0,0)),(2.25,(-35,0,0)),(2.75,(0,0,0)),(3.25,(-35,0,0)),(3.4,(0,0,0))]})
  g={x['name']:x for x in m['groups']};tracks=m['animations'][0]['animators']
  if role<0:
   r,l=g['prl_right_leg']['uuid'],g['pll_left_leg']['uuid'];tracks[r]['keyframes'],tracks[l]['keyframes']=tracks[l]['keyframes'],tracks[r]['keyframes']
  tracks[g['player_root']['uuid']]=animator(g['player_root'],[(0,(0,0,0)),(1.5,(0,0,0)),(3.4,(role*8.8,0,0)),(8,(role*8.8,0,0))],'position')
  # Final mirrored wrists meet exactly at the center between the two participants.
  for side,upper,fore in [(1,'pra_right_arm','prfa_right_forearm'),(-1,'pla_left_arm','plfa_left_forearm')]:
   angle=math.radians(waist);target=[role*8.8,17.2];tx=math.cos(angle)*target[0]+math.sin(angle)*(target[1]-15);ty=-math.sin(angle)*target[0]+math.cos(angle)*(target[1]-15)+15
   dx,dy=tx-side*5,ty-22;dist=math.hypot(dx,dy);l1=5.5;l2=math.hypot(.625,5.625);along=(l1*l1-l2*l2+dist*dist)/(2*dist);h=math.sqrt(max(0,l1*l1-along*along));ex=along*dx/dist-role*h*dy/dist;ey=along*dy/dist+role*h*dx/dist
   a=math.degrees(math.atan2(ey,ex)+math.pi/2);b=math.degrees(math.atan2(dy-ey,dx-ex)-math.atan2(-5.625,side*.625))-a
   a=(a+180)%360-180;b=(b+180)%360-180
   track=tracks[g[upper]['uuid']];track['keyframes'] += animator(g[upper],[(4.8,(0,0,a)),(7.5,(0,0,a)),(8,(0,0,0))])['keyframes']
   tracks[g[fore]['uuid']]=animator(g[fore],[(0,(0,0,0)),(3.4,(0,0,0)),(4.8,(0,0,b)),(7.5,(0,0,b)),(8,(0,0,0))])
  save('players/motionrp_'+name+'.bbmodel',m);(ROOT/(name+'-hand-path.csv')).write_text(hand_path(m,160))

def generate(rig):
 humanoid('motionrp_the_world',GOLD,True);humanoid('motionrp_tengen_myoo',SAMURAI,False);roller();dragon();sphere('motionrp_gravity_core',DARK);sphere('motionrp_rock_moon',ROCK,True);fused();actors(rig);fusion(rig)
 from generate_epic_revision import revise
 revise(rig)
 from generate_epic_refinement import generate as refine
 refine()
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes();assert hashlib.sha256(raw).hexdigest()==RIG_SHA;generate(json.loads(raw))
