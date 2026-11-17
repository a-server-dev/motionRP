package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.aserver.motionrp.core.RasenshurikenTimeline.*;

class RasenshurikenTimelineTest {
    @Test void airborneArcReachesFiveBlocksAtReleaseAndLandsBeforeSceneEnds() {
        assertEquals(0,height(JUMP));assertEquals(5,height(THROW),1e-10);assertEquals(0,height(LAND));
        for (int tick=0;tick<END;tick++) {
            assertTrue(height(tick)>=0 && height(tick)<=5);
            if (tick>JUMP && tick<THROW) assertTrue(height(tick)>height(tick-1));
            if (tick>THROW && tick<LAND) assertTrue(height(tick)<height(tick-1));
            assertTrue(Math.abs(height(tick)-height(tick-1))<.4);
        }
        assertEquals(Phase.RAISE,phase(RAISE));assertEquals(Phase.JUMP,phase(JUMP));
        assertEquals(Phase.THROW,phase(THROW));assertEquals(Phase.IMPACT,phase(IMPACT));
        assertEquals(Phase.FINISHED,phase(END));assertEquals(Phase.FINISHED,phase(-1));
    }
}
