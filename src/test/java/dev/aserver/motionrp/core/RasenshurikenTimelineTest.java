package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.aserver.motionrp.core.RasenshurikenTimeline.*;

class RasenshurikenTimelineTest {
    @Test void airborneArcReachesEightBlocksAtReleaseAndLandsBeforeSceneEnds() {
        assertEquals(0,height(JUMP));assertEquals(8,height(THROW),1e-10);assertEquals(0,height(LAND));
        assertEquals(24/1.5,THROW-JUMP);assertEquals(8,RANGE);
        for (int tick=0;tick<END;tick++) {
            assertTrue(height(tick)>=0 && height(tick)<=8);
            if (tick>JUMP && tick<THROW) assertTrue(height(tick)>height(tick-1));
            if (tick>THROW && tick<LAND) assertTrue(height(tick)<height(tick-1));
            assertTrue(Math.abs(height(tick)-height(tick-1))<.8);
        }
        assertEquals(Phase.RAISE,phase(RAISE));assertEquals(Phase.JUMP,phase(JUMP));
        assertEquals(Phase.THROW,phase(THROW));assertEquals(Phase.IMPACT,phase(IMPACT));
        assertEquals(Phase.FINISHED,phase(END));assertEquals(Phase.FINISHED,phase(-1));
    }
}
