package dev.aserver.motionrp.core;

/** Anime reference ticks, with its intro removed and all pre-turn action accelerated by 1.5. */
public final class BlizzardTimeline {
 private BlizzardTimeline(){}
 public static int at(int reference){return (int)Math.round(reference<=136?Math.max(0,reference-32)/1.5:104/1.5+reference-136);}
 public static final int TURN=at(136),KICK=at(240),IMPACT=at(270),END=at(278);
 public static double smooth(double t,int from,int to){double p=Math.clamp((t-from)/(to-from),0,1);return p*p*(3-2*p);}
 public static double reference(double tick){return tick<=TURN?32+104*tick/(double)TURN:136+tick-TURN;}
}
