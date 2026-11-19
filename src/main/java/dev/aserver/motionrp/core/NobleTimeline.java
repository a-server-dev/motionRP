package dev.aserver.motionrp.core;

public final class NobleTimeline {
    public static final int LIGHT_BLADE=64,CUT_START=90;
    private NobleTimeline() {}
    public static int draw(boolean excalibur) {return excalibur?12:8;}
    public static int release(boolean excalibur) {return excalibur?93:GaeBolgTimeline.THROW;}
    public static int impact(boolean excalibur) {return excalibur?104:GaeBolgTimeline.IMPACT;}
    public static int fade(boolean excalibur) {return excalibur?148:GaeBolgTimeline.FADE;}
    public static int end(boolean excalibur) {return excalibur?176:GaeBolgTimeline.END;}
    public static double reach(boolean excalibur) {return excalibur?30:18;}
    public static int segments(boolean excalibur) {return excalibur?14:6;}
    public static String phase(int tick,boolean excalibur) {
        return tick<0 || tick>=end(excalibur)?"FINISHED":tick<draw(excalibur)?"POSE":tick<32?"DRAW":tick<release(excalibur)?"CHARGE"
            :tick<impact(excalibur)?"RELEASE":tick<fade(excalibur)?"IMPACT":"FADE";
    }
}
