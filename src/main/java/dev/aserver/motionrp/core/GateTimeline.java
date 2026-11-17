package dev.aserver.motionrp.core;

public final class GateTimeline {
    public static final int COUNT=10, OPEN=10, SEQUENTIAL=64, VOLLEY=110, FADE=140, END=160;
    private GateTimeline() {}
    public static int openAt(int gate) { check(gate);return OPEN+gate*2; }
    public static int fireAt(int gate) { check(gate);return gate<6 ? SEQUENTIAL+gate*6 : VOLLEY; }
    public static double opening(int tick,int gate) { return Math.clamp((tick-openAt(gate))/12.0,0,1); }
    private static void check(int gate) { if(gate<0 || gate>=COUNT) throw new IllegalArgumentException("Invalid gate"); }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<OPEN ? "POSE" : tick<44 ? "OPEN"
                : tick<SEQUENTIAL ? "DRAW" : tick<VOLLEY ? "SEQUENTIAL" : tick<FADE ? "VOLLEY" : "FADE";
    }
}
