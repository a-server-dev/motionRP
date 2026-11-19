package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ExcaliburReferenceTest {
    private JsonObject model(String path)throws Exception{try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+path+".bbmodel"))){return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
    @Test void referenceBladeIsTallAndPalmPivotedAndWaveIsBroadAndEmissive()throws Exception{
        var blade=model("models/motionrp_excalibur_charge");double height=0;
        for(var raw:blade.getAsJsonArray("elements")){var e=raw.getAsJsonObject();height=Math.max(height,e.getAsJsonArray("to").get(1).getAsDouble());assertEquals(15,e.get("light_emission").getAsInt());}
        assertTrue(height*8/16>=20);assertEquals("cleave",blade.getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
        var root=blade.getAsJsonArray("groups").asList().stream().map(JsonElement::getAsJsonObject).filter(g->g.get("name").getAsString().equals("light_blade")).findFirst().orElseThrow();
        for(var n:root.getAsJsonArray("origin"))assertEquals(0,n.getAsDouble());
        var wave=model("models/motionrp_excalibur_wave");double lo=0,hi=0;
        for(var raw:wave.getAsJsonArray("elements")){var e=raw.getAsJsonObject();lo=Math.min(lo,e.getAsJsonArray("from").get(0).getAsDouble());hi=Math.max(hi,e.getAsJsonArray("to").get(0).getAsDouble());assertEquals(15,e.get("light_emission").getAsInt());}
        assertTrue((hi-lo)*6/16>9,"Final wavefront should spread well beyond the caster");
    }
    @Test void bracedStanceStaggersFeetWithoutDetachingHipsOrLiftingFeet()throws Exception{
        var m=model("players/motionrp_excalibur");var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:m.getAsJsonArray("groups")){var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        math.parentTree(m.getAsJsonArray("outliner"),null,parents);var an=m.getAsJsonArray("animations").get(0).getAsJsonObject();
        for(int tick=0;tick<176;tick++)for(boolean right:new boolean[]{true,false}){
            double x=right?1.875:-1.875,t=tick/20.;String thigh=names.get(right?"prl_right_leg":"pll_left_leg");
            assertArrayEquals(math.posedPoint(parents.get(thigh),new double[]{x,11.25,0},t,an,groups,parents),math.posedOrigin(thigh,t,an,groups,parents),1e-6);
            var foot=math.posedPoint(names.get(right?"prfl_right_foreleg":"plfl_left_foreleg"),new double[]{x,0,0},t,an,groups,parents);assertEquals(0,foot[1],.03);
        }
        var r=math.posedPoint(names.get("prfl_right_foreleg"),new double[]{1.875,0,0},4,an,groups,parents);var l=math.posedPoint(names.get("plfl_left_foreleg"),new double[]{-1.875,0,0},4,an,groups,parents);assertTrue(r[2]-l[2]>6);
    }
    @Test void precedingPoseIsRecognizedForAutomaticUpgrade()throws Exception{
        byte[] previous;try(var raw=Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.14.1/players_motionrp_excalibur.bbmodel.gz"));var gz=new java.util.zip.GZIPInputStream(raw)){previous=gz.readAllBytes();}
        assertTrue(BundledAssetVersions.knownHashes("players/motionrp_excalibur.bbmodel",Set.of()).contains(BundledAssetUpdater.sha256(previous)));
    }
}
