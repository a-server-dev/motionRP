package dev.aserver.motionrp.core;

public final class GateTimeline {
    public static final int COUNT=20, OPEN=6, DRAW=34, SEQUENTIAL=44, VOLLEY=74, FADE=94, END=108;
    public static final int OPEN_TICKS=8, EMERGE_TICKS=6, FLIGHT_TICKS=12;
    public record Portal(double right,double forward,double up,double scale) {}
    private static final Portal[] PORTALS={
        new Portal(-3.3,-1.8,2.25,.95),new Portal(-1.8,-2.5,3.15,1.05),
        new Portal(-.35,-1.6,2.5,.9),new Portal(1.3,-2.1,3.45,1.1),new Portal(3.15,-2.6,2.7,.85),
        new Portal(-3,-2.9,4.5,1.1),new Portal(-1.4,-2.3,5.1,.9),new Portal(.3,-3.2,4.35,1),
        new Portal(2.2,-2.6,5.05,1.05),new Portal(3.7,-3.4,4.4,.85),
        new Portal(-5.6,-3.1,3.45,.85),new Portal(-4.6,-4.4,5.85,1.15),
        new Portal(-2.8,-4.9,6.65,.85),new Portal(-.75,-3.8,6.2,1.1),
        new Portal(1.15,-5.3,6.95,.8),new Portal(3.35,-4.2,6.3,1.1),
        new Portal(5.25,-3.7,5.5,.9),new Portal(6,-4.8,3.2,1.05),
        new Portal(-5,-2.8,1.65,.8),new Portal(4.8,-2.2,1.45,.95)};
    private GateTimeline() {}
    public static int openAt(int gate) { check(gate);return OPEN+gate; }
    public static int fireAt(int gate) { check(gate);return gate<6 ? SEQUENTIAL+gate*4 : VOLLEY; }
    public static Portal portal(int gate) { check(gate);return PORTALS[gate]; }
    public static boolean shoots(int tick,int gate) { check(gate);return tick==VOLLEY || (gate<6 && tick==fireAt(gate)); }
    public static double opening(int tick,int gate) { return Math.clamp((tick-openAt(gate))/(double)OPEN_TICKS,0,1); }
    private static void check(int gate) { if(gate<0 || gate>=COUNT) throw new IllegalArgumentException("Invalid gate"); }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<OPEN ? "POSE" : tick<DRAW ? "OPEN"
                : tick<SEQUENTIAL ? "DRAW" : tick<VOLLEY ? "SEQUENTIAL" : tick<FADE ? "VOLLEY" : "FADE";
    }
}
