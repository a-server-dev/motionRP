"""Detailed Myoo sweep and returning black-dragon absorption."""
import sys
sys.dont_write_bytecode=True
import copy,math,json
from generate_summons import ROOT,save,player,animation,animator
from generate_epic_revision import put_track,palette_model
from generate_epic_refinement import FIRE
from generate_rasengan import hand_path

def myoo_player(rig):
 m=player(rig,'kokujo_tengen_myoo',11,{})
 for name,keys in {
 'pra_right_arm':[(0,(0,0,0)),(2.7,(0,0,0)),(4.4,(140,-45,0)),(5.9,(140,-45,0)),(6.2,(75,-75,0)),(7.2,(75,95,0)),(11,(75,95,0))],
 'pla_left_arm':[(0,(0,0,0)),(2.7,(0,0,0)),(4.4,(35,0,0)),(5.9,(35,0,0)),(6.2,(-10,0,0)),(11,(-10,0,0))],
 'prfa_right_forearm':[(0,(45,0,0)),(2.7,(45,0,0)),(4.4,(15,0,0)),(11,(15,0,0))],
 'plfa_left_forearm':[(0,(45,0,0)),(2.7,(45,0,0)),(4.4,(55,0,0)),(5.9,(55,0,0)),(6.2,(35,0,0)),(11,(35,0,0))],
 'pw_waist':[(0,(0,0,0)),(2.7,(0,0,0)),(4.4,(-3,-10,0)),(5.9,(-3,-10,0)),(6.2,(-8,-8,0)),(7.2,(-8,12,0)),(11,(-8,12,0))]
 }.items():put_track(m,name,keys)
 walk=animation('myoo_walk',.8,{});walk['loop']='loop';stand=animation('myoo_stand',.15,{});groups={g['name']:g for g in m['groups']}
 for side,name in [(1,'prl_right_leg'),(-1,'pll_left_leg')]:
  g=groups[name];walk['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.2,(side*18,0,0)),(.4,(0,0,0)),(.6,(-side*18,0,0)),(.8,(0,0,0))]);stand['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.15,(0,0,0))])
 m['animations'][0]['override']=True;walk['override']=True;stand['override']=True
 m['animations'].extend([walk,stand])
 save('players/motionrp_kokujo_tengen_myoo.bbmodel',m);(ROOT/'kokujo_tengen_myoo-hand-path.csv').write_text(hand_path(m,220))

def dragon_absorption_player():
 from generate_kokuryuha_rest import generate
 generate()

def dragon_cloak():
 a=palette_model('motionrp_black_dragon_cloak',FIRE);c=a.cube
 for side in [-1,1]:
  for plume in range(9):
   theta=math.radians(-20+plume*12)
   for j in range(6):
    r=8+j*3;w=5.8-j*.8;c('black_flame_mantle_'+str(side)+'_'+str(plume)+'_'+str(j),(side*(5+math.cos(theta)*r),17+math.sin(theta)*r,5+plume*.5+j*.6),(w,6-j*.65,2.7),0 if j<4 else 2,(0,0,side*(plume-4)*5))
 for k in range(8):
  for j in range(4):c('upward_wisp_'+str(k)+'_'+str(j),((k-3.5)*4.2,30+j*3.2,6+k%3),(3-j*.55,4.8,2),0 if j<3 else 3)
 g=a.joint('mantle',(0,18,6));clip=animation('flame',.6,{});clip['loop']='loop';clip['animators'][g['uuid']]=animator(g,[(0,(0,0,-3)),(.2,(0,0,3)),(.4,(0,0,-2)),(.6,(0,0,-3))]);a.m.data['animations']=[clip];a.m.save()

def generate(rig):myoo_player(rig);dragon_absorption_player();dragon_cloak()
