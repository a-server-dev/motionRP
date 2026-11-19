"""0.22: separated Fusion choreography, exact fused skin, humanoid World and dented roller."""
import sys
sys.dont_write_bytecode=True
import math,json,copy,base64,io
from PIL import Image,ImageDraw
from generate_summons import Model,ROOT,save,animation,animator,player,ident
from generate_perfect_susanoo import Armor,continuous
from generate_rasengan import hand_path
GOLD=[(211,173,59,255),(255,238,149,255),(114,92,44,255),(69,174,94,255),(177,137,42,255),(231,203,112,255),(43,74,72,255),(255,255,220,255)]
ROCK=[(67,59,53,255),(105,89,73,255),(138,120,95,255),(74,75,77,255),(40,36,37,255),(166,153,131,255),(107,109,111,255),(196,180,152,255)]
FUSION_END=216
CONTACT=120

def palette_model(name,colors):
 a=Armor(name,False);a.m=Model(name,False,colors);return a

def put_track(m,name,keys,channel='rotation'):
 g=next(g for g in m['groups']if g['name']==name);m['animations'][0]['animators'][g['uuid']]=animator(g,continuous([(t,list(v))for t,v in keys]) if channel=='rotation' else keys,channel)

def exact_skin_model(rig,name,png,clips):
 m=copy.deepcopy(rig);m['name']=m['model_identifier']=name;m['animations']=clips
 mapping={}
 for g in m['groups']:
  mapping[g['uuid']]=name.replace('motionrp_','')+'bone'+g['name'].replace('_','');g['name']=mapping[g['uuid']]
 for a in m['animations']:
  for uid,track in a['animators'].items():track['name']=mapping[uid]
 tex=m['textures'][0];tex.update(name=name+'.png',uuid=ident(name+'/skin'),source='data:image/png;base64,'+base64.b64encode(png).decode(),internal=True)
 for key in ['path','relative_path']:tex.pop(key,None)
 return m

def fused_skin(rig):
 raw=(ROOT.parent/'skins/fusion.png').read_bytes();assert raw[:8]==b'\x89PNG\r\n\x1a\n' and int.from_bytes(raw[16:20],'big')==64 and int.from_bytes(raw[20:24],'big')==64
 groups={g['name']:g for g in rig['groups']};a=animation('fusion_idle',2,{});a['loop']='loop'
 for name,keys in {'pc_chest':[(0,(0,0,0)),(1,(-1.5,0,0)),(2,(0,0,0))],'pra_right_arm':[(0,(0,0,4)),(1,(0,0,6)),(2,(0,0,4))],'pla_left_arm':[(0,(0,0,-4)),(1,(0,0,-6)),(2,(0,0,-4))]}.items():a['animators'][groups[name]['uuid']]=animator(groups[name],keys)
 save('models/motionrp_fusion_warrior.bbmodel',exact_skin_model(rig,'motionrp_fusion_warrior',raw,[a]))

def fusion(rig):
 for role in [1,-1]:
  name='fusion_left'if role==1 else'fusion_right';m=player(rig,name,FUSION_END/20,{})
  waist=-role*85
  put_track(m,'pw_waist',[(0,(0,0,0)),(3.8,(0,0,0)),(5.6,(0,0,waist)),(10.8,(0,0,waist))])
  put_track(m,'player_root',[(0,(0,0,0)),(1.4,(0,0,0)),(3.8,(role*11.6,0,0)),(10.8,(role*11.6,0,0))],'position')
  for side,upper,fore in [(1,'pra_right_arm','prfa_right_forearm'),(-1,'pla_left_arm','plfa_left_forearm')]:
   v=math.radians(waist);target=(role*16.4,16);tx=math.cos(v)*target[0]+math.sin(v)*(target[1]-15);ty=-math.sin(v)*target[0]+math.cos(v)*(target[1]-15)+15
   dx,dy=tx-side*5,ty-22;d=math.hypot(dx,dy);l1=5.5;l2=math.hypot(.625,5.625);assert abs(l1-l2)<d<l1+l2
   along=(l1*l1-l2*l2+d*d)/(2*d);h=math.sqrt(l1*l1-along*along);ex=along*dx/d-role*h*dy/d;ey=along*dy/d+role*h*dx/d
   upper_z=math.degrees(math.atan2(ey,ex)+math.pi/2);fore_z=math.degrees(math.atan2(dy-ey,dx-ex)-math.atan2(-5.625,side*.625))-upper_z
   upper_z=(upper_z+180)%360-180;fore_z=(fore_z+180)%360-180
   # Mirrored full T -> broad overhead arcs -> pointing pose. No body/head overlap.
   canonical_side=side*role
   initial=90 if canonical_side==1 else -90;arc=155 if canonical_side==1 else -155;last=170 if canonical_side==1 else -200
   put_track(m,upper,[(0,(0,0,0)),(.6,(0,0,initial*role)),(1.5,(0,0,initial*role)),(2.6,(15,0,arc*role)),(3.8,(15,0,last*role)),(5.6,(0,0,upper_z)),(10.8,(0,0,upper_z))])
   put_track(m,fore,[(0,(0,0,0)),(3.8,(0,0,0)),(5.6,(0,0,fore_z)),(10.8,(0,0,fore_z))])
  for side,bone in [(1,'prl_right_leg'),(-1,'pll_left_leg')]:
   offset=0 if side*role==1 else .28
   keys=[(0,(0,0,0)),(1.4,(0,0,0))]
   for n in range(3):
    t=1.45+n*.65+offset;keys.extend([(t,(0,0,0)),(t+.17,(32,0,0)),(t+.34,(0,0,0))])
   keys.extend([(3.8,(0,0,0)),(10.8,(0,0,0))]);put_track(m,bone,keys)
  save('players/motionrp_'+name+'.bbmodel',m);(ROOT/(name+'-hand-path.csv')).write_text(hand_path(m,FUSION_END))

