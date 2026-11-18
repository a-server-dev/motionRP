package dev.aserver.motionrp.core;

public final class DragonBallTimeline {
    public static final int RISE=8,RISE_END=48,SPIRIT_RELEASE=106,SPIRIT_IMPACT=132,SPIRIT_FADE=154,SPIRIT_END=176;
    public static final int KAME_RELEASE=56,KAME_IMPACT=72,KAME_FADE=110,KAME_END=128;
    public static final double HEIGHT=50,DIAMETER=80;
    private DragonBallTimeline() {}
    public static double height(int tick) {return HEIGHT*Math.clamp((tick-RISE)/(double)(RISE_END-RISE),0,1);}
    public static int end(boolean spirit) {return spirit?SPIRIT_END:KAME_END;}
    public static String phase(int tick,boolean spirit) {
        if(tick<0 || tick>=end(spirit))return "FINISHED";
        if(spirit) return tick<RISE?"POSE":tick<RISE_END?"RISE":tick<SPIRIT_RELEASE?"CHARGE":tick<SPIRIT_IMPACT?"THROW":tick<SPIRIT_FADE?"IMPACT":"FADE";
        return tick<16?"POSE":tick<KAME_RELEASE?"CHARGE":tick<KAME_IMPACT?"RELEASE":tick<KAME_FADE?"IMPACT":"FADE";
    }
}
