"""Three reference-based football shots, at 1.2x the source effect duration.
Original editable meshes; pinned BetterModel Steve rig, no video pixels embedded.
"""
import sys,json,math,hashlib
from pathlib import Path
sys.dont_write_bytecode=True
from generate_summons import player,animator,save,ident,RIG_SHA,animation
from generate_energy import mesh_model,add_mesh
from generate_battle_visuals import oval
COLORS=[(8,5,22,255),(22,26,94,255),(27,80,255,255),(66,195,255,255),(235,252,255,255),(152,231,255,255),(205,8,35,255),(255,69,81,255)]
SHOTS={'death_sword':(129,84),'eternal_blizzard':(158,110),'death_spear':(132,72)}
def actor(rig,name,end,kick):
 m=player(rig,name,end/20,{})
 def track(b,keys,channel='rotation'):
  g=next(g for g in m['groups']if g['name']==b)
  keys=[(min(end,t+4)if t else 0,v)for t,v in keys]
  # A terminal pose can coincide after the renderer lead correction.
  keys=list({t:v for t,v in keys}.items())
  m['animations'][0]['animators'][g['uuid']]=animator(g,[(t/20,v)for t,v in keys],channel)
 track('player_root',[(0,(0,0,0)),(end,(0,0,0))],'position')
 if name=='death_sword':
  thigh=[(0,(0,0,0)),(12,(95,0,8)),(62,(95,0,8)),(70,(55,0,12)),(76,(-65,0,15)),(80,(-65,0,15)),(kick,(110,0,5)),(kick+5,(125,0,5)),(end-7,(0,0,0)),(end,(0,0,0))]
  knee=[(0,(0,0,0)),(12,(-10,0,0)),(62,(-10,0,0)),(76,(-100,0,0)),(80,(-100,0,0)),(kick,(0,0,0)),(kick+5,(0,0,0)),(end,(0,0,0))]
  chest=[(0,(0,0,0)),(18,(-10,0,0)),(70,(-12,-25,0)),(80,(-20,-35,-8)),(kick,(28,28,8)),(kick+8,(20,20,5)),(end,(0,0,0))]
  root=[(0,(0,0,0)),(end,(0,0,0))]
 elif name=='eternal_blizzard':
  thigh=[(0,(0,0,0)),(17,(65,0,8)),(28,(0,0,0)),(55,(35,0,20)),(75,(55,0,25)),(90,(-65,0,15)),(kick-4,(-65,0,15)),(kick,(115,0,5)),(kick+8,(130,0,8)),(end,(0,0,0))]
  knee=[(0,(0,0,0)),(38,(-55,0,0)),(65,(-90,0,0)),(90,(-100,0,0)),(kick-4,(-100,0,0)),(kick,(0,0,0)),(kick+8,(0,0,0)),(end,(0,0,0))]
  chest=[(0,(0,0,0)),(32,(-18,0,0)),(65,(-20,0,-12)),(90,(-28,-30,-15)),(kick,(25,25,10)),(kick+8,(32,15,10)),(end,(0,0,0))]
  root=[(0,(0,0,0)),(32,(0,0,0))]+[(38+i*5,(0,i*90,0))for i in range(13)]+[(kick,(0,1080,0)),(end,(0,1080,0))]
 else:
  thigh=[(0,(0,0,0)),(12,(80,0,15)),(30,(60,0,20)),(44,(65,0,20)),(kick-10,(-55,0,12)),(kick-4,(-55,0,12)),(kick,(125,0,5)),(kick+7,(140,0,5)),(end,(0,0,0))]
  knee=[(0,(0,0,0)),(20,(-80,0,0)),(44,(-70,0,0)),(kick-10,(-105,0,0)),(kick-4,(-105,0,0)),(kick,(0,0,0)),(kick+7,(0,0,0)),(end,(0,0,0))]
  chest=[(0,(-10,0,0)),(26,(-30,-20,-10)),(kick-10,(-25,-35,-15)),(kick,(35,30,15)),(kick+10,(25,25,12)),(end,(0,0,0))]
  root=[(0,(0,0,0)),(30,(0,0,-12)),(kick-4,(0,0,-12)),(kick,(0,0,8)),(end,(0,0,0))]
 track('player_root',root);track('pc_chest',chest);track('pw_waist',[(0,(0,0,0)),(kick-10,(-12,-18,0)),(kick,(10,18,0)),(end,(0,0,0))])
 track('prl_right_leg',thigh);track('prfl_right_foreleg',knee)
 track('pll_left_leg',[(0,(0,0,0)),(32,(-12,0,-8)),(kick-8,(35 if name!='death_sword'else -8,0,-15)),(kick,(-30,0,-12)),(end,(0,0,0))])
 track('plfl_left_foreleg',[(0,(0,0,0)),(40,(-65 if name!='death_sword'else -8,0,0)),(kick,(-75 if name!='death_sword'else -10,0,0)),(end,(0,0,0))])
 for right in [True,False]:
  sign=1 if right else -1
  track('pra_right_arm'if right else'pla_left_arm',[(0,(0,0,sign*8)),(30,(-20,0,sign*70)),(kick-12,(-50 if right else 80,0,sign*65)),(kick,(75 if right else -25,0,sign*45)),(kick+10,(30 if right else -20,0,sign*35)),(end,(0,0,0))])
  track('prfa_right_forearm'if right else'plfa_left_forearm',[(0,(0,0,0)),(30,(30,0,0)),(kick,(45,0,0)),(end,(0,0,0))])
 track('h_ph_head',[(0,(20,0,0)),(kick-12,(15,-15,0)),(kick,(10,10,0)),(end,(0,0,0))])
 for label,parent,origin in [('shot_boot','prfl_right_foreleg',[1.875,.65,-1.8]),('shot_torso','pc_chest',[0,17,0])]:
  g={'name':label,'uuid':ident(name+'/'+label),'origin':origin,'rotation':[0,0,0],'export':True};m['groups'].append(g)
  uid=next(x['uuid']for x in m['groups']if x['name']==parent)
  def add(ns):
   for n in ns:
    if isinstance(n,dict):
     if n['uuid']==uid:n['children'].append({'uuid':g['uuid'],'children':[]});return True
     if add(n['children']):return True
   return False
  assert add(m['outliner'])
 save('players/motionrp_'+name+'.bbmodel',m)
