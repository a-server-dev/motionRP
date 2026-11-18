#!/usr/bin/env python3
"""Editable Excalibur and Gae Bolg weapons, release effects and baked player palms."""
import sys
sys.dont_write_bytecode=True
import json,math,hashlib
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player,save,animator,animation
from generate_rasengan import hand_path
GOLD=[(244,185,35,255),(255,242,158,255),(21,45,112,255),(226,241,255,255),(75,103,165,255),(198,122,16,255),(255,217,70,255),(255,255,248,255)]
RED=[(142,7,27,255),(255,85,99,255),(38,5,18,255),(225,20,51,255),(255,43,66,255),(75,6,27,255),(211,58,74,255),(255,209,211,255)]

def weapon(excalibur):
    m=Model('motionrp_excalibur_blade' if excalibur else 'motionrp_gae_bolg_spear',colors=GOLD if excalibur else RED);parts=[]
    if excalibur:
        parts.append(m.cube('royal_blue_grip',[0,-2.5,0],[1.5,8,1.5],2))
        for j in range(6):parts.append(m.cube('gold_grip_lacing_'+str(j),[0,-5.7+j*1.2,0],[1.8,.3,1.8],0))
        parts.append(m.cube('gold_pommel',[0,-7,0],[3.2,2.2,2.2],0));parts.append(m.cube('pommel_blue_gem',[0,-7,-1.3],[1.4,1.4,.4],4))
        for side in [-1,1]:
            for j in range(4):parts.append(m.cube(f'swept_gold_quillon_{side}_{j}',[side*(1.6+j*1.7),2.5-j*.5,0],[2.2,1.8,2],0,[0,0,-side*22.5]))
        parts.append(m.cube('guard_blue_crest',[0,3,-1.4],[3.8,4,.5],2));parts.append(m.cube('guard_gold_crest',[0,3,-1.8],[1.3,2.4,.3],1))
        for j in range(15):
            t=j/14;w=3.5 if t<.82 else max(.2,3.5*(1-(t-.82)/.18));y=5+j*1.95
            parts.append(m.cube('silver_blade_'+str(j),[0,y,0],[w,2.2,.9],3))
            parts.append(m.cube('gold_blade_inscription_'+str(j),[0,y,-.52],[.45,1.65,.1],0))
            for side in [-1,1]:parts.append(m.cube(f'white_edge_{side}_{j}',[side*w*.45,y,-.05],[.22,2.2,1],7))
        m.group('excalibur_mesh',parts,origin=(0,12,0))
    else:
        for j in range(15):parts.append(m.cube('red_lance_shaft_'+str(j),[0,-25+j*3.6,0],[.85,3.8,.85],0 if j%3 else 3))
        for j in range(6):parts.append(m.cube('dark_grip_'+str(j),[0,-3+j*1.2,0],[1.25,.7,1.25],2))
        for j in range(10):
            t=j/9;y=27+j*1.4;w=max(.25,3.8*(1-t))
            parts.append(m.cube('crimson_spearhead_'+str(j),[math.sin(t*math.pi)*.7,y,0],[w,1.7,.8],4 if j%2 else 3))
        for side in [-1,1]:
            for j in range(6):parts.append(m.cube(f'barbed_spearhead_{side}_{j}',[side*(1+j*.55),25+j*.75,0],[1.1,2.1,1],3,[0,0,-side*22.5]))
        parts.append(m.cube('shaft_pommel',[0,-27,0],[1.5,2,1.5],4))
        lookup={e['uuid']:e for e in m.data['elements']};upper=[u for u in parts if lookup[u]['from'][1]>=0];lower=[u for u in parts if u not in upper]
        m.group('lance_upper',upper,origin=(0,21,0));m.group('lance_lower',lower,origin=(0,-12,0))
    children=m.data['outliner'][:];g=m.group('noble_socket',[]);m.data['outliner']=[dict(uuid=g['uuid'],children=children)]
    a=animation('release' if excalibur else 'thrust',9,{})
    keys=[(0,(0,0,0)),(3.9,(0,0,0)),(4.4,(-95,0,0)),(9,(-95,0,0))] if excalibur else [(0,(-55,0,0)),(2.9,(-55,0,0)),(3.7,(-90,0,0)),(9,(-90,0,0))]
    a['animators'][g['uuid']]=animator(g,keys);m.data['animations']=[a];m.save()

