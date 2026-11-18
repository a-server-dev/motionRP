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
        assertEquals("FINISHED",TreasuryTimeline.phase(232,true));
    }
    @Test void theHundredGatePoseKeepsTheWristsSpreadAndStandingStill() throws Exception {
        var pose=model("players/motionrp_gate_of_babylon_100");
        assertEquals(5.8,pose.getAsJsonArray("animations").get(0).getAsJsonObject().get("length").getAsDouble());
        var a=TreasuryHandPath.at(20);var b=TreasuryHandPath.at(30);
        assertTrue(Math.abs(a.right())>.7,"Arm must extend sideways: "+a);
        assertEquals(a.right(),b.right(),1e-5);assertEquals(a.height(),b.height(),1e-5);
        assertTrue(TreasuryHandPath.at(TreasuryTimeline.FIRE).forward()>.7,"Ea hand must thrust forward");
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
