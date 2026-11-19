package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceTest {
    @Test void commandCategoriesStaySeparateAndAliasesAreCaseInsensitive() {
        assertEquals(List.of(Performance.RONALDO,Performance.MBAPPE),Performance.choices("goal"));
        assertEquals(List.of(Performance.YAMAKAWA,Performance.MATSUDA),Performance.choices("hr"));
        assertEquals(Performance.RONALDO,Performance.find("goal","SIU").orElseThrow());
        assertEquals(Performance.YAMAKAWA,Performance.find("hr","DOSUKOI").orElseThrow());
        assertEquals(Performance.MATSUDA,Performance.find("hr","ATSUO").orElseThrow());
        assertTrue(Performance.find("goal","yamakawa").isEmpty());
        assertTrue(Performance.find("hr","ronaldo").isEmpty());
        assertTrue(Performance.find("goal","unknown").isEmpty());
    }
}
