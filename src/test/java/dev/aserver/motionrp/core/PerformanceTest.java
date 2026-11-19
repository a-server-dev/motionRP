package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceTest {
    @Test void travelRunsThenJumpsAndTurnsWhileSlidingDecelerates(){
        assertEquals(4,PerformanceTravel.distance(Performance.RONALDO,24),.001);
        assertEquals(0,PerformanceTravel.height(Performance.RONALDO,24));
        assertEquals(1.2,PerformanceTravel.height(Performance.RONALDO,35),.001);
        assertEquals(6,PerformanceTravel.distance(Performance.RONALDO,46),.001);
        assertEquals(0,PerformanceTravel.height(Performance.RONALDO,46));
        assertEquals(180,PerformanceTravel.yaw(Performance.RONALDO,112));
        assertEquals(7,PerformanceTravel.distance(Performance.KNEESLIDE,50),.001);
        double first=PerformanceTravel.distance(Performance.KNEESLIDE,25)-PerformanceTravel.distance(Performance.KNEESLIDE,20);
        double last=PerformanceTravel.distance(Performance.KNEESLIDE,50)-PerformanceTravel.distance(Performance.KNEESLIDE,45);
        assertTrue(first>last && last>0);
    }
    @Test void commandCategoriesStaySeparateAndAliasesAreCaseInsensitive() {
        assertEquals(List.of(Performance.RONALDO,Performance.KNEESLIDE,Performance.KAZU,Performance.MBAPPE),Performance.choices("goal"));
        assertEquals(List.of(Performance.DESTASHA,Performance.YAMAKAWA,Performance.MATSUDA),Performance.choices("hr"));
        assertEquals(Performance.RONALDO,Performance.find("goal","SIU").orElseThrow());
        assertEquals(Performance.YAMAKAWA,Performance.find("hr","DOSUKOI").orElseThrow());
        assertEquals(Performance.MATSUDA,Performance.find("hr","ATSUO").orElseThrow());
        assertTrue(Performance.find("goal","yamakawa").isEmpty());
        assertTrue(Performance.find("hr","ronaldo").isEmpty());
        assertTrue(Performance.find("goal","unknown").isEmpty());
    }
}
