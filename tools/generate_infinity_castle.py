"""Original editable all-around impossible Japanese castle, sliding fusuma and falling player."""
import sys,math,json,hashlib,copy
sys.dont_write_bytecode=True
from generate_summons import Model,player,save,animation,animator,RIG_SHA
from generate_epic_revision import put_track
COLORS=[(18,10,7,255),(67,32,15,255),(132,70,25,255),(212,147,60,255),(238,188,103,255),(255,226,144,255),(127,42,23,255),(6,4,6,255)]

def fusuma():
 m=Model('motionrp_castle_fusuma',colors=COLORS);fixed=[]
 fixed.append(m.cube('black_passage',[0,0,1],[64,48,.4],7))
 for x in [-34,34]:fixed.append(m.cube('lacquer_jamb_'+str(x),[x,0,0],[3,54,3],1))
 for y in [-26,26]:
  fixed.append(m.cube('gold_track_'+str(y),[0,y,0],[72,2,4],3))
  fixed.append(m.cube('dark_track_'+str(y),[0,y+1.5,0],[72,1,5],0))
 m.group('portal_frame',fixed)
 clips=[animation('open',.85,{}),animation('close',.65,{}),animation('slide',2.4,{})];clips[-1]['loop']='loop'
 for side in [-1,1]:
  ids=[m.cube('painted_paper_'+str(side),[side*16,0,0],[32,48,1],4)]
  for x in [side*32,0]:ids.append(m.cube('panel_edge_'+str(side)+'_'+str(x),[x,0,-.4],[.8,48,1.2],0))
  for y in [-23.5,23.5]:ids.append(m.cube('panel_cross_'+str(side)+'_'+str(y),[side*16,y,-.4],[32,.8,1.2],0))
  ids.append(m.cube('recessed_handle_'+str(side),[side*3,-2,-.8],[1.7,3,.4],0))
  # Ink-like stepped mountain and flowering branch, on both faces.
  for j in range(7):
   x=side*(5+j*3.5);h=3+8*math.sin(j*.7)**2
   ids.append(m.cube('ink_ridge_'+str(side)+'_'+str(j),[x,-21+h/2,-.65],[3.6,h,.3],1))
   if j%2==0:ids.append(m.cube('gold_leaf_'+str(side)+'_'+str(j),[x,-6+j*2,-.7],[2,2,.3],3,rotation=[0,0,35]))
  g=m.group('sliding_panel_'+str(side),ids)
  for clip,keys in [(clips[0],[(0,(0,0,0)),(.12,(0,0,0)),(.8,(side*31,0,0))]),(clips[1],[(0,(side*31,0,0)),(.6,(0,0,0))]),(clips[2],[(0,(0,0,0)),(.8,(side*28,0,0)),(1.6,(side*28,0,0)),(2.4,(0,0,0))])]:clip['animators'][g['uuid']]=animator(g,keys,'position')
 m.data['animations']=clips;m.save()
 # Bake the floor orientation into a separate mesh. Yaw-only dummy rotators
 # must not be able to turn the portal back into an upright doorway.
 floor=copy.deepcopy(m.data);floor['name']=floor['model_identifier']='motionrp_castle_hatch'
 def point(p):return [p[0],-p[2],p[1]]
 mapping={'north':'up','south':'down','up':'south','down':'north','east':'east','west':'west'}
 for e in floor['elements']:
  a,b=point(e['from']),point(e['to']);e['from']=[min(x,y)for x,y in zip(a,b)];e['to']=[max(x,y)for x,y in zip(a,b)];e['origin']=point(e['origin'])
  if 'rotation'in e:e['rotation']=[e['rotation'][0],-e['rotation'][2],e['rotation'][1]]
  e['faces']={mapping[k]:v for k,v in e['faces'].items()}
 for g in floor['groups']:g['origin']=point(g['origin'])
 # Slide tracks remain along local X, parallel to the floor.
 save('models/motionrp_castle_hatch.bbmodel',floor)

def platform():
 m=Model('motionrp_castle_platform',colors=COLORS);ids=[]
 ids.append(m.cube('heavy_beam',[0,-3,0],[100,5,52],0))
 for x in range(-3,4):
  ids.append(m.cube('tatami_'+str(x),[x*14,0,0],[13.5,2,48],4))
  ids.append(m.cube('tatami_border_'+str(x),[x*14-6.5,1,0],[.7,.2,48],1))
 for z in [-25,25]:
  ids.append(m.cube('crimson_rail_'+str(z),[0,15,z],[102,2,2],6))
  ids.append(m.cube('lower_rail_'+str(z),[0,5,z],[102,1,1],3))
  for x in [-48,-24,0,24,48]:ids.append(m.cube('baluster_'+str((x,z)),[x,8,z],[2,16,2],1))
 m.group('moving_tatami_balcony',ids);m.save()

