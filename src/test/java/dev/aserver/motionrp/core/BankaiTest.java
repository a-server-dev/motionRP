package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import com.google.gson.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BankaiTest {
    @Test void theRevealedBlackBladesTipClearsTheFloor() throws Exception {
        JsonObject model;
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/models/motionrp_tensa_zangetsu.bbmodel"))) {
            model=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")) {var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        math.parentTree(model.getAsJsonArray("outliner"),null,parents);
        var animation=model.getAsJsonArray("animations").get(1).getAsJsonObject();
        var tip=math.posedPoint(names.get("tensa_blade"),new double[]{.9,29.2,0},1.2,animation,groups,parents);
        double height=BankaiHandPath.at(BankaiTimeline.READY).height()+tip[1]*.6/16;
        assertTrue(height>.1 && height<.4,"Lowered sword tip must stay above the floor: "+height);
    }
    @Test void theBroadBladesCuttingEdgeFacesDownWhileTheTipPointsForward() throws Exception {
        JsonObject model;
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/models/motionrp_zangetsu_cleaver.bbmodel"))) {
            model=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")) {var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        math.parentTree(model.getAsJsonArray("outliner"),null,parents);var animation=model.getAsJsonArray("animations").get(0).getAsJsonObject();
        String mesh=names.get("cleaver_blade");
        var center=math.posedPoint(mesh,new double[]{0,10,0},.5,animation,groups,parents);
        var edge=math.posedPoint(mesh,new double[]{-1,10,0},.5,animation,groups,parents);
        var tip=math.posedPoint(mesh,new double[]{0,20,0},.5,animation,groups,parents);
        assertTrue(edge[1]<center[1]-.9,"Cutting edge must face downward");
        assertTrue(tip[2]<center[2]-9,"Tip must point forward, not toward the floor");
    }
    @Test void theOffHandBracesTheSwordWristThroughoutTheHeldDeclaration() throws Exception {
        JsonObject model;
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/bettermodel/players/motionrp_zangetsu_bankai.bbmodel"))) {
            model=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var math=new RasenganAssetsTest();Map<String,JsonObject> groups=new HashMap<>();Map<String,String> names=new HashMap<>(),parents=new HashMap<>();
        for(var raw:model.getAsJsonArray("groups")) {var g=raw.getAsJsonObject();String id=g.get("uuid").getAsString();groups.put(id,g);names.put(g.get("name").getAsString(),id);}
        math.parentTree(model.getAsJsonArray("outliner"),null,parents);var animation=model.getAsJsonArray("animations").get(0).getAsJsonObject();
        assertEquals(BankaiTimeline.END/20.0,animation.get("length").getAsDouble());
        var rightFirst=math.posedOrigin(names.get("pri_right_item"),1.2,animation,groups,parents);
        var leftBeforeSupport=math.posedOrigin(names.get("pli_left_item"),1.2,animation,groups,parents);
        assertTrue(Math.abs(rightFirst[2]-leftBeforeSupport[2])>5,"Right hand extends before the left hand follows");
        for(double time:new double[]{1.2,3.4,7.8,9.2}) {
            var hip=math.posedOrigin(names.get("phip_hip"),time,animation,groups,parents);
            assertTrue(hip[1]<7.5,"Hip should be deeply lowered");
            for(String name:List.of("prfl_right_foreleg","plfl_left_foreleg")) {
                var foot=math.posedPoint(names.get(name),new double[]{name.startsWith("pr")?1.875:-1.875,0,0},time,animation,groups,parents);
                assertEquals(0,foot[1],.02,"Crouch must keep both feet grounded");
            }
        }
        for(double time:new double[]{3.4,5.2,7.8,9.2}) {
            var right=math.posedOrigin(names.get("pri_right_item"),time,animation,groups,parents);
            var left=math.posedOrigin(names.get("pli_left_item"),time,animation,groups,parents);
            double distance=0;for(int i=0;i<3;i++) distance+=Math.pow(right[i]-left[i],2);
            assertTrue(Math.sqrt(distance)<1.5,"Left palm must brace the sword wrist");assertTrue(right[2]<-6,"Blade stays in front of the body");
        }
    }
    @Test void pressureCompressesCompletelyBeforeTheSmallBladeAppears() {
        assertEquals("ZANGETSU",BankaiTimeline.phase(BankaiTimeline.DRAW));assertEquals("CHARGE",BankaiTimeline.phase(BankaiTimeline.CHARGE));
        assertEquals("BANKAI",BankaiTimeline.phase(BankaiTimeline.BANKAI));assertEquals("COMPRESS",BankaiTimeline.phase(BankaiTimeline.COMPRESS));
        assertEquals("REVEAL",BankaiTimeline.phase(BankaiTimeline.REVEAL));assertEquals("FINISHED",BankaiTimeline.phase(BankaiTimeline.END));
        assertEquals(0,BankaiTimeline.cocoonScale(BankaiTimeline.BANKAI-1));assertEquals(4.2,BankaiTimeline.cocoonScale(BankaiTimeline.COMPRESS),1e-8);
        assertTrue(BankaiTimeline.cocoonScale((BankaiTimeline.COMPRESS+BankaiTimeline.REVEAL)/2)<BankaiTimeline.cocoonScale(BankaiTimeline.COMPRESS));assertEquals(0,BankaiTimeline.cocoonScale(BankaiTimeline.REVEAL));
        for(int tick=0;tick<BankaiTimeline.END;tick++) {
            var hand=BankaiHandPath.at(tick);
            assertTrue(Double.isFinite(hand.right()) && Double.isFinite(hand.forward()) && Double.isFinite(hand.height()));
            assertTrue(hand.height()>.1 && hand.height()<2.5);
        }
    }
}
