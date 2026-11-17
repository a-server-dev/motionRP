#!/usr/bin/env python3
"""Editable gate/treasury/Susanoo models and player motions; no external textures.

Usage: python3 tools/generate_summons.py path/to/pinned/steve.bbmodel
Player rig: BetterModel 3.6.0 (MIT); same pinned rig as generate_rasengan.py.
"""
import sys
sys.dont_write_bytecode = True
import base64
import copy
import hashlib
import json
import math
import struct
import uuid
import zlib
from pathlib import Path
from generate_rasengan import RIG_SHA, animator, player_animator, animation

ROOT = Path(__file__).resolve().parents[1]/'src/main/resources/bettermodel'
COLORS = [(255,207,65,255),(255,248,186,255),(121,68,14,255),(223,238,255,255),
          (75,149,255,255),(140,73,222,255),(66,33,114,255),(216,173,255,255)]


def ident(label):
    return str(uuid.uuid5(uuid.NAMESPACE_URL,'motionrp:summons:'+label))


def palette(ethereal=False):
    def chunk(kind,data):
        return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
    if not ethereal:
        width,height,raw=8,1,b'\0'+b''.join(bytes(c) for c in COLORS)
    else:
        width,height=128,16
        rows=[]
        for y in range(height):
            row=b''
            for x in range(width):
                cell,local=x//16,x%16
                color=COLORS[cell]
                # Real alpha-zero holes work with cutout item rendering. Keep
                # bright bones/edges solid; no custom shader or partial-alpha claim.
                hole=cell in (5,6) and 1<local<14 and 1<y<14 and (local+y)%3==0
                row+=bytes(color[:3]+(0 if hole else 255,))
            rows.append(b'\0'+row)
        raw=b''.join(rows)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',width,height,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')


class Model:
    def __init__(self,name,ethereal=False):
        self.name=name
        self.data=dict(meta=dict(format_version='5.0',model_format='free',box_uv=False),name=name,
            model_identifier=name,resolution=dict(width=8,height=1),elements=[],groups=[],outliner=[],animations=[],
            textures=[dict(name='summon_palette.png',id='0',uuid=ident(name+'/texture'),width=128 if ethereal else 8,height=16 if ethereal else 1,
                uv_width=8,uv_height=1,internal=True,source='data:image/png;base64,'+base64.b64encode(palette(ethereal)).decode())])

    def cube(self,name,center,size,color=0,rotation=None):
        uid=ident(self.name+'/'+name)
        obj=dict(name=name,uuid=uid,type='cube',box_uv=False,origin=center,
            **{'from':[round(c-s/2,6) for c,s in zip(center,size)],'to':[round(c+s/2,6) for c,s in zip(center,size)]},
            faces={f:dict(uv=[color+.05,.05,color+.95,.95],texture=0) for f in ['north','south','east','west','up','down']})
        if rotation: obj['rotation']=rotation
        self.data['elements'].append(obj)
        return uid

    def group(self,name,children,origin=(0,0,0)):
        group=dict(name=name,uuid=ident(self.name+'/'+name),origin=list(origin),rotation=[0,0,0],export=True,isOpen=True)
        self.data['groups'].append(group);self.data['outliner'].append(dict(uuid=group['uuid'],children=children))
        return group

    def save(self):
        save('models/'+self.name+'.bbmodel',self.data)


def save(relative,data):
    path=ROOT/relative;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
    print(path)


def gate():
    model=Model('motionrp_golden_gate');halo=[];center=[]
    for ring,radius,color,width in [(0,9,0,1.25),(1,7.4,1,.6)]:
        for i in range(24):
            a=math.tau*i/24
            halo.append(model.cube(f'ring_{ring}_{i}',[radius*math.cos(a),radius*math.sin(a),0],[width,width,.5],color))
    for i in range(9):
        y=(i-4)*1.6;extent=math.sqrt(7**2-y*y)
        center.append(model.cube(f'aperture_{i}',[0,y,.45],[extent*2,1.65,.25],2))
    for i in range(8):
        a=math.tau*i/8
        halo.append(model.cube(f'sigil_{i}',[6*math.cos(a),6*math.sin(a),-.2],[.7,1.7,.5],1))
    g=model.group('golden_halo',halo);model.group('aperture',center)
    spin=animation('spin',2,{})
    spin['loop']='loop';spin['animators'][g['uuid']]=animator(g,[(i/2,(0,0,i*90)) for i in range(5)])
    model.data['animations']=[spin];model.save()