def castle():
 m=Model('motionrp_castle_chamber',colors=COLORS)
 def cube(ids,name,p,size,color,rot=None):ids.append(m.cube(name,p,size,color,rotation=rot))
 for sector in range(8):
  a=sector*math.tau/8;co,si=math.cos(a),math.sin(a);ids=[]
  def local(x,y,z):return [co*x+si*z,y,-si*x+co*z]
  def box(n,p,size,c):cube(ids,n+'s'+str(sector),local(*p),size,c,[0,sector*45,0])
  box('deep_shadow_wall',(0,0,440),(380,1900,4),7)
  # Protruding chambers, recessed corner towers and offset floor heights break
  # the uniform cylindrical shaft into overlapping independent buildings.
  for tier in range(-4,5):
   offset=((sector*3+tier*5)%5)*13-26
   y=tier*172+offset;front=182+((sector*7+tier*3)%5)*20+(45 if sector%2 else 0)
   width=244+((sector+tier)%3)*22;back=front+148;tag=str(tier)+'_'
   box(tag+'heavy_floor',(0,y-9,front+74),(width+16,16,160),0)
   for plank in range(9):box(tag+'cedar_plank'+str(plank),(-width/2+width*(plank+.5)/9,y,front+72),(width/9-.8,3,144),2 if plank%3 else 3)
   # Exposed ceiling, coffers and the warm strip below each balcony.
   box(tag+'luminous_under_eaves',(0,y-15,front+20),(width-4,4,18),5)
   for x in range(-100,101,25):
    box(tag+'underside_rib'+str(x),(x,y-18,front+68),(3,5,125),1)
    box(tag+'ceiling_rib'+str(x),(x,y+137,front+71),(3,4,144),1)
   for z in range(15,145,26):box(tag+'ceiling_cross'+str(z),(0,y+137,front+z),(width,4,3),1)
   box(tag+'coffered_ceiling',(0,y+140,front+72),(width+12,3,150),3)
   for x in [-width/2,0,width/2]:
    box(tag+'massive_column'+str(x),(x,y+67,back-3),(7,134,7),1)
    box(tag+'column_cap'+str(x),(x,y+134,back-3),(18,5,18),3)
   # Mixed fusuma and illuminated lattice windows, with a dark wood border.
   box(tag+'back_wall',(0,y+65,back+8),(width,126,5),1)
   for panel in range(6):
    x=-width/2+width*(panel+.5)/6;lit=(panel+sector+tier)%3!=0
    box(tag+'paper_screen'+str(panel),(x,y+66,back+4),(width/6-4,116,2),5 if lit else 4)
    box(tag+'screen_stile'+str(panel),(x-width/12,y+66,back+1),(3,122,3),1)
    if lit:
     for dx in [-12,-6,0,6,12]:box(tag+'kumiko_'+str((panel,dx)),(x+dx,y+66,back),(1,116,2),1)
     for dy in [12,29,46,63,80,97,114]:box(tag+'lattice_'+str((panel,dy)),(x,y+dy,back-1),(width/6-4,1.2,2),1)
    else:box(tag+'door_handle'+str(panel),(x-width/12+7,y+58,back-2),(3,5,1),0)
   for x in range(-110,111,20):box(tag+'baluster'+str(x),(x,y+20,front),(2,40,2),1)
   for dy in [7,22,39]:box(tag+'rail_'+str(dy),(0,y+dy,front),(width+12,2,3),1 if dy<39 else 3)
   for x in [-95,95]:
    box(tag+'lantern_paper'+str(x),(x,y+93,front+17),(15,20,15),5)
    for dy in [-11,11]:box(tag+'lantern_cap'+str((x,dy)),(x,y+93+dy,front+17),(18,2,18),1)
    for dx in [-7,0,7]:box(tag+'lantern_cage'+str((x,dx)),(x+dx,y+93,front+9),(1,20,1),3)
    box(tag+'lantern_cord'+str(x),(x,y+121,front+17),(1,36,1),1)
   if (sector+tier)%3!=1:
    # Long diagonal stairs with handrail posts and a landing on the next room.
    for step in range(18):
     x=-130+step*14;sy=y+step*7.8
     box(tag+'stair_tread'+str(step),(x,sy,front-32),(16,3,45),2)
     if step%2==0:
      box(tag+'stair_post'+str(step),(x,sy+16,front-55),(2,32,2),1)
      box(tag+'stair_handrail'+str(step),(x+6,sy+33,front-55),(29,3,3),3)
   if (sector+tier)%3==0:
    # A projecting enclosed room: side panels, transom and glowing lower grille.
    side=-1 if tier%2 else 1;rx=side*67;rz=front-40
    box(tag+'hanging_room_floor',(rx,y+55,rz),(102,9,84),0)
    box(tag+'hanging_room_front',(rx,y+90,rz-40),(98,65,3),4)
    for x in range(-40,41,16):box(tag+'room_panel'+str(x),(rx+x,y+90,rz-43),(2,65,3),1)
    for dy in [59,116]:box(tag+'room_transom'+str(dy),(rx,y+dy,rz-44),(106,5,5),1)
    box(tag+'room_glow',(rx,y+58,rz-44),(90,4,4),5)
  m.group('interlocking_halls_'+str(sector),ids)
 # Golden tree mural on an inset stage, inspired by the reference's warm focal wall.
 ids=[]
 def mural(n,p,size,c,rot=None):cube(ids,'mural_'+n,p,size,c,rot)
 mural('stage',[0,-30,190],[160,10,100],1);mural('gold_screen',[0,30,232],[156,112,3],3)
 for j in range(-5,6):mural('panel'+str(j),[j*14,30,229],[1,112,1],1)
 for j in range(11):
  y=-17+j*8;x=9*math.sin(j*.35)
  mural('trunk'+str(j),[x,y,226],[5+(.5*(10-j)),10,1],0)
  if j>2:
   for sign in [-1,1]:
    for k in range(4):mural('branch'+str((j,sign,k)),[x+sign*(k+1)*10,y+k*4,226],[13,2,1],0,[0,0,sign*22])
 m.group('golden_tree_stage',ids)
 ids=[]
 for y in [-940,940]:cube(ids,'dark_void_cap'+str(y),[0,y,0],[960,3,960],7)
 m.group('abyss',ids);m.save()

