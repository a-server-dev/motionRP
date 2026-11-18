#!/usr/bin/env python3
"""Editable Dragon Ball poses, solid light beam and an 80-block Spirit Bomb at scale 40."""
import sys
sys.dont_write_bytecode=True
import json,hashlib,math
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player
BLUE=[(22,111,240,255),(100,205,255,255),(17,63,173,255),(220,250,255,255),(52,159,255,255),(129,225,255,255),(66,190,255,255),(255,255,255,255)]
def generate(rig):
    for spirit in [False,True]:
        name='genki_dama' if spirit else 'kamehameha'
        poses={}
        for side,arm,fore in [(1,'pra_right_arm','prfa_right_forearm'),(-1,'pla_left_arm','plfa_left_forearm')]:
            poses[arm]=([(0,(0,0,0)),(.4,(-20,0,side*15)),(2.4,(-175,0,side*12)),(5,(-175,0,side*12)),(5.3,(-85,0,side*8)),(8.8,(0,0,0))] if spirit else
                        [(0,(0,0,0)),(.8,(-35,side*55,side*10)),(2.5,(-35,side*55,side*10)),(2.8,(-85,side*20,0)),(5.5,(-85,side*20,0)),(6.4,(0,0,0))])
            poses[fore]=([(0,(0,0,0)),(.4,(-15,0,0)),(2.4,(0,0,0)),(5,(0,0,0)),(5.3,(-15,0,0)),(8.8,(0,0,0))] if spirit else
                         [(0,(0,0,0)),(.8,(-100,0,0)),(2.5,(-100,0,0)),(2.8,(-10,0,0)),(5.5,(-10,0,0)),(6.4,(0,0,0))])
        player(rig,name,8.8 if spirit else 6.4,poses)
    for name in ['motionrp_genki_dama_orb','motionrp_kamehameha_orb']:
        m=Model(name,colors=BLUE);ids=[]
        # Filled sphere, horizontal runs of voxels, not crossed billboards or a cube.
        for y in range(-15,16,2):
            for z in range(-15,16,2):
                d=256-y*y-z*z
                if d>0:ids.append(m.cube(f'sphere_{y}_{z}',[0,y,z],[2*math.sqrt(d),2,2],7 if y>8 else 3 if z<-8 else 1 if y>0 else 4))
        ids.append(m.cube('equatorial_diameter',[0,0,0],[32,1,1],3))
        m.group('energy_sphere',ids);m.save()
    m=Model('motionrp_kamehameha_beam',colors=BLUE)
    ids=[m.cube('white_beam_core',[0,0,0],[6,6,20],7),m.cube('blue_light_horizontal',[0,0,0],[10,4,20],1),m.cube('blue_light_vertical',[0,0,0],[4,10,20],4)]
    m.group('energy_beam',ids);m.save()
if __name__=='__main__':
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned rig')
    generate(json.loads(raw))
