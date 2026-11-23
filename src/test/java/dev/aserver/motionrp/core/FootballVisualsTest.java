package dev.aserver.motionrp.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static dev.aserver.motionrp.core.FootballFootPath.*;
class FootballVisualsTest {
 @Test void bootMovesTowardsTheShotAtContactInsteadOfSwingingBackwards(){
  int contact=FootballTimeline.kick(true);
  for(int t=contact;t<=contact+4;t++)assertTrue(FootballFootPath.at(true,t).boot().forward()>FootballFootPath.at(true,t-1).boot().forward(),"Foot must travel forwards through contact tick "+t);
  assertEquals(8,FootballTimeline.height(50,true));assertEquals(8,FootballTimeline.height(contact,true));
  assertEquals(0,FootballTimeline.height(FootballTimeline.land(true),true),1e-9);
 }
 @Test void headPointsTowardsTheShotAndSlightlyDownAtContact(){
  for(int tick=68;tick<=72;tick++){
   var frame=FootballFootPath.at(true,tick);var axis=unit(sub(frame.upper(),frame.torso()));
   assertTrue(axis.forward()>.95,"Head must point into the shot, not behind it");
   assertTrue(axis.height()<-.08&&axis.height()>-.3,"Torso must be nearly horizontal with the head slightly lower");
  }
 }
 @Test void old047FireIsRecognizedForAutomaticUpgrade()throws Exception{
  String name="players/motionrp_fire_tornado.bbmodel";try(var in=new GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.47.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void horizontalKickIsBankedLeftByAQuarterTurn(){
  for(int tick=68;tick<=72;tick++){
   var frame=FootballFootPath.at(true,tick);var side=unit(sub(frame.side(),frame.torso()));
   assertEquals(0,side.right(),1e-6);assertTrue(side.height()<-.98,"Side axis must turn left/down by 90 degrees");
  }
 }
 @Test void old048FireIsRecognizedForAutomaticUpgrade()throws Exception{
  String name="players/motionrp_fire_tornado.bbmodel";try(var in=new GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.48.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void flameRibbonsStayAroundTheAnimatedTorsoInEveryBodyOrientation(){
  for(int t=20;t<82;t+=3){var f=FootballFootPath.at(true,t);Point center=f.torso();Point axis=unit(sub(f.upper(),center));
   for(int strand=0;strand<2;strand++)for(int i=0;i<FootballVisuals.FLAME_SAMPLES;i++){
    Point p=FootballVisuals.flame(f,t,strand,i),delta=sub(p,center);double along=dot(delta,axis),radius=Math.sqrt(dot(delta,delta)-along*along);
    assertTrue(Math.abs(along)<=.600001);assertTrue(radius>=.49999&&radius<=.64001);
    if(t>=46&&t<=68)assertTrue(p.height()+FootballTimeline.height(t,true)>7,"No standing flame column below the airborne actor");
   }
  }
 }
 @Test void nativePenguinBeakFacesTheCalfInAllDirectionsIncludingPitch(){
  for(double[] d:new double[][]{{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{1,2,3},{-1,-2,-3},{0,1,0},{0,-1,0}}){
   var facing=FootballVisuals.penguinFacing(d[0],d[1],d[2]);double pitch=Math.toRadians(facing.pitch()),yaw=Math.toRadians(facing.yaw()),n=Math.sqrt(d[0]*d[0]+d[1]*d[1]+d[2]*d[2]);
   // The authored +Z beak becomes -Z in BetterModel's mesh conversion.
   double[] beak={Math.sin(yaw)*Math.cos(pitch),Math.sin(pitch),-Math.cos(yaw)*Math.cos(pitch)};
   for(int i=0;i<3;i++)assertEquals(d[i]/n,beak[i],1e-6);
  }
 }
 @Test void old046FireIsRecognizedForAutomaticUpgrade()throws Exception{
  String name="players/motionrp_fire_tornado.bbmodel";try(var in=new GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.46.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 @Test void old044BodyModelsAreRecognizedForAutomaticUpgrade()throws Exception{
  for(String name:List.of("players/motionrp_fire_tornado.bbmodel","players/motionrp_emperor_penguin_1.bbmodel"))try(var in=new GZIPInputStream(Objects.requireNonNull(getClass().getResourceAsStream("/bundled-0.44.0/"+name.replace('/','_')+".gz")))){assertTrue(BundledAssetVersions.knownHashes(name,Set.of()).contains(BundledAssetUpdater.sha256(in.readAllBytes())));}
 }
 private static Point sub(Point a,Point b){return new Point(a.right()-b.right(),a.forward()-b.forward(),a.height()-b.height());}
 private static double dot(Point a,Point b){return a.right()*b.right()+a.forward()*b.forward()+a.height()*b.height();}
 private static Point unit(Point p){double n=Math.sqrt(dot(p,p));return new Point(p.right()/n,p.forward()/n,p.height()/n);}
}
