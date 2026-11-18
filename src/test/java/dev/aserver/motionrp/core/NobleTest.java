package dev.aserver.motionrp.core;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NobleTest {
    @Test void theLanceButtStaysAboveTheFloorThroughoutTheWindupAndThrust() throws Exception {
        var m=model("models/motionrp_gae_bolg_spear");var math=new RasenganAssetsTest();
        Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:m.getAsJsonArray("groups")) {var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        double spearTip=0;
        for(var raw:m.getAsJsonArray("elements"))spearTip=Math.max(spearTip,raw.getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble());
        assertEquals(GaeBolgTimeline.TIP_LENGTH,spearTip*.7/16,.002,"Impact must be placed at the spear tip, not its grip");
        math.parentTree(m.getAsJsonArray("outliner"),null,parents);var animation=m.getAsJsonArray("animations").get(0).getAsJsonObject();
        for(int tick=NobleTimeline.draw(false);tick<NobleTimeline.fade(false);tick++) {
            if(tick>=GaeBolgTimeline.THROW && tick<GaeBolgTimeline.CATCH)continue;
            var butt=math.posedPoint(names.get("lance_lower"),new double[]{0,-28,-.75},(tick-NobleTimeline.draw(false))/20.0,animation,groups,parents);
            double height=NobleHandPath.at(false,tick).height()+butt[1]*.7/16;
            assertTrue(height>.03,"Lance butt clipped the floor at tick "+tick+": "+height);
        }
    }
    private JsonObject model(String path) throws Exception {
        try(var in=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/"+path+".bbmodel"))) {
            return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @Test void palmsAndWeaponReleaseTimingsMatchTheTwoDifferentPerformances() throws Exception {
        for(boolean gold:new boolean[]{true,false}) {
            String id=gold?"excalibur":"gae_bolg";var player=model("players/motionrp_"+id);
            assertEquals(NobleTimeline.end(gold)/20.0,player.getAsJsonArray("animations").get(0).getAsJsonObject().get("length").getAsDouble());
            for(int tick=0;tick<NobleTimeline.end(gold);tick++) {
                var palm=NobleHandPath.at(gold,tick);assertTrue(Double.isFinite(palm.height()+palm.forward()+palm.right()));assertTrue(palm.height()>.3 && palm.height()<2.4);
            }
            assertEquals("RELEASE",NobleTimeline.phase(NobleTimeline.release(gold),gold));assertEquals("FINISHED",NobleTimeline.phase(NobleTimeline.end(gold),gold));
        }
        assertTrue(NobleHandPath.at(true,80).height()>1.9,"Holy sword rises above the head");
        assertTrue(NobleHandPath.at(false,GaeBolgTimeline.APEX).height()>1.8,"Lance hand raises above the head before the throw");
    }
    @Test void zangetsuHasTheClassicLongAsymmetricBlackSpineAndBroadWhiteEdge() throws Exception {
        var m=model("models/motionrp_zangetsu_cleaver");double width=0,length=0;int bevel=0;
        for(var raw:m.getAsJsonArray("elements")) {var e=raw.getAsJsonObject();String name=e.get("name").getAsString();if(name.startsWith("cleaver_spine")) {width=Math.max(width,e.getAsJsonArray("to").get(0).getAsDouble()-e.getAsJsonArray("from").get(0).getAsDouble());length=Math.max(length,e.getAsJsonArray("to").get(1).getAsDouble());}if(name.startsWith("steel_bevel"))bevel++;}
        assertTrue(width>=9 && length/width>4 && length/width<5.5 && bevel>=16);
    }
}
