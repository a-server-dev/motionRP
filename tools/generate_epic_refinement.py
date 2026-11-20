"""Black flame silhouette and the heavy rope-bound armored giant."""
import sys
sys.dont_write_bytecode=True
import math
from generate_summons import Model,animation,animator
from generate_perfect_susanoo import Armor
BROWN=[(38,28,25,255),(69,48,40,255),(92,65,53,255),(125,91,72,255),(19,17,18,255),(145,25,43,255),(225,227,222,255),(139,143,149,255)]
FIRE=[(8,5,12,255),(19,9,28,255),(43,15,61,255),(90,27,115,255),(125,49,151,255),(240,231,244,255),(29,13,40,255),(178,80,195,255)]
def armor(name,palette):
 a=Armor(name,False);a.m=Model(name,False,palette);return a

def flame_dragon():
 for head in [True,False]:
  a=armor('motionrp_black_dragon_head'if head else'motionrp_black_dragon_body',FIRE);c=a.cube
  if head:
   for j in range(8):
    z=5-j*2.5;w=[8,11,13,13,11,9,7,4][j];h=[7,9,10,9,7,5,4,2][j]
    c('flame_silhouette_'+str(j),(0,0,z),(w,h,3.2),0)
   # Two small eye slits are the only facial details.
   for side in [-1,1]:
    c('ember_eye_'+str(side),(side*3,1,-9),(1.8,.55,1.5),5)
    for j in range(5):c('swept_horn_'+str(side)+'_'+str(j),(side*(5+j*.45),3+j*.9,1+j*1.5),(2.6-j*.4,2.6-j*.4,3.5),0)
   for k in range(9):
    q=k*math.tau/9
    for j in range(4):
     r=4+j*.95;c('ragged_flame_crest_'+str(k)+'_'+str(j),(math.cos(q)*r,math.sin(q)*r,4+j*1.8),(3.2-j*.6,3.2-j*.6,4.2),1 if j%3 else 2)
  else:
   c('black_fire_core',(0,0,0),(8,7,10),0)
   for k in range(7):
    q=k*math.tau/7
    for j in range(4):
     r=3+j*.65;c('tapered_fire_tongue_'+str(k)+'_'+str(j),(math.cos(q)*r,math.sin(q)*r,j*2),(2.8-j*.55,2.8-j*.55,4),[0,1,2,1][j])
  g=a.joint('black_flame',(0,0,0));clip=animation('flame',.4,{});clip['loop']='loop'
  clip['animators'][g['uuid']]=animator(g,[(0,(0,0,-8)),(.1,(0,0,7)),(.2,(0,0,-3)),(.3,(0,0,10)),(.4,(0,0,-8))]);a.m.data['animations']=[clip];a.m.save()

