import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
import { spawn, execFileSync } from 'node:child_process';
import mineflayer from 'mineflayer';
import nbt from 'prismarine-nbt';
import { gunzipSync } from 'node:zlib';

// A disposable, loopback-only offline server. Published Velocity/Paper settings
// and real player data are never edited. This does not test Mojang login or rendering.
const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const version = fs.readFileSync(path.join(project, 'build.gradle'), 'utf8').match(/^version = '([^']+)'/m)[1];
const fixture = process.env.MOTIONRP_PAPER_FIXTURE || '/workspace/runtime/paper';
const run = path.join(project, 'build/integration-server');
const resultFile = path.join(project, 'build/integration-result.json');
// A single fixture cannot be shared by the full suite and a short verification.
const fixtureLock=path.join(project,'build/integration-run.lock');fs.mkdirSync(path.dirname(fixtureLock),{recursive:true});
let fixtureLockFd;
try {fixtureLockFd=fs.openSync(fixtureLock,'wx');}
catch(error) {
  if(error.code!=='EEXIST')throw error;
  const oldPid=Number(fs.readFileSync(fixtureLock,'utf8'));let alive=false;
  if(!Number.isSafeInteger(oldPid) || oldPid<=0)throw new Error('Fixture lock is being initialized or is incomplete; do not start another check.');
  if(Number.isSafeInteger(oldPid) && oldPid>0) {try {process.kill(oldPid,0);alive=true;}catch(e) {if(e.code!=='ESRCH')throw e;}}
  if(alive)throw new Error('Another integration check is using this fixture; wait for it to finish.');
  fs.unlinkSync(fixtureLock);fixtureLockFd=fs.openSync(fixtureLock,'wx');
}
fs.writeFileSync(fixtureLockFd,String(process.pid));
process.on('exit',()=>{fs.closeSync(fixtureLockFd);fs.unlinkSync(fixtureLock);});
const port = 25576;
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const checks = [];
const bots = [];
let server;
let output = '';
let log;
let heightSampler;

function pass(name) { checks.push(name); console.log('PASS:', name); }
async function waitFor(predicate, description, timeout = 20_000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (await predicate()) return;
    await sleep(100);
  }
  throw new Error('Timeout: ' + description);
}
function uuid(name) {
  const data = createHash('md5').update('OfflinePlayer:' + name).digest();
  data[6] = (data[6] & 15) | 48;
  data[8] = (data[8] & 63) | 128;
  const h = data.toString('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}
function consoleCommand(command) { server.stdin.write(command + '\n'); }

async function startServer() {
  output = '';
  const opts = (process.env.GRADLE_OPTS || '').trim().split(/\s+/).filter(Boolean);
  const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : 'java';
  server = spawn(java, [...opts, '-XX:ActiveProcessorCount=2', '-Xms512M', '-Xmx2G', '-jar',
    path.join(fixture, 'paper-26.3-253.jar'), '--nogui'], { cwd: run, stdio: ['pipe', 'pipe', 'pipe'] });
  server.on('error', error => { output += error.stack; });
  for (const stream of [server.stdout, server.stderr]) stream.on('data', data => { output += data; log.write(data); });
  await waitFor(() => output.includes('Done (') || server.exitCode !== null, 'Paper startup', 120_000);
  assert.equal(server.exitCode, null, output.slice(-4000));
  assert.ok(output.includes('MotionRP enabled:'), 'MotionRP did not enable');
  pass('Paper 26.3 loaded MotionRP and BetterModel');
}
async function stopServer() {
  if (!server || server.exitCode !== null) return;
  consoleCommand('stop');
  await waitFor(() => server.exitCode !== null, 'clean shutdown', 30_000);
  assert.equal(server.exitCode, 0, 'Paper shutdown failed');
}

async function connect(name) {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port, username: name, auth: 'offline', version: '26.1', physicsEnabled: false });
  bot.physicsEnabled = false;
  // These clients test inventory/interaction packets while stationary. Avoid
  // Mineflayer's autonomous movement/teleport-response packets across translation;
  // teleport confirmations and server-authoritative teleports are still processed.
  const write = bot._client.write.bind(bot._client);
  bot._client.write = (name, packet) => {
    if (['position', 'position_look', 'look', 'flying'].includes(name)) return;
    return write(name, packet);
  };
  bot._testLook = (yaw, pitch) => write('look', { yaw, pitch,
    flags: { onGround: true, hasHorizontalCollision: false } });
  bot._testLookUp = () => bot._testLook(0, -60);
  bot._testLeftClick = () => write('arm_animation', { hand: 'main_hand' });
  bot._testStep = (dx, dy, dz) => write('position_look', {
    x: bot.entity.position.x + dx, y: bot.entity.position.y + dy, z: bot.entity.position.z + dz,
    yaw: 0, pitch: -60, flags: { onGround: dy === 0, hasHorizontalCollision: false }
  });
  bots.push(bot);
  bot.on('error', error => console.error(name, error.message));
  bot.on('kicked', reason => console.error(name, 'kicked', JSON.stringify(reason)));
  bot._testMessages = [];
  bot._testStateId = 0;
  for (const event of ['window_items', 'set_slot']) {
    bot._client.on(event, packet => { if (packet.stateId !== undefined) bot._testStateId = packet.stateId; });
  }
  bot.on('message', message => bot._testMessages.push(message.toString()));
  let spawned = false;
  bot.once('spawn', () => { spawned = true; });
  await waitFor(() => spawned, name + ' spawn', 30_000);
  await sleep(1000);
  if(process.env.MOTIONRP_PERFORMANCE_ONLY!=='1' && process.env.MOTIONRP_VISIBILITY_ONLY!=='1'){bot.chat('/motionrp view on');await sleep(200);}
  return bot;
}
function tokens(bot) { return bot.inventory.items().filter(item => item.name === 'paper').reduce((sum, item) => sum + item.count, 0); }
function registered(bot) { return bot.currentWindow.slots.slice(0, bot.currentWindow.inventoryStart).filter(item => item?.name === 'paper').length; }
async function menu(bot) {
  bot.chat('/motionrp gui');
  await waitFor(() => bot.currentWindow !== null, 'registration GUI');
  await sleep(200);
}
async function closeMenu(bot) { if (bot.currentWindow) bot.closeWindow(bot.currentWindow); await sleep(300); }
async function click(bot, slot, button = 0, mode = 0) {
  if ([2, 4, 6].includes(mode)) {
    // Raw adversarial clicks: Mineflayer does not implement double-click locally.
    bot._client.write('window_click', { windowId: bot.currentWindow.id, stateId: bot._testStateId,
      slot, mouseButton: button, mode, changedSlots: [], cursorItem: null });
  } else await bot.clickWindow(slot, button, mode);
  await sleep(300);
}
async function deposit(bot) {
  const index = bot.currentWindow.slots.findIndex((item, i) => i >= bot.currentWindow.inventoryStart && item?.name === 'paper');
  assert.ok(index >= bot.currentWindow.inventoryStart, 'Motion token missing from storage');
  await click(bot, index);
}
async function disconnect(bot) { bot.quit(); await sleep(500); }
async function readPlayerData(name){const {parsed}=await nbt.parse(fs.readFileSync(path.join(run,'world/players/data',uuid(name)+'.dat')));return nbt.simplify(parsed);}
async function readSaved(name) {
  const { parsed } = await nbt.parse(fs.readFileSync(path.join(run, 'world/players/data', uuid(name) + '.dat')));
  const data = nbt.simplify(parsed);
  const bytes = data.BukkitValues?.['motionrp:loadout_v1'];
  assert.ok(bytes, 'Player PDC loadout is missing');
  return { data, loadout: Buffer.from(bytes) };
}

async function verifyTitanRecovery(a,b,{sceneDisplays,sceneRemoved,removedByRole,summonTrace,renderedForBoth,removedForBoth}) {
  function skinUrls(value) {
    if(typeof value==='string') {
      try {const decoded=JSON.parse(Buffer.from(value,'base64').toString('utf8'));return decoded?.textures?.SKIN?.url?[decoded.textures.SKIN.url]:[];} catch {return [];}
    }
    return value && typeof value==='object' ? Object.values(value).flatMap(skinUrls) : [];
  }
  const profileUpdates=[],scaleUpdates=[],nativeLightning=new Set(),playerFlags=[];
  b._client.on('spawn_entity',p=>{if(p.type===b.registry.entitiesByName.lightning_bolt.id) nativeLightning.add(p.entityId);});
  b._client.on('entity_metadata',p=>{if(p.entityId===a.entity?.id) for(const e of p.metadata) if(e.key===0) playerFlags.push(e.value);});
  b._client.on('player_info',packet=>{
    for(const e of packet.data||[]) if(e.uuid===uuid('MotionTesterA') && e.player) profileUpdates.push(skinUrls(e.player));
  });
  b._client.on('entity_update_attributes',packet=>{
    if(packet.entityId!==a.entity?.id) return;
    for(const p of packet.properties||[]) if(String(p.key).includes('scale')) scaleUpdates.push(p.value);
  });
  const noTitanJournal=async()=>{
    const saved=await readSaved('MotionTesterA');
    return !Object.keys(saved.data.BukkitValues||{}).some(k=>k.startsWith('motionrp:titan_'));
  };
  consoleCommand('attribute MotionTesterA minecraft:scale base set 1.25');await sleep(400);
  sceneDisplays.clear();sceneRemoved.clear();const titanBegin=output.length;
  const titanHealth=[a.health,b.health],titanOrigin=a.entity.position.clone();
  a._testLeftClick();await waitFor(()=>output.slice(titanBegin).includes('Titan phase: GIANT'),'actual giant form',20000);
  await sleep(350);
  const titanEntries=summonTrace('Titan',titanBegin);
  assert.ok(titanEntries.some(e=>e.kind==='giant'));assert.ok(titanEntries.filter(e=>e.kind.startsWith('lightning')).length>=6);
  await waitFor(()=>renderedForBoth(titanEntries),'both clients receive generated lightning and replacement skin models',3000);
  assert.ok(scaleUpdates.some(v=>Math.abs(v-10)<.001),'Observer received no real scale-10 attribute: '+scaleUpdates);
  assert.ok(output.slice(titanBegin).includes('skin=bundled:titan.png'),'Uploaded embedded skin is not selected');
  assert.ok(profileUpdates.every(urls=>urls.length===0),'Bundled giant must preserve the original player profile');
  const pending=await readSaved('MotionTesterA');assert.equal(pending.data.BukkitValues['motionrp:titan_scale_v1'],1.25);
  assert.ok(nativeLightning.size>=4,'No vanilla lightning entities reached the observer');
  pass('hand bite triggers native lightning/flash, a giant skin model, real scale 10 and the exact embedded user skin');
  await waitFor(()=>output.slice(titanBegin).includes('Titan phase: EXIT'),'human exits the nape',20000);
  await waitFor(()=>a.entity.position.y>titanOrigin.y+13,'client sees human emerge at nape',2000);
  await waitFor(()=>Math.abs(scaleUpdates.at(-1)-1.25)<.001,'observer receives restored original scale',3000);assert.equal((profileUpdates.at(-1)?.length??0),0);
  await sleep(300);assert.equal(playerFlags.at(-1)&0x20,0,'The real player remains hidden after leaving its model');
  a._testStep(.2,.1,.2); // Inputs cannot cancel the owned dive.
  pass('original skinned real player emerges at the nape while the giant stays full size');
  await waitFor(()=>output.slice(titanBegin).includes('Titan phase: STEAM'),'landing followed by steam',20000);
  await waitFor(()=>removedForBoth(titanEntries) && sceneRemoved.size===sceneDisplays.size,'giant and lightning cleanup',15000);
  await waitFor(noTitanJournal,'titan state journal cleared');
  assert.ok(Math.abs(scaleUpdates.at(-1)-1.25)<.001,'Scale did not restore: '+scaleUpdates.at(-1));
  assert.ok(profileUpdates.every(urls=>urls.length===0),'Original empty test-player texture properties did not restore');
  assert.ok(Math.abs(a.entity.position.y-titanOrigin.y)<.01);
  assert.ok(Math.abs(a.entity.position.z-(titanOrigin.z-5))<.01);assert.deepEqual([a.health,b.health],titanHealth);
  assert.ok([...nativeLightning].every(id=>removedByRole.observer.has(id)));
  pass('real player dives five blocks behind the giant, lands without damage and all lightning/models clean up');

  sceneDisplays.clear();sceneRemoved.clear();const titanCancel=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(titanCancel).includes('Titan phase: GIANT'),'giant before cancellation',20000);
  const cancelledGiant=summonTrace('Titan',titanCancel);a._testLeftClick();
  await waitFor(()=>removedForBoth(cancelledGiant) && sceneRemoved.size===sceneDisplays.size,'cancel clears giant and lightning');
  await waitFor(noTitanJournal,'cancel restores saved state');
  await waitFor(()=>Math.abs(scaleUpdates.at(-1)-1.25)<.001,'observer receives restored original scale',3000);assert.equal((profileUpdates.at(-1)?.length??0),0);
  pass('cancelling at scale 10 restores the original skin/scale and removes every replacement display');

  await sleep(300); // Controller inputs intentionally debounce for four ticks.
  sceneDisplays.clear();sceneRemoved.clear();const airOrigin=a.entity.position.clone(),airBegin=output.length;
  a._testLeftClick();await waitFor(()=>output.slice(airBegin).includes('Titan phase: DIVE'),'titan dive before cancellation',20000);
  assert.ok(a.entity.position.y>airOrigin.y+10);const airEntries=summonTrace('Titan',airBegin);
  a._testLeftClick();await waitFor(()=>a.entity.position.distanceTo(airOrigin)<.01 && removedForBoth(airEntries),'air cancel restores safe ground');
  await waitFor(noTitanJournal,'air cancel clears recovery state');
  pass('cancelling the nape dive safely returns the actual player to its ground anchor');
  await sleep(300);
  const titanQuit=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(titanQuit).includes('Titan phase: GIANT'),'giant before logout',20000);
  await disconnect(a);await waitFor(noTitanJournal,'logout restores the player.dat state');
  a=await connect('MotionTesterA');
  const restored=await readSaved('MotionTesterA');
  assert.ok(!Object.keys(restored.data.BukkitValues||{}).some(k=>k.startsWith('motionrp:titan_')));
  pass('logout during giant form restores saved scale/skin state before reconnection');
  a._testLook(0,0);const crashOrigin=a.entity.position.clone(),crashBegin=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(crashBegin).includes('Titan phase: DIVE'),'nape dive before recovery test',20000);
  const saveBegin=output.length;consoleCommand('save-all flush');
  await waitFor(()=>output.slice(saveBegin).includes('Saved the game'),'persist simulated interrupted giant state',20000);
  const crashSaved=await readSaved('MotionTesterA');
  assert.equal(crashSaved.data.BukkitValues['motionrp:titan_scale_v1'],1.25);
  assert.ok(crashSaved.data.BukkitValues['motionrp:titan_return_v1']);
  const killed=new Promise(resolve=>server.once('exit',resolve));server.kill('SIGKILL');await killed;server=undefined;
  await disconnect(a);await disconnect(b);await startServer();
  a=await connect('MotionTesterA');b=await connect('MotionTesterB');
  await waitFor(noTitanJournal,'join recovers interrupted Titan state');
  const recovery=await readSaved('MotionTesterA');
  const attr=(recovery.data.attributes||recovery.data.Attributes||[]).find(x=>String(x.id||x.Name).includes('scale'));
  assert.ok(attr && Math.abs((attr.base??attr.Base)-1.25)<.001,'Interrupted scale did not recover');
  assert.ok(a.entity.position.distanceTo(crashOrigin)<.01,'Restart left the player in the air');
  pass('a restart interrupted during the nape dive recovers original scale and returns the player to safe ground');
  assert.ok(!output.includes('Titan scene failed'),'Titan scene logged a runtime failure');

  // Persist a scale-ten pilot seat, kill the disposable server, then verify recovery.
  await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');
  a.setControlState('sneak',true);await sleep(300);
  for(let count=0;(await readSaved('MotionTesterA')).loadout[6]!==3 && count<18;count++) {a._testLeftClick();await sleep(300);}
  a.setControlState('sneak',false);await sleep(300);assert.equal((await readSaved('MotionTesterA')).loadout[6],3);
  const pilotOrigin=a.entity.position.clone(),pilotBegin=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(pilotBegin).includes('pilot seated scale=10.0'),'pilot seat before recovery test',20000);
  assert.ok(a.entity.position.y>pilotOrigin.y+50);
  const pilotSave=output.length;consoleCommand('save-all flush');await waitFor(()=>output.slice(pilotSave).includes('Saved the game'),'persist aerial seat');
  assert.ok((await readSaved('MotionTesterA')).data.BukkitValues['motionrp:aerial_return_v1']);
  const pilotKilled=new Promise(resolve=>server.once('exit',resolve));server.kill('SIGKILL');await pilotKilled;server=undefined;
  await disconnect(a);await disconnect(b);await startServer();a=await connect('MotionTesterA');b=await connect('MotionTesterB');
  await waitFor(async()=>!(await readSaved('MotionTesterA')).data.BukkitValues['motionrp:aerial_return_v1'],'aerial recovery journal cleared');
  assert.ok(a.entity.position.distanceTo(pilotOrigin)<.01,'Restart left the pilot at the neck');
  const pilotRecovered=(await readSaved('MotionTesterA')).data;assert.ok(!pilotRecovered.abilities?.mayfly && !pilotRecovered.abilities?.flying);
  pass('interrupted scale-ten Susanoo pilot seat restores the original ground position and flight abilities after restart');
  return {a,b};
}

async function checkMaskPack(){
  await waitFor(()=>{try{execFileSync('python3',['-c',"import zipfile,json,sys;z=zipfile.ZipFile(sys.argv[1]);items=[json.loads(z.read(n)) for n in z.namelist() if '/items/'in n and 'motionrp_hollow_mask_'in n];assert items and all('mask_'in json.dumps(i) and 'motionrp:energy/mask_'in json.dumps(i) for i in items)",path.join(run,'plugins/BetterModel/build.zip')],{stdio:'pipe'});return true;}catch{return false;}},'single-sided mask pack preparation',20000);
  execFileSync('python3',['-c',`import zipfile,json,sys,collections
z=zipfile.ZipFile(sys.argv[1]);c=collections.Counter()
def walk(d):
 if isinstance(d,list):
  for x in d:walk(x)
 elif isinstance(d,dict):
  ref=d.get('model')
  if isinstance(ref,str) and ref.startswith('motionrp:energy/'):
   model=json.loads(z.read('assets/motionrp/models/'+ref.split(':')[1]+'.json'))
   t=d.get('transformation',[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]);det=t[0]*t[5]-t[1]*t[4];side='south' if det<0 else 'north'
   for element in model['elements']:assert set(element['faces'])=={side},'Mask must render only the exterior winding'
  for tint in d.get('tints',[]):c[tint['value']&0xffffff]+=1
  for v in d.values():walk(v)
for n in z.namelist():
 if '/items/' in n and 'motionrp_hollow_mask_' in n:walk(json.loads(z.read(n)))
white=sum(n for rgb,n in c.items() if min(rgb>>16&255,rgb>>8&255,rgb&255)>180)
red=sum(n for rgb,n in c.items() if rgb>>16&255>130 and rgb>>8&255<50)
dark=sum(n for rgb,n in c.items() if max(rgb>>16&255,rgb>>8&255,rgb&255)<50)
assert white>red>0 and white>dark*3,(white,red,dark,c)
assert c[0]==0,c
`,path.join(run,'plugins/BetterModel/build.zip')]);
  pass('compiled mask keeps a predominantly white face, red markings and limited black eye details');
}

