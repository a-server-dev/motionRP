#!/usr/bin/env python3
"""Editable Excalibur and Gae Bolg weapons, release effects and baked player palms."""
import sys
sys.dont_write_bytecode=True
import json,math,hashlib
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player,save,animator,animation
from generate_rasengan import hand_path
GOLD=[(244,185,35,255),(255,242,158,255),(21,45,112,255),(226,241,255,255),(75,103,165,255),(198,122,16,255),(255,217,70,255),(255,255,248,255)]
RED=[(140,32,30,255),(218,114,101,255),(64,12,18,255),(172,43,40,255),(206,70,60,255),(93,18,21,255),(177,67,63,255),(238,159,149,255)]

def weapon(excalibur):
    m=Model('motionrp_excalibur_blade' if excalibur else 'motionrp_gae_bolg_spear',colors=GOLD if excalibur else RED);parts=[]
    if excalibur:
        parts.append(m.cube('royal_blue_grip',[0,-2.5,0],[1.5,8,1.5],2))
        for j in range(6):parts.append(m.cube('gold_grip_lacing_'+str(j),[0,-5.7+j*1.2,0],[1.6,.18,1.6],4))
        parts.append(m.cube('gold_pommel',[0,-7,0],[3.2,2.2,2.2],0));parts.append(m.cube('pommel_blue_gem',[0,-7,-1.3],[1.4,1.4,.4],4))
        for side in [-1,1]:
            for j in range(4):parts.append(m.cube(f'swept_gold_quillon_{side}_{j}',[side*(1.6+j*1.7),2.5+j*.9,0],[2.2,1.8,2],0,[0,0,-side*22.5]))
        for side in [-1,1]:
            for j in range(4):
                parts.append(m.cube(f'quillon_blue_engraving_{side}_{j}',[side*(1.5+j*1.7),2.5+j*.9,-1.08],[1.7,.25,.12],2,[0,0,side*22.5]))
        parts.append(m.cube('guard_blue_crest' ,[0,3,-1.4],[3.8,4,.5],2));parts.append(m.cube('guard_gold_crest',[0,3,-1.8],[1.3,2.4,.3],1))
        for j in range(15):
            t=j/14;w=3.5 if t<.82 else max(.2,3.5*(1-(t-.82)/.18));y=5+j*1.95
            parts.append(m.cube('silver_blade_'+str(j),[0,y,0],[w,2.2,.9],3))
            parts.append(m.cube('silver_central_ridge_'+str(j),[0,y,-.52],[.3,1.8,.1],7))
            if j<5:parts.append(m.cube('blade_rune_'+str(j),[.6,y,-.56],[.35,.6,.08],4,[0,0,22.5 if j%2 else -22.5]))
            for side in [-1,1]:parts.append(m.cube(f'white_edge_{side}_{j}',[side*w*.45,y,-.05],[.22,2.2,1],7))
        m.group('excalibur_mesh',parts,origin=(0,12,0))
    else:
        for j in range(15):
            y=-25+j*3.6
            parts.append(m.cube('red_lance_shaft_'+str(j),[0,y,0],[.95,3.8,.95],0))
            parts.append(m.cube('carved_shaft_groove_'+str(j),[.3*math.sin(j*.9),y,-.52],[.26,2.8,.1],5,[0,0,22.5 if j%2 else -22.5]))
            parts.append(m.cube('polished_shaft_glint_'+str(j),[-.4,y,-.45],[.12,3.3,.12],1))
        for j in range(6):parts.append(m.cube('dark_grip_'+str(j),[0,-3+j*1.2,0],[1.25,.7,1.25],2))
        for j in range(15):
            t=j/14;y=26+j*1.1;w=max(.16,4.5*(1-t)**.65)
            parts.append(m.cube('leaf_spearhead_'+str(j),[0,y,0],[w,1.3,.85],3))
            parts.append(m.cube('spearhead_central_groove_'+str(j),[0,y,-.48],[.3,1.25,.12],5))
            for side in [-1,1]:parts.append(m.cube(f'leaf_cutting_edge_{side}_{j}',[side*w*.46,y,-.1],[.22,1.3,.8],1))
        for side in [-1,1]:
            for tooth in range(4):
                for j in range(3):parts.append(m.cube(f'jagged_tooth_{side}_{tooth}_{j}',[side*(2.3+j*.45),27+tooth*2.3+j*.35,0],[.8,1.1,.7],3))
        parts.append(m.cube('spearhead_collar',[0,24.2,0],[3.2,2.2,2.4],0))
        parts.append(m.cube('shaft_pommel',[0,-27,0],[1.5,2,1.5],4))
        lookup={e['uuid']:e for e in m.data['elements']};upper=[u for u in parts if lookup[u]['from'][1]>=0];lower=[u for u in parts if u not in upper]
        m.group('lance_upper',upper,origin=(0,21,0));m.group('lance_lower',lower,origin=(0,-12,0))
    children=m.data['outliner'][:];g=m.group('noble_socket',[]);m.data['outliner']=[dict(uuid=g['uuid'],children=children)]
    a=animation('release' if excalibur else 'throw_sequence',9,{})
    keys=[(0,(0,0,0)),(3.9,(0,0,0)),(4.025,(-95,0,0)),(9,(-95,0,0))] if excalibur else [(0,(-85,0,0)),(1.3,(-85,0,0)),(2.1,(-50,0,0)),(2.3,(-90,0,0)),(3.9,(-90,0,0)),(4.2,(-55,0,0)),(9,(-55,0,0))]
    a['animators'][g['uuid']]=animator(g,keys);m.data['animations']=[a];m.save()

