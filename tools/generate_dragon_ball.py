#!/usr/bin/env python3
"""Cupped palms at the right hip, grounded deep stance, and palm-anchored Spirit Bomb."""
import sys
sys.dont_write_bytecode=True
import json,hashlib,math
from pathlib import Path
from generate_summons import ROOT,RIG_SHA,player,save,animator
from generate_rasengan import hand_path
from generate_energy import sphere,beam,BLUE_LIGHT

def generate(rig):
    for spirit in [False,True]:
        name='genki_dama' if spirit else 'kamehameha';poses={}
        for side,arm,fore in [(1,'pra_right_arm','prfa_right_forearm'),(-1,'pla_left_arm','plfa_left_forearm')]:
            charge=(24.873,115.861,57.2) if side==1 else (-54.146,-98.098,-4.994)
            release=(-63.263,side*20.906,-side*6.279)
            elbow=-129.911 if side==1 else -31.602
            poses[arm]=([(0,(0,0,0)),(.4,(-175,0,side*8)),(5.3,(-175,0,side*8)),(5.5,(-85,0,side*8)),(8.8,(0,0,0))] if spirit else
                        [(0,(0,0,0)),(.8,charge),(2.5,charge),(2.8,release),(5.5,release),(6.4,(0,0,0))])
            poses[fore]=([(0,(0,0,0)),(.4,(0,0,0)),(5.3,(0,0,0)),(5.5,(-15,0,0)),(8.8,(0,0,0))] if spirit else
                         [(0,(0,0,0)),(.8,(elbow,0,0)),(2.5,(elbow,0,0)),(2.8,(-17.63,0,0)),(5.5,(-17.63,0,0)),(6.4,(0,0,0))])
        m=player(rig,name,8.8 if spirit else 6.4,poses)
        if not spirit:
            groups={g['name']:g for g in m['groups']};root=groups['player_root'];tracks=m['animations'][0]['animators']
            keytimes=[0,.8,2.5,2.8,5.5,6.4];angles=[0,58,58,48,48,0]
            for side,leg,knee in [(1,'prl_right_leg','prfl_right_foreleg'),(-1,'pll_left_leg','plfl_left_foreleg')]:
                # Opposed thigh/shin rotations keep the feet planted with a fixed hip joint.
                tracks[groups[leg]['uuid']]=animator(groups[leg],[(t,(a,0,side*14 if a else 0)) for t,a in zip(keytimes,angles)])
                tracks[groups[knee]['uuid']]=animator(groups[knee],[(t,(-2*a,0,0)) for t,a in zip(keytimes,angles)])
            # Bake root drop each tick so interpolated leg angles also remain grounded.
            drops=[]
            for tick in range(129):
                t=tick/20
                lo=max((i for i,k in enumerate(keytimes) if k<=t),default=0);hi=min(lo+1,len(keytimes)-1)
                mix=(t-keytimes[lo])/(keytimes[hi]-keytimes[lo]) if hi!=lo else 0
                a=angles[lo]*(1-mix)+angles[hi]*mix;spread=(14 if angles[lo] else 0)*(1-mix)+(14 if angles[hi] else 0)*mix
                drop=11.25*(1-math.cos(math.radians(a))*math.cos(math.radians(spread)))
                drops.append((t,(0,-drop,0)))
            tracks[root['uuid']]=animator(root,drops,'position');save('players/motionrp_'+name+'.bbmodel',m)
        right=hand_path(m,176 if spirit else 128);left=hand_path(m,176 if spirit else 128,'pli_left_item')
        (ROOT/(name+'-palms.csv')).write_text(''.join(a+','+b+'\n' for a,b in zip(right.splitlines(),left.splitlines())))
    sphere('motionrp_genki_dama_orb');sphere('motionrp_kamehameha_orb');beam('motionrp_kamehameha_beam',BLUE_LIGHT)
if __name__=='__main__':
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned rig')
    generate(json.loads(raw))
