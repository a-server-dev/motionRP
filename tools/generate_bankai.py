#!/usr/bin/env python3
"""Initial Zangetsu Bankai: large cleaver, black pressure, compact Tensa and coat tails.
Usage: python3 tools/generate_bankai.py path/to/pinned/steve.bbmodel
"""
import sys
sys.dont_write_bytecode=True
import hashlib,json,math
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player,save,animator,animation
from generate_rasengan import hand_path
COLORS=[(15,17,23,255),(222,231,238,255),(45,48,58,255),(128,5,24,255),(235,239,244,255),(99,101,107,255),(85,63,47,255),(249,250,252,255)]

def blade(tensa=False):
    name='motionrp_tensa_zangetsu' if tensa else 'motionrp_zangetsu_cleaver';m=Model(name,colors=COLORS);parts=[]
    if tensa:
        for j in range(12):
            t=j/11;y=1.5+j*2.4;x=.9*t*t
            parts.append(m.cube('black_blade_'+str(j),[x,y,0],[1.5-.7*t,2.6,.65],0))
            parts.append(m.cube('silver_edge_'+str(j),[x-(1.5-.7*t)/2,y,-.05],[.18,2.6,.75],1))
        # Four bent arms of the traditional manji-shaped guard, in the X/Z plane.
        for i in range(4):
            a=i*math.pi/2
            for j,(x,z,sx,sz) in enumerate([(1.8,0,3.6,.75),(3.3,-1.4,.75,3.5)]):
                X=x*math.cos(a)-z*math.sin(a);Z=x*math.sin(a)+z*math.cos(a)
                parts.append(m.cube(f'manji_guard_{i}_{j}',[X,0,Z],[sz if i%2 else sx,1, sx if i%2 else sz],0))
        parts.append(m.cube('black_wrapped_hilt',[0,-4.7,0],[1.6,8.5,1.6],0))
        for i in range(5):parts.append(m.cube('white_hilt_diamond_'+str(i),[0,-1.2-i*1.5,-.84],[.65,.8,.1],4))
        for i in range(4):
            y=-10-i*1.5
            for side in [-1,1]:
                parts.append(m.cube(f'chain_side_{i}_{side}',[side*.8,y,0],[.3,1.5,.4],5))
                parts.append(m.cube(f'chain_end_{i}_{side}',[0,y+side*.65,0],[1.7,.3,.4],5))
    else:
        for j in range(16):
            t=j/15;y=1.2+j*2.8;width=8*(1-.85*t*t);x=2*t*t
            parts.append(m.cube('cleaver_spine_'+str(j),[x,y,0],[width,3,.9],0))
            parts.append(m.cube('broad_white_edge_'+str(j),[x-width*.32,y,-.1],[width*.38,3,1],4))
        parts.append(m.cube('cloth_grip',[0,-4.5,0],[1.7,9,1.6],1))
        for i in range(7):parts.append(m.cube('cloth_wrap_'+str(i),[0,-.5-i*1.2,-.85],[1.9,.35,.1],5))
        for i in range(6):parts.append(m.cube('trailing_bandage_'+str(i),[.4+math.sin(i)*.35,-10-i*1.3,0],[1.2,1.5,.18],4))
    if tensa:
        m.group('tensa_blade',parts,origin=(0,7,0))
    else:
        blade_ids={e['uuid'] for e in m.data['elements'] if e['name'].startswith(('cleaver_','broad_white_'))}
        m.group('cleaver_blade',[uid for uid in parts if uid in blade_ids],origin=(0,22,0))
        m.group('cleaver_hilt',[uid for uid in parts if uid not in blade_ids],origin=(0,-8,0))
    children=m.data['outliner'][:];g=m.group('sword_socket',[]);m.data['outliner']=[dict(uuid=g['uuid'],children=children)]
    a=animation('point',1,{})
    a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(-90,0,0)),(1,(-90,0,0))]);m.data['animations']=[a];m.save()

def pressure():
    m=Model('motionrp_bankai_cocoon',colors=COLORS);parts=[]
    for j in range(12):
        y=2+j*3.6
        for i in range(5):
            a=j*.7+i*math.tau/5;radius=7.5+math.sin(j)*.7
            parts.append(m.cube(f'black_pressure_{j}_{i}',[radius*math.cos(a),y,radius*math.sin(a)],[2.3,4.1,2.3],0))
            if (j+i)%3==0:parts.append(m.cube(f'white_streak_{j}_{i}',[(radius+.7)*math.cos(a),y,(radius+.7)*math.sin(a)],[.35,4.3,.35],4))
    g=m.group('bankai_column',parts,origin=(0,22,0));a=animation('swirl',1,{})
    a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1,(0,-360,0))]);m.data['animations']=[a];m.save()
    m=Model('motionrp_bankai_wave',colors=COLORS);parts=[]
    for i in range(32):
        a=math.tau*i/32;parts.append(m.cube('pressure_ring_'+str(i),[8*math.cos(a),.2,8*math.sin(a)],[1.4,.3,1.4],4 if i%3 else 0))
    m.group('pressure_ring',parts);m.save()
    m=Model('motionrp_bankai_coat',colors=COLORS)
    for side in [-1,1]:
        parts=[]
        for j in range(5):
            parts.append(m.cube(f'black_tail_{side}_{j}',[side*(2.4+j*.12),2+j*2,2.8+j*.05],[4.6,2.3,.6],0))
            parts.append(m.cube(f'red_lining_{side}_{j}',[side*(2.4+j*.12),2+j*2,2.44+j*.05],[4.2,2.3,.12],3))
        g=m.group('coat_tail_'+str(side),parts,origin=(side*2,12,2.8))
    a=animation('flutter',1.6,{})
    a['loop']='loop'
    for j,g in enumerate(m.data['groups']):a['animators'][g['uuid']]=animator(g,[(0,(-8,0,j*4-2)),(.8,(-15,0,2-j*4)),(1.6,(-8,0,j*4-2))])
    m.data['animations']=[a];m.save()

def main():
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned BetterModel rig')
    rig=json.loads(raw)
    poses={
      'pra_right_arm':[(0,(0,0,0)),(.5,(-90,0,8)),(2,(-90,0,8)),(3.4,(-75,0,10)),(5.6,(-85,0,8)),(6.4,(-95,0,5)),(7.8,(-95,0,5)),(8.8,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.5,(0,0,0)),(2,(0,0,0)),(3.4,(-25,0,0)),(5.6,(-10,0,0)),(6.4,(-5,0,0)),(7.8,(-5,0,0)),(8.8,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.5,(0,0,-8)),(3.4,(-10,0,-18)),(5.6,(0,0,-10)),(7.8,(0,0,-10)),(8.8,(0,0,0))],
      'pc_chest':[(0,(0,0,0)),(.5,(0,-8,0)),(3.4,(6,-12,0)),(5.6,(0,8,0)),(7.8,(0,8,0)),(8.8,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(.5,(-3,8,0)),(3.4,(6,12,0)),(5.6,(-5,-8,0)),(7.8,(-5,-8,0)),(8.8,(0,0,0))],
      'prl_right_leg':[(0,(0,0,0)),(.5,(0,8,0)),(7.8,(0,8,0)),(8.8,(0,0,0))],
      'pll_left_leg':[(0,(0,0,0)),(.5,(0,-8,0)),(7.8,(0,-8,0)),(8.8,(0,0,0))]}
    model=player(rig,'zangetsu_bankai',8.8,poses)
    (ROOT/'zangetsu_bankai-hand-path.csv').write_text(hand_path(model,176))
    blade();blade(True);pressure()
if __name__=='__main__':main()
