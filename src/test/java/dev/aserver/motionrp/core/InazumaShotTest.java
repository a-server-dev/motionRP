package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InazumaShotTest {
 private JsonObject load(String path)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+path+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void sourceDurationsRespectTheSelectedReferenceAndHaveTimeForImpact(){
  for(var shot:InazumaShot.values()){
   if(shot==InazumaShot.DEATH_SPEAR)assertEquals((shot.sourceEnd-shot.sourceStart)*shot.durationScale,shot.end/20.0,.026);
   assertTrue(shot.kick>20&&shot.impact>shot.kick&&shot.end>shot.impact);
   assertEquals(0,shot.height(0),1e-9);assertEquals(0,shot.height(shot.end-1),1e-9);
  }
  assertEquals(90,InazumaShot.DEATH_SWORD.end);assertEquals(132,InazumaShot.DEATH_SPEAR.end);assertEquals(211,InazumaShot.ETERNAL_BLIZZARD.end);
 }
 @Test void swordDarkensOnlyAfterItsPhysicalStrikeAndThenReleases(){
  var sword=InazumaShot.DEATH_SWORD;assertEquals(39,sword.strike());assertTrue(sword.strike()<sword.auraStart());assertTrue(sword.auraStart()<sword.kick);
  assertEquals("DARKEN",sword.phase(46));assertEquals("SHOT",sword.phase(71));
 }
 @Test void playerClipsHaveAnimatedTorsoBothLegsAndALiveBootAttachment()throws Exception{
  for(var shot:InazumaShot.values()){
   var m=load("players/motionrp_"+shot.id);var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();assertEquals(shot.end/20.0,clip.get("length").getAsDouble());
   var tracks=clip.getAsJsonObject("animators");
   for(String bone:List.of("pc_chest","pra_right_arm","pla_left_arm","prl_right_leg","prfl_right_foreleg","pll_left_leg","plfl_left_foreleg")){
    var g=m.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(x->x.get("name").getAsString().equals(bone)).findFirst().orElseThrow();
    var keys=tracks.getAsJsonObject(g.get("uuid").getAsString()).getAsJsonArray("keyframes");
    if(shot==InazumaShot.DEATH_SWORD&&Set.of("pll_left_leg","plfl_left_foreleg").contains(bone)){for(var k:keys)if(k.getAsJsonObject().get("time").getAsDouble()<=2.05&&k.getAsJsonObject().get("channel").getAsString().equals("rotation"))for(var v:k.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject().entrySet())assertEquals(0,v.getValue().getAsDouble());}
    else assertTrue(keys.size()>=3);
   }
   assertTrue(m.getAsJsonArray("groups").asList().stream().anyMatch(g->g.getAsJsonObject().get("name").getAsString().equals("shot_boot")));
  }
 }
 @Test void swordKeepsTheSupportFootAndPilotAtGroundLevel()throws Exception{
  for(int t=0;t<InazumaShot.DEATH_SWORD.end;t++)assertEquals(0,InazumaShot.DEATH_SWORD.height(t));
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/death_sword-support.csv"))){
   var heights=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().mapToDouble(l->Double.parseDouble(l.split(",")[2])).toArray();
   assertTrue(Arrays.stream(heights).allMatch(h->h>=0&&h<.12));assertEquals(0,Arrays.stream(heights).max().orElseThrow()-Arrays.stream(heights).min().orElseThrow(),1e-8);
  }
 }
 @Test void old055SwordIsRecognizedForUpgrade()throws Exception{
  String name="players/motionrp_death_sword.bbmodel";try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.55.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void swordPlantsBothFeetDuringItsHeldHandChop()throws Exception{
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/death_sword-right.csv"))){
   var rows=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Double.parseDouble(l.split(",")[2])).toList();
   for(int t=53;t<=69;t++)assertTrue(rows.get(t)<.12,"The kicking leg must be planted before the hand-only release");
  }
  var m=load("players/motionrp_death_sword");var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  var arm=tracks.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals("pra_right_arm")).findFirst().orElseThrow();
  var held=arm.getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).filter(k->Math.abs(k.get("time").getAsDouble()-2.55)<1e-6||Math.abs(k.get("time").getAsDouble()-2.85)<1e-6).toList();
  assertEquals(2,held.size());assertEquals(held.get(0).get("data_points"),held.get(1).get("data_points"));assertEquals(6,InazumaShot.DEATH_SWORD.handHoldEnd()-InazumaShot.DEATH_SWORD.handHoldStart());
 }
 @Test void blizzardFacesForwardForTheFootCircleThenTurnsTwiceBeforeTheShot()throws Exception{
  var m=load("players/motionrp_eternal_blizzard");var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  var root=tracks.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals("player_root")).findFirst().orElseThrow();
  double maxYaw=0;boolean tilted=false;
  for(var raw:root.getAsJsonArray("keyframes")){var p=raw.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject();maxYaw=Math.max(maxYaw,p.get("y").getAsDouble());tilted|=Math.abs(p.get("x").getAsDouble())>=25&&Math.abs(p.get("z").getAsDouble())>=20;}
  assertEquals(720,maxYaw);assertTrue(tilted);
  var keys=root.getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).filter(k->k.get("channel").getAsString().equals("rotation")).toList();
  for(var pair:List.of(new double[]{17,0},new double[]{27,0},new double[]{70,0},new double[]{129,720})){
   var k=keys.stream().filter(x->Math.abs(x.get("time").getAsDouble()*20-pair[0])<1e-6).findFirst().orElseThrow();assertEquals(pair[1],k.getAsJsonArray("data_points").get(0).getAsJsonObject().get("y").getAsDouble());
  }
 }
 @Test void old056RevisionsAreRecognizedForUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_death_sword.bbmodel","players/motionrp_eternal_blizzard.bbmodel","models/motionrp_blizzard_orb.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.56.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void swordStaysUprightAndTwistedWhileHoldingThenFoldsWithTheChop()throws Exception{
  var m=load("players/motionrp_death_sword");var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  double holdPitch=0,swingPitch=0,holdYaw=0,swingYaw=0;
  for(String name:List.of("pw_waist","pc_chest")){
   var track=tracks.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals(name)).findFirst().orElseThrow();
   for(var raw:track.getAsJsonArray("keyframes")){var k=raw.getAsJsonObject();var p=k.getAsJsonArray("data_points").get(0).getAsJsonObject();double tick=k.get("time").getAsDouble()*20;
    if(Math.abs(tick-51)<1e-6){holdPitch+=p.get("x").getAsDouble();holdYaw+=p.get("y").getAsDouble();}
    if(Math.abs(tick-69)<1e-6){swingPitch+=p.get("x").getAsDouble();swingYaw+=p.get("y").getAsDouble();}
   }
  }
  assertTrue(Math.abs(holdPitch)<20);assertTrue(swingPitch<-60);assertTrue(holdYaw<-20&&swingYaw>20,"Chest and waist unwind across the slash");
 }
 @Test void blizzardFootScoopsUpThenDrawsBackForTheHeldKick()throws Exception{
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/eternal_blizzard-boot.csv"))){
   var feet=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toList();
   assertTrue(feet.get(10)[1]>feet.get(2)[1]+.5);assertTrue(feet.get(10)[2]>feet.get(2)[2]+.4);
   for(int t=143;t<=157;t++)assertTrue(feet.get(t)[1]<-.2,"Kicking boot is drawn back throughout the wind-up");
   assertTrue(feet.get(173)[1]>feet.get(157)[1]+.6,"Foot accelerates forward after the hold");
  }
 }
 @Test void old057ActorsAreRecognizedForUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_death_sword.bbmodel","players/motionrp_eternal_blizzard.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.57.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void blizzardOpposesItsFeetThenLandsBeforeTheMainJump()throws Exception{
  var paths=new ArrayList<List<double[]>>();
  for(String side:List.of("boot","other"))try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/eternal_blizzard-"+side+".csv"))){paths.add(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toList());}
  // The toe circles a free ball; the player does not stand on it or spin as a rigid pivot.
  double previous=0,total=0;
  for(int t=15;t<=27;t++){
   var r=paths.get(0).get(t);double x=r[0]-.1171875,y=r[2]-.03+InazumaShot.ETERNAL_BLIZZARD.height(t+2)-1.5;
   assertEquals(.48,Math.hypot(x,y),1e-6);assertEquals(.4,r[1],1e-7);
   double a=Math.atan2(y,x);if(t>15){double delta=a-previous;while(delta<0)delta+=Math.PI*2;total+=delta;}previous=a;
  }
  assertEquals(Math.PI*2,total,1e-6);
  for(int t=39;t<=69;t++){
   assertTrue(Math.abs(paths.get(0).get(t)[0]-paths.get(1).get(t)[0])>1);
   for(var path:paths)assertTrue(path.get(t)[2]>=0&&path.get(t)[2]<.12);
  }
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/eternal_blizzard-ground.csv"))){var h=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Double.parseDouble(l.split(",")[2])).toList();for(int t=39;t<=69;t++)assertTrue(h.get(t)>=0&&h.get(t)<.12,"Supporting palm touches the ground throughout the crouch");}
  var ice=InazumaShot.ETERNAL_BLIZZARD;assertTrue(ice.height(23)>.8);
  for(int t=39;t<=69;t++)assertEquals(0,ice.height(t),1e-9);
  assertTrue(ice.height(88)>0);assertEquals(3.0,ice.height(125),1e-9);
 }
 @Test void old058ActorsAreRecognizedForUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_death_sword.bbmodel","players/motionrp_eternal_blizzard.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.58.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void swordHoldsItsTwistedFinishingPoseUntilTheEnd()throws Exception{
  var tracks=load("players/motionrp_death_sword").getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  for(String name:List.of("pc_chest","pw_waist","pra_right_arm","pla_left_arm")){
   var track=tracks.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals(name)).findFirst().orElseThrow();
   var keys=track.getAsJsonArray("keyframes");assertEquals(keys.get(keys.size()-2).getAsJsonObject().get("data_points"),keys.get(keys.size()-1).getAsJsonObject().get("data_points"));
  }
 }
 @Test void swordPlantsTheRightFootInFrontImmediatelyAfterTheKick()throws Exception{
  var feet=new ArrayList<List<double[]>>();for(String name:List.of("right","support"))try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/death_sword-"+name+".csv"))){feet.add(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toList());}
  for(int t=55;t<90;t++){assertTrue(feet.get(0).get(t)[1]>feet.get(1).get(t)[1]+.4);for(var path:feet)assertTrue(path.get(t)[2]<.12);}
 }
 @Test void old059ActorsAreRecognizedForUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_death_sword.bbmodel","players/motionrp_eternal_blizzard.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.59.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void swordRestoresAHighStraightKickAndLowersItsPelvisWithoutLiftingTheFeet()throws Exception{
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/death_sword-right.csv"))){var feet=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toList();assertTrue(feet.get(39)[2]>1.0,"Front kick reaches waist/chest height");assertTrue(feet.get(39)[1]>.6);}
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/death_sword-hip.csv"))){var h=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Double.parseDouble(l.split(",")[2])).toList();assertTrue(h.get(81)<h.get(39)-.2,"Pelvis drops over 20 cm as both knees bend");assertTrue(h.get(47)>h.get(55));}
 }
 @Test void old060ActorsAreRecognizedForUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_death_sword.bbmodel","players/motionrp_eternal_blizzard.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.60.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void blizzardCutsItsIntroSpeedsUpOnlyBeforeTurningAndLowersTheFreeArm()throws Exception{
  assertEquals(0,BlizzardTimeline.at(32));assertEquals(69,BlizzardTimeline.TURN);assertEquals(58,BlizzardTimeline.at(194)-BlizzardTimeline.TURN);assertEquals(173,BlizzardTimeline.KICK);assertEquals("CHARGE",InazumaShot.ETERNAL_BLIZZARD.phase(0));
  List<double[]> hand,shoulder;
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/eternal_blizzard-trail.csv"))){hand=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toList();}
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/eternal_blizzard-shoulder.csv"))){shoulder=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toList();}
  for(int t=41;t<=69;t++){double length=0;for(int i=0;i<3;i++)length+=Math.pow(hand.get(t)[i]-shoulder.get(t)[i],2);double vertical=(hand.get(t)[2]-shoulder.get(t)[2])/Math.sqrt(length);assertTrue(vertical>.4&&vertical<.85,"Free arm extends diagonally, not vertically");}
 }
 @Test void old061BlizzardIsRecognizedForUpgrade()throws Exception{
  String name="players/motionrp_eternal_blizzard.bbmodel";try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.61.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void swordDoublesOnlyItsApproachAndKeepsItsPostKickIntervals(){
  assertEquals(39,SwordShotTimeline.STRIKE);assertEquals(90,SwordShotTimeline.END);
  assertEquals(110-78,SwordShotTimeline.SHOT-SwordShotTimeline.STRIKE);assertEquals(129-78,SwordShotTimeline.END-SwordShotTimeline.STRIKE);
 }
 @Test void old062ActorsAreRecognizedForUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_death_sword.bbmodel","players/motionrp_eternal_blizzard.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.62.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void projectileMeshesHaveDistinctSilhouettesAndNoSolidCubeStandins()throws Exception{
  for(String id:List.of("death_sword_edge","death_spear_drill","blizzard_orb","blizzard_flake","blizzard_wind","blizzard_wake")){
   var m=load("models/motionrp_"+id);assertTrue(m.getAsJsonArray("elements").size()>=5);
   for(var e:m.getAsJsonArray("elements"))assertEquals("mesh",e.getAsJsonObject().get("type").getAsString());
  }
 }
}
