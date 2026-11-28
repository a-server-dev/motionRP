package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TackleAndArmorTest {
 private JsonObject model(String path)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+path+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void swordAndBothFistsStayOutsideTheBreastplateDuringFollowThrough()throws Exception{
  var m=model("models/motionrp_tengen_myoo");var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();var math=new RasenganAssetsTest();
  Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
  for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}math.parentTree(m.getAsJsonArray("outliner"),null,parents);
  clip.getAsJsonObject("animators").remove(names.get("battle_torso")); // evaluate in torso-local space
  var blade=m.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject).filter(e->e.get("name").getAsString().equals("giant_sword_blade_forged")).findFirst().orElseThrow();
  for(int tick=70;tick<=166;tick++){
   for(int side:new int[]{-1,1}){
    var p=math.posedPoint(names.get("forearm_"+side),new double[]{side*29,-15,-1},tick/20.,clip,groups,parents);
    assertTrue(clearance(p,28,23,18)>1,"Fist intersects breastplate at "+tick);
   }
   for(var v:blade.getAsJsonObject("vertices").entrySet()){
    var a=v.getValue().getAsJsonArray();var p=math.posedPoint(names.get("forearm_1"),new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble()},tick/20.,clip,groups,parents);
    assertTrue(clearance(p,22,18,12)>1,"Blade intersects breastplate at "+tick);
   }
  }
 }
 private double clearance(double[] p,double x,double y,double z){return Math.pow(p[0]/x,2)+Math.pow((p[1]-18)/y,2)+Math.pow(p[2]/z,2);}
 @Test void napePathMatchesTheAnimatedTackleAndFeetRemainGrounded()throws Exception{
  var m=model("players/motionrp_titan_transform");var clip=m.getAsJsonArray("animations").asList().stream().map(JsonElement::getAsJsonObject).filter(a->a.get("name").getAsString().equals("titan_tackle")).findFirst().orElseThrow();var math=new RasenganAssetsTest();
  Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
  for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}math.parentTree(m.getAsJsonArray("outliner"),null,parents);
  for(int age=0;age<=108;age++){
   var p=math.posedPoint(names.get("h_ph_head"),new double[]{0,23,4.2},age/20.,clip,groups,parents);var baked=TitanTacklePath.at(TitanTimeline.TACKLE+age);
   assertArrayEquals(new double[]{p[0]*10/16,-p[2]*10/16,p[1]*10/16},new double[]{baked.right(),baked.forward(),baked.height()},1e-6);
   double low=100;
   for(int side:new int[]{-1,1})for(double x:new double[]{side<0?-3.75:0,side<0?0:3.75})for(double y:new double[]{0,5.625})for(double z:new double[]{-1.875,1.875})low=Math.min(low,math.posedPoint(names.get(side<0?"plfl_left_foreleg":"prfl_right_foreleg"),new double[]{x,y,z},age/20.,clip,groups,parents)[1]);
   assertEquals(0,low,1e-6,"Feet must stay at ground level");
  }
  assertTrue(TitanTacklePath.at(TitanTimeline.EXIT).height()<9,"Nape must descend into a low tackle");
  var shoulder=math.posedPoint(names.get("pc_chest"),new double[]{0,22,0},2.5,clip,groups,parents);
  var hip=math.posedOrigin(names.get("phip_hip"),2.5,clip,groups,parents);
  assertTrue(shoulder[2]<hip[2]-7,"Shoulders must lead the hips, not remain upright");
  assertEquals(14,TitanTimeline.tackleDistance(TitanTimeline.DRIVE+32));
  assertEquals(-5,TitanTimeline.exitPose(TitanTimeline.LAND,0).forward());
 }
}
