package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PerfectSusanooTest {
    @Test void oldMaskAndArmorUpgradeWithExactBackupsButKeepCustomEdits() throws Exception {
        var root=java.nio.file.Files.createTempDirectory("motionrp-018-upgrade");
        for(String file:List.of("models/motionrp_hollow_mask.bbmodel","models/motionrp_susanoo_armor.bbmodel")){
            byte[] old,current;
            try(var in=getClass().getResourceAsStream("/bundled-0.18.0/"+file.replace('/','_')+".gz");var gzip=new java.util.zip.GZIPInputStream(in)){old=gzip.readAllBytes();}
            try(var in=getClass().getResourceAsStream("/bettermodel/"+file)){current=in.readAllBytes();}
            var target=root.resolve(file);java.nio.file.Files.createDirectories(target.getParent());java.nio.file.Files.write(target,old);
            var updater=new BundledAssetUpdater();var known=BundledAssetVersions.knownHashes(file,Set.of());
            assertEquals(BundledAssetUpdater.Result.UPDATED,updater.install(target,current,known,root.resolve("backups"),root.resolve("updates")));
            assertArrayEquals(current,java.nio.file.Files.readAllBytes(target));
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(old));
            assertArrayEquals(old,java.nio.file.Files.readAllBytes(root.resolve("backups").resolve(target.getFileName()+"."+hash+".bak")));
            java.nio.file.Files.writeString(target,"custom");assertEquals(BundledAssetUpdater.Result.CUSTOM_PRESERVED,updater.install(target,current,known,root.resolve("backups"),root.resolve("updates")));
        }
    }

    @Test void drawnBladeAndScabbardRemainInTheHandsThroughBothCuts() throws Exception {
        JsonObject model;
        try(var in=getClass().getResourceAsStream("/bettermodel/models/motionrp_susanoo_armor.bbmodel")){model=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        var clip=model.getAsJsonArray("animations").get(0).getAsJsonObject();
        Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        var math=new RasenganAssetsTest();math.parentTree(model.getAsJsonArray("outliner"),null,parents);
        for(int tick=16;tick<=104;tick++){
            double t=tick/20.0;
            var right=math.posedPoint(names.get("forearm_1"),new double[]{24,36,0},t,clip,groups,parents);
            var sword=math.posedOrigin(names.get("waist_blade_-1"),t,clip,groups,parents);
            var left=math.posedPoint(names.get("forearm_-1"),new double[]{-24,36,0},t,clip,groups,parents);
            var sheath=math.posedPoint(names.get("waist_scabbard_-1"),new double[]{0,0,-14},t,clip,groups,parents);
            assertArrayEquals(sword,right,.0001,"Right hand must hold the drawn left-waist sword");
            assertArrayEquals(sheath,left,.0001,"Left hand must hold the scabbard");
            assertArrayEquals(new double[]{18,59,-7},math.posedOrigin(names.get("waist_blade_1"),t,clip,groups,parents),.0001,"Other sword stays at the waist");
        }
        var early=math.posedOrigin(names.get("waist_blade_-1"),.8,clip,groups,parents);
        var late=math.posedOrigin(names.get("waist_blade_-1"),1.15,clip,groups,parents);
        assertTrue(late[0]-early[0]>30,"Pull the blade sideways out of its sheath");
    }

    @Test void completeBlueSamuraiIncludesFullLegsTenguWingsAndPairedDetailedBlades() throws Exception {
        JsonObject model;
        try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/models/motionrp_susanoo_armor.bbmodel"))) {
            model=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Set<String> names=new HashSet<>();double minX=0,maxX=0,top=0;int feathers=0,nose=0,blades=0;
        for(var raw:model.getAsJsonArray("elements")) {
            var e=raw.getAsJsonObject();String name=e.get("name").getAsString();names.add(name);
            if(name.startsWith("wing_feather")) feathers++;if(name.startsWith("tengu_long_nose")) nose++;if(name.startsWith("katana_blade")) blades++;
            minX=Math.min(minX,e.getAsJsonArray("from").get(0).getAsDouble());maxX=Math.max(maxX,e.getAsJsonArray("to").get(0).getAsDouble());top=Math.max(top,e.getAsJsonArray("to").get(1).getAsDouble());
        }
        assertTrue(feathers>=180 && nose>=8 && blades>=36);assertTrue(top*2/16>14 && (maxX-minX)*2/16>20);
        assertTrue(names.stream().anyMatch(n->n.startsWith("shin_lamella")) && names.stream().anyMatch(n->n.startsWith("claw")));
        var animations=model.getAsJsonArray("animations");assertEquals("unfurl",animations.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals((SusanooTimeline.READY-SusanooTimeline.ARMOR)/20.0,animations.get(1).getAsJsonObject().get("length").getAsDouble());
        byte[] png=Base64.getDecoder().decode(model.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString().split(",")[1]);
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));int rgb=image.getRGB(5,0);
        assertTrue((rgb&255)>((rgb>>16)&255),"Chakra armor must be blue");
    }
}
