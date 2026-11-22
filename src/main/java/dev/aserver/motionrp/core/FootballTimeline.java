package dev.aserver.motionrp.core;
/** Reference-derived solo clocks; the legacy No.2 assets remain archived, not executable. */
public final class FootballTimeline {
 public static final int CHARGE=24,JUMP=52,KICK=96,IMPACT=128,LAND=138,END=164; // archived No.2 clock
 public static final double RANGE=42,PAIR_SPACING=1.6;
 private FootballTimeline(){}
 public static double progress(int tick,int from,int to){return Math.clamp((tick-from)/(double)(to-from),0,1);}
 public static double smooth(int tick,int from,int to){double p=progress(tick,from,to);return p*p*(3-2*p);}
 public static int charge(boolean fire){return fire?36:20;}
 public static int jump(boolean fire){return fire?46:70;}
 public static int kick(boolean fire){return fire?112:98;}
 public static int impact(boolean fire){return fire?144:124;}
 public static int land(boolean fire){return fire?156:156;}
 public static int end(boolean fire){return fire?178:174;}
 public static double height(int tick,boolean fire){
  if(!fire)return 0; // No.1 plants the supporting leg; it is not a bicycle kick.
  return 3.2*smooth(tick,46,82)*(1-smooth(tick,112,156));
 }
 public static double approach(int tick,boolean fire){return fire?0:.65*smooth(tick,70,98)*(1-smooth(tick,146,174));}
 public static double distance(int tick){return RANGE*progress(tick,KICK,IMPACT);}
 public static double distance(int tick,boolean fire){return RANGE*progress(tick,kick(fire),impact(fire));}
 public static String phase(int tick,boolean fire){
  return tick<charge(fire)?"SETUP":tick<(fire?46:54)?"CHARGE":tick<kick(fire)?"WINDUP":tick<impact(fire)?"SHOT":tick<land(fire)?"LAND":"RECOVER";
 }
}
