"""Original editable football, flame helix, swimming penguin and three shooting motions."""
import sys,json,math,hashlib
from pathlib import Path
sys.dont_write_bytecode=True
from generate_summons import player,save,animator,RIG_SHA
from generate_energy import mesh_model,add_mesh
from generate_battle_visuals import oval
COLORS=[(245,246,255,255),(18,24,40,255),(233,111,20,255),(255,217,53,255),(33,57,104,255),(116,216,255,255),(222,36,49,255),(255,249,200,255)]

def ball():
 m=mesh_model('motionrp_football',COLORS)
 phi=(1+math.sqrt(5))/2
 v=[(0,a,b*phi)for a in [-1,1]for b in [-1,1]]+[(a,b*phi,0)for a in [-1,1]for b in [-1,1]]+[(b*phi,0,a)for a in [-1,1]for b in [-1,1]]
 edges={(i,j)for i in range(12)for j in range(12)if i!=j and abs(sum((v[i][k]-v[j][k])**2 for k in range(3))-4)<.001}
 verts={(i,j):tuple((2*v[i][k]+v[j][k])/3 for k in range(3))for i,j in edges}
 faces=[]
 for i in range(12):faces.append(([(i,j)for a,j in edges if a==i],1))
 for i in range(12):
  for j in range(i+1,12):
   for k in range(j+1,12):
    if (i,j)in edges and (j,k)in edges and (k,i)in edges:faces.append(([(i,j),(j,i),(j,k),(k,j),(k,i),(i,k)],0))
 ids=[]
 for n,(keys,color)in enumerate(faces):
  pts=[verts[k]for k in keys];c=[sum(p[i]for p in pts)/len(pts)for i in range(3)]
  normal=[x/math.sqrt(sum(q*q for q in c))for x in c];a=[pts[0][i]-c[i]for i in range(3)];length=math.sqrt(sum(x*x for x in a));a=[x/length for x in a];b=[normal[1]*a[2]-normal[2]*a[1],normal[2]*a[0]-normal[0]*a[2],normal[0]*a[1]-normal[1]*a[0]]
  pts.sort(key=lambda p:math.atan2(sum((p[i]-c[i])*b[i]for i in range(3)),sum((p[i]-c[i])*a[i]for i in range(3))))
  norm=math.sqrt(sum(q*q for q in pts[0]));points={str(i):[(c[k]+(p[k]-c[k])*.975)*3.8/norm for k in range(3)]for i,p in enumerate(pts)}
  ids.append(add_mesh(m,'stitched_panel_'+str(n),points,[([str(0),str(i),str(i+1)],color)for i in range(1,len(pts)-1)]))
 m.group('football',ids);m.save()

def penguin(red=False):
 colors=list(COLORS)
 if red:colors[1]=(222,19,32,255);colors[4]=(198,9,25,255);colors[6]=(20,12,18,255)
 m=mesh_model('motionrp_emperor_penguin_red'if red else'motionrp_emperor_penguin',colors);ids=[]
 ids.append(oval(m,'navy_body',(0,0,0),(4.1,4.8,7.8),4,rows=7,sides=12))
 ids.append(oval(m,'white_belly',(0,-2.2,1),(3.5,2.8,6.3),0,rows=6,sides=10))
 ids.append(oval(m,'navy_head',(0,1.7,6),(3.5,3.5,3.5),1,rows=7,sides=12))
 for sign in [-1,1]:
  ids.append(oval(m,'face_patch_'+str(sign),(sign*2,2.2,7.7),(1.5,1.9,1.6),0,rows=5,sides=8))
  ids.append(oval(m,'eye_'+str(sign),(sign*2.25,2.6,8.95),(.5,.75,.35),6 if red else 1,rows=4,sides=6))
  ids.append(oval(m,'tucked_foot_'+str(sign),(sign*2,-2.7,-6),(1.6,.8,2.8),2,rows=4,sides=6))
 ids.append(add_mesh(m,'pointed_beak',{'a':[-1.9,.6,8],'b':[1.9,.6,8],'c':[0,2.1,8],'d':[0,.8,12]},[(['a','b','d'],2),(['b','c','d'],3),(['c','a','d'],2),(['a','c','b'],2)]))
 m.group('swimming_penguin',ids)
 from generate_summons import animation
 clip=animation('flap',.4,{});clip['loop']='loop'
 for sign in [-1,1]:
  points={'a':[sign*3.4,1.5,2],'b':[sign*10,-.5,-1],'c':[sign*8,-1.6,-5],'d':[sign*3.5,-1,-3]}
  g=m.group('flipper_'+str(sign),[add_mesh(m,'flipper_mesh_'+str(sign),points,[(['a','b','c'],4),(['a','c','d'],1)])],origin=(sign*3.5,0,0))
  clip['animators'][g['uuid']]=animator(g,[(0,(0,0,-sign*20)),(.2,(0,0,sign*30)),(.4,(0,0,-sign*20))])
 m.data['animations']=[clip];m.save()