def myoo():
 a=armor('motionrp_tengen_myoo',BROWN);c=a.cube
 # Grounded feet and layered greaves, using the same binned-cube export as Susanoo.
 for side in [-1,1]:
  x=side*10
  c('broad_boot_'+str(side),(x,3,-3),(13,6,20),0)
  for j in range(7):
   c('greave_lamella_'+str(side)+'_'+str(j),(x,8+j*3.4,0),(11+j*.2,3.8,13),1 if j%2 else 2)
   c('greave_lip_'+str(side)+'_'+str(j),(x,9+j*3.4,-6.8),(11.5,.65,1.3),3)
  c('knee_guard_'+str(side),(x,33,-2),(15,10,16),1)
  for j in range(4):c('thigh_plate_'+str(side)+'_'+str(j),(x,39+j*3.2,0),(14,4,15),2 if j%2 else 1)
  a.joint('armored_leg_'+str(side),(x,53,0))
 for side in [-1,1]:
  for j in range(6):
   c('waist_tasset_'+str(side)+'_'+str(j),(side*(11+j*.55),53-j*2.8,1),(15,3.3,16),1 if j%2 else 2)
 for x in [-15,-7.5,0,7.5,15]:c('hakama_fold_'+str(x),(x,43,2),(7.2,22,15),4)
 a.joint('tassets_and_hakama',(0,54,0))
 # Curved, massive dark breastplate.
 for j in range(11):
  y=59+j*2.5;w=32+8*math.sin(j*math.pi/10);depth=17+4*math.sin(j*math.pi/10)
  c('breastplate_stratum_'+str(j),(0,y,0),(w,3.2,depth),1 if j%3 else 2)
  c('breastplate_bevel_'+str(j),(0,y,-depth/2-.4),(w*.76,1,.9),2)
 for side in [-1,1]:
  for j in range(6):c('flank_lacing_'+str(side)+'_'+str(j),(side*17,61+j*3,-7),(1.8,1.3,3),7)
 # Thick black ropes encircle the waist and curl behind the shoulders.
 for row in range(3):
  for j in range(28):
   q=j*math.tau/28;c('waist_rope_'+str(row)+'_'+str(j),(21*math.cos(q),55+row*1.2,11.5*math.sin(q)),(3.1,2.7,3.1),4 if j%2 else 0)
 for side in [-1,1]:
  for j in range(25):
   q=-1.5+j*.18;c('hanging_black_rope_'+str(side)+'_'+str(j),(side*(23+8*math.cos(q)),70+17*math.sin(q),10),(3.2,3.2,3.4),4)
 # White knotted waist tie and its two hanging paper/cloth ends.
 for side in [-1,1]:
  c('white_sash_knot_'+str(side),(side*3,53,-12),(5,3.8,4),6,(0,0,side*22.5))
  for j in range(7):c('white_sash_tail_'+str(side)+'_'+str(j),(side*(4+j*.8),50-j*2,-13-.15*j),(3.8,2.7,1.2),6 if j%3 else 7,(0,0,-side*22.5))
 for row in range(9):
  y=64+row*2;w=19+8*math.sin(math.pi*row/8)
  c('overlapping_sternum_'+str(row),(0,y,-11.1),(w,2.6,1.8),2 if row%3 else 1)
  for side in [-1,1]:c('breastplate_rivet_'+str(side)+'_'+str(row),(side*w*.44,y,-12.2),(1,.9,.7),7)
 for side in [-1,1]:
  for j in range(9):
   c('pauldron_edge_stud_'+str(side)+'_'+str(j),(side*(21+j*2.4),85,-14),(1.2,1.4,1.1),7)
   c('neck_lacing_'+str(side)+'_'+str(j),(side*11,84+j*1.8,-9.4),(1.3,1.3,2),0)
 chest=a.joint('heavy_breastplate',(0,56,0))
 # Concealed helmet, hanging crimson face cloth, swept heavy horns.
 c('helmet_void',(0,98,-.5),(18,19,17),4)
 for row in range(6):
  w=21-row*.55;c('closed_helmet_shell_'+str(row),(0,97+row*2,0),(w,2.8,18-row*.5),1)
 for x in [-7.5,-5,-2.5,0,2.5,5,7.5]:c('closed_visor_slit_'+str(x),(x,100,-10),(1.1,12,.9),7 if x in [-7.5,7.5]else 0)
 for side in [-1,1]:
  for j in range(7):
   q=j*math.pi/12;c('swept_helmet_horn_'+str(side)+'_'+str(j),(side*(11+3*math.sin(q)),95+j*2,3+3*math.cos(q)),(4.5-j*.3,3.3,5-j*.35),2)
  for j in range(6):c('helmet_neck_lamella_'+str(side)+'_'+str(j),(side*10,93-j*2.1,2),(5,2.8,18),1)
 for x in [-6,-3,0,3,6]:
  for j in range(4):c('crimson_face_veil_'+str(x)+'_'+str(j),(x+(j%2)*.4,93-j*2,-10),(3.2,2.7,1),5)
 # Rounded crown, oval visor rim and individually modelled hanging cloth folds.
 for j in range(12):
  y=94+j*1.65;f=math.sqrt(max(.04,1-((y-101.5)/13.0)**2));c('curved_helmet_crown_'+str(j),(0,y,1),(24*f,1.9,22*f),1 if j%3 else 2)
 for j in range(28):
  q=j*math.tau/28;c('oval_visor_frame_'+str(j),(11*math.cos(q),100+12*math.sin(q),-11.4),(2.1,2.2,2.4),2 if j%4 else 3)
 for j in range(13):
  x=-9+j*1.5;h=13*math.sqrt(max(.1,1-(x/11)**2));c('metal_visor_grille_'+str(j),(x,101,-12.8),(.65,h,1),7 if j%3==0 else 0)
 for side in [-1,1]:
  for j in range(14):
   q=math.radians(-100+j*180/13);c('curved_helmet_horn_'+str(side)+'_'+str(j),(side*(12+5*math.cos(q)),103+11*math.sin(q),3),(3.2,3.4,5),2 if j%3 else 3)
 for j in range(9):
  for k in range(5):c('ragged_crimson_fold_'+str(j)+'_'+str(k),(-8+j*2+.3*math.sin(k+j),91-k*2.1-(j%3)*.35,-12.4-k*.15),(2.6-k*.25,2.8,1.3),5,(0,0,(j-4)*2))
 helmet=a.joint('closed_helmet_and_veil',(0,88,0))
 # Broad rounded shoulder plates with concentric armored ridges.
 shoulders=[]
 for side in [-1,1]:
  for band in range(9):
   x=side*(21+band*2.4)
   for j in range(12):
    q=-.15+j*math.pi/11;c('rounded_pauldron_'+str(side)+'_'+str(band)+'_'+str(j),(x,81+13*math.sin(q),12*math.cos(q)),(3.2,4,4),2 if band%2 else 1)
   c('pauldron_front_ridge_'+str(side)+'_'+str(band),(x,82,-12.8),(1,11,2),3)
  shoulders.append(a.joint('massive_pauldron_'+str(side),(side*26,81,0)))
 arms=[]
 for side in [-1,1]:
  x=side*29
  c('upper_arm_'+str(side),(x,70,0),(13,23,13),0)
  for j in range(5):c('upper_arm_band_'+str(side)+'_'+str(j),(x,62+j*4,-.5),(14,2,14),2)
  upper=a.joint('sword_arm'if side==1 else'guard_arm',(x,81,0));upper_node=a.m.data['outliner'].pop()
  for j in range(6):
   c('gauntlet_plate_'+str(side)+'_'+str(j),(x,43+j*3,0),(14-j*.5,3.5,14-j*.5),1 if j%2 else 2)
   c('gauntlet_ridge_'+str(side)+'_'+str(j),(x,44+j*3,-7+j*.25),(14-j*.5,.7,1.4),3)
  c('armored_fist_'+str(side),(x,39,-1),(15,11,15),0)
  for k in range(4):c('knuckle_'+str(side)+'_'+str(k),(x-5+k*3.4,39,-9),(2.6,7,2),2)
  if side==1:
   c('giant_sword_hilt',(x,43,-3),(3,18,3),4)
   for j in range(8):c('hilt_binding_'+str(j),(x,36+j*2,-3),(3.5,1,3.5),7)
   c('giant_sword_guard',(x,34,-3),(17,2.6,7),7)
   for j in range(17):
    c('giant_sword_blade_'+str(j),(x,31-j*3.1,-3),(7-j*.18,3.5,1.7),6)
    c('blade_edge_'+str(j),(x-3.2+j*.08,31-j*3.1,-3),(.7,3.5,2.1),7)
  fore=a.joint('forearm_'+str(side),(x,59,0));fore_node=a.m.data['outliner'].pop();upper_node['children'].append(fore_node);a.m.data['outliner'].append(upper_node);arms.append((upper,fore))
 roots=a.m.data['outliner'][:];a.m.data['outliner']=roots[:3];torso=a.m.group('battle_torso',[],origin=(0,56,0));a.m.data['outliner'][-1]['children']=roots[3:]
 clip=animation('slam',8.3,{})
 for side,(upper,fore)in zip([-1,1],arms):
  right=side==1
  clip['animators'][upper['uuid']]=animator(upper,[(0,(0,0,0)),(1.7,(140 if right else 35,-45 if right else 0,0)),(3.2,(140 if right else 35,-45 if right else 0,0)),(3.5,(75 if right else -10,-75 if right else 0,0)),(4.5,(75 if right else -10,95 if right else 0,0)),(8.3,(75 if right else -10,95 if right else 0,0))])
  clip['animators'][fore['uuid']]=animator(fore,[(0,(45,0,0)),(1.7,(15 if right else 55,0,0)),(3.2,(15 if right else 55,0,0)),(3.5,(15 if right else 35,0,0)),(8.3,(15 if right else 35,0,0))])
 clip['animators'][torso['uuid']]=animator(torso,[(0,(0,0,0)),(1.7,(-3,-10,0)),(3.2,(-3,-10,0)),(3.5,(-8,-8,0)),(4.5,(-8,12,0)),(8.3,(-8,12,0))]);idle=animation('idle',1,{});idle['loop']='loop'
 for upper,fore in arms:idle['animators'][fore['uuid']]=animator(fore,[(0,(45,0,0)),(1,(45,0,0))])
 walk=animation('walk',.8,{});walk['loop']='loop';stand=animation('stand_legs',.15,{})
 groups={g['name']:g for g in a.m.data['groups']}
 for side in [-1,1]:
  leg=groups['armored_leg_'+str(side)];walk['animators'][leg['uuid']]=animator(leg,[(0,(0,0,0)),(.2,(side*18,0,0)),(.4,(0,0,0)),(.6,(-side*18,0,0)),(.8,(0,0,0))]);stand['animators'][leg['uuid']]=animator(leg,[(0,(0,0,0)),(.15,(0,0,0))])
 for action in [clip,idle,walk,stand]:action['override']=True
 a.m.data['animations']=[clip,idle,walk,stand]
 # The visible waist is the ground anchor, including during summon/fade scaling.
 # Keep empty leg nodes so old walk clips remain compatible, never floating legs.
 removed=set()
 def discard_geometry(nodes):
  for node in nodes:
   if isinstance(node,dict):discard_geometry(node['children']);node['children']=[]
   else:removed.add(node)
 discard_geometry(a.m.data['outliner'][:3])
 a.m.data['elements']=[e for e in a.m.data['elements'] if e['uuid'] not in removed]
 for e in a.m.data['elements']:
  for key in ['from','to','origin']:
   if key in e:e[key][1]-=54
 for g in a.m.data['groups']:g['origin'][1]-=54
 a.m.save()

def generate():flame_dragon();myoo()
