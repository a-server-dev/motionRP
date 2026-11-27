package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PistolBalseAssetsTest {
 private JsonObject load(String name)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+name+".bbmodel"))){return JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();}}
 @Test void newModelsHaveLegalMinecraftCubeSizesAndUniqueIds()throws Exception{
  for(String id:List.of("models/motionrp_gigant_fist","models/motionrp_balse_crystal","models/motionrp_balse_light")){
   var m=load(id);var ids=new HashSet<String>();for(var v:m.getAsJsonArray("elements")){var e=v.getAsJsonObject();assertTrue(ids.add(e.get("uuid").getAsString()));for(int axis=0;axis<3;axis++){double size=e.getAsJsonArray("to").get(axis).getAsDouble()-e.getAsJsonArray("from").get(axis).getAsDouble();assertTrue(size>0&&size<=32);}}
   assertFalse(ids.isEmpty());
  }
 }
 @Test void pairedRolesHaveSynchronizedDurationAndConsentKind()throws Exception{
  for(String role:List.of("left","right")){var a=load("players/motionrp_balse_"+role).getAsJsonArray("animations").get(0).getAsJsonObject();assertEquals(7,a.get("length").getAsDouble());assertEquals("balse_"+role,a.get("name").getAsString());}
  assertEquals(140,PairMotion.fromEffect("pair_balse").orElseThrow().duration());
  var sessions=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();assertTrue(sessions.invite(a,b,0,"balse"));assertTrue(sessions.accept(b,a,1,"fusion").isEmpty());assertTrue(sessions.accept(b,a,2,"balse").isPresent());
 }
 @Test void released073ModelsCanUpgradeWithoutOverwritingCustomModels()throws Exception{
  for(String file:List.of("players/motionrp_fire_tornado.bbmodel","models/motionrp_susanoo_armor.bbmodel"))try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.73.0/"+file.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(file,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void skinArmInflatesThroughItsOwnBonesWithoutChangingSkinGeometry()throws Exception{
  var model=load("players/motionrp_gigant_pistol");
  try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.74.0/players_motionrp_gigant_pistol.bbmodel.gz")))){var old=JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();assertEquals(old.get("elements"),model.get("elements"));assertEquals(old.get("textures"),model.get("textures"));}
  for(var v:model.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators").entrySet()){
   var track=v.getValue().getAsJsonObject();if(!Set.of("pra_right_arm","prfa_right_forearm").contains(track.get("name").getAsString()))continue;
   assertTrue(track.getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).anyMatch(k->k.get("channel").getAsString().equals("scale")));
   assertFalse(track.getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).anyMatch(k->k.get("channel").getAsString().equals("position")),"Do not disconnect the shoulder or elbow");
  }
 }
 @Test void bothBalseHandsReachTheCrystalByBendingTheElbows()throws Exception{
  for(String role:List.of("left","right")){
   var model=load("players/motionrp_balse_"+role);Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();for(var raw:model.getAsJsonArray("groups")){var g=raw.getAsJsonObject();groups.put(g.get("uuid").getAsString(),g);names.put(g.get("name").getAsString(),g.get("uuid").getAsString());}
   var math=new RasenganAssetsTest();math.parentTree(model.getAsJsonArray("outliner"),null,parents);var palm=math.posedOrigin(names.get("balse_palm"),2.5,model.getAsJsonArray("animations").get(0).getAsJsonObject(),groups,parents);assertArrayEquals(new double[]{role.equals("left")?8.8:-8.8,21.6,-10.4},palm,.0001);
  }
 }
 @Test void thickBodyCoresAndLayeredWingsReplaceTheThinFrame()throws Exception{
  var model=load("models/motionrp_susanoo_armor");var elements=model.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject).toList();
  for(String name:List.of("solid_torso_core","solid_leg_core_-1","solid_leg_core_1")){
   var parts=elements.stream().filter(e->e.get("name").getAsString().startsWith(name+"_")).toList();assertFalse(parts.isEmpty());double lo=parts.stream().mapToDouble(e->e.getAsJsonArray("from").get(1).getAsDouble()).min().orElseThrow(),hi=parts.stream().mapToDouble(e->e.getAsJsonArray("to").get(1).getAsDouble()).max().orElseThrow();assertEquals(48,hi-lo,.001);
  }
  for(String name:List.of("solid_forearm_core","upper_arm_plate","wing_scapular_surface","kabuto_dome","helmet_fukigaeshi"))assertTrue(elements.stream().anyMatch(e->e.get("name").getAsString().startsWith(name)));
 }
 @Test void upperArmTapersIntoAnExactlyFiftySevenTimesForearm()throws Exception{
  var m=load("players/motionrp_gigant_pistol");var tracks=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");double last=1;int sections=0;
  double[] parent=null,fore=null;
  for(var raw:m.getAsJsonArray("groups")){
   var g=raw.getAsJsonObject();String name=g.get("name").getAsString();if(!name.startsWith("pra_gigant_taper_")&&!name.equals("pra_right_arm")&&!name.equals("prfa_right_forearm"))continue;
   var keys=tracks.getAsJsonObject(g.get("uuid").getAsString()).getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).filter(k->k.get("channel").getAsString().equals("scale")&&Math.abs(k.get("time").getAsDouble()-2.4)<.00001).toList();assertEquals(1,keys.size());var v=keys.get(0).getAsJsonArray("data_points").get(0).getAsJsonObject();double[] xyz={v.get("x").getAsDouble(),v.get("y").getAsDouble(),v.get("z").getAsDouble()};
   if(name.equals("pra_right_arm"))parent=xyz;else if(name.equals("prfa_right_forearm"))fore=xyz;else{double width=xyz[0]*32;assertTrue(width>last&&width<57);last=width;sections++;assertEquals(1.0/16,xyz[1],.00001);}
  }
  assertEquals(16,sections);assertNotNull(parent);assertNotNull(fore);for(int axis=0;axis<3;axis++)assertEquals(57,parent[axis]*fore[axis],.00001);
 }
 @Test void released075SkinArmUpgradesByItsKnownHash()throws Exception{
  String file="players/motionrp_gigant_pistol.bbmodel";try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.75.0/"+file.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(file,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
}
