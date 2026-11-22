package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GiveAccessTest {
 @Test void namedPlayerCanDistributeWithoutOperator(){assertTrue(GiveAccess.playerAllowed("EmeraldREX91",false));assertTrue(GiveAccess.playerAllowed("emeraldrex91",false));}
 @Test void operatorsCanDistribute(){assertTrue(GiveAccess.playerAllowed("OtherPlayer",true));}
 @Test void otherPlayersAndSimilarNamesAreRejected(){for(String name:new String[]{"OtherPlayer","EmeraldREX910","xEmeraldREX91","EmeraldREX9I",null})assertFalse(GiveAccess.playerAllowed(name,false));}
}
