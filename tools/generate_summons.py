#!/usr/bin/env python3
"""Editable gate/treasury/Susanoo/cherry-blade models and player motions; no external textures.

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


def palette(ethereal=False, colors=COLORS):
    def chunk(kind,data):
        return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
    if not ethereal:
        width,height,raw=8,1,b'\0'+b''.join(bytes(c) for c in colors)
    else:
        width,height=128,16
        rows=[]
        for y in range(height):
            row=b''
            for x in range(width):
                cell,local=x//16,x%16
                color=colors[cell]
                # Real alpha-zero holes work with cutout item rendering. Keep
                # bright bones/edges solid; no custom shader or partial-alpha claim.
                hole=cell in (5,6) and 1<local<14 and 1<y<14 and (local+y)%3==0
                row+=bytes(color[:3]+(0 if hole else 255,))
            rows.append(b'\0'+row)
        raw=b''.join(rows)
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',width,height,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')


class Model:
    def __init__(self,name,ethereal=False,colors=COLORS):
        self.name=name
        self.data=dict(meta=dict(format_version='5.0',model_format='free',box_uv=False),name=name,
            model_identifier=name,resolution=dict(width=8,height=1),elements=[],groups=[],outliner=[],animations=[],
            textures=[dict(name='summon_palette.png',id='0',uuid=ident(name+'/texture'),width=128 if ethereal else 8,height=16 if ethereal else 1,
                uv_width=8,uv_height=1,internal=True,source='data:image/png;base64,'+base64.b64encode(palette(ethereal,colors)).decode())])

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
    return model


def cherry_models():
    colors=[(246,134,187,255),(255,220,238,255),(110,47,91,255),(242,245,255,255),
            (191,139,241,255),(255,170,210,255),(156,66,121,255),(255,248,254,255)]
    steel=[(170,151,99,255),(210,215,226,255),(24,25,31,255),(224,230,239,255),
           (103,117,137,255),(40,43,52,255),(201,191,162,255),(247,248,252,255)]
    blade=Model('motionrp_senbonzakura_blade',colors=steel)
    # Author tip-down directly: the point is at Y=0 and the handle at Y=45.
    # This avoids relying on dummy entity pitch to invert an upright item.
    parts=[]
    for j in range(13):
        width=.45+min(j,10)*.19;x=.65*(j/12)**2;y=1.5+j*2.7
        parts.append(blade.cube('steel_'+str(j),[x,y,0],[width,2.9,.85],3))
        parts.append(blade.cube('cutting_edge_'+str(j),[x-width/2,y,-.12],[.22,2.9,.96],7))
        parts.append(blade.cube('spine_'+str(j),[x+width/2,y,.1],[.22,2.9,.75],4))
    parts += [blade.cube('habaki',[.65,35.7,0],[2.9,2.3,1.3],0),
              blade.cube('oval_tsuba',[.65,37.6,0],[5.6,1.4,3.2],0),
              blade.cube('tsuba_rim',[.65,37.9,0],[6,.45,3.5],6),
              blade.cube('black_grip',[.65,44.2,0],[1.65,11.5,1.6],2),
              blade.cube('pommel',[.65,50.5,0],[2,1.3,1.9],0)]
    for j in range(7):
        parts.append(blade.cube('hilt_wrap_'+str(j),[.65,39.2+j*1.5,0],[1.9,.5,1.85],5))
        parts.append(blade.cube('wrap_diamond_'+str(j),[.65,39.8+j*1.5,-.85],[.6,.65,.12],1))
    blade.group('cherry_katana',parts);blade.save()
    field=Model('motionrp_senbonzakura_field_blade',colors=steel);parts=[]
    # Blade-only standing sheets: mirrored yaw in the scene puts this +X edge
    # and swept upper point on the OUTSIDE of each side of the avenue.
    for j in range(24):
        y=1.1+j*2.2;t=max(0,(j-13)/10);curve=6*t*t
        width=2.8*(1-.85*max(0,(j-18)/5))
        parts.append(field.cube('forged_face_'+str(j),[curve,y,0],[width,2.3,.85],3))
        parts.append(field.cube('outer_cutting_edge_'+str(j),[curve+width/2,y,-.03],[.22,2.3,1],7))
        parts.append(field.cube('inner_spine_'+str(j),[curve-width/2,y,.08],[.25,2.3,.95],4))
        parts.append(field.cube('forging_ridge_'+str(j),[curve-.2,y,-.49],[.18,2.3,.12],1))
        if j<12:
            parts.append(field.cube('subtle_hamon_'+str(j),[curve+.55+(.12 if j%2 else -.12),y,-.51],[.12,1.3,.1],6))
    field.group('naked_outward_blade',parts);field.save()
    petals=Model('motionrp_senbonzakura_petals',colors=colors);parts=[]
    for j in range(32):
        angle=j*math.pi*(3-math.sqrt(5));radius=4+(j%7)*1.25
        center=[math.cos(angle)*radius,((j*7)%19)-9,math.sin(angle)*radius*.7]
        parts.append(petals.cube('petal_'+str(j),center,[2.5,.24,1.25],j%2, [0,0,22.5 if j%2 else -22.5]))
        parts.append(petals.cube('petal_tip_'+str(j),[center[0]+1.2,center[1],center[2]],[.7,.2,.75],7))
    g=petals.group('petal_cloud',parts);spin=animation('swirl',1.5,{})
    spin['loop']='loop';spin['animators'][g['uuid']]=animator(g,[(i*.375,(i%2*15,i*90,i%2*12)) for i in range(5)])
    petals.data['animations']=[spin];petals.save()


def titan_bolt():
    colors=[(255,183,30,255),(255,255,222,255),(244,124,20,255),(255,236,112,255),
            (196,221,255,255),(255,216,64,255),(230,150,24,255),(255,255,255,255)]
    model=Model('motionrp_titan_lightning',colors=colors);parts=[]
    x,y=0,0
    for j in range(8):
        sign=1 if j%2 else -1;angle=sign*22.5
        length=24;dx=-math.sin(math.radians(angle))*length;dy=math.cos(math.radians(angle))*length
        center=[x+dx/2,y+dy/2,0]
        parts.append(model.cube('electric_halo_'+str(j),center,[2.1,length,.9],0,[0,0,angle]))
        parts.append(model.cube('white_hot_core_'+str(j),[center[0],center[1],-.5],[.75,length,.45],7,[0,0,angle]))
        if j in (2,4,6):
            parts.append(model.cube('branch_'+str(j),[center[0]+sign*8,center[1]-7,.2],[1,27,.7],3,[0,0,-sign*45]))
        x+=dx;y+=dy
    model.group('lightning',parts);model.save()


def main():
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA: raise SystemExit('Input is not the pinned upstream BetterModel 3.6.0 rig')
    rig=json.loads(raw)
    player(rig,'gate_of_babylon',5.8,{
        'pra_right_arm':[(0,(0,0,0)),(.35,(-50,0,-35)),(5.35,(-50,0,-35)),(5.8,(0,0,0))],
        'prfa_right_forearm':[(0,(0,0,0)),(.35,(-8,0,-70)),(5.35,(-8,0,-70)),(5.8,(0,0,0))],
        'pla_left_arm':[(0,(0,0,0)),(.35,(-50,0,35)),(5.35,(-50,0,35)),(5.8,(0,0,0))],
        'plfa_left_forearm':[(0,(0,0,0)),(.35,(5,0,70)),(5.35,(5,0,70)),(5.8,(0,0,0))],
        'h_ph_head':[(0,(0,0,0)),(.35,(-12,8,0)),(5.35,(-12,8,0)),(5.8,(0,0,0))]})
    player(rig,'susanoo',6.4,{
        'pra_right_arm':[(0,(0,0,0)),(.3,(-75,0,-30)),(1.3,(-75,0,-30)),(2.7,(35,-25,-35)),
            (3.8,(35,-25,-35)),(4.1,(-105,10,-5)),(5,(-105,10,-5)),(6.4,(0,0,0))],
        'prfa_right_forearm':[(0,(0,0,0)),(.3,(-50,0,0)),(1.3,(-50,0,0)),(2.7,(-35,0,0)),(4.1,(0,0,0)),(6.4,(0,0,0))],
        'pla_left_arm':[(0,(0,0,0)),(.3,(-75,0,30)),(1.3,(-75,0,30)),(2.7,(-10,0,-60)),(5,(-10,0,-60)),(6.4,(0,0,0))],
        'plfa_left_forearm':[(0,(0,0,0)),(.3,(-50,0,0)),(1.3,(-50,0,0)),(2.7,(-25,0,0)),(6.4,(0,0,0))],
        'pc_chest':[(0,(0,0,0)),(1.3,(5,0,0)),(3.7,(5,-25,0)),(4.1,(12,20,0)),(5,(12,20,0)),(6.4,(0,0,0))],
        'prl_right_leg':[(0,(0,0,0)),(.5,(-15,0,15)),(5,(-15,0,15)),(6.4,(0,0,0))],
        'pll_left_leg':[(0,(0,0,0)),(.5,(10,0,-15)),(5,(10,0,-15)),(6.4,(0,0,0))]})
    player(rig,'senbonzakura',10.6,{
        'pra_right_arm':[(0,(0,0,0)),(.2,(-65,0,-8)),(.7,(-50,0,-8)),(1.0,(0,0,0)),
            (3.2,(-65,0,-18)),(7.5,(-65,0,-18)),(7.65,(-95,0,-10)),(8.8,(-95,0,-10)),(10.6,(0,0,0))],
        'prfa_right_forearm':[(0,(0,0,0)),(.2,(0,0,0)),(3.2,(-20,0,0)),(7.65,(0,0,0)),(10.6,(0,0,0))],
        'pla_left_arm':[(0,(0,0,0)),(3.2,(0,0,-12)),(8.8,(0,0,-12)),(10.6,(0,0,0))],
        'pc_chest':[(0,(0,0,0)),(.2,(0,-5,0)),(7.5,(0,-5,0)),(7.65,(5,10,0)),(10.6,(0,0,0))],
        'h_ph_head':[(0,(0,0,0)),(.2,(8,0,0)),(3.2,(0,0,0)),(10.6,(0,0,0))]})
    titan=player(rig,'titan_transform',10.3,{
        'pra_right_arm':[(0,(0,0,0)),(.4,(-70,30,-10)),(1.45,(-70,30,-10)),(2.1,(-30,0,20)),(9,(0,0,0)),(10.3,(0,0,0))],
        'prfa_right_forearm':[(0,(0,0,0)),(.4,(-100,0,0)),(.8,(-106,0,0)),(1.1,(-98,0,0)),(1.45,(-104,0,0)),(2.1,(-25,0,0)),(9,(0,0,0)),(10.3,(0,0,0))],
        'h_ph_head':[(0,(0,0,0)),(.4,(15,-5,0)),(1.45,(15,-5,0)),(2.1,(-15,0,0)),(10.3,(0,0,0))],
        'pla_left_arm':[(0,(0,0,0)),(1.5,(0,0,-15)),(2.1,(15,0,-25)),(10.3,(0,0,0))]})
    groups={g['name']:g for g in titan['groups']};idle=animation('titan_idle',2,{})
    idle['loop']='loop'
    for name,keys in {
        'pc_chest':[(0,(0,0,0)),(1,(-4,0,0)),(2,(0,0,0))],
        'h_ph_head':[(0,(-10,0,0)),(1,(-15,0,0)),(2,(-10,0,0))],
        'pra_right_arm':[(0,(5,0,15)),(1,(8,0,18)),(2,(5,0,15))],
        'pla_left_arm':[(0,(5,0,-15)),(1,(8,0,-18)),(2,(5,0,-15))]}.items():
        g=groups[name];idle['animators'][g['uuid']]=player_animator(g,keys)
    titan['animations'].append(idle);save('players/motionrp_titan_transform.bbmodel',titan)
    titan_bolt()
    cherry_models()
    gate()
    for kind in ['sword','spear','axe']: weapon(kind)
    ribs();armor();slash()


if __name__=='__main__': main()
