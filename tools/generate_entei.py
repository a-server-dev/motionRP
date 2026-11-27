"""Editable great flame circle and a curved solar fireball with animated corona."""
import sys,math,json,hashlib,base64,io
from PIL import Image
sys.dont_write_bytecode=True
from pathlib import Path
from generate_summons import player,save,animation,animator,RIG_SHA
from generate_energy import mesh_model,add_mesh,glow
FIRE=[(255,81,7,255),(255,202,44,255),(174,20,3,255),(255,249,167,255),(255,132,8,255),(255,224,78,255),(221,45,4,255),(255,255,225,255)]
def generate(rig):
 m=player(rig,'entei',9.5,{
  'pra_right_arm':[(0,(0,0,0)),(1.2,(50,0,20)),(2,(145,0,8)),(4.5,(172,0,4)),(5.5,(165,-15,5)),(6.0,(95,0,0)),(7,(95,0,0)),(8.7,(20,0,4)),(9.5,(0,0,0))],
  'prfa_right_forearm':[(0,(0,0,0)),(1.2,(30,0,0)),(2,(15,0,0)),(4.5,(5,0,0)),(5.5,(10,0,0)),(6.0,(0,0,0)),(9.5,(0,0,0))],
  'pla_left_arm':[(0,(0,0,0)),(1,(30,0,-35)),(2,(55,0,-25)),(4.5,(55,0,-25)),(5.5,(20,0,-20)),(6.0,(-30,0,-15)),(8,(-30,0,-15)),(9.5,(0,0,0))],
  'plfa_left_forearm':[(0,(0,0,0)),(1,(45,0,0)),(5.5,(45,0,0)),(6.0,(60,0,0)),(9.5,(0,0,0))],
  'pc_chest':[(0,(0,0,0)),(2,(-8,0,0)),(4.5,(-12,0,0)),(5.5,(-18,-15,0)),(6.0,(22,10,0)),(8,(15,8,0)),(9.5,(0,0,0))],
  'h_ph_head':[(0,(0,0,0)),(2,(-20,0,0)),(5.5,(-25,0,0)),(6.0,(0,0,0)),(9.5,(0,0,0))],
  'prl_right_leg':[(0,(0,0,0)),(1,(-12,0,10)),(6.0,(-20,0,12)),(8,(-20,0,12)),(9.5,(0,0,0))],
  'pll_left_leg':[(0,(0,0,0)),(1,(20,0,-10)),(6.0,(35,0,-12)),(8,(35,0,-12)),(9.5,(0,0,0))],
  'plfl_left_foreleg':[(0,(0,0,0)),(1,(-30,0,0)),(6.0,(-45,0,0)),(8,(-45,0,0)),(9.5,(0,0,0))]})
 # These are direct Blockbench joint angles, not the older reversed pose table.
 for tr in m['animations'][0]['animators'].values():
  for k in tr['keyframes']:
   if k['channel']=='rotation':k['data_points'][0]['x']=str(-float(k['data_points'][0]['x']))
 save('players/motionrp_entei.bbmodel',m)
 m=mesh_model('motionrp_entei_sun',FIRE);vertices={};faces=[];lat,lon=32,64
 for j in range(lat+1):
  p=math.pi*j/lat
  for k in range(lon):
   q=math.tau*k/lon;r=16*(1+.018*math.sin(q*7+p*11));vertices[f'{j}_{k}']=[r*math.sin(p)*math.cos(q),r*math.cos(p),r*math.sin(p)*math.sin(q)]
 for j in range(lat):
  for k in range(lon):
   color=1 if (k*7+j*11)%13<3 else 4 if (k+j*3)%7<2 else 0
   faces.append(([f'{j}_{k}',f'{j}_{(k+1)%lon}',f'{j+1}_{(k+1)%lon}',f'{j+1}_{k}'],color))
 uid=add_mesh(m,'molten_solar_surface',vertices,faces);core=m.group('solar_core',[uid]);a=animation('burn',2,{});a['loop']='loop';a['animators'][core['uuid']]=animator(core,[(0,(0,0,0)),(1,(0,180,0)),(2,(0,360,0))])
 for strand in range(8):
  vertices={};faces=[]
  for j in range(33):
   t=j/32;p=math.pi*(.05+.9*t);q=strand*math.tau/8+t*4;radius=16.3+.8*math.sin(t*math.pi*4+strand)**2
   for side in [-1,1]:
    u=q+side*.018;vertices[f'{j}_{side}']=[radius*math.sin(p)*math.cos(u),radius*math.cos(p),radius*math.sin(p)*math.sin(u)]
   if j:faces.append(([f'{j-1}_-1',f'{j-1}_1',f'{j}_1',f'{j}_-1'],3 if strand%3==0 else 6))
  uid=add_mesh(m,'plasma_channel_'+str(strand),vertices,faces);g=m.group('plasma_'+str(strand),[uid]);a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1,(0,180 if strand%2 else -180,0)),(2,(0,360 if strand%2 else -360,0))])
 for i in range(12):
  vertices={};faces=[];q=i*math.tau/12
  for j in range(9):
   t=j/8;r=15+5*math.sin(t*math.pi/2);angle=q+t*.55
   for side in [-1,1]:vertices[f'{j}_{side}']=[r*math.cos(angle+side*.045*(1-t)),(t-.5)*5+5*math.sin(q*3),r*math.sin(angle+side*.045*(1-t))]
   if j:faces.append(([f'{j-1}_-1',f'{j-1}_1',f'{j}_1',f'{j}_-1'],5 if i%2 else 1))
  uid=add_mesh(m,'corona_flame_'+str(i),vertices,faces);g=m.group('corona_'+str(i),[uid]);a['animators'][g['uuid']]=animator(g,[(0,(1,1,1)),(.5,(1.08,1.2,1.08)),(1,(1,1,1)),(1.5,(1.1,.9,1.1)),(2,(1,1,1))],'scale')
 # Continuous equirectangular plasma replaces differently colored polygon tiles.
 texture=Image.new('RGBA',(256,256));pix=texture.load()
 for y in range(128):
  v=y/127*math.pi
  for x in range(256):
   u=x/256*math.tau;f=math.sin(u*5+1.4*math.sin(v*3))+.5*math.sin(u*11-v*6)+.25*math.sin(u*23+v*17);h=max(0,min(1,(f+1.75)/3.5))
   channel=abs(math.sin(u*4+1.6*math.sin(v*4)))
   if channel<.045:color=(215,45+int(30*h),2,255)
   else:color=(255,65+int(143*h),3+int(40*h*h),255)
   pix[x,y]=color
 for y in range(128,144):
  for x in range(256):pix[x,y]=FIRE[x//32]
 for element in m.data['elements']:
  for face in element['faces'].values():
   if element['name']=='molten_solar_surface':
    ids=[tuple(map(int,k.split('_')))for k in face['vertices']];seam=any(k==lon-1 for j,k in ids)and any(k==0 for j,k in ids)
    face['uv']={key:[(lon if seam and k==0 else k)/lon*256,j/lat*128]for key,(j,k)in zip(face['vertices'],ids)}
   else:face['uv']={key:[uv[0]*16,128+uv[1]]for key,uv in face['uv'].items()}
 buf=io.BytesIO();texture.save(buf,format='PNG');m.data['resolution']={'width':256,'height':256};m.data['textures'][0].update(width=256,height=256,uv_width=256,uv_height=256,source='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode())
 m.data['animations']=[a];glow(m)
if __name__=='__main__':
 raw=Path(sys.argv[1]).read_bytes();assert hashlib.sha256(raw).hexdigest()==RIG_SHA;generate(json.loads(raw))
