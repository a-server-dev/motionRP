package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;

class BattleVisualsTest {
 private static final List<String> NAMES=List.of("kurama","asura_kurama","susanoo","indra_susanoo","gold_rasenshuriken","dark_rasenshuriken","indra_arrow","clash_orb","chakra_spark","chakra_storm");
 private byte[] current(String name)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/models/motionrp_duel_"+name+".bbmodel"))){return in.readAllBytes();}}
 private byte[] previous(String name)throws Exception{try(var in=new GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.29.0/models_motionrp_duel_"+name+".bbmodel.gz")))){return in.readAllBytes();}}
 private JsonObject json(byte[] b){return JsonParser.parseString(new String(b,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
 @Test void allSixArmsShareTheSameLeftOrRightShoulder()throws Exception{
  var m=json(current("asura_kurama"));var groups=new HashMap<String,JsonObject>();for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();groups.put(g.get("name").getAsString(),g);}
  for(int side:List.of(-1,1))for(int row=1;row<3;row++)assertEquals(groups.get("fox_upper_0_"+side).get("origin"),groups.get("fox_upper_"+row+"_"+side).get("origin"));
 }
 @Test void susanooHasLongerLegsAndASmallerHeadThan030()throws Exception{
  for(String name:List.of("susanoo","indra_susanoo")){
   JsonObject old;try(var in=new GZIPInputStream(getClass().getResourceAsStream("/bundled-0.30.0/models_motionrp_duel_"+name+".bbmodel.gz"))){old=json(in.readAllBytes());}var now=json(current(name));
   for(var raw:now.getAsJsonArray("groups")){var g=raw.getAsJsonObject();if(g.get("name").getAsString().equals("susanoo_leg_1"))assertEquals(60,g.getAsJsonArray("origin").get(1).getAsDouble());}
   double[] widths=new double[2];int i=0;for(var m:List.of(old,now)){for(var raw:m.getAsJsonArray("elements")){var e=raw.getAsJsonObject();if(e.get("name").getAsString().equals("tengu_helmet_shell")){double lo=1e9,hi=-1e9;for(var v:e.getAsJsonObject("vertices").entrySet()){double x=v.getValue().getAsJsonArray().get(0).getAsDouble();lo=Math.min(lo,x);hi=Math.max(hi,x);}widths[i]=hi-lo;}}i++;}assertTrue(widths[1]<widths[0]*.7);
  }
 }
 @Test void chargeAndReleaseAreDistinctClipsAndShareTheLaunchClock()throws Exception{
  assertTrue(FinalBattleTimeline.TECHNIQUES>FinalBattleTimeline.CHARGE+40);assertEquals(8,FinalBattleTimeline.RELEASE-FinalBattleTimeline.WINDUP);assertEquals(3,FinalBattleTimeline.EXPLOSION_SCALE);
  for(String name:List.of("asura_kurama","indra_susanoo")){var m=json(current(name));var clips=new HashMap<String,JsonObject>();for(var raw:m.getAsJsonArray("animations")){var a=raw.getAsJsonObject();clips.put(a.get("name").getAsString(),a);}assertTrue(clips.containsKey("gather"));assertFalse(clips.containsKey("exchange"));assertEquals(2,clips.get("release").get("length").getAsInt());assertEquals(8,clips.get("charge").get("length").getAsInt());}
 }
 @Test void shipped030AvatarsAndActorsMigrateToNewPivotsWithBackups()throws Exception{
  var root=Files.createTempDirectory("motionrp-031-migrate");for(String file:List.of("models/motionrp_duel_kurama.bbmodel","models/motionrp_duel_asura_kurama.bbmodel","models/motionrp_duel_susanoo.bbmodel","models/motionrp_duel_indra_susanoo.bbmodel","players/motionrp_final_battle_naruto.bbmodel","players/motionrp_final_battle_sasuke.bbmodel")){
   byte[] old,now;try(var in=new GZIPInputStream(getClass().getResourceAsStream("/bundled-0.30.0/"+file.replace('/','_')+".gz"))){old=in.readAllBytes();}try(var in=getClass().getResourceAsStream("/bettermodel/"+file)){now=in.readAllBytes();}var target=root.resolve(file);Files.createDirectories(target.getParent());Files.write(target,old);var u=new BundledAssetUpdater();var hashes=BundledAssetVersions.knownHashes(file,Set.of());assertEquals(BundledAssetUpdater.Result.UPDATED,u.install(target,now,hashes,root.resolve("backups"),root.resolve("updates")));assertTrue(Files.exists(root.resolve("backups").resolve(target.getFileName()+"."+BundledAssetUpdater.sha256(old)+".bak")));Files.writeString(target,"custom");assertEquals(BundledAssetUpdater.Result.CUSTOM_PRESERVED,u.install(target,now,hashes,root.resolve("backups"),root.resolve("updates")));
  }
 }
 private void collect(JsonArray nodes,Map<String,Integer> refs){for(var raw:nodes)if(raw.isJsonPrimitive())refs.merge(raw.getAsString(),1,Integer::sum);else collect(raw.getAsJsonObject().getAsJsonArray("children"),refs);}
 @Test void sculptedGeometryIsFiniteAttachedOnceAndWithinTheTriangleBudget()throws Exception{
  for(String name:NAMES){var m=json(current(name));Map<String,Integer> refs=new HashMap<>();collect(m.getAsJsonArray("outliner"),refs);int triangles=0;
   for(var raw:m.getAsJsonArray("elements")){var e=raw.getAsJsonObject();assertEquals("mesh",e.get("type").getAsString());assertEquals(1,refs.get(e.get("uuid").getAsString()));var vertices=e.getAsJsonObject("vertices");
    for(var v:vertices.entrySet())for(var value:v.getValue().getAsJsonArray()){double d=value.getAsDouble();assertTrue(Double.isFinite(d)&&Math.abs(d)<300,name+" has unbounded geometry");}
    for(var f:e.getAsJsonObject("faces").entrySet()){var face=f.getValue().getAsJsonObject();assertEquals(3,face.getAsJsonArray("vertices").size());for(var id:face.getAsJsonArray("vertices")){assertTrue(vertices.has(id.getAsString()));assertTrue(face.getAsJsonObject("uv").has(id.getAsString()));}triangles++;}
   }assertTrue(triangles<45000,name+" exceeds the bounded per-model geometry budget");
  }
 }
 @Test void mirroredEyesAndArmorPanelsKeepTheirFrontFacesFacingTheViewer()throws Exception{
  for(String name:List.of("kurama","asura_kurama","susanoo","indra_susanoo"))for(var raw:json(current(name)).getAsJsonArray("elements")){
   var e=raw.getAsJsonObject();String label=e.get("name").getAsString();if(!label.contains("eye_socket")&&!label.contains("almond_eye"))continue;
   var vertices=e.getAsJsonObject("vertices");int fronts=0;for(var f:e.getAsJsonObject("faces").entrySet()){
    var ids=f.getValue().getAsJsonObject().getAsJsonArray("vertices");if(!ids.get(0).getAsString().equals("front"))continue;
    var a=vertices.getAsJsonArray(ids.get(0).getAsString());var b=vertices.getAsJsonArray(ids.get(1).getAsString());var c=vertices.getAsJsonArray(ids.get(2).getAsString());
    double normalZ=(b.get(0).getAsDouble()-a.get(0).getAsDouble())*(c.get(1).getAsDouble()-a.get(1).getAsDouble())-(b.get(1).getAsDouble()-a.get(1).getAsDouble())*(c.get(0).getAsDouble()-a.get(0).getAsDouble());assertTrue(normalZ<0,label+" must face forward on both sides");fronts++;
   }assertTrue(fronts>=3);
  }
 }
 @Test void energyVeilsUseVisibleVanillaCutoutRatherThanUnsupportedIntermediateAlpha()throws Exception{
  for(String name:List.of("gold_rasenshuriken","dark_rasenshuriken","indra_arrow","chakra_storm")){
   var m=json(current(name));String source=m.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();var image=ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(source.substring(source.indexOf(',')+1))));int solid=0,holes=0;
   for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int a=image.getRGB(x,y)>>>24;assertTrue(a==0||a==255);if(a==0)holes++;else solid++;}
   assertTrue(solid>holes&&holes>100,"Veils need both visible strands and real holes");
  }
 }
 @Test void shipped029ModelsUpgradeWithBackupsAndCustomModelsAreRetained()throws Exception{
  var root=Files.createTempDirectory("motionrp-030-migrate");for(String name:NAMES){String file="models/motionrp_duel_"+name+".bbmodel";byte[] old=previous(name),now=current(name);var target=root.resolve(file);Files.createDirectories(target.getParent());Files.write(target,old);var updater=new BundledAssetUpdater();var hashes=BundledAssetVersions.knownHashes(file,Set.of());
   assertEquals(BundledAssetUpdater.Result.UPDATED,updater.install(target,now,hashes,root.resolve("backups"),root.resolve("updates")));assertArrayEquals(old,Files.readAllBytes(root.resolve("backups").resolve(target.getFileName()+"."+BundledAssetUpdater.sha256(old)+".bak")));
   Files.writeString(target,"custom");assertEquals(BundledAssetUpdater.Result.CUSTOM_PRESERVED,updater.install(target,now,hashes,root.resolve("backups"),root.resolve("updates")));
  }
 }
}
