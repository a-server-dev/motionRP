package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PerfectSusanooTest {
    @Test void swordStaysPosedPastFadeWhileTorsoMotionRemainsRestrained()throws Exception{
        JsonObject old,current;
        try(var in=getClass().getResourceAsStream("/bundled-0.20.0/models_motionrp_susanoo_armor.bbmodel.gz");var gzip=new java.util.zip.GZIPInputStream(in)){old=JsonParser.parseString(new String(gzip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        try(var in=getClass().getResourceAsStream("/bettermodel/models/motionrp_susanoo_armor.bbmodel")){current=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        var before=old.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");var after=current.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
        var clip=current.getAsJsonArray("animations").get(0).getAsJsonObject();
        assertTrue(clip.get("length").getAsDouble()*20>SusanooTimeline.END-SusanooTimeline.READY,"Weapons must not reset before fade cleanup");
        for(var raw:after.entrySet()){
            var entry=raw.getValue().getAsJsonObject();
            if(entry.get("name").getAsString().startsWith("blade")){
                var positions=entry.getAsJsonArray("keyframes").asList().stream().map(JsonElement::getAsJsonObject).filter(k->k.get("channel").getAsString().equals("position")).toList();
                assertEquals(positions.get(positions.size()-2).get("data_points"),positions.get(positions.size()-1).get("data_points"));
            }
        }
        var torso=after.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(t->t.get("name").getAsString().equals("battle_torso")).findFirst().orElseThrow();
        for(var raw:torso.getAsJsonArray("keyframes")){var key=raw.getAsJsonObject();double t=key.get("time").getAsDouble();if(t>=2.7&&t<=4.2){var v=key.getAsJsonArray("data_points").get(0).getAsJsonObject();assertTrue(Math.abs(v.get("x").getAsDouble())<5&&Math.abs(v.get("y").getAsDouble())<10&&Math.abs(v.get("z").getAsDouble())<3);}}
        assertTrue(current.getAsJsonArray("elements").size()>old.getAsJsonArray("elements").size()+150);
    }

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

    @Test void bladeSweepClearsChestAndHelmetBetweenAnimationKeys()throws Exception{
        JsonObject model;try(var in=getClass().getResourceAsStream("/bettermodel/models/motionrp_susanoo_armor.bbmodel")){model=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        var clip=model.getAsJsonArray("animations").get(0).getAsJsonObject();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        var math=new RasenganAssetsTest();math.parentTree(model.getAsJsonArray("outliner"),null,parents);
        var boxes=model.getAsJsonArray("elements").asList().stream().map(JsonElement::getAsJsonObject).filter(e->{String n=e.get("name").getAsString();return n.startsWith("breastplate_")||n.startsWith("temple_")||n.startsWith("helmet_crown")||n.startsWith("tengu_face_contour");}).toList();
        for(int frame=0;frame<=208;frame++){
            double t=frame/40.0;var origin=math.posedPoint(names.get("battle_torso"),new double[]{0,57,0},t,clip,groups,parents);double[][] basis=new double[3][];
            for(int axis=0;axis<3;axis++){double[] p={0,57,0};p[axis]++;basis[axis]=math.posedPoint(names.get("battle_torso"),p,t,clip,groups,parents);for(int j=0;j<3;j++)basis[axis][j]-=origin[j];}
            for(int distance=10;distance<=60;distance+=2){
                var world=math.posedPoint(names.get("waist_blade_-1"),new double[]{0,0,-distance},t,clip,groups,parents);double[] point={0,57,0};
                for(int axis=0;axis<3;axis++)for(int j=0;j<3;j++)point[axis]+=(world[j]-origin[j])*basis[axis][j];
                for(var box:boxes){double squared=0;for(int j=0;j<3;j++){double lo=box.getAsJsonArray("from").get(j).getAsDouble(),hi=box.getAsJsonArray("to").get(j).getAsDouble();double d=Math.max(0,Math.max(lo-point[j],point[j]-hi));squared+=d*d;}assertTrue(squared>9,"Blade thickness must clear the torso and helmet at t="+t);}
            }
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
            assertArrayEquals(math.posedPoint(names.get("battle_torso"),new double[]{18,59,-7},t,clip,groups,parents),math.posedOrigin(names.get("waist_blade_1"),t,clip,groups,parents),.0001,"Other sword stays at the waist");
        }
        for(int tick=SusanooTimeline.ARMOR+44;tick<SusanooTimeline.FADE;tick++){
            var point=math.posedPoint(names.get("battle_torso"),new double[]{0,82.8,-7.44},Math.max(0,(tick-SusanooTimeline.READY)/20.0),clip,groups,parents);
            var pilot=SusanooPilotPath.at(tick);assertEquals(point[0]*10/16,pilot.right(),.00001);assertEquals(-point[2]*10/16,pilot.forward(),.00001);assertEquals(point[1]*10/16,pilot.height(),.00001);
        }
        var overhead=math.posedPoint(names.get("waist_blade_-1"),new double[]{0,0,-60},2.9,clip,groups,parents);
        var crown=math.posedPoint(names.get("battle_torso"),new double[]{0,110,0},2.9,clip,groups,parents);
        assertTrue(overhead[1]>crown[1]+20 && overhead[2]>crown[2]+15,"Wind the blade high and behind the head");
        var firstA=math.posedPoint(names.get("waist_blade_-1"),new double[]{0,0,-60},1.3,clip,groups,parents);
        var firstB=math.posedPoint(names.get("waist_blade_-1"),new double[]{0,0,-60},1.5,clip,groups,parents);
        assertTrue(Math.abs(firstB[0]-firstA[0])>70,"Draw cut sweeps the blade across a broad horizontal arc");
        var early=math.posedOrigin(names.get("waist_blade_-1"),.8,clip,groups,parents);
        var late=math.posedOrigin(names.get("waist_blade_-1"),1.15,clip,groups,parents);
        assertTrue(Math.sqrt(java.util.stream.IntStream.range(0,3).mapToDouble(i->Math.pow(late[i]-early[i],2)).sum())>30,"Pull the blade sideways out of its sheath");
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
