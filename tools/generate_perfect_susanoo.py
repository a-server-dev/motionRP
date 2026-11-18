#!/usr/bin/env python3
"""Madara-inspired complete blue Susanoo: full armor, tengu helm, wings and paired swords.
Usage: python3 tools/generate_perfect_susanoo.py path/to/pinned/steve.bbmodel
"""
import sys
sys.dont_write_bytecode=True
import json,math,hashlib
from pathlib import Path
from collections import defaultdict
from generate_summons import Model,ROOT,RIG_SHA,player,save,animator,animation
BLUE=[(17,65,150,255),(164,242,255,255),(10,34,92,255),(222,255,255,255),
      (39,154,235,255),(73,169,245,255),(31,95,187,255),(101,218,255,255)]

class Armor:
    def __init__(self,name,ethereal=True):self.m=Model(name,ethereal,BLUE);self.parts=[]
    def cube(self,name,p,size,color=5,rotation=None):
        # Split long pieces before grouping so every exported Minecraft cube is legal.
        counts=[max(1,math.ceil(s/22)) for s in size];ids=[]
        for x in range(counts[0]):
            for y in range(counts[1]):
                for z in range(counts[2]):
                    indexes=[x,y,z];dims=[size[i]/counts[i] for i in range(3)]
                    center=[p[i]-size[i]/2+dims[i]*(indexes[i]+.5) for i in range(3)]
                    uid=self.m.cube(name+f'_{x}_{y}_{z}',center,dims,color,rotation);ids.append(uid);self.parts.append(uid)
        return ids
    def joint(self,name,pivot):
        elements={e['uuid']:e for e in self.m.data['elements']};bins=defaultdict(list)
        for uid in self.parts:
            e=elements[uid];center=[(a+b)/2 for a,b in zip(e['from'],e['to'])]
            bins[tuple(math.floor(v/16) for v in center)].append(uid)
        children=[]
        for i,ids in enumerate(bins.values()):
            lo=[min(elements[u]['from'][a] for u in ids) for a in range(3)];hi=[max(elements[u]['to'][a] for u in ids) for a in range(3)]
            assert all(b-a<=48 for a,b in zip(lo,hi)),(name,lo,hi)
            self.m.group(f'{name}_mesh_{i}',ids,origin=tuple((a+b)/2 for a,b in zip(lo,hi)));children.append(self.m.data['outliner'].pop())
        g=self.m.group(name,[],origin=pivot);self.m.data['outliner'][-1]['children']=children;self.parts=[];return g

