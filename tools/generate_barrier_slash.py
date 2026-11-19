#!/usr/bin/env python3
"""Seven-petal Rho Aias and black/red Bankai Getsuga; editable, embedded textures."""
import sys
sys.dont_write_bytecode=True
import copy,hashlib,json,math
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player,save,animator,animation,player_animator
from generate_energy import mesh_model,add_mesh,glow
from generate_rasengan import hand_path
PINK=[(188,34,119,255),(232,66,157,255),(255,139,210,255),(255,210,241,255),(138,22,94,255),(246,104,185,255),(255,173,228,255),(255,244,253,255)]
DARK=[(9,5,15,255),(35,8,28,255),(110,9,37,255),(235,22,65,255),(255,77,101,255),(160,16,47,255),(255,163,175,255),(255,230,231,255)]
def shield():
    m=mesh_model('motionrp_rho_aias_shield',PINK);a=animation('bloom',1,{})
    def dome(rad,tan):return -8.5*math.sqrt(max(0,1-min(1,(rad*rad+tan*tan)/(18*18))))
    for petal in range(7):
        angle=petal*math.tau/7;v={};faces=[]
        def point(rad,tan,z=0):return [rad*math.cos(angle)-tan*math.sin(angle),rad*math.sin(angle)+tan*math.cos(angle),dome(rad,tan)+z]
        v['center']=point(8,0)
        for ring,size in enumerate([.33,.66,.92,1]):
            for j in range(33):
                t=j*math.tau/32;key=f'r{ring}_{j}'
                v[key]=point(8+10*size*math.cos(t),5.5*size*math.sin(t))
                if j:
                    color=(3 if j%4 else 7) if ring==3 else (1 if petal%2 else 0)
                    faces.append((['center',f'r0_{j-1}',key] if ring==0 else [f'r{ring-1}_{j-1}',f'r{ring}_{j-1}',key,f'r{ring-1}_{j}'],color))
        faces+=[(list(reversed(ids)),color) for ids,color in faces[:]]
        uid=add_mesh(m,'petal_surface_'+str(petal),v,faces);g=m.group('petal_'+str(petal),[uid])
        delay=petal*.07
        a['animators'][g['uuid']]=animator(g,[(0,(.025,.025,.025)),(delay,(.025,.025,.025)),(delay+.4,(1,1,1)),(1,(1,1,1))],'scale')
    ids=[]
    for i in range(28):
        a0=i*math.tau/28;ids.append(m.cube('center_sigil_'+str(i),[2.7*math.cos(a0),2.7*math.sin(a0),dome(2.7,0)-.2],[.55,.55,.12],7))
    m.group('sevenfold_sigil',ids);m.data['animations']=[a];glow(m)
def crescent():
    m=mesh_model('motionrp_getsuga_crescent',DARK)
    for band,(radius,thick,color,width) in enumerate([(24,7,0,2.0),(25,1.7,3,2.3),(18,1,5,1.8)]):
        v={};f=[]
        for i in range(49):
            a=math.radians(-84+168*i/48);taper=max(.08,math.cos(a)**2);inner=radius-thick*taper
            for side,x in [('l',-width),('r',width)]:
                v[f'{side}o{i}']=[x,math.sin(a)*radius,-math.cos(a)*radius]
                v[f'{side}i{i}']=[x,math.sin(a)*inner,-math.cos(a)*inner]
            if i:
                for side in ['l','r']:f.append(([f'{side}o{i-1}',f'{side}o{i}',f'{side}i{i}',f'{side}i{i-1}'],color))
                f.append(([f'lo{i-1}',f'ro{i-1}',f'ro{i}',f'lo{i}'],4 if band==1 else color))
                f.append(([f'li{i-1}',f'li{i}',f'ri{i}',f'ri{i-1}'],color))
        for xyz in v.values():
            x,y=xyz[:2];tilt=math.radians(-35);xyz[0]=x*math.cos(tilt)-y*math.sin(tilt);xyz[1]=x*math.sin(tilt)+y*math.cos(tilt)
        uid=add_mesh(m,'crescent_band_'+str(band),v,f);m.group('crescent_'+str(band),[uid])
    glow(m)
def weapon():
    m=json.loads((ROOT/'models/motionrp_tensa_zangetsu.bbmodel').read_text());m['name']=m['model_identifier']='motionrp_getsuga_tensa'
    # Attach at the middle of the wrapped grip, not at the guard.
    for e in m['elements']:
        for key in ['from','to','origin']:
            if key in e:e[key][1]+=6.5
    groups={g['name']:g for g in m['groups']};groups['tensa_blade']['origin'][1]+=6.5
    for e in m['elements']:
        if e['name']=='black_wrapped_hilt':e['from'][1]=-6;e['to'][1]=6;e['origin'][1]=0
        if e['name'].startswith('white_hilt_diamond_'):
            center=4.8-int(e['name'].split('_')[-1])*2.3;dy=center-e['origin'][1]
            for key in ['from','to','origin']:e[key][1]+=dy
        if e['name'].startswith('chain_'):
            for key in ['from','to','origin']:e[key][1]-=3.5
    a=animation('cut',6.2,{})
    a['animators'][groups['sword_socket']['uuid']]=animator(groups['sword_socket'],[(0,(-20,0,0)),(.4,(-20,0,0)),(1.4,(35,0,-25)),(2.5,(35,0,-25)),(2.8,(-125,0,-45)),(5.3,(-125,0,-45)),(6.6,(-20,0,0))])
    a['animators'][groups['edge_down_roll']['uuid']]=animator(groups['edge_down_roll'],[(0,(0,-90,0)),(6.6,(0,-90,0))]);
    for track in a['animators'].values():
        for key in track['keyframes']:key['time']=max(0,key['time']-.4)
    m['animations']=[a];save('models/motionrp_getsuga_tensa.bbmodel',m)
