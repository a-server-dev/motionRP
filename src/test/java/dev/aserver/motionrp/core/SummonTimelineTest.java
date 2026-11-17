package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SummonTimelineTest {
    @Test void tenScatteredGatesOpenBeforeSequentialShotsAndAllTenJoinTheVolley() {
        Set<Integer> sequential=new HashSet<>();
        Set<Double> heights=new HashSet<>(),depths=new HashSet<>();int volley=0;
        for(int gate=0;gate<GateTimeline.COUNT;gate++) {
            assertEquals(0,GateTimeline.opening(GateTimeline.openAt(gate),gate));
            assertEquals(1,GateTimeline.opening(GateTimeline.fireAt(gate),gate));
            assertTrue(GateTimeline.fireAt(gate)+18<GateTimeline.FADE);
            if(gate<6) sequential.add(GateTimeline.fireAt(gate));
            else assertEquals(GateTimeline.VOLLEY,GateTimeline.fireAt(gate));
            if(GateTimeline.shoots(GateTimeline.VOLLEY,gate)) volley++;
            heights.add(GateTimeline.portal(gate).up());depths.add(GateTimeline.portal(gate).forward());
        }
        assertEquals(6,sequential.size());assertEquals("FINISHED",GateTimeline.phase(GateTimeline.END));
        assertEquals(10,volley);assertTrue(heights.size()>8);assertTrue(depths.size()>7);
        assertThrows(IllegalArgumentException.class,()->GateTimeline.fireAt(-1));
        assertThrows(IllegalArgumentException.class,()->GateTimeline.fireAt(10));
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
