#!/usr/bin/env python3
"""Seven-petal Rho Aias and black/red Bankai Getsuga; editable, embedded textures."""
import sys
sys.dont_write_bytecode=True
import copy,hashlib,json,math
from pathlib import Path
from generate_summons import Model,ROOT,RIG_SHA,player,save,animator,animation,player_animator
from generate_energy import mesh_model,add_mesh,glow
from generate_rasengan import hand_path
from hollow_kinematics import sampler,matrix,mul,vector,euler
PINK=[(188,34,119,255),(232,66,157,255),(255,139,210,255),(255,210,241,255),(138,22,94,255),(246,104,185,255),(255,173,228,255),(255,244,253,255)]
DARK=[(9,5,15,255),(28,8,40,255),(72,16,120,255),(142,42,235,255),(211,110,255,255),(95,22,160,255),(229,180,255,255),(255,238,255,255)]
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
            poses['pw_waist']=[(0,(0,0,0)),(.4,(0,0,0)),(1.4,(-10,-18,0)),(2.5,(-10,-18,0)),(2.8,(28,24,-5)),(3.2,(18,15,-3)),(4.1,(12,10,0)),(5.3,(12,10,0)),(6.6,(0,0,0))]
            poses['pc_chest']=[(0,(0,0,0)),(1.4,(-4,-10,0)),(2.5,(-4,-10,0)),(2.8,(14,12,-3)),(3.2,(8,8,-2)),(4.1,(5,5,0)),(5.3,(5,5,0)),(6.6,(0,0,0))]
            poses['h_ph_head']=[(0,(0,0,0)),(1.4,(-8,28,0)),(2.5,(-8,28,0)),(2.8,(10,-36,8)),(3.2,(5,-23,5)),(5.3,(0,-15,0)),(6.6,(0,0,0))]
            bends=[(0,0),(1.4,35),(2.5,35),(2.8,52),(3.2,42),(4.1,30),(5.3,30),(6.6,0)]
        m=player(rig,name,end/20,poses);g={g['name']:g for g in m['groups']};tracks=m['animations'][0]['animators'];drop=[]
        for side,leg,knee in [(1,'prl_right_leg','prfl_right_foreleg'),(-1,'pll_left_leg','plfl_left_foreleg')]:
            tracks[g[leg]['uuid']]=player_animator(g[leg],[(t,(-a,0,side*12*a/35)) for t,a in bends]);tracks[g[knee]['uuid']]=player_animator(g[knee],[(t,(a*2,0,0)) for t,a in bends])
        for tick in range(end+1):
            t=tick/20;lo=max((x for x in bends if x[0]<=t),default=bends[0]);hi=min((x for x in bends if x[0]>=t),default=bends[-1]);f=(t-lo[0])/(hi[0]-lo[0]) if hi[0]!=lo[0] else 0;a=lo[1]*(1-f)+hi[1]*f
            drop.append((t,(0,-11.25*(1-math.cos(math.radians(a))*math.cos(math.radians(12*a/35))),0)))
        tracks[g['player_root']['uuid']]=animator(g['player_root'],drop,'position')
        if not shield:
            prefix=[(12,(-118.83057,34.42578,-77.72337),-25.90309),(14,(-106.65096,29.72642,-76.52480),-48.74212),(16,(-95.57199,28.12860,-80.06693),-63.55864),(18,(-85.15057,26.88720,-85.38078),-75.10598),(20,(-75.01839,25.46008,-92.38223),-84.56594),(22,(-65.16199,23.46188,-101.26309),-92.39072),(24,(-55.98555,20.49292,-112.11064),-98.75500),(26,(-48.41147,16.26454,-124.57339),-103.69703)]
            for bone,track in list(tracks.items()):
                for key in track['keyframes']:key['time']=round(key['time']+2.2,8)
                if track['name'] in ['pla_left_arm','plfa_left_forearm']:
                    upper=track['name']=='pla_left_arm';values=[(0,(0,0,0))]+[(t/20,q if upper else (f,0,0)) for t,q,f in prefix]
                    for key in track['keyframes']:
                        if key['time']==2.2:
                            q=prefix[-1][1] if upper else (prefix[-1][2],0,0);key['data_points'][0].update(x=str(-q[0]),y=str(q[1]),z=str(q[2]))
                    track['keyframes']=player_animator(g[track['name']],values)['keyframes']+track['keyframes']
                else:
                    channel=track['keyframes'][0]['channel'];neutral=animator(g[track['name']],[(0,(0,0,0))],channel);track['keyframes']=neutral['keyframes']+track['keyframes']
            end+=44;m['animations'][0]['length']=end/20
        save('players/motionrp_'+name+'.bbmodel',m)
        (ROOT/(name+'-hand-path.csv')).write_text(hand_path(m,end,'pli_left_item' if shield else 'pri_right_item'))
        if not shield:
            right=[[float(x) for x in line.split(',')] for line in hand_path(m,end).splitlines()]
            left=hand_path(m,end,'pli_left_item');(ROOT/'getsuga_tensho-left-hand-path.csv').write_text(left)
            rotation=sampler(m);head=hand_path(m,end,'h_ph_head');head_rows=[]
            for line in head.splitlines():
                xyz=[float(x) for x in line.split(',')];xyz[1]-=.05;xyz[2]-=.03;head_rows.append(','.join(f'{v:.8f}' for v in xyz))
            (ROOT/'getsuga_tensho-head-path.csv').write_text('\n'.join(head_rows)+'\n')
            rows=[];weapon_keys=[];previous=None
            for tick,palm in enumerate(right):
                global_tick=tick;tick=max(0,tick-44)
                if tick<28:f=max(0,min(1,(tick-8)/20));pitch=-20+55*f;roll=-25*f
                elif tick<=50:pitch=35;roll=-25
                elif tick<=56:f=(tick-50)/6;pitch=35-160*f;roll=-25-20*f
                elif tick<=106:pitch=-125;roll=-45
                else:f=(tick-106)/26;pitch=-125+105*f;roll=-45+45*f
                total=mul(rotation('pc_chest',global_tick/20),matrix(pitch,0,roll));axis_source=vector(total,[0,1,0]);axis=[axis_source[0],-axis_source[2],axis_source[1]]
                grip=[v-a*1.75/16 for v,a in zip(palm,axis)];tip=[v+a*(35.7*.65/16) for v,a in zip(grip,axis)]
                rows.append(','.join(f'{v:.8f}' for v in grip+tip))
                if global_tick>=52:
                    angles=euler(total)
                    if previous:
                        for i in range(3):
                            while angles[i]-previous[i]>180:angles[i]-=360
                            while angles[i]-previous[i]<-180:angles[i]+=360
                    previous=angles;weapon_keys.append(((global_tick-52)/20,angles))
            (ROOT/'getsuga_tensho-blade-path.csv').write_text('\n'.join(rows)+'\n')
            blade=json.loads((ROOT/'models/motionrp_getsuga_tensa.bbmodel').read_text());groups={x['name']:x for x in blade['groups']};clip=blade['animations'][0];clip['animators'][groups['sword_socket']['uuid']]=animator(groups['sword_socket'],weapon_keys);save('models/motionrp_getsuga_tensa.bbmodel',blade)
            from generate_hollow_mask import generate
            generate(m,end)