def tornado():
 m=mesh_model('motionrp_fire_tornado',COLORS);ids=[]
 for strand in range(5):
  vertices={};faces=[]
  for j in range(37):
   h=j/36*64;r=5+12*j/36;q=j*.42+strand*math.tau/5
   for tag,angle,y in [('a',q-.17,h-2.2),('b',q+.17,h+2.2)]:vertices[tag+str(j)]=[r*math.cos(angle),y,r*math.sin(angle)]
   if j:faces.append((['a'+str(j-1),'b'+str(j-1),'b'+str(j),'a'+str(j)],2 if strand%2 else 3))
  ids.append(add_mesh(m,'rising_flame_'+str(strand),vertices,faces))
 g=m.group('flame_spiral',ids)
 from generate_summons import animation
 a=animation('spin',.8,{});a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(j*.2,(0,j*90,0))for j in range(5)])
 m.data['animations']=[a]
 for e in m.data['elements']:e['light_emission']=15
 m.save()

def actors(rig):
 for name in ['fire_tornado','emperor_penguin_1','emperor_penguin_2_left','emperor_penguin_2_right']:
  fire=name=='fire_tornado';duo='_2_'in name;side=-1 if name.endswith('right')else 1
  m=player(rig,name,8.2,{})
  def track(b,keys,channel='rotation'):
   g=next(g for g in m['groups']if g['name']==b)
   m['animations'][0]['animators'][g['uuid']]=animator(g,[(t/20,v)for t,v in keys],channel)
  track('player_root',[(0,(0,0,0)),(164,(0,0,0))],'position')
  if fire:
   track('player_root',[(0,(0,0,0)),(44,(0,0,0)),(60,(-80,90,0)),(72,(-180,180,0)),(84,(-270,270,0)),(94,(-340,360,0)),(100,(-360,360,0)),(122,(-360,360,0)),(145,(-360,360,0)),(164,(-360,360,0))])
  elif not duo:
   track('player_root',[(0,(0,0,0)),(62,(0,0,0)),(74,(-90,0,0)),(86,(-200,0,0)),(96,(-320,0,0)),(110,(-360,0,0)),(164,(-360,0,0))])
  track('pc_chest',[(0,(0,0,0)),(24,(15,0,0)),(52,(-20,0,side*12 if duo else 0)),(82,(-30,side*15,side*25 if duo else -10)),(96,(28,-side*18,-side*20 if duo else 8)),(115,(18,0,0)),(145,(0,0,0)),(164,(0,0,0))])
  track('pw_waist',[(0,(0,0,0)),(60,(-15,side*12,0)),(96,(15,-side*15,0)),(138,(0,0,0)),(164,(0,0,0))])
  for right in [True,False]:
   kicking=right if not duo else right==(side==1)
   sign=1 if right else -1
   track('prl_right_leg'if right else'pll_left_leg',[(0,(0,0,0)),(24,(35 if kicking else -12,0,sign*8)),(52,(70 if kicking else -15,0,sign*12)),(78,(-65 if kicking else 35,0,sign*20)),(90,(-80 if kicking else 50,0,sign*22)),(96,(120 if kicking else -35,0,sign*18)),(104,(95 if kicking else -25,0,sign*15)),(130,(15,0,sign*8)),(146,(0,0,0)),(164,(0,0,0))])
   track('prfl_right_foreleg'if right else'plfl_left_foreleg',[(0,(0,0,0)),(52,(-75 if kicking else -40,0,0)),(82,(-100 if kicking else -65,0,0)),(94,(-35,0,0)),(96,(0 if kicking else -60,0,0)),(108,(-15 if kicking else -70,0,0)),(138,(-20,0,0)),(148,(0,0,0)),(164,(0,0,0))])
   track('pra_right_arm'if right else'pla_left_arm',[(0,(0,0,sign*8)),(30,(25,0,sign*30)),(65,(-40,0,sign*65)),(88,(-65,0,sign*75)),(96,(35,0,sign*40)),(120,(12,0,sign*22)),(145,(0,0,sign*5)),(164,(0,0,0))])
   track('prfa_right_forearm'if right else'plfa_left_forearm',[(0,(15,0,0)),(56,(70,0,0)),(90,(35,0,0)),(110,(55,0,0)),(145,(0,0,0)),(164,(0,0,0))])
  track('h_ph_head',[(0,(15,0,0)),(45,(-15,0,0)),(90,(-20,0,0)),(100,(10,0,0)),(145,(0,0,0)),(164,(0,0,0))])
  save('players/motionrp_'+name+'.bbmodel',m)
