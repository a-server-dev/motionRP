package dev.aserver.motionrp.core;
/** Shared chase clock and bounded, swept rock-slam flight. */
public final class BrolyTimeline {
 public static final int TURN=28,CHARGE=40,GRAB=50,LAUNCH=56,END=144;
 public static final double ESCAPE_DISTANCE=4,MAX_DISTANCE=24,SPEED=1.5,BODY_MARGIN=.38;
 private BrolyTimeline(){}
 public static double fleeDistance(int tick){return ESCAPE_DISTANCE*Math.clamp(tick/(double)TURN,0,1);}
 public static double turnProgress(int tick){double p=Math.clamp((tick-TURN-3)/(double)(CHARGE-TURN-3),0,1);return p*p*(3-2*p);}
 public static double chargeProgress(int tick){double p=Math.clamp((tick-CHARGE)/(double)(GRAB-CHARGE),0,1);return p*p;}
 public static double nextDistance(double distance){return Math.min(MAX_DISTANCE,distance+SPEED);}
 public static double safeTravel(double step,double hitDistance){return Math.clamp(hitDistance-.08,0,step);}
 public static String phase(int tick,int impact){return tick<TURN?"FLEE":tick<CHARGE?"TURN":tick<GRAB?"CHARGE":tick<LAUNCH?"GRAB":impact<0?"FLIGHT":tick<impact+20?"IMPACT":"RECOVER";}
}
