package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnergyRevisionTest {
    @TempDir Path root;
    private JsonObject model(String path)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+path+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
    @Test void sphereLowerEdgeFollowsRaisedPalmsThroughoutTheRiseAndGrowth(){
        for(int tick=DragonBallTimeline.RISE;tick<DragonBallTimeline.SPIRIT_RELEASE;tick++){
            var palms=EnergyHandPath.at(true,tick);assertTrue(palms.right().height()>2 && palms.left().height()>2,"Both arms stay above the head while charging");
            double radius=.1+39.9*Math.clamp((tick-8)/88.0,0,1);var center=EnergyHandPath.sphereCenter(tick,radius);
            assertEquals(palms.middle().height()+.12,center.height()-radius,1e-8);
            assertEquals(palms.middle().right(),center.right());assertEquals(palms.middle().forward(),center.forward());
        }
    }
    @Test void deepKamehamehaStanceKeepsFeetAndHipsAttachedAsPalmsMoveFromRightHipToFront()throws Exception{
        var m=model("players/motionrp_kamehameha");var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        math.parentTree(m.getAsJsonArray("outliner"),null,parents);var an=m.getAsJsonArray("animations").get(0).getAsJsonObject();
        for(int tick=0;tick<128;tick++)for(boolean right:new boolean[]{true,false}){
            double x=right?1.875:-1.875,t=tick/20.;String thigh=names.get(right?"prl_right_leg":"pll_left_leg");
            assertArrayEquals(math.posedPoint(parents.get(thigh),new double[]{x,11.25,0},t,an,groups,parents),math.posedOrigin(thigh,t,an,groups,parents),1e-6);
            var foot=math.posedPoint(names.get(right?"prfl_right_foreleg":"plfl_left_foreleg"),new double[]{x,0,0},t,an,groups,parents);assertEquals(0,foot[1],.03,"Foot contact at "+tick);
        }
        var charge=EnergyHandPath.at(false,30);for(var palm:List.of(charge.right(),charge.left())){assertTrue(palm.right()>.18 && palm.forward()>.3 && palm.height()<.85);}
        var hip=math.posedOrigin(names.get("prl_right_leg"),1.5,an,groups,parents);assertTrue(hip[1]<6.5,"Deeply lowered waist");
        var release=EnergyHandPath.at(false,56);for(var palm:List.of(release.right(),release.left()))assertTrue(Math.abs(palm.right())<.15 && palm.forward()>.6);
    }
    @Test void fasterExcaliburCutAndBeamUseEmissiveRoundCoreAndMovingFilaments()throws Exception{
        assertEquals(11,NobleTimeline.impact(true)-NobleTimeline.release(true));
        var weapon=model("models/motionrp_excalibur_blade");var keys=weapon.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators").entrySet().iterator().next().getValue().getAsJsonObject().getAsJsonArray("keyframes");
        assertEquals(2.5,(keys.get(2).getAsJsonObject().get("time").getAsDouble()-keys.get(1).getAsJsonObject().get("time").getAsDouble())*20,1e-8);
        for(String id:List.of("excalibur_light","excalibur_pillar","kamehameha_beam","kamehameha_orb","genki_dama_orb")){
            var m=model("models/motionrp_"+id);assertFalse(m.getAsJsonArray("animations").isEmpty());
            for(var raw:m.getAsJsonArray("elements"))assertEquals(15,raw.getAsJsonObject().get("light_emission").getAsInt());
        }
    }
    @Test void all014EnergyAssetsMigrateToTheNewEmissiveModelsWithTheirOriginalsBackedUp()throws Exception{
        for(String file:List.of("models/motionrp_kamehameha_beam.bbmodel","models/motionrp_genki_dama_orb.bbmodel","models/motionrp_excalibur_light.bbmodel","models/motionrp_excalibur_blade.bbmodel","models/motionrp_excalibur_pillar.bbmodel","models/motionrp_kamehameha_orb.bbmodel","players/motionrp_excalibur.bbmodel","players/motionrp_kamehameha.bbmodel","players/motionrp_genki_dama.bbmodel")){
            byte[] previous,current;
            try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.14.0/"+file.replace('/','_')+".gz"));var gz=new java.util.zip.GZIPInputStream(in)){previous=gz.readAllBytes();}
            try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+file))){current=in.readAllBytes();}
            var target=root.resolve(file);Files.createDirectories(target.getParent());Files.write(target,previous);
            assertEquals(BundledAssetUpdater.Result.UPDATED,new BundledAssetUpdater().install(target,current,BundledAssetVersions.knownHashes(file,Set.of()),root.resolve("backups"),root.resolve("updates")));
            assertArrayEquals(previous,Files.readAllBytes(root.resolve("backups/"+target.getFileName()+"."+BundledAssetUpdater.sha256(previous)+".bak")));
        }
    }
}
