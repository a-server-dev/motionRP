package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FootballAssetsTest {
 private JsonObject load(String id)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+id+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void actorsShareContactAndEndClocksAndKeepBothKneesArticulated()throws Exception{
  for(String id:List.of("fire_tornado","emperor_penguin_1","emperor_penguin_2_left","emperor_penguin_2_right")){
   var m=load("players/motionrp_"+id);var a=m.getAsJsonArray("animations").get(0).getAsJsonObject();assertEquals(FootballTimeline.END/20.0,a.get("length").getAsDouble());
   var tracks=a.getAsJsonObject("animators");
   for(String bone:List.of("player_root","prl_right_leg","pll_left_leg","prfl_right_foreleg","plfl_left_foreleg","pc_chest")){
    var g=m.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(b->b.get("name").getAsString().equals(bone)).findFirst().orElseThrow();
    var keys=tracks.getAsJsonObject(g.get("uuid").getAsString()).getAsJsonArray("keyframes");assertTrue(keys.size()>=2);
    if(bone.contains("foreleg"))for(var key:keys)assertTrue(key.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject().get("x").getAsDouble()<=0,"Knees must not bend forward");
    if(bone.contains("_leg"))assertTrue(keys.asList().stream().anyMatch(k->Math.abs(k.getAsJsonObject().get("time").getAsDouble()-FootballTimeline.KICK/20.0)<.001));
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
  for(boolean fire:new boolean[]{true,false}){assertEquals(0,FootballTimeline.height(0,fire));assertEquals(0,FootballTimeline.height(FootballTimeline.LAND,fire),1e-8);assertTrue(FootballTimeline.height(90,fire)>1.8);}
  assertEquals(0,FootballTimeline.distance(FootballTimeline.KICK));assertEquals(42,FootballTimeline.distance(FootballTimeline.IMPACT));
  var sessions=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();assertTrue(sessions.invite(a,b,0,"emperor_penguin_2"));assertTrue(sessions.accept(b,a,1,"fusion").isEmpty());assertTrue(sessions.accept(b,a,1,"broly").isEmpty());assertTrue(sessions.accept(b,a,1,"emperor_penguin_2").isPresent());
 }
}
