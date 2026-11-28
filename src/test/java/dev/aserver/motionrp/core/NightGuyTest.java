package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class NightGuyTest {
 @Test void sprintCoversFiftyBlocksInTwentyTicksAndEndsWithoutRetreat(){
  double previous=0;for(int tick=0;tick<NightGuyTimeline.END;tick++){
   double d=NightGuyTimeline.forward(tick,50,28);assertTrue(d>=previous&&d<=78);previous=d;
   assertTrue(NightGuyTimeline.height(tick)>=0&&NightGuyTimeline.height(tick)<=4.6);
  }
  assertEquals(50,NightGuyTimeline.forward(NightGuyTimeline.DASH,50,28));assertEquals(78,NightGuyTimeline.forward(NightGuyTimeline.IMPACT,50,28));
  for(int tick=NightGuyTimeline.RUN;tick<NightGuyTimeline.LEAP;tick++)assertEquals(2.5,NightGuyTimeline.sprint(tick+1,50)-NightGuyTimeline.sprint(tick,50),1e-9);
  assertEquals(20,NightGuyTimeline.LEAP-NightGuyTimeline.RUN);
  assertEquals(0,NightGuyTimeline.height(NightGuyTimeline.LANDED));assertEquals("DASH",NightGuyTimeline.phase(NightGuyTimeline.DASH));assertEquals("FINISHED",NightGuyTimeline.phase(NightGuyTimeline.END));
 }
 @Test void rightHeelLeadsTheHeadAndTheOppositeLegStaysBent()throws Exception{
  JsonObject m;try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/players/motionrp_night_guy.bbmodel"))){m=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
  var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();assertEquals("night_guy",clip.get("name").getAsString());assertEquals(NightGuyTimeline.END/20.,clip.get("length").getAsDouble());
  var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
  for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}math.parentTree(m.getAsJsonArray("outliner"),null,parents);
  for(int tick=147;tick<=154;tick++){
   var foot=math.posedPoint(names.get("prfl_right_foreleg"),new double[]{1.875,.3,-1.875},tick/20.,clip,groups,parents);
   var head=math.posedOrigin(names.get("h_ph_head"),tick/20.,clip,groups,parents);
   var tucked=math.posedPoint(names.get("plfl_left_foreleg"),new double[]{-1.875,.3,-1.875},tick/20.,clip,groups,parents);
   assertTrue(foot[2]<head[2]-8,"Heel must lead the torso and head into the kick");assertTrue(foot[2]<tucked[2]-6,"Opposite leg must remain tucked");
  }
  for(var raw:clip.getAsJsonObject("animators").entrySet())for(var key:raw.getValue().getAsJsonObject().getAsJsonArray("keyframes"))assertTrue(key.getAsJsonObject().get("time").getAsDouble()<=NightGuyTimeline.END/20.);
 }
 @Test void sprintKeepsOneFootOnTheGroundWhileAlternatingLegs()throws Exception{
  JsonObject m;try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/players/motionrp_night_guy.bbmodel"))){m=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
  var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
  for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}math.parentTree(m.getAsJsonArray("outliner"),null,parents);
  for(int tick=NightGuyTimeline.RUN;tick<NightGuyTimeline.LEAP;tick++){
   double low=100;for(int side:new int[]{-1,1})for(double x:new double[]{side<0?-3.75:0,side<0?0:3.75})for(double y:new double[]{0,5.625})for(double z:new double[]{-1.875,1.875})low=Math.min(low,math.posedPoint(names.get(side<0?"plfl_left_foreleg":"prfl_right_foreleg"),new double[]{x,y,z},tick/20.,clip,groups,parents)[1]);
   assertEquals(0,low,1e-5,"Sprint must keep a foot planted at tick "+tick);
  }
  var right=math.posedOrigin(names.get("prfl_right_foreleg"),4.8,clip,groups,parents);var next=math.posedOrigin(names.get("prfl_right_foreleg"),4.9,clip,groups,parents);assertTrue(Math.abs(right[2]-next[2])>5,"Legs must alternate rapidly");
 }
}
