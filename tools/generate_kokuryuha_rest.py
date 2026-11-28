"""Remove the hands-on-waist pose after absorption; keep the initial cast unchanged."""
import sys,json
sys.dont_write_bytecode=True
from generate_summons import ROOT,save
from generate_epic_revision import put_track
from generate_rasengan import hand_path
def generate():
 m=json.loads((ROOT/'players/motionrp_kokuryuha.bbmodel').read_text());groups={g['name']:g for g in m['groups']}
 for name,end in [('pra_right_arm',(0,0,7)),('pla_left_arm',(0,0,-7)),('prfa_right_forearm',(0,0,0)),('plfa_left_forearm',(0,0,0)),('pw_waist',(0,0,0)),('pc_chest',(0,0,0)),('h_ph_head',(0,0,0))]:
  track=m['animations'][0]['animators'].get(groups[name]['uuid'],{});keys=[]
  for k in track.get('keyframes',[]):
   if k['channel']=='rotation' and k['time']<=8:keys.append((k['time'],tuple(float(k['data_points'][0][c])for c in 'xyz')))
  if not keys:keys=[(0,(0,0,0)),(8,(0,0,0))]
  keys.extend([(9.6,end),(16,end)]);put_track(m,name,keys)
 save('players/motionrp_kokuryuha.bbmodel',m);(ROOT/'kokuryuha-hand-path.csv').write_text(hand_path(m,320))

if __name__=='__main__':generate()
