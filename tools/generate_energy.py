"""Emissive rounded energy meshes and moving surface filaments, using vanilla item models."""
import math
from generate_summons import Model,animator,animation
GOLD_LIGHT=[(255,225,108,255),(255,248,191,255),(255,235,133,255),(255,254,229,255),(255,244,161,255),(255,250,205,255),(255,238,145,255),(255,255,255,255)]
BLUE_LIGHT=[(92,184,255,255),(166,231,255,255),(116,205,255,255),(228,252,255,255),(140,219,255,255),(190,242,255,255),(111,213,255,255),(255,255,255,255)]
def glow(m):
    for e in m.data['elements']:e['light_emission']=15
    for g in m.data['groups']:g['light_emission']=15
    m.save()
def beam(name,colors):
    m=Model(name,colors=colors);ids=[]
    for row in range(-4,5):
        y=row*.8;w=2*math.sqrt(max(.1,3.7**2-y*y))
        ids.append(m.cube('round_radiant_core_'+str(row),[0,y,0],[w,.85,20],7 if abs(row)<3 else 3))
    m.group('radiant_core',ids)
    flow=animation('flow',.5,{});flow['loop']='loop'
    for strand in range(3):
        ids=[]
        for j in range(16):
            z=-9.6+j*1.28;angle=j*.65+strand*math.tau/3
            ids.append(m.cube(f'light_filament_{strand}_{j}',[4.4*math.cos(angle),4.4*math.sin(angle),z],[.55,.55,1.45],1 if j%3 else 7))
        g=m.group('stream_'+str(strand),ids)
        flow['animators'][g['uuid']]=animator(g,[(t/8,(0,0,t*(90 if strand%2 else -90))) for t in range(5)])
    m.data['animations']=[flow];glow(m)
