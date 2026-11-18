package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SummonTimelineTest {
    @Test void twentyScatteredGatesCompleteFasterAndAllJoinTheVolley() {
        Set<Integer> sequential=new HashSet<>();
        Set<Double> heights=new HashSet<>(),depths=new HashSet<>();int volley=0;
        for(int gate=0;gate<GateTimeline.COUNT;gate++) {
            assertEquals(0,GateTimeline.opening(GateTimeline.openAt(gate),gate));
            assertEquals(1,GateTimeline.opening(GateTimeline.fireAt(gate),gate));
            assertTrue(GateTimeline.fireAt(gate)+GateTimeline.FLIGHT_TICKS<GateTimeline.FADE);
            sequential.add(GateTimeline.fireAt(gate));
            if(GateTimeline.shoots(GateTimeline.VOLLEY,gate)) volley++;
            heights.add(GateTimeline.portal(gate).up());depths.add(GateTimeline.portal(gate).forward());
        }
        assertEquals(20,sequential.size());assertEquals("FINISHED",GateTimeline.phase(GateTimeline.END));
        int rapid=0;
        for(int tick=0;tick<GateTimeline.VOLLEY;tick++) for(int gate=0;gate<20;gate++) if(GateTimeline.shoots(tick,gate)) rapid++;
        assertEquals(30,rapid);
        assertEquals(20,volley);assertTrue(heights.size()>16);assertTrue(depths.size()>12);
        assertEquals(116,GateTimeline.END);assertEquals(2,GateTimeline.FLIGHT_TICKS);
        assertTrue(GateTimeline.portal(17).right()-GateTimeline.portal(10).right()>16);
        for(int i=1;i<20;i++) assertEquals(1,GateTimeline.fireAt(i)-GateTimeline.fireAt(i-1));
        assertThrows(IllegalArgumentException.class,()->GateTimeline.fireAt(-1));
        assertThrows(IllegalArgumentException.class,()->GateTimeline.fireAt(GateTimeline.COUNT));
    }
    @Test void cherryBladesRiseFullyBeforeDissolvingThenSwirlAndConverge() {
        for(int i=0;i<SenbonzakuraTimeline.SWORDS;i++) {
            assertTrue(SenbonzakuraTimeline.swordAt(i)>=SenbonzakuraTimeline.RISE);
            assertTrue(SenbonzakuraTimeline.swordAt(i)+SenbonzakuraTimeline.RISE_TICKS<=SenbonzakuraTimeline.BLOOM);
            var pose=SenbonzakuraTimeline.blade(i);
            assertEquals(2.6,Math.abs(pose.right()));
            assertEquals(i%2==0?180:0,pose.yawOffset());
            if(i%2==1) assertEquals(SenbonzakuraTimeline.blade(i-1).forward(),pose.forward());
            if(i>=2) assertTrue(pose.forward()>SenbonzakuraTimeline.blade(i-2).forward());
        }
        assertEquals(SenbonzakuraTimeline.BLOOM,SenbonzakuraTimeline.dissolveAt(47));
        assertEquals(SenbonzakuraTimeline.BLOOM+15,SenbonzakuraTimeline.dissolveAt(0));
        for(int i=2;i<48;i++) assertTrue(SenbonzakuraTimeline.dissolveAt(i)<=SenbonzakuraTimeline.dissolveAt(i-2));
        assertTrue(SenbonzakuraTimeline.RISE-SenbonzakuraTimeline.DROP>=28);
        assertEquals(48,SenbonzakuraTimeline.SWORDS);
        assertTrue(SenbonzakuraTimeline.blade(0).forward()<0 && SenbonzakuraTimeline.blade(47).forward()>0);
        assertTrue(SenbonzakuraTimeline.blade(2).forward()-SenbonzakuraTimeline.blade(0).forward()<.7);
        assertTrue(SenbonzakuraTimeline.SURGE-SenbonzakuraTimeline.ORBIT>=76);
        assertEquals("BLOOM",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.BLOOM));
        assertEquals("ORBIT",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.ORBIT));
        assertEquals("SURGE",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.SURGE));
        assertEquals("IMPACT",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.IMPACT));
        assertEquals("FINISHED",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.END));
    }
    @Test void titanKeepsTheGiantFullSizeWhileTheHumanLeavesItsNape() {
        for(double original:new double[]{.75,1,1.25,2}) {
            assertEquals(original,TitanTimeline.scale(0,original));
            assertEquals(10,TitanTimeline.scale(TitanTimeline.GROWN,original));
            assertEquals(original,TitanTimeline.scale(TitanTimeline.END,original));
            assertEquals(10,TitanTimeline.scale(TitanTimeline.EXIT+10,original));
        }
        assertEquals("FLASH",TitanTimeline.phase(TitanTimeline.FLASH));
        assertEquals("GIANT",TitanTimeline.phase(TitanTimeline.GROWN));
        assertEquals("FINISHED",TitanTimeline.phase(TitanTimeline.END));
    }
    @Test void napeExitLeadsBehindTheGiantAndAcceleratesDownToTheLanding() {
        for(double ground:new double[]{-2,0,1}) {
            var start=TitanTimeline.exitPose(TitanTimeline.EXIT,ground);
            var end=TitanTimeline.exitPose(TitanTimeline.LAND,ground);
            assertEquals(TitanTimeline.NAPE_HEIGHT,start.height());assertEquals(-1.8,start.forward());
            assertEquals(ground,end.height(),1e-9);assertEquals(-5,end.forward());
            for(int tick=TitanTimeline.DIVE+1;tick<=TitanTimeline.LAND;tick++) {
                var before=TitanTimeline.exitPose(tick-1,ground);var next=TitanTimeline.exitPose(tick,ground);
                assertTrue(next.height()<before.height());assertTrue(next.forward()<before.forward());
            }
        }
    }
    @Test void guardianFormsBeforeWindingUpAndLeavesTimeForImpactAndDissolve() {
        int[] moments={SusanooTimeline.RIBS,SusanooTimeline.ARMOR,SusanooTimeline.READY,SusanooTimeline.SLASH,
                SusanooTimeline.IMPACT,SusanooTimeline.FADE,SusanooTimeline.END};
        String[] phases={"RIBS","ARMOR","WINDUP","SLASH","IMPACT","FADE","FINISHED"};
        for(int i=0;i<moments.length;i++) {
            if(i>0) assertTrue(moments[i]>moments[i-1]);
            assertEquals(phases[i],SusanooTimeline.phase(moments[i]));
        }
        assertTrue(SusanooTimeline.SLASH+20<=SusanooTimeline.FADE);
    }
}
