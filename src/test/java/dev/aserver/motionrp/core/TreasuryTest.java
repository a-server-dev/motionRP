package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class TreasuryTest {
    private JsonObject model(String name) throws Exception {
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+name+".bbmodel"))) {
            return JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @Test void allHundredOrTwoHundredGatesFireOneRapidPassThenAllJoinTheVolley() {
        for(boolean serious:new boolean[]{false,true}) {
            Set<TreasuryTimeline.Portal> unique=new HashSet<>();int[] waves=new int[30];int surrounding=0;
            for(int i=0;i<TreasuryTimeline.count(serious);i++) {
                var p=TreasuryTimeline.portal(i,serious);assertTrue(unique.add(p));
                assertTrue(TreasuryTimeline.openAt(i,serious)+8<TreasuryTimeline.rapidAt(i,serious));
                int rapid=TreasuryTimeline.rapidAt(i,serious);waves[rapid-42]++;
                assertTrue(rapid+1+6<84);assertTrue(p.height()>.5 && p.height()<13);
                if(p.surroundsTarget()) { surrounding++;assertEquals(6,Math.hypot(p.right(),p.forward()),1e-6); }
                else assertTrue(p.forward()<=-2.5 && Math.abs(p.right())<12);
            }
            assertEquals(serious?100:0,surrounding);
            for(int count:waves) assertTrue(count>0 && count<8);
            assertEquals(serious?400:200,Arrays.stream(waves).sum()+TreasuryTimeline.count(serious));
        }
        assertEquals("KEY",TreasuryTimeline.phase(TreasuryTimeline.KEY,true));
        assertTrue(TreasuryTimeline.KEY<72 && TreasuryTimeline.EA<72);
        assertEquals("ENUMA_ELISH",TreasuryTimeline.phase(TreasuryTimeline.FIRE,true));
        assertEquals("FINISHED",TreasuryTimeline.phase(272,true));
    }
    @Test void theHundredGatePoseKeepsTheWristsSpreadAndStandingStill() throws Exception {
        var pose=model("players/motionrp_gate_of_babylon_100");
        assertEquals(5.8,pose.getAsJsonArray("animations").get(0).getAsJsonObject().get("length").getAsDouble());
        var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:pose.getAsJsonArray("groups")) { var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id); }
        math.parentTree(pose.getAsJsonArray("outliner"),null,parents);
        var animation=pose.getAsJsonArray("animations").get(0).getAsJsonObject();
        var a=math.posedOrigin(names.get("pri_right_item"),1,animation,groups,parents);var b=math.posedOrigin(names.get("pri_right_item"),2,animation,groups,parents);
        assertTrue(Math.abs(a[0])>11,"Hundred-gate arm must remain spread");assertArrayEquals(a,b,1e-7);
    }
    @Test void seriousStancePlantsStaggeredFeetAndHoldsEaBesideTheFaceBeforeThrusting() throws Exception {
        var pose=model("players/motionrp_gate_of_babylon_enuma");var math=new RasenganAssetsTest();
        Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:pose.getAsJsonArray("groups")) { var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id); }
        math.parentTree(pose.getAsJsonArray("outliner"),null,parents);var animation=pose.getAsJsonArray("animations").get(0).getAsJsonObject();
        assertEquals(13.6,animation.get("length").getAsDouble());
        for(double time:new double[]{6,8,9.3}) {
            var right=math.posedPoint(names.get("prfl_right_foreleg"),new double[]{1.875,0,0},time,animation,groups,parents);
            var left=math.posedPoint(names.get("plfl_left_foreleg"),new double[]{-1.875,0,0},time,animation,groups,parents);
            assertTrue(Math.abs(right[1])<.5 && Math.abs(left[1])<.5,"Feet must stay planted");
            assertTrue(Math.abs(right[2]-left[2])>3,"One foot must be behind the other");
            assertTrue(Math.hypot(right[0]-left[0],right[2]-left[2])>6,"Stance must be wider than the original idle");
        }
        for(double time:new double[]{1,3,TreasuryTimeline.EA_READY/20.0}) {
            var right=math.posedPoint(names.get("prfl_right_foreleg"),new double[]{1.875,0,0},time,animation,groups,parents);
            var left=math.posedPoint(names.get("plfl_left_foreleg"),new double[]{-1.875,0,0},time,animation,groups,parents);
            assertEquals(0,right[2]-left[2],.01,"Feet stay neutral until Ea is fully drawn");
        }
        var charge=TreasuryHandPath.at(140);var fire=TreasuryHandPath.at(TreasuryTimeline.FIRE);
        assertTrue(charge.height()>1.3 && charge.height()<1.9,"Charging hand should be beside the face: "+charge);
        assertTrue(fire.forward()>charge.forward()+.4,"The hand must thrust toward the target");
    }
    @Test void chargeAndSwordTiltReachFullPowerAtTheReleaseAndTheVortexWidens() {
        assertEquals(0,TreasuryTimeline.charge(TreasuryTimeline.CHARGE));assertEquals(1,TreasuryTimeline.charge(TreasuryTimeline.FIRE));
        assertEquals(0,TreasuryTimeline.thrust(TreasuryTimeline.THRUST));assertEquals(1,TreasuryTimeline.thrust(TreasuryTimeline.FIRE));
        assertTrue(TreasuryTimeline.beamScale(15)>TreasuryTimeline.beamScale(0)*2.5);
    }
    @Test void eaHasThreeIndependentlyRotatingCylindersAndASeparateVortex() throws Exception {
        var ea=model("models/motionrp_ea");int cylinders=0;
        for(var g:ea.getAsJsonArray("groups")) if(g.getAsJsonObject().get("name").getAsString().startsWith("rotating_cylinder")) cylinders++;
        assertEquals(3,cylinders);
        assertEquals(3,ea.getAsJsonArray("animations").get(0).getAsJsonObject().getAsJsonObject("animators").size());
        var level=ea.getAsJsonArray("animations").get(1).getAsJsonObject();assertEquals("level",level.get("name").getAsString());
        var frames=level.getAsJsonObject("animators").entrySet().iterator().next().getValue().getAsJsonObject().getAsJsonArray("keyframes");
        var thrust=frames.get(2).getAsJsonObject();assertEquals((TreasuryTimeline.FIRE-TreasuryTimeline.EA)/20.0,thrust.get("time").getAsDouble());
        assertEquals(-90,thrust.getAsJsonArray("data_points").get(0).getAsJsonObject().get("x").getAsDouble());
        assertEquals("vortex",model("models/motionrp_enuma_vortex").getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(1,model("models/motionrp_golden_gate_mass").getAsJsonArray("groups").size());
    }
    @Test void theGiantsEmbeddedSkinIsExactlyTheUsersPngWithNoPlayerSkinTags() throws Exception {
        var giant=model("models/motionrp_titan_body");String source=giant.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
        byte[] embedded=Base64.getDecoder().decode(source.substring(source.indexOf(',')+1));
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/skins/titan.png"))) { assertArrayEquals(input.readAllBytes(),embedded); }
        assertEquals("d0e83d779b65914e886fee4abcea1108ea09738a384beec0b343cb981c4e7787",dev.aserver.motionrp.pack.PackSnapshot.digest("SHA-256",embedded));
        var image=ImageIO.read(new ByteArrayInputStream(embedded));assertEquals(64,image.getWidth());assertEquals(64,image.getHeight());
        for(var g:giant.getAsJsonArray("groups")) assertFalse(g.getAsJsonObject().get("name").getAsString().contains("_"));
        assertEquals("titan_idle",giant.getAsJsonArray("animations").get(0).getAsJsonObject().get("name").getAsString());
    }
}
