package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DragonBallTest {
    private JsonObject model(String path) throws Exception {
        try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+path+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
    }
    @Test void spiritBombAscentReachesFiftyBlocksInFortyTicksAndItsModelIsEightyBlocksWide() throws Exception {
        assertEquals(40,DragonBallTimeline.RISE_END-DragonBallTimeline.RISE);
        assertEquals(0,DragonBallTimeline.height(DragonBallTimeline.RISE));assertEquals(50,DragonBallTimeline.height(DragonBallTimeline.RISE_END));
        var m=model("models/motionrp_genki_dama_orb");double[] min={100,100,100},max={-100,-100,-100};
        for(var raw:m.getAsJsonArray("elements")){var e=raw.getAsJsonObject();if(e.get("name").getAsString().startsWith("surface_arc"))continue;for(int axis=0;axis<3;axis++){min[axis]=Math.min(min[axis],e.getAsJsonArray("from").get(axis).getAsDouble());max[axis]=Math.max(max[axis],e.getAsJsonArray("to").get(axis).getAsDouble());}}
        for(int axis=0;axis<3;axis++)assertEquals(80,(max[axis]-min[axis])*40/16,.01);
        assertTrue(m.getAsJsonArray("elements").size()<400,"Bound the giant sphere rendering cost");
    }
    @Test void bothArmsRiseWithTheAscentAndNewPlayerAnimationsCoverTheirFullScenes() throws Exception {
        for(boolean spirit:new boolean[]{false,true}){
            var m=model("players/motionrp_"+(spirit?"genki_dama":"kamehameha"));var an=m.getAsJsonArray("animations").get(0).getAsJsonObject();
            assertEquals(DragonBallTimeline.end(spirit)/20.0,an.get("length").getAsDouble());
            if(spirit){var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
                for(var g:m.getAsJsonArray("groups")){var obj=g.getAsJsonObject();String id=obj.get("uuid").getAsString();groups.put(id,obj);names.put(obj.get("name").getAsString(),id);}
                math.parentTree(m.getAsJsonArray("outliner"),null,parents);
                for(String hand:List.of("pri_right_item","pli_left_item")){var p=math.posedOrigin(names.get(hand),2.4,an,groups,parents);assertTrue(p[1]>25,"Both palms must be above the head");}
            }
        }
    }
    @Test void cleaverGripAttachesToTheStraightSpineInsteadOfTheMiddleOfItsBlade() throws Exception {
        var m=model("models/motionrp_zangetsu_cleaver");
        for(var raw:m.getAsJsonArray("elements")){var e=raw.getAsJsonObject();if(e.get("name").getAsString().startsWith("cleaver_spine")){assertEquals(0,e.getAsJsonArray("to").get(0).getAsDouble(),1e-6);assertTrue(e.getAsJsonArray("from").get(1).getAsDouble()>0);}}
    }
    @Test void twoDifferentSusanooSwordArmsStrikeInSequenceAndArmorIsOpaque() throws Exception {
        var m=model("models/motionrp_susanoo_armor");var an=m.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators");
        String left=null,right=null;for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();if(g.get("name").getAsString().equals("guard_arm"))left=g.get("uuid").getAsString();if(g.get("name").getAsString().equals("sword_arm"))right=g.get("uuid").getAsString();}
        assertNotEquals(an.get(left),an.get(right));assertTrue(SusanooTimeline.LEFT_IMPACT<SusanooTimeline.RIGHT_SLASH);
        var source=m.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();var png=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(source.split(",")[1])));
        for(int x=0;x<png.getWidth();x++)for(int y=0;y<png.getHeight();y++)assertEquals(255,(png.getRGB(x,y)>>>24)&255);
    }
    @Test void everyChanged013AssetUpgradesWithBackupAndKeepsCustomModelsUntouched() throws Exception {
        var root=java.nio.file.Files.createTempDirectory("motionrp-013-upgrade");
        for(String file:List.of("models/motionrp_susanoo_armor.bbmodel","models/motionrp_susanoo_slash.bbmodel","models/motionrp_excalibur_light.bbmodel","models/motionrp_excalibur_blade.bbmodel","models/motionrp_zangetsu_cleaver.bbmodel","models/motionrp_gae_bolg_spear.bbmodel","players/motionrp_gae_bolg.bbmodel","players/motionrp_excalibur.bbmodel")) {
            byte[] previous,current;
            try(var raw=Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.13.0/"+file.replace('/','_')+".gz"));var gzip=new java.util.zip.GZIPInputStream(raw)){previous=gzip.readAllBytes();}
            try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+file))){current=in.readAllBytes();}
            Set<String> known=BundledAssetVersions.knownHashes(file,Set.of());
            var target=root.resolve(file);java.nio.file.Files.createDirectories(target.getParent());java.nio.file.Files.write(target,previous);
            var updater=new BundledAssetUpdater();assertEquals(java.util.Arrays.equals(previous,current)?BundledAssetUpdater.Result.UNCHANGED:BundledAssetUpdater.Result.UPDATED,updater.install(target,current,known,root.resolve("backups"),root.resolve("updates")));
            assertArrayEquals(current,java.nio.file.Files.readAllBytes(target));
            java.nio.file.Files.writeString(target,"custom model");assertEquals(BundledAssetUpdater.Result.CUSTOM_PRESERVED,updater.install(target,current,known,root.resolve("backups"),root.resolve("updates")));
        }
    }
}
