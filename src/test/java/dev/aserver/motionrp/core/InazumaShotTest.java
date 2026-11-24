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
  assertEquals("DARKEN",sword.phase(85));assertEquals("SHOT",sword.phase(104));
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
 @Test void projectileMeshesHaveDistinctSilhouettesAndNoSolidCubeStandins()throws Exception{
  for(String id:List.of("death_sword_edge","death_spear_drill","blizzard_orb","blizzard_flake")){
   var m=load("models/motionrp_"+id);assertTrue(m.getAsJsonArray("elements").size()>=5);
   for(var e:m.getAsJsonArray("elements"))assertEquals("mesh",e.getAsJsonObject().get("type").getAsString());
  }
 }
}
