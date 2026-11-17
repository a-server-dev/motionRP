package dev.aserver.motionrp.core;

public final class GateTimeline {
    public static final int COUNT=10, OPEN=10, SEQUENTIAL=64, VOLLEY=110, FADE=140, END=160;
    public record Portal(double right,double forward,double up,double scale) {}
    private static final Portal[] PORTALS={
        new Portal(-3.3,-1.8,2.25,.95),new Portal(-1.8,-2.5,3.15,1.05),
        new Portal(-.35,-1.6,2.5,.9),new Portal(1.3,-2.1,3.45,1.1),new Portal(3.15,-2.6,2.7,.85),
        new Portal(-3,-2.9,4.5,1.1),new Portal(-1.4,-2.3,5.1,.9),new Portal(.3,-3.2,4.35,1),
        new Portal(2.2,-2.6,5.05,1.05),new Portal(3.7,-3.4,4.4,.85)};
    private GateTimeline() {}
    public static int openAt(int gate) { check(gate);return OPEN+gate*2; }
    public static int fireAt(int gate) { check(gate);return gate<6 ? SEQUENTIAL+gate*6 : VOLLEY; }
    public static Portal portal(int gate) { check(gate);return PORTALS[gate]; }
    public static boolean shoots(int tick,int gate) { check(gate);return tick==VOLLEY || (gate<6 && tick==fireAt(gate)); }
    public static double opening(int tick,int gate) { return Math.clamp((tick-openAt(gate))/12.0,0,1); }
    private static void check(int gate) { if(gate<0 || gate>=COUNT) throw new IllegalArgumentException("Invalid gate"); }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<OPEN ? "POSE" : tick<44 ? "OPEN"
                : tick<SEQUENTIAL ? "DRAW" : tick<VOLLEY ? "SEQUENTIAL" : tick<FADE ? "VOLLEY" : "FADE";
    }
}
