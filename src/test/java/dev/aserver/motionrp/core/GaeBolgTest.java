package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.aserver.motionrp.core.GaeBolgTimeline.*;

class GaeBolgTest {
    @Test void crouchingStartStaggersBothFeetWithoutDetachingTheThighs() throws Exception {
        com.google.gson.JsonObject m;
        try(var in=java.util.Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/players/motionrp_gae_bolg.bbmodel"))) {
            m=com.google.gson.JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var math=new RasenganAssetsTest();var groups=new java.util.HashMap<String,com.google.gson.JsonObject>();var names=new java.util.HashMap<String,String>();var parents=new java.util.HashMap<String,String>();
        for(var raw:m.getAsJsonArray("groups")) {var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        math.parentTree(m.getAsJsonArray("outliner"),null,parents);var anim=m.getAsJsonArray("animations").get(0).getAsJsonObject();
        var right=math.posedPoint(names.get("prfl_right_foreleg"),new double[]{1.875,0,0},.6,anim,groups,parents);
        var left=math.posedPoint(names.get("plfl_left_foreleg"),new double[]{-1.875,0,0},.6,anim,groups,parents);
        assertTrue(right[2]-left[2]>9,"Runner's rear and front feet must be visibly staggered");
        var shoulder=math.posedOrigin(names.get("pra_right_arm"),THROW/20.0,anim,groups,parents);
        var palm=math.posedOrigin(names.get("pri_right_item"),THROW/20.0,anim,groups,parents);
        double angle=Math.toDegrees(Math.atan2(shoulder[1]-palm[1],Math.hypot(shoulder[0]-palm[0],shoulder[2]-palm[2])));
        assertEquals(60,angle,5,"Throwing arm follows the downward launch angle");
        for(int tick=0;tick<END;tick++) for(boolean r:new boolean[]{true,false}) {
            String thigh=names.get(r?"prl_right_leg":"pll_left_leg");double x=r?1.875:-1.875,t=tick/20.0;
            var socket=math.posedPoint(parents.get(thigh),new double[]{x,11.25,0},t,anim,groups,parents);
            assertArrayEquals(socket,math.posedOrigin(thigh,t,anim,groups,parents),1e-6);
            if(tick<=DASH_END || tick>=LAND) {
                var foot=math.posedPoint(names.get(r?"prfl_right_foreleg":"plfl_left_foreleg"),new double[]{x,0,0},t,anim,groups,parents);
                assertEquals(0,foot[1],.03,"Grounded part of the run at tick "+tick);
            }
        }
    }

    @Test void acceleratesTwentyBlocksInTenTicksThenReachesFifteenBlocksBeforeThrowing() {
        assertEquals(10,DASH_END-DASH);assertEquals(0,distance(DASH));assertEquals(20,distance(DASH_END));
        double previous=0;for(int i=DASH+1;i<=DASH_END;i++) {double step=distance(i)-distance(i-1);assertTrue(step>previous);previous=step;}
        assertEquals(15,height(APEX));assertEquals(15,height(THROW));assertEquals(0,height(LAND));
        assertEquals(60,ANGLE);assertTrue(IMPACT<LAND && LAND<RETURN && RETURN<CATCH && CATCH<END);
        assertTrue(LAND-THROW<=8);assertEquals("RETURN",phase(RETURN));assertEquals("CATCH",phase(CATCH));
        for(int tick=0;tick<END;tick++)assertTrue(height(tick)>=0 && height(tick)<=15);
    }
}