def reference_getsuga():
    """Mask -> one-handed clearing swing -> two-handed overhead charge -> release."""
    model=json.loads((ROOT/'players/motionrp_getsuga_tensho.bbmodel').read_text())
    clip=model['animations'][0];assert clip['length']==8.8,'Regenerate the base choreography before refining it'
    groups={g['name']:g for g in model['groups']};tracks=clip['animators'];end=200
    # Insert 24 ticks before the existing overhead draw. Keep the solved two-hand
    # grip and the planted-foot finishing stroke, retimed together as one unit.
    for track in tracks.values():
        for key in track['keyframes']:
            if key['time']>=2.2:key['time']=round(key['time']+1.2,8)
    clip['length']=end/20
    def extend(name,keys):
        track=tracks[groups[name]['uuid']]
        replacements=player_animator(groups[name],[(t/20,q)for t,q in keys])['keyframes']
        times={k['time']for k in replacements}
        track['keyframes']=[k for k in track['keyframes'] if k['time']not in times]+replacements
        track['keyframes'].sort(key=lambda k:k['time'])
    extend('pla_left_arm',[(36,(0,0,-8)),(44,(0,0,-8)),(52,(8,0,-15)),(60,(4,0,-12)),(68,(0,0,0))])
    extend('plfa_left_forearm',[(36,(0,0,0)),(44,(0,0,0)),(52,(-12,0,0)),(60,(-8,0,0)),(68,(0,0,0))])
    extend('pra_right_arm',[(36,(0,0,0)),(44,(-72,-30,32)),(48,(-100,-35,46)),(52,(-68,12,-22)),(56,(-28,25,-38)),(62,(-12,10,-12)),(68,(0,0,0))])
    extend('prfa_right_forearm',[(36,(0,0,0)),(44,(-55,0,0)),(48,(-40,0,0)),(52,(-18,0,0)),(56,(-8,0,0)),(62,(-5,0,0)),(68,(0,0,0))])
    extend('pw_waist',[(36,(0,0,0)),(44,(0,-8,0)),(48,(0,-10,0)),(52,(0,4,0)),(56,(0,12,0)),(62,(0,5,0)),(68,(0,0,0))])
    extend('pc_chest',[(36,(0,0,0)),(44,(0,-15,0)),(48,(0,-18,0)),(52,(0,8,0)),(56,(0,20,0)),(62,(0,10,0)),(68,(0,0,0))])
    extend('h_ph_head',[(36,(0,0,0)),(44,(0,18,0)),(48,(0,20,0)),(52,(0,-8,0)),(56,(0,-22,0)),(62,(0,-10,0)),(68,(0,0,0))])
    save('players/motionrp_getsuga_tensho.bbmodel',model)
    right=[[float(v)for v in row.split(',')]for row in hand_path(model,end).splitlines()]
    (ROOT/'getsuga_tensho-hand-path.csv').write_text(hand_path(model,end))
    (ROOT/'getsuga_tensho-left-hand-path.csv').write_text(hand_path(model,end,'pli_left_item'))
    head=[]
    for row in hand_path(model,end,'h_ph_head').splitlines():
        p=[float(v)for v in row.split(',')];p[1]-=.05;p[2]-=.03;head.append(','.join(f'{v:.8f}'for v in p))
    (ROOT/'getsuga_tensho-head-path.csv').write_text('\n'.join(head)+'\n')
    def sample(keys,t):
        lo=max((k for k in keys if k[0]<=t),default=keys[0]);hi=min((k for k in keys if k[0]>=t),default=keys[-1]);f=(t-lo[0])/(hi[0]-lo[0])if hi[0]!=lo[0]else 0
        return tuple(a*(1-f)+b*f for a,b in zip(lo[1],hi[1]))
    flourish=[(0,(-100,0,0)),(36,(-100,0,0)),(44,(-10,0,-75)),(48,(20,0,-12)),(52,(-70,0,35)),(56,(-135,0,-35)),(62,(-105,0,10)),(68,(-68,0,0)),(76,(-20,0,0))]
    rotation=sampler(model);rows=[];weapon_keys=[];previous=None
    for tick,palm in enumerate(right):
        old=max(0,tick-68)
        if tick<76:pitch,yaw,roll=sample(flourish,tick)
        elif old<28:f=max(0,min(1,(old-8)/20));pitch=-20+55*f;yaw=0;roll=-25*f
        elif old<=50:pitch,yaw,roll=35,0,-25
        elif old<=56:f=(old-50)/6;pitch=35-160*f;yaw=0;roll=-25-20*f
        elif old<=106:pitch,yaw,roll=-125,0,-45
        else:f=(old-106)/26;pitch=-125+105*f;yaw=0;roll=-45+45*f
        total=mul(rotation('pc_chest',tick/20),matrix(pitch,yaw,roll));axis_source=vector(total,[0,1,0]);axis=[axis_source[0],-axis_source[2],axis_source[1]]
        grip=[v-a*(1.75/16*max(0,min(1,(tick-68)/8)))for v,a in zip(palm,axis)]
        tip=[v+a*(35.7*.65/16)for v,a in zip(grip,axis)];rows.append(','.join(f'{v:.8f}'for v in grip+tip))
        angles=euler(total)
        if previous:
            for i in range(3):
                while angles[i]-previous[i]>180:angles[i]-=360
                while angles[i]-previous[i]<-180:angles[i]+=360
        previous=angles;weapon_keys.append((tick/20,angles))
    (ROOT/'getsuga_tensho-blade-path.csv').write_text('\n'.join(rows)+'\n')
    weapon=json.loads((ROOT/'models/motionrp_getsuga_tensa.bbmodel').read_text());socket=next(g for g in weapon['groups']if g['name']=='sword_socket');edge=next(g for g in weapon['groups']if g['name']=='edge_down_roll');weapon['animations']=[animation('cut',end/20,{socket['uuid']:animator(socket,weapon_keys),edge['uuid']:animator(edge,[(0,(0,-90,0)),(end/20,(0,-90,0))])})]
    save('models/motionrp_getsuga_tensa.bbmodel',weapon)
    from generate_hollow_mask import generate
    generate(model,end)


if __name__=='__main__':
    raw=Path(sys.argv[1]).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=RIG_SHA:raise ValueError('Not the pinned rig')
    shield();crescent();weapon();actors(json.loads(raw));reference_getsuga()
