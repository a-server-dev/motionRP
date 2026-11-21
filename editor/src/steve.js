import {partsToModel} from './ai-model.js';

// Standard BetterModel player mapper names; aliases are the public AI recipe format.
export const STEVE_BONES = {
  root:'player_root', hip:'phip_hip', waist:'pw_waist', chest:'pc_chest', head:'h_ph_head',
  right_arm:'pra_right_arm', right_forearm:'prfa_right_forearm',
  left_arm:'pla_left_arm', left_forearm:'plfa_left_forearm',
  right_leg:'prl_right_leg', right_foreleg:'prfl_right_foreleg',
  left_leg:'pll_left_leg', left_foreleg:'plfl_left_foreleg',
};
let rigPromise;
export async function steveRig() {
  rigPromise ??= fetch(import.meta.env.BASE_URL+'steve-rig.bbmodel').then(r=>{
    if(!r.ok)throw Error('スティーブのリグを読み込めませんでした。再読み込みしてください。');
    return r.json();
  }).catch(e=>{rigPromise=null;throw e;});
  return structuredClone(await rigPromise);
}

export function animateSteve(raw, template) {
  if(raw.target!=='player')throw Error('スティーブ用JSONにはtarget:"player"を指定してください。');
  const model=structuredClone(template), groups=new Map(model.groups.map(b=>[b.name,b]));
  const bones=Object.entries(STEVE_BONES).map(([id,name])=>{
    const b=groups.get(name);if(!b)throw Error('スティーブのリグに関節がありません: '+name);
    return {id,origin:b.origin};
  });
  // Reuse the bounded, data-only keyframe converter. Geometry is always the fixed player rig.
  const generated=partsToModel({name:'steve_motion',bones,parts:[{from:[0,0,0],to:[1,1,1]}],animations:raw.animations},()=>model.textures[0].source);
  const aliases=new Map(generated.groups.map(b=>[b.uuid,b.name]));
  model.animations=generated.animations.map(a=>({...a,animators:Object.fromEntries(Object.entries(a.animators).map(([uuid,track])=>{
    const b=groups.get(STEVE_BONES[aliases.get(uuid)]);
    return [b.uuid,{...track,name:b.name}];
  }))}));
  if(!model.animations.length){
    const b=groups.get('player_root');
    model.animations=[{uuid:crypto.randomUUID(),name:'idle',length:1,loop:'loop',override:false,animators:{[b.uuid]:{name:b.name,type:'bone',keyframes:[{channel:'position',time:0,interpolation:'linear',data_points:[{x:'0',y:'0',z:'0'}]}]}}}];
  }
  model.name='steve_motion';
  return model;
}

export function stevePrompt(description) {
  return `Minecraftのスティーブを動かすアニメーションを作ってください。作りたい動き: ${description}
返答は説明なしのJSONのみ。Webツールが固定のスティーブ用リグへ適用し、bbmodelに変換します。形やスキンは作らず、partsやbonesの定義は不要です。targetは必ず"player"です。
{"target":"player","animations":[{"name":"wave","length":2,"loop":true,"tracks":[{"bone":"right_arm","keyframes":[{"time":0,"rotation":[0,0,100]},{"time":1,"rotation":[0,0,145]},{"time":2,"rotation":[0,0,100]}]},{"bone":"right_forearm","keyframes":[{"time":0,"rotation":[0,0,0]},{"time":0.5,"rotation":[0,0,-35]},{"time":1,"rotation":[0,0,15]},{"time":1.5,"rotation":[0,0,-35]},{"time":2,"rotation":[0,0,0]}]}]}]}
使えるboneは ${Object.keys(STEVE_BONES).join(', ')} のみ。rootは全身、hipは腰全体、waistは胴体下部、chestは胸、headは頭。armは肩、forearmは肘、legは股関節、forelegは膝です。左右はスティーブ本人の左右。上腕には前腕、上脚には下脚が追従します。胸→腕・頭、腰→胸・脚の既存の親子関係で動きます。
単位: timeとlengthは秒（tickではない）、rotationは初期姿勢からの追加回転の度数、positionは初期姿勢からの相対移動（16=1ブロック）、scaleは倍率です。Yが上、前は-Z、本人の右は+Xです。正のX回転で腕が前へ上がり、本人右腕は正のZ回転で横へ上がります。両腕を上げるときは左右のZ回転を反対符号にしてください。
最大32クリップ、各lengthは0より大きく600秒以下。nameは重複しない英小文字・数字・_で32文字以内。loopはtrue/false。各tracksのboneは重複不可。keyframesのtimeは0〜length。rotation/position/scaleは数値3個の配列、interpolationはlinear（標準）かstep。最初と最後にキーを置き、ループなら両端を同じ姿勢にします。全体4096チャンネル以下。数式・コード・URL・外部ファイルは禁止です。依頼された動作がわかるように腕だけでなく頭・胸・腰・脚にも必要に応じてキーをつけてください。`;
}
