"""Original two-player rock-slam poses, cosmetic stone slab and cracked impact ring."""
import sys
sys.dont_write_bytecode=True
import json,math,hashlib
from pathlib import Path
from generate_summons import ROOT,RIG_SHA,player,save,animation,animator
from generate_epic_revision import put_track,palette_model,ROCK

def actors(rig):
 for attack in [True,False]:
  name='broly_attacker'if attack else'broly_victim';m=player(rig,name,7.2,{})
  # This clock bone is intentionally never overridden by phase animations.
  put_track(m,'player_root',[(0,(0,0,0)),(7.2,(0,0,0))],'position');m['animations'][0]['override']=True
  groups={g['name']:g for g in m['groups']}
  poses={
   'grapple':{'pc_chest':(-12,0,0)if attack else(20,0,0),'pw_waist':(0,0,0)if attack else(-10,0,0),'pra_right_arm':(120,-10,0)if attack else(65,-10,20),'prfa_right_forearm':(8,0,0)if attack else(75,0,0),'pla_left_arm':(40,0,-15)if attack else(75,10,-20),'plfa_left_forearm':(80,0,0),'h_ph_head':(0,0,0)if attack else(-25,0,0),'prl_right_leg':(15,0,8),'pll_left_leg':(-15,0,-8)},
   'flight':{'pc_chest':(25,0,0)if attack else(-50,0,0),'pra_right_arm':(100,0,0)if attack else(135,0,35),'prfa_right_forearm':(5,0,0)if attack else(35,0,0),'pla_left_arm':(55,0,-12)if attack else(120,0,-35),'plfa_left_forearm':(70,0,0),'prl_right_leg':(-25,0,10),'prfl_right_foreleg':(-50,0,0),'pll_left_leg':(30,0,-10),'plfl_left_foreleg':(-65,0,0),'h_ph_head':(5,0,0)if attack else(-25,0,0)},
   'impact':{'pc_chest':(30,0,0)if attack else(-35,0,0),'pra_right_arm':(110,0,0)if attack else(100,0,30),'prfa_right_forearm':(0,0,0)if attack else(80,0,0),'pla_left_arm':(65,0,-20)if attack else(95,0,-30),'plfa_left_forearm':(70,0,0),'h_ph_head':(5,0,0)if attack else(-40,0,0),'prl_right_leg':(10,0,12),'pll_left_leg':(-15,0,-12)},
   'recover':{'pc_chest':(0,0,0)if attack else(30,0,0),'pw_waist':(0,0,0)if attack else(12,0,0),'pra_right_arm':(0,0,5)if attack else(15,0,15),'pla_left_arm':(0,0,-5)if attack else(15,0,-15),'prfa_right_forearm':(0,0,0),'plfa_left_forearm':(0,0,0),'h_ph_head':(0,0,0)if attack else(35,0,0),'prl_right_leg':(0,0,0)if attack else(30,0,12),'pll_left_leg':(0,0,0)if attack else(25,0,-12),'prfl_right_foreleg':(0,0,0)if attack else(-50,0,0),'plfl_left_foreleg':(0,0,0)if attack else(-45,0,0)}
  }
  poses['stand']={b:(0,0,0)for b in ['pc_chest','pw_waist','pra_right_arm','prfa_right_forearm','pla_left_arm','plfa_left_forearm','h_ph_head','prl_right_leg','pll_left_leg','prfl_right_foreleg','plfl_left_foreleg']}
  poses['charge']={'pc_chest':(40,0,0),'pra_right_arm':(105,0,0),'prfa_right_forearm':(5,0,0),'pla_left_arm':(-35,0,-15),'plfa_left_forearm':(70,0,0),'prl_right_leg':(-30,0,10),'pll_left_leg':(35,0,-10),'prfl_right_foreleg':(-65,0,0),'plfl_left_foreleg':(-35,0,0),'h_ph_head':(-15,0,0)}
  poses['fright']={'pc_chest':(-15,0,0),'pra_right_arm':(65,0,28),'pla_left_arm':(65,0,-28),'prfa_right_forearm':(80,0,0),'plfa_left_forearm':(80,0,0),'h_ph_head':(-12,0,0),'prl_right_leg':(15,0,12),'pll_left_leg':(-12,0,-12)}
  for phase,pose in poses.items():
   a=animation(phase,.5 if phase=='flight' else 1,{});a['override']=True
   for bone,value in pose.items():a['animators'][groups[bone]['uuid']]=animator(groups[bone],[(0,value),(.5 if phase=='flight' else 1,value)])
   if phase=='flight':a['loop']='loop'
   m['animations'].append(a)
  run=animation('flee',.4,{});run['override']=True;run['loop']='loop'
  for bone,sign in [('pra_right_arm',1),('pla_left_arm',-1),('prl_right_leg',-1),('pll_left_leg',1)]:
   run['animators'][groups[bone]['uuid']]=animator(groups[bone],[(0,(sign*45,0,0)),(.1,(0,0,0)),(.2,(-sign*45,0,0)),(.3,(0,0,0)),(.4,(sign*45,0,0))])
  for bone,value in {'pc_chest':(22,0,0),'h_ph_head':(-10,0,0),'prfa_right_forearm':(65,0,0),'plfa_left_forearm':(65,0,0)}.items():run['animators'][groups[bone]['uuid']]=animator(groups[bone],[(0,value),(.4,value)])
  for bone,offset in [('prfl_right_foreleg',0),('plfl_left_foreleg',.2)]:
   run['animators'][groups[bone]['uuid']]=animator(groups[bone],[(t,(-25-25*math.cos((t+offset)*math.tau/.4),0,0))for t in [0,.1,.2,.3,.4]])
  turn=animation('turn',.6,{});turn['override']=True
  for bone,keys in {'h_ph_head':[(0,(-10,0,0)),(.15,(-5,65,0)),(.35,(-5,40,0)),(.6,(-12,0,0))],'pc_chest':[(0,(22,0,0)),(.3,(0,20,0)),(.6,(-12,0,0))],'pra_right_arm':[(0,(0,0,0)),(.6,(65,0,28))],'pla_left_arm':[(0,(0,0,0)),(.6,(65,0,-28))],'prfa_right_forearm':[(0,(65,0,0)),(.6,(80,0,0))],'plfa_left_forearm':[(0,(65,0,0)),(.6,(80,0,0))],'prl_right_leg':[(0,(10,0,10)),(.6,(15,0,12))],'pll_left_leg':[(0,(-10,0,-10)),(.6,(-12,0,-12))],'prfl_right_foreleg':[(0,(-25,0,0)),(.6,(0,0,0))],'plfl_left_foreleg':[(0,(-25,0,0)),(.6,(0,0,0))]}.items():turn['animators'][groups[bone]['uuid']]=animator(groups[bone],keys)
  m['animations'].extend([run,turn])
  save('players/motionrp_'+name+'.bbmodel',m)

def stones():
 a=palette_model('motionrp_broly_wall',ROCK)
 for row in range(-2,3):
  for col in range(-2,3):
   edge=abs(row)+abs(col)==4
   if edge:continue
   a.cube('jagged_rock_slab_'+str(row)+'_'+str(col),(col*5.6,row*5.6,1+math.sin(row*7+col)*.5),(6.2,6.3,3.5+((row-col)%3)),1+(row+col)%3,(0,0,((row*3+col)%3-1)*6))
 a.joint('stone_wall',(0,0,0));a.m.save()
 a=palette_model('motionrp_broly_crater',ROCK)
 for i in range(18):
  q=i*math.tau/18;a.cube('raised_crater_rim_'+str(i),(8*math.cos(q),8*math.sin(q),0),(3.8,3.7,3.8),i%4,(0,0,math.degrees(q)))
  for j in range(3):
   r=5+j*3;a.cube('radial_crack_'+str(i)+'_'+str(j),(r*math.cos(q),r*math.sin(q),-.6),(2.8,.38,.18),4,(0,0,math.degrees(q)))
 a.joint('impact_ring',(0,0,0));a.m.save()
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes()
 if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise SystemExit('Expected pinned Steve rig')
 actors(json.loads(raw));stones()
