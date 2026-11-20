package dev.aserver.motionrp.core;
/** Twenty ticks/second; the portal return is exactly eight seconds after falling begins. */
public final class InfinityCastleTimeline {
 public static final int FALL=20,RETURN=FALL+160,LAND=198,END=214;
 public static final double DROP=72,EXIT_HEIGHT=3;
 private InfinityCastleTimeline(){}
 public static double depth(int tick){return DROP*Math.pow(Math.clamp((tick-FALL)/(double)(RETURN-FALL),0,1),1.12);}
 public static double exitHeight(int tick){return EXIT_HEIGHT*(1-Math.clamp((tick-RETURN)/(double)(LAND-RETURN),0,1));}
 public static String phase(int tick){return tick<FALL?"FUSUMA":tick<RETURN?"FALL":tick<LAND?"RETURN":tick<END?"CLOSE":"FINISHED";}
}