def finish(m,ids,spin=False):
 g=m.group('shot_effect',ids)
 if spin:
  a=animation('spin',.4,{g['uuid']:animator(g,[(i*.1,(0,0,i*90))for i in range(5)])});a['loop']='loop';m.data['animations']=[a]
 for e in m.data['elements']:e['light_emission']=15
 m.save()
def effects():
 m=mesh_model('motionrp_death_sword_edge',COLORS);ids=[]
 for i in range(5):
  q=i*math.tau/5;c,s=math.cos(q),math.sin(q)
  def p(x,y,z):return[x*c-y*s,x*s+y*c,z]
  for outline,w,color in [(True,8,2),(False,5.2,0)]:
   ids.append(add_mesh(m,f'blade_{i}_{outline}',{'a':p(-w,0,-15),'b':p(w,0,-15),'c':p(0,0,52),'d':p(0,3,-4)},[(['a','b','c'],color),(['a','c','d'],3 if outline else 1)]))
 finish(m,ids)
 m=mesh_model('motionrp_death_spear_drill',COLORS);ids=[]
 for i in range(12):
  a=i*math.tau/12;b=(i+1)*math.tau/12
  ids.append(add_mesh(m,'spear_facet_'+str(i),{'a':[12*math.cos(a),12*math.sin(a),-16],'b':[12*math.cos(b),12*math.sin(b),-16],'tip':[0,0,60]},[(['a','b','tip'],0 if i%2 else 6)]))
 for strand in range(3):
  v={};f=[]
  for j in range(33):
   z=-24+j*2.5;r=14*(1-j/34);a=j*.6+strand*math.tau/3
   for side in [0,1]:v[f'{j}_{side}']=[r*math.cos(a+side*.22),r*math.sin(a+side*.22),z]
   if j:f.append(([f'{j-1}_0',f'{j-1}_1',f'{j}_1',f'{j}_0'],7 if strand==0 else 6))
  ids.append(add_mesh(m,'red_drill_helix_'+str(strand),v,f))
 finish(m,ids,True)
 m=mesh_model('motionrp_blizzard_orb',COLORS);ids=[oval(m,'icy_core',(0,0,0),(8.5,8.5,8.5),5,rows=10,sides=16)]
 for k in range(4):
  v={};f=[]
  for j in range(41):
   a=j*math.tau/40;r=11.5;tilt=k*math.pi/4
   for side in [0,1]:
    x=(r+side*1.3)*math.cos(a);y=(r+side*1.3)*math.sin(a);v[f'{j}_{side}']=[x,y*math.cos(tilt),y*math.sin(tilt)]
   if j:f.append(([f'{j-1}_0',f'{j-1}_1',f'{j}_1',f'{j}_0'],4 if k%2 else 3))
  ids.append(add_mesh(m,'white_ice_ribbon_'+str(k),v,f))
 finish(m,ids,True)
 m=mesh_model('motionrp_blizzard_flake',COLORS);ids=[]
 for k in range(6):
  a=k*math.tau/6
  def point(x,y):return[x*math.cos(a)-y*math.sin(a),x*math.sin(a)+y*math.cos(a),0]
  segments=[((0,0),(0,12))]+[((0,y),(s*3,y+3))for y in [5,8]for s in [-1,1]]
  for j,(p,q)in enumerate(segments):
   ids.append(add_mesh(m,f'crystal_{k}_{j}',{'a':point(p[0]-.55,p[1]),'b':point(p[0]+.55,p[1]),'c':point(q[0]+.55,q[1]),'d':point(q[0]-.55,q[1])},[(['a','b','c','d'],4 if j else 5)]))
 finish(m,ids,True)
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes();assert hashlib.sha256(raw).hexdigest()==RIG_SHA
 for name,(end,kick)in SHOTS.items():actor(json.loads(raw),name,end,kick)
 effects()
