package dev.aserver.motionrp;

import dev.aserver.motionrp.core.FootballTimeline;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;
import static dev.aserver.motionrp.core.FootballTimeline.*;

/** Cosmetic football shots: a single shared ball for No.2; no damage or block edits. */
final class FootballScene extends SummonScene {
 private final EntityTracker body;
 private final boolean fire,pair,lead;
 private final AerialAnchor flight;
 private Location center,ballPosition,previousBall;
 private DummyTracker ball,spiral;
 private final List<DummyTracker> penguins=new ArrayList<>();
 private boolean impacted;private double shotHeight;
 FootballScene(MotionPlugin plugin,Player owner,EntityTracker body,String effect){
  super(plugin,owner,"Football-"+effect,END,56,true);this.body=body;
  fire=effect.equals("fire_tornado");pair=effect.startsWith("emperor_penguin_2_");lead=!effect.endsWith("_right");
  flight=pair?null:new AerialAnchor(plugin,owner);
  body.rotation(()->new ModelRotation(0,origin.getYaw()));
 }
 @Override String phase(){return FootballTimeline.phase(tick);}
 private Location point(double right,double forward,double up){
  double yaw=Math.toRadians(center.getYaw());return center.clone().add(-Math.cos(yaw)*right-Math.sin(yaw)*forward,up,-Math.sin(yaw)*right+Math.cos(yaw)*forward);
 }
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  if(center==null){center=pair?plugin.fusion().center(owner.getUniqueId()):origin.clone();if(center==null){plugin.playback().stop(owner.getUniqueId());return;}}
  // Both roles use the coordinator's return journals. Solo jumps own a separate journal.
  double height=height(tick,fire);Location seat=origin.clone().add(0,height,0);
  if(tick>=JUMP&&tick<=LAND){
   Location from=owner.getLocation();Vector delta=seat.toVector().subtract(from.toVector());
   int samples=Math.max(1,(int)Math.ceil(delta.length()/.18));
   for(int i=1;i<=samples;i++){
    Vector shift=delta.clone().multiply(i/(double)samples);Location check=from.clone().add(shift);
    if(!check.getWorld().isChunkLoaded(check.getBlockX()>>4,check.getBlockZ()>>4)||owner.wouldCollideUsing(owner.getBoundingBox().shift(shift))){plugin.playback().stop(owner.getUniqueId());return;}
   }
   if(!(pair?plugin.fusion().movePenguin(owner.getUniqueId(),seat):flight.moveTo(seat,origin))){plugin.playback().stop(owner.getUniqueId());return;}
  }
  if(!lead)return;
  if(tick==0){ballPosition=point(0,1,.5);ball=model("motionrp_football",ballPosition,1,"football");ball.update(TrackerUpdateAction.moveDuration(1));ball.rotation(()->new ModelRotation(tick*17,center.getYaw()+tick*23));}
  if(tick==CHARGE){
   if(fire){spiral=model("motionrp_fire_tornado",point(0,0,.1),.8f,"fire_spiral");loop(spiral,"spin");sound(center,Sound.ITEM_FIRECHARGE_USE,1.4f,.65f);}
   else {int count=pair?8:6;for(int i=0;i<count;i++){var p=model("motionrp_emperor_penguin",point(0,1,.25),.65f,"penguin_"+i);p.update(TrackerUpdateAction.moveDuration(1));loop(p,"flap");penguins.add(p);}particle(point(0,1,.1),Particle.CLOUD,32,1,.08);sound(center,Sound.ENTITY_ENDER_DRAGON_GROWL,.8f,1.6f);}
  }
  if(!impacted&&tick<KICK){
   double lift=tick<JUMP?progress(tick,CHARGE,JUMP)*.5:height+.55;
   if(fire)lift=tick<CHARGE?.5:tick<JUMP?.5+progress(tick,CHARGE,JUMP)*1.2:height+.65;
   ballPosition=point(0,1,Math.max(.5,lift));place(ball,ballPosition);
   if(fire&&tick>=CHARGE){
    if(tick%2==0){double a=tick*.6;particle(point(Math.cos(a)*1.1,Math.sin(a)*1.1,height+.35),Particle.FLAME,8,.15,.04);particle(owner.getLocation().add(0,.2,0),Particle.SMOKE,3,.2,.01);}
   }
   for(int i=0;i<penguins.size();i++){
    double q=tick*.23+i*Math.PI*2/penguins.size(),r=1.65-.8*progress(tick,70,KICK);
    var p=penguins.get(i);Location pos=point(Math.cos(q)*r,1+Math.sin(q)*r,.35+progress(tick,CHARGE,80)*(height+1.1)+.3*Math.sin(q*2));
    place(p,pos);p.rotation(()->new ModelRotation(-15,center.getYaw()+(float)Math.toDegrees(-q)));
    if(tick%3==0)dust(pos,new Particle.DustOptions(Color.fromRGB(pair?75:178,pair?186:25,pair?255:42),.85f),2,.15);
   }
  }
  if(tick==KICK){
   shotHeight=ballPosition.getY()-center.getY();previousBall=ballPosition.clone();trace("kick contact; shared ball released");
   for(Player viewer:audience.players())viewer.spawnParticle(Particle.FLASH,ballPosition,1,0,0,0,0,Color.fromRGB(fire?255:120,fire?180:210,fire?65:255));particle(ballPosition,Particle.SWEEP_ATTACK,4,.3,0);
   sound(ballPosition,Sound.ENTITY_PLAYER_ATTACK_STRONG,1.8f,.65f);sound(ballPosition,Sound.ENTITY_BREEZE_WIND_BURST,1.5f,.8f);
   if(spiral!=null)spiral.rotation(()->new ModelRotation(-90,center.getYaw()));
  }
  if(tick>=KICK&&!impacted){
   double distance=distance(tick);Location next=point(0,1+distance,shotHeight+(1.2-shotHeight)*progress(tick,KICK,IMPACT));
   Vector direction=next.toVector().subtract(previousBall.toVector());double length=direction.length();
   if(!next.getWorld().isChunkLoaded(next.getBlockX()>>4,next.getBlockZ()>>4)){impact(previousBall);}
   else{
    var hit=length<.001?null:next.getWorld().rayTraceBlocks(previousBall,direction.clone().normalize(),length,FluidCollisionMode.NEVER,true);
    if(hit!=null)impact(hit.getHitPosition().toLocation(next.getWorld()));
    else {ballPosition=next;place(ball,next);
     if(spiral!=null){spiral.scaler(ModelScaler.value(.6f));place(spiral,next);}
     for(int i=0;i<penguins.size();i++){
      double q=tick*.65+i*Math.PI*2/penguins.size();Location p=point(Math.cos(q)*(.6+i*.06),1+distance,next.getY()-center.getY()+Math.sin(q)*(.6+i*.06));place(penguins.get(i),p);penguins.get(i).rotation(()->new ModelRotation(0,center.getYaw()));
     }
     for(int j=0;j<3;j++){Location p=previousBall.clone().add(direction.clone().multiply(j/3.0));particle(p,fire?Particle.FLAME:Particle.ELECTRIC_SPARK,fire?10:5,.2,.015);if(!fire)dust(p,new Particle.DustOptions(pair?Color.fromRGB(70,185,255):Color.fromRGB(175,25,55),1.1f),4,.25);}
     previousBall=next;
     if(tick>=IMPACT)impact(next);
    }
   }
  }
 }
 private void place(DummyTracker tracker,Location location){tracker.location(BukkitAdapter.adapt(location));}
 private void impact(Location location){
  if(impacted)return;impacted=true;
  audience.hide(ball);ball=null;if(spiral!=null){audience.hide(spiral);spiral=null;}for(var p:penguins)audience.hide(p);penguins.clear();
  particle(location,Particle.EXPLOSION_EMITTER,1,0,0);particle(location,Particle.GUST,12,1,.04);particle(location,Particle.CLOUD,60,1.3,.12);particle(location,fire?Particle.FLAME:Particle.ELECTRIC_SPARK,80,1.2,.12);
  sound(location,Sound.ENTITY_GENERIC_EXPLODE,2,.65f);trace("cosmetic shot impact");
 }
 @Override public boolean ownsMove(Location from,Location to){return pair?plugin.fusion().ownsMove(owner.getUniqueId(),from,to):flight.owns(from,to);}
 @Override public void externalTeleport(){if(flight!=null)flight.finishAtCurrent();}
 @Override void onClose(){if(flight!=null)flight.close();}
}