def actors(rig):
    for shield in [True,False]:
        name='rho_aias' if shield else 'getsuga_tensho';end=180 if shield else 132
        if shield:
            poses={
             'pla_left_arm':[(0,(0,0,0)),(.8,(-85,0,-8)),(7,(-85,0,-8)),(9,(0,0,0))],
             'plfa_left_forearm':[(0,(0,0,0)),(.8,(-10,0,0)),(7,(-10,0,0)),(9,(0,0,0))],
             'pra_right_arm':[(0,(0,0,0)),(.8,(10,0,20)),(7,(10,0,20)),(9,(0,0,0))],
             'prfa_right_forearm':[(0,(0,0,0)),(.8,(-45,0,0)),(7,(-45,0,0)),(9,(0,0,0))]}
            bends=[(0,0),(.8,28),(7,28),(9,0)]
        else:
            right_charge=(-53.37122297,-55.75108226,129.04496159);right_cut=(-116.67637224,14.42228763,106.88808567)
            left_keys=[(1.4,(-103.73323,31.67401,-84.64766),-39.15040),(2.5,(-103.73331,31.67394,-84.64752),-39.15040),(2.55,(-81.37310,25.35202,-99.47662),-64.96639),(2.6,(-69.90254,19.58455,-114.61742),-80.98800),(2.65,(-67.72465,15.35524,-124.84960),-92.42298),(2.7,(-71.15935,13.39516,-130.25283),-100.40211),(2.75,(-76.31236,13.04296,-132.45855),-105.91279),(2.8,(-79.56641,13.25449,-133.00343),-110.83714)]
            poses={
             'pra_right_arm':[(0,(0,0,0)),(.4,(-30,0,15)),(1.4,right_charge),(2.5,right_charge),(2.8,right_cut),(5.3,right_cut),(6.6,(0,0,0))],
             'prfa_right_forearm':[(0,(0,0,0)),(.4,(-30,0,0)),(1.4,(-44.92547122,0,0)),(2.5,(-44.92547122,0,0)),(2.8,(-40.82488903,0,0)),(5.3,(-40.82488903,0,0)),(6.6,(0,0,0))],
             'pla_left_arm':[(0,(0,0,0))]+[(t,q) for t,q,f in left_keys]+[(5.3,left_keys[-1][1]),(6.6,(0,0,0))],
             'plfa_left_forearm':[(0,(0,0,0))]+[(t,(f,0,0)) for t,q,f in left_keys]+[(5.3,(left_keys[-1][2],0,0)),(6.6,(0,0,0))]}
            bends=[(0,0),(1.4,35),(2.5,35),(2.8,25),(5.3,25),(6.6,0)]
        m=player(rig,name,end/20,poses);g={g['name']:g for g in m['groups']};tracks=m['animations'][0]['animators'];drop=[]
        for side,leg,knee in [(1,'prl_right_leg','prfl_right_foreleg'),(-1,'pll_left_leg','plfl_left_foreleg')]:
            tracks[g[leg]['uuid']]=player_animator(g[leg],[(t,(-a,0,side*12*a/35)) for t,a in bends]);tracks[g[knee]['uuid']]=player_animator(g[knee],[(t,(a*2,0,0)) for t,a in bends])
        for tick in range(end+1):
            t=tick/20;lo=max((x for x in bends if x[0]<=t),default=bends[0]);hi=min((x for x in bends if x[0]>=t),default=bends[-1]);f=(t-lo[0])/(hi[0]-lo[0]) if hi[0]!=lo[0] else 0;a=lo[1]*(1-f)+hi[1]*f
            drop.append((t,(0,-11.25*(1-math.cos(math.radians(a))*math.cos(math.radians(12*a/35))),0)))
        tracks[g['player_root']['uuid']]=animator(g['player_root'],drop,'position');save('players/motionrp_'+name+'.bbmodel',m)
        (ROOT/(name+'-hand-path.csv')).write_text(hand_path(m,end,'pli_left_item' if shield else 'pri_right_item'))
        if not shield:
            right=[[float(x) for x in line.split(',')] for line in hand_path(m,end).splitlines()]
            left=hand_path(m,end,'pli_left_item');(ROOT/'getsuga_tensho-left-hand-path.csv').write_text(left)
            rows=[]
            for tick,palm in enumerate(right):
                if tick<28:f=max(0,min(1,(tick-8)/20));pitch=-20+55*f;roll=-25*f
                elif tick<=50:pitch=35;roll=-25
                elif tick<=56:f=(tick-50)/6;pitch=35-160*f;roll=-25-20*f
                elif tick<=106:pitch=-125;roll=-45
                else:f=(tick-106)/26;pitch=-125+105*f;roll=-45+45*f
                x,z=math.radians(pitch),math.radians(roll)
                # Rz*Rx acting on the sword's up axis, converted into world-relative coordinates.
                axis=[-math.sin(z)*math.cos(x),-math.sin(x),math.cos(z)*math.cos(x)]
                grip=[v-a*1.75/16 for v,a in zip(palm,axis)]
                tip=[v+a*(35.7*.65/16) for v,a in zip(grip,axis)]
                rows.append(','.join(f'{v:.8f}' for v in grip+tip))
            (ROOT/'getsuga_tensho-blade-path.csv').write_text('\n'.join(rows)+'\n')

if __name__=='__main__':
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned rig')
    shield();crescent();weapon();actors(json.loads(raw))
