package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class HollowMaskTest {
 private JsonObject read(String file)throws Exception{try(var in=getClass().getResourceAsStream("/bettermodel/"+file+".bbmodel")){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void openEyesHaveVisibleWhiteHeight()throws Exception{
  var image=ImageIO.read(getClass().getResourceAsStream("/bettermodel/textures/motionrp_hollow_mask.png"));
  for(int x:new int[]{40,87}){int white=0;for(int y=50;y<88;y++){int c=image.getRGB(x,y);if((c>>>16&255)>248 && (c>>>8&255)>248 && (c&255)>248)white++;}assertTrue(white>=10,"Eyes must be open rather than a one-row slit");}
 }
 @Test void maskFacesAreInvisibleFromTheFirstPersonEyeThroughoutTheSwing()throws Exception{
  var mask=read("models/motionrp_hollow_mask");var actor=read("players/motionrp_getsuga_tensho");var clip=actor.getAsJsonArray("animations").get(0).getAsJsonObject();
  Map<String,JsonObject> groups=new HashMap<>();Map<String,String> parents=new HashMap<>();String head=null;
  for(var raw:actor.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);if(g.get("name").getAsString().equals("h_ph_head"))head=id;}
  var math=new RasenganAssetsTest();math.parentTree(actor.getAsJsonArray("outliner"),null,parents);
  for(int tick:new int[]{12,26,44,72,94,100,140,175})for(var raw:mask.getAsJsonArray("elements")){
   var mesh=raw.getAsJsonObject();Map<String,double[]> world=new HashMap<>();
   for(var entry:mesh.getAsJsonObject("vertices").entrySet()){var v=entry.getValue().getAsJsonArray();world.put(entry.getKey(),math.posedPoint(head,new double[]{v.get(0).getAsDouble(),22.5+v.get(1).getAsDouble(),v.get(2).getAsDouble()},tick/20.0,clip,groups,parents));}
   Set<Set<String>> seen=new HashSet<>();
   for(var face:mesh.getAsJsonObject("faces").entrySet()){
    var ids=face.getValue().getAsJsonObject().getAsJsonArray("vertices").asList().stream().map(JsonElement::getAsString).toList();assertTrue(seen.add(Set.copyOf(ids)),"Do not add reversed/back faces");
    var p=world.get(ids.get(0));var q=world.get(ids.get(1));var r=world.get(ids.get(2));
    double[] a={q[0]-p[0],q[1]-p[1],q[2]-p[2]},b={r[0]-p[0],r[1]-p[1],r[2]-p[2]};
    double[] n={a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
    for(double eyeHeight:new double[]{25.92,20.32})assertTrue(n[0]*-p[0]+n[1]*(eyeHeight-p[1])+n[2]*-p[2]<0,"Mask front face must not face the owner's first-person camera");
   }
  }
 }
}
