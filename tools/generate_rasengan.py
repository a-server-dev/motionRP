#!/usr/bin/env python3
"""Generate editable Blockbench assets. Rig source: BetterModel 3.6.0 (MIT).

Usage: python3 tools/generate_rasengan.py path/to/steve.bbmodel
The input hash pins the original upstream player rig, not a user-modified file.
No external images are required: the orb palette is generated as PNG bytes.
"""
import base64
import copy
import hashlib
import json
import math
from pathlib import Path
import struct
import sys
import uuid
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/bettermodel'
RIG_SHA = '005777374aee4c632c5d79dc3b34bf3bc845a0e82ad494d2d36829a61e1ae13a'


def ident(label):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'motionrp:rasengan:' + label))


def animation(name, length, tracks):
    result = dict(uuid=ident(name), name=name, loop='once', override=False,
                  length=length, snapping=20, selected=False, animators={})
    for bone, keys in tracks.items():
        result['animators'][bone['uuid'] if isinstance(bone, dict) else bone] = keys
    return result


def animator(group, keys, channel='rotation'):
    return dict(name=group['name'], type='bone', rotation_global=False,
                keyframes=[dict(channel=channel, time=t, interpolation='linear', color=-1,
                                uuid=ident(f"{group['uuid']}/{channel}/{t}/{values}"),
                                data_points=[dict(zip(('x', 'y', 'z'), map(str, values)))])
                           for t, values in keys])


