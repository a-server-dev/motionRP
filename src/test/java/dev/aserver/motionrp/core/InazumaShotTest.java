package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InazumaShotTest {
 private JsonObject load(String path)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+path+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void sourceEffectRangesAreStretchedByTwentyPercentAndHaveTimeForImpact(){
  for(var shot:InazumaShot.values()){
   assertEquals((shot.sourceEnd-shot.sourceStart)*1.2,shot.end/20.0,.026);
   assertTrue(shot.kick>20&&shot.impact>shot.kick&&shot.end>shot.impact);
   assertEquals(0,shot.height(0),1e-9);assertEquals(0,shot.height(shot.end-1),1e-9);
  }
  assertEquals(129,InazumaShot.DEATH_SWORD.end);assertEquals(132,InazumaShot.DEATH_SPEAR.end);assertEquals(158,InazumaShot.ETERNAL_BLIZZARD.end);
 }
 @Test void swordDarkensOnlyAfterItsPhysicalStrikeAndThenReleases(){
  var sword=InazumaShot.DEATH_SWORD;assertEquals(78,sword.strike());assertTrue(sword.strike()<sword.auraStart());assertTrue(sword.auraStart()<sword.kick);
  assertEquals("DARKEN",sword.phase(85));assertEquals("SHOT",sword.phase(110));
 }
 @Test void playerClipsHaveAnimatedTorsoBothLegsAndALiveBootAttachment()throws Exception{
  for(var shot:InazumaShot.values()){
   var m=load("players/motionrp_"+shot.id);var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();assertEquals(shot.end/20.0,clip.get("length").getAsDouble());
   var tracks=clip.getAsJsonObject("animators");
   for(String bone:List.of("pc_chest","pra_right_arm","pla_left_arm","prl_right_leg","prfl_right_foreleg","pll_left_leg","plfl_left_foreleg")){
    var g=m.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(x->x.get("name").getAsString().equals(bone)).findFirst().orElseThrow();
    var keys=tracks.getAsJsonObject(g.get("uuid").getAsString()).getAsJsonArray("keyframes");
    if(shot==InazumaShot.DEATH_SWORD&&Set.of("pll_left_leg","plfl_left_foreleg").contains(bone)){for(var k:keys)for(var v:k.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject().entrySet())assertEquals(0,v.getValue().getAsDouble());}
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
   for(int t=92;t<=108;t++)assertTrue(rows.get(t)<.12,"The kicking leg must be planted before the hand-only release");
  }
  var m=load("players/motionrp_death_sword");var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  var arm=tracks.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals("pra_right_arm")).findFirst().orElseThrow();
  var held=arm.getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).filter(k->Math.abs(k.get("time").getAsDouble()-4.5)<1e-6||Math.abs(k.get("time").getAsDouble()-4.8)<1e-6).toList();
  assertEquals(2,held.size());assertEquals(held.get(0).get("data_points"),held.get(1).get("data_points"));assertEquals(6,InazumaShot.DEATH_SWORD.handHoldEnd()-InazumaShot.DEATH_SWORD.handHoldStart());
 }
 @Test void blizzardUsesOneTiltedTurnAndAnIndependentWindEffect()throws Exception{
  var m=load("players/motionrp_eternal_blizzard");var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  var root=tracks.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals("player_root")).findFirst().orElseThrow();
  double maxYaw=0;boolean tilted=false;
  for(var raw:root.getAsJsonArray("keyframes")){var p=raw.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject();maxYaw=Math.max(maxYaw,p.get("y").getAsDouble());tilted|=Math.abs(p.get("x").getAsDouble())>=25&&Math.abs(p.get("z").getAsDouble())>=20;}
  assertEquals(360,maxYaw);assertTrue(tilted);
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
    if(Math.abs(tick-90)<1e-6){holdPitch+=p.get("x").getAsDouble();holdYaw+=p.get("y").getAsDouble();}
    if(Math.abs(tick-108)<1e-6){swingPitch+=p.get("x").getAsDouble();swingYaw+=p.get("y").getAsDouble();}
   }
  }
  assertTrue(Math.abs(holdPitch)<20);assertTrue(swingPitch<-60);assertTrue(holdYaw<-20&&swingYaw>20,"Chest and waist unwind across the slash");
 }
 @Test void blizzardFootScoopsUpThenDrawsBackForTheHeldKick()throws Exception{
  try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/eternal_blizzard-boot.csv"))){
   var feet=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toList();
   assertTrue(feet.get(26)[1]>feet.get(12)[1]+.5);assertTrue(feet.get(26)[2]>feet.get(12)[2]+.4);
   for(int t=100;t<=108;t++)assertTrue(feet.get(t)[1]<-.2,"Kicking boot is drawn back throughout the wind-up");
   assertTrue(feet.get(112)[1]>feet.get(108)[1]+.6,"Foot accelerates forward after the hold");
  }
 }
 @Test void old057ActorsAreRecognizedForUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_death_sword.bbmodel","players/motionrp_eternal_blizzard.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.57.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void projectileMeshesHaveDistinctSilhouettesAndNoSolidCubeStandins()throws Exception{
  for(String id:List.of("death_sword_edge","death_spear_drill","blizzard_orb","blizzard_flake","blizzard_wind","blizzard_wake")){
   var m=load("models/motionrp_"+id);assertTrue(m.getAsJsonArray("elements").size()>=5);
   for(var e:m.getAsJsonArray("elements"))assertEquals("mesh",e.getAsJsonObject().get("type").getAsString());
  }
 }
}
