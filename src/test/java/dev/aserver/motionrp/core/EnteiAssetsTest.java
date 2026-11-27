package dev.aserver.motionrp.core;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EnteiAssetsTest {
 private JsonObject load(String name)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+name+".bbmodel"))){return JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();}}
 @Test void sunHasACurvedSurfaceInAllThreeDimensionsWithIndependentPlasmaAndCorona()throws Exception{
  var m=load("models/motionrp_entei_sun");assertTrue(m.getAsJsonArray("elements").size()>15);var core=m.getAsJsonArray("elements").get(0).getAsJsonObject();assertEquals("mesh",core.get("type").getAsString());assertTrue(core.getAsJsonObject("faces").size()>2000);
  for(int axis=0;axis<3;axis++){final int a=axis;var points=core.getAsJsonObject("vertices").entrySet().stream().mapToDouble(v->v.getValue().getAsJsonArray().get(a).getAsDouble()).summaryStatistics();assertTrue(points.getMax()-points.getMin()>31);}
  var texture=m.getAsJsonArray("textures").get(0).getAsJsonObject();byte[] png=Base64.getDecoder().decode(texture.get("source").getAsString().split(",")[1]);var image=javax.imageio.ImageIO.read(new ByteArrayInputStream(png));assertEquals(256,image.getWidth());assertEquals(256,image.getHeight());var colors=new HashSet<Integer>();for(int y=0;y<128;y++)for(int x=0;x<256;x++)colors.add(image.getRGB(x,y));assertTrue(colors.size()>100,"Use a continuous fire texture rather than differently colored polygon tiles");
  assertEquals("burn",m.getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
 }
 @Test void raisedHandHoldsBeforeTheForwardThrowAndBodyLastsThroughCleanup()throws Exception{
  var m=load("players/motionrp_entei");var a=m.getAsJsonArray("animations").get(0).getAsJsonObject();assertEquals(9.5,a.get("length").getAsDouble());var arm=a.getAsJsonObject("animators").entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(e->e.get("name").getAsString().equals("pra_right_arm")).findFirst().orElseThrow();
  var keys=arm.getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).filter(k->k.get("channel").getAsString().equals("rotation")).toList();assertTrue(keys.stream().anyMatch(k->k.get("time").getAsDouble()==4.5&&k.getAsJsonArray("data_points").get(0).getAsJsonObject().get("x").getAsDouble()>160));assertTrue(keys.stream().anyMatch(k->k.get("time").getAsDouble()==6&&k.getAsJsonArray("data_points").get(0).getAsJsonObject().get("x").getAsDouble()==95));
 }
 @Test void released076ArmCanUpgradeWithoutLosingUserEdits()throws Exception{
  String file="players/motionrp_gigant_pistol.bbmodel";try(var in=new java.util.zip.GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.76.0/"+file.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(file,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
}
