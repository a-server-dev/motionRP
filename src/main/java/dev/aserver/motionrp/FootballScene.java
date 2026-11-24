package dev.aserver.motionrp;

import dev.aserver.motionrp.core.FootballTimeline;
import dev.aserver.motionrp.core.FootballFootPath;
import dev.aserver.motionrp.core.FootballVisuals;
import dev.aserver.motionrp.core.FootballBallArc;
import kr.toxicity.model.api.platform.PlatformBillboard;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;
import static dev.aserver.motionrp.core.FootballTimeline.*;

/** Cosmetic reference-based solo shots. No damage, block edits, or retired No.2 playback. */
final class FootballScene extends SummonScene {
 private final EntityTracker body;
 private final boolean fire;
 private final AerialAnchor flight;
 private Location ballPosition,previousBall,shotStart,previousBoot;
 private DummyTracker ball;
 private final List<DummyTracker> penguins=new ArrayList<>();
 private final List<Location> birdPositions=new ArrayList<>();
 private final List<FootballVisuals.Facing> birdAttachedFacings=new ArrayList<>();
 private boolean impacted;
 FootballScene(MotionPlugin plugin,Player owner,EntityTracker body,String effect){
  super(plugin,owner,"Football-"+effect,end(effect.equals("fire_tornado")),56,true);this.body=body;
  fire=effect.equals("fire_tornado");flight=new AerialAnchor(plugin,owner);
  body.rotation(()->new ModelRotation(0,origin.getYaw()));
 }
 @Override String phase(){return FootballTimeline.phase(tick,fire);}
 private Location attach(FootballFootPath.Point p,int t){return at(p.right(),p.forward()+approach(t,fire),p.height()+height(t,fire));}
 private Location boot(int t){return attach(FootballFootPath.at(fire,t).boot(),t);}
 private Location calf(int t){return attach(FootballFootPath.at(fire,t).calf(),t);}
 private Location liveBoot(){var p=body.bone("football_boot").worldPosition();return owner.getLocation().add(p.x,p.y,p.z);}
 private Location contact(){return (fire?liveBoot():boot(kick(false))).add(at(0,.25,.03).toVector().subtract(origin.toVector()));}
 private FootballFootPath.Point local(Location location){
  Vector delta=location.toVector().subtract(origin.toVector());double yaw=Math.toRadians(origin.getYaw());
  return new FootballFootPath.Point(-Math.cos(yaw)*delta.getX()-Math.sin(yaw)*delta.getZ(),-Math.sin(yaw)*delta.getX()+Math.cos(yaw)*delta.getZ(),delta.getY());
 }
 private Location blend(Location a,Location b,double p){return a.clone().add(b.toVector().subtract(a.toVector()).multiply(p));}
 /** DummyTracker.location teleports with pitch zero, even when its rotation cache
  * is unchanged. Reapply every bone's rotation after each movement packet. */
 private void placeBird(DummyTracker tracker,Location position,FootballVisuals.Facing facing){
  Location pose=position.clone();pose.setYaw(facing.yaw());pose.setPitch(facing.pitch());
  tracker.location(BukkitAdapter.adapt(pose));
  var rotation=new ModelRotation(facing.pitch(),facing.yaw());
  tracker.rotator(ModelRotator.DEFAULT);tracker.rotation(()->rotation);tracker.forceUpdate(true);
  var packets=BetterModel.nms().createBundler(tracker.bones().size());
  for(var bone:tracker.bones())bone.rotate(rotation,packets);
  for(Player viewer:audience.players())packets.send(BukkitAdapter.adapt(viewer));
 }
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  int release=kick(fire),hit=FootballTimeline.impact(fire);
  if((fire&&tick>=jump(true)&&tick<=land(true))||(!fire&&tick>=70)){
   Location seat=at(0,approach(tick,fire),height(tick,fire)),from=owner.getLocation();Vector delta=seat.toVector().subtract(from.toVector());int samples=Math.max(1,(int)Math.ceil(delta.length()/.18));
   for(int i=1;i<=samples;i++){
    Vector shift=delta.clone().multiply(i/(double)samples);Location check=from.clone().add(shift);
    if(!check.getWorld().isChunkLoaded(check.getBlockX()>>4,check.getBlockZ()>>4)||owner.wouldCollideUsing(owner.getBoundingBox().shift(shift))){plugin.playback().stop(owner.getUniqueId());return;}
   }
   if(!flight.moveTo(seat,origin)){plugin.playback().stop(owner.getUniqueId());return;}
  }
  if(tick==0){ballPosition=at(.12,fire?.9:1.55,.3);ball=model("motionrp_football",ballPosition,1,"football");ball.update(TrackerUpdateAction.moveDuration(1));ball.rotation(()->new ModelRotation(tick*17,origin.getYaw()+tick*23));}
  if(tick==charge(fire)){
   if(fire)sound(origin,Sound.ITEM_FIRECHARGE_USE,1.4f,.65f);
   else for(int i=0;i<5;i++){
    double a=i*Math.PI*2/5;Location start=at(Math.cos(a)*3,Math.sin(a)*3,.1);
    var p=model("motionrp_emperor_penguin_red",start,.55f,"red_penguin_"+i);p.update(TrackerUpdateAction.composite(TrackerUpdateAction.moveDuration(1),TrackerUpdateAction.billboard(PlatformBillboard.FIXED)));p.rotator(ModelRotator.DEFAULT);loop(p,"flap");penguins.add(p);birdPositions.add(start);birdAttachedFacings.add(new FootballVisuals.Facing(0,0));
    particle(start,Particle.SPLASH,12,.2,.05);particle(start,Particle.CLOUD,8,.2,.03);
   }
   if(!fire)sound(origin,Sound.ENTITY_ENDER_DRAGON_GROWL,.7f,1.8f);
  }
  if(tick<release||(fire&&tick==release)){
   Location target=contact();
   if(fire){
    if(tick<8)ballPosition=blend(at(.12,.9,.3),boot(8).add(0,.15,0),smooth(tick,4,8));
    else{
     Location expected=boot(release+4).add(at(0,.25,.03).toVector().subtract(origin.toVector()));
     var point=FootballBallArc.at(tick,local(boot(8).add(0,.15,0)),local(expected),local(target));
     ballPosition=at(point.right(),point.forward(),point.height());
     if(tick==FootballBallArc.APEX_TICK||tick>=release-4)trace("lob tick="+tick+" height="+(ballPosition.getY()-origin.getY())+" contactGap="+ballPosition.distance(target));
    }
   }else ballPosition=blend(at(.12,1.55,.3),target,smooth(tick,86,release));
   place(ball,ballPosition);
   // Birds climb out of the turf, sweep upward with white wakes, then bite the raised calf.
   for(int i=0;i<penguins.size();i++){
    double a=i*Math.PI*2/5;Location c=calf(tick);Vector axis=boot(tick).toVector().subtract(c.toVector()).normalize();Vector u=at(1,0,0).toVector().subtract(origin.toVector());u.subtract(axis.clone().multiply(u.dot(axis)));if(u.lengthSquared()<.001)u=new Vector(0,0,1);u.normalize();Vector v=axis.clone().crossProduct(u).normalize();Vector radial=u.multiply(Math.cos(a)).add(v.multiply(Math.sin(a)));
    Location targetBird=c.clone().add(radial.clone().multiply(.5));double p=smooth(tick,charge(false)+i*2,54+i*2);
    Location pos=blend(at(Math.cos(a)*3,Math.sin(a)*3,.1),targetBird,p).add(0,Math.sin(p*Math.PI)*3.4,0);
    var bird=penguins.get(i);Vector beak=c.toVector().subtract(pos.toVector());var facing=FootballVisuals.penguinFacing(beak.getX(),beak.getY(),beak.getZ());birdAttachedFacings.set(i,facing);placeBird(bird,pos,facing);
    if(tick==90)trace("penguin target index="+i+" from="+pos.getX()+","+pos.getY()+","+pos.getZ()+" target="+c.getX()+","+c.getY()+","+c.getZ());
    if(tick%2==0&&p<1){Location previous=birdPositions.get(i);for(int j=0;j<3;j++)particle(blend(previous,pos,j/3.0),Particle.END_ROD,1,.02,0);}
    birdPositions.set(i,pos);
   }
  }
  if(fire&&tick>=charge(true)&&tick<release+14&&tick%2==0){
   var frame=FootballFootPath.at(true,tick);
   for(int strand=0;strand<2;strand++)for(int i=0;i<FootballVisuals.FLAME_SAMPLES;i++)particle(attach(FootballVisuals.flame(frame,tick,strand,i),tick),Particle.FLAME,1,.025,0);
   particle(boot(tick),Particle.FLAME,4,.08,.005);
  }
  if(fire&&(tick==release-4||tick==release+4)){
   var right=body.bone("football_support_boot").worldPosition();Location left=liveBoot();Vector aim=at(0,1,0).toVector().subtract(origin.toVector());
   double lf=left.toVector().subtract(owner.getLocation().toVector()).dot(aim),rf=right.x*aim.getX()+right.z*aim.getZ();
   trace("leg exchange tick="+tick+" left="+lf+" right="+rf);
  }
  if(fire&&tick>=release-4&&tick<=release){
   Location foot=liveBoot();Vector aim=at(0,1,0).toVector().subtract(origin.toVector());
   if(previousBoot!=null)trace("live foot tick="+tick+" forward="+foot.toVector().subtract(previousBoot.toVector()).dot(aim)+" xyz="+foot.getX()+","+foot.getY()+","+foot.getZ());previousBoot=foot;
  }
  if(tick==release){
   if(fire){
    var torso=body.bone("football_torso").worldPosition();var upper=body.bone("football_upper").worldPosition();var axis=upper.sub(torso,new org.joml.Vector3f()).normalize();
    var side=body.bone("football_side").worldPosition().sub(torso,new org.joml.Vector3f()).normalize();trace("kick bank side up="+side.y);
    Vector aim=at(0,1,0).toVector().subtract(origin.toVector());trace("kick body axis forward="+(axis.x*aim.getX()+axis.z*aim.getZ())+" up="+axis.y);
   }
   ballPosition=contact();shotStart=ballPosition.clone();previousBall=shotStart.clone();place(ball,ballPosition);trace("kick contact at "+(fire?"live":"baked")+" boot; ball released");
   for(Player viewer:audience.players())viewer.spawnParticle(Particle.FLASH,ballPosition,1,0,0,0,0,Color.fromRGB(255,fire?190:130,fire?65:45));
   particle(ballPosition,Particle.SWEEP_ATTACK,3,.15,0);sound(ballPosition,Sound.ENTITY_PLAYER_ATTACK_STRONG,1.8f,.65f);sound(ballPosition,Sound.ENTITY_BREEZE_WIND_BURST,1.5f,.8f);
  }
  if(tick>=release&&!impacted){
   double p=progress(tick,release,hit);Vector forward=at(0,distance(tick,fire),0).toVector().subtract(origin.toVector());
   Location next=shotStart.clone().add(forward).add(0,(1.2-(shotStart.getY()-origin.getY()))*p,0);
   Vector direction=next.toVector().subtract(previousBall.toVector());double length=direction.length();
   if(!next.getWorld().isChunkLoaded(next.getBlockX()>>4,next.getBlockZ()>>4))impact(previousBall);
   else{
    var obstacle=length<.001?null:next.getWorld().rayTraceBlocks(previousBall,direction.clone().normalize(),length,FluidCollisionMode.NEVER,true);
    if(obstacle!=null)impact(obstacle.getHitPosition().toLocation(next.getWorld()));
    else{
     ballPosition=next;place(ball,next);
     for(int i=0;i<penguins.size();i++){
      double a=i*Math.PI*2/5+tick*.4;Vector right=at(Math.cos(a)*.48,0,Math.sin(a)*.48).toVector().subtract(origin.toVector());
      Location target=next.clone().add(right);Location pos=blend(birdPositions.get(i),target,smooth(tick,release,release+7));Vector travel=direction.lengthSquared()>.000001?direction:at(0,1,0).toVector().subtract(origin.toVector());
      var flightFacing=FootballVisuals.penguinFacing(travel.getX(),travel.getY(),travel.getZ());
      var facing=FootballVisuals.launchFacing(birdAttachedFacings.get(i),flightFacing,smooth(tick,release,release+7));placeBird(penguins.get(i),pos,facing);
      if(tick==release)trace("penguin release index="+i+" yawDelta="+(facing.yaw()-birdAttachedFacings.get(i).yaw())+" pitchDelta="+(facing.pitch()-birdAttachedFacings.get(i).pitch()));
      if(tick%2==0)particle(pos,Particle.END_ROD,2,.04,0);
     }
     for(int j=0;j<3;j++){Location at=blend(previousBall,next,j/3.0);particle(at,fire?Particle.FLAME:Particle.END_ROD,fire?10:4,.14,.01);if(!fire)dust(at,new Particle.DustOptions(Color.fromRGB(245,55,30),.9f),3,.12);}
     previousBall=next;if(tick>=hit)impact(next);
    }
   }
  }
 }
 private void place(DummyTracker tracker,Location location){tracker.location(BukkitAdapter.adapt(location));}
 private void impact(Location location){
  if(impacted)return;impacted=true;audience.hide(ball);ball=null;for(var p:penguins)audience.hide(p);penguins.clear();
  particle(location,Particle.EXPLOSION_EMITTER,1,0,0);particle(location,Particle.GUST,12,1,.04);particle(location,Particle.CLOUD,50,1.1,.1);particle(location,Particle.FLAME,60,1,.08);sound(location,Sound.ENTITY_GENERIC_EXPLODE,2,.65f);trace("cosmetic shot impact tick="+tick);
 }
 @Override public boolean ownsMove(Location from,Location to){return flight.owns(from,to);}
 @Override public void externalTeleport(){flight.finishAtCurrent();}
 @Override void onClose(){flight.close();}
}
