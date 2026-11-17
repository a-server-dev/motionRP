import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
import { spawn, execFileSync } from 'node:child_process';
import mineflayer from 'mineflayer';
import nbt from 'prismarine-nbt';

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
  const legacyHash = '5c6a2b0425861d8fd648dfeacc5b61592ca2ef1c46da3ae6dfa55d08e46916d6';
  if (fs.existsSync(fixturePlayer) && createHash('sha256').update(fs.readFileSync(fixturePlayer)).digest('hex') === legacyHash) {
    const backup = path.join(run, 'plugins/MotionRP/model-backups', 'motionrp_rasengan.bbmodel.' + legacyHash + '.bak');
    assert.deepEqual(fs.readFileSync(backup), fs.readFileSync(fixturePlayer));
  }
  pass('installed player animation matches current bundle and an untouched legacy model is backed up during upgrade');
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
  await startServer();
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
  const itemMetadataIndex = b.registry.entitiesByName.item_display.metadataKeys.indexOf('item_stack');
  for (const [client, role] of [[a, 'owner'], [b, 'observer']]) {
    client._client.on('entity_metadata', packet => {
      for (const entry of packet.metadata) {
        if (entry.key === itemMetadataIndex) visibleItems[role].set(packet.entityId, entry.value);
      }
    });
  }
  let particles = 0;
  let sounds = 0;
  b._client.on('spawn_entity', packet => { if (packet.type === b.registry.entitiesByName.item_display.id) sceneDisplays.add(packet.entityId); });
  b._client.on('entity_destroy', packet => { for (const id of packet.entityIds) if (sceneDisplays.has(id)) sceneRemoved.add(id); });
  b._client.on('world_particles', () => particles++);
  b._client.on('sound_effect', () => sounds++);
  const health = b.health;
  let lowestHealth = health;
  const trackHealth = () => { lowestHealth = Math.min(lowestHealth, b.health); };
  b.on('health', trackHealth);
  const begin = output.length;
  a._testLeftClick();
  await waitFor(() => output.slice(begin).includes('Rasengan phase: CHARGE'), 'charge item metadata sample');
  await sleep(400);
  const chargeTrace = output.slice(begin);
  for (const role of ['owner', 'observer']) for (const kind of ['clone', 'orb']) {
    const ids = chargeTrace.match(new RegExp('Rasengan displays ' + kind + ' ids=([0-9,]+)'))[1].split(',').map(Number);
    assert.ok(ids.some(id => (visibleItems[role].get(id)?.itemCount || 0) > 0), role + '/' + kind + ' was empty while charging');
  }
  await waitFor(() => output.slice(begin).includes('Rasengan phase: DISSIPATE'), 'complete Rasengan choreography', 15_000);
  await waitFor(() => sceneDisplays.size > 12 && sceneRemoved.size === sceneDisplays.size, 'clone, orb and body cleanup', 10_000);
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
    sceneDisplays.clear(); sceneRemoved.clear();
    const replayBegin = output.length;
    a._testLeftClick();
    await waitFor(() => output.slice(replayBegin).includes('Rasengan phase: DISSIPATE'), 'consecutive Rasengan replay ' + replay, 15_000);
    await waitFor(() => sceneDisplays.size > 12 && sceneRemoved.size === sceneDisplays.size, 'consecutive replay cleanup');
    await sleep(300);
  }
  pass('three complete Rasengan plays plus cancellation can replay without relogging or reopening the GUI');

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
  for (const bot of bots) { try { bot.quit(); bot._client.socket?.destroy(); } catch {} }
  try { await stopServer(); } catch (error) { console.error(error.message); server?.kill('SIGTERM'); process.exitCode = 1; }
  if (log) await new Promise(resolve => log.end(resolve));
  // Mineflayer can retain timers after a failed configuration/login handshake.
  // The child server has been stopped and the test outcome is already recorded.
  process.exit(process.exitCode || 0);
}