def weapon(kind):
    model=Model('motionrp_treasury_'+kind);parts=[]
    def cube(name,center,size,color): parts.append(model.cube(name,center,size,color))
    cube('grip',[0,0,2],[1.2,1.2,6],2);cube('pommel',[0,0,5.3],[2,2,1.6],0)
    if kind=='sword':
        cube('crossguard',[0,0,-1.2],[7,1.4,1.5],0)
        cube('blade',[0,0,-9],[2.4,.8,14],3);cube('blade_ridge',[0,-.5,-9],[.5,.3,13],1)
        cube('point',[0,0,-16.5],[1.2,.7,2],3)
    elif kind=='spear':
        cube('shaft',[0,0,-4],[.8,.8,17],0)
        for i in range(5): cube('spearhead_'+str(i),[0,0,-13-i],[3.8-i*.7,.7,1.2],3)
        cube('collar',[0,0,-10.5],[2,2,2],1)
    else:
        cube('haft',[0,0,-5],[1.1,1.1,17],2);cube('axe_head',[0,0,-11],[5,1.5,4],0)
        for side in [-1,1]:
            for i in range(3): cube(f'axe_edge_{side}_{i}',[side*(3+i),0,-11],[1.2,1,6-i],3)
    model.group(kind,parts);model.save()


def ribs():
    model=Model('motionrp_susanoo_ribs',True);parts=[]
    for level in range(4):
        for segment in range(11):
            angle=math.radians(15+segment*15)
            # Open chest front (-Z); curved ribs wrap around the player.
            parts.append(model.cube(f'rib_{level}_{segment}',[12*math.cos(angle),5+level*5,8*math.sin(angle)],
                                    [2.8,1.3,2.5],7 if level%2 else 5))
    parts.append(model.cube('spine',[0,13,8],[2.5,26,2.5],5))
    parts.append(model.cube('pelvis',[0,2,2],[18,3,8],6))
    model.group('ribcage',parts);model.save()


def armor():
    model=Model('motionrp_susanoo_armor',True)
    # Open rib framework rather than a filled rectangular chest.
    torso=[model.cube('spinal_column',[0,20,5],[2.3,24,2.3],7),
           model.cube('sternum',[0,20,-6],[1.6,22,1.5],7)]
    for row in range(5):
        for i in range(20):
            angle=math.tau*i/20
            torso.append(model.cube(f'chakra_rib_{row}_{i}',[10*math.cos(angle),12+row*3.4,6*math.sin(angle)],
                                    [2.8,1.15,2],7 if i%5==0 else 5))
    for side in [-1,1]:
        for j in range(3):
            torso.append(model.cube(f'waist_lamella_{side}_{j}',[side*(5+j*3),6-j,2],
                                    [3.4,7,3],6,[0,0,-side*22.5]))
    model.group('armored_torso',torso,origin=(0,11,0))
    # Stepped, tapered mask, swept cheek plates, long horns and angry white eyes.
    head=[]
    for row,width in enumerate([5,8,10,12,11,9]):
        head.append(model.cube(f'mask_contour_{row}',[0,29.5+row*1.7,-1],[width,1.8,7],6))
    for side in [-1,1]:
        head.append(model.cube(f'temple_{side}',[side*6,36,1],[3,9,8],5))
        head.append(model.cube(f'cheek_flare_{side}',[side*6.5,31,-2.7],[2,9,3],7,[0,0,-side*22.5]))
        head.append(model.cube(f'eye_{side}',[side*2.6,35.8,-4.8],[3.5,.65,.4],3,[0,0,side*22.5]))
        head.append(model.cube(f'brow_{side}',[side*2.5,37,-4.8],[4,.8,.6],5,[0,0,side*22.5]))
        for j in range(3):
            head.append(model.cube(f'horn_{side}_{j}',[side*(6.5+j*2),40+j*3,1],
                                   [3-j*.6,5,3-j*.5],7,[0,0,-side*22.5]))
        head.append(model.cube(f'horn_tip_{side}',[side*11.3,49,1],[1,4,1],3,[0,0,-side*22.5]))
        head.append(model.cube(f'fang_{side}',[side*2.7,30,-4.8],[.9,2.3,.8],7))
    head.append(model.cube('forehead_crest',[0,40,-1],[2,6,5],7))
    head.append(model.cube('chin_tip',[0,27.5,-2],[2,3,3],7))
    model.group('samurai_head',head,origin=(0,30,0))
    for side,name in [(1,'sword_arm'),(-1,'guard_arm')]:
        parts=[model.cube(name+'_upper',[side*18,21,1],[5,12,5],5),
               model.cube(name+'_arm_bone',[side*18,21,-2],[1.5,12,1.5],7),
               model.cube(name+'_forearm',[side*18,12,0],[5,9,5],5),
               model.cube(name+'_palm',[side*18,6,-1],[5.5,4,5],6)]
        for j in range(4):
            parts.append(model.cube(f'{name}_shoulder_lamella_{j}',[side*(12+j*.8),31-j*2.1,1],
                                    [7+j*1.1,1.6,11],6 if j%2 else 5,[0,0,-side*22.5]))
        for j in range(3):
            parts.append(model.cube(f'{name}_bracer_{j}',[side*18,10+j*2.5,-.3],[7,1.4,7],7))
        for j in range(4):
            parts.append(model.cube(f'{name}_finger_{j}',[side*18-2+j*1.3,3.5,-2],[.9,4,1.5],7))
        if side==1:
            parts += [model.cube('katana_grip',[18,7,-6],[1.5,1.5,9],6),
                      model.cube('katana_guard',[18,7,-11],[7,1.5,1.5],7)]
            for j in range(8):
                parts.append(model.cube(f'curved_katana_{j}',[18+j*.18,7,-13-j*2.5],[2.5-j*.22,.6,2.8],7))
                parts.append(model.cube(f'katana_glow_edge_{j}',[16.6+j*.29,7,-13-j*2.5],[.35,.7,2.8],3))
        g=model.group(name,parts,origin=(side*14,29,1))
        if side==1:
            attack=animation('slash',2.4,{})
            attack['animators'][g['uuid']]=animator(g,[(0,(0,0,-10)),(.6,(-35,-30,-65)),
                (1,(-10,15,-30)),(1.2,(65,55,25)),(1.7,(65,55,25)),(2.4,(0,0,-10))])
            model.data['animations']=[attack]
    model.save()


