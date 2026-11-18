#!/usr/bin/env python3
"""100/200-gate variants, Bab-ilu, Ea, vortex and exact user-supplied Titan PNG.
Usage: python3 tools/generate_treasury.py /path/to/pinned/steve.bbmodel
The skin PNG is embedded unchanged; no Mojang upload/signing is needed.
"""
import sys
sys.dont_write_bytecode=True
import base64,copy,hashlib,json,math
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player,save,animation,animator,ident
from generate_rasengan import hand_path

def dense_gate():
    m=Model('motionrp_golden_gate_mass');parts=[]
    for i in range(24):
        a=math.tau*i/24
        parts.append(m.cube('gold_rim_'+str(i),[9*math.cos(a),9*math.sin(a),0],[1.6,1.6,.5],1 if i%3==0 else 0))
    for i in range(16):
        a=math.tau*i/16
        parts.append(m.cube('inner_sigil_'+str(i),[6.8*math.cos(a),6.8*math.sin(a),-.15],[.7,1.1,.5],1))
    for i in range(9):
        y=(i-4)*1.6
        parts.append(m.cube('aperture_'+str(i),[0,y,.4],[2*math.sqrt(49-y*y),1.7,.3],2))
    g=m.group('mass_gate',parts);a=animation('spin',2,{})
    a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(2,(0,0,360))]);m.data['animations']=[a];m.save()

def key_and_ea():
    colors=[(244,190,44,255),(255,241,158,255),(26,20,29,255),(180,13,34,255),(255,45,65,255),(91,5,19,255),(235,220,182,255),(255,251,242,255)]
    m=Model('motionrp_bab_ilu',colors=colors);parts=[]
    parts.append(m.cube('key_shaft',[0,5,0],[1.8,12,1.8],0))
    for i in range(12):
        a=math.tau*i/12;parts.append(m.cube('crown_'+str(i),[4*math.cos(a),15+4*math.sin(a),0],[1.8,1.8,1],0))
    parts.append(m.cube('ruby',[0,15,0],[2.8,2.8,1.5],3))
    for i in range(3):parts.append(m.cube('key_tooth_'+str(i),[2.4,1+i*2.6,0],[4,1.2,1.6],1))
    m.group('bab_ilu',parts);m.save()
    m=Model('motionrp_ea',colors=colors);parts=[]
    parts.append(m.cube('black_grip',[0,3,0],[1.5,7,1.5],2));parts.append(m.cube('gold_pommel',[0,-1,0],[3,2,3],0))
    for y in [0,2,4,6]:parts.append(m.cube('grip_inlay_'+str(y),[0,y,-.8],[.8,.5,.15],0))
    for side in [-1,1]:
        for j in range(3):parts.append(m.cube('guard_'+str(side)+'_'+str(j),[side*(2+j*1.7),7+j*.8,0],[2.2,2.1,3-j*.6],0,[0,0,-side*22.5]))
    m.group('ea_grip',parts)
    cylinder_groups=[]
    for j in range(3):
        parts=[];y=12+j*7.5
        for i in range(8):
            a=math.tau*i/8;x,z=2.4*math.cos(a),2.4*math.sin(a)
            parts.append(m.cube(f'cylinder_{j}_{i}',[x,y,z],[1.7,6.8,1.7],2,[0,(i%2)*45,0]))
            parts.append(m.cube(f'red_rune_{j}_{i}',[x*1.17,y,z*1.17],[.55,4.8,.55],4 if i%2==0 else 3,[0,(i%2)*45,0]))
            parts.append(m.cube(f'gold_ring_{j}_{i}',[x,y+3.2,z],[1.9,.65,1.9],0,[0,(i%2)*45,0]))
        cylinder_groups.append(m.group('rotating_cylinder_'+str(j),parts,origin=(0,y,0)))
    children=m.data['outliner'][:];root=m.group('ea_root',[]);m.data['outliner']=[dict(uuid=root['uuid'],children=children)]
    spin=animation('spin',1,{})
    spin['loop']='loop'
    for j,g in enumerate(cylinder_groups):spin['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1,(0,360 if j%2==0 else -360,0))])
    level=animation('level',10,{})
    level['animators'][root['uuid']]=animator(root,[(0,(0,0,0)),(5.1,(0,0,0)),(5.7,(-90,0,0)),(10,(-90,0,0))])
    m.data['animations']=[spin,level];m.save()
    m=Model('motionrp_enuma_vortex',colors=colors);parts=[]
    parts.append(m.cube('white_energy_core',[0,0,0],[2.8,2.8,31],7))
    for i in range(16):
        a=i*math.pi/4;z=-14+i*1.8
        for strand in range(4):
            angle=a+strand*math.tau/4
            parts.append(m.cube(f'red_wind_{i}_{strand}',[7*math.cos(angle),7*math.sin(angle),z],[1.8,1.8,2.5],7 if strand==0 else 2 if strand==3 else 3))
    g=m.group('enuma_vortex',parts);a=animation('vortex',.8,{})
    a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(.8,(0,0,360))]);m.data['animations']=[a];m.save()

