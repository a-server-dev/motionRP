#!/usr/bin/env python3
"""Validate data-only Studio projects and compile namespaced BetterModel resources."""
import argparse,base64,copy,json,math,re,shutil,sys,uuid,struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
PARTICLES={'END_ROD','FLAME','SOUL_FIRE_FLAME','CLOUD','CRIT','ELECTRIC_SPARK','GUST','SMALL_GUST','EXPLOSION','EXPLOSION_EMITTER','POOF'}
SOUNDS={'ENTITY_PLAYER_ATTACK_SWEEP','BLOCK_AMETHYST_BLOCK_CHIME','ENTITY_GENERIC_EXPLODE','ENTITY_BREEZE_WIND_BURST','ENTITY_ENDERMAN_TELEPORT','BLOCK_NOTE_BLOCK_BANJO'}
def require(ok,message):
 if not ok:raise ValueError(message)
def number(v,lo,hi,label):require(type(v)in(int,float)and math.isfinite(v)and lo<=v<=hi,label);return v
def integer(v,lo,hi,label):require(type(v)is int,label);return number(v,lo,hi,label)
def vector(v,label):require(type(v)is list and len(v)==3,label);return [number(x,-64,64,label)for x in v]
def slug(v,label):require(type(v)is str and re.fullmatch(r'[a-z][a-z0-9_]{0,31}',v),label);return v
def validate_model(m,label):
 require(type(m)is dict and type(m.get('elements'))is list,label+': bbmodelのelementsが必要です')
 require(len(m['elements'])<=20000,label+': 要素数は20000までです')
 require(len(m.get('groups',[]))<=512,label+': ボーン数は512までです')
 require(1<=len(m.get('textures',[]))<=16,label+': 埋込PNGテクスチャは1〜16個です')
 require(sum(len(e.get('faces',{}))for e in m['elements'])<=200000,label+': 面数が多すぎます')
 def check_tree(nodes,depth=0):
  require(depth<=128,label+': 階層が深すぎます')
  for n in nodes:
   if type(n)is dict:check_tree(n.get('children',[]),depth+1)
 check_tree(m.get('outliner',[]))
 groups={g['uuid']:g for g in m.get('groups',[])}
 def normalize(nodes):
  result=[]
  for n in nodes:
   if type(n)is str:result.append(n);continue
   uid=n['uuid']
   if uid not in groups:groups[uid]={'uuid':uid,'name':n.get('name','bone'),'origin':n.get('origin',[0,0,0]),'rotation':n.get('rotation',[0,0,0]),'export':n.get('export',True)}
   result.append({'uuid':uid,'children':normalize(n.get('children',[]))})
  return result
 m['outliner']=normalize(m.get('outliner',[]));m['groups']=list(groups.values());require(len(groups)<=512,label+': ボーン数は512までです')
 for i,t in enumerate(m.get('textures',[])):
  src=t.get('source','');require(src.startswith('data:image/png;base64,'),label+': テクスチャをPNGで埋め込んでください')
  raw=base64.b64decode(src.split(',',1)[1],validate=True);require(len(raw)<=8*1024*1024 and raw.startswith(b'\x89PNG\r\n\x1a\n'),label+': PNGが不正です')
  w,h=struct.unpack('!II',raw[16:24]);require(0<w<=4096 and 0<h<=4096,label+': PNGは4096pxまでです')
  t['name']='studio_texture_'+str(i)+'.png';t.pop('path',None);t.pop('relative_path',None)
 for a in m.get('animations',[]):
  number(a.get('length',0),0,600,label+': アニメーションは600秒までです')
  for track in a.get('animators',{}).values():
   require(track.get('type','bone')=='bone',label+': 効果・コマンド用アニメーターは使用できません。Studioのイベントで設定してください')
   for k in track.get('keyframes',[]):
    require(k.get('channel')in('position','rotation','scale'),label+': 未対応のキーフレームです')
    number(k.get('time',0),0,600,label+': キーフレーム時刻が不正です')
    for point in k.get('data_points',[]):
     for axis in ['x','y','z']:number(float(point.get(axis,1 if k['channel']=='scale'else 0)),-8192,8192,label+': 数式入りキーフレームは対応していません')
 for e in m['elements']:

  require(e.get('type','cube')in('cube','mesh'),label+': cube / mesh のみ対応しています')
  points=[e.get('from',[0,0,0]),e.get('to',[0,0,0]),e.get('origin',[0,0,0])]+list(e.get('vertices',{}).values())
  for p in points:
   require(type(p)is list and len(p)==3,label+': 座標が不正です')
   for x in p:number(x,-8192,8192,label+': モデル座標は±8192までです')
 return m

