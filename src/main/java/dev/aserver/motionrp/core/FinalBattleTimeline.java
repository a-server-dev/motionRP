package dev.aserver.motionrp.core;
/** Shared clock for both roles, their avatars and the three colliding projectiles. */
public final class FinalBattleTimeline {
 public static final int END=880,FORM=40,AVATARS=140,ASCEND=240,ASURA=320,CHARGE=400,RELEASE=560,COLLISION=600,AFTERMATH=720,RETURN=800;
 private FinalBattleTimeline(){}
 public static String phase(int tick){return tick<40?"READY":tick<140?"AVATARS":tick<240?"EXCHANGE":tick<320?"ASCEND":tick<400?"TRANSFORM":tick<560?"CHARGE":tick<600?"RELEASE":tick<720?"COLLISION":tick<800?"AFTERMATH":tick<END?"RETURN":"FINISHED";}
 public static double form(int tick){return Math.clamp((tick-40)/100.0,0,1);}
 public static double lift(int tick){return 20*Math.clamp((tick-240)/80.0,0,1);}
 public static double approach(int tick){return tick<140?0:tick<180?(tick-140)/40.0:tick<200?1:tick<240?(240-tick)/40.0:0;}
 public static double distance(int tick){return 58-20*approach(tick);}
 public static double projectile(int tick){return Math.clamp((tick-RELEASE)/40.0,0,1);}
 public static double groundReturn(int tick){return Math.clamp((END-tick)/80.0,0,1);}
}
