#!/usr/bin/env python3
"""Madara-inspired complete blue Susanoo: full armor, tengu helm, wings and paired swords.
Usage: python3 tools/generate_perfect_susanoo.py path/to/pinned/steve.bbmodel
"""
import sys
sys.dont_write_bytecode=True
import json,math,hashlib
from pathlib import Path
from collections import defaultdict
from hollow_kinematics import matrix,mul,vector,euler
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
        for j in range(7):
            for sx in [-1,1]:
                c(f'greave_rivet_{side}_{j}_{sx}',[x+sx*3.1,9+j*5,-5.1],[.65,.65,.65],1)
                c(f'ankle_lace_{side}_{j}_{sx}',[x+sx*2,3+j*.7,-10.9],[.5,.45,.4],0)
        c(f'knee_engraving_{side}',[x,29,-7.9],[.65,4,.45],1)
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
    for side in [-1,1]:
        for row in range(7):
            for col in range(4):
                c(f'cuirass_rivet_{side}_{row}_{col}',[side*(3+col*3.4),58+row*3.2,-8.8],[.55,.55,.5],1)
        c(f'chest_inner_ridge_{side}',[side*4,69,-8.8],[.7,19,.6],0)
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
        x=side*24
        for j in range(5):
            c(f'shoulder_scale_{side}_{j}',[side*(21+j*.7),83-j*2.1,1],[17+j*.9,3.1,20-j*.4],6 if j%2 else 5,[0,0,-side*22.5])
            c(f'shoulder_trim_{side}_{j}',[side*(21+j*.7),83.8-j*2.1,-8.5+j*.25],[15+j*.7,.6,.9],7,[0,0,-side*22.5])
        for row in range(4):
            for col in range(5):c(f'pauldron_lacing_{side}_{row}_{col}',[side*(16+col*3),82-row*2,-9.2],[.65,.65,.7],1)
        a.joint(f'pauldron_{side}',(side*22,81,0))
        c(f'upper_arm_{side}',[x,71,0],[8,20,9],5)
        upper_parts=a.parts[:];a.parts=[]
        for j in range(6):
            c(f'forearm_lamella_{side}_{j}',[x,42+j*3.2,-1],[10,3.5,11],6 if j%2 else 5)
            c(f'bracer_ring_{side}_{j}',[x,43+j*3.2,-6.8],[10.5,.8,.8],7)
        c(f'bracer_center_plate_{side}',[x,49,-7.2],[6,13,1.8],0)
        c(f'bracer_inlaid_diamond_{side}',[x,50,-8.3],[3.5,5,.6],4,[0,0,45])
        for row in range(6):
            for edge in [-1,1]:c(f'bracer_stud_{side}_{row}_{edge}',[x+edge*4.3,43+row*3.2,-7],[.6,.6,.65],1)
        c(f'palm_{side}',[x,36,0],[8,7,7],6)
        for digit in range(4):
            c(f'finger_{side}_{digit}',[x-3+digit*2,32.5,-2],[1.5,6,2.5],7)
            c(f'finger_joint_{side}_{digit}',[x-3+digit*2,34.5,-3.5],[1.6,1.5,1.5],1)
        fore=a.joint(f'forearm_{side}',(x,61,0));node=a.m.data['outliner'].pop()
        a.parts=upper_parts;upper=a.joint('sword_arm' if side==1 else 'guard_arm',(x,81,0));a.m.data['outliner'][-1]['children'].append(node)
        arm_joints.append((side,upper,fore))
    # Both swords begin sheathed at the waist. Only the left blade is drawn.
    weapons={}
    for side in [-1,1]:
        for j in range(18):
            z=-10-j*2.9;t=j/17;w=5.1*(1-.85*t*t)
            c(f'katana_blade_{side}_{j}',[side*2*t*t,0,z],[w,1.1,3.2],5)
            c(f'katana_edge_{side}_{j}',[side*2*t*t-w/2,0,z],[.55,1.5,3.2],3)
            if j%3==0:c(f'blade_hamon_{side}_{j}',[side*2*t*t,.7,z],[w*.45,.3,2],1)
        c(f'katana_hilt_{side}',[0,0,0],[2.2,2.2,13],0)
        for j in range(6):c(f'hilt_wrap_{side}_{j}',[0,0,-5+j*1.8],[2.7,2.7,.6],7)
        c(f'katana_guard_{side}',[0,0,-7],[12,3,2.5],7)
        weapons[f'blade_{side}']=a.joint(f'waist_blade_{side}',(0,0,0))
        for j in range(18):
            z=-10-j*2.9;w=6*(1-.7*(j/17)**2)
            c(f'paired_scabbard_{side}_{j}',[0,0,z],[w,3,3.3],0)
            c(f'scabbard_inlay_{side}_{j}',[0,-1.6,z],[1.2,.4,3.3],7)
        c(f'scabbard_mouth_{side}',[0,0,-8],[7,4,2],7)
        weapons[f'sheath_{side}']=a.joint(f'waist_scabbard_{side}',(0,0,0))
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
    roots=a.m.data['outliner'][:];a.m.data['outliner']=roots[:3];a.m.torso=a.m.group('battle_torso',[],origin=(0,57,0));a.m.data['outliner'][-1]['children']=roots[3:]
    slash=animation('slash',5.2,{})
    choreograph(a.m,slash,arm_joints,weapons)
    unfold=animation('unfurl',3.4,{})
    for side,g in zip([-1,1],wings):unfold['animators'][g['uuid']]=animator(g,[(0,(0,side*65,side*28)),(1,(0,side*35,side*15)),(3.4,(0,0,0))])
    unfold['animators'].update(a.m.idle_tracks)
    a.m.data['animations']=[slash,unfold];a.m.save()

