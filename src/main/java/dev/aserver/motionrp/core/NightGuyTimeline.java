package dev.aserver.motionrp.core;
/** Sprint fifty blocks in twenty ticks, launch the kick, then land at the endpoint. */
public final class NightGuyTimeline {
 public static final int STEAM=32,DRAGON=76,RUN=96,LEAP=116,DASH=132,IMPACT=154,RECOVER=178,LANDED=214,END=226;
 public static final double RUN_DISTANCE=50;
 private NightGuyTimeline(){}
 public static double ease(double p){p=Math.clamp(p,0,1);return p*p*(3-2*p);}
 public static double sprint(int tick,double reach){return reach*Math.clamp((tick-RUN)/(double)(LEAP-RUN),0,1);}
 public static double forward(int tick,double runReach,double kickReach){return sprint(tick,runReach)+kickReach*ease((tick-DASH)/(double)(IMPACT-DASH));}
 public static double height(int tick){if(tick<LEAP)return 0;if(tick<DASH)return 4*ease((tick-LEAP)/(double)(DASH-LEAP));if(tick<RECOVER)return 4+.6*Math.sin(Math.PI*Math.clamp((tick-DASH)/(double)(RECOVER-DASH),0,1));return 4*(1-ease((tick-RECOVER)/(double)(LANDED-RECOVER)));}
 public static String phase(int tick){return tick>=END?"FINISHED":tick<STEAM?"STANCE":tick<DRAGON?"BLOOD_STEAM":tick<RUN?"DRAGON":tick<LEAP?"SPRINT":tick<DASH?"LEAP":tick<IMPACT?"DASH":tick<RECOVER?"IMPACT":tick<LANDED?"LANDING":"FADE";}
}