def complete():
    a=Armor('motionrp_susanoo_armor');c=a.cube
    # Full boots, articulated greaves and thigh armor.
    for side in [-1,1]:
        x=side*9
        for j in range(4):
            c(f'boot_{side}_{j}',[x,1.5+j*1.3,-3+j*.8],[9-j*.4,1.5,15-j*1.4],6)
        for toe in range(3):
            c(f'claw_{side}_{toe}',[x-3+toe*3,1.1,-11],[1.6,1.8,5],7,[22.5,0,0])
        for j in range(6):
            y=8+j*3.3;w=7.8-j*.3
            c(f'shin_lamella_{side}_{j}',[x,y,0],[w,3.6,8.5],6 if j%2 else 5)
            c(f'shin_edge_{side}_{j}',[x,y+.8,-4.5],[w+.4,.65,.7],7)
            c(f'shin_ridge_{side}_{j}',[x,y,-4.9],[1,2.5,1],1)
        c(f'knee_shield_{side}',[x,29,-3],[10,7,8],5,[0,0,side*22.5])
        c(f'knee_diamond_{side}',[x,29,-7.3],[4,4,.8],7,[0,0,45])
        for j in range(4):
            c(f'thigh_plate_{side}_{j}',[x,34+j*4,0],[9.5,4.4,10],6 if j%2 else 5)
            c(f'thigh_trim_{side}_{j}',[x,35.5+j*4,-5.2],[9.7,.7,.8],7)
        a.joint(f'armored_leg_{side}',(x,51,0))
    # Broad lamellar skirt with an open center and separate side tassets.
    for side in [-1,1]:
        for j in range(6):
            y=39+j*3.1;w=10.5-j*.5
            c(f'front_tasset_{side}_{j}',[side*(7+(5-j)*.55),y,-6],[w,3.3,3],6)
            c(f'tasset_border_{side}_{j}',[side*(7+(5-j)*.55),y-1.2,-7.7],[w+.3,.5,.5],7)
            c(f'side_tasset_{side}_{j}',[side*(15+(5-j)*.4),y,2],[3.5,3.4,12],5)
            c(f'back_tasset_{side}_{j}',[side*8,y,7],[11,3.3,3],6)
        for j in range(4):c(f'tasset_lacing_{side}_{j}',[side*6,44+j*3,-8.2],[1.2,.7,.5],1)
    a.joint('hakama_and_tassets',(0,55,0))
    for j in range(8):
        y=57+j*3.2;w=20+min(j,5)*2.1
        c(f'breastplate_{j}',[0,y,0],[w,3.5,15],5 if j%2 else 6)
        c(f'chest_edge_{j}',[0,y+1,-7.8],[w+.5,.6,.8],7)
        for side in [-1,1]:
            c(f'chest_flute_{side}_{j}',[side*(w*.32),y,-8.4],[1.3,2.8,.8],1)
            c(f'breastplate_bevel_{side}_{j}',[side*w/2,y,-5.5],[2.3,3.5,4],4,[0,0,side*22.5])
    c('belt',[0,54,0],[25,4,16],0);c('belt_crest',[0,54,-8.5],[7,5,1.5],7)
    for side in [-1,1]:
        for j in range(4):c(f'collar_{side}_{j}',[side*(3+j*3.3),82-j*1.1,-5],[4.5,3,6],7,[0,0,-side*22.5])
    a.joint('azure_breastplate',(0,55,0))
    # Tengu mask: layered cheeks, luminous eye slits, long pointed nose and swept horns.
    for j,w in enumerate([8,12,16,19,18,15]):c(f'tengu_face_contour_{j}',[0,88+j*2.3,-1],[w,2.5,12],6)
    for side in [-1,1]:
        c(f'temple_{side}',[side*9,99,1],[4,14,13],5)
        c(f'eye_slit_{side}',[side*4.6,99,-7.3],[6,.9,.8],3,[0,0,side*22.5])
        c(f'angry_brow_{side}',[side*4.6,101,-7.4],[7,1.5,1],7,[0,0,side*22.5])
        for j in range(4):c(f'swept_cheek_{side}_{j}',[side*(5.5+j*1.3),90+j*1.5,-6.5],[2.5,5.5,2],4,[0,0,-side*22.5])
        c(f'fang_{side}',[side*3.5,88,-7.8],[1.5,4,1.4],3)
        for j in range(5):
            c(f'helmet_horn_{side}_{j}',[side*(9+j*1.7),105+j*2.7,1+j*.7],[4-j*.55,4.4,4-j*.5],7,[0,0,-side*22.5])
        for j in range(4):c(f'helmet_neck_guard_{side}_{j}',[side*(8+j*.5),87+j*2.6,5],[4.5,3,11],6)
    for j in range(8):
        c(f'tengu_long_nose_{j}',[0,97-j*.8,-7.8-j*1.45],[4.5-j*.48,2.2,2.3],7 if j%2 else 4)
    c('helmet_crown',[0,105,1],[15,4,11],6)
    c('forehead_pilot_gem',[0,103,-6.5],[4.3,5.4,1.5],3,[0,0,45])
    for tooth in range(5):c(f'mask_tooth_{tooth}',[-4+tooth*2,88.8,-7.8],[1,1.5,.9],7)
    a.joint('tengu_helmet',(0,85,0))
    arm_joints=[]
    for side in [-1,1]:
        x=side*28
        for j in range(5):
            c(f'shoulder_scale_{side}_{j}',[side*(21+j*.7),83-j*2.1,1],[15+j*.7,2.4,19-j*.5],6 if j%2 else 5,[0,0,-side*22.5])
            c(f'shoulder_trim_{side}_{j}',[side*(21+j*.7),83.8-j*2.1,-8.5+j*.25],[15+j*.7,.6,.9],7,[0,0,-side*22.5])
        c(f'upper_arm_{side}',[x,69,0],[8,19,9],5)
        for j in range(5):
            c(f'forearm_lamella_{side}_{j}',[x,50+j*3.2,-1],[10,3.5,11],6 if j%2 else 5)
            c(f'bracer_ring_{side}_{j}',[x,51+j*3.2,-6.8],[10.5,.8,.8],7)
        c(f'palm_{side}',[x,44,-2],[8,7,7],6)
        for digit in range(4):
            c(f'finger_{side}_{digit}',[x-3+digit*2,40.5,-4],[1.5,6,2.5],7)
            c(f'finger_joint_{side}_{digit}',[x-3+digit*2,42.5,-5.5],[1.6,1.5,1.5],1)
        # Katana points forward; the shoulder raises it into the windup.
        c(f'katana_hilt_{side}',[x,44,-9],[2.2,2.2,13],0)
        for j in range(6):c(f'hilt_wrap_{side}_{j}',[x,44,-4-j*1.8],[2.7,2.7,.6],7)
        c(f'katana_guard_{side}',[x,44,-16],[12,3,2.5],7)
        for j in range(18):
            t=j/17;w=5.1*(1-.85*t*t);z=-19-j*3.1;curve=side*3*t*t
            c(f'katana_blade_{side}_{j}',[x+curve,44,z],[w,1.1,3.5],5)
            c(f'katana_edge_{side}_{j}',[x+curve-side*w/2,44,z],[.55,1.5,3.5],3)
            if j%3==0:c(f'blade_hamon_{side}_{j}',[x+curve,44.7,z],[w*.45,.3,2],1)
        arm_joints.append(a.joint('sword_arm' if side==1 else 'guard_arm',(side*22,81,0)))
    # Rear arms and paired sheaths at the wing roots distinguish the complete form.
    for side in [-1,1]:
        for j in range(5):
            c(f'rear_arm_{side}_{j}',[side*(17+j*2.2),75-j*3,11],[6,5,7],5)
            c(f'rear_arm_joint_{side}_{j}',[side*(17+j*2.2),75-j*3,15],[4,2,2],7)
        for j in range(12):c(f'paired_scabbard_{side}_{j}',[side*25,58+j*.6,17+j*3],[4,4,3.5],6)
        a.joint(f'rear_arm_and_scabbard_{side}',(side*15,78,10))
    wings=[]
    for side in [-1,1]:
        for j in range(10):
            c(f'wing_spar_{side}_{j}',[side*(19+j*6),81+j*1.7,10+j*.6],[8,4,5],7)
        for feather in range(14):
            base=26+feather*4.2;y=85+feather*.9;z=11+feather*.35
            for j in range(7):
                t=j/6;xx=side*(base+10*t);yy=y-(13+feather*1.8)*t;zz=z+8*t;w=5*(1-.78*t)
                c(f'wing_feather_{side}_{feather}_{j}',[xx,yy,zz],[w,5.4,1.6],5 if feather%2 else 6,[0,0,-side*22.5])
                c(f'feather_spine_{side}_{feather}_{j}',[xx,yy,zz-.95],[.65,5.5,.6],7,[0,0,-side*22.5])
        wings.append(a.joint('right_wing' if side==1 else 'left_wing',(side*17,82,10)))
    slash=animation('slash',5.2,{})
    for side,g in zip([-1,1],arm_joints):
        keys=[(0,(0,0,0)),(1.5,(55,-side*15,-side*12)),(2,(55,-side*15,-side*12)),(2.6,(-10,side*25,side*12)),(3.5,(-10,side*25,side*12)),(5.2,(0,0,0))]
        slash['animators'][g['uuid']]=animator(g,keys)
    unfold=animation('unfurl',3.4,{})
    for side,g in zip([-1,1],wings):unfold['animators'][g['uuid']]=animator(g,[(0,(0,side*65,side*28)),(1,(0,side*35,side*15)),(3.4,(0,0,0))])
    a.m.data['animations']=[slash,unfold];a.m.save()

