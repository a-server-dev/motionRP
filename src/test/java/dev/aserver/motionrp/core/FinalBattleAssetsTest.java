package dev.aserver.motionrp.core;
import com.google.gson.*;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class FinalBattleAssetsTest {
 private JsonObject load(String f)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+f+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void allAvatarsAndProjectilesHaveValidTreesAndAnimationClocks()throws Exception{
  for(String name:List.of("duel_kurama","duel_asura_kurama","duel_susanoo","duel_indra_susanoo","duel_gold_rasenshuriken","duel_dark_rasenshuriken","duel_indra_arrow","duel_clash_orb","duel_chakra_spark","black_dragon_cloak","final_battle_naruto","final_battle_sasuke")){
   var m=load((name.startsWith("final_battle")?"players/":"models/")+"motionrp_"+name);Set<String> ids=new HashSet<>();for(String field:List.of("groups","elements"))for(var raw:m.getAsJsonArray(field))assertTrue(ids.add(raw.getAsJsonObject().get("uuid").getAsString()),name);tree(m.getAsJsonArray("outliner"),ids);
   for(var raw:m.getAsJsonArray("animations")){var a=raw.getAsJsonObject();for(var entry:a.getAsJsonObject("animators").entrySet()){assertTrue(ids.contains(entry.getKey()));double previous=-1;for(var k:entry.getValue().getAsJsonObject().getAsJsonArray("keyframes")){double t=k.getAsJsonObject().get("time").getAsDouble();assertTrue(t>=previous&&t<=a.get("length").getAsDouble(),name);previous=t;}}}
  }
 }
 private void tree(JsonArray nodes,Set<String> ids){for(var n:nodes)if(n.isJsonPrimitive())assertTrue(ids.contains(n.getAsString()));else{var g=n.getAsJsonObject();assertTrue(ids.contains(g.get("uuid").getAsString()));tree(g.getAsJsonArray("children"),ids);}}
 @Test void avatarsHaveNineTailsThreeHeadsSixArmsAndARealBow()throws Exception{
  var fox=load("models/motionrp_duel_asura_kurama");var names=fox.getAsJsonArray("groups").asList().stream().map(g->g.getAsJsonObject().get("name").getAsString()).toList();for(int i=0;i<9;i++)assertTrue(names.contains("nine_tail_"+i));for(int i=0;i<3;i++)assertTrue(names.contains("fox_head_"+i));for(int row=0;row<3;row++)for(int side:List.of(-1,1))assertTrue(names.contains("fox_upper_"+row+"_"+side));
  var s=load("models/motionrp_duel_indra_susanoo");assertTrue(s.getAsJsonArray("elements").asList().stream().anyMatch(e->e.getAsJsonObject().get("name").getAsString().startsWith("taut_bowstring")));
  for(String role:List.of("naruto","sasuke"))assertEquals(44,load("players/motionrp_final_battle_"+role).getAsJsonArray("animations").get(0).getAsJsonObject().get("length").getAsDouble());
 }
 @Test void sharedClockReturnsBothRolesAndKeepsTheirProjectilesSynchronized(){
  assertEquals(0,FinalBattleTimeline.projectile(559));assertEquals(1,FinalBattleTimeline.projectile(600));assertEquals("COLLISION",FinalBattleTimeline.phase(600));assertEquals("RETURN",FinalBattleTimeline.phase(800));assertEquals(0,FinalBattleTimeline.groundReturn(880));assertEquals(1,FinalBattleTimeline.form(140));assertEquals(20,FinalBattleTimeline.lift(320));assertEquals(58,FinalBattleTimeline.distance(240));assertEquals(38,FinalBattleTimeline.distance(180));
 }
 @Test void invitationForOneSceneCannotBeAcceptedAsTheOtherScene(){
  var sessions=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();assertTrue(sessions.invite(a,b,0,"finalbattle"));assertTrue(sessions.accept(b,a,1,"fusion").isEmpty());assertEquals("finalbattle",sessions.accept(b,a,2,"finalbattle").orElseThrow().kind());assertEquals(Set.of(a,b),sessions.cancel(a));
 }
 @Test void myooMatchesTheHumanWindupAndSweepsAcrossTheBody()throws Exception{
  var m=load("models/motionrp_tengen_myoo");var a=m.getAsJsonArray("animations").get(0).getAsJsonObject();var right=a.getAsJsonObject("animators").entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(t->t.get("name").getAsString().equals("sword_arm")).findFirst().orElseThrow();var ks=right.getAsJsonArray("keyframes");boolean high=false,left=false,rightward=false;for(var raw:ks){var p=raw.getAsJsonObject().getAsJsonArray("data_points").get(0).getAsJsonObject();high|=p.get("x").getAsDouble()>=135;left|=p.get("y").getAsDouble()<=-70;rightward|=p.get("y").getAsDouble()>=90;}assertTrue(high&&left&&rightward);
  assertEquals(320,EpicTimeline.end("kokuryuha"));assertEquals("RETURN",EpicTimeline.phase("kokuryuha",112));assertEquals("DEVOUR",EpicTimeline.phase("kokuryuha",172));assertEquals("CLOAK",EpicTimeline.phase("kokuryuha",208));
 }
 @Test void myooWalkingIsRestrictedToItsLegBones()throws Exception{
  for(String f:List.of("players/motionrp_kokujo_tengen_myoo","models/motionrp_tengen_myoo"))for(var raw:load(f).getAsJsonArray("animations"))assertTrue(raw.getAsJsonObject().get("override").getAsBoolean(),"Partial animations must skip unmentioned bones to preserve the attack clock");
 }
 @Test void myooUpperArmsUseExactlyTheHumanPoseThroughoutTheSweep()throws Exception{
  var human=load("players/motionrp_kokujo_tengen_myoo");var giant=load("models/motionrp_tengen_myoo");var math=new RasenganAssetsTest();
  Map<String,JsonObject> hg=new HashMap<>(),gg=new HashMap<>();Map<String,String> hn=new HashMap<>(),gn=new HashMap<>(),hp=new HashMap<>(),gp=new HashMap<>();
  for(var raw:human.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();hg.put(id,g);hn.put(g.get("name").getAsString(),id);}for(var raw:giant.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();gg.put(id,g);gn.put(g.get("name").getAsString(),id);}math.parentTree(human.getAsJsonArray("outliner"),null,hp);math.parentTree(giant.getAsJsonArray("outliner"),null,gp);
  var ha=human.getAsJsonArray("animations").get(0).getAsJsonObject();var ga=giant.getAsJsonArray("animations").get(0).getAsJsonObject();
  for(int tick=54;tick<=148;tick++)for(int side:List.of(-1,1)){
   var hs=math.posedOrigin(hn.get(side==1?"pra_right_arm":"pla_left_arm"),tick/20.0,ha,hg,hp);var he=math.posedOrigin(hn.get(side==1?"prfa_right_forearm":"plfa_left_forearm"),tick/20.0,ha,hg,hp);var gs=math.posedOrigin(gn.get(side==1?"sword_arm":"guard_arm"),(tick-54)/20.0,ga,gg,gp);var ge=math.posedOrigin(gn.get("forearm_"+side),(tick-54)/20.0,ga,gg,gp);
   for(int axis=0;axis<3;axis++)assertEquals(he[axis]-hs[axis],(ge[axis]-gs[axis])/4,.00001,"Arm direction drift at tick "+tick);
  }
 }

}
