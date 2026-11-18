package dev.aserver.motionrp.core;

public final class BankaiTimeline {
    public static final int DRAW=12,RIGHT_READY=24,SUPPORT=40,CHARGE=68,BANKAI=104,COMPRESS=156,REVEAL=184,READY=208,END=256;
    private BankaiTimeline() {}
    public static double cocoonScale(int tick) {
        if(tick<BANKAI || tick>=REVEAL) return 0;
        if(tick<COMPRESS) return .2+4.0*Math.clamp((tick-BANKAI)/20.0,0,1);
        return 4.2*Math.clamp((REVEAL-tick)/(double)(REVEAL-COMPRESS),0,1);
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END?"FINISHED":tick<CHARGE?"ZANGETSU":tick<BANKAI?"CHARGE":tick<COMPRESS?"BANKAI"
                :tick<REVEAL?"COMPRESS":tick<READY?"REVEAL":"TENSA_ZANGETSU";
    }
}
