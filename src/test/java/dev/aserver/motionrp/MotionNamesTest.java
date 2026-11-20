package dev.aserver.motionrp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MotionNamesTest {
    @Test void shippedLabelsMigrateWithoutOverwritingCustomNames(){
        assertEquals("王の財宝(慢心)",MotionNames.updated("gate_of_babylon","王の財宝"));
        assertEquals("王の財宝",MotionNames.updated("gate_of_babylon_100","王の財宝・百門"));
        assertEquals("王の財宝",MotionNames.updated("gate_of_babylon_100","王の財宝"));
        assertEquals("私の盾",MotionNames.updated("susanoo","私の盾"));
        assertEquals("独自モーション",MotionNames.updated("custom","独自モーション"));
        assertNull(MotionNames.updated("broly",null));
    }
}
