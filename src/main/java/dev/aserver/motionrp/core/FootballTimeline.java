package dev.aserver.motionrp.core;
/** Shared ball/contact clock; both Emperor Penguin No.2 roles hit on the same tick. */
public final class FootballTimeline {
 public static final int CHARGE=24,JUMP=52,KICK=96,IMPACT=128,LAND=138,END=164;
 public static final double RANGE=42,PAIR_SPACING=1.6;
 private FootballTimeline(){}
 public static double progress(int tick,int from,int to){return Math.clamp((tick-from)/(double)(to-from),0,1);}
 public static double height(int tick,boolean fire){
  if(tick<JUMP||tick>LAND)return 0;
  return (fire?3.2:2.0)*Math.sin(Math.PI*progress(tick,JUMP,LAND));
 }
 public static double distance(int tick){return RANGE*progress(tick,KICK,IMPACT);}
 public static String phase(int tick){return tick<CHARGE?"SETUP":tick<JUMP?"CHARGE":tick<KICK?"WINDUP":tick<IMPACT?"SHOT":tick<LAND?"LAND":"RECOVER";}
}
