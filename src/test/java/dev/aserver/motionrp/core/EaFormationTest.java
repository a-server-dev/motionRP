package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.aserver.motionrp.core.TreasuryTimeline.*;

class EaFormationTest {
    @Test void branchesGrowIntoABoundedTreeThenConvergeIntoTheSwordAxis() {
        assertTrue(EaFormation.points(BRANCH-1).isEmpty());assertTrue(EaFormation.points(BRANCH).isEmpty());
        int early=EaFormation.points(BRANCH+8).size(),full=EaFormation.points(CONVERGE).size();
        assertTrue(early>0 && full>early*2);assertTrue(full<650,"Bound the particle work per scene tick");
        double before=EaFormation.points(CONVERGE).stream().mapToDouble(p->Math.hypot(p.right(),p.forward())).max().orElseThrow();
        double mid=EaFormation.points((CONVERGE+EA_READY)/2).stream().mapToDouble(p->Math.hypot(p.right(),p.forward())).max().orElseThrow();
        assertTrue(before>2 && mid<before*.6);
        for(var p:EaFormation.points(EA_READY)) {assertEquals(0,p.right(),1e-8);assertEquals(0,p.forward(),1e-8);assertTrue(p.up()>=.12 && p.up()<=1.63);}
        assertTrue(EaFormation.points(EA_READY+1).isEmpty());
    }
    @Test void keyRevealsFromItsTipTowardTheHiltAboveTheGate() {
        assertTrue(EaFormation.keyBandVisible(15,KEY));assertFalse(EaFormation.keyBandVisible(8,KEY));
        assertTrue(EaFormation.keyBandVisible(8,(KEY+KEY_READY)/2));assertFalse(EaFormation.keyBandVisible(2,(KEY+KEY_READY)/2));
        assertTrue(EaFormation.keyBandVisible(2,KEY_READY));
    }
}
