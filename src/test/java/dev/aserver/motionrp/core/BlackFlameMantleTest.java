package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BlackFlameMantleTest {
 @Test void flamesFormATallUnevenCrownWithoutCoveringTheFrontOfTheBody(){
  for(int tick=208;tick<288;tick+=4){
   var points=BlackFlameMantle.samples(tick);
   assertTrue(points.stream().allMatch(p->p.forward()<-.4));
   assertTrue(points.stream().anyMatch(p->p.height()>3.2));
   assertTrue(points.stream().anyMatch(p->p.right()<-1.7));assertTrue(points.stream().anyMatch(p->p.right()>1.9));
   assertTrue(points.stream().noneMatch(p->Math.abs(p.right())<.4&&p.height()<2));
   assertTrue(points.stream().allMatch(p->p.height()<4&&Math.abs(p.right())<3.2),"Flames must stay compact");
   assertTrue(points.size()<300,"Bound per-viewer particle emission");
  }
 }
 @Test void absorbedCasterKeepsBothHandsBelowTheWaist()throws Exception{
  com.google.gson.JsonObject m;try(var in=java.util.Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/players/motionrp_kokuryuha.bbmodel"))){m=com.google.gson.JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
  var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();var math=new RasenganAssetsTest();
  java.util.Map<String,com.google.gson.JsonObject> groups=new java.util.HashMap<>();java.util.Map<String,String> names=new java.util.HashMap<>(),parents=new java.util.HashMap<>();
  for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}math.parentTree(m.getAsJsonArray("outliner"),null,parents);
  for(int tick=208;tick<320;tick+=4){
   var waist=math.posedOrigin(names.get("pw_waist"),tick/20.,clip,groups,parents);
   for(String hand:java.util.List.of("pri_right_item","pli_left_item")){var p=math.posedOrigin(names.get(hand),tick/20.,clip,groups,parents);assertTrue(p[1]<waist[1]-3.5,"Hands must hang down, not rest on the waist");}
  }
 }
}