def actor(rig):
 m=player(rig,'infinity_castle',10.7,{})
 poses={
 'player_root':[(0,(0,0,0)),(.35,(0,0,0)),(.7,(40,0,-3)),(1,(88,0,4)),(1.5,(95,0,7)),(4,(86,0,-7)),(6,(94,0,6)),(8.8,(90,0,-3)),(9.15,(65,0,0)),(9.9,(0,0,0)),(10.7,(0,0,0))],
 'pc_chest':[(0,(0,0,0)),(1,(-5,0,0)),(4,(3,4,-3)),(8.8,(-4,0,2)),(9.9,(15,0,0)),(10.7,(0,0,0))],
 'h_ph_head':[(0,(0,0,0)),(1,(-8,0,0)),(8.8,(-5,0,0)),(9.9,(0,0,0))],
 'pra_right_arm':[(0,(0,0,0)),(1,(15,0,62)),(4,(-10,0,75)),(8.8,(10,0,65)),(9.9,(30,0,20)),(10.7,(0,0,0))],
 'pla_left_arm':[(0,(0,0,0)),(1,(5,0,-72)),(4,(20,0,-60)),(8.8,(-5,0,-75)),(9.9,(25,0,-20)),(10.7,(0,0,0))],
 'prfa_right_forearm':[(0,(0,0,0)),(1,(22,0,0)),(8.8,(32,0,0)),(10.7,(0,0,0))],
 'plfa_left_forearm':[(0,(0,0,0)),(1,(35,0,0)),(8.8,(20,0,0)),(10.7,(0,0,0))],
 'prl_right_leg':[(0,(0,0,0)),(1,(8,0,15)),(4,(-8,0,20)),(8.8,(10,0,15)),(9.9,(18,0,7)),(10.7,(0,0,0))],
 'pll_left_leg':[(0,(0,0,0)),(1,(-12,0,-18)),(4,(12,0,-12)),(8.8,(-8,0,-20)),(9.9,(15,0,-7)),(10.7,(0,0,0))],
 'prfl_right_foreleg':[(0,(0,0,0)),(1,(-20,0,0)),(8.8,(-30,0,0)),(9.9,(-25,0,0)),(10.7,(0,0,0))],
 'plfl_left_foreleg':[(0,(0,0,0)),(1,(-35,0,0)),(8.8,(-18,0,0)),(9.9,(-25,0,0)),(10.7,(0,0,0))]}
 for bone,keys in poses.items():put_track(m,bone,keys)
 save('players/motionrp_infinity_castle.bbmodel',m)

if __name__=='__main__':
 raw=open(sys.argv[1],'rb').read();assert hashlib.sha256(raw).hexdigest()==RIG_SHA
 actor(json.loads(raw));fusuma();platform();castle()