def player_model(rig):
    model = copy.deepcopy(rig)
    model['name'] = model['model_identifier'] = 'motionrp_rasengan'
    groups = {g['name']: g for g in model['groups']}
    # 0..0.4 hand sign; 0.4..3.0 assisted formation; 3.0..3.6 wind-up;
    # 3.6..4.1 thrust; 4.1..4.8 dissipate and return to a neutral stance.
    tracks = {
        'pra_right_arm': [(0,(0,0,0)),(.2,(-70,15,-32)),(.4,(-80,0,-35)),
                          (1,(-85,0,-10)),(2.9,(-85,0,-10)),(3.45,(-35,-25,-25)),
                          (3.8,(-100,0,-5)),(4.15,(-100,0,-5)),(4.8,(0,0,0))],
        'prfa_right_forearm': [(0,(0,0,0)),(.2,(-55,0,0)),(.4,(-35,0,0)),
                              (1,(0,0,0)),(2.9,(0,0,0)),(3.45,(-65,0,0)),
                              (3.8,(0,0,0)),(4.15,(0,0,0)),(4.8,(0,0,0))],
        'pla_left_arm': [(0,(0,0,0)),(.2,(-70,-15,32)),(.4,(-80,0,35)),
                         (1,(-35,-20,25)),(2.9,(-35,-20,25)),(3.45,(20,0,25)),
                         (3.8,(35,-15,18)),(4.15,(35,-15,18)),(4.8,(0,0,0))],
        'plfa_left_forearm': [(0,(0,0,0)),(.2,(-55,0,0)),(.4,(-35,0,0)),
                             (1,(-55,0,0)),(2.9,(-55,0,0)),(3.6,(-30,0,0)),(4.8,(0,0,0))],
        'pc_chest': [(0,(0,0,0)),(.4,(0,-8,0)),(2.9,(0,-8,0)),(3.45,(10,25,-5)),
                     (3.8,(12,-20,0)),(4.15,(12,-20,0)),(4.8,(0,0,0))],
        'prl_right_leg': [(0,(0,0,0)),(.5,(-5,0,-8)),(3.45,(-15,0,-10)),(3.8,(-25,0,-8)),(4.8,(0,0,0))],
        'pll_left_leg': [(0,(0,0,0)),(.5,(5,0,8)),(3.45,(10,0,10)),(3.8,(20,0,8)),(4.8,(0,0,0))],
        'h_ph_head': [(0,(0,0,0)),(.4,(10,0,0)),(2.9,(10,0,0)),(3.8,(-5,10,0)),(4.8,(0,0,0))]
    }
    main = animation('rasengan', 4.8, {})
    for name, keys in tracks.items():
        g = groups[name]
        main['animators'][g['uuid']] = animator(g, keys)
    helper = animation('rasengan_helper', 2.6, {})
    for name, side in [('pra_right_arm',-1),('pla_left_arm',1)]:
        g = groups[name]
        keys = [(0,(0,0,0)),(.2,(-75,side*15,side*20))]
        for i in range(11):
            angle=math.tau*i/4 + (math.pi if side==1 else 0)
            keys.append((round(.4+i*.2,2),(-75+10*math.cos(angle),
                side*(20+8*math.sin(angle)),side*(25+10*math.cos(angle)))))
        keys += [(2.6,(-75,side*15,side*20))]
        helper['animators'][g['uuid']] = animator(g,keys)
    for name in ['prfa_right_forearm','plfa_left_forearm']:
        g=groups[name]
        helper['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.2,(-38,0,0)),(2.6,(-38,0,0))])
    g=groups['pc_chest']
    helper['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.3,(15,0,0)),(2.6,(15,0,0))])
    model['animations']=[main,helper]
    return model


def png_palette():
    colors=[(226,255,255,255),(83,215,255,255),(25,112,245,255),(154,243,255,255)]
    raw=b''.join(b'\x00'+b''.join(bytes(c) for c in colors) for _ in range(4))
    def chunk(kind,data):
        return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',4,4,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')


def orb_model():
    m=dict(meta=dict(format_version='5.0',model_format='free',box_uv=False),name='motionrp_rasengan_orb',
           model_identifier='motionrp_rasengan_orb',resolution=dict(width=4,height=4),
           elements=[],groups=[],outliner=[],textures=[],animations=[])
    m['textures']=[dict(name='rasengan_palette.png',id='0',uuid=ident('palette'),width=4,height=4,
                        uv_width=4,uv_height=4,internal=True,source='data:image/png;base64,'+base64.b64encode(png_palette()).decode())]
    def cube(name,center,size,color):
        uid=ident(name)
        half=[s/2 for s in size]
        m['elements'].append(dict(name=name,uuid=uid,type='cube',box_uv=False,origin=[0,0,0],
            **{'from':[round(c-h,5) for c,h in zip(center,half)],'to':[round(c+h,5) for c,h in zip(center,half)]},
            faces={f:dict(uv=[color+.05,.05,color+.95,.95],texture=0) for f in ['north','east','south','west','up','down']}))
        return uid
    def group(name,children):
        g=dict(name=name,uuid=ident(name),origin=[0,0,0],rotation=[0,0,0],export=True,isOpen=True)
        m['groups'].append(g)
        m['outliner'].append(dict(uuid=g['uuid'],children=children))
        return g
    # Stepped spherical core, rather than a single cube. Units are 1/16 block.
    core=[]
    for layer in range(9):
        y=(layer-4)*.6
        extent=math.sqrt(2.7**2-y*y)
        core.append(cube(f'core_x_{layer}',[0,y,0],[extent*2,.62,extent*1.2],1))
        core.append(cube(f'core_z_{layer}',[0,y,0],[extent*1.2,.62,extent*2],1))
        core.append(cube(f'core_mid_{layer}',[0,y,0],[extent*1.65,.62,extent*1.65],1))
    core.append(cube('bright_glint',[-.65,.65,-2.45],[.9,.9,.4],0))
    group('rasengan_core',core)
    spin=animation('spin',1.0,{})
    spin['loop']='loop'
    for axis in range(3):
        parts=[]
        for i in range(20):
            a=math.tau*i/20
            p=[3.35*math.cos(a),3.35*math.sin(a),0]
            if axis==1: p=[p[0],0,p[1]]
            if axis==2: p=[0,p[0],p[1]]
            parts.append(cube(f'flow_{axis}_{i}',p,[.55,.55,.55],0 if i%3==0 else (2 if axis==1 else 3)))
        g=group(f'rasengan_flow_{axis}',parts)
        keys=[]
        for i in range(5):
            rot=[0,0,0];rot[axis]=i*(90 if axis!=1 else -180)
            keys.append((i/4,rot))
        spin['animators'][g['uuid']]=animator(g,keys)
    m['animations']=[spin]
    return m


def main():
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA: raise SystemExit('Input is not the pinned upstream BetterModel 3.6.0 rig')
    for relative,model in [('players/motionrp_rasengan.bbmodel',player_model(json.loads(raw))),
                           ('models/motionrp_rasengan_orb.bbmodel',orb_model())]:
        path=ROOT/relative
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n')
        print(path)


if __name__=='__main__': main()
