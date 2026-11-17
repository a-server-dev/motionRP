package dev.aserver.motionrp.core;

public final class SusanooTimeline {
    public static final int RIBS=8, ARMOR=32, READY=60, SLASH=80, IMPACT=94, FADE=108, END=128;
    private SusanooTimeline() {}
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<RIBS ? "SIGNS" : tick<ARMOR ? "RIBS"
                : tick<READY ? "ARMOR" : tick<SLASH ? "WINDUP" : tick<IMPACT ? "SLASH"
                : tick<FADE ? "IMPACT" : "FADE";
    }
}
