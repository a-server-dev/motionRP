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
 @Test void visualReplacementKeepsAllAnimatedPivotsAndTheSharedTiming()throws Exception{
  for(String name:List.of("kurama","asura_kurama","susanoo","indra_susanoo")){
   var old=json(previous(name));var now=json(current(name));Map<String,JsonObject> prior=new HashMap<>(),bones=new HashMap<>();
   for(var raw:old.getAsJsonArray("groups")){var g=raw.getAsJsonObject();prior.put(g.get("uuid").getAsString(),g);}for(var raw:now.getAsJsonArray("groups")){var g=raw.getAsJsonObject();bones.put(g.get("uuid").getAsString(),g);}
   assertEquals(old.get("animations"),now.get("animations"),"Visual polish must preserve the paired choreography");
   for(var raw:now.getAsJsonArray("animations"))for(String id:raw.getAsJsonObject().getAsJsonObject("animators").keySet()){
    assertNotNull(bones.get(id));assertEquals(prior.get(id).get("origin"),bones.get(id).get("origin"));assertEquals(prior.get(id).get("rotation"),bones.get(id).get("rotation"));
   }
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
