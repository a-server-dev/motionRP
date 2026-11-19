"""Matrix sampling for the authored sword and head attachments (source coordinates)."""
import math

def matrix(x,y,z):
 x,y,z=map(math.radians,(x,y,z));cx,sx=math.cos(x),math.sin(x);cy,sy=math.cos(y),math.sin(y);cz,sz=math.cos(z),math.sin(z)
 return [[cz*cy,cz*sy*sx-sz*cx,cz*sy*cx+sz*sx],[sz*cy,sz*sy*sx+cz*cx,sz*sy*cx-cz*sx],[-sy,cy*sx,cy*cx]]
def mul(a,b):return [[sum(a[i][k]*b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def vector(a,v):return [sum(a[i][j]*v[j] for j in range(3)) for i in range(3)]
def euler(a):return [math.degrees(math.atan2(a[2][1],a[2][2])),math.degrees(math.asin(max(-1,min(1,-a[2][0])))),math.degrees(math.atan2(a[1][0],a[0][0]))]
def sampler(m):
 groups={g['uuid']:g for g in m['groups']};names={g['name']:g['uuid'] for g in m['groups']};parents={};tracks=m['animations'][0]['animators']
 def walk(nodes,parent=None):
  for node in nodes:
   if isinstance(node,dict):parents[node['uuid']]=parent;walk(node['children'],node['uuid'])
 walk(m['outliner'])
 def sample(id,t):
  frames=[k for k in tracks.get(id,{}).get('keyframes',[]) if k['channel']=='rotation']
  if not frames:return [0,0,0]
  lo=max((k for k in frames if k['time']<=t),key=lambda k:k['time'],default=frames[0]);hi=min((k for k in frames if k['time']>=t),key=lambda k:k['time'],default=frames[-1]);f=(t-lo['time'])/(hi['time']-lo['time']) if hi['time']!=lo['time'] else 0
  return [float(lo['data_points'][0][c])*(1-f)+float(hi['data_points'][0][c])*f for c in 'xyz']
 def rotation(name,t):
  id=names[name];chain=[]
  while id is not None:chain.append(id);id=parents[id]
  result=matrix(0,0,0)
  for id in reversed(chain):result=mul(result,matrix(*sample(id,t)))
  return result
 return rotation
