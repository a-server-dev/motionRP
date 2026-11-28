package dev.aserver.motionrp.core;
/** Sprint fifty blocks in twenty ticks, launch the kick, then land at the endpoint. */
public final class NightGuyTimeline {
 public static final int STEAM=32,DRAGON=76,RUN=96,LEAP=116,DASH=122,IMPACT=138,RECOVER=144,LANDED=148,END=160;
 public static final double RUN_DISTANCE=50;
 private NightGuyTimeline(){}
 public static double ease(double p){p=Math.clamp(p,0,1);return p*p*(3-2*p);}
 public static double sprint(int tick,double reach){return reach*Math.clamp((tick-RUN)/(double)(LEAP-RUN),0,1);}
 public static double forward(int tick,double runReach,double kickReach){return sprint(tick,runReach)+kickReach*flightProgress((tick-LEAP)/(double)(IMPACT-LEAP));}
 public static double flightProgress(double p){p=Math.clamp(p,0,1);return p*(2-p);}
 public static double height(int tick){double p=Math.clamp((tick-LEAP)/(double)(LANDED-LEAP),0,1);return 12.8*p*(1-p);}

 public static String phase(int tick){return tick>=END?"FINISHED":tick<STEAM?"STANCE":tick<DRAGON?"BLOOD_STEAM":tick<RUN?"DRAGON":tick<LEAP?"SPRINT":tick<DASH?"LEAP":tick<IMPACT?"DASH":tick<RECOVER?"IMPACT":tick<LANDED?"LANDING":"FADE";}
}