def punch_pose(tick):
 progress=max(0,min(1,(tick-104)/72));phase=(tick-104)%8;strength=(1+math.cos(phase*math.tau/8))/2
 if tick>=172:strength=max(strength,(tick-172)/4)
 chest=-20-15*progress;result={}
 for side,upper,fore in [(1,'pra_right_arm','prfa_right_forearm'),(-1,'pla_left_arm','plfa_left_forearm')]:
  p=strength if side==1 else 1-strength
  if tick>=172:p=max(p,(tick-172)/4)
  gy=25*(1-p)+(13-2.85*progress)*p;gz=2*(1-p)+(-8+progress)*p
  angle=math.radians(-chest);dy=(gy-15)*math.cos(angle)-gz*math.sin(angle)+15-22;dz=(gy-15)*math.sin(angle)+gz*math.cos(angle)
  d=math.hypot(dy,dz);l1,l2=5.5,5.625;assert abs(l1-l2)<d<l1+l2,(tick,side,d)
  along=(l1*l1-l2*l2+d*d)/(2*d);h=math.sqrt(l1*l1-along*along);ey=along*dy/d-h*dz/d;ez=along*dz/d+h*dy/d
  a=math.degrees(math.atan2(-ez,-ey));b=math.degrees(math.atan2(-(dz-ez),-(dy-ey)))-a;b=(b+180)%360-180
  result[upper]=(a,0,0);result[fore]=(b,0,0)
 result['pw_waist']=(chest,0,0);result['h_ph_head']=(12+4*progress,0,0)
 return result

def road_actor(rig):
 m=player(rig,'road_roller',12.2,{})
 for name in ['pra_right_arm','pla_left_arm','prfa_right_forearm','plfa_left_forearm','pw_waist','h_ph_head']:
  if name in ['pra_right_arm','pla_left_arm']:first=[(0,(0,0,0)),(.8,(90,0,(-15 if name.startswith('pra') else 15))),(2,(165,0,0)),(3.5,(165,0,0)),(4.8,(0,0,0))]
  else:first=[(0,(0,0,0)),(4.8,(0,0,0))]
  keys=first+[(t/20,punch_pose(t)[name])for t in range(104,177)]+[(9.0,punch_pose(176)[name]),(9.8,(0,0,0)),(12.2,(0,0,0))];put_track(m,name,keys)
 save('players/motionrp_road_roller.bbmodel',m);(ROOT/'road_roller-hand-path.csv').write_text(hand_path(m,244))