def interp(keys,t):
    lo=max((k for k in keys if k[0]<=t),key=lambda k:k[0],default=keys[0]);hi=min((k for k in keys if k[0]>=t),key=lambda k:k[0],default=keys[-1]);f=(t-lo[0])/(hi[0]-lo[0]) if hi[0]!=lo[0] else 0
    return [a*(1-f)+b*f for a,b in zip(lo[1],hi[1])]
def normalized(v):
    d=math.sqrt(sum(x*x for x in v));return [x/d for x in v]
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def direction_rotation(direction,down=False):
    axis=normalized(direction);y=[-v for v in axis] if down else [0,1,0]
    if down:
        z=normalized([0,-y[2],y[1]]) if abs(y[1])+abs(y[2])>.001 else [0,0,1];x=normalized(cross(y,z));z=cross(x,y)
    else:
        z=[-v for v in axis];ref=[0,1,0] if abs(z[1])<.95 else [0,0,1];x=normalized(cross(ref,z));y=cross(z,x)
    return [[x[i],y[i],z[i]] for i in range(3)]
def transpose(m):return [list(v) for v in zip(*m)]
def continuous(keys):
    out=[]
    for t,rot in keys:
        if out:
            for i in range(3):
                while rot[i]-out[-1][1][i]>180:rot[i]-=360
                while rot[i]-out[-1][1][i]<-180:rot[i]+=360
        out.append((t,rot))
    return out