def slash():
    model=Model('motionrp_susanoo_slash');parts=[]
    for i in range(22):
        angle=math.radians(-65+i*6)
        parts.append(model.cube('arc_'+str(i),[18*math.cos(angle),18*math.sin(angle),0],[2.5,2.8,.5],3 if i%3 else 7))
    model.group('crescent',parts);model.save()


def player(rig,name,length,poses):
    model=copy.deepcopy(rig);model['name']=model['model_identifier']='motionrp_'+name
    groups={g['name']:g for g in model['groups']};motion=animation(name,length,{})
    for bone,keys in poses.items():
        g=groups[bone];motion['animators'][g['uuid']]=player_animator(g,keys)
    model['animations']=[motion];save('players/motionrp_'+name+'.bbmodel',model)


def main():
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA: raise SystemExit('Input is not the pinned upstream BetterModel 3.6.0 rig')
    rig=json.loads(raw)
    player(rig,'gate_of_babylon',8,{
        'pra_right_arm':[(0,(0,0,0)),(.65,(-55,-20,45)),(2.9,(-55,-20,45)),(3.2,(-70,0,25)),
            (4.9,(-70,0,25)),(5.3,(-45,-15,45)),(5.5,(-80,10,20)),(6.8,(-80,10,20)),(8,(0,0,0))],
        'prfa_right_forearm':[(0,(0,0,0)),(.65,(-65,0,15)),(2.9,(-65,0,15)),(3.2,(-15,0,5)),
            (4.9,(-15,0,5)),(5.3,(-70,0,15)),(5.5,(-10,0,0)),(8,(0,0,0))],
        'pla_left_arm':[(0,(0,0,0)),(.65,(15,-10,25)),(6.8,(15,-10,25)),(8,(0,0,0))],
        'plfa_left_forearm':[(0,(0,0,0)),(.65,(-75,0,-15)),(6.8,(-75,0,-15)),(8,(0,0,0))],
        'h_ph_head':[(0,(0,0,0)),(.65,(-18,12,-5)),(6.8,(-18,12,-5)),(8,(0,0,0))],
        'pc_chest':[(0,(0,0,0)),(.65,(-7,-15,-6)),(3.2,(-7,-10,-6)),(5.5,(-5,5,-4)),(8,(0,0,0))],
        'prl_right_leg':[(0,(0,0,0)),(.65,(-5,0,12)),(6.8,(-5,0,12)),(8,(0,0,0))],
        'pll_left_leg':[(0,(0,0,0)),(.65,(5,0,-12)),(6.8,(5,0,-12)),(8,(0,0,0))]})
    player(rig,'susanoo',6.4,{
        'pra_right_arm':[(0,(0,0,0)),(.3,(-75,0,-30)),(1.3,(-75,0,-30)),(2.7,(35,-25,-35)),
            (3.8,(35,-25,-35)),(4.1,(-105,10,-5)),(5,(-105,10,-5)),(6.4,(0,0,0))],
        'prfa_right_forearm':[(0,(0,0,0)),(.3,(-50,0,0)),(1.3,(-50,0,0)),(2.7,(-35,0,0)),(4.1,(0,0,0)),(6.4,(0,0,0))],
        'pla_left_arm':[(0,(0,0,0)),(.3,(-75,0,30)),(1.3,(-75,0,30)),(2.7,(-10,0,-60)),(5,(-10,0,-60)),(6.4,(0,0,0))],
        'plfa_left_forearm':[(0,(0,0,0)),(.3,(-50,0,0)),(1.3,(-50,0,0)),(2.7,(-25,0,0)),(6.4,(0,0,0))],
        'pc_chest':[(0,(0,0,0)),(1.3,(5,0,0)),(3.7,(5,-25,0)),(4.1,(12,20,0)),(5,(12,20,0)),(6.4,(0,0,0))],
        'prl_right_leg':[(0,(0,0,0)),(.5,(-15,0,15)),(5,(-15,0,15)),(6.4,(0,0,0))],
        'pll_left_leg':[(0,(0,0,0)),(.5,(10,0,-15)),(5,(10,0,-15)),(6.4,(0,0,0))]})
    gate()
    for kind in ['sword','spear','axe']: weapon(kind)
    ribs();armor();slash()


if __name__=='__main__': main()
