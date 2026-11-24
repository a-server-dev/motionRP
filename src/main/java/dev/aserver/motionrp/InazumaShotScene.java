package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;
import static dev.aserver.motionrp.core.FootballTimeline.*;

/** Three reference shots; display-only projectiles and reversible, swept pilot movement. */
final class InazumaShotScene extends SummonScene {
 private final EntityTracker body;private final InazumaShot shot;private final AerialAnchor flight;
 private DummyTracker ball,aura;private final List<DummyTracker> flakes=new ArrayList<>();
 private Location ballAt,shotStart,previousBall,swordLiftStart,swordContact;private boolean impacted;private double minimumKnifeGap=Double.POSITIVE_INFINITY;
 InazumaShotScene(MotionPlugin plugin,Player owner,EntityTracker body,String id){
  super(plugin,owner,"Inazuma-"+id,InazumaShot.from(id).end,64,true);this.body=body;shot=InazumaShot.from(id);flight=new AerialAnchor(plugin,owner);
  body.rotation(()->new ModelRotation(0,origin.getYaw()));
 }
 @Override String phase(){return shot.phase(tick);}
 private Location boot(){var p=body.bone("shot_boot").worldPosition();return owner.getLocation().add(p.x,p.y,p.z);}
 private Location contact(){return boot().add(at(0,.26,.03).toVector().subtract(origin.toVector()));}
 private Location blend(Location a,Location b,double p){return a.clone().add(b.toVector().subtract(a.toVector()).multiply(p));}
 private Vector forward(){return at(0,1,0).toVector().subtract(origin.toVector());}
 private void pose(DummyTracker tracker,Location at,Vector direction){
  var facing=FootballVisuals.penguinFacing(direction.getX(),direction.getY(),direction.getZ());var rotation=new ModelRotation(facing.pitch(),facing.yaw());
  Location loc=at.clone();loc.setYaw(facing.yaw());loc.setPitch(facing.pitch());tracker.location(BukkitAdapter.adapt(loc));
  tracker.rotator(ModelRotator.DEFAULT);tracker.rotation(()->rotation);
  var packets=BetterModel.nms().createBundler(tracker.bones().size());for(var b:tracker.bones())b.rotate(rotation,packets);
  for(Player p:audience.players())packets.send(BukkitAdapter.adapt(p));
 }
 private boolean movePilot(){
  if(shot.jumpHeight==0)return true;Location seat=at(0,0,shot.height(tick)),from=owner.getLocation();Vector delta=seat.toVector().subtract(from.toVector());
  int count=Math.max(1,(int)Math.ceil(delta.length()/.18));
  for(int i=1;i<=count;i++){Vector step=delta.clone().multiply(i/(double)count);Location test=from.clone().add(step);
   if(!test.getWorld().isChunkLoaded(test.getBlockX()>>4,test.getBlockZ()>>4)||owner.wouldCollideUsing(owner.getBoundingBox().shift(step))){plugin.playback().stop(owner.getUniqueId());return false;}}
  if(!flight.moveTo(seat,origin)){plugin.playback().stop(owner.getUniqueId());return false;}return true;
 }
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));if(!movePilot())return;
  if(tick==0){ballAt=at(.12,.4,.35);ball=model("motionrp_football",ballAt,1,"ball");ball.update(TrackerUpdateAction.moveDuration(1));trace("duration="+shot.end+" source="+shot.sourceStart+".."+shot.sourceEnd+" speed=1.2x-duration");}
  if(tick==shot.auraStart()){
   String id=switch(shot){case DEATH_SWORD->"motionrp_death_sword_edge";case ETERNAL_BLIZZARD->"motionrp_blizzard_orb";case DEATH_SPEAR->"motionrp_death_spear_drill";};
   if(shot==InazumaShot.DEATH_SWORD){
    audience.hide(ball);ball=model("motionrp_death_sword_core",ballAt,1,"dark_ball");ball.update(TrackerUpdateAction.moveDuration(1));trace("dark conversion tick="+tick);
   }
   aura=model(id,ballAt,.2f,"charge_effect");aura.update(TrackerUpdateAction.moveDuration(1));
   if(shot!=InazumaShot.DEATH_SWORD)loop(aura,"spin");
   sound(origin,shot==InazumaShot.ETERNAL_BLIZZARD?Sound.BLOCK_AMETHYST_BLOCK_CHIME:Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,1.2f,.65f);
   if(shot==InazumaShot.ETERNAL_BLIZZARD)for(int i=0;i<6;i++){var f=model("motionrp_blizzard_flake",ballAt,.3f,"snowflake_"+i);f.update(TrackerUpdateAction.moveDuration(1));loop(f,"spin");flakes.add(f);}
  }
  if(tick<=shot.kick){
   Location target=contact();
   if(shot==InazumaShot.DEATH_SWORD){
    if(tick<16)ballAt=blend(at(.12,.4,.35),target,smooth(tick,0,16));
    else if(tick<44)ballAt=target;
    else if(tick<60){if(swordLiftStart==null)swordLiftStart=ballAt.clone();ballAt=blend(swordLiftStart,at(.1,.9,1.85),smooth(tick,44,60));}
    else if(tick<=shot.strike())ballAt=blend(at(.1,.9,1.85),target,smooth(tick,shot.strike()-6,shot.strike()));
    else ballAt=blend(swordContact,at(.1,1.15,.75),smooth(tick,shot.strike(),88));
    if(tick==shot.strike()){
     swordContact=ballAt.clone();particle(ballAt,Particle.CRIT,18,.12,.08);particle(ballAt,Particle.SWEEP_ATTACK,1,0,0);
     sound(ballAt,Sound.ENTITY_PLAYER_ATTACK_STRONG,1.4f,.8f);trace("strike tick="+tick+" contactGap="+ballAt.distance(target));
    }
   }else{
    double high=shot==InazumaShot.ETERNAL_BLIZZARD?5.4:3.3;Location top=at(0,1,high);
    ballAt=tick<shot.kick-12?blend(at(.12,.4,.35),top,smooth(tick,12,shot==InazumaShot.ETERNAL_BLIZZARD?44:38)):blend(top,target,smooth(tick,shot.kick-12,shot.kick));
   }
   pose(ball,ballAt,forward());
   if(aura!=null){aura.scaler(ModelScaler.value((float)(.2+1.0*smooth(tick,shot.auraStart(),shot.kick))));pose(aura,ballAt,forward());}
   if(tick%2==0&&tick>=shot.auraStart())chargeParticles();
   if(tick==shot.strike())trace("contactGap="+ballAt.distance(target)+" boot="+ballAt.getX()+","+ballAt.getY()+","+ballAt.getZ());
  }
  if(shot==InazumaShot.DEATH_SWORD&&tick>=88&&tick<=shot.kick){
   var edge=body.bone("shot_knife_edge").worldPosition();Location hand=owner.getLocation().add(edge.x,edge.y,edge.z);double gap=hand.distance(ballAt)-5.25/16;
   minimumKnifeGap=Math.min(minimumKnifeGap,gap);trace("knife tick="+tick+" gap="+gap+" hand="+hand.getX()+","+hand.getY()+","+hand.getZ());
   if(tick>=94&&tick%2==0)particle(hand,Particle.SWEEP_ATTACK,1,0,0);
   if(tick==shot.kick)trace("knife minimumGap="+minimumKnifeGap+" release tick="+tick);
   if(tick==100){
    var torso=body.bone("shot_torso").worldPosition();var upper=body.bone("shot_upper").worldPosition().sub(torso,new org.joml.Vector3f()).normalize();var support=body.bone("shot_support_boot").worldPosition();
    trace("deep bend forward="+(upper.x*forward().getX()+upper.z*forward().getZ())+" up="+upper.y+" supportHeight="+support.y+" pilotHeight="+(owner.getLocation().getY()-origin.getY()));
   }
  }
  if(tick==shot.kick){
   shotStart=shot==InazumaShot.DEATH_SWORD?ballAt.clone():contact();previousBall=shotStart.clone();ballAt=shotStart;pose(ball,ballAt,forward());
   particle(ballAt,Particle.SWEEP_ATTACK,3,.1,0);particle(ballAt,Particle.ELECTRIC_SPARK,16,.25,.03);
   sound(ballAt,Sound.ENTITY_PLAYER_ATTACK_STRONG,1.6f,.6f);sound(ballAt,Sound.ENTITY_BREEZE_WIND_BURST,1.6f,shot==InazumaShot.ETERNAL_BLIZZARD?1.2f:.65f);trace("kick tick="+tick);
  }
  if(tick>=shot.kick&&!impacted)projectile();
  for(int i=0;i<flakes.size();i++){
   double angle=tick*.15+i*Math.PI/3;Location center=tick<shot.kick?ballAt:previousBall;
   if(center!=null)pose(flakes.get(i),center.clone().add(Math.cos(angle)*1.5,Math.sin(angle)*1.5,Math.sin(angle*.6)*.5),forward());
  }
 }
 private void chargeParticles(){
  boolean ice=shot==InazumaShot.ETERNAL_BLIZZARD;Color color=ice?Color.fromRGB(110,225,255):shot==InazumaShot.DEATH_SWORD?Color.fromRGB(25,65,245):Color.fromRGB(235,20,45);
  for(int strand=0;strand<2;strand++)for(int i=0;i<9;i++){
   double a=tick*.35+i*.55+strand*Math.PI,r=.5+i*.045;Location p=ballAt.clone().add(Math.cos(a)*r,(i-4)*.15,Math.sin(a)*r);
   dust(p,new Particle.DustOptions(color,.8f),1,.025);particle(p,ice?Particle.SNOWFLAKE:Particle.SMOKE,1,.015,0);
  }
  if(ice){Location torso=owner.getLocation().add(0,1,0);particle(torso,Particle.SNOWFLAKE,6,.7,.02);particle(torso,Particle.CLOUD,3,.6,.005);}
  if(tick%12==0)sound(ballAt,ice?Sound.BLOCK_GLASS_BREAK:Sound.BLOCK_BEACON_POWER_SELECT,.45f,ice?1.8f:.7f);
 }
 private void projectile(){
  double p=progress(tick,shot.kick,shot.impact);Location next=shotStart.clone().add(forward().multiply(42*p)).add(0,(1.1-(shotStart.getY()-origin.getY()))*p,0);
  Vector delta=next.toVector().subtract(previousBall.toVector());double length=delta.length();
  if(!next.getWorld().isChunkLoaded(next.getBlockX()>>4,next.getBlockZ()>>4)){impact(previousBall);return;}
  var hit=length<.0001?null:next.getWorld().rayTraceBlocks(previousBall,delta.clone().normalize(),length,FluidCollisionMode.NEVER,true);
  if(hit!=null){impact(hit.getHitPosition().toLocation(next.getWorld()));return;}
  Vector direction=length<.0001?forward():delta;ballAt=next;pose(ball,next,direction);if(aura!=null){aura.scaler(ModelScaler.value(shot==InazumaShot.ETERNAL_BLIZZARD?1.35f:1.1f));pose(aura,next,direction);}
  for(int j=0;j<4;j++){
   Location wake=blend(previousBall,next,j/4.0);boolean ice=shot==InazumaShot.ETERNAL_BLIZZARD;
   particle(wake,ice?Particle.SNOWFLAKE:Particle.SMOKE,ice?8:4,.28,.015);
   dust(wake,new Particle.DustOptions(ice?Color.fromRGB(175,245,255):shot==InazumaShot.DEATH_SWORD?Color.fromRGB(28,65,245):Color.fromRGB(240,15,40),1.3f),5,.2);
   if(ice)particle(wake,Particle.END_ROD,2,.15,0);
  }
  if(tick%8==0&&shot==InazumaShot.DEATH_SPEAR)sound(next,Sound.ITEM_TRIDENT_THROW,.65f,.55f);
  previousBall=next;if(tick>=shot.impact)impact(next);
 }
 private void impact(Location at){
  if(impacted)return;impacted=true;audience.hide(ball);ball=null;audience.hide(aura);aura=null;
  particle(at,Particle.EXPLOSION_EMITTER,1,0,0);particle(at,Particle.GUST,14,1,.05);particle(at,Particle.CLOUD,65,1.6,.1);
  boolean ice=shot==InazumaShot.ETERNAL_BLIZZARD;particle(at,ice?Particle.SNOWFLAKE:Particle.ELECTRIC_SPARK,100,2,.08);
  sound(at,ice?Sound.BLOCK_GLASS_BREAK:Sound.ENTITY_GENERIC_EXPLODE,2,.65f);trace("impact tick="+tick+" cosmetic=true");
 }
 @Override public boolean ownsMove(Location from,Location to){return flight.owns(from,to);}
 @Override public void externalTeleport(){flight.finishAtCurrent();}
 @Override void onClose(){flight.close();}
}
