package dev.aserver.motionrp.core;

public final class SenbonzakuraTimeline {
    public static final int DROP=4, RISE=32, BLOOM=80, ORBIT=104, SURGE=180, IMPACT=204, FADE=216, END=240;
    public static final int SWORDS=48, CLOUDS=8, RISE_TICKS=16;
    public record Blade(double right,double forward) { public float yawOffset() { return right<0 ? 180 : 0; } }
    public static Blade blade(int index) {
        if(index<0 || index>=SWORDS) throw new IllegalArgumentException("Invalid blade");
        return new Blade(index%2==0 ? -2.6 : 2.6,-8+(index/2)*.65);
    }
    private SenbonzakuraTimeline() {}
    public static int swordAt(int index) {
        if(index<0 || index>=SWORDS) throw new IllegalArgumentException("Invalid blade");
        return RISE+(index/2);
    }
    public static int dissolveAt(int index) {
        blade(index);return BLOOM+(SWORDS/2-1-index/2)*15/(SWORDS/2-1);
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END ? "FINISHED" : tick<RISE ? "DROP" : tick<BLOOM ? "RISE"
                : tick<ORBIT ? "BLOOM" : tick<SURGE ? "ORBIT" : tick<IMPACT ? "SURGE"
                : tick<FADE ? "IMPACT" : "FADE";
    }
}
