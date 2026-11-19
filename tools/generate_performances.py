#!/usr/bin/env python3
"""Seven command-only celebrations, with the player's own skin and planted hip/foot joints."""
import sys
sys.dont_write_bytecode=True
import copy,hashlib,json,math
from pathlib import Path
from generate_summons import RIG_SHA,save,animation,animator,player_animator

def sample(keys,t):
    a=max((k for k in keys if k[0]<=t),default=keys[0],key=lambda k:k[0]);b=min((k for k in keys if k[0]>=t),default=keys[-1],key=lambda k:k[0])
    f=(t-a[0])/(b[0]-a[0]) if b[0]!=a[0] else 0
    return [x*(1-f)+y*f for x,y in zip(a[1],b[1])]
def generate(rig):
    m=copy.deepcopy(rig);m['name']=m['model_identifier']='motionrp_performances';groups={g['name']:g for g in m['groups']};m['animations']=[]
    def make(name,end,poses,legs,jump=None,turn=None):
        a=animation(name,end/20,{})
        for bone,keys in poses.items():a['animators'][groups[bone]['uuid']]=player_animator(groups[bone],[(t/20,v) for t,v in keys])
        legkeys={k:[] for k in ['prl_right_leg','prfl_right_foreleg','pll_left_leg','plfl_left_foreleg']};drop=[];rot=[]
        for tick in range(end+1):
            r,kr,l,kl,spread=sample(legs,tick)
            feet=[11.25-5.625*math.cos(math.radians(spread))*(math.cos(math.radians(u))+math.cos(math.radians(u+k))) for u,k in [(r,kr),(l,kl)]]
            y=(jump(tick) if jump else 0)-min(feet);drop.append((tick/20,(0,y,0)))
            for bone,value in [('prl_right_leg',(r,0,spread)),('prfl_right_foreleg',(kr,0,0)),('pll_left_leg',(l,0,-spread)),('plfl_left_foreleg',(kl,0,0))]:legkeys[bone].append((tick/20,value))
            if turn:rot.append((tick/20,(0,turn(tick),0)))
        for bone,keys in legkeys.items():a['animators'][groups[bone]['uuid']]=player_animator(groups[bone],keys)
        root=groups['player_root'];track=animator(root,drop,'position')
        if rot:track['keyframes']+=animator(root,rot)['keyframes']
        track['keyframes'].sort(key=lambda f:f['time'])
        a['animators'][root['uuid']]=track;m['animations'].append(a)
    make('ronaldo',112,{
      'pra_right_arm':[(0,(0,0,0)),(10,(-50,0,15)),(18,(25,0,12)),(28,(-165,0,15)),(36,(-165,0,15)),(46,(10,0,38)),(90,(10,0,38)),(112,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(10,(25,0,-12)),(18,(-50,0,-15)),(28,(-165,0,-15)),(36,(-165,0,-15)),(46,(10,0,-38)),(90,(10,0,-38)),(112,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(18,(-30,0,0)),(30,(-10,0,0)),(46,(0,0,0)),(112,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(18,(-30,0,0)),(30,(-10,0,0)),(46,(0,0,0)),(112,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(28,(-12,0,0)),(46,(-8,0,0)),(90,(-8,0,0)),(112,(0,0,0))]},
      [(0,(0,0,0,0,0)),(8,(-30,45,25,10,6)),(16,(25,10,-30,45,6)),(24,(-35,70,-35,70,10)),(36,(-45,70,-25,50,12)),(46,(-35,70,-35,70,26)),(54,(-12,24,-12,24,26)),(90,(-12,24,-12,24,26)),(112,(0,0,0,0,0))],
      None,
      lambda t:180*max(0,min(1,(t-24)/22)))
    make('kneeslide',100,{
      'pra_right_arm':[(0,(0,0,0)),(8,(-45,0,12)),(16,(25,0,12)),(23,(-30,0,55)),(32,(5,0,65)),(68,(5,0,65)),(100,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(8,(25,0,-12)),(16,(-45,0,-12)),(23,(-30,0,-55)),(32,(5,0,-65)),(68,(5,0,-65)),(100,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(16,(-35,0,0)),(32,(-15,0,0)),(68,(-15,0,0)),(100,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(16,(-35,0,0)),(32,(-15,0,0)),(68,(-15,0,0)),(100,(0,0,0))],
      'pw_waist':[(0,(0,0,0)),(24,(12,0,0)),(60,(12,0,0)),(100,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(24,(-18,0,0)),(68,(-18,0,0)),(100,(0,0,0))]},
      [(0,(0,0,0,0,0)),(8,(-30,45,25,10,5)),(16,(25,10,-30,45,5)),(20,(-15,60,-15,60,6)),(26,(0,90,0,90,6)),(68,(0,90,0,90,6)),(86,(-30,60,-30,60,8)),(100,(0,0,0,0,0))])
    make('mbappe',100,{
      'pra_right_arm':[(0,(0,0,0)),(14,(-45,0,25)),(28,(-111.7934,41.9191,76.0437)),(82,(-111.7934,41.9191,76.0437)),(100,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(14,(-50,0,0)),(28,(-53.2183,0,0)),(82,(-53.2183,0,0)),(100,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(14,(-45,0,-25)),(28,(-126.1255,-22.6459,-103.4792)),(82,(-126.1255,-22.6459,-103.4792)),(100,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(14,(-50,0,0)),(28,(-44.3461,0,0)),(82,(-44.3461,0,0)),(100,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(28,(-10,0,0)),(82,(-10,0,0)),(100,(0,0,0))]},
      [(0,(0,0,0,0,0)),(12,(-18,36,-18,36,6)),(26,(-20,40,-20,40,14)),(32,(-8,16,-8,16,14)),(82,(-8,16,-8,16,14)),(100,(0,0,0,0,0))],lambda t:4*math.sin(math.pi*(t-12)/14) if 12<t<26 else 0)
    make('yamakawa',112,{
      'pra_right_arm':[(0,(0,0,0)),(18,(-20,0,55)),(28,(-165,0,-42)),(62,(-165,0,-42)),(82,(-50,0,30)),(112,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(18,(-20,0,-55)),(28,(-155,0,40)),(62,(-155,0,40)),(82,(-50,0,-30)),(112,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(18,(-25,0,0)),(28,(-10,0,0)),(62,(-10,0,0)),(112,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(18,(-25,0,0)),(28,(-10,0,0)),(62,(-10,0,0)),(112,(0,0,0))],
      'pw_waist':[(0,(0,0,0)),(28,(0,0,6)),(62,(0,0,6)),(112,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(28,(-15,0,0)),(62,(-15,0,0)),(112,(0,0,0))]},
      [(0,(0,0,0,0,0)),(18,(-20,40,-20,40,25)),(28,(-50,100,-50,100,38)),(62,(-50,100,-50,100,38)),(82,(-15,30,-15,30,20)),(112,(0,0,0,0,0))])
    # Continuous shoulder circles, then a held finish. Euler keys stay unwrapped.
    poses={b:[(0,(0,0,0))] for b in ['pra_right_arm','pla_left_arm','prfa_right_forearm','plfa_left_forearm','pw_waist','pc_chest','h_ph_head']}
    for t in range(20,117,4):
        phase=(t-20)*360/32;beat=math.sin((t-20)*math.tau/16)
        poses['pra_right_arm'].append((t,(phase,0,25*math.cos(math.radians(phase)))))
        poses['pla_left_arm'].append((t,(phase+180,0,-25*math.cos(math.radians(phase+180)))))
        for bone in ['prfa_right_forearm','plfa_left_forearm']:poses[bone].append((t,(-12,0,0)))
        poses['pw_waist'].append((t,(-6,beat*20,beat*5)))
        poses['pc_chest'].append((t,(0,-beat*8,0)));poses['h_ph_head'].append((t,(-6,-beat*8,0)))
    finish={'pra_right_arm':(1290,0,-30),'pla_left_arm':(1430,0,25),'prfa_right_forearm':(-5,0,0),'plfa_left_forearm':(-10,0,0),'pw_waist':(-8,-15,0),'pc_chest':(0,0,0),'h_ph_head':(-12,15,0)}
    for bone,keys in poses.items():
        keys.extend([(128,finish[bone]),(160,finish[bone]),(180,(1440,0,0) if bone=='pra_right_arm' else (1440,0,0) if bone=='pla_left_arm' else (0,0,0))])
    make('kazu',180,poses,[(0,(0,0,0,0,0)),(180,(0,0,0,0,0))])
    a=m['animations'][-1];tracks=a['animators'];rkeys=[];lkeys=[];rk=[];lk=[];roots=[]
    for tick in range(181):
        feet=[0.,0.];lift=[0.,0.]
        if tick<20:hip=11.25-3*tick/20;spread=8*tick/20
        else:
            hip=8.25;spread=8
            for step in range(12):
                start=20+step*8;side=step%2;target=-4 if step%4<2 else 4
                if tick>=start+8:feet[side]=target
                elif tick>=start:
                    f=(tick-start)/8;e=f*f*(3-2*f);feet[side]+=(target-feet[side])*e;lift[side]=2.5*math.sin(math.pi*f);break
                else:break
        if tick>=116:
            f=min(1,(tick-116)/12);left=min(1,2*f);right=max(0,2*f-1);feet=[4-right,4-6.5*left];lift=[3*right,1.5*math.sin(math.pi*left)]
        if tick>=160:
            f=(tick-160)/20;feet=[3*(1-f),-2.5*(1-f)];lift=[3*(1-f),0];hip=8.25+3*f;spread=8*(1-f)
        center=sum(feet)*.175 if tick<116 else 1.4*(1-min(1,(tick-116)/12))
        roots.append((tick/20,(0,hip-11.25,center)))
        for side,upper,knee in [(0,rkeys,rk),(1,lkeys,lk)]:
            vertical=(hip-lift[side])/math.cos(math.radians(spread));z=feet[side]-center;length=5.625
            bend=math.acos(max(-1,min(1,(vertical*vertical+z*z-2*length*length)/(2*length*length))))
            thigh=math.atan2(z,vertical)-bend/2
            upper.append((tick/20,(math.degrees(thigh),0,spread if side==0 else -spread)));knee.append((tick/20,(math.degrees(bend),0,0)))
    for bone,keys in [('prl_right_leg',rkeys),('pll_left_leg',lkeys),('prfl_right_foreleg',rk),('plfl_left_foreleg',lk)]:tracks[groups[bone]['uuid']]=player_animator(groups[bone],keys)
    tracks[groups['player_root']['uuid']]=animator(groups['player_root'],roots,'position')
    # Only the right arm acts: forward, sweeping down, then a brow-level salute.
    make('destasha',112,{
      'pra_right_arm':[(0,(0,0,0)),(18,(-90,0,0)),(30,(15,0,15)),(48,(-48.4808,-47.6508,120.8730)),(82,(-48.4808,-47.6508,120.8730)),(112,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(18,(0,0,0)),(30,(0,0,0)),(48,(-64.5366,0,0)),(82,(-64.5366,0,0)),(112,(0,0,0))]},
      [(0,(0,0,0,0,0)),(18,(-8,16,-8,16,8)),(82,(-8,16,-8,16,8)),(112,(0,0,0,0,0))])
    # Lean back while keeping the fist in FRONT of the face, not behind the skull.
    make('matsuda',100,{
      'pw_waist':[(0,(0,0,0)),(18,(-10,0,0)),(30,(-20,0,0)),(78,(-20,0,0)),(100,(0,0,0))],
      'pc_chest':[(0,(0,0,0)),(30,(-8,0,0)),(78,(-8,0,0)),(100,(0,0,0))],
      'pra_right_arm':[(0,(0,0,0)),(12,(-45,0,5)),(30,(-85,0,0)),(42,(-90,0,0)),(50,(-85,0,0)),(62,(-90,0,0)),(78,(-85,0,0)),(100,(0,0,0))],
      'prfa_right_forearm':[(0,(0,0,0)),(12,(-60,0,0)),(30,(-60,0,0)),(42,(-55,0,0)),(50,(-60,0,0)),(62,(-55,0,0)),(78,(-60,0,0)),(100,(0,0,0))],
      'pla_left_arm':[(0,(0,0,0)),(16,(5,0,-8)),(78,(5,0,-8)),(100,(0,0,0))],
      'plfa_left_forearm':[(0,(0,0,0)),(16,(-20,0,0)),(78,(-20,0,0)),(100,(0,0,0))],
      'h_ph_head':[(0,(0,0,0)),(30,(-12,-5,0)),(78,(-12,-5,0)),(100,(0,0,0))]},
      [(0,(0,0,0,0,0)),(16,(-12,24,-12,24,12)),(30,(-6,12,-6,12,14)),(78,(-6,12,-6,12,14)),(100,(0,0,0,0,0))])
    save('players/motionrp_performances.bbmodel',m)
if __name__=='__main__':
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned BetterModel rig')
    generate(json.loads(raw))
