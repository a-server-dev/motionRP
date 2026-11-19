package dev.aserver.motionrp.core;

public final class SusanooTimeline {
    public static final int RIBS=8, ARMOR=32, READY=100, SLASH=128, LEFT_IMPACT=130, RIGHT_SLASH=164, IMPACT=170, FADE=204, END=236;
    private SusanooTimeline() {}
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<RIBS ? "SIGNS" : tick<ARMOR ? "RIBS"
                : tick<READY ? "ARMOR" : tick<SLASH ? "WINDUP" : tick<IMPACT ? "SLASH"
                : tick<FADE ? "IMPACT" : "FADE";
    }
}
