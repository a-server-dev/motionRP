package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FootballAssetsTest {
 private JsonObject load(String id)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+id+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void actorsShareContactAndEndClocksAndKeepBothKneesArticulated()throws Exception{
  for(String id:List.of("fire_tornado","emperor_penguin_1","emperor_penguin_2_left","emperor_penguin_2_right")){
   var m=load("players/motionrp_"+id);var a=m.getAsJsonArray("animations").get(0).getAsJsonObject();assertEquals((id.contains("_2_")?FootballTimeline.END:FootballTimeline.end(id.equals("fire_tornado")))/20.0,a.get("length").getAsDouble());
   var tracks=a.getAsJsonObject("animators");
   for(String bone:List.of("player_root","prl_right_leg","pll_left_leg","prfl_right_foreleg","plfl_left_foreleg","pc_chest")){
    var g=m.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(b->b.get("name").getAsString().equals(bone)).findFirst().orElseThrow();
    var keys=tracks.getAsJsonObject(g.get("uuid").getAsString()).getAsJsonArray("keyframes");assertTrue(keys.size()>=2);
    if(bone.contains("foreleg"))for(var key:keys)assertTrue(key.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject().get("x").getAsDouble()<=0,"Knees must not bend forward");
    if(bone.contains("_leg"))assertTrue(keys.asList().stream().anyMatch(k->Math.abs(k.getAsJsonObject().get("time").getAsDouble()-(id.contains("_2_")?FootballTimeline.KICK:FootballTimeline.kick(id.equals("fire_tornado")))/20.0)<.001));
   }
  }
 }
 @Test void ballHasTwelvePentagonsAndTwentyHexagonsAndPenguinHasFaceAndFlippers()throws Exception{
  assertEquals(32,load("models/motionrp_football").getAsJsonArray("elements").size());
  var p=load("models/motionrp_emperor_penguin");var names=p.getAsJsonArray("elements").asList().stream().map(e->e.getAsJsonObject().get("name").getAsString()).toList();
  assertTrue(names.containsAll(List.of("white_belly","pointed_beak","eye_-1","eye_1","flipper_mesh_-1","flipper_mesh_1")));
  assertEquals("flap",p.getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
 }
 @Test void flightReturnsToGroundAndPairConsentCannotBeAcceptedAsAnotherMotion(){
  for(boolean fire:new boolean[]{true,false}){assertEquals(0,FootballTimeline.height(0,fire));assertEquals(0,FootballTimeline.height(FootballTimeline.land(fire),fire),1e-8);assertEquals(fire?3.2:0,FootballTimeline.height(90,fire),1e-8);}
  assertEquals(0,FootballTimeline.distance(FootballTimeline.KICK));assertEquals(42,FootballTimeline.distance(FootballTimeline.IMPACT));
  var sessions=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();assertTrue(sessions.invite(a,b,0,"emperor_penguin_2"));assertTrue(sessions.accept(b,a,1,"fusion").isEmpty());assertTrue(sessions.accept(b,a,1,"broly").isEmpty());assertTrue(sessions.accept(b,a,1,"emperor_penguin_2").isPresent());
 }

 @Test void referencePosesAndCalfAttachmentsReplaceTheOldBicycleKick()throws Exception{
  var no1=load("players/motionrp_emperor_penguin_1");
  String root=no1.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(g->g.get("name").getAsString().equals("player_root")).findFirst().orElseThrow().get("uuid").getAsString();
  for(var key:no1.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators").getAsJsonObject(root).getAsJsonArray("keyframes"))assertEquals("position",key.getAsJsonObject().get("channel").getAsString(),"No.1 keeps a planted support leg, not a full-body flip");
  var raised=FootballFootPath.at(false,75);assertTrue(raised.boot().forward()<-.1);assertTrue(raised.boot().height()>1.2);assertTrue(FootballFootPath.at(false,98).boot().forward()>.5);
  var fire=FootballFootPath.at(true,112);assertTrue(fire.boot().height()>1.3);assertTrue(fire.boot().forward()>.2);
  assertTrue(PairMotion.fromEffect("pair_emperor_penguin_2").isEmpty());assertNotNull(load("players/motionrp_emperor_penguin_2_left"));
  var penguin=load("models/motionrp_emperor_penguin_red");var source=penguin.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();var png=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(source.substring(source.indexOf(',')+1))));boolean red=false;for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++){int rgb=png.getRGB(x,y);if((rgb>>16&255)>150&&(rgb>>8&255)<50&&(rgb&255)<70)red=true;}assertTrue(red,"Reference penguins must be red");
 }
 @Test void shipped043AssetsAreSafeToUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_fire_tornado.bbmodel","players/motionrp_emperor_penguin_1.bbmodel","models/motionrp_football.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.43.0/"+name.replace('/','_')+".gz")))){String hash=BundledAssetUpdater.sha256(in.readAllBytes());assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(hash));}
 }
}
