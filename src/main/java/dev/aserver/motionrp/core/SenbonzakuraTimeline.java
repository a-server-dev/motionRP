package dev.aserver.motionrp.core;

public final class SenbonzakuraTimeline {
    public static final int DROP=4, RISE=16, BLOOM=50, ORBIT=60, SURGE=92, IMPACT=112, FADE=124, END=144;
    public static final int SWORDS=12, CLOUDS=6;
    private SenbonzakuraTimeline() {}
    public static int swordAt(int index) {
        if(index<0 || index>=SWORDS) throw new IllegalArgumentException("Invalid blade");
        return RISE+index*2;
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<RISE ? "DROP" : tick<BLOOM ? "RISE"
                : tick<ORBIT ? "BLOOM" : tick<SURGE ? "ORBIT" : tick<IMPACT ? "SURGE"
                : tick<FADE ? "IMPACT" : "FADE";
    }
}