def mesh_model(name,colors):
    import base64,struct,zlib
    m=Model(name,colors=colors)
    raw=b''.join(b'\0'+b''.join(bytes(colors[x//2]) for x in range(16)) for _ in range(16))
    def chunk(t,b):return struct.pack('!I',len(b))+t+b+struct.pack('!I',zlib.crc32(t+b)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')
    m.data['resolution']={'width':16,'height':16}
    m.data['textures'][0].update(width=16,height=16,uv_width=16,uv_height=16,source='data:image/png;base64,'+base64.b64encode(png).decode())
    return m

def add_mesh(m,name,vertices,faces):
    from generate_summons import ident
    uid=ident(m.name+'/'+name)
    data={}
    for ids,color in faces:
        triangles=[ids] if len(ids)==3 else [[ids[0],ids[1],ids[2]],[ids[0],ids[2],ids[3]]]
        for triangle in triangles:
            uv=[[color*2+.2,8.2],[color*2+.8,8.2],[color*2+.8,8.8]]
            data[str(len(data))]={'vertices':triangle,'uv':dict(zip(triangle,uv)),'texture':0}
    m.data['elements'].append({'name':name,'uuid':uid,'type':'mesh','origin':[0,0,0],'rotation':[0,0,0],'vertices':vertices,'faces':data})
    return uid

def ribbon(m,name,points,width,color):
    vertices={};faces=[]
    for j,(x,y,z) in enumerate(points):
        length=max(.001,math.hypot(x,z));dx,dz=x/length*width/2,z/length*width/2
        vertices[f'a{j}']=[x-dx,y,z-dz];vertices[f'b{j}']=[x+dx,y,z+dz]
        if j:faces.append(([f'a{j-1}',f'b{j-1}',f'b{j}',f'a{j}'],color))
    return add_mesh(m,name,vertices,faces)

def pillar():
    m=mesh_model('motionrp_excalibur_pillar',GOLD_LIGHT);a=animation('surge',.7,{});a['loop']='loop'
    for strand in range(6):
        points=[]
        for j in range(49):
            angle=j*.25+strand*math.tau/6;r=1.3+.8*math.sin(j*.12+strand)**2
            points.append((r*math.cos(angle),j*.5,r*math.sin(angle)))
        uid=ribbon(m,'light_ribbon_'+str(strand),points,.12,7 if strand%2 else 1)
        g=m.group('rising_light_'+str(strand),[uid],origin=(0,12,0))
        a['animators'][g['uuid']]=animator(g,[(t*.7/4,(0,t*90,0)) for t in range(5)])
    m.data['animations']=[a];glow(m)

def sphere(name):
    # No voxel slabs: an actual curved surface with moving plasma channels.
    m=mesh_model(name,BLUE_LIGHT);vertices={};faces=[];lat,lon=24,48
    for j in range(lat+1):
        phi=math.pi*j/lat
        for i in range(lon):
            theta=math.tau*i/lon
            vertices[f'v{j}_{i}']=[16*math.sin(phi)*math.cos(theta),16*math.cos(phi),16*math.sin(phi)*math.sin(theta)]
    for j in range(lat):
        phi=math.pi*(j+.5)/lat
        for i in range(lon):
            theta=math.tau*(i+.5)/lon
            flow=abs(math.sin(theta*5+2*math.sin(phi*4)+.7*math.sin(theta*3-phi*7)))
            color=7 if flow<.18 else 3 if flow<.32 else 1 if flow<.6 else 4
            ids=[f'v{j}_{i}',f'v{j}_{(i+1)%lon}',f'v{j+1}_{(i+1)%lon}',f'v{j+1}_{i}']
            if j==0:ids=[ids[0],ids[2],ids[3]]
            elif j==lat-1:ids=[ids[0],ids[1],ids[2]]
            faces.append((ids,color))
    uid=add_mesh(m,'plasma_sphere',vertices,faces);g=m.group('energy_sphere',[uid])
    a=animation('corona',4,{});a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(t,(0,t*90,0)) for t in range(5)])
    for strand in range(3):
        v={};f=[]
        for j in range(97):
            theta=j*math.tau/96
            phi=math.pi/2+.48*math.sin(theta*3+strand*2)
            for edge,delta in [('a',-.012),('b',.012)]:
                p=phi+delta;r=16.25
                v[f'{edge}{j}']=[r*math.sin(p)*math.cos(theta),r*math.cos(p),r*math.sin(p)*math.sin(theta)]
            if j:f.append(([f'a{j-1}',f'b{j-1}',f'b{j}',f'a{j}'],7))
        uid=add_mesh(m,'plasma_current_'+str(strand),v,f);g=m.group('corona_'+str(strand),[uid])
        a['animators'][g['uuid']]=animator(g,[(t,(t*90 if strand==1 else 0,t*(-90 if strand%2 else 90),0)) for t in range(5)])
    m.data['animations']=[a];glow(m)

def excalibur_reference():
    # Tall blade split into legal child meshes, pivoting at the same palm as the actual sword.
    m=Model('motionrp_excalibur_charge',colors=GOLD_LIGHT);children=[]
    for half in range(2):
        ids=[]
        for j in range(10):
            y=half*20+j*2+1;w=1.3+3.8*math.sin(y/40*math.pi*.8)
            ids.append(m.cube(f'light_blade_core_{half}_{j}',[0,y,0],[w,2.2,.8],7))
            for side in [-1,1]:
                ids.append(m.cube(f'gold_blade_flare_{half}_{j}_{side}',[side*(w*.55+.6*math.sin(j)),y,.05],[.55,2.4,.6],1))
        m.group('blade_half_'+str(half),ids,origin=(0,half*20+10,0));children.append(m.data['outliner'].pop())
    g=m.group('light_blade',[]);m.data['outliner'][-1]['children']=children
    a=animation('cleave',1.6,{});a['animators'][g['uuid']]=animator(g,[(0,(0,0,0)),(1.3,(0,0,0)),(1.425,(-95,0,0)),(1.6,(-95,0,0))]);m.data['animations']=[a];glow(m)
    # A wide, flame-edged wavefront which runs along the ground ahead of the narrow white core.
    m=Model('motionrp_excalibur_wave',colors=GOLD_LIGHT);ids=[]
    for j in range(25):
        x=-12+j;y=.5+3*(1-(x/12)**2);z=2.8*(x/12)**2
        ids.append(m.cube('golden_wave_front_'+str(j),[x,y,z],[1.2,1.2,1],3))
        for k in range(3):
            ids.append(m.cube(f'wave_flame_{j}_{k}',[x,.8+y+k*(.6+.8*(1-abs(x)/13)),z-k*.4],[.6,1.2,.7],7 if k==0 else 1))
    g=m.group('surging_wave',ids);a=animation('rush',.3,{});a['loop']='loop';a['animators'][g['uuid']]=animator(g,[(0,(1,1,1)),(.15,(1.08,1.3,1)),(.3,(1,1,1))],'scale');m.data['animations']=[a];glow(m)
