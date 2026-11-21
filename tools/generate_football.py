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
  norm=math.sqrt(sum(q*q for q in pts[0]));points={str(i):[(c[k]+(p[k]-c[k])*.975)*8/norm for k in range(3)]for i,p in enumerate(pts)}
  ids.append(add_mesh(m,'stitched_panel_'+str(n),points,[([str(0),str(i),str(i+1)],color)for i in range(1,len(pts)-1)]))
 m.group('football',ids);m.save()

def penguin():
 m=mesh_model('motionrp_emperor_penguin',COLORS);ids=[]
 ids.append(oval(m,'navy_body',(0,0,0),(4.1,4.8,7.8),4,rows=7,sides=12))
 ids.append(oval(m,'white_belly',(0,-2.2,1),(3.5,2.8,6.3),0,rows=6,sides=10))
 ids.append(oval(m,'navy_head',(0,1.7,6),(3.5,3.5,3.5),1,rows=7,sides=12))
 for sign in [-1,1]:
  ids.append(oval(m,'face_patch_'+str(sign),(sign*2,2.2,7.7),(1.5,1.9,1.6),0,rows=5,sides=8))
  ids.append(oval(m,'eye_'+str(sign),(sign*2.25,2.6,8.95),(.5,.75,.35),1,rows=4,sides=6))
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
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes()
 if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise SystemExit('Expected pinned Steve rig')
 actors(json.loads(raw));ball();penguin();tornado()
