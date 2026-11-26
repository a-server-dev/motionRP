package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.aserver.motionrp.core.FootballFootPath.Point;

class FootballBallArcTest {
 private final Point launch=new Point(.1,.3,.6),expected=new Point(0,.7,9);
 @Test void ballWaitsAtTheScoopPointUntilTheFootFlick(){
  for(int t=0;t<=FootballBallArc.TOSS;t++)assertEquals(launch,FootballBallArc.at(t,launch,expected,expected));
 }
 @Test void ballRisesAboveThePlayerAndFallsWithoutArrivingEarly(){
  double previous=launch.height();
  for(int t=8;t<=32;t++){var p=FootballBallArc.at(t,launch,expected,expected);assertTrue(p.height()>=previous);previous=p.height();}
  assertEquals(15,previous);assertEquals(8,FootballTimeline.FIRE_HEIGHT);
  for(int t=33;t<70;t++){var p=FootballBallArc.at(t,launch,expected,expected);assertTrue(p.height()<previous);assertTrue(p.height()>expected.height());previous=p.height();}
 }
 @Test void liveFootDoesNotPullTheBallOutOfItsLobDuringTheSpin(){
  Point movingFoot=new Point(2,-3,4);
  for(int t=8;t<=64;t++)assertEquals(FootballBallArc.at(t,launch,expected,expected),FootballBallArc.at(t,launch,expected,movingFoot));
 }
 @Test void rendererCorrectionIsCompleteAtContactWithoutASnap(){
  Point live=new Point(.35,1,9.1);
  assertEquals(live.right(),FootballBallArc.at(70,launch,expected,live).right(),1e-12);
  assertEquals(live.forward(),FootballBallArc.at(70,launch,expected,live).forward(),1e-12);
  assertEquals(live.height(),FootballBallArc.at(70,launch,expected,live).height(),1e-12);
  assertTrue(FootballBallArc.at(69,launch,expected,live).height()-live.height()>.25);
 }
}
