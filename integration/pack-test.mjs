import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {spawn,execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import mineflayer from 'mineflayer';

// Protocol/lifecycle test only. Seed a previously verified publication cache;
// GitHub HTTP upload and anonymous download are tested separately with a local fake API.
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const repository=fs.readFileSync(path.join(project,'src/main/resources/config.yml'),'utf8').match(/^  repository: (.+)$/m)[1];
const fixture=process.env.MOTIONRP_PAPER_FIXTURE || '/workspace/runtime/paper';
const previous=path.join(project,'build/integration-server');
const run=path.join(project,'build/pack-test-server'),port=25577;
const version=fs.readFileSync(path.join(project,'build.gradle'),'utf8').match(/^version = '([^']+)'/m)[1];
let server,bot,other,output='';const checks=[],offers=[],removals=[],messages=[];
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function waitFor(predicate,label,timeout=20000) {const end=Date.now()+timeout;while(Date.now()<end){if(predicate())return;await sleep(100);}throw Error('Timeout: '+label+'\n'+output.slice(-2000));}
const pass=s=>{checks.push(s);console.log('PASS:',s);};
const command=s=>server.stdin.write(s+'\n');
const pluginDir=path.join(run,'plugins/MotionRP');
function publication(revision) {
  const target=path.join(pluginDir,'mortionRP.zip');
  execFileSync('python3',['-c',`import zipfile,sys
with zipfile.ZipFile(sys.argv[1]) as old, zipfile.ZipFile(sys.argv[2],'w',zipfile.ZIP_DEFLATED) as new:
 for name in old.namelist(): new.writestr(name,old.read(name))
 new.writestr('motionrp_test_revision.txt',sys.argv[3])`,path.join(previous,'plugins/BetterModel/build.zip'),target,revision]);
  const data=fs.readFileSync(target),sha1=createHash('sha1').update(data).digest('hex'),sha256=createHash('sha256').update(data).digest('hex');
  fs.writeFileSync(path.join(pluginDir,'pack-publication.properties'),`repository=${repository}\nsha1=${sha1}\nsha256=${sha256}\n`);
  return {sha1,sha256,url:`https://github.com/${repository}/releases/download/motionrp-pack-${sha256}/mortionRP.zip`};
}
try {
  assert.ok(fs.existsSync(path.join(previous,'plugins/BetterModel/build.zip')),'Run npm test once to prepare the isolated Paper fixture');
  fs.rmSync(run,{recursive:true,force:true});fs.mkdirSync(path.join(run,'plugins'),{recursive:true});
  for(const name of ['libraries','cache','versions']) fs.symlinkSync(path.join(fixture,name),path.join(run,name),'dir');
  for(const name of ['BetterModel.jar','ViaVersion.jar','ViaBackwards.jar']) fs.copyFileSync(path.join(previous,'plugins',name),path.join(run,'plugins',name));
  fs.cpSync(path.join(previous,'plugins/BetterModel'),path.join(run,'plugins/BetterModel'),{recursive:true});
  fs.cpSync(path.join(previous,'world'),path.join(run,'world'),{recursive:true});
  fs.copyFileSync(path.join(project,`build/libs/motionRP-${version}.jar`),path.join(run,'plugins/MotionRP.jar'));
  fs.mkdirSync(pluginDir,{recursive:true});
  const configPath=path.join(pluginDir,'config.yml');
  const config=fs.readFileSync(path.join(project,'src/main/resources/config.yml'),'utf8').replace('enabled: false','enabled: true');
  fs.writeFileSync(configPath,config.replace('name: ロードローラー\n','name: ロードローラーだッ！（ザ・ワールド）\n').replace('name: 岩盤\n','name: ブロリー 岩盤（二人用）\n').replace('name: 王の財宝(慢心)\n','name: 王の財宝\n').replace('name: 王の財宝(本気)\n','name: 王の財宝・本気（エヌマエリシュ）\n'));
  const initial=publication('initial');
  let properties=fs.readFileSync(path.join(previous,'server.properties'),'utf8').replace(/^server-port=.*$/m,`server-port=${port}`)
    .replace(/^resource-pack=.*$/m,'resource-pack=https://example.invalid/base.zip').replace(/^resource-pack-sha1=.*$/m,'resource-pack-sha1='+'1'.repeat(40));
  fs.writeFileSync(path.join(run,'server.properties'),properties);fs.writeFileSync(path.join(run,'eula.txt'),'eula=true\n');
  const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin/java'):'java';
  const opts=(process.env.GRADLE_OPTS||'').trim().split(/\s+/).filter(Boolean);
  server=spawn(java,[...opts,'-XX:ActiveProcessorCount=2','-Xms512M','-Xmx2G','-jar',path.join(fixture,'paper-26.3-253.jar'),'--nogui'],
    {cwd:run,stdio:['pipe','pipe','pipe'],env:{...process.env,MOTIONRP_GITHUB_TOKEN:''}});
  for(const stream of [server.stdout,server.stderr]) stream.on('data',data=>{output+=data;});
  await waitFor(()=>output.includes('Done (') || server.exitCode!==null,'Paper startup',120000);
  assert.equal(server.exitCode,null);assert.ok(output.includes('MotionRP enabled:'));
  bot=mineflayer.createBot({host:'127.0.0.1',port,username:'PackTester',auth:'offline',version:'26.1',physicsEnabled:false});
  bot.physicsEnabled=false;
  const write=bot._client.write.bind(bot._client);
  bot._client.write=(name,packet)=>{
    if(['position','position_look','look','flying'].includes(name)) return;
    return write(name,packet);
  };
  bot.on('error',e=>console.error(e.message));bot.on('message',m=>messages.push(m.toString()));
  bot._client.on('add_resource_pack',packet=>{
    offers.push(packet);
    if(bot._client.state==='configuration') {
      // Mineflayer injects some plugins after configuration; answer the base pack early.
      bot._client.write('resource_pack_receive',{uuid:packet.uuid,result:3});
      bot._client.write('resource_pack_receive',{uuid:packet.uuid,result:0});
    }
  });
  bot._client.on('remove_resource_pack',packet=>removals.push(packet));
  await waitFor(()=>bot.entity,'first player spawn');
  const otherOffers=[],otherMessages=[];
  other=mineflayer.createBot({host:'127.0.0.1',port,username:'PackOther',auth:'offline',version:'26.1',physicsEnabled:false});other.physicsEnabled=false;
  const otherWrite=other._client.write.bind(other._client);other._client.write=(name,packet)=>{if(['position','position_look','look','flying'].includes(name))return;return otherWrite(name,packet);};
  other.on('error',e=>console.error(e.message));other.on('message',m=>otherMessages.push(m.toString()));
  other._client.on('add_resource_pack',p=>{otherOffers.push(p);if(other._client.state==='configuration'){other._client.write('resource_pack_receive',{uuid:p.uuid,result:3});other._client.write('resource_pack_receive',{uuid:p.uuid,result:0});}});
  await waitFor(()=>other.entity,'second player spawn');await sleep(3000);
  assert.ok(!offers.some(p=>p.url===initial.url));assert.ok(!otherOffers.some(p=>p.url===initial.url));
  pass('Neither joining player receives the MotionRP pack automatically');
  bot.chat('/motionrp reload');await waitFor(()=>offers.some(p=>p.url===initial.url),'individual reload offer');
  const first=offers.find(p=>p.url===initial.url);assert.equal(first.hash,initial.sha1);assert.equal(first.forced,false);
  assert.ok(offers.some(p=>p.url==='https://example.invalid/base.zip'));
  assert.ok(removals.every(p=>p.uuid===first.uuid),'Another plugin/server pack was removed');
  assert.ok(!otherOffers.some(p=>p.url===initial.url));
  pass('A non-admin reload sends the verified pack only to its requester and preserves the base pack');
  bot.chat('/motionrp reload');await waitFor(()=>offers.filter(p=>p.url===initial.url).length===2,'explicit reapply of unchanged pack');await sleep(1000);
  assert.ok(!otherOffers.some(p=>p.url===initial.url));pass('Explicit reload reapplies an unchanged pack without sending to anyone else');
  const migrated=fs.readFileSync(configPath,'utf8');for(const label of ['ロードローラー','岩盤','王の財宝(慢心)','王の財宝(本気)','卍解・天鎖斬月','卍解・千本桜景義','突き穿つ死翔の槍'])assert.ok(migrated.includes('name: '+label));
  pass('Existing shipped motion names migrate to the requested labels');
  bot.chat('/motionrp view on');await sleep(250);
  const displays=new Set(),destroyed=new Set();bot._client.on('spawn_entity',p=>{if(p.type===bot.registry.entitiesByName.item_display.id)displays.add(p.entityId);});bot._client.on('entity_destroy',p=>p.entityIds.forEach(id=>destroyed.add(id)));
  const before=messages.length;bot.chat('/goalperformance kazu');await waitFor(()=>displays.size>0,'performance visibly begins');assert.ok(!messages.slice(before).some(m=>m.includes('再生:')));
  const active=[...displays],offerCount=offers.length;bot.chat('/motionrp reload');await waitFor(()=>offers.length>offerCount,'reload during performance');await sleep(250);assert.ok(active.some(id=>!destroyed.has(id)),'Personal reload must leave the active motion running');bot.chat('/goalperformance stop');
  pass('Successful playback stays silent and a personal pack reload leaves its active model running');
  command('motionrp pack publish');await waitFor(()=>output.includes('パックの確認と公開を予約しました'),'manual publication command');
  command('motionrp pack status');await waitFor(()=>output.includes('リソースパック: GitHub認証未設定'),'manual status command');
  pass('manual publication/status commands schedule work without blocking or exposing credentials');
  bot._client.write('resource_pack_receive',{uuid:first.uuid,result:2});
  await waitFor(()=>messages.some(m=>m.includes('パックの適用に失敗')),'download failure message');
  pass('failed downloads produce actionable player feedback');
  const failedNotice=messages.length;bot.chat('/goalperformance kazu');await waitFor(()=>messages.slice(failedNotice).some(m=>m.includes('近くでモーションが再生')&&m.includes('リソパが未適用')),'nearby failed-pack reminder');bot.chat('/goalperformance stop');pass('A known failed pack triggers a nearby-playback reminder with a reapply action');
  other.chat('/motionrp reload');await waitFor(()=>otherOffers.some(p=>p.url===initial.url),'other player explicitly requests old pack');other._client.write('resource_pack_receive',{uuid:first.uuid,result:0});await sleep(300);

  const next=publication('next');const count=offers.length;command('motionrp configreload');await sleep(1500);assert.equal(offers.length,count);assert.ok(!otherOffers.some(p=>p.url===next.url));bot.chat('/motionrp reload');
  await waitFor(()=>offers.some(p=>p.url===next.url),'updated cached publication offer');
  const second=offers.find(p=>p.url===next.url);assert.equal(second.uuid,first.uuid);assert.equal(second.hash,next.sha1);
  assert.ok(removals.every(p=>p.uuid===first.uuid));assert.ok(!removals.some(p=>p.uuid===undefined));
  assert.ok(!otherOffers.some(p=>p.url===next.url));pass('A new cached publication stays unsent after config reload and reaches only the player who requests it');
  const staleNotice=otherMessages.length;other.chat('/goalperformance kazu');await waitFor(()=>otherMessages.slice(staleNotice).some(m=>m.includes('最新リソパが未適用')),'loaded old pack warning');other.chat('/goalperformance stop');pass('A confirmed loaded older pack triggers an update reminder after publication changes, without auto-applying the new pack');
  fs.writeFileSync(path.join(pluginDir,'github-token.txt'),'TEST_SECRET_NEVER_LOG\ninvalid-second-line');command('motionrp configreload');
  await waitFor(()=>output.includes('自動公開失敗（IOException）'),'invalid credential rejected');
  assert.ok(!output.includes('TEST_SECRET_NEVER_LOG'));
  pass('invalid token files are rejected without exposing their contents');
  fs.unlinkSync(path.join(pluginDir,'github-token.txt'));
  fs.writeFileSync(configPath,config.replace(`repository: ${repository}`,'repository: ../invalid'));command('motionrp configreload');
  await waitFor(()=>output.includes('Resource pack settings are invalid'),'invalid optional config');
  command('motionrp status');await waitFor(()=>output.includes('BetterModel プレイヤーモデル'),'motion service still enabled');
  pass('invalid automatic-pack configuration does not disable the motion plugin');
  bot.quit();bot=undefined;other.quit();other=undefined;command('stop');await waitFor(()=>server.exitCode!==null,'shutdown',30000);assert.equal(server.exitCode,0);
  fs.writeFileSync(path.join(project,'build/pack-integration-result.json'),JSON.stringify({passed:checks.length,checks,limitations:['Previously verified cache is simulated. Real GitHub upload and anonymous download use the local fake API in Java tests. Minecraft visual rendering is not tested.']},null,2));
  console.log('Pack integration checks completed:',checks.length);
} catch(error) {console.error(error.stack);process.exitCode=1;}
finally {
  if(bot){bot.quit();bot._client.socket?.destroy();}
  if(other){other.quit();other._client.socket?.destroy();}
  if(server && server.exitCode===null) {command('stop');try{await waitFor(()=>server.exitCode!==null,'cleanup',30000);}catch{server.kill('SIGKILL');}}
  fs.writeFileSync(path.join(project,'build/pack-integration-server.log'),output);
  process.exit(process.exitCode||0);
}
