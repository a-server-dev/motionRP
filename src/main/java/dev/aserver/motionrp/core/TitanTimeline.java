package dev.aserver.motionrp.core;

public final class TitanTimeline {
    public static final int BITE=8, FLASH=30, TRANSFORM=42, GROWN=60, SHRINK=170, END=206;
    public static final double SCALE=10;
    private TitanTimeline() {}
    public static double scale(int tick,double original) {
        if(tick<TRANSFORM || tick>=END) return original;
        double p=tick<GROWN ? (tick-TRANSFORM)/(double)(GROWN-TRANSFORM)
                : tick<SHRINK ? 1 : (END-tick)/(double)(END-SHRINK);
        p=Math.clamp(p,0,1);p=p*p*(3-2*p);return original+(SCALE-original)*p;
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<FLASH ? "BITE" : tick<TRANSFORM ? "FLASH"
                : tick<GROWN ? "TRANSFORM" : tick<SHRINK ? "GIANT" : "STEAM";
    }
}
