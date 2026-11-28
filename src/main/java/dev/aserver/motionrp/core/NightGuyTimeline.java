package dev.aserver.motionrp.core;
/** One shared clock for the player's kick and the red dragon aura. */
public final class NightGuyTimeline {
 public static final int STEAM=32,DRAGON=76,LEAP=96,DASH=112,IMPACT=134,RECOVER=158,RETURN=194,LANDED=224,END=236;
 private NightGuyTimeline(){}
 public static double ease(double p){p=Math.clamp(p,0,1);return p*p*(3-2*p);}
 public static double forward(int tick,double reach){return reach*ease((tick-DASH)/(double)(IMPACT-DASH));}
 public static double height(int tick){if(tick<LEAP)return 0;if(tick<DASH)return 4*ease((tick-LEAP)/(double)(DASH-LEAP));if(tick<RECOVER)return 4+.6*Math.sin(Math.PI*Math.clamp((tick-DASH)/(double)(RECOVER-DASH),0,1));return 4*(1-ease((tick-RECOVER)/(double)(RETURN-RECOVER)));}
 public static String phase(int tick){return tick>=END?"FINISHED":tick<STEAM?"STANCE":tick<DRAGON?"BLOOD_STEAM":tick<LEAP?"DRAGON":tick<DASH?"LEAP":tick<IMPACT?"DASH":tick<RECOVER?"IMPACT":tick<RETURN?"RECOVER":tick<LANDED?"RETURN":"FADE";}
}
