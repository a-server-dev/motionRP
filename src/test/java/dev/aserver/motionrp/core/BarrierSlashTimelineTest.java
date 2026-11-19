package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BarrierSlashTimelineTest {
    @Test void sevenLayersUnfoldBeforeTheHoldAndCrescentHasBoundedTravel(){
        double size=5;int previous=0;for(int i=0;i<7;i++){assertEquals((1.89-i*.14)*(i==0?1.5:2),BarrierSlashTimeline.layerScale(i),1e-9);if(i>1)assertTrue(BarrierSlashTimeline.layerScale(i)<size);size=BarrierSlashTimeline.layerScale(i);int t=BarrierSlashTimeline.layerStart(i);assertTrue(t>previous && t<72);previous=t;}
        assertThrows(IllegalArgumentException.class,()->BarrierSlashTimeline.layerStart(7));
        for(int i=1;i<7;i++)assertEquals(.8,BarrierSlashTimeline.layerForward(i)-BarrierSlashTimeline.layerForward(i-1),1e-9);
        assertEquals(0,BarrierSlashTimeline.distance(87));assertEquals(16,BarrierSlashTimeline.distance(108));assertEquals(32,BarrierSlashTimeline.distance(10000));
        assertTrue(BarrierSlashTimeline.GETSUGA_IMPACT<BarrierSlashTimeline.GETSUGA_END);
    }
}
