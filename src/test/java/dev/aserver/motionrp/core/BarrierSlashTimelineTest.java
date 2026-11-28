package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BarrierSlashTimelineTest {
    @Test void oneHandedSweepStaysOnTheRightPalmAndPrecedesTheOverheadCharge(){
        assertTrue(BarrierSlashTimeline.MASK_COMPLETE<BarrierSlashTimeline.GETSUGA_PREFIX);
        assertTrue(BarrierSlashTimeline.GETSUGA_PREFIX<BarrierSlashTimeline.GETSUGA_FLOURISH_END);
        assertTrue(BarrierSlashTimeline.GETSUGA_FLOURISH_END<BarrierSlashTimeline.GETSUGA_CHARGE);
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(int tick=BarrierSlashTimeline.GETSUGA_PREFIX;tick<=BarrierSlashTimeline.GETSUGA_FLOURISH_END;tick++){
            var grip=GetsugaBladePath.at(tick).grip();var hand=BarrierHandPath.at(false,tick);var left=BarrierHandPath.left(tick);
            assertEquals(hand.right(),grip.right(),1e-7);assertEquals(hand.forward(),grip.forward(),1e-7);assertEquals(hand.height(),grip.height(),1e-7);
            double gap=Math.sqrt(Math.pow(left.right()-hand.right(),2)+Math.pow(left.forward()-hand.forward(),2)+Math.pow(left.height()-hand.height(),2));
            assertTrue(gap>.35,"The left hand is free during the clearing swing");
            double tip=GetsugaBladePath.at(tick).tip().right();min=Math.min(min,tip);max=Math.max(max,tip);
        }
        assertTrue(max-min>2.5,"The clearing swing must visibly sweep across the actor");
    }

    @Test void sevenLayersUnfoldBeforeTheHoldAndCrescentHasBoundedTravel(){
        double size=5;int previous=0;for(int i=0;i<7;i++){assertEquals((1.89-i*.14)*(i==0?1.5:2),BarrierSlashTimeline.layerScale(i),1e-9);if(i>1)assertTrue(BarrierSlashTimeline.layerScale(i)<size);size=BarrierSlashTimeline.layerScale(i);int t=BarrierSlashTimeline.layerStart(i);assertTrue(t>previous && t<72);previous=t;}
        assertThrows(IllegalArgumentException.class,()->BarrierSlashTimeline.layerStart(7));
        for(int i=1;i<7;i++)assertEquals(.8,BarrierSlashTimeline.layerForward(i)-BarrierSlashTimeline.layerForward(i-1),1e-9);
        assertEquals(0,BarrierSlashTimeline.distance(BarrierSlashTimeline.GETSUGA_RELEASE-1));assertEquals(16,BarrierSlashTimeline.distance(BarrierSlashTimeline.GETSUGA_RELEASE+20));assertEquals(32,BarrierSlashTimeline.distance(10000));
        assertTrue(BarrierSlashTimeline.GETSUGA_PREFIX-BarrierSlashTimeline.MASK_COMPLETE>=18);
        assertTrue(BarrierSlashTimeline.GETSUGA_IMPACT<BarrierSlashTimeline.GETSUGA_END);
    }
}
