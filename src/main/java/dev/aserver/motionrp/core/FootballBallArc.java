package dev.aserver.motionrp.core;

import static dev.aserver.motionrp.core.FootballFootPath.Point;

/** A high lob that reaches the animated boot only on the shot's contact tick. */
public final class FootballBallArc {
 public static final int TOSS=8,APEX_TICK=32;
 public static final double APEX_HEIGHT=15;
 private FootballBallArc(){}
 private static Point blend(Point a,Point b,double p){return new Point(a.right()+(b.right()-a.right())*p,a.forward()+(b.forward()-a.forward())*p,a.height()+(b.height()-a.height())*p);}
 public static Point at(int tick,Point launch,Point expectedContact,Point liveContact){
  Point peak=new Point(expectedContact.right(),expectedContact.forward(),APEX_HEIGHT);
  if(tick<=APEX_TICK){double p=FootballTimeline.progress(tick,TOSS,APEX_TICK);return blend(launch,peak,1-(1-p)*(1-p));}
  int contact=FootballTimeline.kick(true);
  double p=FootballTimeline.progress(tick,APEX_TICK,contact);
  Point falling=blend(peak,expectedContact,p*p);
  // Correct the small renderer timing difference only at the end of the fall;
  // the ball must not follow the boot throughout the wind-up.
  double correction=FootballTimeline.smooth(tick,contact-4,contact);
  return new Point(falling.right()+(liveContact.right()-expectedContact.right())*correction,
   falling.forward()+(liveContact.forward()-expectedContact.forward())*correction,
   falling.height()+(liveContact.height()-expectedContact.height())*correction);
 }
}
