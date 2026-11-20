package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class InfinityCastleAssetsTest {
 private JsonObject load(String file)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+file+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void floorPortalIsHorizontalWithoutRelyingOnEntityPitch()throws Exception{
  var m=load("models/motionrp_castle_hatch");double[] min={1e9,1e9,1e9},max={-1e9,-1e9,-1e9};
  for(var raw:m.getAsJsonArray("elements")){var e=raw.getAsJsonObject();for(int i=0;i<3;i++){min[i]=Math.min(min[i],e.getAsJsonArray("from").get(i).getAsDouble());max[i]=Math.max(max[i],e.getAsJsonArray("to").get(i).getAsDouble());}if(e.get("name").getAsString().startsWith("painted_paper"))assertTrue(e.getAsJsonObject("faces").has("up"));}
  assertTrue(max[1]-min[1]<6,"Floor thickness must stay below half a block");assertTrue(max[0]-min[0]>64);assertTrue(max[2]-min[2]>48);
  for(var raw:m.getAsJsonArray("animations")){var a=raw.getAsJsonObject();for(var t:a.getAsJsonObject("animators").entrySet())for(var k:t.getValue().getAsJsonObject().getAsJsonArray("keyframes")){var f=k.getAsJsonObject();assertEquals("position",f.get("channel").getAsString());var p=f.getAsJsonArray("data_points").get(0).getAsJsonObject();assertEquals(0,p.get("y").getAsDouble());assertEquals(0,p.get("z").getAsDouble());}}
 }
 @Test void fallRotatesTheWholeBodyFaceUpAndRestoresUprightForLanding()throws Exception{
  var m=load("players/motionrp_infinity_castle");String root=m.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(g->g.get("name").getAsString().equals("player_root")).findFirst().orElseThrow().get("uuid").getAsString();
  var keys=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators").getAsJsonObject(root).getAsJsonArray("keyframes");boolean falling=false,landed=false;
  for(var raw:keys){var k=raw.getAsJsonObject();if(!k.get("channel").getAsString().equals("rotation"))continue;double t=k.get("time").getAsDouble(),x=k.getAsJsonArray("data_points").get(0).getAsJsonObject().get("x").getAsDouble();if(t>=1&&t<=8.8){assertTrue(Math.sin(Math.toRadians(x))>.97,"Face normal must point up; back faces the ground");falling=true;}if(t>=9.9){assertEquals(0,x,1e-6);landed=true;}}
  assertTrue(falling&&landed);
 }
}