def charge_models():
    colors=[(255,195,54,255),(255,236,166,255),(28,6,22,255),(195,10,35,255),(255,48,65,255),(80,3,20,255),(255,250,221,255),(255,255,249,255)]
    m=Model('motionrp_enuma_charge',colors=colors);parts=[]
    for j in range(9):
        y=-4+j;size=max(1,2*math.sqrt(max(0,25-y*y)))
        parts.append(m.cube('white_core_'+str(j),[0,y,0],[size,1.1,size],7 if j%2 else 6))
    for j in range(18):
        a=math.tau*j/18
        parts.append(m.cube('gold_spark_'+str(j),[7*math.cos(a),7*math.sin(a),(j%3-1)*2],[1,2.5,1],0,[0,0,22.5 if j%2 else -22.5]))
    g=m.group('charge_core',parts);a=animation('spin',1,{})
    a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1,(0,0,360))]);m.data['animations']=[a];m.save()
    m=Model('motionrp_enuma_ring',colors=colors);parts=[]
    for ring,(radius,color) in enumerate([(5.5,1),(8,4),(10,2)]):
        for j in range(32):
            angle=math.tau*j/32
            parts.append(m.cube(f'ring_{ring}_{j}',[radius*math.cos(angle),radius*math.sin(angle),ring*.18],[1.2,1.2,.3],color))
    for j in range(8):
        a=j*math.pi/4
        parts.append(m.cube('red_fracture_'+str(j),[10*math.cos(a),10*math.sin(a),-.4],[.65,4.5,.3],3,[0,0,45 if j%2 else -45]))
    g=m.group('space_ring',parts);a=animation('spin',1.4,{})
    a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1.4,(0,0,-360))]);m.data['animations']=[a];m.save()
    m=Model('motionrp_enuma_rift',colors=colors);parts=[]
    for j in range(8):
        z=-14+j*4;x=(-1 if j%2 else 1)*(1+j%3)
        parts.append(m.cube('black_tear_'+str(j),[x,0,z],[3.2,.12,5.3],2))
        parts.append(m.cube('red_edge_'+str(j),[x-.5,.09,z],[1.1,.1,5.4],4))
        parts.append(m.cube('hot_edge_'+str(j),[x-1,.16,z],[.3,.08,5.4],6))
    m.group('ground_rift',parts);m.save()


def titan_skin(rig):
    skin=ROOT.parent/'skins/titan.png';raw=skin.read_bytes()
    if raw[:8]!=b'\x89PNG\r\n\x1a\n' or int.from_bytes(raw[16:20],'big')!=64 or int.from_bytes(raw[20:24],'big')!=64:raise ValueError('Titan skin must be a 64x64 PNG')
    model=copy.deepcopy(rig);model['name']=model['model_identifier']='motionrp_titan_body'
    player_model=json.loads((ROOT/'players/motionrp_titan_transform.bbmodel').read_text())
    model['animations']=[copy.deepcopy(next(a for a in player_model['animations'] if a['name']=='titan_idle'))]
    names={}
    for g in model['groups']:
        g['name']='titanbone'+g['name'].replace('_','');names[g['uuid']]=g['name']
    for a in model['animations']:
        for key,track in a['animators'].items():track['name']=names[key]
    tex=model['textures'][0];tex['name']='titan.png';tex['uuid']=ident('titan-skin');tex['source']='data:image/png;base64,'+base64.b64encode(raw).decode();tex['internal']=True
    for key in ['path','relative_path']:tex.pop(key,None)
    save('models/motionrp_titan_body.bbmodel',model)
    print('Embedded exact Titan skin SHA256:',hashlib.sha256(raw).hexdigest())

