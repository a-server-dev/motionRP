package dev.aserver.motionrp.core;

/** One rapid pass in thirty ticks, one full volley, then optional Ea. */
public final class TreasuryTimeline {
    public static final int OPEN=6,DRAW=34,RAPID=42,RAPID_TICKS=30,VOLLEY=84,FLIGHT=2;
    public static final int KEY=48,EA=70,CHARGE=104,FIRE=160,IMPACT=176,RECOVER=204,SERIOUS_END=232;
    public record Portal(double right,double forward,double height,double scale,boolean surroundsTarget) {}
    private TreasuryTimeline() {}
    public static int count(boolean serious) { return serious?200:100; }
    public static int end(boolean serious) { return serious?SERIOUS_END:116; }
    private static void check(int index,boolean serious) { if(index<0 || index>=count(serious)) throw new IllegalArgumentException("Invalid treasury gate"); }
    public static int openAt(int index,boolean serious) { check(index,serious);return OPEN+index*20/count(serious); }
    public static int rapidAt(int index,boolean serious) { check(index,serious);return RAPID+index*RAPID_TICKS/count(serious); }
    public static Portal portal(int index,boolean serious) {
        check(index,serious);int i=index%100;double angle=i*Math.PI*(3-Math.sqrt(5));
        if(index<100) {
            double radius=Math.sqrt((i+.5)/100.0);
            return new Portal(12*radius*Math.cos(angle),-2.5-3*((i*.61803398875)%1),6.6+5.8*radius*Math.sin(angle),.78+.2*((i*.4142)%1),false);
        }
        return new Portal(6*Math.cos(angle),6*Math.sin(angle),.8+7*((i*.41421356237)%1),.72+.2*((i*.732)%1),true);
    }
    public static String phase(int tick,boolean serious) {
        if(tick<0 || tick>=end(serious)) return "FINISHED";
        if(tick<OPEN) return "POSE";if(tick<DRAW) return "OPEN";if(tick<RAPID) return "DRAW";
        if(!serious) return tick<VOLLEY?"RAPID":tick<102?"VOLLEY":"FADE";
        return tick<KEY?"RAPID":tick<EA?"KEY":tick<VOLLEY?"EA_DRAW":tick<CHARGE?"VOLLEY"
                :tick<FIRE?"EA_CHARGE":tick<IMPACT?"ENUMA_ELISH":tick<RECOVER?"IMPACT":"FADE";
    }
}
