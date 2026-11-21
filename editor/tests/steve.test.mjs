import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {animateSteve, STEVE_BONES, stevePrompt} from '../src/steve.js';
import {validateModel,validateProject} from '../src/project.js';
const template=JSON.parse(fs.readFileSync(new URL('../public/steve-rig.bbmodel',import.meta.url)));
const recipe=JSON.parse(fs.readFileSync(new URL('../public/steve-wave.json',import.meta.url)));

test('Steve recipes preserve the real player rig, UVs and mapper names',()=>{
 const before=JSON.stringify(template), m=animateSteve(recipe,template);
 assert.equal(JSON.stringify(template),before);
 assert.deepEqual(m.elements,template.elements);
 assert.deepEqual(m.textures,template.textures);
 assert.deepEqual(m.outliner,template.outliner);
 for(const name of Object.values(STEVE_BONES))assert.ok(m.groups.some(b=>b.name===name));
 const arm=m.groups.find(b=>b.name===STEVE_BONES.right_arm);
 assert.equal(m.animations[0].animators[arm.uuid].name,arm.name);
 assert.equal(m.animations[0].animators[arm.uuid].keyframes[1].data_points[0].z,'145');
 validateModel(m);
 assert.deepEqual(validateProject({version:1,id:'steve_wave',name:'Steve wave',duration:100,models:[{id:'steve',kind:'player'}],events:[],body:{model:'steve',animation:'wave'}},{steve:m}),[]);
});
test('Steve accepts only supported player joints and bounded keyframes',()=>{
 const bad=structuredClone(recipe);bad.animations[0].tracks[0].bone='tentacle';assert.throws(()=>animateSteve(bad,template));
 bad.animations[0].tracks[0].bone='right_arm';bad.animations[0].tracks[0].keyframes[0].rotation=['random()',0,0];assert.throws(()=>animateSteve(bad,template));
 assert.equal(animateSteve({target:'player'},template).animations[0].name,'idle');
});
test('Steve prompt defaults to the fixed player rig and explains joint axes',()=>{
 const s=stevePrompt('手を振る');for(const word of ['手を振る','"target":"player"','right_arm','right_forearm','秒','本人の右は+X'])assert.ok(s.includes(word));
});