def reference_actors(rig):
 from generate_summons import ident
 from generate_rasengan import hand_path
 for fire in [True,False]:
  name='fire_tornado'if fire else'emperor_penguin_1';end=178 if fire else 174;kick=112 if fire else 98
  m=player(rig,name,end/20,{})
  def track(b,keys,channel='rotation'):
   g=next(g for g in m['groups']if g['name']==b);a=m['animations'][0]['animators'];entry=a.setdefault(g['uuid'],{'name':g['name'],'type':'bone','keyframes':[]})
   entry['keyframes']+=animator(g,[(t/20,v)for t,v in keys],channel)['keyframes']
  track('player_root',[(0,(0,0,0)),(end,(0,0,0))],'position')
  if fire:
   track('player_root',[(0,(0,0,0)),(44,(0,0,0)),(62,(-45,90,0)),(76,(-80,180,0)),(88,(-95,270,0)),(106,(-100,360,0)),(112,(-100,360,0)),(124,(-165,360,0)),(134,(-270,360,0)),(148,(-350,360,0)),(156,(-360,360,0)),(end,(-360,360,0))])
   poses={
    'pc_chest':[(0,(0,0,0)),(16,(-10,0,-8)),(30,(0,0,0)),(60,(-18,0,0)),(96,(-10,8,-8)),(104,(0,0,0)),(112,(0,0,0)),(120,(0,0,0)),(134,(0,0,0)),(end,(0,0,0))],
    'pw_waist':[(0,(0,0,0)),(60,(-8,0,0)),(100,(-5,5,0)),(104,(0,0,0)),(112,(0,0,0)),(120,(0,0,0)),(156,(0,0,0)),(end,(0,0,0))],
    'prl_right_leg':[(0,(0,0,0)),(10,(-20,0,0)),(16,(65,0,5)),(30,(0,0,0)),(52,(25,0,15)),(76,(75,0,20)),(102,(65,0,12)),(108,(125,0,8)),(112,(165,0,5)),(120,(175,0,8)),(134,(15,0,15)),(156,(0,0,0)),(end,(0,0,0))],
    'prfl_right_foreleg':[(0,(0,0,0)),(10,(-45,0,0)),(16,(0,0,0)),(30,(0,0,0)),(70,(-85,0,0)),(102,(-90,0,0)),(108,(-40,0,0)),(112,(0,0,0)),(126,(-25,0,0)),(146,(-25,0,0)),(156,(0,0,0)),(end,(0,0,0))],
    'pll_left_leg':[(0,(0,0,0)),(30,(-12,0,-5)),(52,(35,0,-15)),(90,(40,0,-20)),(112,(-35,0,-18)),(128,(45,0,-12)),(156,(0,0,0)),(end,(0,0,0))],
    'plfl_left_foreleg':[(0,(0,0,0)),(46,(-35,0,0)),(70,(-95,0,0)),(110,(-75,0,0)),(132,(-40,0,0)),(156,(0,0,0)),(end,(0,0,0))]
   }
  else:
   poses={
    'pc_chest':[(0,(0,0,0)),(22,(-28,0,0)),(44,(-48,0,0)),(76,(-48,0,0)),(90,(-45,12,0)),(98,(-28,-15,0)),(114,(-32,0,0)),(132,(-48,0,0)),(148,(-35,0,0)),(end,(0,0,0))],
    'pw_waist':[(0,(0,0,0)),(44,(-25,0,0)),(90,(-25,0,0)),(98,(-8,0,0)),(132,(-16,0,0)),(end,(0,0,0))],
    'prl_right_leg':[(0,(0,0,0)),(20,(-55,0,8)),(44,(-160,0,8)),(76,(-160,0,8)),(90,(-165,0,8)),(94,(-65,0,5)),(98,(105,0,5)),(108,(85,0,10)),(124,(35,0,12)),(132,(38,0,12)),(138,(25,0,12)),(146,(32,0,12)),(158,(15,0,8)),(end,(0,0,0))],
    'prfl_right_foreleg':[(0,(0,0,0)),(44,(-8,0,0)),(90,(-8,0,0)),(98,(0,0,0)),(122,(-35,0,0)),(132,(-48,0,0)),(138,(-28,0,0)),(146,(-42,0,0)),(end,(0,0,0))],
    'pll_left_leg':[(0,(0,0,0)),(44,(8,0,-8)),(90,(8,0,-8)),(98,(-12,0,-10)),(124,(12,0,-10)),(end,(0,0,0))],
    'plfl_left_foreleg':[(0,(0,0,0)),(44,(-12,0,0)),(90,(-12,0,0)),(98,(-24,0,0)),(124,(-18,0,0)),(end,(0,0,0))]
   }
  for bone,keys in poses.items():track(bone,keys)
  for right in [True,False]:
   sign=1 if right else -1
   if fire:arm=[(0,(0,0,sign*8)),(20,(10,0,sign*25)),(62,(-35,0,sign*65)),(100,(-55 if right else 120,0,sign*65)),(112,(35 if right else 105,0,sign*40)),(134,(0,0,sign*55)),(156,(0,0,0)),(end,(0,0,0))]
   else:arm=[(0,(0,0,sign*8)),(44,(-15,0,sign*75)),(90,(-15,0,sign*75)),(98,(25,0,sign*45)),(120,(10,0,sign*35)),(136,(65,-sign*25,sign*5)),(152,(45,0,sign*10)),(end,(0,0,0))]
   track('pra_right_arm'if right else'pla_left_arm',arm)
   track('prfa_right_forearm'if right else'plfa_left_forearm',[(0,(0,0,0)),(44,(20,0,0)),(kick,(30,0,0)),(136,(65 if not fire else 20,0,0)),(end,(0,0,0))])
  track('h_ph_head',[(0,(12,0,0)),(44,(25,0,0)),(kick-8,(35,0,0)),(kick,(12,0,0)),(136,(30 if not fire else 0,0,0)),(end,(0,0,0))])
  if fire:
   # Tilt on the parent, spin around the body's local axis on the child.
   root=next(g for g in m['groups']if g['name']=='player_root')
   spin={'name':'football_spin','uuid':ident(name+'/spin'),'origin':root['origin'][:],'rotation':[0,0,0],'export':True}
   bank={'name':'football_bank','uuid':ident(name+'/bank'),'origin':root['origin'][:],'rotation':[0,0,0],'export':True}
   m['groups'] += [spin,bank]
   # The upstream Steve rig puts both thighs beside the hip, not inside it.
   # Rotate every body branch together; leave the shadow and name tag outside.
   body_ids={g['uuid']for g in m['groups']if g['name']in {'phip_hip','prl_right_leg','pll_left_leg'}}
   node=next(n for n in m['outliner']if isinstance(n,dict)and n['uuid']==root['uuid'])
   branches=[n for n in node['children']if isinstance(n,dict)and n['uuid']in body_ids]
   assert len(branches)==3
   node['children']=[n for n in node['children']if n not in branches]
   node['children'].append({'uuid':bank['uuid'],'children':[{'uuid':spin['uuid'],'children':branches}]})
   landmarks=[(0,0),(16,8),(36,20),(46,26),(82,46),(106,64),(112,68),(126,78),(144,88),(156,98),(178,108)]
   def retime(t):
    for (a,x),(b,y)in zip(landmarks,landmarks[1:]):
     if t<=b:return x+(t-a)*(y-x)/(b-a)
    return 108
   for entry in m['animations'][0]['animators'].values():
    for key in entry['keyframes']:
     key['time']=retime(key['time']*20)/20
     if entry['name']=='player_root'and key['channel']=='rotation':key['data_points'][0]['y']=0
   # Explicit quarter turns prevent quaternion interpolation taking a shortcut.
   track('football_spin',[(0,(0,0,0)),(26,(0,0,0))]+[(28+i*1.75,(0,i*90,0))for i in range(17)]+[(108,(0,1440,0))])
   # Quarter-turn left around the horizontal body's own axis at the kick.
   # Keep the four-turn spin track independent and unbank before landing.
   track('football_bank',[(0,(0,0,0)),(56,(0,0,0)),(62,(0,90,0)),(78,(0,90,0)),(96,(0,0,0)),(108,(0,0,0))])
   # BetterModel's player track is evaluated ahead of the scene tick.
   # Hold the initial pose for four ticks so the visible boot reaches contact
   # during the scene's release, before the follow-through reverses its motion.
   for entry in m['animations'][0]['animators'].values():
    for key in entry['keyframes']:
     if key['time']>0:key['time']+=4/20
   end=112;m['animations'][0]['length']=end/20
  # Real boot and calf attachments, sampled from these exact animated joints.
  parent=None
  def attach(nodes,uid):
   for node in nodes:
    if isinstance(node,dict):
     if node['uuid']==parent['uuid']:node['children'].append({'uuid':uid,'children':[]});return True
     if attach(node['children'],uid):return True
   return False
  paths=[]
  for label,bone,origin in [('football_boot','prfl_right_foreleg',(1.875,.65,-1.8)),('football_calf','prfl_right_foreleg',(1.875,4.1,0)),('football_torso','pc_chest',(0,17,0)),('football_upper','pc_chest',(0,22,0)),('football_side','pc_chest',(5,17,0))]:
   parent=next(g for g in m['groups']if g['name']==bone)
   g={'name':label,'uuid':ident(name+'/'+label),'origin':list(origin),'rotation':[0,0,0],'export':True};m['groups'].append(g);assert attach(m['outliner'],g['uuid'])
   paths.append(hand_path(m,end,label).strip().splitlines())
  save('players/motionrp_'+name+'.bbmodel',m)
  from generate_summons import ROOT
  (ROOT/(name+'-boot.csv')).write_text('\n'.join(','.join(row)for row in zip(*paths))+'\n')

if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes()
 if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise SystemExit('Expected pinned Steve rig')
 actors(json.loads(raw));reference_actors(json.loads(raw));ball();penguin();penguin(True);tornado()
