package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BarrierSlashTimelineTest {
    @Test void sevenLayersUnfoldBeforeTheHoldAndCrescentHasBoundedTravel(){
        double size=2;int previous=0;for(int i=0;i<7;i++){assertTrue(BarrierSlashTimeline.layerScale(i)<size);size=BarrierSlashTimeline.layerScale(i);int t=BarrierSlashTimeline.layerStart(i);assertTrue(t>previous && t<72);previous=t;}
        assertThrows(IllegalArgumentException.class,()->BarrierSlashTimeline.layerStart(7));
        assertEquals(0,BarrierSlashTimeline.distance(87));assertEquals(16,BarrierSlashTimeline.distance(108));assertEquals(32,BarrierSlashTimeline.distance(10000));
        assertTrue(BarrierSlashTimeline.GETSUGA_IMPACT<BarrierSlashTimeline.GETSUGA_END);
    }
}
