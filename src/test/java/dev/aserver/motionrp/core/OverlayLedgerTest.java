package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OverlayLedgerTest {
    @Test void viewerOptOutRestoresAllOverlappingCellsAndAllowsFreshOptIn(){
        var ledger=new OverlayLedger<String,Integer>();var a=new Object();var b=new Object();
        ledger.update(a,Map.of("viewer",Set.of(1,2)));ledger.update(b,Map.of("viewer",Set.of(2,3)));
        assertEquals(Set.of(1,2,3),ledger.removeViewer("viewer"));assertTrue(ledger.release(a).isEmpty());
        assertEquals(Set.of(2,3),ledger.update(b,Map.of("viewer",Set.of(2,3))).get("viewer").shown());
    }
    @Test void overlappingOwnersOnlyRestoreTheirLastReference() {
        var ledger=new OverlayLedger<String,Integer>();Object a=new String("cast"),b=new String("cast");
        assertEquals(Set.of(1,2),ledger.update(a,Map.of("viewer",Set.of(1,2))).get("viewer").shown());
        assertEquals(Set.of(3),ledger.update(b,Map.of("viewer",Set.of(2,3))).get("viewer").shown());
        assertEquals(Set.of(1),ledger.release(a).get("viewer").restored());
        assertEquals(Set.of(2,3),ledger.release(b).get("viewer").restored());
        assertTrue(ledger.release(b).isEmpty());assertTrue(ledger.clear().isEmpty());
    }
    @Test void leavingAndRejoiningDoesNotReuseStaleClientState() {
        var ledger=new OverlayLedger<String,Integer>();var owner=new Object();
        ledger.update(owner,Map.of("a",Set.of(1),"b",Set.of(1,2)));
        ledger.forget("a");
        var changes=ledger.update(owner,Map.of("a",Set.of(1),"b",Set.of(1,2)));
        assertEquals(Set.of(1),changes.get("a").shown());assertFalse(changes.containsKey("b"));
        assertEquals(Map.of("a",Set.of(1),"b",Set.of(1,2)),ledger.clear());assertTrue(ledger.release(owner).isEmpty());
    }
    @Test void audienceAndShapeChangesRestoreOnlyTheRemovedCells() {
        var ledger=new OverlayLedger<String,Integer>();var owner=new Object();
        ledger.update(owner,Map.of("a",Set.of(1,2),"b",Set.of(1)));
        var changes=ledger.update(owner,Map.of("a",Set.of(2,3)));
        assertEquals(Set.of(3),changes.get("a").shown());assertEquals(Set.of(1),changes.get("a").restored());
        assertEquals(Set.of(1),changes.get("b").restored());
    }
}