def compile_project(folder):
 require((folder/'project.json').stat().st_size<=1024*1024,'project.jsonは1MiBまでです')
 p=json.loads((folder/'project.json').read_text());slug(p.get('id'),'idは英小文字から始まる32文字以内の英数字・_');require(p['id']==folder.name,'フォルダ名とidが一致しません')
 require(p.get('version')==1,'project versionは1にしてください');require(type(p.get('enabled',True))is bool,'enabledはbooleanです')
 require(type(p.get('name'))is str and 1<=len(p['name'])<=64,'表示名は1〜64文字です');integer(p.get('duration'),2,12000,'長さは2〜12000tickです')
 require(type(p.get('models'))is list and 1<=len(p['models'])<=16,'モデルは1〜16個です');require(type(p.get('events'))is list and len(p['events'])<=128,'イベントは128個までです')
 assets={};models={};total_bytes=0;compiled=copy.deepcopy(p);prefix='motionrp_studio_'+p['id']
 for entry in compiled['models']:
  mid=slug(entry.get('id'),'モデルidが不正です');require(mid not in models,'モデルidが重複しています');require(entry.get('kind','display')in('display','player'),'モデル種別が不正です')
  file=entry.get('file','');require(re.fullmatch(r'models/[a-z][a-z0-9_]{0,31}\.bbmodel',file or ''),'モデルのパスはmodels/<id>.bbmodelです')
  path=folder/file;require(not path.is_symlink()and path.resolve().is_relative_to(folder.resolve()),'外部パスは使用できません');require(path.stat().st_size<=30*1024*1024,'1ファイル30MiBまでです')
  total_bytes+=path.stat().st_size;require(total_bytes<=80*1024*1024,'作品のモデル合計は80MiBまでです')
  model=validate_model(json.loads(path.read_text()),file);model['name']=model['model_identifier']=prefix+'_'+mid
  entry['asset']=model['name'];kind=entry.get('kind','display');entry['kind']=kind
  names={}
  for gi,g in enumerate(model.get('groups',[])):
   if kind=='player':require(re.fullmatch(r'[A-Za-z0-9_-]{1,80}',g.get('name','')),'プレイヤーのボーン名が不正です')
   else:g['name']='studio_bone_'+str(gi)
   names[g['uuid']]=g['name']
  for ei,e in enumerate(model['elements']):e['name']='part_'+str(ei)
  for a in model.get('animations',[]):
   for uid,track in a.get('animators',{}).items():
    require(uid in names,'アニメーションのボーン参照が不正です');track['name']=names[uid]
  entry['animations']=[a['name']for a in model.get('animations',[])];require(len(entry['animations'])<=64,'アニメーション数は64個までです')
  if kind=='player':require(any(g.get('name')=='player_root'for g in model.get('groups',[])),'プレイヤー用にはBetterModelのplayer_rootリグが必要です')
  assets[('players/'if kind=='player'else'models/')+model['name']+'.bbmodel']=model;models[mid]=entry
 body=p.get('body')
 if body:
  require(body.get('model')in models and models[body['model']]['kind']=='player','本体モデルが不正です');require(body.get('animation')in models[body['model']]['animations'],'本体アニメーションがありません')
  compiled['bodyModel']=models[body['model']]['asset'];compiled['bodyAnimation']=body['animation']
 else:
  rig=json.loads((ROOT/'src/main/resources/bettermodel/players/motionrp_rasengan.bbmodel').read_text());name=prefix+'_body';rig['name']=rig['model_identifier']=name
  root=next(g for g in rig['groups']if g['name']=='player_root')
  rig['animations']=[{'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,name+'_clock')),'name':'studio_clock','loop':'once','override':False,'length':p['duration']/20,'animators':{root['uuid']:{'name':'player_root','type':'bone','keyframes':[{'channel':'position','time':t,'interpolation':'linear','data_points':[{'x':'0','y':'0','z':'0'}]}for t in [0,p['duration']/20]]}}}]
  assets['players/'+name+'.bbmodel']=rig;compiled['bodyModel']=name;compiled['bodyAnimation']='studio_clock'
 moves=[];ids=set();particle_budget=0;sound_count=0
 for e in compiled['events']:
  eid=slug(e.get('id'),'イベントidが不正です');require(eid not in ids,'イベントidが重複しています');ids.add(eid);typ=e.get('type');require(typ in('model','particle','sound','move_player'),'未対応のイベントです')
  integer(e.get('tick'),0,p['duration']-1,'イベント開始tickが範囲外です')
  if typ!='sound':integer(e.get('endTick'),e['tick']+1,p['duration'],'イベント終了tickが範囲外です')
  require(e.get('anchor','origin')in('origin','player'),'追従先が不正です');vector(e.get('from',[0,0,0]),'開始位置が不正です');vector(e.get('to',e.get('from',[0,0,0])),'終了位置が不正です')
  if typ=='model':
   require(e.get('model')in models and models[e['model']]['kind']=='display','配置できるのは表示用モデルです');e['asset']=models[e['model']]['asset'];number(e.get('scale',1),.05,64,'スケールは0.05〜64です');number(e.get('pitch',0),-360,360,'pitchが範囲外です');number(e.get('yaw',0),-360,360,'yawが範囲外です')
   if e.get('animation'):require(e['animation']in models[e['model']]['animations'],'選択したアニメーションがありません')
  if typ=='particle':particle_budget+=e.get('count',8)/max(1,e.get('every',4));require(e.get('particle')in PARTICLES,'未対応のパーティクルです');integer(e.get('count',8),1,64,'粒子数は1〜64です');integer(e.get('every',4),2,200,'粒子間隔は2〜200tickです');number(e.get('spread',.3),0,8,'粒子範囲は0〜8です')
  if typ=='sound':require(e.get('sound')in SOUNDS,'未対応の音です');number(e.get('volume',1),0,3,'音量は0〜3です');number(e.get('pitch',1),.5,2,'音程は0.5〜2です')
  if typ=='move_player':
   require(e['endTick']-e['tick']>=5,'一時移動は5tick以上です');require(math.dist(e.get('from',[0,0,0]),e['to'])/(e['endTick']-e['tick']-1)<=1.5,'移動速度は1tickあたり1.5ブロックまでです');moves.append(e)
 require(particle_budget<=48,'パーティクルの合計発生量が多すぎます（最大48個/tick）')
 require(sum(e['type']=='sound'for e in p['events'])<=48,'サウンドは48イベントまでです')
 require(sum(e['type']=='model'for e in p['events'])<=24,'配置モデルイベントは24個までです')
 last=None
 for e in sorted(moves,key=lambda x:x['tick']):
  require(last is None or last['endTick']<=e['tick'],'移動区間を重ねることはできません');require(e.get('from',[0,0,0])==(last['to']if last else[0,0,0]),'移動の開始位置を前の終了位置に合わせてください');last=e
 return compiled,assets

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--output',type=Path);ap.add_argument('--check',action='store_true');args=ap.parse_args();out=args.output
 if out:
  if out.exists():shutil.rmtree(out)
  out.mkdir(parents=True)
 projects=[];seen=set()
 for f in sorted((ROOT/'projects').glob('*/project.json')):
  require(not f.is_symlink()and not f.parent.is_symlink(),'symlinkは使用できません');p,assets=compile_project(f.parent);require(p['id']not in seen,'idが重複しています');seen.add(p['id'])
  print('VALID',p['id'],'enabled='+str(p.get('enabled',True)))
  if not p.get('enabled',True):continue
  p['assets']=list(assets);projects.append(p)
  if out:
   for name,data in assets.items():path=out/'bettermodel'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False))
 if out:
  (out/'studio').mkdir(exist_ok=True);(out/'studio/index.json').write_text(json.dumps({'version':1,'projects':projects},ensure_ascii=False))
 print('Studio projects:',len(projects))
if __name__=='__main__':
 try:main()
 except (ValueError,KeyError,TypeError,OSError)as e:print('Studio validation failed:',str(e),file=sys.stderr);sys.exit(1)