def world_skin():
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 gold,light,shade,green,darkgold,warm,teal,white=GOLD
 rects=[(0,0,32,16),(16,16,40,32),(40,16,56,32),(0,16,16,32),(32,48,48,64),(16,48,32,64)]
 for x,y,X,Y in rects:d.rectangle((x,y,X-1,Y-1),fill=gold)
 # Classic skin UVs: anatomical panel lines, six-pack, dark suit gaps and green hearts.
 d.rectangle((20,20,27,31),fill=warm)
 for y in [21,24,27]:
  d.rectangle((21,y,26,y+1),fill=light);d.line((23,y,23,y+2),fill=shade);d.line((20,y+2,27,y+2),fill=darkgold)
 d.rectangle((20,29,27,31),fill=teal);d.polygon([(22,29),(23,29),(24,30),(25,29),(26,29),(24,32)],fill=green)
 for x,y in [(44,20),(36,52)]:
  d.rectangle((x,y,x+3,y+3),fill=light);d.rectangle((x,y+4,x+3,y+6),fill=teal);d.rectangle((x,y+7,x+3,y+11),fill=warm);d.line((x,y+7,x,y+11),fill=light);d.line((x+3,y+8,x+3,y+11),fill=shade)
 for x,y in [(4,20),(20,52)]:
  d.rectangle((x,y,x+3,y+3),fill=warm);d.rectangle((x,y+4,x+3,y+5),fill=teal);d.rectangle((x+1,y+6,x+2,y+7),fill=green);d.rectangle((x,y+8,x+3,y+11),fill=light);d.line((x,y+11,x+3,y+11),fill=darkgold)
 d.rectangle((8,8,15,11),fill=light);d.rectangle((8,12,15,12),fill=shade);d.point((9,12),fill=white);d.point((14,12),fill=white);d.rectangle((8,13,15,15),fill=teal);d.point((11,14),fill=green);d.point((12,14),fill=green)
 d.rectangle((40,8,47,10),fill=light);d.line((40,11,47,11),fill=warm)
 for i,color in enumerate(GOLD):d.point((56+i,0),fill=color)
 path=ROOT.parent/'skins/the_world.png';im.save(path);return path.read_bytes()

def the_world(rig):
 g={x['name']:x for x in rig['groups']};clip=animation('barrage',3.6,{})
 for name in punch_pose(104):clip['animators'][g[name]['uuid']]=animator(g[name],continuous([(t/20,list(punch_pose(104+t)[name]))for t in range(73)]))
 idle=animation('world_idle',2,{});idle['loop']='loop'
 for name in ['pra_right_arm','pla_left_arm']:idle['animators'][g[name]['uuid']]=animator(g[name],[(0,(0,0,5 if 'pra' in name else -5)),(1,(0,0,7 if 'pra' in name else -7)),(2,(0,0,5 if 'pra' in name else -5))])
 m=exact_skin_model(rig,'motionrp_the_world',world_skin(),[clip,idle]);groups={x['uuid']:x for x in m['groups']};nodes={}
 def walk(ns):
  for node in ns:
   if isinstance(node,dict):nodes[node['uuid']]=node;walk(node['children'])
 walk(m['outliner'])
 def cube(name,center,size,color,parent,rot=None):
  uid=ident('world-detail/'+name);e={'name':name,'uuid':uid,'type':'cube','box_uv':False,'origin':list(center),'from':[a-b/2 for a,b in zip(center,size)],'to':[a+b/2 for a,b in zip(center,size)],'faces':{side:{'uv':[56+color+.05,.05,56+color+.95,.95],'texture':0}for side in ['north','south','east','west','up','down']}}
  if rot:e['rotation']=rot
  m['elements'].append(e);nodes[g[parent]['uuid']]['children'].append(uid)
 cube('helmet_brow',(0,28.4,-4),(8.2,.8,.7),1,'h_ph_head')
 for side in [-1,1]:
  cube(f'helmet_cheek_{side}',(side*3.7,25.7,-2.6),(.8,3,3),4,'h_ph_head')
  cube(f'forehead_heart_{side}',(side*.4,27.4,-4.2),(.9,.9,.5),3,'h_ph_head',(0,0,side*45))
  arm='pra_right_arm'if side==1 else'pla_left_arm';leg='prfl_right_foreleg'if side==1 else'plfl_left_foreleg'
  cube(f'shoulder_plate_{side}',(side*5.6,21.8,0),(4.8,2.6,5),1,arm)
  cube(f'shoulder_gap_{side}',(side*5.6,20.2,-.1),(4.8,.5,5.1),6,arm)
  cube(f'knee_heart_{side}',(side*1.875,6.3,-2.15),(1.8,1.8,.5),3,leg,(0,0,45))
  cube(f'backpack_tank_{side}',(side*2.3,18.5,3.4),(1.8,6,1.8),6,'pc_chest')
  for y in [16.2,20.8]:cube(f'tank_strap_{side}_{y}',(side*2.3,y,3.4),(2.1,.7,2.1),4,'pc_chest')
 cube('belt_heart',(0,12.7,-2.3),(1.8,1.8,.6),3,'pw_waist',(0,0,45));save('models/motionrp_the_world.bbmodel',m)

