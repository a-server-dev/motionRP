"""Sculpted chakra creatures and faceted armour; preserves the existing animation rig.
All geometry is original. Sizes and pivots remain compatible with the shared duel clock.
"""
import math,copy
from generate_energy import mesh_model,add_mesh
from generate_summons import ident

def sub(a,b):return tuple(x-y for x,y in zip(a,b))
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def cross(a,b):return(a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def unit(a):
 d=math.sqrt(dot(a,a));return tuple(x/max(1e-8,d)for x in a)

def tube(m,name,points,radii,color=1,sides=10,flatten=1):
 vs={};faces=[]
 for j,p in enumerate(points):
  tangent=unit(sub(points[min(j+1,len(points)-1)],points[max(0,j-1)]));axis=(0,0,1)if abs(tangent[2])<.9 else(0,1,0);u=unit(cross(tangent,axis));v=cross(tangent,u)
  for k in range(sides):
   q=k*math.tau/sides;vs[f'{j}_{k}']=[p[a]+radii[j]*(u[a]*math.cos(q)+v[a]*math.sin(q)*flatten)for a in range(3)]
  if j:
   for k in range(sides):faces.append(([f'{j-1}_{k}',f'{j-1}_{(k+1)%sides}',f'{j}_{(k+1)%sides}',f'{j}_{k}'],color if k%5 else max(0,color-1)))
 for j,reverse in [(0,True),(len(points)-1,False)]:
  vs['cap'+str(j)]=list(points[j])
  for k in range(sides):faces.append((['cap'+str(j),f'{j}_{(k+1)%sides}'if reverse else f'{j}_{k}',f'{j}_{k}'if reverse else f'{j}_{(k+1)%sides}'],color))
 return add_mesh(m,name,vs,faces)

def oval(m,name,center,radii,color=1,rows=12,sides=20):
 vs={};fs=[]
 for j in range(rows+1):
  p=math.pi*j/rows
  for k in range(sides):
   q=k*math.tau/sides;vs[f'{j}_{k}']=[center[0]+radii[0]*math.sin(p)*math.cos(q),center[1]+radii[1]*math.cos(p),center[2]+radii[2]*math.sin(p)*math.sin(q)]
 for j in range(rows):
  for k in range(sides):
   ids=[f'{j}_{k}',f'{j}_{(k+1)%sides}',f'{j+1}_{(k+1)%sides}',f'{j+1}_{k}'];c=color if k%6 else max(0,color-1)
   if j==0:ids=[ids[0],ids[2],ids[3]]
   elif j==rows-1:ids=ids[:3]
   fs.append((ids,c))
 return add_mesh(m,name,vs,fs)

def plate(m,name,outline,depth,color=1):
 # Convex, bevelled shield: a raised inner face, sloping border, solid rear.
 # Mirroring a panel must not reverse the visible face's winding.
 signed=sum(p[0]*q[1]-q[0]*p[1]for p,q in zip(outline,outline[1:]+outline[:1]))
 if signed>0:outline=list(reversed(outline))
 center=tuple(sum(p[a]for p in outline)/len(outline)for a in range(3));vs={};n=len(outline);fs=[]
 for i,p in enumerate(outline):
  vs['o'+str(i)]=list(p);vs['i'+str(i)]=[center[a]+(p[a]-center[a])*.82-(depth if a==2 else 0)for a in range(3)];vs['b'+str(i)]=[p[0],p[1],p[2]+depth*.3]
 vs['front']=[center[0],center[1],center[2]-depth*1.12];vs['back']=[center[0],center[1],center[2]+depth*.3]
 for i in range(n):
  k=(i+1)%n;fs.extend([(['o'+str(i),'o'+str(k),'i'+str(k),'i'+str(i)],min(7,color+1)),(['front','i'+str(i),'i'+str(k)],color),(['o'+str(k),'o'+str(i),'b'+str(i),'b'+str(k)],max(0,color-1)),(['back','b'+str(k),'b'+str(i)],color)])
 return add_mesh(m,name,vs,fs)

class Sculpt:
 def __init__(self,old,palette):
  self.old=old;self.palette=palette;self.m=mesh_model(old['name'],palette);self.m.data['animations']=copy.deepcopy(old['animations']);self.groups={g['name']:g for g in old['groups']};self.nodes={}
 def attach(self,name,ids,parent=None):
  g=copy.deepcopy(self.groups[name]);self.m.data['groups'].append(g);node={'uuid':g['uuid'],'children':ids};self.nodes[name]=node
  (self.nodes[parent]['children'] if parent else self.m.data['outliner']).append(node)
 def finish(self):
  fox='kurama' in self.old['name'];roots=self.m.data['outliner'][:];names={g['uuid']:g['name']for g in self.m.data['groups']}
  upper=[n for n in roots if not names[n['uuid']].startswith(('fox_leg_','nine_tail_','susanoo_leg_'))]
  torso=self.m.group('combat_torso',[],origin=(0,42 if fox else 64,0));self.m.data['outliner']=[n for n in roots if n not in upper]+[{'uuid':torso['uuid'],'children':upper}]
  from generate_summons import animator
  for clip in self.m.data['animations']:
   if clip['name']=='release':clip['animators'][torso['uuid']]=animator(torso,[(0,(0,0,0)),(.4,(0,0,0)),(.7,(12,0,0)if fox else(-5,-8,0)),(1.2,(8,0,0)if fox else(-3,-5,0)),(2,(0,0,0))])
  for g in self.m.data['groups']:g['light_emission']=8
  for e in self.m.data['elements']:e['light_emission']=8
  palette=self.palette;material_texture(self.m,palette);return self.m.data

def fox(old,asura,palette):
 a=Sculpt(old,palette);m=a.m
 for side in [-1,1]:
  x=side*9;ids=[tube(m,'digitigrade_haunch_'+str(side),[(x,43,2),(x+side*2,33,4),(x,23,7),(x,10,1)],[6,6.8,4.2,3],1,12),oval(m,'fox_ankle_'+str(side),(x,8,0),(4.3,7,4.5),0),oval(m,'splayed_fox_paw_'+str(side),(x,3,-5),(6.5,3.3,10),1)]
  for toe in range(3):ids.append(tube(m,'ivory_claw_'+str(side)+'_'+str(toe),[(x-4+toe*4,3,-10),(x-4+toe*4,2,-15),(x-4+toe*4,1,-17)],[1.5,1,.08],3,6))
  a.attach('fox_leg_'+str(side),ids)
 ids=[tube(m,'sculpted_fox_torso',[(0,41,1),(0,49,1),(0,59,0),(0,69,0),(0,78,1),(0,84,2)],[10,10.5,12,16,17,10],1,20,.64)]
 # Deep pectoral masses and a tapering abdomen, rather than stacked slabs.
 for side in [-1,1]:
  ids.append(oval(m,'pectoral_'+str(side),(side*8,75,-4),(9.5,8,6),1))
  for row in range(3):ids.append(oval(m,'abdominal_'+str(side)+'_'+str(row),(side*4,56+row*5,-6),(4.8,3.2,2.6),1,6,10))
  for mark in range(3):
   pts=[(side*(3+j*2.3),79-mark*4-j*.65,-10+.15*j*j)for j in range(5)];ids.append(tube(m,'curved_chest_seal_'+str(side)+'_'+str(mark),pts,[.7]*5,3,6))
  for j in range(3):ids.append(oval(m,'magatama_'+str(side)+'_'+str(j),(side*(3+j*3.3),83-j*1.8,-8.8),(1.3,2,.5),3,6,8))
  for plume in range(4):
   pts=[(side*(12+plume*1.4),80-plume,2),(side*(21+plume*2),83-plume,3),(side*(23+plume*3),88-plume*2,4)];ids.append(tube(m,'shoulder_chakra_flame_'+str(side)+'_'+str(plume),pts,[3.8,2.4,.08],0,8))
 pts=[(5.5*math.cos(j*math.tau/40),51+5.5*math.sin(j*math.tau/40),-7.7)for j in range(41)];ids.append(tube(m,'spiral_abdomen_seal',pts,[.55]*41,3,5));a.attach('chakra_body',ids)
 for i,(x,y,z,size)in enumerate([(-16,88,3,14),(0,95,-2,18),(16,88,3,14)]if asura else[(0,90,0,20)]):
  label='fox_head_'+str(i);ids=[]
  def ov(n,p,r,c=1):ids.append(oval(m,label+'_'+n,p,r,c))
  ov('sculpted_skull',(x,y,z),(size*.51,size*.48,size*.40),0)
  ov('tapered_muzzle',(x,y-size*.18,z-size*.48),(size*.31,size*.20,size*.48),1)
  ov('black_nose',(x,y-size*.13,z-size*.88),(size*.12,size*.07,size*.08),3)
  ov('open_jaw_shadow',(x,y-size*.34,z-size*.51),(size*.32,size*.12,size*.38),3)
  ov('angular_lower_jaw',(x,y-size*.41,z-size*.46),(size*.34,size*.08,size*.36),0)
  for side in [-1,1]:
   q=side
   ear=[(x+q*size*.29,y+size*.27,z),(x+q*size*.58,y+size*.95,z+size*.09),(x+q*size*.54,y+size*.23,z-size*.10)]
   ids.append(plate(m,label+'_pointed_ear_'+str(side),ear,size*.10,0));inner=[(x+q*size*.37,y+size*.34,z-size*.11),(x+q*size*.56,y+size*.81,z-size*.03),(x+q*size*.49,y+size*.32,z-size*.14)];ids.append(plate(m,label+'_ear_ink_'+str(side),inner,.15,3))
   eye=[(x+q*size*.10,y+size*.02,z-size*.39),(x+q*size*.42,y+size*.17,z-size*.28),(x+q*size*.34,y-size*.04,z-size*.41),(x+q*size*.17,y-size*.08,z-size*.43)]
   ids.append(plate(m,label+'_fierce_eye_socket_'+str(side),eye,.6,3));ov('eye_iris_'+str(side),(x+q*size*.25,y+size*.015,z-size*.45),(size*.085,size*.055,.3),4);ov('slit_pupil_'+str(side),(x+q*size*.25,y+size*.015,z-size*.47),(.24,size*.055,.17),3)
   for stripe in range(3):
    pts=[(x+q*size*(.25+j*.075),y-size*(.08+stripe*.12+j*.035),z-size*(.43-j*.045))for j in range(4)];ids.append(tube(m,label+'_cheek_ink_'+str(side)+'_'+str(stripe),pts,[size*.018]*4,3,5))
   ids.append(tube(m,label+'_upper_fang_'+str(side),[(x+q*size*.20,y-size*.27,z-size*.65),(x+q*size*.20,y-size*.42,z-size*.66)],[size*.055,.03],5,7))
   for fur in range(4):ids.append(tube(m,label+'_cheek_flare_'+str(side)+'_'+str(fur),[(x+q*size*.4,y-size*.02,z),(x+q*size*(.7+fur*.035),y-size*(.08+fur*.1),z+size*.1),(x+q*size*.48,y-size*(.32+fur*.06),z+size*.14)],[size*.13,size*.07,.04],0,7))
  for side in [-1,1]:
   pts=[(x+side*size*(.07+j*.085),y+size*(.28-j*.03),z-size*(.37-j*.025))for j in range(5)];ids.append(tube(m,label+'_ink_brow_'+str(side),pts,[size*.025]*5,3,6))
  ids.append(tube(m,label+'_forehead_chakra_crest',[(x,y+size*.43,z-size*.24),(x,y+size*.28,z-size*.37),(x,y+size*.12,z-size*.42)],[size*.045,size*.025,.05],3,6))
  a.attach(label,ids)
 for i in range(9):
  angle=(i-4)*math.pi/6;pts=[];rs=[]
  for j in range(33):
   t=j/32;r=5+53*t;pts.append((math.sin(angle)*r+3*math.sin(math.pi*t*2+i*.4)*t,40+math.cos(angle)*r+5*math.sin(math.pi*t),12+18*math.sin(math.pi*t*.8)+7*t));rs.append(.08+6.5*(1-t)**.7)
  ids=[tube(m,'continuous_tapered_tail_'+str(i),pts,rs,1,12)]
  seam=[(p[0],p[1],p[2]-r*.96)for p,r in zip(pts,rs)];ids.append(tube(m,'flowing_tail_seal_'+str(i),seam,[max(.03,r*.14)for r in rs],3,5));a.attach('nine_tail_'+str(i),ids)
 for row in range(3 if asura else 1):
  for side in [-1,1]:
   x=side*19;y=81;ids=[oval(m,'deltoid_'+str(row)+'_'+str(side),(x,y-3,0),(5.7,6,5.5),1),tube(m,'bicep_'+str(row)+'_'+str(side),[(x,y-3,0),(x+side*.7,y-10,-.3),(x,y-20,0)],[4.5,5,3.2],1,12)]
   upper='fox_upper_'+str(row)+'_'+str(side);a.attach(upper,ids)
   ids=[tube(m,'tapered_forearm_'+str(row)+'_'+str(side),[(x,y-20,0),(x,y-27,-.3),(x,y-37,-1)],[3.3,4.5,3],1,12),oval(m,'anatomical_palm_'+str(row)+'_'+str(side),(x,y-40,-1),(5.6,5,4),1,8,12)]
   for finger in range(4):
    px=x-4.2+finger*2.8;length=8 if asura and row==0 else 4;pts=[(px,y-42,-2),(px,y-45,-3),(px,y-42-length,-3.5)];ids.append(tube(m,'articulated_finger_'+str(row)+'_'+str(side)+'_'+str(finger),pts,[1.15,1,.65],0,7))
   ids.append(tube(m,'opposed_thumb_'+str(row)+'_'+str(side),[(x+side*4,y-38,-2),(x+side*7,y-41,-3),(x+side*6,y-44,-4)],[1.8,1.4,.8],0,8));a.attach('fox_fore_'+str(row)+'_'+str(side),ids,upper)
 return a.finish()

def susanoo(old,indra,palette):
 a=Sculpt(old,palette);m=a.m
 for side in [-1,1]:
  x=side*10;ids=[tube(m,'sculpted_armored_leg_'+str(side),[(x,49,1),(x,36,1),(x,29,0),(x,17,1),(x,6,0)],[6,5.6,5,5.5,4],1,10,1.1)]
  for row in range(5):
   y=9+row*7;ids.append(plate(m,'overlapping_shin_plate_'+str(side)+'_'+str(row),[(x-5.5,y+4,-5),(x+5.5,y+4,-5),(x+4,y-4,-6),(x,y-6,-7),(x-4,y-4,-6)],1.4,1))
  ids.append(plate(m,'angular_knee_guard_'+str(side),[(x,y0,z)for x,y0,z in [(x-6,34,-6),(x,39,-8),(x+6,34,-6),(x,27,-9)]],1.7,2))
  ids.append(oval(m,'armored_foot_'+str(side),(x,3,-4),(6.5,3.5,11),1,6,10));a.attach('susanoo_leg_'+str(side),ids)
 ids=[]
 for strip in range(9):
  x=(strip-4)*4.5;ids.append(plate(m,'separate_skirt_lamella_'+str(strip),[(x-2.3,55,-10),(x+2.3,55,-10),(x+3.3,37,-12),(x-3.3,37,-12)],1.2,1 if strip%2 else 0))
 a.attach('susanoo_waist',ids)
 ids=[tube(m,'armored_ribcage',[(0,54,0),(0,62,0),(0,72,0),(0,81,1),(0,85,1)],[12,15,18,19,11],1,14,.64)]
 for side in [-1,1]:
  ids.append(plate(m,'deep_pectoral_plate_'+str(side),[(side*1,80,-10),(side*15,83,-8),(side*18,73,-10),(side*6,69,-13),(side*1,73,-12)],2,1))
  for rib in range(6):
   pts=[(side*(3+j*2.7),61+rib*3.3+j*.3,-10+max(0,j-2)*1.2)for j in range(6)];ids.append(tube(m,'rib_energy_channel_'+str(side)+'_'+str(rib),pts,[.30 if indra else .22]*6,5 if indra else 2,5))
  ids.append(tube(m,'collar_contour_'+str(side),[(side*2,86,-5),(side*9,85,-8),(side*18,81,-5)],[1.5,1.3,1],2,8))
 for row in range(4):ids.append(plate(m,'abdominal_armor_'+str(row),[(-5,58+row*4,-10),(5,58+row*4,-10),(4,54+row*4,-10),(-4,54+row*4,-10)],1.1,1))
 if indra:ids.append(oval(m,'luminous_chest_chakra',(0,75,-13),(2.2,6.5,1.2),5,8,10))
 ids.append(tube(m,'armored_neck',[(0,83,2),(0,95,2)],[7,6.8],1,10));a.attach('susanoo_chest',ids)
 ids=[oval(m,'tengu_helmet_shell',(0,98,1),(11,12,9),1,12,16)]
 for side in [-1,1]:
  ids.append(plate(m,'swept_tengu_brow_'+str(side),[(side*.5,103,-8),(side*11,106,-4),(side*9,99,-9),(side*3,98,-10)],2.3,2))
  ids.append(plate(m,'luminous_almond_eye_'+str(side),[(side*2,99,-11),(side*8,102,-9.4),(side*7,98,-10.8),(side*3,97.8,-11.4)],.2,7))
  for row in range(4):ids.append(plate(m,'layered_cheek_lamella_'+str(side)+'_'+str(row),[(side*7,96-row*2,-8),(side*13,97-row*2,-2),(side*12,92-row*2,3),(side*8,91-row*2,-6)],1,1))
  pts=[(side*7,106,0),(side*11,112,1),(side*14,120,4),(side*13,125,7)];ids.append(tube(m,'swept_crown_horn_'+str(side),pts,[3.2,2.5,1.3,.06],2,9))
 ids.append(plate(m,'long_tengu_beak',[(-3,99,-10),(3,99,-10),(2,92,-16),(0,88,-19),(-2,92,-16)],2.4,2))
 ids.append(plate(m,'razor_forehead_crest',[(-1.3,102,-7),(0,119,-4),(1.3,102,-7),(0,98,-10)],1.7,2));a.attach('sasuke_mask',ids)
 for side in [-1,1]:
  ids=[]
  # An actual connected membrane and long pointed feathers, instead of bars.
  for j in range(16):
   x=side*(21+j*3.4);top=104-j*1.6;bottom=top-24-j*.9+4*math.sin(j*.7);outline=[(x,top,15),(x+side*5,top-2,16),(x+side*9,bottom-5,19),(x+side*3,bottom,18),(x-side*1,top-7,15)]
   ids.append(plate(m,'swept_wing_blade_'+str(side)+'_'+str(j),outline,.8,1 if j%3 else 0))
   ids.append(tube(m,'feather_spine_'+str(side)+'_'+str(j),[(x,top,14.6),(x+side*3,(top+bottom)/2,16.4),(x+side*8,bottom-4,18.5)],[.4,.3,.06],5 if indra else 2,5))
  ids.append(tube(m,'continuous_wing_leading_edge_'+str(side),[(side*18,83,14)]+[(side*(21+j*3.4),104-j*1.6,14)for j in range(16)],[3.4]+[3.0-j*.15 for j in range(16)],2,9));a.attach('susanoo_wing_'+str(side),ids)
 for side in [-1,1]:
  x=side*26;ids=[oval(m,'rounded_shoulder_armor_'+str(side),(x,80,1),(10,8,10),1,8,12)]
  for row in range(4):ids.append(plate(m,'shoulder_lamella_'+str(side)+'_'+str(row),[(x-10,84-row*3,-6),(x,87-row*3,-10),(x+10,84-row*3,-6),(x+9,80-row*3,-7),(x-9,80-row*3,-7)],1.5,1))
  ids.append(tube(m,'tapered_upper_arm_'+str(side),[(x,78,0),(x,69,0),(x,59,0)],[5.2,6,4],1,10));upper='susanoo_arm_'+str(side);a.attach(upper,ids)
  ids=[tube(m,'solid_forearm_'+str(side),[(x,59,0),(x,49,0),(x,41,0)],[4.3,6,4.5],1,10)]
  for row in range(4):
   y=44+row*4;ids.append(plate(m,'vambrace_plate_'+str(side)+'_'+str(row),[(x-6,y+3,-5),(x+6,y+3,-5),(x+4.5,y-3,-7),(x-4.5,y-3,-7)],1.2,1))
  ids.append(oval(m,'articulated_gauntlet_'+str(side),(x,37,-1),(6.5,5.5,6),1,6,10))
  for finger in range(4):ids.append(tube(m,'gauntlet_finger_'+str(side)+'_'+str(finger),[(x-4.5+finger*3,39,-5),(x-4.5+finger*3,35,-6),(x-4.5+finger*3,33,-3)],[1.2,1.3,.9],2,6))
  a.attach('susanoo_forearm_'+str(side),ids,upper)
 pts=[(-26,81+46*math.sin(math.radians(-100+j*200/48)),-30-14*math.cos(math.radians(-100+j*200/48)))for j in range(49)];ids=[tube(m,'continuous_chakra_bow',pts,[1.6+1.0*math.sin(math.pi*j/48)for j in range(49)],5 if indra else 2,10)]
 for segment,(p,q)in enumerate([((-26,126,-28),(-18,81,0)),((-18,81,0),(-26,36,-28))]):ids.append(tube(m,'taut_bowstring_'+str(segment),[p,q],[.22,.22],7,6))
 a.attach('indra_bow',ids)
 def proportions(p,head=False,leg=False):
  x,y,z=p
  if head:return[x*.75*.84,107+(y-97)*.75,z*.75]
  return[x*.84,y*1.2 if leg else y+10,z]
 head_ids=set(a.nodes['sasuke_mask']['children']);leg_ids=set(a.nodes['susanoo_leg_-1']['children']+a.nodes['susanoo_leg_1']['children'])
 for e in m.data['elements']:
  for key,v in e['vertices'].items():e['vertices'][key]=proportions(v,e['uuid']in head_ids,e['uuid']in leg_ids)
 for g in m.data['groups']:
  g['origin']=proportions(g['origin'],g['name']=='sasuke_mask',g['name'].startswith('susanoo_leg_'))
 return a.finish()

def material_texture(m,palette,energy=False):
 """Vanilla cutout-compatible perforated veils; partial alpha is not relied on."""
 import io,base64,random
 from PIL import Image
 image=Image.new('RGBA',(256,128));rng=random.Random(730)
 for y in range(128):
  for x in range(256):
   index=x//32;c=palette[index];coverage=c[3]/255
   wave=.045*math.sin(y*.16+x*.23)+.025*math.sin(y*.41-x*.17);grain=(rng.random()-.5)*(.035 if not energy else .07)
   strength=.96+wave+grain;alpha=255 if coverage>=.99 or rng.random()<coverage else 0
   image.putpixel((x,y),tuple(max(0,min(255,round(v*strength)))for v in c[:3])+(alpha,))
 raw=io.BytesIO();image.save(raw,format='PNG');m.data['resolution']={'width':16,'height':16};m.data['textures'][0].update(width=256,height=128,uv_width=16,uv_height=16,source='data:image/png;base64,'+base64.b64encode(raw.getvalue()).decode())
 # Expand the palette sample to carry material detail and dither across faces.
 for e in m.data['elements']:
  if e['type']!='mesh':continue
  for f in e['faces'].values():
   old=list(f['uv'].values());index=min(7,int(old[0][0]//2));coords=[(index*2+.12,.2),(index*2+1.88,.2),(index*2+1.88,15.8)]
   f['uv']={key:list(coords[i%3])for i,key in enumerate(f['vertices'])}
 return m
