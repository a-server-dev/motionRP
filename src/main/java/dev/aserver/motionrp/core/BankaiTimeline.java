package dev.aserver.motionrp.core;

public final class BankaiTimeline {
    public static final int DRAW=8,CHARGE=40,BANKAI=68,COMPRESS=96,REVEAL=112,READY=128,END=176;
    private BankaiTimeline() {}
    public static double cocoonScale(int tick) {
        if(tick<BANKAI || tick>=REVEAL) return 0;
        if(tick<COMPRESS) return .2+2.2*Math.clamp((tick-BANKAI)/20.0,0,1);
        return 2.4*Math.clamp((REVEAL-tick)/(double)(REVEAL-COMPRESS),0,1);
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END?"FINISHED":tick<CHARGE?"ZANGETSU":tick<BANKAI?"CHARGE":tick<COMPRESS?"BANKAI"
                :tick<REVEAL?"COMPRESS":tick<READY?"REVEAL":"TENSA_ZANGETSU";
    }
}
