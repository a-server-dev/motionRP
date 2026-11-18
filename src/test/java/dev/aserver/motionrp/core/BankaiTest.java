package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BankaiTest {
    @Test void pressureCompressesCompletelyBeforeTheSmallBladeAppears() {
        assertEquals("ZANGETSU",BankaiTimeline.phase(8));assertEquals("CHARGE",BankaiTimeline.phase(40));
        assertEquals("BANKAI",BankaiTimeline.phase(68));assertEquals("COMPRESS",BankaiTimeline.phase(96));
        assertEquals("REVEAL",BankaiTimeline.phase(112));assertEquals("FINISHED",BankaiTimeline.phase(176));
        assertEquals(0,BankaiTimeline.cocoonScale(67));assertEquals(2.4,BankaiTimeline.cocoonScale(96),1e-8);
        assertTrue(BankaiTimeline.cocoonScale(104)<BankaiTimeline.cocoonScale(96));assertEquals(0,BankaiTimeline.cocoonScale(112));
        for(int tick=0;tick<BankaiTimeline.END;tick++) {
            var hand=BankaiHandPath.at(tick);
            assertTrue(Double.isFinite(hand.right()) && Double.isFinite(hand.forward()) && Double.isFinite(hand.height()));
            assertTrue(hand.height()>.1 && hand.height()<2.5);
        }
    }
}
