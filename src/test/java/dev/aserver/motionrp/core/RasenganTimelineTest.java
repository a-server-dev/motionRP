package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RasenganTimelineTest {
    @Test void dashTravelsFourBlocksOnlyDuringThrustWithBoundedSteps() {
        assertEquals(0, RasenganTimeline.dashDistance(71));
        assertEquals(4, RasenganTimeline.dashDistance(81));
        for (int tick = 0; tick < RasenganTimeline.END; tick++) {
            double step = RasenganTimeline.dashDistance(tick) - RasenganTimeline.dashDistance(tick - 1);
            assertTrue(step >= 0 && step <= .6);
            if (tick < 72 || tick >= 82) assertEquals(0, step);
        }
    }
    @Test void cloneAppearsBeforeOrbAndDisappearsBeforeThrust() {
        assertFalse(RasenganTimeline.at(7).cloneVisible());
        assertTrue(RasenganTimeline.at(8).cloneVisible());
        assertFalse(RasenganTimeline.at(19).orbVisible());
        assertTrue(RasenganTimeline.at(20).cloneVisible());
        assertTrue(RasenganTimeline.at(20).orbVisible());
        assertTrue(RasenganTimeline.at(59).cloneVisible());
        assertFalse(RasenganTimeline.at(60).cloneVisible());
        assertTrue(RasenganTimeline.at(60).orbVisible());
        assertEquals(RasenganTimeline.Phase.THRUST, RasenganTimeline.at(72).phase());
    }

    @Test void formationGrowsThenWindupAndThrustMoveTheOrbAlongTheHand() {
        assertTrue(RasenganTimeline.at(20).scale() < RasenganTimeline.at(40).scale());
        assertEquals(1, RasenganTimeline.at(59).scale(), .0001);
        assertTrue(RasenganTimeline.at(71).forward() < RasenganTimeline.at(60).forward());
        assertTrue(RasenganTimeline.at(81).forward() > RasenganTimeline.at(72).forward());
        assertTrue(RasenganTimeline.at(93).scale() < RasenganTimeline.at(82).scale());
    }

    @Test void finishedOrOutOfRangeFramesNeverLeaveDisplaysAlive() {
        for (int tick : new int[]{-1, 96, 100, Integer.MAX_VALUE}) {
            var frame = RasenganTimeline.at(tick);
            assertEquals(RasenganTimeline.Phase.FINISHED, frame.phase());
            assertFalse(frame.cloneVisible());
            assertFalse(frame.orbVisible());
            assertEquals(0, frame.scale());
        }
        for (int tick = 0; tick < 96; tick++) {
            var frame = RasenganTimeline.at(tick);
            assertTrue(Double.isFinite(frame.scale()) && frame.scale() > 0 && frame.scale() <= 1);
            assertTrue(Double.isFinite(frame.forward()) && frame.forward() > 0 && frame.forward() <= 1.05);
        }
        assertFalse(RasenganTimeline.at(94).orbVisible());
    }
}