if(process.env.MOTIONRP_PAIR_ITEMS_ONLY==='1'){
 let code=0;
 try{
  fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
  log=fs.createWriteStream(path.join(project,'build/pair-items-smoke.log'));await startServer();
  const suffix=String(Date.now()).slice(-6),an='PairA'+suffix,bn='PairB'+suffix,cn='PairC'+suffix;
  const a=await connect(an),b=await connect(bn),c=await connect(cn);
  consoleCommand('fill -5 -60 -3 12 -54 45 air');consoleCommand('fill -5 -61 -3 12 -61 45 grass_block');
  for(const [name,x]of [[an,.5],[bn,3.5],[cn,7.5]])consoleCommand(`tp ${name} ${x} -60.0 0.5 0 0`);await sleep(600);
  const bases=[a.entity.position.clone(),b.entity.position.clone()];
  for(const name of [an,bn])consoleCommand('motionrp give controller '+name);
  consoleCommand('motionrp give motion fusion 64 '+an);await waitFor(()=>tokens(a)===64&&b.inventory.items().some(i=>i.name==='carrot_on_a_stick'),'paired item distribution');
  await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');await b.equip(b.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');await sleep(300);
  const right=bot=>{bot._testLookUp();bot._client.write('use_item',{hand:'main_hand',sequence:1,rotation:{x:0,y:-60}});};
  right(a);await waitFor(()=>a.currentWindow,'controller opens menu');await deposit(a);assert.equal(registered(a),1);assert.equal(tokens(a),63);await closeMenu(a);pass('Paired paper tokens stack to 64 and register in the existing controller GUI');
  const clicks=[];b.on('message',m=>{const raw=JSON.stringify(m.json);for(const hit of raw.matchAll(/\/motionrp accept [0-9a-f-]{36}/g))clicks.push(hit[0]);});
  async function inviteItem(){const n=b._testMessages.length,k=clicks.length;a._testLeftClick();await waitFor(()=>b._testMessages.slice(n).some(m=>m.includes('fusion accept '+an)),'nearest item invitation');await waitFor(()=>clicks.length>k,'clickable accept UUID');return clicks.at(-1);}
  async function cancel(){b.chat('/motionrp fusion cancel');await sleep(650);assert.ok(a.entity.position.distanceTo(bases[0])<.02);assert.ok(b.entity.position.distanceTo(bases[1])<.02);}
  async function began(begin){await waitFor(()=>output.slice(begin).includes('FusionLeft phase:'),'pair begins');assert.equal(b.currentWindow,null);}
  let token=await inviteItem();await sleep(300);assert.ok(a.entity.position.distanceTo(bases[0])<.02&&b.entity.position.distanceTo(bases[1])<.02);assert.ok(!c._testMessages.some(m=>m.includes('fusion accept '+an)));pass('Left-click invites only the closest player and does not move either player before consent');
  let begin=output.length;b.chat(token);await began(begin);await cancel();pass('The unique clickable chat command accepts and cancellation restores both players');
  const old=token;b.setQuickBarSlot(8);await sleep(300);token=await inviteItem();b.chat(old);await sleep(400);assert.ok(b._testMessages.at(-1).includes('無効'));assert.ok(b.entity.position.distanceTo(bases[1])<.02);await b.equip(b.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');await sleep(300);begin=output.length;right(b);await began(begin);await sleep(300);right(b);await sleep(300);assert.equal(b.currentWindow,null);assert.ok(!b._testMessages.at(-1).includes('中止'));await cancel();pass('Stale chat buttons cannot accept a new invite; controller right-click accepts without opening a GUI, including repeated clicks');
  await inviteItem();begin=output.length;b.chat('/motionrp accept');await began(begin);await cancel();pass('The plain accept command accepts the current incoming invitation');
  a.chat('/motionrp broly');await waitFor(()=>b._testMessages.some(m=>m.includes('broly accept '+an)),'nearest Broly command invitation');begin=output.length;b.chat('/motionrp accept');await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: FLEE'),'Broly accept');b.chat('/motionrp broly cancel');await sleep(650);pass('No-argument Broly command invites nearest and shares the same acceptance flow');
  consoleCommand('motionrp give motion broly 1 '+cn);consoleCommand('motionrp give controller '+cn);await waitFor(()=>tokens(c)===1,'Broly token');await c.equip(c.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');await menu(c);await deposit(c);await closeMenu(c);const msg=b._testMessages.length;c._testLeftClick();await waitFor(()=>b._testMessages.slice(msg).some(m=>m.includes('broly accept '+cn)),'Broly item invites nearest');begin=output.length;b.chat('/motionrp accept');await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: FLEE'),'Broly item begins');b.chat('/motionrp broly cancel');await sleep(650);pass('The Broly token also registers, invites the nearest player and starts both roles after consent');
  right(b);await waitFor(()=>b.currentWindow,'no invite opens GUI');await closeMenu(b);pass('Controller right-click still opens registration when no invitation exists');
  assert.ok(!/scene failed|Motion playback failed/.test(output));for(const bot of [a,b,c])await disconnect(bot);await stopServer();
 }catch(e){code=1;console.error(e.stack);}
 finally{for(const bot of bots)try{bot.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/pair-items-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
 process.exit(code);
}

if(process.env.MOTIONRP_BROLY_ONLY==='1'){
 let code=0;
 try{
  fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
  for(const dir of ['players','models'])for(const file of fs.readdirSync(path.join(project,'src/main/resources/bettermodel',dir)))if(file.endsWith('.bbmodel'))fs.copyFileSync(path.join(project,'src/main/resources/bettermodel',dir,file),path.join(run,'plugins/BetterModel',dir,file));
  log=fs.createWriteStream(path.join(project,'build/broly-smoke.log'));await startServer();
  const suffix=String(Date.now()).slice(-6),an='BrolyA'+suffix,bn='BrolyB'+suffix,cn='BrolyC'+suffix,dn='BrolyD'+suffix;
  const a=await connect(an);let b=await connect(bn);const c=await connect(cn),d=await connect(dn);d.chat('/motionrp view off');
  consoleCommand('fill -5 -60 -2 10 -54 45 air');consoleCommand('fill -5 -61 -2 10 -61 45 grass_block');
  for(const [name,x,z]of [[an,.5,.5],[bn,.5,4.5],[cn,7.5,.5],[dn,9.5,.5]])consoleCommand(`tp ${name} ${x} -60 ${z} 0 0`);await sleep(600);consoleCommand('save-all flush');await sleep(500);
  const bases=[a.entity.position.clone(),b.entity.position.clone()],health=[a.health,b.health],spawned=[new Set(),new Set(),new Set(),new Set()],removed=[new Set(),new Set(),new Set(),new Set()],items=[new Map(),new Map(),new Map(),new Map()],particles=[0,0,0,0],wallScales=[new Map(),new Map(),new Map(),new Map()];
  function watch(bot,i){const ix=bot.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack'),sx=bot.registry.entitiesByName.item_display.metadataKeys.indexOf('scale');bot._client.on('spawn_entity',p=>{if(p.type===bot.registry.entitiesByName.item_display.id)spawned[i].add(p.entityId);});bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[i].add(id)));bot._client.on('entity_metadata',p=>{for(const e of p.metadata){if(e.key===ix)items[i].set(p.entityId,e.value);if(e.key===sx){if(!wallScales[i].has(p.entityId))wallScales[i].set(p.entityId,[]);wallScales[i].get(p.entityId).push(e.value);}}});bot._client.on('world_particles',()=>particles[i]++);}
  [a,b,c,d].forEach(watch);const clear=()=>spawned.forEach(s=>s.clear());const cleanup=(offline=-1)=>spawned.every((set,i)=>i===offline||[...set].every(id=>removed[i].has(id)));
  async function restored(){assert.ok(a.entity.position.distanceTo(bases[0])<.02);assert.ok(b.entity.position.distanceTo(bases[1])<.02);for(const n of [an,bn])assert.ok(!(await readPlayerData(n)).BukkitValues?.['motionrp:aerial_return_v1']);}
  async function start(){clear();a.chat('/motionrp broly '+bn);await waitFor(()=>b._testMessages.some(m=>m.includes('broly accept '+an)),'Broly invitation');await sleep(200);const begin=output.length;b.chat('/motionrp broly accept '+an);await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: FLEE')||output.slice(begin).includes('BrolyAttacker phase: FINISHED'),'Broly begins');assert.ok(!output.slice(begin).includes('BrolyAttacker phase: FINISHED'),'Broly ended on start: '+JSON.stringify(a._testMessages.slice(-5)));return begin;}
  b.chat('/motionrp broly accept '+an);await sleep(300);await restored();pass('Broly requires an invitation and acceptance before either player moves');
  a.chat('/motionrp broly '+bn);await sleep(250);b.chat('/motionrp fusion accept '+an);await sleep(250);await restored();b.chat('/motionrp broly cancel');await sleep(250);pass('Fusion acceptance cannot consume a Broly invitation');
  let begin=await start(),off=particles[3];await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: TURN'),'recipient completes the escape',12000);assert.ok(Math.abs(b.entity.position.z-(bases[1].z+4))<.12,'Victim escapes four blocks from the acceptance point');assert.ok(Math.hypot(a.entity.position.x-bases[0].x,a.entity.position.z-bases[0].z)<.02,'Attacker waits horizontally while the recipient escapes and turns');assert.ok(a.entity.position.y>bases[0].y+1&&b.entity.position.y>bases[1].y+1,'Both roles visibly hover above their original ground');await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: CHARGE'),'attacker charges only after turn',10000);pass('Recipient escapes from the actual acceptance point before turning; attacker waits and then charges');await waitFor(()=>output.slice(begin).includes('rock impact cause=DISTANCE'),'distance fallback impact',15000);assert.match(output.slice(begin),/cause=DISTANCE distance=24(?:\.0)? /);await sleep(550);assert.ok(b.entity.position.y>bases[1].y+3,'Impact centre stays above ground so the round recess is fully visible');assert.ok(b.entity.position.z>bases[1].z+30.2,'Victim is pressed into the cosmetic recess');await waitFor(cleanup,'normal Broly cleanup',15000);await restored();
  const entries=[...output.slice(begin).matchAll(/BrolyAttacker displays (\w+) ids=([0-9,]+)/g)].map(m=>({kind:m[1],ids:m[2].split(',').map(Number)}));for(const kind of ['fallback_rock_wall','dented_rock_wall','impact_crater','rock_fragment0','rock_fragment13']){const entry=entries.find(e=>e.kind===kind);assert.ok(entry,kind);for(const i of [0,1,2])assert.ok(entry.ids.some(id=>(items[i].get(id)?.itemCount||0)>0),kind+' reaches all ON clients');}assert.equal(spawned[3].size,0);assert.equal(particles[3],off);assert.deepEqual([a.health,b.health],health);pass('Open flight reaches exactly 24 blocks, hits cosmetic rock, renders debris to ON viewers, restores both players and leaves OFF viewers untouched');
  const dent=entries.find(e=>e.kind==='dented_rock_wall');for(const i of [0,1,2])assert.ok(dent.ids.some(id=>{const samples=wallScales[i].get(id)||[];return samples.some(v=>v.z<v.x*.01)&&samples.some(v=>v.z>v.x*.9);}), 'Grey wall changes its actual depth on every ON client');pass('The large grey rock face visibly deforms in depth rather than merely adding a particle ring');
  const originalVictim=bases[1].clone();consoleCommand(`tp ${bn} 4.5 -60.0 0.5 0 0`);await waitFor(()=>Math.abs(b.entity.position.x-4.5)<.02,'side-by-side recipient');bases[1]=b.entity.position.clone();begin=await start();await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: TURN'),'sideways escape');assert.ok(Math.abs(b.entity.position.x-8.5)<.12&&Math.abs(b.entity.position.z-.5)<.02);b.chat('/motionrp broly cancel');await sleep(400);await waitFor(cleanup,'sideways escape cleanup');await restored();consoleCommand(`tp ${bn} ${originalVictim.x} ${originalVictim.y} ${originalVictim.z}`);await waitFor(()=>b.entity.position.distanceTo(originalVictim)<.02,'restore original test position');bases[1]=originalVictim;pass('Side-by-side acceptance also escapes from the actual recipient position and normalizes turn direction');
  consoleCommand('fill -3 -60 16 3 -55 16 stone');await sleep(300);begin=await start();let farthest=-100;const sample=setInterval(()=>{farthest=Math.max(farthest,b.entity.position.z);},10);await waitFor(()=>output.slice(begin).includes('rock impact cause=BLOCK'),'real stone wall impact',12000);await waitFor(cleanup,'real block cleanup',15000);clearInterval(sample);await restored();assert.ok(farthest<15.71,'Victim must stay outside the wall, observed '+farthest);assert.ok(!output.slice(begin).includes('cause=DISTANCE'));const query=output.length;consoleCommand('execute if block 0 -59 16 stone run say BROLY_WALL_INTACT');await waitFor(()=>output.slice(query).includes('BROLY_WALL_INTACT'),'terrain remains intact');pass('Swept flight stops before a real stone wall, does not tunnel or damage terrain, and restores both roles');
  consoleCommand('fill -3 -60 16 3 -55 16 air');consoleCommand('setblock 0 -59 16 oak_fence');await sleep(250);begin=await start();await waitFor(()=>output.slice(begin).includes('rock impact cause=BLOCK'),'thin fence collision',12000);await waitFor(cleanup,'fence cleanup',15000);await restored();pass('Swept collision also catches a thin fence between flight endpoints');consoleCommand('setblock 0 -59 16 air');
  consoleCommand('fill -3 -60 24 3 -55 24 stone');await sleep(250);begin=await start();await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: FLIGHT'),'wall removal during flight');consoleCommand('fill -3 -60 24 3 -55 24 air');await waitFor(()=>output.slice(begin).includes('rock impact cause=DISTANCE'),'removed wall falls back to max distance',12000);assert.ok(output.slice(begin).includes('displays dented_rock_wall'));await waitFor(cleanup,'dynamic fallback cleanup',15000);await restored();pass('A wall removed during flight still produces a cosmetic wall at the distance limit');
  consoleCommand('fill -3 -60 6 3 -55 6 stone');await sleep(250);begin=await start();await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: TURN'),'escape stops at nearby wall');assert.ok(b.entity.position.z<5.71&&b.entity.position.z>bases[1].z,'Escape must stop outside nearby terrain');await waitFor(()=>output.slice(begin).includes('rock impact cause=BLOCK'),'nearby-wall impact',12000);await waitFor(cleanup,'nearby wall cleanup',15000);await restored();consoleCommand('fill -3 -60 6 3 -55 6 air');pass('Escape itself stops before nearby terrain and still turns before the charge');
  for(const phase of ['FLEE','TURN','CHARGE','GRAB','FLIGHT','IMPACT']){begin=await start();await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: '+phase),'Broly '+phase,15000);b.chat('/motionrp broly cancel');await waitFor(cleanup,'Broly cancel '+phase);await sleep(350);await restored();pass('Cancelling either role during '+phase+' restores both players and every display');}
  for(const end of ['teleport','quit']){begin=await start();await waitFor(()=>output.slice(begin).includes('BrolyAttacker phase: GRAB'),'Broly '+end,10000);if(end==='teleport')consoleCommand(`tp ${bn} 6.0 -60.0 0.5`);else await disconnect(b);await waitFor(()=>cleanup(end==='quit'?1:-1),'Broly '+end+' cleanup');assert.ok(a.entity.position.distanceTo(bases[0])<.02);if(end==='teleport'){assert.ok(Math.abs(b.entity.position.x-6)<.02);consoleCommand(`tp ${bn} ${bases[1].x} ${bases[1].y} ${bases[1].z}`);await sleep(300);}else{b=await connect(bn);watch(b,1);await waitFor(()=>b.entity.position.distanceTo(bases[1])<.02,'Broly reconnect recovery');}await restored();pass('Broly '+end+' restores the partner while respecting external teleport and reconnect recovery');}
  // Existing pair command remains operational, while the postponed battle stays unavailable.
  clear();a.chat('/motionrp finalbattle '+bn);await sleep(250);await restored();assert.ok(!b._testMessages.slice(-2).some(m=>m.includes('finalbattle accept')));a.chat('/motionrp fusion '+bn);await sleep(250);begin=output.length;b.chat('/motionrp fusion accept '+an);await waitFor(()=>output.slice(begin).includes('FusionLeft phase:'),'Fusion still starts');b.chat('/motionrp fusion cancel');await waitFor(cleanup,'Fusion regression cleanup');await sleep(300);await restored();pass('Fusion still works and the postponed final-battle command remains unavailable');
  assert.ok(!/scene failed|Motion playback failed/.test(output));for(const bot of [a,b,c,d])await disconnect(bot);await stopServer();
 }catch(e){code=1;console.error(e.stack);}
 finally{for(const bot of bots)try{bot.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/broly-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
 process.exit(code);
}

if(process.env.MOTIONRP_FINAL_BATTLE_ONLY==='1'){
 let code=0;
 try{
  fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
  for(const dir of ['players','models'])for(const file of fs.readdirSync(path.join(project,'src/main/resources/bettermodel',dir)))if(file.endsWith('.bbmodel'))fs.copyFileSync(path.join(project,'src/main/resources/bettermodel',dir,file),path.join(run,'plugins/BetterModel',dir,file));
  log=fs.createWriteStream(path.join(project,'build/final-battle-smoke.log'));await startServer();
  const suffix=String(Date.now()).slice(-6),an='DuelA'+suffix,bn='DuelB'+suffix,cn='DuelC'+suffix,dn='DuelD'+suffix;
  const a=await connect(an);let b=await connect(bn);const c=await connect(cn),d=await connect(dn);c.chat('/motionrp view off');consoleCommand(`tp ${an} 0 -60 0 0 0`);consoleCommand(`tp ${bn} 5 -60 0 0 0`);consoleCommand(`tp ${cn} 8 -60 0 0 0`);consoleCommand(`tp ${dn} 12 -60 0 0 0`);await sleep(500);
  consoleCommand('save-all flush');await sleep(600);const bases=[a.entity.position.clone(),b.entity.position.clone()],health=[a.health,b.health];
  const spawned=[new Set(),new Set(),new Set(),new Set()],removed=[new Set(),new Set(),new Set(),new Set()],items=[new Map(),new Map(),new Map(),new Map()],particles=[0,0,0,0],maxScales=[new Map(),new Map(),new Map(),new Map()];
  function watch(bot,i){const ix=bot.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack'),sx=bot.registry.entitiesByName.item_display.metadataKeys.indexOf('scale');bot._client.on('spawn_entity',p=>{if(p.type===bot.registry.entitiesByName.item_display.id){spawned[i].add(p.entityId);removed[i].delete(p.entityId);}});bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[i].add(id)));bot._client.on('entity_metadata',p=>{for(const m of p.metadata){if(m.key===ix)items[i].set(p.entityId,m.value);if(m.key===sx)maxScales[i].set(p.entityId,Math.max(maxScales[i].get(p.entityId)||0,m.value.x));}});bot._client.on('world_particles',()=>particles[i]++);}
  [a,b,c,d].forEach(watch);const clear=()=>spawned.forEach(s=>s.clear());const cleanup=(offline=-1)=>spawned.every((set,i)=>i===offline||[...set].every(id=>removed[i].has(id)));
  async function restored(){assert.ok(a.entity.position.distanceTo(bases[0])<.02,'Naruto returns to original position');assert.ok(b.entity.position.distanceTo(bases[1])<.02,'Sasuke returns to original position');for(const name of [an,bn])assert.ok(!(await readPlayerData(name)).BukkitValues?.['motionrp:aerial_return_v1'],'recovery journal cleared');}
  b.chat('/motionrp finalbattle accept '+an);await sleep(300);await restored();pass('Final battle requires an invitation and consent before either participant moves');
  a.chat('/motionrp finalbattle '+bn);await waitFor(()=>b._testMessages.some(m=>m.includes('finalbattle accept '+an)),'final battle invitation');b.chat('/motionrp fusion accept '+an);await sleep(300);await restored();pass('Fusion acceptance cannot consume a final battle invitation');
  let begin=output.length,off=particles[2];b.chat('/motionrp finalbattle accept '+an);await waitFor(()=>output.slice(begin).includes('FinalBattleSasuke phase: AVATARS'),'both avatars start',20000);assert.ok(a.entity.position.distanceTo(bases[0])>40&&b.entity.position.distanceTo(bases[1])>40);
  await waitFor(()=>output.slice(begin).includes('FinalBattleNaruto phase: CHARGE'),'two giant techniques charge',35000);assert.ok(a.entity.position.y>bases[0].y+40&&b.entity.position.y>bases[1].y+40);pass('Both consenting participants occupy their airborne avatars');
  await waitFor(()=>output.slice(begin).includes('FinalBattleNaruto phase: RETURN'),'full cinematic reaches return',40000);await waitFor(cleanup,'full final battle cleanup',15000);await restored();
  const sequence=output.slice(begin);let last=-1;for(const marker of ['both avatars brace and gather chakra','three-headed six-armed Kurama and Indra Susanoo formed','Six Paths Ultra-Big Ball Rasenshuriken and Indra arrow charging','techniques form above raised palms','Kurama throws overhead','both Rasenshuriken and Indra arrow released together','Indra arrow and twin Rasenshuriken collide']){const next=sequence.indexOf(marker);assert.ok(next>last,'Readable preparation, formation, throw and impact order: '+marker);last=next;}assert.ok(!sequence.includes('trade giant blows'));pass('Chakra gathering precedes transformation, held techniques, throwing and bow release; impact only follows flight');
  const entries=[...output.slice(begin).matchAll(/FinalBattleNaruto displays (\w+) ids=([0-9,]+)/g)].map(m=>({kind:m[1],ids:m[2].split(',').map(Number)}));
  for(const kind of ['kurama','sasuke_susanoo','kurama_clone0','kurama_clone1','bijuu_chakra0','bijuu_chakra8','asura_kurama','indra_susanoo','gold_rasenshuriken','dark_rasenshuriken','indra_arrow','chakra_collision','chakra_storm']){const entry=entries.find(e=>e.kind===kind);assert.ok(entry,kind+' has display IDs');assert.ok(items.filter((_,i)=>i!==2).every(map=>entry.ids.some(id=>(map.get(id)?.itemCount||0)>0)),kind+' has rendered item data for participants and ON spectator');}
  const blast=entries.find(e=>e.kind==='chakra_collision');for(const i of [0,1,3])assert.ok(blast.ids.some(id=>(maxScales[i].get(id)||0)>90),'Three-times larger collision sphere reaches every ON client; observed maximum '+Math.max(...blast.ids.map(id=>maxScales[i].get(id)||0)));pass('Enlarged collision sphere reaches a scale over 90 on both participants and the ON spectator');
  assert.equal(spawned[2].size,0);assert.equal(particles[2],off);assert.deepEqual([a.health,b.health],health);pass('Nine-tailed and Asura Kurama, both Susanoo forms, twin shuriken, lightning arrow, collision and ground-reaching storm reach both participants and an ON spectator; OFF receives none');
  for(const phase of ['READY','TRANSFORM','RELEASE','COLLISION']){
   clear();a.chat('/motionrp finalbattle '+bn);await sleep(250);begin=output.length;b.chat('/motionrp finalbattle accept '+an);await waitFor(()=>output.slice(begin).includes('FinalBattleNaruto phase: '+phase),'final battle '+phase,40000);if(phase==='COLLISION')await waitFor(()=>output.slice(begin).includes('collision expands into a ground-reaching spiralling chakra storm'),'storm exists before cancellation');b.chat('/motionrp finalbattle cancel');await waitFor(cleanup,'cancel final battle '+phase);await sleep(600);await restored();if(phase!=='COLLISION')assert.ok(!output.slice(begin).includes('Indra arrow and twin Rasenshuriken collide'));pass('Cancelling either role during '+phase+' removes both roles and every owned model');
  }
  for(const end of ['teleport','quit']){
   clear();a.chat('/motionrp finalbattle '+bn);await sleep(250);begin=output.length;b.chat('/motionrp finalbattle accept '+an);await waitFor(()=>output.slice(begin).includes('FinalBattleNaruto phase: AVATARS'),'final battle '+end,20000);
   if(end==='teleport')consoleCommand(`tp ${bn} 6.0 -60.0 0.0`);else await disconnect(b);
   await waitFor(()=>cleanup(end==='quit'?1:-1),'final battle '+end+' cleanup');assert.ok(a.entity.position.distanceTo(bases[0])<.02);assert.ok(!(await readPlayerData(an)).BukkitValues?.['motionrp:aerial_return_v1']);
   if(end==='teleport'){assert.ok(Math.abs(b.entity.position.x-6)<.02);consoleCommand(`tp ${bn} ${bases[1].x} ${bases[1].y} ${bases[1].z}`);await sleep(300);}else{b=await connect(bn);watch(b,1);await waitFor(()=>b.entity.position.distanceTo(bases[1])<.02,'Sasuke reconnect recovery');}
   await restored();pass('Final battle '+end+' cancels both roles while preserving explicit teleport destinations and recovery');
  }
  assert.ok(!/scene failed|Motion playback failed/.test(output));await disconnect(a);await disconnect(b);await disconnect(c);await disconnect(d);await stopServer();
 }catch(e){code=1;console.error(e.stack);}
 finally{for(const bot of bots)try{bot.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/final-battle-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
 process.exit(code);
}

if(process.env.MOTIONRP_EPIC_ONLY==='1'){
 let code=0;
 try{
  fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
  for(const id of ['road_roller','kokuryuha','kokujo_tengen_myoo','chibaku_tensei','fusion_left','fusion_right'])fs.copyFileSync(path.join(project,'src/main/resources/bettermodel/players/motionrp_'+id+'.bbmodel'),path.join(run,'plugins/BetterModel/players/motionrp_'+id+'.bbmodel'));
  for(const id of ['the_world','the_world_upper','world_arm_afterimage','road_roller','black_dragon_head','black_dragon_body','tengen_myoo','gravity_core','gravity_rock','black_dragon_cloak','rock_moon','fusion_warrior','roller_debris'])fs.copyFileSync(path.join(project,'src/main/resources/bettermodel/models/motionrp_'+id+'.bbmodel'),path.join(run,'plugins/BetterModel/models/motionrp_'+id+'.bbmodel'));
  for(const file of fs.readdirSync(path.join(project,'src/test/resources/bundled-0.27.0'))){const target=file.replace(/^(models|players)_/,'$1/').replace(/\.gz$/,'');fs.writeFileSync(path.join(run,'plugins/BetterModel',target),gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/bundled-0.27.0',file))));}
  const old=gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/bundled-0.20.0/models_motionrp_susanoo_armor.bbmodel.gz')));fs.writeFileSync(path.join(run,'plugins/BetterModel/models/motionrp_susanoo_armor.bbmodel'),old);
  log=fs.createWriteStream(path.join(project,'build/epic-smoke.log'));await startServer();await checkMaskPack();
  for(const file of fs.readdirSync(path.join(project,"src/test/resources/bundled-0.27.0"))){const target=file.replace(/^(models|players)_/,"$1/").replace(/\.gz$/,"");assert.deepEqual(fs.readFileSync(path.join(run,"plugins/BetterModel",target)),fs.readFileSync(path.join(project,"src/main/resources/bettermodel",target)));}pass("0.27 giant and player assets migrate to the revised choreography");
  const suffix=String(Date.now()).slice(-6),an='EpicA'+suffix,bn='EpicB'+suffix,cn='EpicC'+suffix;
  const a=await connect(an);let b=await connect(bn);const c=await connect(cn);c.chat('/motionrp view off');
  consoleCommand(`tp ${an} 0 -60 0 0 0`);consoleCommand(`tp ${bn} 5 -60 0 0 0`);consoleCommand(`tp ${cn} 8 -60 0 0 0`);await sleep(500);
  const spawned=[new Set(),new Set(),new Set()],removed=[new Set(),new Set(),new Set()],particles=[0,0,0],items=[new Map(),new Map(),new Map()];
  for(const [i,bot]of[a,b,c].entries()){
   bot._client.on('spawn_entity',p=>{if(p.type===bot.registry.entitiesByName.item_display.id){spawned[i].add(p.entityId);removed[i].delete(p.entityId);}});
   bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[i].add(id)));
   bot._client.on('world_particles',()=>particles[i]++);
   const itemIndex=bot.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack');
   bot._client.on('entity_metadata',p=>{for(const m of p.metadata)if(m.key===itemIndex)items[i].set(p.entityId,m.value);});
  }
  const cleanup=(offline=-1)=>spawned.every((set,i)=>i===offline||[...set].every(id=>removed[i].has(id)));
  const clear=()=>{for(const s of spawned)s.clear();};
  consoleCommand(`motionrp give controller ${an}`);
  const ids=['road_roller','kokuryuha','kokujo_tengen_myoo','chibaku_tensei'];
  for(const [i,id]of ids.entries()){
   consoleCommand(`clear ${an} minecraft:paper`);await sleep(200);consoleCommand(`motionrp give motion ${id} 64 ${an}`);await waitFor(()=>tokens(a)===64,id+' token stack');await menu(a);assert.equal(a.currentWindow.inventoryStart,27);await deposit(a);assert.equal(registered(a),i+1);assert.equal(tokens(a),63);await closeMenu(a);
  }
  pass('all four new motions stack to 64 and register in the expanded 27-slot GUI');
  await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');const health=[a.health,b.health],anchor=a.entity.position.clone();
  const essential={road_roller:['the_world','the_world_upper','road_roller','arm_afterimage0','arm_afterimage11'],kokuryuha:['dragon_head','dragon_segment17','black_flame_cloak'],kokujo_tengen_myoo:['tengen_myoo'],chibaku_tensei:['gravity_core','attracted_rock0','attracted_rock71','attracted_rock143']};
  for(const [i,id]of ids.entries()){
   if(i<Number(process.env.MOTIONRP_EPIC_START_AT||0)){a.setControlState('sneak',true);await sleep(250);a._testLeftClick();await sleep(250);a.setControlState('sneak',false);await sleep(250);continue;}
   clear();const begin=output.length,off=particles[2];a._testLeftClick();if(id==='kokujo_tengen_myoo'){await waitFor(()=>output.slice(begin).includes(id+' phase: WINDUP'),'following giant windup');const moved=a.entity.position.clone();moved.x+=1;a._testStep(1,0,0);Object.assign(a.entity.position,moved);await sleep(250);const query=output.length;consoleCommand('data get entity '+an+' Pos');await waitFor(()=>output.slice(query).replace(/\u001b\[[0-9;]*m/g,'').includes('['+moved.x+'d, '+moved.y.toFixed(1)+'d, '+moved.z+'d]'),'Myoo allows walking while the giant follows');a._testStep(-1,0,0);Object.assign(a.entity.position,anchor);}if(id==='chibaku_tensei'){await waitFor(()=>output.slice(begin).includes(id+' phase: MOON'),'all rocks have accumulated',30000);const rocks=[...output.slice(begin).matchAll(/chibaku_tensei displays attracted_rock\d+ ids=([0-9,]+)/g)];assert.equal(rocks.length,144);for(const rock of rocks)for(const entityId of rock[1].split(',').map(Number))assert.ok(!removed[0].has(entityId)&&!removed[1].has(entityId),'arrived rock remains visible on the completed sphere');}await waitFor(()=>output.slice(begin).includes(id+' phase: FADE'),id+' completes',30000);await waitFor(cleanup,id+' cleanup');
   for(const kind of essential[id]){const match=output.slice(begin).match(new RegExp(id+' displays '+kind+' ids=([0-9,]+)'));assert.ok(match,id+' '+kind);const entityIds=match[1].split(',').map(Number);assert.ok(items.slice(0,2).every(map=>entityIds.some(e=>(map.get(e)?.itemCount||0)>0)),kind+' has rendered items for both ON viewers');}
   if(id==='road_roller'){assert.ok(output.slice(begin).includes('roller raised above both hands'));assert.ok(output.slice(begin).includes('initial World dissolves at apex'));assert.ok(output.slice(begin).includes('upper-body World follows player from behind'));assert.ok(output.slice(begin).includes('both fists raised; full-body finishing slam'));}
   if(id==='chibaku_tensei')assert.equal((output.slice(begin).match(/rock retained on shell index=/g)||[]).length,144);
   assert.ok(spawned[0].size>0&&spawned[1].size>0);assert.equal(spawned[2].size,0);assert.equal(particles[2],off);assert.deepEqual([a.health,b.health],health);assert.ok(a.entity.position.distanceTo(anchor)<.01);
   assert.ok(!(await readSaved(an)).data.BukkitValues?.['motionrp:aerial_return_v1']);pass(id+' renders for two ON viewers, remains hidden from OFF, finishes without damage and cleans up');
   clear();const cancel=output.length;a._testLeftClick();await waitFor(()=>id==='road_roller'?a.entity.position.y>anchor.y+5:output.slice(cancel).includes(id+' displays '),'cancel '+id+' during effect');a._testLeftClick();await waitFor(cleanup,id+' cancelled cleanup');await sleep(600);assert.ok(cleanup());assert.ok(a.entity.position.distanceTo(anchor)<.01);pass(id+' cancellation clears every model and restores any flight state');
   if(id==='kokuryuha'){for(const phase of ['RETURN','DEVOUR','CLOAK']){clear();const start=output.length;a._testLeftClick();await waitFor(()=>output.slice(start).includes('kokuryuha phase: '+phase),'black dragon '+phase,20000);a._testLeftClick();await waitFor(cleanup,'cancel black dragon '+phase);await sleep(600);assert.ok(cleanup());pass('black dragon '+phase+' cancellation removes all flame and dragon models');}}
   if(id==='road_roller'){clear();const after=output.length;a._testLeftClick();await waitFor(()=>output.slice(after).includes('displays arm_afterimage11'),'all arm ghosts start');a._testLeftClick();await waitFor(cleanup,'cancel rapid arm ghosts');await sleep(600);assert.ok(cleanup());assert.ok(!output.slice(after).includes('final road roller blast'));pass('cancelling during the arm barrage removes every afterimage and prevents the final blast');}
   if(i<ids.length-1){a.setControlState('sneak',true);await sleep(250);a._testLeftClick();await sleep(250);a.setControlState('sneak',false);await sleep(250);}
  }
  const aBase=a.entity.position.clone(),bBase=b.entity.position.clone();clear();
  b.chat('/motionrp fusion accept '+an);await sleep(400);assert.ok(a.entity.position.distanceTo(aBase)<.01&&b.entity.position.distanceTo(bBase)<.01);assert.ok(cleanup());pass('Fusion cannot start without a valid invitation');
  a.chat('/motionrp fusion '+bn);await waitFor(()=>b._testMessages.some(m=>m.includes('fusion accept '+an)),'partner invitation');assert.ok(a.entity.position.distanceTo(aBase)<.01&&b.entity.position.distanceTo(bBase)<.01);
  let begin=output.length;b.chat('/motionrp fusion accept '+an);await waitFor(()=>output.slice(begin).includes('FusionLeft phase: ARMS')&&output.slice(begin).includes('FusionRight phase: ARMS'),'both fusion roles start');
  await waitFor(()=>output.slice(begin).includes('temporary fused warrior revealed'),'fused warrior',15000);await waitFor(cleanup,'fusion full cleanup',12000);
  assert.ok(a.entity.position.distanceTo(aBase)<.01&&b.entity.position.distanceTo(bBase)<.01);assert.equal(spawned[2].size,0);assert.deepEqual([a.health,b.health],health);pass('consenting Fusion mirrors two actors, reveals a temporary warrior and restores both positions');
  for(const end of ['cancel','teleport','quit']){
   clear();a.chat('/motionrp fusion '+bn);await sleep(300);begin=output.length;b.chat('/motionrp fusion accept '+an);await waitFor(()=>output.slice(begin).includes('FusionRight phase: ARMS'),'fusion '+end+' starts');
   if(end==='cancel')b.chat('/motionrp fusion cancel');else if(end==='teleport')consoleCommand(`tp ${bn} 6.0 -60.0 0.0`);else await disconnect(b);
   await waitFor(()=>cleanup(end==='quit'?1:-1),'both roles stop on '+end);await sleep(600);assert.ok(!output.slice(begin).includes('temporary fused warrior'));assert.ok(a.entity.position.distanceTo(aBase)<.01);
   if(end==='teleport'){assert.ok(Math.abs(b.entity.position.x-6)<.01,JSON.stringify(b.entity.position));consoleCommand(`tp ${bn} ${bBase.x} ${bBase.y} ${bBase.z}`);await sleep(400);}
   if(end==='quit'){b=await connect(bn);await waitFor(()=>b.entity.position.distanceTo(bBase)<.01,'fusion reconnect recovery');}
   assert.ok(!(await readSaved(an)).data.BukkitValues?.['motionrp:aerial_return_v1']);assert.ok(!(await readPlayerData(bn)).BukkitValues?.['motionrp:aerial_return_v1']);
   pass('Fusion '+end+' stops both roles and clears their recovery journals');
  }
  assert.ok(!/scene failed|Motion playback failed/.test(output));await disconnect(a);await disconnect(b);await disconnect(c);await stopServer();
 }catch(e){code=1;console.error(e.stack);}
 finally{for(const bot of bots)try{bot.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/epic-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
 process.exit(code);
}

if(process.env.MOTIONRP_TREASURY_ONLY==='1'){
 let code=0;
 try{
  fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
  log=fs.createWriteStream(path.join(project,'build/treasury-smoke.log'));await startServer();
  const suffix=String(Date.now()).slice(-6),an='VaultA'+suffix,bn='VaultB'+suffix;
  const a=await connect(an),b=await connect(bn);consoleCommand(`tp ${an} 0 -60 0 0 0`);consoleCommand(`tp ${bn} 5 -60 0 0 0`);await sleep(400);
  const spawned=[new Set(),new Set()],removed=[new Set(),new Set()],sounds=[[],[]];
  for(const [i,bot]of[a,b].entries()){
   bot._client.on('spawn_entity',p=>{if(p.type===bot.registry.entitiesByName.item_display.id){spawned[i].add(p.entityId);removed[i].delete(p.entityId);}});
   bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[i].add(id)));
   bot._client.on('sound_effect',p=>{const h=p.sound;const name=h?.data?.soundName??bot.registry.sounds[h?h.soundId+1:p.soundId]?.name;if(name)sounds[i].push(name);});
  }
  const cleanup=()=>spawned.every((set,i)=>[...set].every(id=>removed[i].has(id)));
  consoleCommand(`motionrp give controller ${an}`);
  for(const id of ['gate_of_babylon_100','gate_of_babylon_enuma']){
   consoleCommand(`clear ${an} minecraft:paper`);await sleep(200);consoleCommand(`motionrp give motion ${id} 1 ${an}`);await waitFor(()=>tokens(a)===1,id+' token');await menu(a);await deposit(a);await closeMenu(a);
  }
  await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');const health=[a.health,b.health],anchor=a.entity.position.clone();
  for(const [prefix,count]of [['Treasury100',100],['TreasuryEnuma',200]]){
   const begin=output.length;for(const s of spawned)s.clear();for(const s of sounds)s.length=0;
   a._testLeftClick();await waitFor(()=>output.slice(begin).includes(prefix+' phase: FADE'),prefix+' completes',35000);await waitFor(cleanup,prefix+' cleanup');
   const text=output.slice(begin),fires=[...text.matchAll(new RegExp(prefix+' fire index=(\\d+) tick=(\\d+)','g'))].map(m=>[+m[1],+m[2]]);
   assert.deepEqual(fires,[...Array.from({length:count},(_,i)=>[i,42+Math.floor(i*15/count)]),...Array.from({length:count},(_,i)=>[i,57+Math.floor(i*15/count)]),...Array.from({length:count},(_,i)=>[i,84])]);
   assert.equal([...text.matchAll(new RegExp(prefix+' explosion index=','g'))].length,count*3);
   for(const list of sounds){assert.ok(list.filter(n=>n.includes('trident.throw')).length>=25,'rapid throw cues');assert.ok(list.filter(n=>n.includes('generic.explode')).length>=10,'audible impact cues');}
   assert.deepEqual([a.health,b.health],health);assert.ok(a.entity.position.distanceTo(anchor)<.01);
   pass(prefix+' fires '+(count*3)+' projectiles with two rapid passes, audible launch/impact cues for both viewers and full cleanup');
   if(count===100){
    const cancel=output.length;for(const s of spawned)s.clear();a._testLeftClick();await waitFor(()=>output.slice(cancel).includes(prefix+' fire index=0'),'rapid cancellation');a._testLeftClick();await waitFor(cleanup,'rapid cancellation cleanup');await sleep(1700);assert.ok(!output.slice(cancel).includes('volley blast'));pass('cancelling the denser rapid fire prevents the later passes and volley');
    a.setControlState('sneak',true);await sleep(250);a._testLeftClick();await sleep(250);a.setControlState('sneak',false);await sleep(250);
   }
  }
  assert.ok(!/Treasury100 scene failed|TreasuryEnuma scene failed/.test(output));await disconnect(a);await disconnect(b);await stopServer();
 }catch(e){code=1;console.error(e.stack);}
 finally{for(const bot of bots)try{bot.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/treasury-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
 process.exit(code);
}

if(process.env.MOTIONRP_BARRIER_ONLY==='1'){
  let code=0;
  try{
    fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
    const previous=[];
    for(const file of ['models/motionrp_hollow_mask.bbmodel','models/motionrp_rho_aias_shield.bbmodel','models/motionrp_getsuga_crescent.bbmodel','models/motionrp_getsuga_tensa.bbmodel','players/motionrp_rho_aias.bbmodel','players/motionrp_getsuga_tensho.bbmodel']){
      const latest=path.join(project,'src/test/resources/bundled-0.19.0',file.replace('/','_')+'.gz');const old=fs.existsSync(latest)?latest:path.join(project,'src/test/resources/bundled-0.17.0',file.replace('/','_')+'.gz');
      if(fs.existsSync(old)){const bytes=gunzipSync(fs.readFileSync(old));fs.writeFileSync(path.join(run,'plugins/BetterModel',file),bytes);previous.push([file,bytes]);}else fs.rmSync(path.join(run,'plugins/BetterModel',file),{force:true});
    }
    log=fs.createWriteStream(path.join(project,'build/barrier-smoke.log'));await startServer();await checkMaskPack();
    for(const [file,bytes]of previous){assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel',file)),fs.readFileSync(path.join(project,'src/main/resources/bettermodel',file)));const hash=createHash('sha256').update(bytes).digest('hex');assert.deepEqual(fs.readFileSync(path.join(run,'plugins/MotionRP/model-backups',path.basename(file)+'.'+hash+'.bak')),bytes);}pass('prior shield, mask, katana and two-handed player assets migrate with exact backups');
    const suffix=String(Date.now()).slice(-6),an='ArtsA'+suffix,bn='ArtsB'+suffix;
    const a=await connect(an),b=await connect(bn);consoleCommand(`tp ${an} 0 -60 0 0 0`);consoleCommand(`tp ${bn} 5 -60 0 0 0`);await sleep(400);
    const spawned=[new Set(),new Set()],removed=[new Set(),new Set()],items=[new Map(),new Map()],particles=[0,0];
    for(const [i,bot]of[a,b].entries()){
      const index=bot.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack');
      bot._client.on('spawn_entity',p=>{if([bot.registry.entitiesByName.item_display.id,bot.registry.entitiesByName.text_display.id].includes(p.type)){spawned[i].add(p.entityId);removed[i].delete(p.entityId);}});
      bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[i].add(id)));
      bot._client.on('entity_metadata',p=>{for(const m of p.metadata)if(m.key===index)items[i].set(p.entityId,m.value);});
      bot._client.on('world_particles',()=>particles[i]++);
    }
    const entries=(prefix,begin)=>[...output.slice(begin).matchAll(new RegExp(prefix+' displays (\\w+) ids=([0-9,]+)','g'))].map(m=>({kind:m[1],ids:m[2].split(',').map(Number)}));
    const visible=es=>items.every(map=>es.every(e=>e.ids.some(id=>(map.get(id)?.itemCount||0)>0)));
    const cleanup=()=>spawned.every((set,i)=>[...set].every(id=>removed[i].has(id)));
    consoleCommand(`motionrp give controller ${an}`);
    for(const [id,slot]of[['rho_aias',0],['getsuga_tensho',1]]){
      consoleCommand(`clear ${an} minecraft:paper`);await sleep(200);consoleCommand(`motionrp give motion ${id} 64 ${an}`);await waitFor(()=>tokens(a)===64,id+' stackable token');
      await menu(a);await deposit(a);assert.equal(registered(a),slot+1);assert.equal(tokens(a),63);await closeMenu(a);
    }
    pass('new definitions migrate, both tokens stack to 64 and physical GUI registration works');
    await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');const health=[a.health,b.health],anchor=a.entity.position.clone();
    let begin=output.length;a._testLeftClick();await waitFor(()=>output.slice(begin).includes('RhoAias phase: UNFURL'),'shield unfolding');
    b.chat('/motionrp view off');await waitFor(()=>[...spawned[1]].every(id=>removed[1].has(id)),'OFF removes partial shield');await sleep(300);const stopped=particles[1];await sleep(350);assert.equal(particles[1],stopped);
    b.chat('/motionrp view on');await waitFor(()=>output.slice(begin).includes('RhoAias phase: HOLD'),'all shield layers opened');
    const shield=entries('RhoAias',begin);assert.equal(shield.length,1);await waitFor(()=>visible(shield),'front flower renders for both clients');const domes=[...output.slice(begin).matchAll(/RhoAias translucent layer=(\d+) alpha=42 ids=([0-9,]+)/g)];assert.equal(domes.length,6);await waitFor(()=>domes.every(m=>m[2].split(',').every(id=>spawned.every(set=>set.has(Number(id))))),'six translucent domes reach both clients');
    await waitFor(()=>output.slice(begin).includes('RhoAias phase: FADE'),'shield fade');await waitFor(cleanup,'shield/body cleanup');
    assert.deepEqual([a.health,b.health],health);assert.ok(a.entity.position.distanceTo(anchor)<.01);pass('front flower and six translucent domes unfold for both viewers, respect OFF/ON and clean up without damage');
    for(const s of spawned)s.clear();begin=output.length;a._testLeftClick();await waitFor(()=>output.slice(begin).includes('RhoAias phase: HOLD'),'shield cancellation hold');a._testLeftClick();await waitFor(cleanup,'cancel shield cleanup');await sleep(400);const cancelParticles=particles[0];await sleep(650);assert.equal(particles[0],cancelParticles);pass('cancelling Rho Aias destroys every layer and stops future particles');
    a.setControlState('sneak',true);await sleep(250);a._testLeftClick();await sleep(250);a.setControlState('sneak',false);await sleep(250);
    for(const s of spawned)s.clear();begin=output.length;a._testLeftClick();await waitFor(()=>output.slice(begin).includes('GetsugaTensho phase: FADE'),'complete Bankai crescent');
    const slash=entries('GetsugaTensho',begin);assert.deepEqual(slash.map(e=>e.kind),['hollow_mask','tensa_zangetsu','crescent','crescent_trail0','crescent_trail1']);assert.ok(output.slice(begin).includes('two-handed overhead charge; blade aura=45 foot aura=3'));await waitFor(()=>visible(slash),'katana and curved slash reach both clients');await waitFor(cleanup,'Getsuga cleanup');
    assert.ok(output.slice(begin).includes('crescent impact distance=32'));assert.deepEqual([a.health,b.health],health);assert.ok(a.entity.position.distanceTo(anchor)<.01);pass('Tensa Zangetsu launches one curved black/red slash and impacts thirty-two blocks ahead without moving or damaging players');
    for(const phase of ['MASK','CHARGE','FLIGHT','TRAILS']){
      for(const s of spawned)s.clear();begin=output.length;a._testLeftClick();await waitFor(()=>output.slice(begin).includes(phase==='MASK'?'mask band=2':phase==='TRAILS'?'GetsugaTensho displays crescent_trail1':'GetsugaTensho phase: '+phase),'Getsuga '+phase);a._testLeftClick();await waitFor(cleanup,'cancel '+phase);await sleep(2100);assert.ok(cleanup(),'late model after cancelling');assert.ok(!output.slice(begin).includes('crescent impact'));pass('Getsuga cancellation during '+phase+' prevents impact and removes owned displays');
    }
    consoleCommand('fill -2 -60 8 2 -56 8 minecraft:stone');await sleep(300);begin=output.length;a._testLeftClick();await waitFor(()=>output.slice(begin).includes('crescent impact distance='),'wall blocks crescent');
    const hit=Number(output.slice(begin).match(/crescent impact distance=([0-9.]+)/)[1]);assert.ok(hit>0 && hit<8);await waitFor(cleanup,'wall impact cleanup');consoleCommand('fill -2 -60 8 2 -56 8 minecraft:air');pass('crescent hits the nearby wall instead of passing through it');
    b.chat('/motionrp view off');await sleep(300);spawned[1].clear();const off=particles[1];begin=output.length;a._testLeftClick();await waitFor(()=>output.slice(begin).includes('GetsugaTensho phase: FADE'),'OFF-viewer cast');assert.equal(spawned[1].size,0);assert.equal(particles[1],off);await waitFor(cleanup,'OFF test cleanup');pass('new Getsuga models and particles are never sent to an OFF viewer');
    await disconnect(a);await disconnect(b);await stopServer();assert.ok(!/RhoAias scene failed|GetsugaTensho scene failed|Motion playback failed/.test(output));
  }catch(error){code=1;console.error(error.stack);}
  finally{for(const bot of bots)try{bot.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/barrier-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
  process.exit(code);
}

if(process.env.MOTIONRP_TITAN_ONLY==='1'){
  let code=0;
  try{
    fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
    log=fs.createWriteStream(path.join(project,'build/titan-smoke.log'));await startServer();
    let a=await connect('MotionTesterA'),b=await connect('MotionTesterB');
    consoleCommand('tp MotionTesterA 0 -60 0 0 0');consoleCommand('tp MotionTesterB 5 -60 0 0 0');await sleep(500);
    await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');
    a.setControlState('sneak',true);await sleep(300);for(let i=0;(await readSaved('MotionTesterA')).loadout[6]!==5 && i<18;i++){a._testLeftClick();await sleep(300);}a.setControlState('sneak',false);await sleep(300);a._testLook(0,0);
    const sceneDisplays=new Set(),sceneRemoved=new Set(),visibleItems={owner:new Map(),observer:new Map()},removedByRole={owner:new Set(),observer:new Set()};
    for(const [bot,role]of[[a,'owner'],[b,'observer']]){
      bot._client.on('spawn_entity',p=>{if(role==='observer' && p.type===bot.registry.entitiesByName.item_display.id)sceneDisplays.add(p.entityId);});
      bot._client.on('entity_destroy',p=>{for(const id of p.entityIds){removedByRole[role].add(id);if(role==='observer' && sceneDisplays.has(id))sceneRemoved.add(id);}});
      const index=bot.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack');bot._client.on('entity_metadata',p=>{for(const m of p.metadata)if(m.key===index)visibleItems[role].set(p.entityId,m.value);});
    }
    const summonTrace=(prefix,begin)=>[...output.slice(begin).matchAll(new RegExp(prefix+' displays (\\w+) ids=([0-9,]+)','g'))].map(m=>({kind:m[1],ids:m[2].split(',').map(Number)}));
    const renderedForBoth=entries=>['owner','observer'].every(role=>entries.every(e=>e.ids.some(id=>(visibleItems[role].get(id)?.itemCount||0)>0)));
    const removedForBoth=entries=>['owner','observer'].every(role=>entries.every(e=>e.ids.every(id=>removedByRole[role].has(id))));
    ({a,b}=await verifyTitanRecovery(a,b,{sceneDisplays,sceneRemoved,removedByRole,summonTrace,renderedForBoth,removedForBoth}));
    await disconnect(a);await disconnect(b);await stopServer();
  }catch(error){code=1;console.error(error.stack);}
  finally{for(const b of bots)try{b.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/titan-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
  process.exit(code);
}

if(process.env.MOTIONRP_VISIBILITY_ONLY==='1'){
  let code=0;
  try{
    fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
    log=fs.createWriteStream(path.join(project,'build/visibility-smoke.log'));await startServer();
    const suffix=String(Date.now()).slice(-6);const an='ViewA'+suffix,bn='ViewB'+suffix;
    const a=await connect(an),b=await connect(bn);consoleCommand(`tp ${an} 0 -60 0 0 0`);consoleCommand(`tp ${bn} 5 -60 0 0 0`);await sleep(400);
    const spawned=[new Set(),new Set()],removed=[new Set(),new Set()],particles=[0,0],bolts=[0,0];let offInvisible=false;
    for(const [i,bot]of[a,b].entries()){
      bot._client.on('spawn_entity',p=>{if(p.type===bot.registry.entitiesByName.item_display.id)spawned[i].add(p.entityId);if(p.type===bot.registry.entitiesByName.lightning_bolt.id)bolts[i]++;});
      bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[i].add(id)));
      bot._client.on('world_particles',()=>particles[i]++);
      if(i===1)bot._client.on('entity_metadata',p=>{if(p.entityId===a.entity.id && p.metadata.some(m=>m.key===0 && (Number(m.value)&32)))offInvisible=true;});
    }
    consoleCommand(`motionrp give controller ${an}`);consoleCommand(`motionrp give motion rasengan 1 ${an}`);
    await waitFor(()=>tokens(a)===1,'visibility test token');await menu(a);await deposit(a);await closeMenu(a);
    await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');
    let start=output.length;a._testLeftClick();await waitFor(()=>output.slice(start).includes('Rasengan phase:'),'default-off playback');await sleep(600);
    assert.equal(spawned[0].size,0);assert.equal(spawned[1].size,0);assert.deepEqual(particles,[0,0]);pass('new players default OFF for actor model, clone, orb and particles');
    a.chat('/motionrp view on');await waitFor(()=>spawned[0].size>0 && particles[0]>0,'caster opts in');assert.equal(spawned[1].size,0);assert.equal(particles[1],0);
    b.chat('/motionrp view on');await waitFor(()=>spawned[1].size>0 && particles[1]>0,'observer opts in mid-scene');
    b.chat('/motionrp view off');await waitFor(()=>[...spawned[1]].every(id=>removed[1].has(id)),'OFF immediately removes owned display entities');
    await sleep(200);const offParticles=particles[1];await sleep(500);assert.equal(particles[1],offParticles);pass('ON joins mid-scene; OFF removes body/effects and stops new particles');
    // Stop current scene with the same controller and add Titan as the second slot.
    a._testLeftClick();await sleep(250);consoleCommand(`motionrp give motion titan_transform 1 ${an}`);await waitFor(()=>tokens(a)===1,'Titan visibility token');await menu(a);await deposit(a);await closeMenu(a);
    a.setControlState('sneak',true);await sleep(200);a._testLeftClick();await sleep(200);a.setControlState('sneak',false);await sleep(250);
    start=output.length;a._testLeftClick();await waitFor(()=>output.slice(start).includes('Titan native lightning id='),'filtered native lightning',10000);await sleep(500);
    assert.ok(bolts[0]>0,'enabled caster has no native lightning');assert.equal(bolts[1],0,'OFF observer saw native lightning');assert.equal(offInvisible,false,'Titan globally hid the source player from OFF observer');
    a._testLeftClick();await sleep(400);pass('Titan native lightning is hidden before spawning to OFF observers');
    const oldCount=spawned[1].size;a.chat('/goalperformance mbappe');await waitFor(()=>spawned[1].size>oldCount,'sports performance remains visible with MotionRP view OFF');await sleep(5500);pass('sports commands remain independent of original MotionRP viewer preferences');
    consoleCommand(`tp ${bn} 80 -60 0`);await sleep(300);b.chat('/motionrp view on');await sleep(200);
    const distantCount=spawned[1].size;start=output.length;a._testLeftClick();await waitFor(()=>output.slice(start).includes('Titan native lightning id='),'range-bounded Titan');await sleep(400);
    assert.equal(spawned[1].size,distantCount,'ON viewer beyond configured range saw scene models');a._testLeftClick();b.chat('/motionrp view off');await sleep(300);pass('configured viewer range also bounds large scenes');
    await disconnect(a);await disconnect(b);await stopServer();
    const saved=await readSaved(an);assert.equal(saved.data.BukkitValues['motionrp:view_enabled'],1);const {parsed:otherData}=await nbt.parse(fs.readFileSync(path.join(run,'world/players/data',uuid(bn)+'.dat')));assert.equal(nbt.simplify(otherData).BukkitValues['motionrp:view_enabled'],0);
    await startServer();const rejoined=await connect(an);rejoined.chat('/motionrp view');await waitFor(()=>rejoined._testMessages.some(m=>m.includes('演出表示: OFF')),'saved ON toggles OFF after server restart');pass('viewer preference persists through disconnect and server restart');
    await disconnect(rejoined);await stopServer();assert.ok(!/scene failed|Could not pass event|Motion playback failed/.test(output));
  }catch(error){code=1;console.error(error.stack);}
  finally{for(const b of bots)try{b.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/visibility-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
  process.exit(code);
}

// Command celebrations use the same disposable Paper fixture, without tokens or GUI input.
if(process.env.MOTIONRP_PERFORMANCE_ONLY==='1') {
  let code=0;
  try {
    assert.ok(fs.existsSync(path.join(run,'server.properties')),'Run full integration first');
    fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
    const oldPerformance=gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/bundled-0.17.1/players_motionrp_performances.bbmodel.gz')));
    fs.writeFileSync(path.join(run,'plugins/BetterModel/players/motionrp_performances.bbmodel'),oldPerformance);
    log=fs.createWriteStream(path.join(project,'build/performance-smoke.log'));await startServer();
    assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel/players/motionrp_performances.bbmodel')),fs.readFileSync(path.join(project,'src/main/resources/bettermodel/players/motionrp_performances.bbmodel')));
    const oldHash=createHash('sha256').update(oldPerformance).digest('hex');assert.deepEqual(fs.readFileSync(path.join(run,'plugins/MotionRP/model-backups/motionrp_performances.bbmodel.'+oldHash+'.bak')),oldPerformance);pass('0.17.1 performance model migrates with exact backup');
    const a=await connect('MotionTesterA'),b=await connect('MotionTesterB');
    consoleCommand('tp MotionTesterA 0 -60 0 0 0');consoleCommand('tp MotionTesterB 5 -60 0 0 0');
    await waitFor(()=>Math.abs(a.entity.position.z-.5)<.01 && Math.abs(a.entity.position.y+60)<.01,'celebration origins');
    const before=(await readSaved('MotionTesterA')).loadout,health=[a.health,b.health];
    const inventory=()=>JSON.stringify(a.inventory.items().map(i=>[i.slot,i.name,i.count]));
    await a.unequip('hand');await sleep(300);
    const expectedItems=inventory();assert.equal(a.heldItem,null,'commands run empty handed');
    const spawned=[new Set(),new Set()],removed=[new Set(),new Set()];
    for(const [i,bot] of [a,b].entries()){
      bot._client.on('spawn_entity',p=>{if(p.type===bot.registry.entitiesByName.item_display.id)spawned[i].add(p.entityId);});
      bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[i].add(id)));
    }
    for(const [command,id] of [['goalperformance ronaldo','ronaldo'],['goalperformance mbappe','mbappe'],['goalperformance kneeslide','kneeslide'],['goalperformance kazu','kazu'],['hrperformance destasha','destasha'],['HRperformance yamakawa','yamakawa'],['HRperformance matsuda','matsuda']]){
      a._testLook(0,0);await sleep(150);for(const s of spawned)s.clear();const origin=a.entity.position.clone();let highest=origin.y;
      const sampler=setInterval(()=>{highest=Math.max(highest,a.entity.position.y);},20);
      const start=output.length;a.chat('/'+command);
      await waitFor(()=>output.slice(start).includes(`Performance-${id} phase: PREPARE`),id+' direct start');
      if(id==='ronaldo'){a._testStep(.3,.42,.2);await sleep(250);assert.ok(Math.abs(a.entity.position.y+60)<.02,'jump input moved player');}
      await waitFor(()=>output.slice(start).includes(`Performance-${id} phase: RECOVER`),id+' full animation',10000);
      clearInterval(sampler);
      if(id==='ronaldo'){assert.ok(highest-origin.y>1.1,'Siu did not physically jump');assert.ok(Math.abs(a.entity.position.z-origin.z-6)<.05,'Siu did not run forward six blocks');assert.ok(output.slice(start).includes('travel complete distance=6.0 yaw=180.0'));}
      if(id==='kneeslide')assert.ok(Math.abs(a.entity.position.z-origin.z-7)<.05,'Slide did not travel seven blocks');
      assert.ok(output.slice(start).includes(`Performance-${id} signature tick=`));
      assert.ok(spawned.every(s=>s.size>0),'body not sent to both players');
      await waitFor(()=>spawned.every((s,i)=>[...s].every(id=>removed[i].has(id))),id+' body cleanup');
      assert.equal(inventory(),expectedItems,'command touched inventory');
      assert.deepEqual([a.health,b.health],health);assert.deepEqual((await readSaved('MotionTesterA')).loadout,before);
      assert.ok(!(await readSaved('MotionTesterA')).data.BukkitValues?.['motionrp:aerial_return_v1'],'travel journal left behind');
      pass(command+' plays empty-handed for owner and observer, finishes and preserves loadout');
    }
    for(const [command,allowed] of [['goalperformance',['ronaldo','mbappe','kneeslide','kazu']],['HRperformance',['yamakawa','matsuda','destasha']]]){
      const start=output.length;a.chat('/'+command);await waitFor(()=>/Performance-\w+ phase: PREPARE/.test(output.slice(start)),'random choice');
      const id=output.slice(start).match(/Performance-(\w+) phase: PREPARE/)[1];assert.ok(allowed.includes(id));
      a.chat('/'+command+' stop');await sleep(500);pass(command+' chooses within category and stops');
    }
    let start=output.length;a.chat('/goalperformance ronaldo');await waitFor(()=>output.slice(start).includes('Performance-ronaldo phase: PREPARE'),'replacement source');
    a.chat('/HRperformance matsuda');await waitFor(()=>output.slice(start).includes('Performance-matsuda phase: PREPARE'),'replacement target');
    a.chat('/goalperformance yamakawa');await sleep(300);
    assert.ok(a._testMessages.some(m=>m.includes('使い方: /goalperformance')));
    await waitFor(()=>output.slice(start).includes('Performance-matsuda phase: RECOVER'),'invalid selection leaves current running',10000);
    await sleep(1500);pass('commands replace playback safely; wrong category does not cancel current playback');
    start=output.length;a.chat('/goalperformance mbappe');await waitFor(()=>output.slice(start).includes('Performance-mbappe phase: PREPARE'),'cancel source');
    consoleCommand('tp MotionTesterA 1 -60 0');await sleep(700);
    assert.ok(!output.slice(start).includes('Performance-mbappe signature'),'external teleport failed to cancel');
    consoleCommand('tp MotionTesterA 0 -60 0 0 0');await sleep(400);
    start=output.length;a.chat('/goalperformance ronaldo');await waitFor(()=>a.entity.position.y>-59,'Siu airborne cancellation');
    a.chat('/goalperformance stop');await sleep(500);assert.ok(Math.abs(a.entity.position.y+60)<.02 && Math.abs(a.entity.position.z-.5)<.02,'airborne cancellation did not restore origin');
    pass('airborne Siu stop restores safe ground position');
    consoleCommand('setblock 0 -60 2 minecraft:stone');consoleCommand('setblock 0 -59 2 minecraft:stone');await sleep(400);
    start=output.length;a.chat('/goalperformance kneeslide');await waitFor(()=>a._testMessages.some(m=>m.includes('進行先に障害物')),'slide obstruction');
    assert.ok(a.entity.position.z<2,'slide passed through wall');
    consoleCommand('setblock 0 -60 2 minecraft:air');consoleCommand('setblock 0 -59 2 minecraft:air');
    pass('sliding stops before a solid wall');
    consoleCommand('goalperformance ronaldo');await waitFor(()=>output.includes('プレイヤーがゲーム内で実行してください。'),'console rejection');
    pass('external teleport cancels; console gets a clear player-only response');
    for(const bot of bots)await disconnect(bot);await stopServer();
    assert.ok(!/Performance-.*scene failed|Could not pass event|Could not load.*motionrp_performances|Error occurred while enabling MotionRP/.test(output));
  }catch(error){code=1;console.error(error.stack);}
  finally{for(const bot of bots)try{bot.quit();}catch{}try{await stopServer();}catch{}log?.end();fs.writeFileSync(path.join(project,'build/performance-smoke-result.json'),JSON.stringify({passed:code===0,version,checks},null,2));}
  process.exit(code);
}

// Short verification against a fixture created by the full suite; never touches a real server.
if(process.env.MOTIONRP_NOBLE_ONLY==='1') {
  let code=0;
  try {
    assert.ok(fs.existsSync(path.join(run,'world/players/data',uuid('MotionTesterA')+'.dat')),'Run the full integration suite first');
    fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
    for(const file of ['motionrp_hollow_mask','motionrp_excalibur_charge','motionrp_excalibur_wave','motionrp_excalibur_pillar','motionrp_susanoo_slash','motionrp_kamehameha_orb','motionrp_kamehameha_beam','motionrp_genki_dama_orb','motionrp_excalibur_blade','motionrp_excalibur_light','motionrp_gae_bolg_spear','motionrp_gae_bolg_trail','motionrp_gae_bolg_thorns','motionrp_susanoo_armor','motionrp_zangetsu_cleaver'])
      fs.copyFileSync(path.join(project,'src/main/resources/bettermodel/models',file+'.bbmodel'),path.join(run,'plugins/BetterModel/models',file+'.bbmodel'));
    for(const id of ['excalibur','gae_bolg','kamehameha','genki_dama'])fs.copyFileSync(path.join(project,'src/main/resources/bettermodel/players','motionrp_'+id+'.bbmodel'),path.join(run,'plugins/BetterModel/players','motionrp_'+id+'.bbmodel'));
    const energyUpgrade=[];
    if(process.env.MOTIONRP_VERIFY_ENERGY_UPGRADE==='1')for(const file of fs.readdirSync(path.join(project,'src/test/resources/bundled-0.14.0'))){
      const relative=file.replace(/^(models|players)_/,'$1/').replace(/\.gz$/,'');
      const previous=gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/bundled-0.14.0',file)));
      fs.writeFileSync(path.join(run,'plugins/BetterModel',relative),previous);energyUpgrade.push([relative,previous]);
    }
    let referenceUpgrade;
    if(process.env.MOTIONRP_VERIFY_REFERENCE_UPGRADE==='1'){
      referenceUpgrade=gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/bundled-0.14.1/players_motionrp_excalibur.bbmodel.gz')));
      fs.writeFileSync(path.join(run,'plugins/BetterModel/players/motionrp_excalibur.bbmodel'),referenceUpgrade);
    }
    const surfaceUpgrade=[];
    if(process.env.MOTIONRP_VERIFY_SURFACE_UPGRADE==='1')for(const file of fs.readdirSync(path.join(project,'src/test/resources/bundled-0.14.2'))){
      const relative=file.replace(/^(models|players)_/,'$1/').replace(/\.gz$/,'');const old=gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/bundled-0.14.2',file)));
      fs.writeFileSync(path.join(run,'plugins/BetterModel',relative),old);surfaceUpgrade.push([relative,old]);
    }
    const oldArmor=gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/bundled-0.18.0/models_motionrp_susanoo_armor.bbmodel.gz')));fs.writeFileSync(path.join(run,'plugins/BetterModel/models/motionrp_susanoo_armor.bbmodel'),oldArmor);
    log=fs.createWriteStream(path.join(project,'build/noble-smoke.log'));await startServer();await checkMaskPack();
    assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel/models/motionrp_susanoo_armor.bbmodel')),fs.readFileSync(path.join(project,'src/main/resources/bettermodel/models/motionrp_susanoo_armor.bbmodel')));pass('0.18.0 Susanoo armor migrates to the new sheath and articulated arms');
    for(const [relative,old] of surfaceUpgrade){assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel',relative)),fs.readFileSync(path.join(project,'src/main/resources/bettermodel',relative)));const h=createHash('sha256').update(old).digest('hex');assert.deepEqual(fs.readFileSync(path.join(run,'plugins/MotionRP/model-backups',path.basename(relative)+'.'+h+'.bak')),old);}
    if(surfaceUpgrade.length)pass('0.14.2 sphere, pillar and arm assets migrate with exact backups');
    await waitFor(()=>fs.existsSync(path.join(run,'plugins/BetterModel/build.zip')) && fs.readFileSync(path.join(run,'plugins/BetterModel/build.zip')).includes(Buffer.from('assets/motionrp/models/energy/')),'energy pack is prepared for manual distribution');

    if(referenceUpgrade){
      assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel/players/motionrp_excalibur.bbmodel')),fs.readFileSync(path.join(project,'src/main/resources/bettermodel/players/motionrp_excalibur.bbmodel')));
      const hash=createHash('sha256').update(referenceUpgrade).digest('hex');assert.deepEqual(fs.readFileSync(path.join(run,'plugins/MotionRP/model-backups/motionrp_excalibur.bbmodel.'+hash+'.bak')),referenceUpgrade);
      pass('0.14.1 Excalibur pose migrates with an exact backup');
    }

    for(const [relative,previous] of energyUpgrade){
      assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel',relative)),fs.readFileSync(path.join(project,'src/main/resources/bettermodel',relative)));
      const hash=createHash('sha256').update(previous).digest('hex');
      assert.deepEqual(fs.readFileSync(path.join(run,'plugins/MotionRP/model-backups',path.basename(relative)+'.'+hash+'.bak')),previous);
    }
    if(energyUpgrade.length)pass('all nine untouched 0.14 energy assets migrate with exact backups');

    const a=await connect('MotionTesterA'),b=await connect('MotionTesterB');
    // Previous smoke throws advance the player. Reset both viewers for repeatable range checks.
    consoleCommand('tp MotionTesterA 0 -60 0 0 0');consoleCommand('tp MotionTesterB 5 -60 0 0 0');
    await waitFor(()=>Math.abs(a.entity.position.z-.5)<.01 && Math.abs(b.entity.position.z-.5)<.01 && Math.abs(a.entity.position.y+60)<.01,'repeatable smoke origins');
    const items={owner:new Map(),observer:new Map()},removed={owner:new Set(),observer:new Set()},spawned={owner:new Set(),observer:new Set()};
    const itemIndex=b.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack');
    const scales={owner:new Map(),observer:new Map()};const scaleIndex=b.registry.entitiesByName.item_display.metadataKeys.indexOf('scale');
    for(const [bot,role] of [[a,'owner'],[b,'observer']]) {
      bot._client.on('spawn_entity',p=>{if(p.type===b.registry.entitiesByName.item_display.id)spawned[role].add(p.entityId);});
      bot._client.on('entity_metadata',p=>{for(const e of p.metadata){if(e.key===itemIndex)items[role].set(p.entityId,e.value);if(e.key===scaleIndex)scales[role].set(p.entityId,e.value);}});
      bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>removed[role].add(id)));
    }
    await a.equip(a.inventory.items().find(i=>i.name==='carrot_on_a_stick'),'hand');
    if(process.env.MOTIONRP_SUSANOO_ONLY!=='1'){
    for(const [slot,prefix,segments] of [[9,'Excalibur',14],[10,'GaeBolg',0]]) {
      a.setControlState('sneak',true);await sleep(300);
      for(let i=0;(await readSaved('MotionTesterA')).loadout[6]!==slot && i<18;i++) {a._testLeftClick();await sleep(300);}
      a.setControlState('sneak',false);await sleep(300);a._testLook(0,0);
      assert.equal((await readSaved('MotionTesterA')).loadout[6],slot);
      const start=output.length,health=[a.health,b.health];for(const role of ['owner','observer'])spawned[role].clear();a._testLeftClick();
      await waitFor(()=>output.slice(start).includes(prefix+' phase: FADE'),prefix+' smoke release',20000);
      const entries=[...output.slice(start).matchAll(new RegExp(prefix+' displays (\\w+) ids=([0-9,]+)','g'))].map(m=>({kind:m[1],ids:m[2].split(',').map(Number)}));
      assert.equal(entries.filter(e=>e.kind.startsWith('beam')).length,segments);
      if(prefix==='Excalibur')for(const kind of ['light_blade','wavefront','light_pillar'])assert.ok(entries.some(e=>e.kind===kind),kind+' present');
      for(const role of ['owner','observer']) assert.ok(entries.every(e=>e.ids.some(id=>(items[role].get(id)?.itemCount||0)>0)));
      await waitFor(()=>['owner','observer'].every(role=>[...spawned[role]].every(id=>removed[role].has(id))),prefix+' smoke cleanup');
      assert.deepEqual([a.health,b.health],health);pass(prefix+' final model reload, release, both-client display and cleanup');
    }
    a.setControlState('sneak',true);await sleep(300);
    for(let i=0;(await readSaved('MotionTesterA')).loadout[6]!==9 && i<18;i++){a._testLeftClick();await sleep(300);}
    a.setControlState('sneak',false);await sleep(300);
    for(const kind of ['light_blade','wavefront']){
      const begin=output.length;for(const role of ['owner','observer'])spawned[role].clear();a._testLeftClick();
      await waitFor(()=>output.slice(begin).includes('Excalibur displays '+kind),'Excalibur '+kind+' before cancellation');a._testLeftClick();
      await waitFor(()=>['owner','observer'].every(role=>[...spawned[role]].every(id=>removed[role].has(id))),'Excalibur '+kind+' cancellation cleanup');
      await sleep(1200);assert.ok(!output.slice(begin).includes('Excalibur displays light_pillar'));
      pass('cancelling Excalibur '+kind+' removes every effect and prevents the impact pillar');
    }
    for(const [slot,id,prefix] of [[11,'kamehameha','Kamehameha'],[12,'genki_dama','GenkiDama']]) {
      await menu(a);const registeredAlready=a.currentWindow.slots[slot]?.name==='paper';await closeMenu(a);
      if(!registeredAlready){
        consoleCommand('clear MotionTesterA minecraft:paper');await waitFor(()=>tokens(a)===0,'clear spare tokens');
        consoleCommand(`motionrp give motion ${id} 1 MotionTesterA`);await waitFor(()=>tokens(a)===1,'Dragon Ball token');
        await menu(a);await deposit(a);await closeMenu(a);
      }
      a.setControlState('sneak',true);await sleep(300);
      for(let i=0;(await readSaved('MotionTesterA')).loadout[6]!==slot && i<18;i++){a._testLeftClick();await sleep(300);}
      a.setControlState('sneak',false);await sleep(300);const base=a.entity.position.clone(),begin=output.length,dragonHealth=[a.health,b.health];
      for(const role of ['owner','observer'])spawned[role].clear();a._testLeftClick();
      if(id==='genki_dama') {await waitFor(()=>output.slice(begin).includes('rise complete height=50.0 ticks=40 diameter=80'),'Spirit Bomb 50-block ascent');await waitFor(()=>Math.abs(a.entity.position.y-base.y-50)<.1,'client follows full ascent');
        await waitFor(()=>output.slice(begin).includes('sphere ready diameter=80 bottom tracks raised palms'),'full sphere at raised palms');
        const match=output.slice(begin).match(/GenkiDama displays charge_orb ids=([0-9,]+)/);assert.ok(match);const core=Number(match[1].split(',')[0]);
        assert.ok(!removed.owner.has(core),'ON caster lost the Spirit Bomb while rising beyond the ground audience range');
        const span=Number(execFileSync('python3',['-c',`import zipfile,json,sys
z=zipfile.ZipFile(sys.argv[1]);f=next(n for n in z.namelist() if n.endswith('/motionrp_genki_dama_orb_energy_sphere.json') and '/items/' in n);item=json.loads(z.read(f))['model'];assert item['type'].split(':')[-1]=='composite'
def count_models(v):
 if isinstance(v,dict):return int('transformation' in v)+sum(count_models(x) for x in v.values())
 if isinstance(v,list):return sum(count_models(x) for x in v)
 return 0
assert count_models(item)>500
m=json.load(open(sys.argv[2]));mesh=next(e for e in m['elements'] if e['name']=='plasma_sphere');v=list(mesh['vertices'].values());print(max(p[0] for p in v)-min(p[0] for p in v))`,path.join(run,'plugins/BetterModel/build.zip'),path.join(project,'src/main/resources/bettermodel/models/motionrp_genki_dama_orb.bbmodel')],{encoding:'utf8'}));
        await waitFor(()=>['owner','observer'].every(role=>Math.abs((scales[role].get(core)?.x||0)*span/16-80)<.05),'both clients receive an actual 80-block sphere');
}
      if(id==='genki_dama'){
        await waitFor(()=>output.slice(begin).includes('GenkiDama pilot returned after impact'),'return follows the impact');
        const trace=output.slice(begin);assert.ok(trace.includes('impact airborneHeight=50.0'));assert.ok(trace.indexOf('GenkiDama impact')<trace.indexOf('GenkiDama pilot returned after impact'));
      }
      await waitFor(()=>output.slice(begin).includes(prefix+' phase: FADE'),prefix+' complete',20000);
      assert.ok(a.entity.position.distanceTo(base)<.01);
      const entries=[...output.slice(begin).matchAll(new RegExp(prefix+' displays (\\w+) ids=([0-9,]+)','g'))].map(m=>({kind:m[1],ids:m[2].split(',').map(Number)}));
      assert.ok(entries.some(e=>e.kind==='charge_orb'));
      assert.equal(entries.filter(e=>e.kind.startsWith('beam')).length,id==='kamehameha'?20:0);
      for(const role of ['owner','observer'])assert.ok(entries.every(e=>e.ids.some(id=>(items[role].get(id)?.itemCount||0)>0)),prefix+' renderable models reach '+role);
      await waitFor(()=>['owner','observer'].every(role=>[...spawned[role]].every(e=>removed[role].has(e))),prefix+' cleanup');
      assert.deepEqual([a.health,b.health],dragonHealth);pass(prefix+' both-client rendering, complete release and clean recovery without damage');
      if(id==='genki_dama'){const beginCancel=output.length;a._testLeftClick();await waitFor(()=>a.entity.position.y>base.y+8,'Spirit Bomb airborne cancel');a._testLeftClick();await waitFor(()=>a.entity.position.distanceTo(base)<.01,'Spirit Bomb cancelled ground return');await sleep(1000);assert.ok(!output.slice(beginCancel).includes('GenkiDama impact'));pass('Spirit Bomb cancellation restores grounded player before later impact');}
    }
    const airBase=a.entity.position.clone(),airBegin=output.length;a._testLeftClick();
    await waitFor(()=>output.slice(airBegin).includes('GenkiDama phase: THROW'),'cancel a thrown Spirit Bomb');assert.ok(a.entity.position.y>airBase.y+49);a._testLeftClick();
    await waitFor(()=>a.entity.position.distanceTo(airBase)<.01,'throw cancellation returns to ground');await sleep(1800);assert.ok(!output.slice(airBegin).includes('GenkiDama impact'));
    pass('cancelling after the throw returns the airborne player and prevents the impact');
    }
    a.setControlState('sneak',true);await sleep(300);
    for(let i=0;(await readSaved('MotionTesterA')).loadout[6]!==3 && i<18;i++) {a._testLeftClick();await sleep(300);}
    a.setControlState('sneak',false);await sleep(300);const seatOrigin=a.entity.position.clone(),seatBegin=output.length;
    for(const role of ['owner','observer'])spawned[role].clear();a._testLeftClick();
    await waitFor(()=>output.slice(seatBegin).includes('pilot seated scale=10.0'),'final neck seat');assert.ok(a.entity.position.y>seatOrigin.y+50);
    await waitFor(()=>output.slice(seatBegin).includes('Susanoo phase: FADE'),'final neck return',20000);assert.ok(a.entity.position.distanceTo(seatOrigin)<.01);
    await waitFor(()=>['owner','observer'].every(role=>[...spawned[role]].every(id=>removed[role].has(id))),'final full giant cleanup');
    for(const phrase of ['draw left waist scabbard','blade clears scabbard','draw-cut ground blast and wind start','kesa ground blast and wind start'])assert.ok(output.slice(seatBegin).includes(phrase),phrase);
    pass('Final complete Susanoo sheath draw, two ground blasts, scale-ten neck seat and grounded return');
    assert.ok(!/Excalibur scene failed|GaeBolg scene failed|Kamehameha scene failed|GenkiDama scene failed|Susanoo scene failed/.test(output));
    fs.writeFileSync(path.join(project,'build/noble-smoke-result.json'),JSON.stringify({passed:checks.length,checks},null,2));
  } catch(e) {console.error(e.stack);code=1;}
  finally {for(const bot of bots)bot.quit();await stopServer();if(log)log.end();}
  process.exit(code);
}


try {
  fs.mkdirSync(path.dirname(run), { recursive: true });
  fs.rmSync(resultFile, { force: true });
  fs.rmSync(run, { recursive: true, force: true });
  fs.mkdirSync(path.join(run, 'plugins'), { recursive: true });
  log = fs.createWriteStream(path.join(project, 'build/integration-server.log'));
  for (const directory of ['libraries', 'cache', 'versions']) {
    const source = path.join(fixture, directory);
    if (fs.existsSync(source)) fs.symlinkSync(source, path.join(run, directory), 'dir');
  }
  fs.copyFileSync(path.join(fixture, 'plugins/bettermodel-3.6.0-paper.jar'), path.join(run, 'plugins/BetterModel.jar'));
  fs.copyFileSync(path.join(project, `build/libs/motionRP-${version}.jar`), path.join(run, 'plugins/MotionRP.jar'));
  fs.mkdirSync(path.join(run, 'plugins/MotionRP'));
  fs.writeFileSync(path.join(run, 'plugins/MotionRP/config.yml'),
    fs.readFileSync(path.join(project, 'src/main/resources/config.yml'), 'utf8')
      .replace('debug-input: false', 'debug-input: true')
      .replace(/  excalibur:\n[\s\S]*?(?=  gae_bolg:)/, '')
      .replace(/  gae_bolg:\n[\s\S]*?(?=  zangetsu_bankai:)/, '')
      .replace(/  zangetsu_bankai:\n[\s\S]*?(?=  gate_of_babylon_100:)/, '')
      .replace(/  gate_of_babylon_100:\n[\s\S]*?(?=  titan_transform:)/, '')
      .replace(/  titan_transform:\n[\s\S]*?(?=  senbonzakura:)/, '')
      .replace(/  senbonzakura:\n[\s\S]*?(?=  gate_of_babylon:)/, '')
      .replace(/  gate_of_babylon:\n[\s\S]*?(?=  susanoo:)/, '')
      .replace(/  susanoo:\n[\s\S]*?(?=  rasenshuriken:)/, '')
      .replace(/  rasenshuriken:\n[\s\S]*?(?=  rasengan:)/, '')
      .replace(/  rasengan:\n[\s\S]*?(?=  roll:)/, ''));
  // Mineflayer currently supports 26.1. Official Via plugins adapt the test
  // clients to a real 26.3 server; MotionRP still runs against the 26.3 API.
  for (const [name, checksum] of [
    ['ViaVersion', '24e4876fefe73e08a4c87cd6c457ee0916e62dd69656631caf41e26f909d504b'],
    ['ViaBackwards', '9a148a80ea22074f4bd1626cdbe254a4cd998bf44e7ceaff4e285a1b6b35da42']
  ]) {
    const target = path.join(run, 'plugins', name + '.jar');
    execFileSync('curl', ['-fsSL', '--max-time', '60', '-o', target,
      `https://github.com/ViaVersion/${name}/releases/download/5.12.2/${name}-5.12.2.jar`]);
    assert.equal(createHash('sha256').update(fs.readFileSync(target)).digest('hex'), checksum, name + ' checksum');
  }
  fs.cpSync(path.join(fixture, 'plugins/BetterModel'), path.join(run, 'plugins/BetterModel'), { recursive: true });
  const oldShuriken=gunzipSync(fs.readFileSync(path.join(project,'src/test/resources/motionrp_rasenshuriken-0.3.0.bbmodel.gz')));
  fs.mkdirSync(path.join(run,'plugins/BetterModel/players'),{recursive:true});
  fs.writeFileSync(path.join(run,'plugins/BetterModel/players/motionrp_rasenshuriken.bbmodel'),oldShuriken);
  const oldSummons=[['models/motionrp_gae_bolg_spear','79a465c56e8e1599bd4e95eaffcf70427af5a5bbcffdf1cbeabf08ade160db92','0.12.0'],['models/motionrp_excalibur_blade','f52925ae4aaa05a189851ba32a820f8ddf49888c1444d3540999d9153841b219','0.12.0'],['players/motionrp_gae_bolg','233b327d4b95091c583e1d2c432ae12ef26782a51d8829eb66dc1654c23c14e4','0.12.0'],['models/motionrp_susanoo_slash','6255e3d0879d4442415cf00594cb7ed59928179965e7ad7d2b9ae2388a24dbf6','0.10.0'],['players/motionrp_susanoo','435fb48bd71a7c2b7357b4070ff3084a351d4884ac7810cedd4accacc7c155d9','0.10.0'],['models/motionrp_zangetsu_cleaver','2b9a0b0bfc733d1fb25e099e3a0c1c795f66f129ece7971cecb80000e724b794','0.12.0'],['models/motionrp_bankai_cocoon','d6f38cfd0592d3ce2dd6fa5154269bc340f9f4eb8f6fe528f79b04f761faa127','0.9.0'],['models/motionrp_tensa_zangetsu','cf68b92b1a7cde04d8b7b0c4a5f435b621d9c9760e1102675d969416c7068806','0.9.0'],['players/motionrp_zangetsu_bankai','1eef505842f410eca1b28c82c6707903c016e247f5ac4ffdd537a3b68207d822','0.10.0'],['models/motionrp_bab_ilu','545322bab257c47b88f2b4a96cdbe67adece3bc641c07c06ce4b16cf4a656039','0.9.0'],['players/motionrp_gate_of_babylon_enuma','4be25d52494f9f97b56693808dc9aef769e205679b4fed7bc7c77e5c0131c7b5','0.10.0'],['models/motionrp_ea','97161bc3d6a447a255f60c5f42e54c15ea58007c0f28e3270f10581fbaf5dad0','0.10.0'],['models/motionrp_enuma_vortex','9ddebc5469047326fbff87242a29e64723952d0147512c4cf6339643042ecb0e','0.8.0'],['players/motionrp_titan_transform','bfbc83f290b4252a4088fd825e0fca0bed7a4a019e74f4305d33a791d3f20857','0.6.0'],['players/motionrp_gate_of_babylon','2e0bae16f4570116a383ccaa91e09319564665870b34c7cc03a9c156c34c2272','0.5.0'],
    ['players/motionrp_senbonzakura','7a38d287afad202d93644f1d54ba0267baa33eff01315860675bd940c5944afe','0.5.0'],
    ['models/motionrp_senbonzakura_blade','aebce2d6a72045a193e54ebaad4e7cf8fd579ccc19aba3182b659c5f5ed44d70','0.5.0'],
    ['models/motionrp_susanoo_armor','7edbd7f936024d0c1b5d47c618d2ef04e08f59f7a435a2e6f2edf65372a0466c','0.12.0'],
    ['models/motionrp_susanoo_ribs','ef9b07b05a0000b5a0f9d908a621e4543d71db9081d1bb61b06b97fa2a0cf62b','0.10.0']];
  for(const [relative,,version='0.4.0'] of oldSummons) {
    const base=path.basename(relative);
    fs.writeFileSync(path.join(run,'plugins/BetterModel',relative+'.bbmodel'),
      gunzipSync(fs.readFileSync(path.join(project,'src/test/resources',base+'-'+version+'.bbmodel.gz'))));
  }
  fs.writeFileSync(path.join(run, 'eula.txt'), 'eula=true\n');
  fs.writeFileSync(path.join(run, 'server.properties'), `server-ip=127.0.0.1
server-port=${port}
online-mode=false
enforce-secure-profile=false
white-list=false
enforce-whitelist=false
view-distance=4
simulation-distance=4
spawn-protection=0
max-players=5
difficulty=peaceful
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
generate-structures=false
`);
  fs.writeFileSync(path.join(run, 'ops.json'), JSON.stringify([{ uuid: uuid('MotionTesterA'), name: 'MotionTesterA', level: 4, bypassesPlayerLimit: true }]));
  await startServer();
  const playerAsset = 'players/motionrp_rasengan.bbmodel';
  assert.deepEqual(fs.readFileSync(path.join(run, 'plugins/BetterModel', playerAsset)),
    fs.readFileSync(path.join(project, 'src/main/resources/bettermodel', playerAsset)));
  const fixturePlayer = path.join(fixture, 'plugins/BetterModel', playerAsset);
  const legacyHash = fs.existsSync(fixturePlayer) ? createHash('sha256').update(fs.readFileSync(fixturePlayer)).digest('hex') : '';
  if (['5c6a2b0425861d8fd648dfeacc5b61592ca2ef1c46da3ae6dfa55d08e46916d6',
    'e62db71a5d780a4ae7a3a3771e281e90c3b0fa4795c121e331ebf616b56d4c5d',
    '2e2fba7179fb111f67963a2619896c7cca7a2f434d241a1679f3a50f93bafc2e'].includes(legacyHash)) {
    const backup = path.join(run, 'plugins/MotionRP/model-backups', 'motionrp_rasengan.bbmodel.' + legacyHash + '.bak');
    assert.deepEqual(fs.readFileSync(backup), fs.readFileSync(fixturePlayer));
  }
  pass('installed player animation matches current bundle and an untouched legacy model is backed up during upgrade');
  assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel/players/motionrp_rasenshuriken.bbmodel')),
    fs.readFileSync(path.join(project,'src/main/resources/bettermodel/players/motionrp_rasenshuriken.bbmodel')));
  assert.deepEqual(fs.readFileSync(path.join(run,'plugins/MotionRP/model-backups/motionrp_rasenshuriken.bbmodel.497bcb980eef3288e8ece86779fa8651c6fb97f40ef49cde598b78232c30dce1.bak')),oldShuriken);
  pass('untouched 0.3.0 shuriken animation is backed up and replaced with the faster body motion');
  for(const [relative,hash] of oldSummons) {
    assert.deepEqual(fs.readFileSync(path.join(run,'plugins/BetterModel',relative+'.bbmodel')),
      fs.readFileSync(path.join(project,'src/main/resources/bettermodel',relative+'.bbmodel')));
    assert.equal(createHash('sha256').update(fs.readFileSync(path.join(run,'plugins/MotionRP/model-backups',path.basename(relative)+'.bbmodel.'+hash+'.bak'))).digest('hex'),hash);
  }
  pass('legacy guardian and 0.5.0 treasury/cherry assets are backed up and upgraded');
  let a = await connect('MotionTesterA');
  let b = await connect('MotionTesterB');
  consoleCommand('motionrp give controller MotionTesterA');
  consoleCommand('motionrp give motion roll 64 MotionTesterA');
  await waitFor(() => tokens(a) === 64, 'stack of 64 motion items');
  assert.ok(a.inventory.items().some(i => i.name === 'carrot_on_a_stick' && i.count === 1));
  pass('controller distribution and stack of 64 identical motion tokens');

  await a.equip(a.inventory.items().find(i => i.name === 'carrot_on_a_stick'), 'hand');
  a._testLookUp();
  a._client.write('use_item', { hand: 'main_hand', sequence: 1, rotation: { x: 0, y: -60 } });
  await waitFor(() => a.currentWindow !== null, 'controller right-click GUI');
  await sleep(200);
  pass('right-clicking the controller opens the registration GUI');
  assert.equal(registered(a), 0);
  await deposit(a);
  assert.equal(registered(a), 1);
  assert.equal(tokens(a), 63);
  await deposit(a);
  assert.equal(registered(a), 2);
  assert.equal(tokens(a), 62);
  await closeMenu(a);
  await menu(a);
  assert.equal(registered(a), 2);
  pass('physical one-item deposits and GUI close/reopen preserve exact total');

  // Vanilla hotbar swap, collect-to-cursor and item drop must not extract GUI copies.
  for (const [button, mode] of [[0, 2], [0, 6], [1, 4]]) {
    await click(a, 0, button, mode);
    assert.equal(registered(a), 2);
    assert.equal(tokens(a), 62);
  }
  await click(a, 0);
  assert.equal(registered(a), 1);
  assert.equal(tokens(a), 63);
  await deposit(a);
  assert.equal(registered(a), 2);
  assert.equal(tokens(a), 62);
  pass('number-key, collect and drop paths cannot duplicate display items; withdrawal returns one original');

  await menu(b);
  assert.equal(registered(b), 0);
  await closeMenu(b);
  pass('loadouts are isolated per player');
  await closeMenu(a);

  // Cycle using the real controller click packet, not a direct method call.
  const controller = a.inventory.items().find(i => i.name === 'carrot_on_a_stick');
  await a.equip(controller, 'hand');
  a._testLookUp();
  a.setControlState('sneak', true);
  await sleep(300);
  // Removing the originally selected first slot above selected slot two.
  // Verify wrap to slot one, then a second click selects slot two again.
  assert.equal((await readSaved('MotionTesterA')).loadout[6], 1);
  for (const selected of [0, 1]) {
    a._testLeftClick();
    await waitFor(async () => {
      try { return (await readSaved('MotionTesterA')).loadout[6] === selected; } catch { return false; }
    }, 'shift-left selection saved: ' + selected);
    await sleep(300);
  }
  a.setControlState('sneak', false);
  await sleep(300);
  let saved = await readSaved('MotionTesterA');
  assert.equal(saved.loadout[6], 1);
  pass('sneak-left selects the second slot and saves selection into player.dat');

  const seenDisplays = new Set();
  const removedDisplays = new Set();
  const displayType = b.registry.entitiesByName.item_display.id;
  b._client.on('spawn_entity', packet => { if (packet.type === displayType) seenDisplays.add(packet.entityId); });
  b._client.on('entity_destroy', packet => { for (const id of packet.entityIds) if (seenDisplays.has(id)) removedDisplays.add(id); });
  // Place both clients near each other; wait for chunk and entity tracking.
  consoleCommand('tp MotionTesterA 0 -60 0');
  consoleCommand('tp MotionTesterB 2 -60 0');
  await sleep(2000);
  a._testLookUp();
  await sleep(300);
  a._testLeftClick();
  await waitFor(() => a._testMessages.some(m => m.includes('再生:')), 'controller playback');
  await waitFor(() => seenDisplays.size > 0, 'other player receives model displays');
  await waitFor(() => removedDisplays.size === seenDisplays.size, 'completed animation removes its displays');
  pass('left-click starts BetterModel playback; another client receives and then removes model display entities');

  await disconnect(a);
  await disconnect(b);
  await stopServer();
  saved = await readSaved('MotionTesterA');
  assert.equal(saved.loadout[6], 1);
  const priorConfig=path.join(run,'plugins/MotionRP/config.yml');
  fs.writeFileSync(priorConfig,fs.readFileSync(priorConfig,'utf8').replace(/(  senbonzakura:\n[\s\S]*?max-ticks:) \d+/, '$1 180').replace(/(  gate_of_babylon_enuma:\n[\s\S]*?max-ticks:) \d+/, '$1 380').replace(/(  zangetsu_bankai:\n[\s\S]*?max-ticks:) \d+/, '$1 220').replace(/(  susanoo:\n[\s\S]*?max-ticks:) \d+/, '$1 180'));
  await startServer();
  assert.ok(/senbonzakura:\n[\s\S]*?max-ticks: 260/.test(fs.readFileSync(priorConfig,'utf8')));
  assert.ok(/gate_of_babylon_enuma:\n[\s\S]*?max-ticks: 480/.test(fs.readFileSync(priorConfig,'utf8')));
  assert.ok(/zangetsu_bankai:\n[\s\S]*?max-ticks: 280/.test(fs.readFileSync(priorConfig,'utf8')));
  pass('Enuma and Bankai old timeouts upgrade to fit the held declaration and branching Ea formation');
  pass('old bundled Senbonzakura timeout is extended automatically for the longer petal orbit');
  a = await connect('MotionTesterA');
  assert.equal(tokens(a), 62);
  await menu(a);
  assert.equal(registered(a), 2);
  await click(a, 0);
  await click(a, 1);
  assert.equal(tokens(a), 64);
  assert.equal(registered(a), 0);
  await closeMenu(a);
  pass('server restart restores registration, selected slot and 62+2 items; both tokens are reclaimable');

  // Full inventory: 63 paper + one in escrow, then fill paper stack and every empty slot.
  await menu(a);
  await deposit(a);
  await closeMenu(a);
  consoleCommand('motionrp give motion roll 1 MotionTesterA');
  await waitFor(() => tokens(a) === 64, 'full paper stack');
  const empty = a.inventory.emptySlotCount();
  consoleCommand('give MotionTesterA minecraft:stone ' + (empty * 64));
  await waitFor(() => a.inventory.emptySlotCount() === 0, 'filled inventory');
  await menu(a);
  await click(a, 0);
  assert.equal(registered(a), 1);
  assert.equal(tokens(a), 64);
  await closeMenu(a);
  consoleCommand('clear MotionTesterA minecraft:stone');
  await waitFor(() => a.inventory.items().every(i => i.name !== 'stone'), 'free storage');
  await menu(a);
  await click(a, 0);
  assert.equal(registered(a), 0);
  assert.equal(tokens(a), 65);
  pass('full inventory keeps escrow item; freeing space allows lossless return');

  await closeMenu(a);
  // Exercise the custom scene on the actual 26.3 server after the regression tests.
  b = await connect('MotionTesterB');
  // A near-camera effect must still send item data when the viewer faces away
  // (F5 may look in a different direction from the server-side eye direction).
  const bmConfig = path.join(run, 'plugins/BetterModel/config.yml');
  fs.writeFileSync(bmConfig, fs.readFileSync(bmConfig, 'utf8').replace(/^min-sight:.*$/m, 'min-sight: 0'));
  const reloadBegin = output.length;
  consoleCommand('bettermodel reload');
  await waitFor(() => /Reload completed/i.test(output.slice(reloadBegin)), 'reload strict sight culling test config', 30_000);
  consoleCommand('motionrp status');
  await waitFor(() => output.includes('rasengan:') && output.includes('rasengan: \u001b[97m利用可能')
      || /rasengan:.*利用可能/.test(output), 'bundled Rasengan player/orb models loaded', 30_000);
  pass('bundled Rasengan bbmodels are installed and parsed by BetterModel');
  consoleCommand('clear MotionTesterA minecraft:paper');
  await waitFor(() => tokens(a) === 0, 'clear regression tokens');
  consoleCommand('motionrp give motion rasengan 1 MotionTesterA');
  await waitFor(() => tokens(a) === 1, 'Rasengan token');
  await menu(a);
  await deposit(a);
  assert.equal(registered(a), 1);
  assert.equal(tokens(a), 0);
  await closeMenu(a);
  await a.equip(a.inventory.items().find(i => i.name === 'carrot_on_a_stick'), 'hand');
  consoleCommand('tp MotionTesterA 0 -60 0');
  consoleCommand('tp MotionTesterB 2 -60 0');
  await sleep(1000);
  a._testLookUp();
  b._testLook(-90, 0);
  const sceneDisplays = new Set();
  const sceneRemoved = new Set();
  const visibleItems = { owner: new Map(), observer: new Map() };
  const rotations = new Map();
  const removedByRole = { owner: new Set(), observer: new Set() };
  let observeBankai=false,observeFormation=false;const bankaiParticles={owner:[],observer:[]};let branchDust=0,branchSparks=0;
  const itemMetadataIndex = b.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack');
  for (const [client, role] of [[a, 'owner'], [b, 'observer']]) {
    client._client.on('entity_destroy', packet => { for (const id of packet.entityIds) removedByRole[role].add(id); });
    client._client.on('world_particles',packet=>{
      if(observeBankai) bankaiParticles[role].push({type:packet.particle.type,amount:packet.amount,z:packet.z});
      if(observeFormation && role==='observer') {if(packet.particle.type==='dust') branchDust++;if(packet.particle.type==='electric_spark') branchSparks++;}
    });
    client._client.on('entity_metadata', packet => {
      for (const entry of packet.metadata) {
        if (entry.key === itemMetadataIndex) visibleItems[role].set(packet.entityId, entry.value);
        if (role==='observer' && entry.key===b.registry.entitiesByName.item_display.metadataKeys.indexOf('left_rotation')) {
          if(!rotations.has(packet.entityId)) rotations.set(packet.entityId,new Set());
          rotations.get(packet.entityId).add(JSON.stringify(entry.value));
        }
      }
    });
  }
  let particles = 0;
  let sounds = 0;
  const windSounds = [];
  b._client.on('sound_effect', packet => {
    // Protocol registry holders are zero-based after decoding; minecraft-data's
    // sound table is one-based. Mineflayer's event misses this +1 in 26.1.
    const holder=packet.sound;
    const name=holder?.data?.soundName ?? b.registry.sounds[holder ? holder.soundId+1 : packet.soundId]?.name;
    if (name) windSounds.push(name);
  });
  b._client.on('spawn_entity', packet => { if (packet.type === b.registry.entitiesByName.item_display.id) sceneDisplays.add(packet.entityId); });
  b._client.on('entity_destroy', packet => { for (const id of packet.entityIds) if (sceneDisplays.has(id)) sceneRemoved.add(id); });
  b._client.on('world_particles', () => particles++);
  b._client.on('sound_effect', () => sounds++);
  const health = b.health;
  let lowestHealth = health;
  const trackHealth = () => { lowestHealth = Math.min(lowestHealth, b.health); };
  b.on('health', trackHealth);
  const begin = output.length;
  const dashOrigin = a.entity.position.clone();
  a._testLeftClick();
  await waitFor(() => output.slice(begin).includes('Rasengan phase: CHARGE'), 'charge item metadata sample');
  await sleep(400);
  const chargeTrace = output.slice(begin);
  assert.ok(a.entity.position.distanceTo(dashOrigin) < .01, 'Player moved before thrust');
  for (const offset of [[.2, 0, 0], [0, .2, .1], [0, 0, -.2]]) {
    a._testStep(...offset);
    await sleep(150);
  }
  const lockedBegin = output.length;
  consoleCommand('data get entity MotionTesterA Pos');
  await waitFor(() => output.slice(lockedBegin).includes('has the following entity data:'), 'server position while frozen');
  const lockedPosition = output.slice(lockedBegin).replace(/\u001b\[[0-9;]*m/g, '').match(/has the following entity data: \[([^\]]+)\]/)[1]
    .split(',').map(v => Number(v.trim().replace(/d$/, '')));
  assert.ok(Math.abs(lockedPosition[0] - dashOrigin.x) < .01
    && Math.abs(lockedPosition[1] - dashOrigin.y) < .01
    && Math.abs(lockedPosition[2] - dashOrigin.z) < .01, 'Movement input changed actual server position: ' + lockedPosition);
  for (const role of ['owner', 'observer']) for (const kind of ['clone', 'orb']) {
    const ids = chargeTrace.match(new RegExp('Rasengan displays ' + kind + ' ids=([0-9,]+)'))[1].split(',').map(Number);
    assert.ok(ids.some(id => (visibleItems[role].get(id)?.itemCount || 0) > 0), role + '/' + kind + ' was empty while charging');
  }
  await waitFor(() => output.slice(begin).includes('Rasengan phase: DISSIPATE'), 'complete Rasengan choreography', 15_000);
  await waitFor(() => sceneDisplays.size > 12 && sceneRemoved.size === sceneDisplays.size, 'clone, orb and body cleanup', 10_000);
  assert.ok(Math.abs(a.entity.position.z - dashOrigin.z - 4) < .05, 'Thrust did not move the real player four blocks: ' + a.entity.position);
  assert.ok(Math.abs(a.entity.position.x - dashOrigin.x) < .01, 'Dash moved sideways');
  const dashEnd = a.entity.position.clone();
  await sleep(300);
  assert.ok(a.entity.position.distanceTo(dashEnd) < .01, 'Player drifted after scene ended');
  pass('real player stays still during charge, dashes four blocks during thrust and stops without drift');
  pass('walking and jumping inputs stay at the server anchor without cancelling playback or the scripted dash');
  const trace = output.slice(begin);
  const idsFor = kind => trace.match(new RegExp('Rasengan displays ' + kind + ' ids=([0-9,]+)'))[1].split(',').map(Number);
  fs.writeFileSync(path.join(project, 'build/visual-metadata.json'), JSON.stringify({
    clone: idsFor('clone'), orb: idsFor('orb'),
    owner: Object.fromEntries(visibleItems.owner), observer: Object.fromEntries(visibleItems.observer)
  }, (_, value) => typeof value === 'bigint' ? value.toString() : value, 2));
  for (const role of ['owner', 'observer']) for (const kind of ['clone', 'orb']) {
    const rendered = idsFor(kind).filter(id => {
      const item = visibleItems[role].get(id);
      return item && (item.itemCount || item.count || 0) > 0;
    });
    assert.ok(rendered.length > 0, role + ' received no renderable item metadata for ' + kind);
  }
  pass('both caster and observer receive nonempty renderable item metadata for clone and orb');
  for (const phase of ['SIGNS', 'ASSIST', 'CHARGE', 'WINDUP', 'THRUST', 'DISSIPATE']) {
    assert.ok(output.slice(begin).includes('Rasengan phase: ' + phase), 'Missing phase ' + phase);
  }
  assert.ok(particles > 0, 'No effect particles received');
  assert.ok(sounds > 0, 'No effect sounds received');
  assert.equal(b.health, health, 'Cosmetic Rasengan changed another player health');
  assert.equal(lowestHealth, health, 'Cosmetic Rasengan temporarily damaged another player');
  b.off('health', trackHealth);
  pass('shadow clone, charging orb, thrust, particles/sounds and full cleanup reach another player without damage');
  assert.ok(windSounds.some(name => name.includes('breeze')), 'No breeze sound was received: ' + windSounds);
  assert.ok(!windSounds.some(name => /amethyst|bell|beacon/.test(name)), 'Old ringing sound remains: ' + windSounds);
  pass('Rasengan uses breeze wind sounds instead of ringing amethyst/beacon sounds');

  sceneDisplays.clear(); sceneRemoved.clear();
  const cancelBegin = output.length;
  a._testLeftClick();
  await waitFor(() => output.slice(cancelBegin).includes('Rasengan phase: CHARGE'), 'Rasengan charge before cancellation');
  await waitFor(() => sceneDisplays.size > 12, 'clone and orb before cancellation');
  a._testLeftClick();
  await waitFor(() => sceneRemoved.size === sceneDisplays.size, 'cancel clears every effect display');
  const lastParticles = particles;
  await sleep(600);
  assert.equal(particles, lastParticles, 'Particles continued after cancellation');
  assert.ok(!output.slice(cancelBegin).includes('Rasengan phase: THRUST'), 'Cancelled scene kept running');
  pass('repeat-click cancellation removes clone/orb/body and stops future particles and phases');

  for (let replay = 0; replay < 2; replay++) {
    consoleCommand('tp MotionTesterA 0 -60 0 0 0');
    if (replay === 0) consoleCommand('fill -1 -60 2 1 -58 2 minecraft:stone');
    else {
      consoleCommand('fill -1 -60 2 1 -58 2 minecraft:air');
      consoleCommand('fill -1 -64 2 1 -61 6 minecraft:air');
    }
    await waitFor(() => a.entity.position.distanceTo(dashOrigin) < .01, 'reset before obstruction test');
    await sleep(300);
    a._testLookUp();
    sceneDisplays.clear(); sceneRemoved.clear();
    const replayBegin = output.length;
    a._testLeftClick();
    await waitFor(() => output.slice(replayBegin).includes('Rasengan phase: DISSIPATE'), 'consecutive Rasengan replay ' + replay, 15_000);
    await waitFor(() => sceneDisplays.size > 12 && sceneRemoved.size === sceneDisplays.size, 'consecutive replay cleanup');
    assert.ok(a.entity.position.z > 1 && a.entity.position.z < 2, 'Dash failed to stop before ' + (replay === 0 ? 'wall' : 'cliff') + ': ' + a.entity.position);
    assert.ok(Math.abs(a.entity.position.y + 60) < .01, 'Dash fell or climbed');
    await sleep(300);
  }
  pass('three complete Rasengan plays plus cancellation can replay without relogging or reopening the GUI');
  pass('wall collision and missing ground stop real travel while the animation finishes');

  const teleportBegin = output.length;
  a._testLeftClick();
  await waitFor(() => output.slice(teleportBegin).includes('Rasengan phase: CHARGE'), 'charge before external teleport');
  consoleCommand('tp MotionTesterA 0 -60 0');
  await sleep(1600);
  assert.ok(!output.slice(teleportBegin).includes('Rasengan phase: THRUST'), 'External teleport failed to cancel scene');
  assert.ok(a.entity.position.distanceTo(dashOrigin) < .01, 'Cancelled dash moved player');
  pass('external teleport still cancels playback and prevents future dash movement');

  sceneDisplays.clear(); sceneRemoved.clear();
  const dashCancelBegin = output.length;
  a._testLeftClick();
  await waitFor(() => output.slice(dashCancelBegin).includes('Rasengan phase: THRUST'), 'dash before cancellation');
  a._testLeftClick();
  await waitFor(() => sceneRemoved.size === sceneDisplays.size, 'cancel during dash removes displays');
  const cancelledAt = a.entity.position.clone();
  await sleep(700);
  assert.ok(a.entity.position.distanceTo(cancelledAt) < .01, 'Dash kept moving after cancellation');
  pass('cancelling during the dash immediately ends travel and removes displays');
  a._testStep(.2, 0, 0);
  await sleep(250);
  const unlockedBegin = output.length;
  consoleCommand('data get entity MotionTesterA Pos');
  await waitFor(() => output.slice(unlockedBegin).includes('has the following entity data:'), 'server position after cancellation');
  const unlockedPosition = output.slice(unlockedBegin).replace(/\u001b\[[0-9;]*m/g, '').match(/has the following entity data: \[([^\]]+)\]/)[1]
    .split(',').map(v => Number(v.trim().replace(/d$/, '')));
  assert.ok(Math.abs(unlockedPosition[0] - cancelledAt.x - .2) < .01, 'Player remained frozen after cancellation');
  pass('ordinary movement is restored immediately after playback stops');

  // Motion two: migration, real GUI selection, two assistants, actual jump and throw.
  consoleCommand('fill -2 -64 -1 2 -61 9 minecraft:stone');
  consoleCommand('tp MotionTesterA 0 -60 0 0 0');
  consoleCommand('tp MotionTesterB 3 -60 0');
  await sleep(600);
  consoleCommand('motionrp status');
  await waitFor(() => /rasenshuriken:.*利用可能/.test(output), 'migrated Rasenshuriken definition available');
  const migratedConfig = fs.readFileSync(path.join(run, 'plugins/MotionRP/config.yml'), 'utf8');
  assert.ok(migratedConfig.includes('rasenshuriken-definition-installed: true'));
  consoleCommand('motionrp give motion rasenshuriken 64 MotionTesterA');
  await waitFor(() => tokens(a) === 64, 'stack of shuriken tokens');
  await menu(a); await deposit(a);
  assert.equal(registered(a), 2);assert.equal(tokens(a), 63);
  await closeMenu(a);
  await a.equip(a.inventory.items().find(i => i.name === 'carrot_on_a_stick'), 'hand');
  a.setControlState('sneak', true);await sleep(300);a._testLeftClick();
  await waitFor(async () => (await readSaved('MotionTesterA')).loadout[6] === 1, 'motion two selected');
  a.setControlState('sneak', false);await sleep(300);a._testLookUp();
  pass('Rasenshuriken migrates into an old config, stacks to 64 and registers/selects as the second motion');
  // Cancel while BOTH helpers still exist (midair cancellation is too late to
  // detect Tracker.equals collapsing two instances of the same player model).
  const formationCancelBegin=output.length;
  a._testLeftClick();
  await waitFor(() => output.slice(formationCancelBegin).includes('Rasenshuriken phase: CHARGE'), 'formation before cancellation');
  await sleep(300);
  const cancelTrace=output.slice(formationCancelBegin);
  const cancelledIds=['clone1','clone2','weapon'].flatMap(kind =>
    cancelTrace.match(new RegExp('Rasenshuriken displays '+kind+' ids=([0-9,]+)'))[1].split(',').map(Number));
  a._testLeftClick();
  await waitFor(() => ['owner','observer'].every(role => cancelledIds.every(id => removedByRole[role].has(id))),
    'both helpers and weapon removed from BOTH clients on formation cancel');
  await sleep(300);
  pass('cancelling during formation destroys every display of both same-model clones and weapon for caster and observer');
  sceneDisplays.clear();sceneRemoved.clear();
  const shurikenBegin=output.length;
  const shurikenSoundBegin=windSounds.length;
  const jumpOrigin=a.entity.position.clone();
  let peak=jumpOrigin.y, minimumCasterHealth=a.health;
  const casterHealth=a.health, observerHealth=b.health;
  heightSampler=setInterval(() => { peak=Math.max(peak,a.entity.position.y);minimumCasterHealth=Math.min(minimumCasterHealth,a.health); },20);
  a._testLeftClick();
  await waitFor(() => output.slice(shurikenBegin).includes('Rasenshuriken phase: CHARGE'), 'two-clone formation');
  await sleep(400);
  const formation=output.slice(shurikenBegin);
  const shurikenIds=kind => formation.match(new RegExp('Rasenshuriken displays '+kind+' ids=([0-9,]+)'))[1].split(',').map(Number);
  const clone1=shurikenIds('clone1'),clone2=shurikenIds('clone2');
  assert.ok(clone1.every(id => !clone2.includes(id)), 'Two clones reused display IDs');
  for (const role of ['owner','observer']) for (const kind of ['clone1','clone2','weapon']) {
    assert.ok(shurikenIds(kind).some(id => (visibleItems[role].get(id)?.itemCount || 0)>0), role+' has no renderable '+kind);
  }
  pass('caster and observer receive two separate skinned assistants and the renderable four-blade weapon');
  await waitFor(() => output.slice(shurikenBegin).includes('Rasenshuriken phase: LAND'), 'shuriken landing',15000);
  await waitFor(() => sceneDisplays.size>30 && sceneRemoved.size===sceneDisplays.size, 'all shuriken displays cleaned up');
  clearInterval(heightSampler);heightSampler=null;
  const shurikenTrace=output.slice(shurikenBegin);
  for (const phase of ['SIGNS','ASSIST','CHARGE','RAISE','STEP','JUMP','THROW','IMPACT','LAND'])
    assert.ok(shurikenTrace.includes('Rasenshuriken phase: '+phase), 'Missing shuriken phase '+phase);
  assert.ok(Math.abs(peak-jumpOrigin.y-8)<.06, 'Real jump height is not eight blocks: '+(peak-jumpOrigin.y));
  assert.ok(Math.abs(a.entity.position.y-jumpOrigin.y)<.01, 'Caster did not land');
  const burst=shurikenTrace.match(/Rasenshuriken burst x=([-\d.]+) y=([-\d.]+) z=([-\d.]+)/);
  assert.ok(burst, 'No wind impact');
  assert.ok(Math.abs(Number(burst[1])-jumpOrigin.x)<.01);
  assert.ok(Math.abs(Number(burst[3])-jumpOrigin.z-.35-8)<.06, 'Impact was not eight blocks beyond takeoff');
  assert.ok(Math.abs(Number(burst[2])-jumpOrigin.y-.15)<.06, 'Impact was not at ground level');
  assert.equal(minimumCasterHealth,casterHealth,'Jump caused damage');assert.equal(b.health,observerHealth,'Wind blast damaged observer');
  const groundCheckBegin=output.length;
  consoleCommand(`execute positioned ${Number(burst[1])} -61 ${Number(burst[3])} if block ~ ~ ~ minecraft:stone run say MOTIONRP_GROUND_UNCHANGED`);
  await waitFor(() => output.slice(groundCheckBegin).includes('MOTIONRP_GROUND_UNCHANGED'), 'wind burst preserves impact block');
  assert.ok(shurikenTrace.includes('phase: JUMP tick=90') && shurikenTrace.includes('phase: THROW tick=106'), 'Jump-to-release duration is not 16 ticks');
  pass('weapon raises, player jumps eight blocks and releases in 16 ticks, hits ground eight blocks ahead and lands without damage');
  const shurikenSounds=windSounds.slice(shurikenSoundBegin);
  assert.ok(shurikenSounds.filter(name => name.includes('breeze.charge')).length>=4,'Charge did not repeat: '+shurikenSounds);
  assert.ok(shurikenSounds.some(name => name.includes('generic.explode')),'Layered explosion sound missing: '+shurikenSounds);
  pass('charge sound repeats through formation and a generic explosion sound layers with the wind impact');

  sceneDisplays.clear();sceneRemoved.clear();
  const airCancelBegin=output.length;a._testLeftClick();
  await waitFor(() => output.slice(airCancelBegin).includes('Rasenshuriken phase: JUMP'), 'second jump before cancel');
  await sleep(350);a._testLeftClick();
  await waitFor(() => sceneRemoved.size===sceneDisplays.size && Math.abs(a.entity.position.y-jumpOrigin.y)<.01, 'midair cancel returns to takeoff and cleans up');
  await sleep(1200);
  assert.ok(!output.slice(airCancelBegin).includes('Rasenshuriken burst'), 'Cancelled shuriken exploded later');
  assert.equal(a.health,casterHealth);
  pass('midair cancellation restores safe footing, removes both clones/weapon and prevents later wind explosion');

  consoleCommand('fill -1 -57 -1 1 -57 3 minecraft:stone');await sleep(300);
  // The swing-only test helper must aim into air; a real client sends a dig
  // packet when looking at this ceiling, which is already handled separately.
  a._testLook(0,0);await sleep(150);
  sceneDisplays.clear();sceneRemoved.clear();
  const ceilingBegin=output.length;a._testLeftClick();
  await waitFor(() => output.slice(ceilingBegin).includes('Rasenshuriken phase: JUMP'), 'jump under low ceiling');
  await waitFor(() => sceneDisplays.size>30 && sceneRemoved.size===sceneDisplays.size, 'ceiling collision cleans up all displays');
  assert.ok(Math.abs(a.entity.position.y-jumpOrigin.y)<.01, 'Ceiling abort did not restore footing');
  assert.ok(!output.slice(ceilingBegin).includes('Rasenshuriken phase: THROW'), 'Jump tunneled through ceiling');
  assert.equal(a.health,casterHealth);
  pass('a low ceiling aborts the jump without tunneling, damage or leaving model displays');
  assert.ok(!/Rasenshuriken scene failed|Rasengan scene failed/.test(output), 'Scene logged a runtime failure');

  consoleCommand('fill -1 -57 -1 1 -57 3 minecraft:air');
  consoleCommand('tp MotionTesterA 0 -60 0 0 0');consoleCommand('tp MotionTesterB 5 -60 0');
  await sleep(500);a._testLook(0,0);
  const summonTrace=(prefix,begin) => [...output.slice(begin).matchAll(new RegExp(prefix+' displays (\\w+) ids=([0-9,]+)','g'))]
    .map(match => ({kind:match[1],ids:match[2].split(',').map(Number)}));
  const removedForBoth=entries => ['owner','observer'].every(role => entries.every(e => e.ids.every(id => removedByRole[role].has(id))));
  const renderedForBoth=entries => ['owner','observer'].every(role => entries.every(e => e.ids.some(id => (visibleItems[role].get(id)?.itemCount||0)>0)));
  async function addSummon(id,selected) {
    consoleCommand('clear MotionTesterA minecraft:paper');await waitFor(() => tokens(a)===0,'free token slots');
    consoleCommand(`motionrp give motion ${id} 64 MotionTesterA`);await waitFor(() => tokens(a)===64,id+' stack');
    await menu(a);await deposit(a);assert.equal(registered(a),selected+1);assert.equal(tokens(a),63);await closeMenu(a);
    await a.equip(a.inventory.items().find(i => i.name==='carrot_on_a_stick'),'hand');
    a.setControlState('sneak',true);await sleep(300);a._testLeftClick();
    await waitFor(async () => (await readSaved('MotionTesterA')).loadout[6]===selected,id+' selected');
    a.setControlState('sneak',false);await sleep(300);a._testLook(0,0);
  }
  const newConfig=fs.readFileSync(path.join(run,'plugins/MotionRP/config.yml'),'utf8');
  assert.ok(newConfig.includes('gate_of_babylon-definition-installed: true'));
  assert.ok(newConfig.includes('susanoo-definition-installed: true'));
  assert.ok(newConfig.includes('senbonzakura-definition-installed: true'));
  consoleCommand('motionrp status');
  await waitFor(() => /gate_of_babylon:.*利用可能/.test(output) && /susanoo:.*利用可能/.test(output),'both summoned motion catalogs available');
  pass('both new definitions migrate into an old config and every summon model loads in BetterModel');

  consoleCommand('fill -8 -61 18 8 -61 23 minecraft:stone');await sleep(200);
  await addSummon('gate_of_babylon',2);
  pass('Gate of Babylon stacks to 64 and registers/selects in the same per-player GUI');
  sceneDisplays.clear();sceneRemoved.clear();
  const gateBegin=output.length,gateOrigin=a.entity.position.clone();
  const gateHealth=[a.health,b.health];let gateLowest=[...gateHealth];
  heightSampler=setInterval(() => {gateLowest=[Math.min(gateLowest[0],a.health),Math.min(gateLowest[1],b.health)];},20);
  a._testLeftClick();await waitFor(() => output.slice(gateBegin).includes('Gate phase: DRAW'),'all portals open');await sleep(300);
  const gateEntries=summonTrace('Gate',gateBegin);
  assert.equal(gateEntries.filter(e => e.kind.startsWith('portal')).length,20);
  assert.equal(gateEntries.filter(e => e.kind.startsWith('weapon')).length,20);
  assert.ok(renderedForBoth(gateEntries),'Some portal or weapon has empty render metadata');
  a._testStep(.2,0,.2);await sleep(150);
  pass('caster and observer receive all twenty golden portals and twenty loaded weapons');
  await waitFor(() => output.slice(gateBegin).includes('Gate phase: FADE'),'final gate volley',15000);
  await waitFor(() => sceneDisplays.size>25 && sceneRemoved.size===sceneDisplays.size && removedForBoth(gateEntries),'all gate models cleaned up');
  clearInterval(heightSampler);heightSampler=null;
  const gateTrace=output.slice(gateBegin);
  const fired=[...gateTrace.matchAll(/Gate fire index=(\d+) tick=(\d+)/g)].map(m => [Number(m[1]),Number(m[2])]);
  assert.deepEqual(fired,[...Array.from({length:30},(_,i)=>[i%20,42+i]),...Array.from({length:20},(_,i)=>[i,84])]);
  assert.equal([...gateTrace.matchAll(/Gate impact index=/g)].length,50);
  const impactRanges=[...gateTrace.matchAll(/Gate impact index=\d+ tick=\d+ forward=([\d.]+)/g)].map(m=>Number(m[1]));
  assert.equal(impactRanges.length,50);assert.ok(impactRanges.every(d=>d>=19.9 && d<=21.3),impactRanges.join(','));
  pass('every unobstructed treasury shot targets ground about twenty blocks ahead');
  const reloaded=[...gateTrace.matchAll(/Gate reload index=(\d+) tick=(\d+)/g)].map(m => [Number(m[1]),Number(m[2])]);
  assert.deepEqual(reloaded,Array.from({length:30},(_,i)=>[i%20,43+i]));
  const allGateEntries=summonTrace('Gate',gateBegin);
  assert.equal(allGateEntries.length,70);assert.ok(renderedForBoth(allGateEntries));assert.ok(removedForBoth(allGateEntries));
  assert.ok(a.entity.position.distanceTo(gateOrigin)<.01);assert.deepEqual(gateLowest,gateHealth);
  pass('thirty shots fire every tick with two-tick flights, followed by the twenty-gate volley and fifty impacts');
  const blastTimes=[...gateTrace.matchAll(/Gate explosion index=(\d+) tick=(\d+)/g)].map(m=>Number(m[2]));
  assert.equal(blastTimes.length,50);assert.equal(blastTimes.filter(t=>t<84).length,30);assert.equal(blastTimes.filter(t=>t>=84).length,20);
  const gateBlockBegin=output.length;
  consoleCommand(`execute positioned ${gateOrigin.x} ${gateOrigin.y-1} ${gateOrigin.z+20} if block ~ ~ ~ minecraft:stone run say GATE_GROUND_UNCHANGED`);
  await waitFor(()=>output.slice(gateBlockBegin).includes('GATE_GROUND_UNCHANGED'),'explosive volley keeps the impact floor');
  pass('all thirty rapid hits and twenty volley hits make cosmetic explosions while the impact blocks stay intact');
  sceneDisplays.clear();sceneRemoved.clear();const gateCancel=output.length;a._testLeftClick();
  await waitFor(() => output.slice(gateCancel).includes('Gate phase: DRAW'),'gate cancellation before firing');
  const cancelGates=summonTrace('Gate',gateCancel);assert.equal(cancelGates.length,40);
  a._testLeftClick();await waitFor(() => removedForBoth(cancelGates) && sceneRemoved.size===sceneDisplays.size,'cancel removes all same-model gates and weapons');
  await sleep(500);assert.ok(!output.slice(gateCancel).includes('Gate fire index='));
  pass('cancelling Gate of Babylon removes all forty portal/weapon trackers for both clients and prevents later shots');
  sceneDisplays.clear();sceneRemoved.clear();const reloadCancel=output.length;a._testLeftClick();
  await waitFor(() => output.slice(reloadCancel).includes('Gate reload index=0'),'loaded and flying weapons coexist');
  const reloadingGates=summonTrace('Gate',reloadCancel);assert.ok(reloadingGates.length>=41);
  a._testLeftClick();await waitFor(() => removedForBoth(reloadingGates) && sceneRemoved.size===sceneDisplays.size,'cancel in-flight and replacement weapons');
  const cancelledFireCount=[...output.slice(reloadCancel).matchAll(/Gate fire index=/g)].length;
  await sleep(800);assert.equal([...output.slice(reloadCancel).matchAll(/Gate fire index=/g)].length,cancelledFireCount);
  pass('cancelling during reload removes both in-flight and newly loaded weapons with no later volley');

  await addSummon('susanoo',3);pass('Susanoo stacks to 64 and registers/selects as a separate GUI motion');
  sceneDisplays.clear();sceneRemoved.clear();const susanooBegin=output.length;
  const guardianOrigin=a.entity.position.clone(),guardianHealth=[a.health,b.health];
  a._testLeftClick();await waitFor(() => output.slice(susanooBegin).includes('Susanoo phase: WINDUP'),'guardian armor ready');
  assert.ok(a.entity.position.y>guardianOrigin.y+50 && a.entity.position.y<guardianOrigin.y+54,'Player sits in the scale-ten neck');
  assert.ok(output.slice(susanooBegin).includes('pilot seated scale=10.0'));
  await waitFor(() => output.slice(susanooBegin).includes('Susanoo phase: FADE'),'guardian slash completed',15000);
  await waitFor(()=>a.entity.position.distanceTo(guardianOrigin)<.01,'client receives pilot return before dissolution',2000);
  const guardianEntries=summonTrace('Susanoo',susanooBegin);
  assert.deepEqual(guardianEntries.map(e => e.kind),['ribs','armor','slash_left','slash_right']);
  assert.ok(renderedForBoth(guardianEntries));
  const armorIds=guardianEntries.find(e => e.kind==='armor').ids;
  assert.ok(armorIds.length>80,'Complete armor must include wings, full legs, mask and paired weapons');
  assert.ok(output.slice(susanooBegin).includes('complete armor and wings formed tick=76'));
  assert.ok(armorIds.some(id => (rotations.get(id)?.size||0)>2),'Giant sword arm sent no changing rotations');
  await waitFor(() => sceneRemoved.size===sceneDisplays.size && removedForBoth(guardianEntries),'guardian cleanup for both viewers');
  for(const phase of ['SIGNS','RIBS','ARMOR','WINDUP','SLASH','IMPACT','FADE'])
    assert.ok(output.slice(susanooBegin).includes('Susanoo phase: '+phase));
  assert.ok(a.entity.position.distanceTo(guardianOrigin)<.01);assert.deepEqual([a.health,b.health],guardianHealth);
  pass('blue ribs form into the full winged samurai; left sweep and right diagonal slash animate and clean up without damage');
  sceneDisplays.clear();sceneRemoved.clear();const guardianCancel=output.length;a._testLeftClick();
  await waitFor(() => output.slice(guardianCancel).includes('Susanoo phase: ARMOR'),'guardian cancellation during formation');
  const cancelGuardian=summonTrace('Susanoo',guardianCancel);a._testLeftClick();
  await waitFor(() => removedForBoth(cancelGuardian) && sceneRemoved.size===sceneDisplays.size,'cancel guardian ribs and armor');
  await sleep(700);assert.ok(!output.slice(guardianCancel).includes('Susanoo phase: SLASH'));
  pass('cancelling Susanoo during formation clears both ribs and armor and prevents the slash');
  assert.ok(!/Gate scene failed|Susanoo scene failed/.test(output),'A summon scene logged a runtime failure');

  await addSummon('senbonzakura',4);
  consoleCommand('motionrp status');await waitFor(() => /senbonzakura:.*利用可能/.test(output),'new cherry models available');
  pass('Senbonzakura migrates into the old config, stacks to 64 and registers/selects as the fifth GUI motion');
  sceneDisplays.clear();sceneRemoved.clear();const cherryBegin=output.length;
  const cherryOrigin=a.entity.position.clone(),cherryHealth=[a.health,b.health];
  const particleBeforeCherry=particles;
  a._testLeftClick();await waitFor(() => output.slice(cherryBegin).includes('Senbonzakura phase: ORBIT'),'swords dissolve into petals');
  await sleep(400);
  const cherryEntries=summonTrace('Senbonzakura',cherryBegin);
  assert.equal(cherryEntries.filter(e => e.kind.startsWith('blade')).length,48);
  assert.equal(cherryEntries.filter(e => e.kind.startsWith('petals')).length,8);
  assert.equal(cherryEntries.filter(e => e.kind==='dropped').length,1);
  assert.equal(cherryEntries.filter(e=>e.kind.startsWith('scatter')).length,48);
  const dissolves=[...output.slice(cherryBegin).matchAll(/Senbonzakura dissolve index=(\d+) tick=(\d+)/g)].map(m=>[Number(m[1]),Number(m[2])]);
  assert.equal(dissolves.length,48);assert.equal(dissolves[0][1],80);assert.equal(dissolves.at(-1)[1],95);
  for(const [index,tick] of dissolves) assert.equal(tick,80+Math.floor((23-Math.floor(index/2))*15/23));
  pass('all forty-eight blades shed local swirling petals from far to near across fifteen ticks');
  assert.ok(renderedForBoth(cherryEntries),'Cherry blade/cloud render metadata missing');
  pass('caster and observer receive a held katana, forty-eight dense blade-only sheets and eight petal clouds');
  await waitFor(() => output.slice(cherryBegin).includes('Senbonzakura phase: FADE'),'petal convergence and impact',15000);
  await waitFor(() => removedForBoth(cherryEntries) && sceneRemoved.size===sceneDisplays.size,'cherry scene completely removed');
  for(const phase of ['DROP','RISE','BLOOM','ORBIT','SURGE','IMPACT','FADE'])
    assert.ok(output.slice(cherryBegin).includes('Senbonzakura phase: '+phase));
  assert.ok(cherryEntries.filter(e => e.kind.startsWith('petals')).some(e => e.ids.some(id => (rotations.get(id)?.size||0)>4)));
  assert.ok(particles>particleBeforeCherry);assert.ok(a.entity.position.distanceTo(cherryOrigin)<.01);
  assert.deepEqual([a.health,b.health],cherryHealth);
  pass('pink blade clouds rotate, orbit and converge with particles, then clean up without moving or damaging players');
  sceneDisplays.clear();sceneRemoved.clear();const bladeCancel=output.length;a._testLeftClick();
  await waitFor(() => output.slice(bladeCancel).includes('Senbonzakura displays blade5'),'cancel while ground swords rise');
  const cancelledBlades=summonTrace('Senbonzakura',bladeCancel);a._testLeftClick();
  await waitFor(() => removedForBoth(cancelledBlades) && sceneRemoved.size===sceneDisplays.size,'all same-model rising swords removed');
  await sleep(2200);assert.ok(!output.slice(bladeCancel).includes('Senbonzakura phase: BLOOM'));
  pass('cancelling during sword emergence removes every blade and prevents petal formation');
  sceneDisplays.clear();sceneRemoved.clear();const petalsCancel=output.length;a._testLeftClick();
  await waitFor(() => output.slice(petalsCancel).includes('Senbonzakura phase: ORBIT'),'cancel during petal orbit');
  const cancelledPetals=summonTrace('Senbonzakura',petalsCancel);a._testLeftClick();
  await waitFor(() => removedForBoth(cancelledPetals) && sceneRemoved.size===sceneDisplays.size,'all petal clouds removed');
  const stoppedParticles=particles;await sleep(4100);
  assert.equal(particles,stoppedParticles);assert.ok(!output.slice(petalsCancel).includes('Senbonzakura phase: SURGE'));
  pass('cancelling during petal orbit removes all clouds, stops future particles and prevents convergence');
  assert.ok(!output.includes('Senbonzakura scene failed'),'Cherry scene logged a runtime failure');

  await addSummon('titan_transform',5);
  consoleCommand('motionrp status');await waitFor(()=>/titan_transform:.*利用可能/.test(output),'sixth motion available');
  pass('Titan transformation migrates into the old config and stacks/registers/selects as the sixth custom motion');
  await addSummon('gate_of_babylon_100',6);
  consoleCommand('motionrp status');await waitFor(()=>/gate_of_babylon_100:.*利用可能/.test(output),'hundred gate availability');
  pass('hundred-gate motion migrates, stacks to 64 and registers/selects in the seventh custom slot');
  sceneDisplays.clear();sceneRemoved.clear();const hundredBegin=output.length,hundredOrigin=a.entity.position.clone(),hundredHealth=[a.health,b.health];
  a._testLeftClick();await waitFor(()=>output.slice(hundredBegin).includes('Treasury100 phase: DRAW'),'hundred gates loaded',20000);
  await sleep(350);const hundredInitial=summonTrace('Treasury100',hundredBegin);
  assert.equal(hundredInitial.filter(e=>e.kind.startsWith('portal')).length,100);
  assert.equal(hundredInitial.filter(e=>/^weapon\d+$/.test(e.kind)).length,100);
  await waitFor(()=>renderedForBoth(hundredInitial),'all hundred gates/weapons rendered');
  a._testStep(.2,.1,.1);
  await waitFor(()=>output.slice(hundredBegin).includes('Treasury100 phase: FADE'),'hundred gate volley',25000);
  await waitFor(()=>sceneRemoved.size===sceneDisplays.size,'hundred gates cleanup');
  const hundredTrace=output.slice(hundredBegin),hundredEntries=summonTrace('Treasury100',hundredBegin);
  const hundredFires=[...hundredTrace.matchAll(/Treasury100 fire index=(\d+) tick=(\d+)/g)].map(m=>[Number(m[1]),Number(m[2])]);
  assert.deepEqual(hundredFires,[...Array.from({length:100},(_,i)=>[i,42+Math.floor(i*15/100)]),...Array.from({length:100},(_,i)=>[i,57+Math.floor(i*15/100)]),...Array.from({length:100},(_,i)=>[i,84])]);
  assert.equal([...hundredTrace.matchAll(/Treasury100 impact index=/g)].length,300);
  assert.ok(hundredTrace.includes('volley blast tick=86'));assert.ok(removedForBoth(hundredEntries));
  assert.ok(a.entity.position.distanceTo(hundredOrigin)<.01);assert.deepEqual([a.health,b.health],hundredHealth);
  assert.equal([...output.slice(hundredBegin).matchAll(/Treasury100 explosion index=/g)].length,300);
  pass('all hundred gates shoot in thirty rapid ticks then join one hundred-shot volley with a sand/dust blast and full cleanup');
  sceneDisplays.clear();sceneRemoved.clear();const hundredCancel=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(hundredCancel).includes('Treasury100 fire index=0'),'hundred gate rapid before cancel',20000);
  const hundredCancelled=summonTrace('Treasury100',hundredCancel);a._testLeftClick();
  await waitFor(()=>removedForBoth(hundredCancelled) && sceneRemoved.size===sceneDisplays.size,'hundred gate rapid cancel');
  const stoppedHundred=particles;await sleep(700);assert.equal(particles,stoppedHundred);assert.ok(!output.slice(hundredCancel).includes('volley blast'));
  pass('cancelling the hundred-gate rapid pass removes all flying and loaded weapons and prevents the final blast');

  await addSummon('gate_of_babylon_enuma',7);
  consoleCommand(`summon minecraft:cow ${hundredOrigin.x} ${hundredOrigin.y} ${hundredOrigin.z+20} {NoAI:1b,NoGravity:1b,Silent:1b,Tags:["motionrp_target"]}`);await sleep(500);
  const cutSample=hundredOrigin.offset(0,-1,10).floored();
  consoleCommand(`setblock ${cutSample.x} ${cutSample.y} ${cutSample.z} minecraft:stone`);await sleep(500);
  const sampleType=bot=>bot.blockAt(cutSample)?.name;
  await waitFor(()=>sampleType(a)==='stone' && sampleType(b)==='stone','floor sample loaded');
  sceneDisplays.clear();sceneRemoved.clear();const enumaBegin=output.length,enumaHealth=[a.health,b.health];
  a._testLeftClick();await waitFor(()=>output.slice(enumaBegin).includes('TreasuryEnuma phase: DRAW'),'two hundred gates loaded',25000);
  await sleep(300);const enumaInitial=summonTrace('TreasuryEnuma',enumaBegin);
  assert.equal(enumaInitial.filter(e=>e.kind.startsWith('portal')).length,200);
  assert.equal(enumaInitial.filter(e=>/^weapon\d+$/.test(e.kind)).length,200);
  await waitFor(()=>renderedForBoth(enumaInitial),'two hundred gates/weapons visible',20000);
  const cage=[...output.slice(enumaBegin).matchAll(/TreasuryEnuma position index=(\d+) x=([-\d.]+) y=([-\d.]+) z=([-\d.]+)/g)].filter(m=>Number(m[1])>=100);
  assert.equal(cage.length,100);
  assert.ok(cage.every(m=>Math.abs(Math.hypot(Number(m[2])-hundredOrigin.x,Number(m[4])-(hundredOrigin.z+20))-6)<.05));
  pass('serious treasury shows one hundred rear gates and one hundred gates enclosing the living target twenty blocks ahead');
  await waitFor(()=>output.slice(enumaBegin).includes('TreasuryEnuma phase: RED_BRANCHES'),'red branching formation');observeFormation=true;
  await waitFor(()=>output.slice(enumaBegin).includes('TreasuryEnuma phase: CONVERGENCE'),'red branches converge');observeFormation=false;
  assert.ok(branchDust>1000 && branchSparks>20,'Branching red paths and sparks reach the observer before Ea materializes');
  pass('fixed hand gate reveals sixteen key bands, then thousands of red path particles branch and converge before Ea appears');
  await waitFor(()=>sampleType(a)==='air' && sampleType(b)==='air','excavated ground visible to both clients',30000);
  const worldCheck=output.length;consoleCommand(`execute if block ${cutSample.x} ${cutSample.y} ${cutSample.z} minecraft:stone run say ENUMA_WORLD_UNCHANGED`);
  await waitFor(()=>output.slice(worldCheck).includes('ENUMA_WORLD_UNCHANGED'),'actual floor preserved');
  await waitFor(()=>output.slice(enumaBegin).includes('TreasuryEnuma phase: FADE'),'Enuma Elish and impact',30000);
  await waitFor(()=>sceneRemoved.size===sceneDisplays.size,'Ea/beam/gates cleanup',15000);
  const enumaTrace=output.slice(enumaBegin),enumaEntries=summonTrace('TreasuryEnuma',enumaBegin);
  const enumaFires=[...enumaTrace.matchAll(/TreasuryEnuma fire index=(\d+) tick=(\d+)/g)].map(m=>[Number(m[1]),Number(m[2])]);
  assert.deepEqual(enumaFires,[...Array.from({length:200},(_,i)=>[i,42+Math.floor(i*15/200)]),...Array.from({length:200},(_,i)=>[i,57+Math.floor(i*15/200)]),...Array.from({length:200},(_,i)=>[i,84])]);
  assert.equal([...enumaTrace.matchAll(/TreasuryEnuma impact index=/g)].length,600);
  assert.equal([...enumaTrace.matchAll(/TreasuryEnuma explosion index=/g)].length,600);
  for(const kind of ['bab_ilu','hand_vault','ea']) assert.ok(enumaEntries.some(e=>e.kind===kind));
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('beam')).length,16);
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('charge_ring')).length,2);
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('shock_ring')).length,3);
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('rift')).length,0);
  assert.ok(!enumaEntries.some(e=>e.kind==='charge_core' || e.kind==='impact_core'));
  assert.ok(enumaTrace.includes('ea ready tick=232'));
  assert.ok(enumaTrace.includes('hand vault anchored tick=40'));
  assert.ok(enumaTrace.includes('red branches begin tick=76') && enumaTrace.includes('red branches converge tick=172'));
  assert.ok(enumaTrace.includes('hand vault anchored tick=40'));
  assert.equal(enumaEntries.find(e=>e.kind==='bab_ilu').ids.length,16,'Key has sixteen independently revealed bands');
  assert.ok(enumaTrace.includes('red branches begin tick=76') && enumaTrace.includes('red branches converge tick=172'));
  pass('Ea is fully drawn before the stance; rings and vortex reach both clients without spheres or flat ground marks');
  assert.ok(enumaTrace.includes('enuma impact tick=352'));assert.ok(renderedForBoth(enumaEntries));assert.ok(removedForBoth(enumaEntries));
  assert.deepEqual([a.health,b.health],enumaHealth);assert.ok(a.entity.position.distanceTo(hundredOrigin)<.01);
  const healthCheck=output.length;consoleCommand('execute if entity @e[tag=motionrp_target,nbt={Health:10.0f}] run say TREASURY_TARGET_UNHARMED');
  await waitFor(()=>output.slice(healthCheck).includes('TREASURY_TARGET_UNHARMED'),'target stays unharmed');
  await waitFor(()=>sampleType(a)==='stone' && sampleType(b)==='stone','ground restored after natural completion');
  pass('client-only excavation has actual depth and restores for both clients while the server world stays intact');
  pass('two hundred gates fire four hundred cosmetic shots as Bab-ilu and spinning Ea lead into a sixteen-part Enuma Elish vortex without damaging its target');
  sceneDisplays.clear();sceneRemoved.clear();const chargeCancel=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(chargeCancel).includes('TreasuryEnuma phase: EA_CHARGE'),'Ea charge before cancel',20000);
  const cancelledCharge=summonTrace('TreasuryEnuma',chargeCancel);a._testLeftClick();
  await waitFor(()=>removedForBoth(cancelledCharge) && sceneRemoved.size===sceneDisplays.size,'Ea charge cancel cleanup');
  const stoppedCharge=particles;await sleep(1200);assert.equal(particles,stoppedCharge);assert.ok(!output.slice(chargeCancel).includes('phase: ENUMA_ELISH'));
  pass('cancelling during Ea charging removes the key/weapon and every gate and prevents the beam');
  sceneDisplays.clear();sceneRemoved.clear();const beamCancel=output.length;a._testLeftClick();
  await waitFor(()=>sampleType(a)==='air' && sampleType(b)==='air','Enuma excavation before cancel',25000);
  const cancelledBeam=summonTrace('TreasuryEnuma',beamCancel);a._testLeftClick();
  await waitFor(()=>removedForBoth(cancelledBeam) && sceneRemoved.size===sceneDisplays.size,'Enuma beam cancel cleanup');
  const stoppedBeam=particles;await sleep(1000);assert.equal(particles,stoppedBeam);assert.ok(!output.slice(beamCancel).includes('enuma impact'));
  await waitFor(()=>sampleType(a)==='stone' && sampleType(b)==='stone','cancelled excavation restored');
  pass('cancelling the emitted Enuma vortex restores terrain for both clients, removes every segment and stops later impacts and particles');
  consoleCommand('kill @e[tag=motionrp_target]');
  assert.ok(!/Treasury100 scene failed|TreasuryEnuma scene failed/.test(output));
  await addSummon('zangetsu_bankai',8);
  sceneDisplays.clear();sceneRemoved.clear();const bankaiStart=output.length;observeBankai=true;
  a._testLeftClick();await waitFor(()=>output.slice(bankaiStart).includes('Bankai phase: TENSA_ZANGETSU'),'Tensa Zangetsu reveal',20000);
  const bankaiEntries=summonTrace('Bankai',bankaiStart);
  assert.ok(!bankaiEntries.some(e=>e.kind==='black_coat'));
  for(const kind of ['zangetsu','cocoon','pressure_wave','tensa_zangetsu']) assert.ok(bankaiEntries.some(e=>e.kind===kind));
  await waitFor(()=>renderedForBoth(bankaiEntries) && removedForBoth(bankaiEntries) && sceneRemoved.size===sceneDisplays.size,'Bankai natural cleanup',12000);
  observeBankai=false;
  for(const role of ['owner','observer']) {
    const packets=bankaiParticles[role],sum=type=>packets.filter(p=>p.type===type).reduce((n,p)=>n+p.amount,0);
    assert.ok(sum('dust_plume')>2000 && sum('cloud')>3000 && sum('block')>4000 && sum('flash')>=3,'Dense whiteout/dust reaches '+role);
    assert.ok(packets.some(p=>p.type==='dust_plume' && p.z>hundredOrigin.z+8),'Dust covers the forward area too');
  }
  const bankaiTrace=output.slice(bankaiStart);
  assert.ok(bankaiTrace.includes('whiteout and dust begin tick=104') && bankaiTrace.includes('dust clears; Tensa revealed tick=184'));
  assert.deepEqual([a.health,b.health],enumaHealth);
  pass('first Bankai braces the edge-down blade, whites out the caster and forward area with dense dust, then reveals Tensa Zangetsu on both clients');
  for(const phase of ['BANKAI','REVEAL']) {
    sceneDisplays.clear();sceneRemoved.clear();const start=output.length;a._testLeftClick();
    await waitFor(()=>output.slice(start).includes('Bankai phase: '+phase),'Bankai cancel phase '+phase,15000);
    const entries=summonTrace('Bankai',start);a._testLeftClick();
    await waitFor(()=>removedForBoth(entries) && sceneRemoved.size===sceneDisplays.size,'Bankai cancelled cleanup');
    const stopped=particles;await sleep(700);assert.equal(particles,stopped);
  }
  assert.ok(!output.includes('Bankai scene failed'));
  pass('cancelling Bankai during the pressure column or final reveal removes all models and stops the effect');
  for(const [id,prefix,slot,segments] of [['excalibur','Excalibur',9,14],['gae_bolg','GaeBolg',10,0]]) {
    await addSummon(id,slot);sceneDisplays.clear();sceneRemoved.clear();const begin=output.length,anchor=a.entity.position.clone(),healthBefore=[a.health,b.health];
    a._testLeftClick();await waitFor(()=>output.slice(begin).includes(prefix+' phase: FADE'),prefix+' completed Noble Phantasm',20000);
    const entries=summonTrace(prefix,begin);assert.ok(entries.some(e=>e.kind==='weapon'));assert.equal(entries.filter(e=>e.kind.startsWith('beam')).length,segments);
    if(id==='gae_bolg') {
      assert.ok(!entries.some(e=>e.kind==='heart_thorns'));
      const t=output.slice(begin);for(const line of ['dash complete distance=20','apex height=15','throw angle=120.0','impact spear explosion tick=60','landed forward=20','spear caught tick=86'])assert.ok(t.includes(line),line);
    }
    assert.ok(renderedForBoth(entries));await waitFor(()=>removedForBoth(entries) && sceneRemoved.size===sceneDisplays.size,prefix+' full cleanup');
    if(id==='excalibur')assert.ok(output.slice(begin).includes(prefix+' impact reach='));
    assert.deepEqual([a.health,b.health],healthBefore);assert.ok(a.entity.position.distanceTo(id==='gae_bolg'?anchor.offset(0,0,20):anchor)<.01);
    pass(prefix+' registers beyond slot nine, displays its weapon and full release on both clients and finishes without damage');
    sceneDisplays.clear();sceneRemoved.clear();const cancel=output.length,cancelOrigin=a.entity.position.clone();a._testLeftClick();
    await waitFor(()=>id==='gae_bolg'?a.entity.position.y>cancelOrigin.y+4:output.slice(cancel).includes(prefix+' phase: CHARGE'),prefix+' before cancellation');
    const cancelled=summonTrace(prefix,cancel);a._testLeftClick();await waitFor(()=>removedForBoth(cancelled) && sceneRemoved.size===sceneDisplays.size,prefix+' cancelled cleanup');
    const stopped=particles;await sleep(800);assert.equal(particles,stopped);assert.ok(!output.slice(cancel).includes(prefix+' impact'));
    if(id==='gae_bolg')assert.ok(a.entity.position.distanceTo(cancelOrigin)<.01,'Air cancellation restores safe ground');
    pass(prefix+' cancellation removes the held weapon and prevents the later release, particles and impact');
    assert.ok(!output.includes(prefix+' scene failed'));
  }
  // Scripted acceleration must not tunnel through walls, ceilings or missing ground.
  for(const obstruction of ['wall','ceiling','gap']) {
    const base=a.entity.position.clone(),x=Math.floor(base.x),y=Math.floor(base.y),z=Math.floor(base.z),begin=output.length;
    if(obstruction==='wall')consoleCommand(`fill ${x-1} ${y} ${z+5} ${x+1} ${y+3} ${z+5} minecraft:stone`);
    else if(obstruction==='ceiling')consoleCommand(`fill ${x-1} ${y+5} ${z+19} ${x+1} ${y+5} ${z+21} minecraft:stone`);
    else consoleCommand(`fill ${x-1} -64 ${z+5} ${x+1} ${y-1} ${z+6} minecraft:air`);
    await sleep(300);sceneDisplays.clear();sceneRemoved.clear();const health=[a.health,b.health];a._testLeftClick();
    await waitFor(()=>sceneDisplays.size>0 && sceneRemoved.size===sceneDisplays.size,'Gae Bolg '+obstruction+' abort',15000);
    assert.ok(a.entity.position.distanceTo(base)<.01);assert.deepEqual([a.health,b.health],health);
    assert.ok(!output.slice(begin).includes('throw angle='));assert.ok(!(await readSaved('MotionTesterA')).data.BukkitValues['motionrp:aerial_return_v1']);
    if(obstruction==='wall')consoleCommand(`fill ${x-1} ${y} ${z+5} ${x+1} ${y+3} ${z+5} minecraft:air`);
    else if(obstruction==='ceiling')consoleCommand(`fill ${x-1} ${y+5} ${z+19} ${x+1} ${y+5} ${z+21} minecraft:air`);
    else consoleCommand(`fill ${x-1} -64 ${z+5} ${x+1} ${y-1} ${z+6} minecraft:stone`);
    await sleep(300);pass('Gae Bolg '+obstruction+' stops physical travel, restores safe footing and removes the spear without damage');
  }
  a.setControlState('sneak',true);await sleep(300);
  for(let i=0;i<6;i++) {a._testLeftClick();await sleep(300);}
  a.setControlState('sneak',false);await sleep(300);
  assert.equal((await readSaved('MotionTesterA')).loadout[6],5);a._testLook(0,0);
  ({a,b}=await verifyTitanRecovery(a,b,{sceneDisplays,sceneRemoved,removedByRole,summonTrace,renderedForBoth,removedForBoth}));
  await disconnect(b);
  await disconnect(a);
  await stopServer();
  fs.writeFileSync(resultFile, JSON.stringify({ passed: checks.length, checks, limitations: [
    'Loopback offline 26.1 test clients via ViaVersion/ViaBackwards 5.12.2 to Paper 26.3; native 26.3 client behavior, Mojang authentication and Velocity login forwarding are not tested.',
    'Model packets are verified; client visual rendering and resource-pack acceptance require an actual Minecraft client.'
  ] }, null, 2));
  console.log('Integration checks completed:', checks.length);
} catch (error) {
  console.error(error.stack);
  fs.writeFileSync(resultFile, JSON.stringify({ passed: checks.length, checks, error: error.stack }, null, 2));
  process.exitCode = 1;
} finally {
  if (heightSampler) clearInterval(heightSampler);
  for (const bot of bots) { try { bot.quit(); bot._client.socket?.destroy(); } catch {} }
  try { await stopServer(); } catch (error) { console.error(error.message); server?.kill('SIGTERM'); process.exitCode = 1; }
  if (log) await new Promise(resolve => log.end(resolve));
  // Mineflayer can retain timers after a failed configuration/login handshake.
  // The child server has been stopped and the test outcome is already recorded.
  process.exit(process.exitCode || 0);
}
