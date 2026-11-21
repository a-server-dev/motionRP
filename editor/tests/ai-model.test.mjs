import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {partsToModel} from '../src/ai-model.js';
import {validateProject, promptFor} from '../src/project.js';

const sample = JSON.parse(fs.readFileSync(new URL('../public/animated-example.json', import.meta.url)));
const texture = JSON.parse(fs.readFileSync(new URL('../public/example-sword.bbmodel', import.meta.url))).textures[0].source;
const convert = raw => partsToModel(raw, () => texture);

test('AI rig converts named parent/child bones and numeric keys into real bbmodel animations', () => {
  const m = convert(sample), arm = m.groups.find(b=>b.name==='arm'), forearm=m.groups.find(b=>b.name==='forearm');
  assert.deepEqual(arm.origin,[-5,22,0]);
  const armNode=m.outliner[0].children.find(n=>n.uuid===arm.uuid);
  assert.ok(armNode.children.some(n=>n.uuid===forearm.uuid));
  assert.equal(m.animations[0].loop,'loop');
  assert.equal(m.animations[0].animators[arm.uuid].name,'arm');
  assert.equal(m.animations[0].animators[arm.uuid].keyframes[1].data_points[0].z,'-145');
  assert.deepEqual(validateProject({version:1,id:'wave',name:'Wave',duration:100,models:[{id:'robot',kind:'display'}],events:[{id:'robot',type:'model',model:'robot',tick:0,endTick:100,scale:1,animation:'wave'}]},{robot:m}),[]);
});

test('legacy static parts and raw bbmodel remain supported', () => {
  const m=convert({parts:[{from:[0,0,0],to:[1,1,1],color:'#ffffff'}]});
  assert.equal(m.groups.length,1);assert.equal(m.animations.length,0);
  assert.equal(convert(m),m);
});

test('invalid bone graphs, references, expressions and unbounded keys are rejected', () => {
  const cases=[
    r=>r.bones[0].parent='forearm',
    r=>r.bones[0].parent='missing',
    r=>r.parts[0].bone='missing',
    r=>r.bones.push({...r.bones[0]}),
    r=>r.animations[0].tracks[0].bone='missing',
    r=>r.animations[0].tracks[0].keyframes[0].rotation=['Math.random()',0,0],
    r=>r.animations[0].tracks[0].keyframes[0].time=3,
    r=>r.animations[0].tracks[0].keyframes[0].scale=[0,1,1],
    r=>r.animations[0].tracks[0].keyframes[0].interpolation='script',
    r=>r.animations[0].tracks[0].keyframes.push({...r.animations[0].tracks[0].keyframes[0]}),
    r=>r.animations[0].length=601,
  ];
  for(const mutate of cases){const r=structuredClone(sample);mutate(r);assert.throws(()=>convert(r));}
});

test('position, rotation, scale and step interpolation survive conversion',()=>{
  const r=structuredClone(sample);
  r.animations[0].loop=false;
  r.animations[0].tracks[0].keyframes[0]={time:0,position:[0,2,0],rotation:[0,0,-90],scale:[1,2,1],interpolation:'step'};
  const m=convert(r),arm=m.groups.find(b=>b.name==='arm');
  assert.equal(m.animations[0].loop,'once');
  assert.deepEqual(m.animations[0].animators[arm.uuid].keyframes.slice(0,3).map(k=>k.channel),['position','rotation','scale']);
  assert.equal(m.animations[0].animators[arm.uuid].keyframes[0].interpolation,'step');
});

test('AI prompt teaches the actual animation schema and units',()=>{
  const prompt=promptFor('右手を振るロボット');
  for(const word of ['右手を振るロボット','bones','tracks','keyframes','秒','初期姿勢','循環'])assert.ok(prompt.includes(word));
});
