package dev.aserver.motionrp.core;
/** Bounded swept flight: every segment is tested, not only the destination. */
public final class BrolyTimeline {
 public static final int LAUNCH=32,END=120;
 public static final double MAX_DISTANCE=24,SPEED=1.5,BODY_MARGIN=.38;
 private BrolyTimeline(){}
 public static double nextDistance(double distance){return Math.min(MAX_DISTANCE,distance+SPEED);}
 public static double safeTravel(double step,double hitDistance){return Math.clamp(hitDistance-.08,0,step);}
 public static String phase(int tick,int impact){return tick<12?"APPROACH":tick<LAUNCH?"GRAB":impact<0?"FLIGHT":tick<impact+20?"IMPACT":"RECOVER";}
}
