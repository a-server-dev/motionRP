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
    if(bone.contains("_leg"))assertTrue(keys.asList().stream().anyMatch(k->Math.abs(k.getAsJsonObject().get("time").getAsDouble()-(id.contains("_2_")?FootballTimeline.KICK:(FootballTimeline.kick(id.equals("fire_tornado"))+(id.equals("fire_tornado")?4:0)))/20.0)<.001));
   }
  }
 }
 @Test void fireMakesFourLocalAxisTurnsBeforeTheFasterKick()throws Exception{
  var m=load("players/motionrp_fire_tornado");var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  var spin=tracks.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals("football_spin")).findFirst().orElseThrow();
  double previous=0;int quarters=0;
  for(var raw:spin.getAsJsonArray("keyframes")){var k=raw.getAsJsonObject();double y=k.getAsJsonArray("data_points").get(0).getAsJsonObject().get("y").getAsDouble();assertTrue(y-previous<=90.001);if(y>previous)quarters++;previous=y;}
  assertEquals(1440,previous);assertEquals(16,quarters);assertEquals(112,FootballTimeline.end(true));assertEquals(68,FootballTimeline.kick(true));
 }
 @Test void entireBodyIncludingBothThighsSharesTheSpinParent()throws Exception{
  var model=load("players/motionrp_fire_tornado");var names=new HashMap<String,String>();
  for(var raw:model.getAsJsonArray("groups")){var g=raw.getAsJsonObject();names.put(g.get("uuid").getAsString(),g.get("name").getAsString());}
  var root=model.getAsJsonArray("outliner").get(0).getAsJsonObject();
  var bank=root.getAsJsonArray("children").asList().stream().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).filter(n->"football_bank".equals(names.get(n.get("uuid").getAsString()))).findFirst().orElseThrow();
  var spin=bank.getAsJsonArray("children").get(0).getAsJsonObject();assertEquals("football_spin",names.get(spin.get("uuid").getAsString()));
  var body=spin.getAsJsonArray("children").asList().stream().map(JsonElement::getAsJsonObject).map(n->names.get(n.get("uuid").getAsString())).collect(java.util.stream.Collectors.toSet());
  assertEquals(Set.of("phip_hip","prl_right_leg","pll_left_leg"),body,"Both thigh branches must rotate with the hip");
 }
 @Test void fireShootsWithTheLeftLegAndAttachesTheBallToThatBoot()throws Exception{
  var model=load("players/motionrp_fire_tornado");var groups=new HashMap<String,JsonObject>();
  for(var raw:model.getAsJsonArray("groups")){var g=raw.getAsJsonObject();groups.put(g.get("name").getAsString(),g);}
  var left=node(model.getAsJsonArray("outliner"),groups.get("plfl_left_foreleg").get("uuid").getAsString());
  assertNotNull(node(left.getAsJsonArray("children"),groups.get("football_boot").get("uuid").getAsString()));
  assertTrue(groups.get("football_boot").getAsJsonArray("origin").get(0).getAsDouble()<0);
  var tracks=model.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
  for(String name:List.of("pll_left_leg","prl_right_leg")){
   var keys=tracks.getAsJsonObject(groups.get(name).get("uuid").getAsString()).getAsJsonArray("keyframes");
   var contact=keys.asList().stream().map(JsonElement::getAsJsonObject).filter(k->Math.abs(k.get("time").getAsDouble()-3.6)<1e-6).findFirst().orElseThrow();
   double pitch=contact.getAsJsonArray("data_points").get(0).getAsJsonObject().get("x").getAsDouble();assertEquals(name.equals("pll_left_leg")?165:-20,pitch);
  }
 }
 private JsonObject node(JsonArray nodes,String id){
  for(var raw:nodes)if(raw.isJsonObject()){var n=raw.getAsJsonObject();if(n.get("uuid").getAsString().equals(id))return n;var child=node(n.getAsJsonArray("children"),id);if(child!=null)return child;}return null;
 }
 @Test void ballHasTwelvePentagonsAndTwentyHexagonsAndPenguinHasFaceAndFlippers()throws Exception{
  assertEquals(32,load("models/motionrp_football").getAsJsonArray("elements").size());
  var p=load("models/motionrp_emperor_penguin");var names=p.getAsJsonArray("elements").asList().stream().map(e->e.getAsJsonObject().get("name").getAsString()).toList();
  assertTrue(names.containsAll(List.of("white_belly","pointed_beak","eye_-1","eye_1","flipper_mesh_-1","flipper_mesh_1")));
  assertEquals("flap",p.getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
 }
 @Test void flightReturnsToGroundAndPairConsentCannotBeAcceptedAsAnotherMotion(){
  for(boolean fire:new boolean[]{true,false}){assertEquals(0,FootballTimeline.height(0,fire));assertEquals(0,FootballTimeline.height(FootballTimeline.land(fire),fire),1e-8);assertEquals(fire?FootballTimeline.FIRE_HEIGHT:0,FootballTimeline.height(fire?50:90,fire),1e-8);}
  assertEquals(0,FootballTimeline.distance(FootballTimeline.KICK));assertEquals(42,FootballTimeline.distance(FootballTimeline.IMPACT));
  var sessions=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();assertTrue(sessions.invite(a,b,0,"emperor_penguin_2"));assertTrue(sessions.accept(b,a,1,"fusion").isEmpty());assertTrue(sessions.accept(b,a,1,"broly").isEmpty());assertTrue(sessions.accept(b,a,1,"emperor_penguin_2").isPresent());
 }

 @Test void referencePosesAndCalfAttachmentsReplaceTheOldBicycleKick()throws Exception{
  var no1=load("players/motionrp_emperor_penguin_1");
  String root=no1.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(g->g.get("name").getAsString().equals("player_root")).findFirst().orElseThrow().get("uuid").getAsString();
  for(var key:no1.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators").getAsJsonObject(root).getAsJsonArray("keyframes"))assertEquals("position",key.getAsJsonObject().get("channel").getAsString(),"No.1 keeps a planted support leg, not a full-body flip");
  var raised=FootballFootPath.at(false,75);assertTrue(raised.boot().forward()<-.1);assertTrue(raised.boot().height()>1.2);assertTrue(FootballFootPath.at(false,98).boot().forward()>.5);
  var fire=FootballFootPath.at(true,FootballTimeline.kick(true)+4);assertTrue(fire.boot().height()>.8);assertTrue(fire.boot().forward()>.2);
  assertTrue(PairMotion.fromEffect("pair_emperor_penguin_2").isEmpty());assertNotNull(load("players/motionrp_emperor_penguin_2_left"));
  var penguin=load("models/motionrp_emperor_penguin_red");var source=penguin.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();var png=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(source.substring(source.indexOf(',')+1))));boolean red=false;for(int y=0;y<png.getHeight();y++)for(int x=0;x<png.getWidth();x++){int rgb=png.getRGB(x,y);if((rgb>>16&255)>150&&(rgb>>8&255)<50&&(rgb&255)<70)red=true;}assertTrue(red,"Reference penguins must be red");
 }
 @Test void shipped043AssetsAreSafeToUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_fire_tornado.bbmodel","players/motionrp_emperor_penguin_1.bbmodel","models/motionrp_football.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.43.0/"+name.replace('/','_')+".gz")))){String hash=BundledAssetUpdater.sha256(in.readAllBytes());assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(hash));}
 }
}
