package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EpicAssetsTest {
 private JsonObject model(String file)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+file+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void everyNewModelHasValidTreeAndBoundedAnimationKeys()throws Exception{
  var files=new ArrayList<String>();for(String id:EpicTimeline.EFFECTS)files.add("players/motionrp_"+id);files.addAll(List.of("players/motionrp_fusion_left","players/motionrp_fusion_right"));
  for(String id:List.of("the_world","road_roller","black_dragon_head","black_dragon_body","tengen_myoo","gravity_core","gravity_rock","rock_moon","fusion_warrior","roller_debris"))files.add("models/motionrp_"+id);
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
    var p=math.posedOrigin(names.get(hand),6.0,clip,groups,parents);assertEquals(role.endsWith("left")?1.75:-1.75,p[0]/16,.00001);assertEquals(1.0,p[1]/16,.00001);assertEquals(0,p[2],.00001);
   }
  }
 }
 @Test void modelsContainRecognizableMechanicalAndCreatureDetails()throws Exception{
  for(var check:Map.of("road_roller","radiator_grille","the_world","forehead_heart","black_dragon_head","swept_horn","tengen_myoo","giant_sword_blade").entrySet()){
   var m=model("models/motionrp_"+check.getKey());assertTrue(m.getAsJsonArray("elements").asList().stream().anyMatch(e->e.getAsJsonObject().get("name").getAsString().startsWith(check.getValue())));
  }
  assertEquals(10,EpicTimeline.moonScale(200));assertEquals(36,EpicTimeline.dragonDistance(95));assertTrue(EpicTimeline.rollerHeight(70)==16);assertTrue(EpicTimeline.rollerHeight(104)<.2);
 }
 @Test void fusedAvatarEmbedsTheExactProvidedSkin()throws Exception{
  var m=model("models/motionrp_fusion_warrior");String source=m.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
  try(var in=getClass().getResourceAsStream("/skins/fusion.png")){assertArrayEquals(in.readAllBytes(),Base64.getDecoder().decode(source.substring(source.indexOf(',')+1)));}
  assertEquals("fusion_idle",m.getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
 }
 @Test void retainedRocksCoverTheWholeSphere(){
  for(int i=0;i<96;i++){var n=EpicTimeline.rockNormal(i);assertEquals(1,Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]),1e-9);}
  assertEquals(260,EpicTimeline.end("chibaku_tensei"));assertEquals(216,FusionTimeline.END);
 }

 @Test void fusionHeadsStayOnTheirOwnSideThroughoutTheLean()throws Exception{
  var math=new RasenganAssetsTest();
  for(String role:List.of("fusion_left","fusion_right")){
   var m=model("players/motionrp_"+role);var clip=m.getAsJsonArray("animations").get(0).getAsJsonObject();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
   for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String uid=g.get("uuid").getAsString();groups.put(uid,g);names.put(g.get("name").getAsString(),uid);}math.parentTree(m.getAsJsonArray("outliner"),null,parents);
   boolean left=role.endsWith("left");
   for(int tick=0;tick<=FusionTimeline.FUSED;tick++)for(double x:new double[]{-4.2,4.2})for(double y:new double[]{22.05,30.45})for(double z:new double[]{-4.2,4.2}){
    double worldX=math.posedPoint(names.get("h_ph_head"),new double[]{x,y,z},tick/20.0,clip,groups,parents)[0]/16+(left?-FusionTimeline.HALF_SPACING:FusionTimeline.HALF_SPACING);
    assertTrue(left?worldX<0:worldX>0,role+" head crosses partner at tick "+tick);
   }
  }
 }

 @Test void releasedAssetsUpgradeWithBackupsAndPreserveCustomModels()throws Exception{
  var root=java.nio.file.Files.createTempDirectory("motionrp-022-upgrade");
  for(String file:List.of("models/motionrp_fusion_warrior.bbmodel","models/motionrp_road_roller.bbmodel","models/motionrp_rock_moon.bbmodel","models/motionrp_the_world.bbmodel","players/motionrp_chibaku_tensei.bbmodel","players/motionrp_fusion_left.bbmodel","players/motionrp_fusion_right.bbmodel","players/motionrp_road_roller.bbmodel")){
   byte[] old,current;try(var in=getClass().getResourceAsStream("/bundled-0.21.0/"+file.replace('/','_')+".gz");var gz=new java.util.zip.GZIPInputStream(in)){old=gz.readAllBytes();}
   try(var in=getClass().getResourceAsStream("/bettermodel/"+file)){current=in.readAllBytes();}
   var target=root.resolve(file);java.nio.file.Files.createDirectories(target.getParent());java.nio.file.Files.write(target,old);var updater=new BundledAssetUpdater();var known=BundledAssetVersions.knownHashes(file,Set.of());
   assertEquals(BundledAssetUpdater.Result.UPDATED,updater.install(target,current,known,root.resolve("backups"),root.resolve("updates")));assertArrayEquals(current,java.nio.file.Files.readAllBytes(target));
   String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(old));assertArrayEquals(old,java.nio.file.Files.readAllBytes(root.resolve("backups").resolve(target.getFileName()+"."+hash+".bak")));
   java.nio.file.Files.writeString(target,"custom");assertEquals(BundledAssetUpdater.Result.CUSTOM_PRESERVED,updater.install(target,current,known,root.resolve("backups"),root.resolve("updates")));
  }
 }

 @Test void playerAndStandPunchWithTheSameAlternatingArmMotion()throws Exception{
  var actor=model("players/motionrp_road_roller");var stand=model("models/motionrp_the_world");var math=new RasenganAssetsTest();
  Map<String,JsonObject> ag=new HashMap<>(),sg=new HashMap<>();Map<String,String> ap=new HashMap<>(),sp=new HashMap<>(),names=new HashMap<>();
  for(var raw:actor.getAsJsonArray("groups")){var g=raw.getAsJsonObject();ag.put(g.get("uuid").getAsString(),g);names.put(g.get("name").getAsString(),g.get("uuid").getAsString());}
  for(var raw:stand.getAsJsonArray("groups")){var g=raw.getAsJsonObject();sg.put(g.get("uuid").getAsString(),g);}
  math.parentTree(actor.getAsJsonArray("outliner"),null,ap);math.parentTree(stand.getAsJsonArray("outliner"),null,sp);
  var ac=actor.getAsJsonArray("animations").get(0).getAsJsonObject();var sc=stand.getAsJsonArray("animations").get(0).getAsJsonObject();
  for(int tick=104;tick<=176;tick++)for(String hand:List.of("pri_right_item","pli_left_item")){
   String uid=names.get(hand);assertArrayEquals(math.posedOrigin(uid,tick/20.0,ac,ag,ap),math.posedOrigin(uid,(tick-104)/20.0,sc,sg,sp),.00001,"Player/World arm timing drift at "+tick);
  }
  var r=math.posedOrigin(names.get("pri_right_item"),5.2,ac,ag,ap);var l=math.posedOrigin(names.get("pli_left_item"),5.2,ac,ag,ap);assertTrue(l[1]-r[1]>10,"One hand must strike while the other retracts");
 }

}