def roller():
 a=palette_model('motionrp_road_roller',GOLD);c=a.cube
 for end in [-1,1]:
  for j in range(24):
   q=j*math.tau/24;c(f'steel_drum_{end}_{j}',(0,7.5+7*math.cos(q),end*18+7*math.sin(q)),(34,2.2,2.2),6)
  for side in [-1,1]:
   c(f'drum_hub_{end}_{side}',(side*17.7,7.5,end*18),(1.8,5,5),4)
   c(f'drum_fork_{end}_{side}',(side*18,10.5,end*16),(2.6,11,3.5),0)
   for y in [7.5,10.5]:c(f'axle_bolt_{end}_{side}_{y}',(side*19.5,y,end*18),(1,1,1),1)
 a.joint('drums_and_forks',(0,0,0))
 c('chassis',(0,13,0),(31,7,46),0);c('dark_frame',(0,10.5,0),(26,3,45),2);c('deck',(0,16.5,0),(33,2,48),5)
 for side in [-1,1]:
  c(f'fender_{side}',(side*16,16,-14),(3,2,18),0)
  for z in [-20,-12,-4,4,12,20]:c(f'chassis_rivet_{side}_{z}',(side*15.8,13,z),(.8,.9,.9),1)
 deck=a.joint('chassis_deck',(0,10,0))
 c('engine_hood',(0,20,-6),(24,8,22),0)
 for side in [-1,1]:
  c(f'hood_bevel_{side}',(side*12,20,-6),(2,7,23),4)
  c(f'hood_seam_{side}',(side*8,24.1,-6),(.5,.25,20),2)
 for j in range(8):c(f'radiator_grille_{j}',(-9.5+j*2.7,20,-17.3),(1,5,.65),6)
 for side in [-1,1]:
  c(f'headlamp_{side}',(side*10,21.5,-18),(4,3,1.5),7)
  c(f'hood_latch_{side}',(side*10,18.5,3),(1.4,2,1),6)
 hood=a.joint('crushable_hood',(0,17,-6))
 c('driver_seat',(0,19,12),(10,3,9),6);c('seat_back',(0,23,16),(10,8,2),6)
 c('steering_column',(0,20,5),(1.4,7,1.4),2)
 for j in range(12):
  q=j*math.tau/12;c(f'steering_wheel_{j}',(3*math.cos(q),23,5+3*math.sin(q)),(1.6,.8,1.6),6)
 for side in [-1,1]:
  c(f'roll_bar_{side}',(side*12,23,13),(1.8,14,1.8),0)
  c(f'grab_rail_{side}',(side*16,19,3),(1,5,1),6)
 c('roll_bar_crossbeam',(0,30,13),(26,1.8,1.8),0);c('short_exhaust',(10,24,-1),(2,8,2),2)
 cab=a.joint('crushable_cab',(0,17,12))
 clip=animation('crumple',3.6,{})
 for g,drop,sy in [(hood,1.6,.62),(deck,2.7,.8),(cab,4.5,.62)]:
  keys=[]
  for channel,values in [('position',[(0,(0,0,0)),(.9,(0,-drop*.2,0)),(1.8,(.4,-drop*.45,.2)),(2.7,(-.3,-drop*.75,.4)),(3.6,(.5,-drop,.6))]),('scale',[(0,(1,1,1)),(3.6,(1.03,sy,1.03))])]:
   keys+=animator(g,values,channel)['keyframes']
  clip['animators'][g['uuid']]={'name':g['name'],'type':'bone','keyframes':sorted(keys,key=lambda k:k['time'])}
 a.m.data['animations']=[clip];a.m.save()
 a=palette_model('motionrp_roller_debris',GOLD);a.cube('torn_yellow_sheet',(0,0,0),(6,1,5),0);a.cube('dark_broken_frame',(1,1,0),(2,3,4),6);a.joint('debris',(0,0,0));a.m.save()

def moon():
 a=palette_model('motionrp_rock_moon',ROCK);c=a.cube
 c('solid_rock_core',(0,0,0),(18,18,18),4)
 for row in range(11):
  y=-12.5+row*2.5;radius=math.sqrt(max(0,14*14-y*y));count=max(5,round(radius*1.4))
  for j in range(count):
   q=j*math.tau/count+row*.36;r=radius*(.96+.025*math.sin(j*7+row));w=3+((j+row)%3)/2
   c(f'accreted_stratum_{row}_{j}',(r*math.cos(q),y,r*math.sin(q)),(w,3.8,w),[1,2,3,5,6][(j*7+row)%5])
 a.joint('rock_body',(0,0,0));a.m.save()

def gravity_actor():
 m=json.loads((ROOT/'players/motionrp_chibaku_tensei.bbmodel').read_text());m['animations'][0]['length']=13
 save('players/motionrp_chibaku_tensei.bbmodel',m);(ROOT/'chibaku_tensei-hand-path.csv').write_text(hand_path(m,260))

def revise(rig):
 fusion(rig);fused_skin(rig);the_world(rig);roller();road_actor(rig);moon();gravity_actor()
