"""Original editable all-around impossible Japanese castle, sliding fusuma and falling player."""
import sys,math,json,hashlib
sys.dont_write_bytecode=True
from generate_summons import Model,player,save,animation,animator,RIG_SHA
from generate_epic_revision import put_track
COLORS=[(22,12,15,255),(80,32,23,255),(143,63,32,255),(208,146,66,255),(239,218,163,255),(255,188,72,255),(153,32,26,255),(7,6,11,255)]

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
 # Eight sides enclose the fall path all the way above and below the camera.
 for sector in range(8):
  a=sector*math.tau/8;co,si=math.cos(a),math.sin(a);ids=[]
  def local(x,y,z):return [co*x+si*z,y,-si*x+co*z]
  def box(n,p,size,c):cube(ids,n+str(sector),local(*p),size,c,[0,sector*45,0])
  box('deep_shadow_wall',(0,0,408),(350,1900,4),7)
  for tier in range(-4,5):
   y=tier*168;tag=str(tier)+'_'
   box(tag+'tier_beam',(0,y-10,304),(272,14,144),0)
   for plank in range(9):box(tag+'plank'+str(plank),(-120+plank*30,y,300),(29,3,140),2 if plank%3 else 3)
   for x in [-128,-64,0,64,128]:
    box(tag+'pillar'+str(x),(x,y+58,357),(7,116,7),1)
    box(tag+'cap'+str(x),(x,y+116,357),(14,5,14),3)
   box(tag+'cream_wall',(0,y+57,386),(260,108,3),4)
   for x in range(-120,121,20):box(tag+'window_mullion'+str(x),(x,y+56,382),(2,100,2),1)
   for dy in [12,34,56,78,100]:box(tag+'window_rail'+str(dy),(0,y+dy,381),(256,2,2),1)
   for x in range(-120,121,24):box(tag+'railing_post'+str(x),(x,y+20,235),(3,40,3),6)
   box(tag+'railing',(0,y+39,235),(272,4,4),6)
   box(tag+'gold_rail',(0,y+42,235),(276,1.5,5),3)
   for roof in range(4):box(tag+'eaves'+str(roof),(0,y+126+roof*4,326+roof*10),(294-roof*8,3,156-roof*12),0 if roof%2 else 1)
   # Lanterns have cages, lit paper, top and tassel.
   for x in [-100,100]:
    box(tag+'lantern_paper'+str(x),(x,y+80,257),(13,22,13),5)
    for dy in [-12,12]:box(tag+'lantern_cap'+str((x,dy)),(x,y+80+dy,257),(17,3,17),0)
    for dx in [-7,7]:box(tag+'lantern_cage'+str((x,dx)),(x+dx,y+80,250),(1,24,1),1)
    box(tag+'lantern_cord'+str(x),(x,y+107,257),(1,30,1),3)
   if (sector+tier)%2==0:
    for step in range(14):box(tag+'impossible_stair'+str(step),(-110+step*16,y+step*8,206),(18,4,38),2)
  m.group('stacked_halls_'+str(sector),ids)
 ids=[]
 for y in [-940,940]:cube(ids,'dark_void_cap'+str(y),[0,y,0],[900,3,900],7)
 m.group('abyss',ids);m.save()

def actor(rig):
 m=player(rig,'infinity_castle',10.7,{})
 poses={'pc_chest':[(0,(0,0,0)),(.65,(12,0,0)),(1.1,(-35,0,8)),(2,(-25,5,-12)),(4,(-38,-10,15)),(6,(-24,10,-8)),(8.6,(-35,0,8)),(9,(10,0,0)),(9.9,(20,0,0)),(10.6,(0,0,0))],
 'pra_right_arm':[(0,(0,0,0)),(1,(135,0,40)),(4,(115,0,65)),(7,(145,0,35)),(9,(90,0,30)),(10.7,(0,0,0))],
 'pla_left_arm':[(0,(0,0,0)),(1,(125,0,-55)),(4,(145,0,-35)),(7,(115,0,-65)),(9,(90,0,-30)),(10.7,(0,0,0))],
 'prfa_right_forearm':[(0,(0,0,0)),(1,(45,0,0)),(8.8,(35,0,0)),(10.7,(0,0,0))],
 'plfa_left_forearm':[(0,(0,0,0)),(1,(55,0,0)),(8.8,(40,0,0)),(10.7,(0,0,0))],
 'prl_right_leg':[(0,(0,0,0)),(1,(25,0,18)),(4,(10,0,22)),(8.7,(30,0,15)),(10,(22,0,8)),(10.7,(0,0,0))],
 'pll_left_leg':[(0,(0,0,0)),(1,(-15,0,-20)),(4,(25,0,-14)),(8.7,(-5,0,-22)),(10,(15,0,-8)),(10.7,(0,0,0))],
 'prfl_right_foreleg':[(0,(0,0,0)),(1,(-45,0,0)),(8.7,(-30,0,0)),(10,(-35,0,0)),(10.7,(0,0,0))],
 'plfl_left_foreleg':[(0,(0,0,0)),(1,(-25,0,0)),(8.7,(-50,0,0)),(10,(-35,0,0)),(10.7,(0,0,0))]}
 for bone,keys in poses.items():put_track(m,bone,keys)
 save('players/motionrp_infinity_castle.bbmodel',m)

if __name__=='__main__':
 raw=open(sys.argv[1],'rb').read();assert hashlib.sha256(raw).hexdigest()==RIG_SHA
 actor(json.loads(raw));fusuma();platform();castle()