def scaffold():
    a=Armor('motionrp_susanoo_ribs')
    for level in range(6):
        for i in range(16):
            theta=math.pi*i/15
            a.cube(f'blue_chakra_rib_{level}_{i}',[14*math.cos(theta),7+level*6,9*math.sin(theta)],[3,1.7,3],7)
    a.cube('blue_spine',[0,23,9],[3,42,3],7);a.cube('blue_pelvis',[0,3,2],[24,4,12],5);a.joint('ribcage',(0,0,0));a.m.save()
    a=Armor('motionrp_susanoo_slash',False)
    for j in range(36):
        theta=math.radians(-85+j*170/35);r=23
        a.cube(f'azure_crescent_{j}',[r*math.cos(theta),r*math.sin(theta),0],[2.2,3.2,.8],7)
        a.cube(f'white_crescent_edge_{j}',[(r+1.2)*math.cos(theta),(r+1.2)*math.sin(theta),-.5],[.8,2.8,.5],3)
    a.joint('crescent',(0,0,0));a.m.save()

def generate(rig):
    player(rig,'susanoo',11.8,{
      'pra_right_arm':[(0,(0,0,0)),(.5,(-72,0,-25)),(2,(-72,0,-25)),(3,(-30,0,-38)),(9.8,(-30,0,-38)),(11.8,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(.5,(-65,0,0)),(2,(-65,0,0)),(3,(-70,0,-30)),(9.8,(-70,0,-30)),(11.8,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(.5,(-72,0,25)),(2,(-72,0,25)),(3,(-30,0,38)),(9.8,(-30,0,38)),(11.8,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(.5,(-65,0,0)),(2,(-65,0,0)),(3,(-70,0,30)),(9.8,(-70,0,30)),(11.8,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(2,(-8,0,0)),(9.8,(-8,0,0)),(11.8,(0,0,0))]})
    scaffold();complete()

if __name__=='__main__':
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned BetterModel rig')
    generate(json.loads(raw))
