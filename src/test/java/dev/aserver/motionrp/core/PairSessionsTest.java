package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PairSessionsTest {
    @Test void recipientConsentReservesBothRolesAndEitherSideCancels(){
        var p=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();var c=UUID.randomUUID();
        assertFalse(p.invite(a,a,0));assertTrue(p.invite(a,b,0));assertFalse(p.invite(c,b,0));
        assertTrue(p.accept(a,b,1).isEmpty());assertTrue(p.accept(c,a,1).isEmpty());
        var s=p.accept(b,a,2).orElseThrow();assertEquals(s,p.session(a).orElseThrow());assertEquals(s,p.session(b).orElseThrow());
        assertFalse(p.invite(a,c,3));assertEquals(Set.of(a,b),p.cancel(b));assertTrue(p.session(a).isEmpty());
        assertTrue(p.cancel(a).isEmpty());assertTrue(p.invite(a,c,4));
    }
    @Test void expiredDeclinedAndShutdownInvitationsCannotStart(){
        var p=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();
        assertTrue(p.invite(a,b,0));assertTrue(p.accept(b,a,30_000).isEmpty());assertTrue(p.invite(a,b,30_000));
        p.cancel(b);assertTrue(p.accept(b,a,30_001).isEmpty());assertTrue(p.invite(a,b,30_002));p.clear();assertTrue(p.accept(b,a,30_003).isEmpty());
    }
    @Test void incomingConsentIsRecipientOnlyExpiresAndGetsANewToken(){
        var p=new PairSessions();var a=UUID.randomUUID();var b=UUID.randomUUID();
        assertTrue(p.invite(a,b,0,"broly"));
        var first=p.incoming(b,1).orElseThrow();
        assertTrue(p.incoming(a,1).isEmpty());assertEquals("broly",first.kind());
        assertTrue(p.accept(b,a,2,"fusion").isEmpty());assertEquals(first,p.incoming(b,2).orElseThrow());
        p.cancel(a);assertTrue(p.incoming(b,3).isEmpty());
        assertTrue(p.invite(a,b,4,"fusion"));assertNotEquals(first.id(),p.incoming(b,5).orElseThrow().id());
        assertTrue(p.incoming(b,30_004).isEmpty());assertTrue(p.invite(a,b,30_004));
    }
    @Test void publicPairItemsExcludePostponedBattle(){
        assertEquals(Set.of("fusion","broly"),Arrays.stream(PairMotion.values()).map(PairMotion::command).collect(java.util.stream.Collectors.toSet()));
        assertEquals(PairMotion.FUSION,PairMotion.fromEffect("pair_fusion").orElseThrow());
        assertTrue(PairMotion.fromEffect("final_battle_naruto").isEmpty());
    }
}