def main():
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned BetterModel 3.6.0 rig')
    rig=json.loads(raw)
    spread={
      'pra_right_arm':[(0,(0,0,0)),(.4,(-10,0,65)),(5.3,(-10,0,65)),(5.8,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.4,(-10,0,-65)),(5.3,(-10,0,-65)),(5.8,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.4,(-10,0,0)),(5.3,(-10,0,0)),(5.8,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(.4,(-10,0,0)),(5.3,(-10,0,0)),(5.8,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(.4,(-14,10,0)),(5.3,(-14,10,0)),(5.8,(0,0,0))]}
    player(rig,'gate_of_babylon_100',5.8,spread)
    poses={
      'player_root':[(0,(0,0,0)),(.5,(0,-18,0)),(11.8,(0,-18,0)),(13.6,(0,0,0))],
      'pra_right_arm':[(0,(0,0,0)),(.5,(-5,0,12)),(1.8,(-5,0,12)),(2.4,(-65,5,10)),(3.5,(-50,0,8)),(8.6,(-50,0,8)),(9.2,(-90,0,5)),(11,(-90,0,5)),(13.6,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.5,(-12,0,0)),(1.8,(-12,0,0)),(2.4,(-35,0,0)),(3.5,(-100,0,0)),(8.6,(-100,0,0)),(9.2,(0,0,0)),(11,(0,0,0)),(13.6,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.5,(-45,0,-28)),(2.8,(-45,0,-28)),(5.2,(3,0,-6)),(8.6,(3,0,-6)),(9.2,(10,0,-12)),(11,(10,0,-12)),(13.6,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(.5,(-10,0,0)),(5.2,(-15,0,0)),(11,(-15,0,0)),(13.6,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(.5,(-10,15,0)),(3.5,(-10,15,0)),(5.2,(-8,12,0)),(8.6,(-8,12,0)),(9.2,(-4,-2,0)),(11,(-4,-2,0)),(13.6,(0,0,0))],
      'pc_chest':[(0,(0,0,0)),(.5,(0,8,0)),(3.5,(0,8,0)),(5.2,(-3,-8,0)),(8.6,(-3,-8,0)),(9.2,(6,20,0)),(11,(6,20,0)),(13.6,(0,0,0))],
      'prl_right_leg':[(0,(0,0,0)),(.5,(3,12,0)),(11.8,(3,12,0)),(13.6,(0,0,0))],
      'prfl_right_foreleg':[(0,(0,0,0)),(.5,(-3,0,0)),(11.8,(-3,0,0)),(13.6,(0,0,0))],
      'pll_left_leg':[(0,(0,0,0)),(.5,(-9,-12,0)),(11.8,(-9,-12,0)),(13.6,(0,0,0))],
      'plfl_left_foreleg':[(0,(0,0,0)),(.5,(9,0,0)),(11.8,(9,0,0)),(13.6,(0,0,0))]}
    model=player(rig,'gate_of_babylon_enuma',13.6,poses)
    groups={g['name']:g for g in model['groups']}
    for name,side in [('prl_right_leg',1),('pll_left_leg',-1)]:
        g=groups[name];keys=[(0,(0,0,0)),(.5,(side,0,2.5 if side==1 else -2)),(8.6,(side,0,2.5 if side==1 else -2)),(9.2,(side,0,2.5 if side==1 else -3.5)),(11.8,(side,0,2.5 if side==1 else -3.5)),(13.6,(0,0,0))]
        model['animations'][0]['animators'][g['uuid']]['keyframes']+=animator(g,keys,'position')['keyframes']
        model['animations'][0]['animators'][g['uuid']]['keyframes'].sort(key=lambda frame:frame['time'])
    save('players/motionrp_gate_of_babylon_enuma.bbmodel',model)
    (ROOT/'gate_of_babylon_enuma-hand-path.csv').write_text(hand_path(model,272))
    charge_models()
    dense_gate();key_and_ea();titan_skin(rig)
if __name__=='__main__':main()
