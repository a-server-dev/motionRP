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
            if(gate<15) sequential.add(GateTimeline.fireAt(gate));
            else assertEquals(GateTimeline.VOLLEY,GateTimeline.fireAt(gate));
            if(GateTimeline.shoots(GateTimeline.VOLLEY,gate)) volley++;
            heights.add(GateTimeline.portal(gate).up());depths.add(GateTimeline.portal(gate).forward());
        }
        assertEquals(15,sequential.size());assertEquals("FINISHED",GateTimeline.phase(GateTimeline.END));
        assertEquals(20,volley);assertTrue(heights.size()>16);assertTrue(depths.size()>12);
        assertEquals(116,GateTimeline.END);assertEquals(12,GateTimeline.FLIGHT_TICKS);
        assertTrue(GateTimeline.portal(17).right()-GateTimeline.portal(10).right()>16);
        for(int i=1;i<15;i++) assertEquals(2,GateTimeline.fireAt(i)-GateTimeline.fireAt(i-1));
        assertThrows(IllegalArgumentException.class,()->GateTimeline.fireAt(-1));
        assertThrows(IllegalArgumentException.class,()->GateTimeline.fireAt(GateTimeline.COUNT));
    }
    @Test void cherryBladesRiseFullyBeforeDissolvingThenSwirlAndConverge() {
        for(int i=0;i<SenbonzakuraTimeline.SWORDS;i++) {
            assertTrue(SenbonzakuraTimeline.swordAt(i)>=SenbonzakuraTimeline.RISE);
            assertTrue(SenbonzakuraTimeline.swordAt(i)+SenbonzakuraTimeline.RISE_TICKS<=SenbonzakuraTimeline.BLOOM);
            var pose=SenbonzakuraTimeline.blade(i);
            assertEquals(2.4,Math.abs(pose.right()));
            if(i%2==1) assertEquals(SenbonzakuraTimeline.blade(i-1).forward(),pose.forward());
            if(i>=2) assertTrue(pose.forward()>SenbonzakuraTimeline.blade(i-2).forward());
        }
        assertTrue(SenbonzakuraTimeline.blade(0).forward()<0 && SenbonzakuraTimeline.blade(11).forward()>0);
        assertTrue(SenbonzakuraTimeline.SURGE-SenbonzakuraTimeline.ORBIT>=76);
        assertEquals("BLOOM",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.BLOOM));
        assertEquals("ORBIT",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.ORBIT));
        assertEquals("SURGE",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.SURGE));
        assertEquals("IMPACT",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.IMPACT));
        assertEquals("FINISHED",SenbonzakuraTimeline.phase(SenbonzakuraTimeline.END));
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
