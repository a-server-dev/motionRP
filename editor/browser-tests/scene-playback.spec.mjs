import {test, expect} from '@playwright/test';
import {readFileSync} from 'node:fs';

const sword = JSON.parse(readFileSync(new URL('../public/example-sword.bbmodel', import.meta.url)));
const bone = sword.groups[0].uuid;
sword.animations = [{name:'swing', length:2, animators:{[bone]:{type:'bone', keyframes:[
  {channel:'rotation', time:0, data_points:[{x:0,y:0,z:0}]},
  {channel:'rotation', time:1, data_points:[{x:0,y:0,z:90}]},
  {channel:'rotation', time:2, data_points:[{x:0,y:0,z:0}]},
]}}}];

async function observePreview(page) {
  await page.evaluate(async () => {
    const {Preview} = await import('/src/preview.js');
    const original = Preview.prototype.frame;
    Preview.prototype.frame = function(tick) {
      original.call(this,tick);
      window.previewSnapshot = {
        tick, only:this.only,
        models:this.instances.map(({e,object})=>({
          model:e.model, visible:!!object.visible, animation:e.animation,
          rotations:[...object.userData.bones.values()].map(b=>b.rotation.z),
          meshes:(()=>{let n=0;object.traverse(o=>{if(o.isMesh)n++;});return n;})(),
        })),
      };
    };
  });
}
async function scrub(page,tick) {
  await page.locator('#scrub').evaluate((input,value)=>{
    input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));
  },tick);
}

test('sample and imported animation render in scene, and replay restarts',async({page})=>{
  const errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto('/motionRP/');
  await expect(page.locator('#project-title')).toHaveText('光の剣');
  await observePreview(page);
  await scrub(page,20);
  let snapshot=await page.evaluate(()=>window.previewSnapshot);
  expect(snapshot.models.find(m=>m.model==='sword')).toMatchObject({visible:true});
  expect(snapshot.models[0].meshes).toBeGreaterThan(0);

  await page.locator('#files').setInputFiles({name:'animated_sword.bbmodel',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(sword))});
  await expect(page.locator('[data-model="animated_sword"]')).toBeVisible();
  await expect(page.locator('.track-label')).toHaveCount(4);
  await page.locator('#scene-view').click();
  await scrub(page,20);
  await expect.poll(async()=>page.evaluate(()=>window.previewSnapshot.models.find(m=>m.model==='animated_sword'))).toMatchObject({visible:true,animation:'swing'});
  snapshot=await page.evaluate(()=>window.previewSnapshot);
  expect(snapshot.models.find(m=>m.model==='animated_sword').rotations[0]).toBeCloseTo(Math.PI/2);

  // Existing drafts can place an unassigned asset without uploading it again.
  await page.locator('[data-model="animated_sword"]').click();
  await page.locator('#place-model').click();
  await expect(page.locator('[data-field="animation"]')).toHaveValue('swing');
  await page.locator('#delete-event').click();
  await page.locator('[data-model="animated_sword"]').click();
  await page.locator('#place-model').click();
  await expect(page.locator('[data-field="animation"]')).toHaveValue('swing');

  await scrub(page,99);
  await page.locator('#play').click();
  await expect.poll(async()=>Number(await page.locator('#scrub').inputValue())).toBeLessThan(40);
  await page.locator('#rewind').click();
  expect(errors).toEqual([]);
});

test('AI sample imports, articulates in scene, and exports an animated project',async({page})=>{
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('/motionRP/');
  await expect(page.locator('#project-title')).toHaveText('光の剣');
  await observePreview(page);
  await page.locator('#ai').click();
  await page.locator('#ai-sample').click();
  await expect(page.locator('#ai-json')).toHaveValue(/"tracks"/);
  await page.locator('#import-ai').click();
  await expect(page.locator('[data-model="generated_model"]')).toContainText('1 clips');
  await page.locator('#scene-view').click();
  await scrub(page,0);
  await expect.poll(async()=>page.evaluate(()=>window.previewSnapshot.models.find(m=>m.model==='generated_model'))).toMatchObject({visible:true,animation:'wave'});
  const initial=await page.evaluate(()=>window.previewSnapshot.models.find(m=>m.model==='generated_model').rotations);
  await scrub(page,20);
  const later=await page.evaluate(()=>window.previewSnapshot.models.find(m=>m.model==='generated_model').rotations);
  expect(later).not.toEqual(initial);
  await page.screenshot({path:'test-results/ai-wave.png'});
  const [download]=await Promise.all([page.waitForEvent('download'),page.locator('#export').click()]);
  await download.saveAs('test-results/ai-animated.zip');
  expect(errors).toEqual([]);
});
