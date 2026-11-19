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
def pillar():
    m=Model('motionrp_excalibur_pillar',colors=GOLD_LIGHT);ids=[]
    for row in range(-3,4):
        z=row*.7;w=2*math.sqrt(max(.1,2.5**2-z*z))
        ids.append(m.cube('round_light_column_'+str(row),[0,12,z],[w,24,.75],7 if abs(row)<2 else 3))
    m.group('column_core',ids,origin=(0,12,0));a=animation('surge',.5,{});a['loop']='loop'
    for i in range(3):
        ids=[]
        for j in range(24):
            angle=j*.65+i*math.tau/3
            ids.append(m.cube(f'column_spark_{i}_{j}',[3*math.cos(angle),j+.5,3*math.sin(angle)],[.3,.8,.3],1))
        g=m.group('rising_sparks_'+str(i),ids,origin=(0,12,0));a['animators'][g['uuid']]=animator(g,[(t/8,(0,t*90,0)) for t in range(5)])
    m.data['animations']=[a];glow(m)
def sphere(name):
    m=Model(name,colors=BLUE_LIGHT);ids=[]
    for y in range(-15,16,2):
        for z in range(-15,16,2):
            d=256-y*y-z*z
            if d>0:
                # Bright core and coloured rim, with the brightest cap following the round surface.
                colour=7 if y*y+z*z<100 else 3 if y*y+z*z<190 else 1
                uid=m.cube(f'sphere_{y}_{z}',[0,y,z],[2*math.sqrt(d),2,2],colour);ids.append(uid)
                # Keep a coherent rounded highlight across all six directions, not flat stripes.
                e=m.data['elements'][-1]
                for face,normal in [('west',(-1,0,0)),('east',(1,0,0)),('up',(0,1,0)),('down',(0,-1,0)),('north',(0,0,-1)),('south',(0,0,1))]:
                    p=[normal[0]*math.sqrt(d),y+normal[1],z+normal[2]];length=max(.001,math.sqrt(sum(v*v for v in p)))
                    light=sum(a*b for a,b in zip(p,[-.35,.55,-.76]))/length
                    palette=[0,2,4,1,5,3,7];index=palette[min(6,max(0,int((light+1)*3.5)))]
                    e['faces'][face]['uv']=[index+.05,.05,index+.95,.95]
    ids.append(m.cube('equatorial_diameter',[0,0,0],[32,1,1],7));m.group('energy_sphere',ids)
    a=animation('corona',1.5,{});a['loop']='loop'
    for axis in range(3):
        ids=[]
        for j in range(48):
            angle=j*math.tau/48;r=16.3
            p=[r*math.cos(angle),r*math.sin(angle),.7*math.sin(angle*3)]
            if axis==1:p=[p[2],p[0],p[1]]
            elif axis==2:p=[p[1],p[2],p[0]]
            ids.append(m.cube(f'surface_arc_{axis}_{j}',p,[.35,.35,.35],7 if j%4 else 6))
        g=m.group('corona_'+str(axis),ids)
        rot=[0,0,0];rot[axis]=360 if axis%2 else -360
        a['animators'][g['uuid']]=animator(g,[(t*1.5/4,tuple(v*t/4 for v in rot)) for t in range(5)])
    m.data['animations']=[a];glow(m)
