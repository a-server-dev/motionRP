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
}
