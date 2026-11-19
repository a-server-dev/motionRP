"""Original curved white/red Hollow mask, split diagonally for a forehead-to-jaw reveal."""
import sys
sys.dont_write_bytecode=True
import base64,io,math
from PIL import Image,ImageDraw
from generate_summons import Model,ident,ROOT,save,animation,animator
from hollow_kinematics import sampler,euler

def texture():
 w,h=128,160;im=Image.new('RGBA',(w,h));p=im.load()
 for y in range(h):
  v=y/(h-1);half=math.sqrt(max(0,1-((v-.45)/.57)**2))*(1-.16*v)
  for x in range(w):
   u=(x-(w-1)/2)/((w-1)/2)
   if abs(u)>half:continue
   edge=abs(u)/max(.01,half);gray=int(242-31*edge**2-7*v)
   p[x,y]=(gray,gray,min(255,gray+5),255)
 d=ImageDraw.Draw(im)
 def poly(points,color):d.polygon([(int(x*w),int(y*h)) for x,y in points],fill=color)
 red=(191,8,35,255)
 for points in [[(.49,.01),(.565,.01),(.55,.28),(.505,.58)],[(.63,.02),(.70,.055),(.60,.29),(.51,.47)],[(.76,.09),(.84,.15),(.64,.33),(.51,.46)],[(.90,.20),(.95,.28),(.68,.38),(.51,.48)]]:poly(points,red)
 poly([(.52,.39),(.55,.40),(.52,.66),(.495,.66)],red)
 for y,width in [(.51,.032),(.565,.026),(.62,.022)]:poly([(.54,y+.08),(.59,y+.025),(.91,y-.05),(.93,y-.05+width),(.63,y+.04+width)],red)
 poly([(.50,.87),(.57,.875),(.91,.77),(.85,.85),(.57,.93),(.56,1),(.49,.98)],red)
 # Open angular eye sockets: several mesh rows of white rather than a one-row slit.
 for points in [[(.04,.33),(.17,.365),(.435,.415),(.48,.55),(.25,.548),(.105,.46)],[(.075,.36),(.18,.395),(.40,.43),(.455,.52),(.27,.515),(.125,.445)]]:
  color=(15,14,23,255) if points[0][0]<.05 else (253,252,251,255)
  poly(points,color);poly([(1-x,y) for x,y in points],color)
 # Long interlocking teeth across the lower face, with an elongated central nose ridge.
 for i in range(12):
  x=.14+i*.061;top=.59+.025*abs(i-5.5);bottom=.86-.035*abs(i-5.5)
  pts=[(x,top),(x+.027,top-.035),(x+.055,bottom),(x+.025,bottom+.025)]
  coords=[(int(a*w),int(b*h)) for a,b in pts];d.polygon(coords,fill=(237,237,243,255));d.line(coords+[coords[0]],fill=(107,107,124,255),width=1)
 d.line([(int(.49*w),int(.48*h)),(int(.465*w),int(.73*h)),(int(.50*w),int(.77*h)),(int(.53*w),int(.73*h))],fill=(100,99,118,255),width=1)
 # Restore transparent silhouette after drawing stripes/teeth.
 alpha=im.getchannel('A');mask=Image.new('L',(w,h));mp=mask.load()
 for y in range(h):
  v=y/(h-1);half=math.sqrt(max(0,1-((v-.45)/.57)**2))*(1-.16*v)
  for x in range(w):mp[x,y]=255 if abs((x-(w-1)/2)/((w-1)/2))<=half else 0
 im.putalpha(mask);out=io.BytesIO();im.save(out,format='PNG');(ROOT/'textures').mkdir(exist_ok=True);(ROOT/'textures/motionrp_hollow_mask.png').write_bytes(out.getvalue());return out.getvalue()
def generate(actor,end=164):
 m=Model('motionrp_hollow_mask',False,[(238,238,244,255),(191,8,35,255),(15,14,23,255),(253,252,251,255),(215,215,224,255),(107,107,124,255),(230,230,237,255),(244,244,249,255)]);art=Image.open(io.BytesIO(texture()))
 cols,rows=32,40;elements=[]
 ranks=[(x+.5)/cols+(y+.5)/rows for y in range(rows) for x in range(cols) if art.getpixel((min(127,int((x+.5)*128/cols)),min(159,int((y+.5)*160/rows))))[3]];low,high=min(ranks),max(ranks)
 for band in range(8):
  verts={};faces={}
  for y in range(rows):
   for x in range(cols):
    # u=0 is the wearer's right. Reveal that upper forehead first, left jaw last.
    if min(7,max(0,int((((x+.5)/cols+(y+.5)/rows)-low)/(high-low)*8)))!=band:continue
    pixel=art.getpixel((min(127,int((x+.5)*128/cols)),min(159,int((y+.5)*160/rows))))
    if pixel[3]==0:continue
    r,g,b,_=pixel;color=1 if r>130 and g<50 else 2 if r<50 else 5 if r<150 else 4 if r<225 else 0
    ids=[]
    for xx,yy in [(x,y),(x,y+1),(x+1,y+1),(x+1,y)]:
     key=f'{xx}_{yy}';u,v=xx/cols,yy/rows;px=4.3-8.6*u;py=8.0-8.3*v;nose=.7*math.exp(-((u-.5)/.11)**2-((v-.53)/.25)**2);z=-4.15-.58*math.sin(math.pi*u)*math.sin(math.pi*v)-nose
     verts[key]=[px,py,z];ids.append(key)
    for tri in [[ids[0],ids[1],ids[2]],[ids[0],ids[2],ids[3]]]:
     # BetterModel mesh UVs use the fixed Minecraft 0..16 range, unlike cube UVs.
     uv={tri[0]:[(color+.15)*2,.15*16],tri[1]:[(color+.15)*2,.85*16],tri[2]:[(color+.85)*2,.85*16]}
     # Outward-facing triangles only: an inside/head camera sees no mask back faces.
     faces[str(len(faces))]={'vertices':tri,'uv':uv,'texture':0}
  uid=ident(m.name+'/band_'+str(band));m.data['elements'].append({'name':'mask_surface_'+str(band),'uuid':uid,'type':'mesh','origin':[0,0,0],'rotation':[0,0,0],'vertices':verts,'faces':faces,'light_emission':15});m.group('mask_band_'+str(band),[uid])
 children=m.data['outliner'][:];g=m.group('mask_socket',[]);m.data['outliner']=[{'uuid':g['uuid'],'children':children}]
 rotation=sampler(actor);keys=[];previous=None
 for tick in range(12,end+1):
  angles=euler(rotation('h_ph_head',tick/20))
  if previous:
   for i in range(3):
    while angles[i]-previous[i]>180:angles[i]-=360
    while angles[i]-previous[i]<-180:angles[i]+=360
  keys.append(((tick-12)/20,angles));previous=angles
 a=animation('wear',(end-12)/20,{});a['animators'][g['uuid']]=animator(g,keys);m.data['animations']=[a];save('models/motionrp_hollow_mask.bbmodel',m.data)
