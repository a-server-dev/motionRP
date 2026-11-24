package dev.aserver.motionrp.core;

/** Piecewise shot timing: Sword doubles its approach; Blizzard removes its intro and accelerates pre-turn action. */
public enum InazumaShot {
 DEATH_SWORD("death_sword",.8,6.166667,SwordShotTimeline.SHOT,SwordShotTimeline.IMPACT,0),
 ETERNAL_BLIZZARD("eternal_blizzard",1.6,13.917,BlizzardTimeline.KICK,BlizzardTimeline.IMPACT,3.0,1.0),
 DEATH_SPEAR("death_spear",0,5.5,ScissorShotTimeline.at(false,173),ScissorShotTimeline.at(false,203),1.8);
 public final String id;public final double sourceStart,sourceEnd,jumpHeight,durationScale;public final int kick,impact,end;
 InazumaShot(String id,double start,double finish,int kick,int impact,double height){this(id,start,finish,kick,impact,height,1.2);}
 InazumaShot(String id,double start,double finish,int kick,int impact,double height,double scale){this.id=id;sourceStart=start;sourceEnd=finish;this.kick=kick;this.impact=impact;durationScale=scale;end=id.equals("death_sword")?SwordShotTimeline.END:id.equals("eternal_blizzard")?BlizzardTimeline.END:(int)Math.round((finish-start)*20*scale);jumpHeight=height;}
 public static InazumaShot from(String id){for(var shot:values())if(shot.id.equals(id))return shot;throw new IllegalArgumentException(id);}
 public int strike(){return this==DEATH_SWORD?SwordShotTimeline.STRIKE:kick;}
 public int handHoldStart(){return SwordShotTimeline.at(88);}
 public int handHoldEnd(){return SwordShotTimeline.at(94);}
 public int auraStart(){return this==DEATH_SWORD?SwordShotTimeline.at(80):this==ETERNAL_BLIZZARD?32:20;}
 public double height(int tick){
  if(this==DEATH_SWORD)return 0;
  return ScissorShotTimeline.height(this==ETERNAL_BLIZZARD,tick,jumpHeight);
 }
 public String phase(int tick){
  if(this==DEATH_SWORD)return tick<SwordShotTimeline.at(20)?"SETUP":tick<SwordShotTimeline.at(62)?"CHARGE":tick<strike()?"WINDUP":tick<SwordShotTimeline.at(88)?"DARKEN":tick<kick?"HANDCUT":tick<impact?"SHOT":"RECOVER";
  return ScissorShotTimeline.phase(this==ETERNAL_BLIZZARD,tick);
 }
}