def effects(excalibur):
    colors=GOLD if excalibur else RED;m=Model('motionrp_excalibur_light' if excalibur else 'motionrp_gae_bolg_trail',colors=colors);parts=[]
    parts.append(m.cube('brilliant_core',[0,0,0],[2.6,2.6,20],7))
    for j in range(12):
        z=-9+j*1.65;theta=j*math.pi/3
        for k in range(3):
            a=theta+k*math.tau/3
            parts.append(m.cube(f'energy_strand_{j}_{k}',[4*math.cos(a),4*math.sin(a),z],[1.4,1.4,2],0 if excalibur else 3))
    g=m.group('noble_energy',parts);a=animation('flow',.6,{})
    a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.6,(0,0,360))]);m.data['animations']=[a];m.save()
    if not excalibur:
        m=Model('motionrp_gae_bolg_thorns',colors=RED);parts=[]
        for branch in range(12):
            a=branch*math.tau/12
            for j in range(5):
                r=4+j*.8;parts.append(m.cube(f'crimson_thorn_{branch}_{j}',[r*math.cos(a),-4+j*2,r*math.sin(a)],[1.0,2.8,1.0],4 if j<3 else 3,[0,0,22.5 if branch%2 else -22.5]))
        m.group('heart_thorns',parts);m.save()

def actors(rig):
    poses={
      'pra_right_arm':[(0,(0,0,0)),(.6,(-68.59,32.18,.1)),(1.6,(-68.59,32.18,.1)),(3.2,(-145.22,51.5,20.46)),(4.5,(-145.22,51.5,20.46)),(5,(-72.89,26.58,.14)),(7.4,(-72.89,26.58,.14)),(8.8,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.6,(-66.97,0,0)),(1.6,(-66.97,0,0)),(3.2,(-50.38,0,0)),(4.5,(-50.38,0,0)),(5,(-13.33,0,0)),(7.4,(-13.33,0,0)),(8.8,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.6,(-55.87,-32.89,-.03)),(1.6,(-55.87,-32.89,-.03)),(3.2,(-114.69,-79.82,-4.43)),(4.5,(-114.69,-79.82,-4.43)),(5,(-71.94,-27.13,-.21)),(7.4,(-71.94,-27.13,-.21)),(8.8,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(.6,(-73.77,0,0)),(1.6,(-73.77,0,0)),(3.2,(-76.78,0,0)),(4.5,(-76.78,0,0)),(5,(0,0,0)),(8.8,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(3.2,(-10,0,0)),(4.5,(-10,0,0)),(5,(3,0,0)),(8.8,(0,0,0))]}
    m=player(rig,'excalibur',8.8,poses);(ROOT/'excalibur-hand-path.csv').write_text(hand_path(m,176))
    m=player(rig,'gae_bolg',6.6,{
      'pra_right_arm':[(0,(0,0,0)),(.6,(-25,0,18)),(2.4,(45,-20,8)),(3.3,(45,-20,8)),(4.1,(-95,0,5)),(5.5,(-95,0,5)),(6.6,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.6,(-45,0,0)),(2.4,(-35,0,0)),(3.3,(-35,0,0)),(4.1,(0,0,0)),(6.6,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.6,(-65,0,-15)),(3.3,(-65,0,-15)),(4.1,(10,0,-20)),(5.5,(10,0,-20)),(6.6,(0,0,0))],
      'pc_chest':[(0,(0,0,0)),(2.4,(0,-20,0)),(3.3,(0,-20,0)),(4.1,(0,10,0)),(5.5,(0,10,0)),(6.6,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(2.4,(-5,20,0)),(3.3,(-5,20,0)),(4.1,(4,-10,0)),(6.6,(0,0,0))]})
    (ROOT/'gae_bolg-hand-path.csv').write_text(hand_path(m,132))

def main():
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned rig')
    actors(json.loads(raw))
    for is_excalibur in [True,False]:weapon(is_excalibur);effects(is_excalibur)
if __name__=='__main__':main()
