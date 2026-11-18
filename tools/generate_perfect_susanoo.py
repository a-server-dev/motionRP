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
    a=Armor('motionrp_susanoo_armor',False);c=a.cube
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
    for side in [-1,1]:
        for fold in range(3):
            c(f'hakama_long_fold_{side}_{fold}',[side*(5+fold*4),43,-8.1],[2.5,17,1.1],0 if fold%2 else 4)
        c(f'waist_knot_{side}',[side*3,53,-10],[5,3,3],0,[0,0,side*22.5])
    a.joint('hakama_and_tassets',(0,55,0))
    for j in range(8):
        y=57+j*3.2;w=20+min(j,5)*2.1
        c(f'breastplate_{j}',[0,y,0],[w,3.5,15],5 if j%2 else 6)
        c(f'chest_edge_{j}',[0,y+1,-7.8],[w+.5,.6,.8],7)
        for side in [-1,1]:
            c(f'chest_flute_{side}_{j}',[side*(w*.32),y,-8.4],[1.3,2.8,.8],1)
            c(f'breastplate_bevel_{side}_{j}',[side*w/2,y,-5.5],[2.3,3.5,4],4,[0,0,side*22.5])
    c('neck_chakra_core',[0,84,-1.7],[12,10,11],5)
    c('neck_collar_edge',[0,83,-7.6],[13,1.5,1],7)
    c('belt',[0,54,0],[25,4,16],0);c('belt_crest',[0,54,-8.5],[7,5,1.5],7)
    for side in [-1,1]:
        for j in range(4):c(f'collar_{side}_{j}',[side*(3+j*3.3),82-j*1.1,-5],[4.5,3,6],7,[0,0,-side*22.5])
    for side in [-1,1]:
        for j in range(10):
            t=j/9
            c(f'crossed_chest_lapel_{side}_{j}',[side*(12*(1-t)),80-24*t,-9],[4.4,3.5,1.5],5,[0,0,-side*22.5])
            c(f'lapel_seam_{side}_{j}',[side*(12*(1-t)+1.1),80-24*t,-9.9],[.5,3.6,.3],0,[0,0,-side*22.5])
    a.joint('azure_breastplate',(0,55,0))
    # Sculpted menpo: recessed eyes, overhanging brows, fangs and a tapered long nose.
    for j,w in enumerate([8,11,14,17,17,15]):
        c(f'tengu_face_contour_{j}',[0,89+j*2.4,-.5],[w,2.7,11.5],0)
    for side in [-1,1]:
        c(f'temple_{side}',[side*8.7,99,1.5],[3.5,12,12],4)
        c(f'recessed_eye_{side}',[side*4.5,99,-6.6],[6.2,2.3,1],2,[0,0,side*22.5])
        c(f'eye_slit_{side}',[side*4.5,99,-7.25],[3.1,.6,.35],1,[0,0,side*22.5])
        c(f'eye_pupil_{side}',[side*4.4,99,-7.5],[.65,1.0,.25],3)
        for j in range(5):
            t=j/4
            c(f'overhanging_brow_{side}_{j}',[side*(1.5+j*1.4),100.1+1.4*math.sin(t*math.pi),-7.4],[2.1,1.5,1.8],4,[0,0,side*22.5])
            c(f'brow_ridge_{side}_{j}',[side*(1.5+j*1.4),100.8+1.4*math.sin(t*math.pi),-8.1],[1.8,.4,.4],4)
        for j in range(9):
            t=j/8;x=side*(6+5*math.sin(t*math.pi*.7));y=97-8*t+2*t*t
            c(f'flame_cheek_{side}_{j}',[x,y,-5.9+2*t],[2.5*(1-.6*t),2.4,2.3],4,[0,0,-side*22.5])
            c(f'cheek_cut_{side}_{j}',[x-side*.5,y,-7.15+2*t],[.55,2.3,.4],0,[0,0,-side*22.5])
        for j in range(5):
            c(f'mandible_{side}_{j}',[side*(3+j*.8),88.8+j*.7,-5.7],[2.4,2.3,3.8],0,[0,0,side*22.5])
        for j in range(4):
            c(f'curved_fang_{side}_{j}',[side*(3.9-j*.25),91-j*.95,-8-j*.25],[1.3-j*.2,1.4,1.2],7)
        for j in range(11):
            t=j/10;x=side*(9+9*t-3*t*t);y=104+14*math.sin(t*math.pi/2);z=1+4*t
            c(f'swept_helmet_horn_{side}_{j}',[x,y,z],[max(.3,4.2*(1-t)),2.3,max(.35,4*(1-t))],4,[0,0,-side*22.5])
            c(f'horn_bevel_{side}_{j}',[x-side*.4,y,z-1.1*(1-t)],[.45,2.2,.5],4)
        for tuft in range(3):
            for j in range(6):
                t=j/5
                c(f'flame_crown_{side}_{tuft}_{j}',[side*(3+tuft*2.6+6*math.sin(t*math.pi/2)-4*t*t),103+tuft*.8+(6+tuft*1.4)*math.sin(t*math.pi*.8),-3+4*t],
                  [max(.3,4.6*(1-t)),1.9,2.8*(1-t)+.3],4,[0,0,-side*22.5])
                c(f'crown_carved_edge_{side}_{tuft}_{j}',[side*(2.6+tuft*2.6+6*math.sin(t*math.pi/2)-4*t*t),103+tuft*.8+(6+tuft*1.4)*math.sin(t*math.pi*.8),-4.2+4*t],[.25,1.5,.3],0)
        for j in range(4):c(f'helmet_neck_guard_{side}_{j}',[side*(8+j*.5),89+j*2.8,5],[4.5,3.2,11],6)
    for j in range(15):
        t=j/14
        c(f'tengu_long_nose_{j}',[0,97.6-5*t,-7.5-13*t],[max(.3,4.2*(1-t)),1.6,1.9],4)
        c(f'nose_carved_keel_{j}',[0,98.1-5*t,-8-13*t],[.45,1.1,1.7],0)
    c('mouth_shadow',[0,91,-6.6],[8,3,1.2],2)
    for j in range(7):
        x=-4.2+j*1.4
        c(f'upper_lip_{j}',[x,92.5+abs(x)*.13,-7.5],[1.6,1,1.2],4)
        c(f'lower_jaw_{j}',[x,88.6+abs(x)*.1,-6.8],[1.6,1.7,2],4)
        if j%2==0:c(f'mask_tooth_{j}',[x,91.7,-7.3],[.7,1.1,.65],7)
    c('helmet_crown',[0,105,2],[14,4,10],0)
    c('forehead_gem_frame',[0,103,-6.3],[4.2,5.3,1],0,[0,0,45])
    c('forehead_diamond_plate',[0,103,-7.1],[2.4,3.6,.6],4,[0,0,45])
    c('forehead_carved_line',[0,103,-7.5],[.3,3.2,.2],0)
    a.joint('tengu_helmet',(0,85,0))
    arm_joints=[]
    for side in [-1,1]:
        x=side*28
        for j in range(5):
            c(f'shoulder_scale_{side}_{j}',[side*(21+j*.7),83-j*2.1,1],[17+j*.9,3.1,20-j*.4],6 if j%2 else 5,[0,0,-side*22.5])
            c(f'shoulder_trim_{side}_{j}',[side*(21+j*.7),83.8-j*2.1,-8.5+j*.25],[15+j*.7,.6,.9],7,[0,0,-side*22.5])
        for j in range(3):
            c(f'pauldron_crest_{side}_{j}',[side*(19+j*4),86+j*.8,-1],[5.5,5-j*.7,15],0,[0,0,-side*22.5])
        c(f'bracer_center_plate_{side}',[x,57,-7.2],[6,13,1.8],0)
        c(f'bracer_inlaid_diamond_{side}',[x,58,-8.3],[3.5,5,.6],4,[0,0,45])
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
        keys=([(0,(0,0,0)),(1.4,(0,-65,-12)),(2.2,(0,65,12)),(3.2,(0,65,12)),(5.2,(0,0,0))] if side==-1 else
              [(0,(0,0,0)),(2.2,(0,0,0)),(3.2,(70,-20,-35)),(4.2,(-35,25,25)),(5.2,(0,0,0))])
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
    g=a.joint('crescent',(0,0,0))
    for name,rot in [('sweep',(90,0,0)),('kesa',(0,0,-45))]:
        an=animation(name,1,{});an['animators'][g['uuid']]=animator(g,[(0,rot),(1,rot)]);a.m.data['animations'].append(an)
    a.m.save()

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
