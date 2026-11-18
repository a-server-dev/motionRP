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
  return bot;
}
function tokens(bot) { return bot.inventory.items().filter(item => item.name === 'paper').reduce((sum, item) => sum + item.count, 0); }
function registered(bot) { return bot.currentWindow.slots.slice(0, 9).filter(item => item?.name === 'paper').length; }
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
  const index = bot.currentWindow.slots.findIndex((item, i) => i >= 9 && item?.name === 'paper');
  assert.ok(index >= 9, 'Motion token missing from storage');
  await click(bot, index);
}
async function disconnect(bot) { bot.quit(); await sleep(500); }
async function readSaved(name) {
  const { parsed } = await nbt.parse(fs.readFileSync(path.join(run, 'world/players/data', uuid(name) + '.dat')));
  const data = nbt.simplify(parsed);
  const bytes = data.BukkitValues?.['motionrp:loadout_v1'];
  assert.ok(bytes, 'Player PDC loadout is missing');
  return { data, loadout: Buffer.from(bytes) };
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
  const oldSummons=[['players/motionrp_gate_of_babylon_enuma','65690445a739722ab052f571ceccd0476f2fab7ce106f2795cb724c45d5a199c','0.8.0'],['models/motionrp_ea','26bf036c8d1230c4da474034a86dc1afbe88f500237d9d5872c8a58318bb3e6e','0.8.0'],['models/motionrp_enuma_vortex','9ddebc5469047326fbff87242a29e64723952d0147512c4cf6339643042ecb0e','0.8.0'],['players/motionrp_titan_transform','bfbc83f290b4252a4088fd825e0fca0bed7a4a019e74f4305d33a791d3f20857','0.6.0'],['players/motionrp_gate_of_babylon','2e0bae16f4570116a383ccaa91e09319564665870b34c7cc03a9c156c34c2272','0.5.0'],
    ['players/motionrp_senbonzakura','7a38d287afad202d93644f1d54ba0267baa33eff01315860675bd940c5944afe','0.5.0'],
    ['models/motionrp_senbonzakura_blade','aebce2d6a72045a193e54ebaad4e7cf8fd579ccc19aba3182b659c5f5ed44d70','0.5.0'],
    ['models/motionrp_susanoo_armor','d054d6147211c1cc72e6281ee5b9cc1a3ddcda8d9da6548e4472affedb16040c'],
    ['models/motionrp_susanoo_ribs','74993bf47a065b5da8cb50a3ceccfbfc6bf6fa5c13d6bd14c2c804773e1db912']];
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
  fs.writeFileSync(priorConfig,fs.readFileSync(priorConfig,'utf8').replace(/(  senbonzakura:\n[\s\S]*?max-ticks:) \d+/, '$1 180').replace(/(  gate_of_babylon_enuma:\n[\s\S]*?max-ticks:) \d+/, '$1 280'));
  await startServer();
  assert.ok(/senbonzakura:\n[\s\S]*?max-ticks: 260/.test(fs.readFileSync(priorConfig,'utf8')));
  assert.ok(/gate_of_babylon_enuma:\n[\s\S]*?max-ticks: 320/.test(fs.readFileSync(priorConfig,'utf8')));
  pass('Enuma old timeout upgrades to fit the longer charging performance');
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
  const itemMetadataIndex = b.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack');
  for (const [client, role] of [[a, 'owner'], [b, 'observer']]) {
    client._client.on('entity_destroy', packet => { for (const id of packet.entityIds) removedByRole[role].add(id); });
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
  assert.equal(blastTimes.length,20);assert.ok(blastTimes.every(t=>t>=84));
  const gateBlockBegin=output.length;
  consoleCommand(`execute positioned ${gateOrigin.x} ${gateOrigin.y-1} ${gateOrigin.z+20} if block ~ ~ ~ minecraft:stone run say GATE_GROUND_UNCHANGED`);
  await waitFor(()=>output.slice(gateBlockBegin).includes('GATE_GROUND_UNCHANGED'),'explosive volley keeps the impact floor');
  pass('only the final volley makes twenty cosmetic explosions and leaves the impact blocks intact');
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
  await waitFor(() => output.slice(susanooBegin).includes('Susanoo phase: FADE'),'guardian slash completed',15000);
  const guardianEntries=summonTrace('Susanoo',susanooBegin);
  assert.deepEqual(guardianEntries.map(e => e.kind),['ribs','armor','slash']);
  assert.ok(renderedForBoth(guardianEntries));
  const armorIds=guardianEntries.find(e => e.kind==='armor').ids;
  assert.ok(armorIds.some(id => (rotations.get(id)?.size||0)>2),'Giant sword arm sent no changing rotations');
  await waitFor(() => sceneRemoved.size===sceneDisplays.size && removedForBoth(guardianEntries),'guardian cleanup for both viewers');
  for(const phase of ['SIGNS','RIBS','ARMOR','WINDUP','SLASH','IMPACT','FADE'])
    assert.ok(output.slice(susanooBegin).includes('Susanoo phase: '+phase));
  assert.ok(a.entity.position.distanceTo(guardianOrigin)<.01);assert.deepEqual([a.health,b.health],guardianHealth);
  pass('ribs form into giant samurai armor, its sword arm animates, a slash travels outward and every model disappears without damage');
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
  assert.deepEqual(hundredFires,[...Array.from({length:100},(_,i)=>[i,42+Math.floor(i*30/100)]),...Array.from({length:100},(_,i)=>[i,84])]);
  assert.equal([...hundredTrace.matchAll(/Treasury100 impact index=/g)].length,200);
  assert.ok(hundredTrace.includes('volley blast tick=86'));assert.ok(removedForBoth(hundredEntries));
  assert.ok(a.entity.position.distanceTo(hundredOrigin)<.01);assert.deepEqual([a.health,b.health],hundredHealth);
  pass('all hundred gates shoot in thirty rapid ticks then join one hundred-shot volley with a sand/dust blast and full cleanup');
  sceneDisplays.clear();sceneRemoved.clear();const hundredCancel=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(hundredCancel).includes('Treasury100 fire index=0'),'hundred gate rapid before cancel',20000);
  const hundredCancelled=summonTrace('Treasury100',hundredCancel);a._testLeftClick();
  await waitFor(()=>removedForBoth(hundredCancelled) && sceneRemoved.size===sceneDisplays.size,'hundred gate rapid cancel');
  const stoppedHundred=particles;await sleep(700);assert.equal(particles,stoppedHundred);assert.ok(!output.slice(hundredCancel).includes('volley blast'));
  pass('cancelling the hundred-gate rapid pass removes all flying and loaded weapons and prevents the final blast');

  await addSummon('gate_of_babylon_enuma',7);
  consoleCommand(`summon minecraft:cow ${hundredOrigin.x} ${hundredOrigin.y} ${hundredOrigin.z+20} {NoAI:1b,NoGravity:1b,Silent:1b,Tags:["motionrp_target"]}`);await sleep(500);
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
  await waitFor(()=>output.slice(enumaBegin).includes('TreasuryEnuma phase: FADE'),'Enuma Elish and impact',30000);
  await waitFor(()=>sceneRemoved.size===sceneDisplays.size,'Ea/beam/gates cleanup',15000);
  const enumaTrace=output.slice(enumaBegin),enumaEntries=summonTrace('TreasuryEnuma',enumaBegin);
  const enumaFires=[...enumaTrace.matchAll(/TreasuryEnuma fire index=(\d+) tick=(\d+)/g)].map(m=>[Number(m[1]),Number(m[2])]);
  assert.deepEqual(enumaFires,[...Array.from({length:200},(_,i)=>[i,42+Math.floor(i*30/200)]),...Array.from({length:200},(_,i)=>[i,84])]);
  assert.equal([...enumaTrace.matchAll(/TreasuryEnuma impact index=/g)].length,400);
  for(const kind of ['bab_ilu','hand_vault','ea']) assert.ok(enumaEntries.some(e=>e.kind===kind));
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('beam')).length,16);
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('charge_ring')).length,2);
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('shock_ring')).length,3);
  assert.equal(enumaEntries.filter(e=>e.kind.startsWith('rift')).length,8);
  assert.ok(enumaEntries.some(e=>e.kind==='charge_core') && enumaEntries.some(e=>e.kind==='impact_core'));
  pass('white-gold core, gyroscopic rings, three traveling shock rings and eight ground rifts reach both clients');
  assert.ok(enumaTrace.includes('enuma impact tick=204'));assert.ok(renderedForBoth(enumaEntries));assert.ok(removedForBoth(enumaEntries));
  assert.deepEqual([a.health,b.health],enumaHealth);assert.ok(a.entity.position.distanceTo(hundredOrigin)<.01);
  const healthCheck=output.length;consoleCommand('execute if entity @e[tag=motionrp_target,nbt={Health:10.0f}] run say TREASURY_TARGET_UNHARMED');
  await waitFor(()=>output.slice(healthCheck).includes('TREASURY_TARGET_UNHARMED'),'target stays unharmed');
  pass('two hundred gates fire four hundred cosmetic shots as Bab-ilu and spinning Ea lead into a sixteen-part Enuma Elish vortex without damaging its target');
  sceneDisplays.clear();sceneRemoved.clear();const chargeCancel=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(chargeCancel).includes('TreasuryEnuma phase: EA_CHARGE'),'Ea charge before cancel',20000);
  const cancelledCharge=summonTrace('TreasuryEnuma',chargeCancel);a._testLeftClick();
  await waitFor(()=>removedForBoth(cancelledCharge) && sceneRemoved.size===sceneDisplays.size,'Ea charge cancel cleanup');
  const stoppedCharge=particles;await sleep(1200);assert.equal(particles,stoppedCharge);assert.ok(!output.slice(chargeCancel).includes('phase: ENUMA_ELISH'));
  pass('cancelling during Ea charging removes the key/weapon and every gate and prevents the beam');
  sceneDisplays.clear();sceneRemoved.clear();const beamCancel=output.length;a._testLeftClick();
  await waitFor(()=>output.slice(beamCancel).includes('TreasuryEnuma displays beam3'),'Enuma beam before cancel',25000);
  const cancelledBeam=summonTrace('TreasuryEnuma',beamCancel);a._testLeftClick();
  await waitFor(()=>removedForBoth(cancelledBeam) && sceneRemoved.size===sceneDisplays.size,'Enuma beam cancel cleanup');
  const stoppedBeam=particles;await sleep(1000);assert.equal(particles,stoppedBeam);assert.ok(!output.slice(beamCancel).includes('enuma impact'));
  pass('cancelling the emitted Enuma vortex removes every segment and stops later impacts and particles');
  consoleCommand('kill @e[tag=motionrp_target]');
  assert.ok(!/Treasury100 scene failed|TreasuryEnuma scene failed/.test(output));
  a.setControlState('sneak',true);await sleep(300);
  for(let i=0;i<6;i++) {a._testLeftClick();await sleep(300);}
  a.setControlState('sneak',false);await sleep(300);
  assert.equal((await readSaved('MotionTesterA')).loadout[6],5);a._testLook(0,0);
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
  assert.ok(renderedForBoth(titanEntries),'Lightning or replacement skin model was not sent');
  assert.ok(scaleUpdates.some(v=>Math.abs(v-10)<.001),'Observer received no real scale-10 attribute: '+scaleUpdates);
  assert.ok(output.slice(titanBegin).includes('skin=bundled:titan.png'),'Uploaded embedded skin is not selected');
  assert.ok(profileUpdates.every(urls=>urls.length===0),'Bundled giant must preserve the original player profile');
  const pending=await readSaved('MotionTesterA');assert.equal(pending.data.BukkitValues['motionrp:titan_scale_v1'],1.25);
  assert.ok(nativeLightning.size>=4,'No vanilla lightning entities reached the observer');
  pass('hand bite triggers native lightning/flash, a giant skin model, real scale 10 and the exact embedded user skin');
  await waitFor(()=>output.slice(titanBegin).includes('Titan phase: EXIT'),'human exits the nape',20000);
  assert.ok(a.entity.position.y>titanOrigin.y+13,'The real player did not emerge at the nape');
  assert.ok(Math.abs(scaleUpdates.at(-1)-1.25)<.001);assert.equal((profileUpdates.at(-1)?.length??0),0);
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
  assert.ok(Math.abs(scaleUpdates.at(-1)-1.25)<.001);assert.equal((profileUpdates.at(-1)?.length??0),0);
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
