package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InfinityCastleTimelineTest {
 @Test void eightSecondFallAndThreeBlockExit(){
  assertEquals(160,InfinityCastleTimeline.RETURN-InfinityCastleTimeline.FALL);
  assertEquals(0,InfinityCastleTimeline.depth(InfinityCastleTimeline.FALL));
  assertEquals(72,InfinityCastleTimeline.depth(InfinityCastleTimeline.RETURN));
  assertEquals(3,InfinityCastleTimeline.exitHeight(InfinityCastleTimeline.RETURN));
  assertEquals(0,InfinityCastleTimeline.exitHeight(InfinityCastleTimeline.LAND));
  double previous=0;for(int tick=0;tick<250;tick++){double next=InfinityCastleTimeline.depth(tick);assertTrue(next>=previous&&next<=72);previous=next;}
 }
}
