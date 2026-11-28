"""A grounded full-speed stride before the Titan's shoulder check."""
import sys,copy,json,math
sys.dont_write_bytecode=True
from generate_summons import ROOT,animation,animator,ident,save
from generate_rasengan import hand_path

def generate():
 m=json.loads((ROOT/'players/motionrp_titan_transform.bbmodel').read_text());groups={g['name']:g for g in m['groups']};a=animation('titan_sprint',1,{});a['override']=True
 for name in ['player_root','pw_waist','pc_chest','h_ph_head','prl_right_leg','pll_left_leg','prfl_right_foreleg','plfl_left_foreleg','pra_right_arm','pla_left_arm','prfa_right_forearm','plfa_left_forearm']:
  keys=[]
  for tick in range(21):
   t=tick/20.;wave=math.cos(t*math.tau/.4);side=-1 if name.startswith('pl')else 1
   if name=='player_root':v=(-8,0,0)
   elif name=='pw_waist':v=(-18,0,0)
   elif name=='pc_chest':v=(-8,4*wave,0)
   elif name=='h_ph_head':v=(25,0,0)
   elif name in ['prl_right_leg','pll_left_leg']:v=(side*55*wave,0,side*8)
   elif name in ['prfl_right_foreleg','plfl_left_foreleg']:v=(-40-50*max(0,-side*wave),0,0)
   elif name in ['pra_right_arm','pla_left_arm']:v=(-side*50*wave,0,side*12)
   else:v=(80+side*20*wave,0,0)
   keys.append((t,v))
  a['animators'][groups[name]['uuid']]=animator(groups[name],keys)
 probe=copy.deepcopy(m);probe['animations']=[a];samples=[]
 def attach(nodes,uid,marker):
  for n in nodes:
   if isinstance(n,dict):
    if n['uuid']==uid:n['children'].append(marker);return True
    if attach(n['children'],uid,marker):return True
  return False
 for side,name in [(1,'prfl_right_foreleg'),(-1,'plfl_left_foreleg')]:
  for x in ([0,3.75]if side==1 else[-3.75,0]):
   for y in [0,5.625]:
    for z in [-1.875,1.875]:
     label=f'titan_sole_{side}_{x}_{y}_{z}';g={'name':label,'uuid':ident(label),'origin':[x,y,z],'rotation':[0,0,0]};probe['groups'].append(g);attach(probe['outliner'],groups[name]['uuid'],{'uuid':g['uuid'],'children':[]})
     samples.append([[float(v)for v in row.split(',')]for row in hand_path(probe,21,label).splitlines()])
 keys=[(tick/20.,(0,-min((rows[tick][2]-.03)*16 for rows in samples),0))for tick in range(21)]
 root=a['animators'][groups['player_root']['uuid']];root['keyframes'].extend(animator(groups['player_root'],keys,'position')['keyframes']);root['keyframes'].sort(key=lambda k:k['time'])
 m['animations']=[v for v in m['animations']if v['name']!='titan_sprint']+[a];save('players/motionrp_titan_transform.bbmodel',m)
 body=json.loads((ROOT/'models/motionrp_titan_body.bbmodel').read_text());b=copy.deepcopy(a);bg={g['uuid']:g for g in body['groups']}
 for uid,t in b['animators'].items():t['name']=bg[uid]['name']
 body['animations']=[v for v in body['animations']if v['name']!='titan_sprint']+[b];save('models/motionrp_titan_body.bbmodel',body)
if __name__=='__main__':generate()