def choreograph(m,slash,arms,weapons):
    hilt=[(0,(-18,59,-7)),(.45,(-18,59,-7)),(.8,(-7,68,-12)),(1.15,(26,72,-32)),(1.3,(26,72,-32)),(1.5,(43,65,-8)),(1.8,(30,60,-10)),(2.05,(34,78,-18)),(2.3,(18,97,12)),(2.7,(10,112,10)),(3.3,(10,112,10)),(3.5,(-6,58,-20)),(4.2,(-6,58,-20)),(5.2,(24,44,-2))]
    axis=[(0,(0,-.24,.97)),(.45,(0,-.24,.97)),(.8,(-.8,0,.6)),(1.3,(-.8,0,.6)),(1.5,(.98,-.04,-.2)),(1.8,(.85,-.12,-.5)),(2.3,(.05,.5,.87)),(2.7,(.15,.8,.58)),(3.3,(.15,.8,.58)),(3.5,(-.55,-.45,-.70)),(4.2,(-.55,-.45,-.70)),(5.2,(0,-.2,-.98))]
    torso=[(0,(0,-12,0)),(.8,(0,-22,-4)),(1.15,(-4,-38,-6)),(1.3,(-4,-38,-6)),(1.5,(12,40,8)),(1.8,(8,35,5)),(2.25,(-6,10,0)),(2.7,(-14,-20,-6)),(3.3,(-18,-28,-6)),(3.5,(22,30,12)),(4.2,(14,22,8)),(5.2,(0,0,0))]
    torso=[(t,(x*(.15 if 2.25<=t<=4.2 else .3),y*(.25 if 2.25<=t<=4.2 else .4),z*.2)) for t,(x,y,z) in torso]
    sheath=[(0,(-18,59,-7)),(.45,(-18,59,-7)),(.8,(-7,68,-12)),(1.15,(-31,67,9)),(1.8,(-31,67,9)),(2.3,(-18,59,-7)),(5.2,(-18,59,-7))]
    sheath_axis=[(0,(0,-.24,.97)),(.45,(0,-.24,.97)),(.8,(-.8,0,.6)),(1.8,(-.8,0,.6)),(2.3,(0,-.24,.97)),(5.2,(0,-.24,.97))]
    tracks={}
    def track(g,ch,keys):
        # Keep raw source Euler angles: these are model joints, not the player rig.
        part=animator(g,continuous(keys) if ch=='rotation' else keys,ch)
        target=tracks.setdefault(g['uuid'],{'name':g['name'],'type':'bone','keyframes':[]});target['keyframes']+=part['keyframes'];target['keyframes'].sort(key=lambda k:k['time'])
    for side,upper,fore in arms:
        ur=[];fr=[]
        for tick in range(105):
            t=tick/20;h=interp(hilt,t);d=normalized(interp(axis,t));sh=interp(sheath,t);sd=normalized(interp(sheath_axis,t))
            if side==1:hand=interp([(0,(26,44,-2)),(.45,(-12,63,-7)),(.8,(-7,68,-12))],t) if t<.8 else h
            else:hand=interp([(0,(-26,44,-2)),(.45,(-18,59,-7)),(.8,tuple(sh[i]+sd[i]*14 for i in range(3)))],t) if t<.8 else [sh[i]+sd[i]*14 for i in range(3)]
            shoulder=[side*24,81,0];delta=[hand[i]-shoulder[i] for i in range(3)];dist=math.sqrt(sum(v*v for v in delta));assert 4.999<dist<45.01,(side,t,dist)
            aim=normalized(delta);along=(20*20-25*25+dist*dist)/(2*dist);height=math.sqrt(max(0,400-along*along));ref=[side,0,.45];dot=sum(ref[i]*aim[i] for i in range(3));bend=normalized([ref[i]-dot*aim[i] for i in range(3)])
            elbow=[shoulder[i]+aim[i]*along+bend[i]*height for i in range(3)]
            a=direction_rotation([elbow[i]-shoulder[i] for i in range(3)],True);b=direction_rotation([hand[i]-elbow[i] for i in range(3)],True)
            ur.append((t,euler(a)));fr.append((t,euler(mul(transpose(a),b))))
        track(upper,'rotation',ur);track(fore,'rotation',fr)
    for name,g in weapons.items():
        positions=[];rotations=[]
        for tick in range(105):
            t=tick/20
            if name.endswith('_1'):p=[18,59,-7];d=[0,-.24,.97]
            else:p=interp(sheath if name.startswith('sheath') else hilt,t);d=interp(sheath_axis if name.startswith('sheath') else axis,t)
            positions.append((t,p));rotations.append((t,euler(direction_rotation(d))))
        track(g,'position',positions);track(g,'rotation',rotations)
    track(m.torso,'rotation',torso)
    slash['animators']=tracks
    rows=[]
    for tick in range(236):
        angles=interp(torso,max(0,(tick-100)/20));p=vector(matrix(*angles),[0,25.8,-7.44]);p[1]+=57
        rows.append(','.join(f'{v:.8f}' for v in [p[0]*10/16,-p[2]*10/16,p[1]*10/16]))
    (ROOT/'susanoo-pilot-path.csv').write_text('\n'.join(rows)+'\n')
    # An explicit held idle clip keeps both weapons at the waist during formation.
    m.idle_tracks={uid:{**track,'keyframes':[dict(k,time=0) for k in track['keyframes'] if k['time']==0]} for uid,track in tracks.items()}

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
