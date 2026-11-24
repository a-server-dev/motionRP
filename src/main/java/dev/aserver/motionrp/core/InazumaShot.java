package dev.aserver.motionrp.core;

/** Reference clip ranges exclude menus, celebration and the repeated second spear shot. */
public enum InazumaShot {
 DEATH_SWORD("death_sword",.8,6.166667,110,123,0),
 ETERNAL_BLIZZARD("eternal_blizzard",0,6.6,112,146,2.4),
 DEATH_SPEAR("death_spear",0,5.5,72,120,1.8);
 public final String id;public final double sourceStart,sourceEnd,jumpHeight;public final int kick,impact,end;
 InazumaShot(String id,double start,double finish,int kick,int impact,double height){this.id=id;sourceStart=start;sourceEnd=finish;this.kick=kick;this.impact=impact;end=(int)Math.round((finish-start)*20*1.2);jumpHeight=height;}
 public static InazumaShot from(String id){for(var shot:values())if(shot.id.equals(id))return shot;throw new IllegalArgumentException(id);}
 public int strike(){return this==DEATH_SWORD?78:kick;}
 public int handHoldStart(){return 88;}
 public int handHoldEnd(){return 94;}
 public int auraStart(){return this==DEATH_SWORD?80:this==ETERNAL_BLIZZARD?38:20;}
 public double height(int tick){
  if(this==DEATH_SWORD)return 0;
  if(this==ETERNAL_BLIZZARD)return .85*FootballTimeline.smooth(tick,22,26)*(1-FootballTimeline.smooth(tick,34,42))+jumpHeight*FootballTimeline.smooth(tick,62,84)*(1-FootballTimeline.smooth(tick,kick+3,end-7));
  return jumpHeight*FootballTimeline.smooth(tick,12,38)*(1-FootballTimeline.smooth(tick,kick+3,end-7));
 }
 public String phase(int tick){
  if(this==DEATH_SWORD)return tick<20?"SETUP":tick<62?"CHARGE":tick<strike()?"WINDUP":tick<88?"DARKEN":tick<kick?"HANDCUT":tick<impact?"SHOT":"RECOVER";
  if(this==ETERNAL_BLIZZARD&&tick>=22&&tick<=62)return tick<38?"FOOT_TWIST":"LAND_RESET";
  return tick<20?"SETUP":tick<kick-12?"CHARGE":tick<kick?"WINDUP":tick<impact?"SHOT":"RECOVER";
 }
}
