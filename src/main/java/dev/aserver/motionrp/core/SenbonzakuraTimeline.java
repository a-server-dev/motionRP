package dev.aserver.motionrp.core;

public final class SenbonzakuraTimeline {
    public static final int DROP=4, RISE=16, BLOOM=64, ORBIT=76, SURGE=152, IMPACT=176, FADE=188, END=212;
    public static final int SWORDS=12, CLOUDS=8, RISE_TICKS=16;
    public record Blade(double right,double forward) {}
    public static Blade blade(int index) {
        if(index<0 || index>=SWORDS) throw new IllegalArgumentException("Invalid blade");
        return new Blade(index%2==0 ? -2.4 : 2.4,-5+(index/2)*2.2);
    }
    private SenbonzakuraTimeline() {}
    public static int swordAt(int index) {
        if(index<0 || index>=SWORDS) throw new IllegalArgumentException("Invalid blade");
        return RISE+(index/2)*4;
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<RISE ? "DROP" : tick<BLOOM ? "RISE"
                : tick<ORBIT ? "BLOOM" : tick<SURGE ? "ORBIT" : tick<IMPACT ? "SURGE"
                : tick<FADE ? "IMPACT" : "FADE";
    }
}
