import {validateModel} from './project.js';

const idPattern = /^[a-z][a-z0-9_]{0,31}$/;
function requireValue(ok, message) { if (!ok) throw Error(message); }
function vector(value, label, fallback) {
  const v = value ?? fallback;
  requireValue(Array.isArray(v) && v.length === 3 && v.every(n => typeof n === 'number' && Number.isFinite(n) && Math.abs(n) <= 8192), `${label}は有限の数値3個（±8192以内）で指定してください。`);
  return [...v];
}
function palette(colors) {
  const canvas = document.createElement('canvas');
  canvas.width = colors.length; canvas.height = 1;
  const context = canvas.getContext('2d');
  colors.forEach((color, i) => { context.fillStyle = color; context.fillRect(i, 0, 1, 1); });
  return canvas.toDataURL('image/png');
}

// Data-only conversion: no expressions, scripts, external textures or commands.
export function partsToModel(raw, createPalette = palette) {
  requireValue(raw && typeof raw === 'object', 'モデルのJSONを指定してください。');
  if (raw.elements) return validateModel(raw);
  requireValue(Array.isArray(raw.parts) && raw.parts.length >= 1 && raw.parts.length <= 200, 'partsを1〜200個含むJSON、またはbbmodelのJSONを貼り付けてください。');
  const sourceBones = raw.bones ?? [];
  requireValue(Array.isArray(sourceBones) && sourceBones.length <= 64, 'bonesは64個までです。');
  const bones = new Map();
  for (const b of sourceBones) {
    requireValue(b && idPattern.test(b.id) && !bones.has(b.id), 'ボーンIDは重複のない英小文字・数字・_（32文字以内）で指定してください。');
    requireValue(b.id !== 'root' || b.parent == null, 'rootに親は指定できません。');
    bones.set(b.id, {id:b.id, parent:b.id === 'root' ? null : b.parent ?? 'root', uuid:crypto.randomUUID(), origin:vector(b.origin,'ボーンの関節位置',[0,0,0]), rotation:vector(b.rotation,'ボーンの角度',[0,0,0])});
  }
  if (!bones.has('root')) bones.set('root', {id:'root', parent:null, uuid:crypto.randomUUID(), origin:[0,0,0], rotation:[0,0,0]});
  for (const b of bones.values()) {
    const seen = new Set(); let current = b;
    while (current.parent != null) {
      requireValue(!seen.has(current.id), 'ボーンの親子関係が循環しています。'); seen.add(current.id);
      requireValue(bones.has(current.parent), `${current.id}: 親ボーン「${current.parent}」がありません。`);
      current = bones.get(current.parent);
    }
  }
  const colors = [...new Set(raw.parts.map(p => p?.color ?? '#e2bd72'))];
  requireValue(colors.every(c => typeof c === 'string' && /^#[0-9a-f]{6}$/i.test(c)), '色は#RRGGBB形式です。');
  const nodes = new Map([...bones.values()].map(b => [b.id,{uuid:b.uuid, children:[]}]));
  const elements = raw.parts.map((p,i) => {
    const bone = p.bone ?? 'root';
    requireValue(bones.has(bone), `部品${i+1}: ボーン「${bone}」がありません。`);
    const from = vector(p.from,'部品の開始座標'), to = vector(p.to,'部品の終了座標');
    requireValue(from.every((n,j) => n <= to[j]), '部品のfromはto以下にしてください。');
    const color = colors.indexOf(p.color ?? '#e2bd72');
    const element = {name:typeof p.name === 'string' ? p.name : 'part_'+i, uuid:crypto.randomUUID(), type:'cube', from, to,
      origin:vector(p.origin,'部品の回転中心',from.map((n,j)=>(n+to[j])/2)), rotation:vector(p.rotation,'部品の角度',[0,0,0]),
      faces:Object.fromEntries(['north','south','east','west','up','down'].map(face=>[face,{uv:[color+.05,.05,color+.95,.95],texture:0}]))};
    nodes.get(bone).children.push(element.uuid);
    return element;
  });
  for (const b of bones.values()) if (b.parent != null) nodes.get(b.parent).children.push(nodes.get(b.id));

  const clips = raw.animations ?? [];
  requireValue(Array.isArray(clips) && clips.length <= 32, 'animationsは32個までです。');
  const names = new Set(); let keyCount = 0;
  const animations = clips.map(clip => {
    requireValue(clip && idPattern.test(clip.name) && !names.has(clip.name), 'アニメーション名は重複のない英小文字・数字・_（32文字以内）で指定してください。'); names.add(clip.name);
    requireValue(typeof clip.length === 'number' && Number.isFinite(clip.length) && clip.length > 0 && clip.length <= 600, 'アニメーションのlengthは秒単位で0より大きく600以下にしてください。');
    requireValue(clip.loop === undefined || typeof clip.loop === 'boolean', 'アニメーションのloopはtrueまたはfalseです。');
    requireValue(Array.isArray(clip.tracks) && clip.tracks.length > 0 && clip.tracks.length <= 64, 'アニメーションには1〜64個のtracksを指定してください。');
    const animators = {};
    for (const track of clip.tracks) {
      const bone = bones.get(track.bone);
      requireValue(bone && !animators[bone.uuid], 'トラックのボーンが存在しないか、重複しています。');
      requireValue(Array.isArray(track.keyframes) && track.keyframes.length > 0, 'トラックにkeyframesを指定してください。');
      const keyframes = [], times = new Set();
      for (const key of track.keyframes) {
        requireValue(typeof key.time === 'number' && Number.isFinite(key.time) && key.time >= 0 && key.time <= clip.length, 'キーフレームのtimeは0〜lengthの秒数です。');
        const interpolation = key.interpolation ?? 'linear';
        requireValue(['linear','step'].includes(interpolation), '補間はlinearまたはstepにしてください。');
        let channels = 0;
        for (const channel of ['position','rotation','scale']) if (key[channel] !== undefined) {
          channels++; keyCount++;
          requireValue(keyCount <= 4096, 'キーフレームの合計は4096チャンネルまでです。');
          requireValue(!times.has(`${key.time}:${channel}`), '同じ時刻の同じチャンネルは重複できません。'); times.add(`${key.time}:${channel}`);
          const v = vector(key[channel], `キーフレームの${channel}`);
          if (channel === 'scale') requireValue(v.every(n=>n>0 && n<=64), 'scaleは0より大きく64以下にしてください。');
          keyframes.push({uuid:crypto.randomUUID(),channel,time:key.time,interpolation,data_points:[{x:String(v[0]),y:String(v[1]),z:String(v[2])}]});
        }
        requireValue(channels > 0, 'キーフレームにはposition・rotation・scaleのいずれかが必要です。');
      }
      animators[bone.uuid] = {name:bone.id,type:'bone',keyframes:keyframes.sort((a,b)=>a.time-b.time)};
    }
    return {uuid:crypto.randomUUID(),name:clip.name,length:clip.length,loop:clip.loop ? 'loop' : 'once',override:false,animators};
  });
  return validateModel({meta:{format_version:'5.0',model_format:'free',box_uv:false},name:typeof raw.name === 'string' ? raw.name : 'generated_model',resolution:{width:colors.length,height:1},
    textures:[{id:'0',uuid:crypto.randomUUID(),name:'palette.png',width:colors.length,height:1,uv_width:colors.length,uv_height:1,source:createPalette(colors)}],elements,
    groups:[...bones.values()].map(b=>({name:b.id,uuid:b.uuid,origin:b.origin,rotation:b.rotation,export:true})),outliner:[nodes.get('root')],animations});
}