def effects(excalibur):
    if excalibur:
        from generate_energy import beam,pillar,GOLD_LIGHT
        beam('motionrp_excalibur_light',GOLD_LIGHT);pillar()

def actors(rig):
    poses={
      'pra_right_arm':[(0,(0,0,0)),(.6,(-68.59,32.18,.1)),(1.6,(-68.59,32.18,.1)),(3.2,(-145.22,51.5,20.46)),(4.5,(-145.22,51.5,20.46)),(4.625,(-72.89,26.58,.14)),(7.4,(-72.89,26.58,.14)),(8.8,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.6,(-66.97,0,0)),(1.6,(-66.97,0,0)),(3.2,(-50.38,0,0)),(4.5,(-50.38,0,0)),(4.625,(-13.33,0,0)),(7.4,(-13.33,0,0)),(8.8,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.6,(-55.87,-32.89,-.03)),(1.6,(-55.87,-32.89,-.03)),(3.2,(-114.69,-79.82,-4.43)),(4.5,(-114.69,-79.82,-4.43)),(4.625,(-71.94,-27.13,-.21)),(7.4,(-71.94,-27.13,-.21)),(8.8,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(.6,(-73.77,0,0)),(1.6,(-73.77,0,0)),(3.2,(-76.78,0,0)),(4.5,(-76.78,0,0)),(4.625,(0,0,0)),(8.8,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(3.2,(-10,0,0)),(4.5,(-10,0,0)),(4.625,(3,0,0)),(8.8,(0,0,0))]}
    m=player(rig,'excalibur',8.8,poses);(ROOT/'excalibur-hand-path.csv').write_text(hand_path(m,176))
    poses={
      'pra_right_arm':[(0,(0,0,0)),(.6,(20,0,15)),(1.2,(40,-5,15)),(1.7,(40,-5,15)),(2.5,(-150,-10,0)),(2.7,(-65,0,5)),(3.1,(-85,0,5)),(4.3,(-65,0,5)),(5,(-65,0,5)),(5.8,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.6,(-20,0,0)),(2.5,(-20,0,0)),(2.7,(0,0,0)),(3.1,(-15,0,0)),(4.3,(-25,0,0)),(5,(-25,0,0)),(5.8,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.6,(-45,0,-8)),(1.2,(-35,0,-15)),(1.7,(-35,0,-15)),(2.5,(-80,0,-35)),(2.7,(10,0,-25)),(3.1,(-25,0,-10)),(4.3,(0,0,-5)),(5.8,(0,0,0))],
      'pc_chest':[(0,(0,0,0)),(.6,(45,0,0)),(1.2,(20,0,0)),(1.7,(20,0,0)),(2.5,(-8,0,0)),(2.7,(5,0,0)),(3.1,(20,0,0)),(4.3,(0,0,0)),(5.8,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(.6,(-45,0,0)),(1.7,(-15,0,0)),(2.5,(12,0,0)),(2.7,(55,0,0)),(3.1,(-10,0,0)),(4.3,(0,0,0)),(5.8,(0,0,0))]}
    def legs(tick):
        spread=8 if tick>=12 and tick<100 else 8*min(1,tick/12) if tick<12 else 8*max(0,(116-tick)/16)
        if tick<24:
            t=min(1,tick/12) if tick<=20 else (24-tick)/4
            hip=11.25*(1-.5*t);local_height=hip/math.cos(math.radians(spread))
            # Independent two-bone solutions: rear knee close to the turf, front foot planted ahead.
            def ik(z):
                d=min(11.25,math.hypot(local_height,z));half=math.acos(d/11.25);mean=math.atan2(-z,local_height)
                upper=mean+half;lower=mean-half
                return -math.degrees(upper),-math.degrees(lower-upper)
            r,kr=ik(8*t);l,kl=ik(-2*t)
            return r,l,kr,kl,spread,11.25-hip
        if tick<=34:
            r=32*math.sin((tick-24)*math.tau/10);return r,-r,0,0,spread,11.25*(1-math.cos(math.radians(r))*math.cos(math.radians(spread)))
        if tick<62:return -35,-35,70,70,spread,0
        a=55*max(0,(82-tick)/20)
        return -a,-a,2*a,2*a,spread,11.25*(1-math.cos(math.radians(a))*math.cos(math.radians(spread)))
    for name,right,lower in [('prl_right_leg',True,False),('pll_left_leg',False,False),('prfl_right_foreleg',True,True),('plfl_left_foreleg',False,True)]:
        poses[name]=[]
        for tick in range(117):
            r,l,kr,kl,spread,_=legs(tick);poses[name].append((tick/20,(kr if lower and right else kl if lower else r if right else l,0,0 if lower else spread if right else -spread)))
    m=player(rig,'gae_bolg',5.8,poses);root=next(g for g in m['groups'] if g['name']=='player_root')
    keys=[]
    for tick in range(117):
        *_,drop=legs(tick)
        keys.append((tick/20,(0,-drop,0)))
    m['animations'][0]['animators'][root['uuid']]=animator(root,keys,'position');save('players/motionrp_gae_bolg.bbmodel',m)
    (ROOT/'gae_bolg-hand-path.csv').write_text(hand_path(m,116))

def main():
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned rig')
    actors(json.loads(raw))
    for is_excalibur in [True,False]:
        weapon(is_excalibur)
        if is_excalibur:effects(True)
if __name__=='__main__':main()
