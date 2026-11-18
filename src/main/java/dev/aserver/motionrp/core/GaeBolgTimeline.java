package dev.aserver.motionrp.core;

public final class GaeBolgTimeline {
    public static final int DRAW=8,DASH=24,DASH_END=34,APEX=50,THROW=54,IMPACT=60,LAND=62,RETURN=74,CATCH=86,FADE=100,END=116;
    public static final double RUN=20,HEIGHT=15,ANGLE=60,TIP_LENGTH=1.84;
    private GaeBolgTimeline() {}
    public static double distance(int tick) {double t=Math.clamp((tick-DASH)/10.0,0,1);return RUN*t*t;}
    public static double height(int tick) {
        if(tick<DASH_END || tick>=LAND)return 0;
        if(tick<APEX) {double t=(tick-DASH_END)/(double)(APEX-DASH_END);return HEIGHT*(2*t-t*t);}
        if(tick<THROW)return HEIGHT;
        double t=(tick-THROW)/(double)(LAND-THROW);return HEIGHT*(1-t*t*(3-2*t));
    }
    public static String phase(int tick) {
        return tick<0 || tick>=END?"FINISHED":tick<DASH?"CROUCH":tick<DASH_END?"DASH":tick<APEX?"JUMP":tick<THROW?"AIM"
            :tick<IMPACT?"THROW":tick<LAND?"EXPLOSION":tick<RETURN?"LANDED":tick<CATCH?"RETURN":tick<FADE?"CATCH":"FADE";
    }
}
