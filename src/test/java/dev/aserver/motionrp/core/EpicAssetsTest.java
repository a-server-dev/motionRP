package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EpicAssetsTest {
 private JsonObject model(String file)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+file+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void everyNewModelHasValidTreeAndBoundedAnimationKeys()throws Exception{
  var files=new ArrayList<String>();for(String id:EpicTimeline.EFFECTS)files.add("players/motionrp_"+id);files.addAll(List.of("players/motionrp_fusion_left","players/motionrp_fusion_right"));
  for(String id:List.of("the_world","road_roller","black_dragon_head","black_dragon_body","tengen_myoo","gravity_core","gravity_rock","rock_moon","fusion_warrior"))files.add("models/motionrp_"+id);
  for(String file:files){var m=model(file);Set<String> ids=new HashSet<>();for(var raw:m.getAsJsonArray("groups"))assertTrue(ids.add(raw.getAsJsonObject().get("uuid").getAsString()));for(var raw:m.getAsJsonArray("elements"))assertTrue(ids.add(raw.getAsJsonObject().get("uuid").getAsString()));tree(m.getAsJsonArray("outliner"),ids);
   for(var raw:m.getAsJsonArray("animations")){var a=raw.getAsJsonObject();for(var track:a.getAsJsonObject("animators").entrySet()){assertTrue(ids.contains(track.getKey()));double previous=-1;for(var frame:track.getValue().getAsJsonObject().getAsJsonArray("keyframes")){double t=frame.getAsJsonObject().get("time").getAsDouble();assertTrue(t>=previous&&t>=0&&t<=a.get("length").getAsDouble(),file+" timeline");previous=t;}}}
  }
 }
 private void tree(JsonArray nodes,Set<String> ids){for(var n:nodes)if(n.isJsonPrimitive())assertTrue(ids.contains(n.getAsString()));else{var g=n.getAsJsonObject();assertTrue(ids.contains(g.get("uuid").getAsString()));tree(g.getAsJsonArray("children"),ids);}}
 @Test void fusionMirrorsBothWristsIntoOneContactPoint()throws Exception{
  for(String role:List.of("fusion_left","fusion_right")){
   var m=model("players/motionrp_"+role);var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
   for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}math.parentTree(m.getAsJsonArray("outliner"),null,parents);
   for(String hand:List.of("pri_right_item","pli_left_item")){
    var p=math.posedOrigin(names.get(hand),4.8,clip,groups,parents);assertEquals(role.endsWith("left")?1.1:-1.1,p[0]/16,.00001);assertEquals(17.2/16,p[1]/16,.00001);assertEquals(0,p[2],.00001);
   }
  }
 }
 @Test void modelsContainRecognizableMechanicalAndCreatureDetails()throws Exception{
  for(var check:Map.of("road_roller","radiator_grille","the_world","forehead_heart","black_dragon_head","swept_horn","tengen_myoo","giant_sword_blade").entrySet()){
   var m=model("models/motionrp_"+check.getKey());assertTrue(m.getAsJsonArray("elements").asList().stream().anyMatch(e->e.getAsJsonObject().get("name").getAsString().startsWith(check.getValue())));
  }
  assertEquals(10,EpicTimeline.moonScale(160));assertEquals(36,EpicTimeline.dragonDistance(95));assertTrue(EpicTimeline.rollerHeight(70)>20);assertTrue(EpicTimeline.rollerHeight(104)<.2);
 }
}
