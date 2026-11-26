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
 private Location ballAt,shotStart,previousBall,swordContact,swordHoldHand,blizzardLiftStart,swordBallRest,swordKickTarget;private boolean impacted;private double minimumKnifeGap=Double.POSITIVE_INFINITY;
 InazumaShotScene(MotionPlugin plugin,Player owner,EntityTracker body,String id){
  super(plugin,owner,"Inazuma-"+id,InazumaShot.from(id).end,64,true);this.body=body;shot=InazumaShot.from(id);flight=new AerialAnchor(plugin,owner);
  body.rotation(()->new ModelRotation(0,origin.getYaw()));
 }
 @Override String phase(){return shot.phase(tick);}
 private int sword(int reference){return SwordShotTimeline.at(reference);}
 private boolean scissor(){return shot==InazumaShot.ETERNAL_BLIZZARD;}
 private int trick(int clock){return ScissorShotTimeline.at(shot==InazumaShot.ETERNAL_BLIZZARD,clock);}
 private int ice(int reference){return trick(BlizzardTimeline.at(reference));}
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
  // The scoop and high kick meet fixed points on a free ball arc; no pre-impact foot attraction.
  if(tick==0){swordBallRest=at(.1171875,.80800776,.34421627);swordKickTarget=at(.1171875,.84406910,1.06542876);ballAt=shot==InazumaShot.DEATH_SWORD||shot==InazumaShot.DEATH_SPEAR?swordBallRest.clone():at(.12,.4,.35);ball=model("motionrp_football",ballAt,1,"ball");ball.update(TrackerUpdateAction.moveDuration(1));trace("duration="+shot.end+" source="+shot.sourceStart+".."+shot.sourceEnd+" durationScale="+shot.durationScale);}
  if(tick==shot.auraStart()){
   String id=switch(shot){case DEATH_SWORD->"motionrp_death_sword_edge";case ETERNAL_BLIZZARD->"motionrp_blizzard_wind";case DEATH_SPEAR->"motionrp_death_spear_drill";};
   if(shot==InazumaShot.DEATH_SWORD){
    audience.hide(ball);ball=model("motionrp_death_sword_core",ballAt,1,"dark_ball");ball.update(TrackerUpdateAction.moveDuration(1));trace("dark conversion tick="+tick);
   }
   aura=model(id,ballAt,.2f,"charge_effect");trace("aura begin tick="+tick);aura.update(TrackerUpdateAction.moveDuration(1));
   if(shot!=InazumaShot.DEATH_SWORD)loop(aura,"spin");
   sound(origin,shot==InazumaShot.ETERNAL_BLIZZARD?Sound.BLOCK_AMETHYST_BLOCK_CHIME:Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,1.2f,.65f);

  }
  if(shot==InazumaShot.ETERNAL_BLIZZARD&&tick==32){
   audience.hide(ball);ball=model("motionrp_blizzard_orb",ballAt,.7f,"frozen_ball");ball.update(TrackerUpdateAction.moveDuration(1));loop(ball,"spin");trace("freeze tick="+tick);
  }
  if(tick<=shot.kick){
   Location target=contact();
   if(shot==InazumaShot.DEATH_SWORD){
    // Before contact the ball is independent of the foot; the kick comes to it.
    if(tick<=sword(22))ballAt=swordBallRest.clone();
    else if(tick<=shot.strike()){
     double p=progress(tick,sword(22),shot.strike());ballAt=blend(swordBallRest,swordKickTarget,p).add(0,.95*4*p*(1-p),0);
    }
    else {
     double p=progress(tick,shot.strike(),sword(88));
     ballAt=blend(swordContact,at(.25,1.15,swordContact.getY()-origin.getY()),p);ballAt.setY(swordContact.getY());
    }
    if(tick==sword(22))trace("scoop contactGap="+ballAt.distance(target)+" preKickDrift="+ballAt.distance(swordBallRest));
    if(tick==shot.strike()){
     swordContact=ballAt.clone();particle(ballAt,Particle.CRIT,18,.12,.08);particle(ballAt,Particle.SWEEP_ATTACK,1,0,0);
     sound(ballAt,Sound.ENTITY_PLAYER_ATTACK_STRONG,1.4f,.8f);trace("strike tick="+tick+" contactGap="+ballAt.distance(target)+" bootHeight="+(boot().getY()-owner.getLocation().getY()));
    }
   }else if(shot==InazumaShot.DEATH_SPEAR){
    if(tick<=10)ballAt=swordBallRest.clone();
    else ballAt=blend(swordBallRest,at(0,.13,10),1-Math.pow(1-progress(tick,10,38),2));
    // Follow the edited front/back scissor gap while the player reaches the apex.
    if(tick>38)ballAt=blend(at(0,.13,10),at(.20,.15,9.82),smooth(tick,38,60));
    if(tick==10)trace("spear loft contactGap="+ballAt.distance(contact()));
    if(tick==38)trace("spear apex height="+(ballAt.getY()-origin.getY()));
    if(tick==60||tick==69)trace("spear apex pause tick="+tick+" height="+(owner.getLocation().getY()-origin.getY()));
    if(tick==70){sound(ballAt,Sound.ENTITY_PLAYER_ATTACK_STRONG,1.4f,.6f);particle(boot(),Particle.CRIT,8,.08,.02);}
    if(tick>=66&&tick<=86){
     var rp=body.bone("shot_boot").worldPosition();var lp=body.bone("ice_other_boot").worldPosition();
     trace("spear pinch tick="+tick+" rightClearance="+spearClearance("pinch_r_")+" leftClearance="+spearClearance("pinch_l_")+" rightForward="+(rp.x*forward().getX()+rp.z*forward().getZ())+" leftForward="+(lp.x*forward().getX()+lp.z*forward().getZ())+" ballScale=1");
    }
   }else{
    Location top=at(0,1,shot==InazumaShot.ETERNAL_BLIZZARD?4.4:3.3);
    if(tick<=ice(44)){
     Location toe=boot().add(0,.25,0);ballAt=tick<ice(38)?at(.12,.4,.35):toe;
     if(tick==ice(44)){blizzardLiftStart=ballAt.clone();trace("loft tick="+tick+" footGap="+(ballAt.distance(boot())-.25));}
    }else if(tick<=trick(34)){
     Location center=at(0,.15,tick<=18?shot.height(tick)+.42:1.27);ballAt=tick<trick(10)?blend(blizzardLiftStart,center,smooth(tick,ice(44),trick(10))):center;
     if(tick%2==0)particle(ballAt,Particle.SNOWFLAKE,3,.1,.005);
     if(tick==trick(10)||tick==trick(18)){
      var left=body.bone("ice_other_boot").worldPosition();Location lf=owner.getLocation().add(left.x,left.y,left.z);Vector rv=boot().toVector().subtract(ballAt.toVector()),lv=lf.toVector().subtract(ballAt.toVector()),side=at(1,0,0).toVector().subtract(origin.toVector());
      trace("foot brush tick="+tick+" rightSide="+rv.dot(side)+" leftSide="+lv.dot(side)+" rightForward="+rv.dot(forward())+" leftForward="+lv.dot(forward()));
     }
     if(tick==trick(34)){blizzardLiftStart=ballAt.clone();trace("right turn complete tick="+tick);}
    }else if(tick<ice(116)){double p=progress(tick,trick(34),ice(116));ballAt=blend(blizzardLiftStart,top,1-(1-p)*(1-p));}
    else if(tick<ice(162))ballAt=top;
    else if(tick<ice(200)){double p=progress(tick,ice(162),ice(200)),r=.55*Math.sin(Math.PI*p),a=p*Math.PI*2;ballAt=top.clone().add(at(r*Math.cos(a),r*Math.sin(a),0).toVector().subtract(origin.toVector()));}
    else ballAt=blend(top,target,smooth(tick,ice(200),shot.kick));
    if(tick>=trick(32))ball.scaler(ModelScaler.value((float)(.7+.7*smooth(tick,trick(32),ice(116)))));
   }
   Vector spin=forward();if(scissor()&&tick>=ice(44)&&tick<=trick(18)){double angle=progress(tick,ice(44),trick(18))*Math.PI*2;spin=new Vector(Math.sin(angle),Math.sin(angle*.5)*.4,Math.cos(angle));}
   if(shot==InazumaShot.DEATH_SPEAR&&tick>=70&&tick<=shot.kick){double p=progress(tick,70,80),a=p*p*Math.PI*6;spin=new Vector(Math.sin(a),.25*Math.sin(a*.5),Math.cos(a));}
   pose(ball,ballAt,spin);
   if(aura!=null){aura.scaler(ModelScaler.value((float)(.2+1.0*smooth(tick,shot.auraStart(),shot.kick))));pose(aura,shot==InazumaShot.ETERNAL_BLIZZARD?owner.getLocation():ballAt,forward());}
   if(tick%2==0&&tick>=shot.auraStart())chargeParticles();
   if(tick==shot.strike()&&shot!=InazumaShot.DEATH_SPEAR)trace("contactGap="+ballAt.distance(target)+" boot="+ballAt.getX()+","+ballAt.getY()+","+ballAt.getZ());
  }
  if(shot==InazumaShot.DEATH_SWORD&&tick>=sword(88)&&tick<=shot.kick){
   var edge=body.bone("shot_knife_edge").worldPosition();Location hand=owner.getLocation().add(edge.x,edge.y,edge.z);double gap=hand.distance(ballAt)-5.25/16;
   minimumKnifeGap=Math.min(minimumKnifeGap,gap);trace("knife tick="+tick+" gap="+gap+" hand="+hand.getX()+","+hand.getY()+","+hand.getZ());
   if(tick==sword(94))swordHoldHand=hand.clone();
   if(tick==sword(96)&&swordHoldHand!=null)trace("hand hold drift="+hand.distance(swordHoldHand));
   if(tick>=sword(98)&&tick%2==0)particle(hand,Particle.SWEEP_ATTACK,1,0,0);
   if(tick==shot.kick)trace("knife minimumGap="+minimumKnifeGap+" release tick="+tick);
   if(tick==sword(94)||tick==shot.kick){
    var torso=body.bone("shot_torso").worldPosition();var upper=body.bone("shot_upper").worldPosition().sub(torso,new org.joml.Vector3f()).normalize();var support=body.bone("shot_support_boot").worldPosition();
    trace("torso phase="+(tick==sword(94)?"hold":"swing")+" forward="+(upper.x*forward().getX()+upper.z*forward().getZ())+" up="+upper.y+" supportHeight="+support.y+" rightHeight="+body.bone("shot_boot").worldPosition().y+" pilotHeight="+(owner.getLocation().getY()-origin.getY()));
   }
  }
  if(shot==InazumaShot.DEATH_SWORD&&tick==shot.end-1){
   var lead=body.bone("shot_knife_edge").worldPosition();var trail=body.bone("shot_trail_hand").worldPosition();var left=body.bone("shot_support_boot").worldPosition();var right=body.bone("shot_boot").worldPosition();
   trace("finish lowHand="+lead.y+" raisedHand="+trail.y+" leftFoot="+left.y+" rightFoot="+right.y+" stance="+((left.x-right.x)*forward().getX()+(left.z-right.z)*forward().getZ())+" hip="+body.bone("shot_hip").worldPosition().y+" armReach="+((lead.x-trail.x)*forward().getX()+(lead.z-trail.z)*forward().getZ()));
  }
  if(scissor()&&(tick==ice(90)||tick==ice(136))){
   var left=body.bone("ice_other_boot").worldPosition();var right=body.bone("shot_boot").worldPosition();
   var diagonal=body.bone("ice_trail_hand").worldPosition().sub(body.bone("ice_left_shoulder").worldPosition(),new org.joml.Vector3f()).normalize();trace("crouch arm vertical="+diagonal.y);
   trace("land reset tick="+tick+" pilot="+(owner.getLocation().getY()-origin.getY())+" left="+left.y+" right="+right.y+" hand="+body.bone("ice_ground_hand").worldPosition().y+" spread="+Math.hypot(left.x-right.x,left.z-right.z));if(tick==ice(90))sound(owner.getLocation(),Sound.ENTITY_PLAYER_SMALL_FALL,.7f,1.2f);
  }
  if(scissor()&&(tick==ice(222)||tick==shot.kick)){
   var foot=boot().toVector().subtract(owner.getLocation().toVector());trace("ice leg tick="+tick+" forward="+foot.dot(forward()));
  }
  if(shot==InazumaShot.DEATH_SWORD&&tick>=shot.strike()&&tick<=shot.kick)trace("ball level tick="+tick+" height="+(ballAt.getY()-origin.getY()));
  if(tick==shot.kick){
   shotStart=shot!=InazumaShot.ETERNAL_BLIZZARD?ballAt.clone():contact();previousBall=shotStart.clone();ballAt=shotStart;pose(ball,ballAt,forward());
   if(shot==InazumaShot.DEATH_SWORD){
    sound(ballAt,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.4f,.65f);trace("shot tick="+tick+" trigger=hand");
   }else{
    particle(ballAt,Particle.SWEEP_ATTACK,3,.1,0);sound(ballAt,Sound.ENTITY_PLAYER_ATTACK_STRONG,1.6f,.6f);trace("kick tick="+tick);
   }
   particle(ballAt,Particle.ELECTRIC_SPARK,16,.25,.03);sound(ballAt,Sound.ENTITY_BREEZE_WIND_BURST,1.6f,shot==InazumaShot.ETERNAL_BLIZZARD?1.2f:.65f);
   if(shot==InazumaShot.ETERNAL_BLIZZARD){
    audience.hide(aura);aura=model("motionrp_blizzard_wake",ballAt,1,"frozen_wake");aura.update(TrackerUpdateAction.moveDuration(1));loop(aura,"spin");
    for(int i=0;i<6;i++){var f=model("motionrp_blizzard_flake",ballAt,.65f,"snowflake_"+i);f.update(TrackerUpdateAction.moveDuration(1));loop(f,"spin");flakes.add(f);}
   }
  }
  if(tick>=shot.kick&&!impacted)projectile();
  for(int i=0;i<flakes.size();i++){
   double angle=tick*.15+i*Math.PI/3;Location center=tick<shot.kick?ballAt:previousBall;
   if(center!=null)pose(flakes.get(i),center.clone().add(Math.cos(angle)*1.5,Math.sin(angle)*1.5,Math.sin(angle*.6)*.5),forward());
  }
 }
 private double spearClearance(String prefix){
  var p=body.bone(prefix+"o").worldPosition();Vector o=new Vector(p.x,p.y,p.z),center=ballAt.toVector().subtract(owner.getLocation().toVector()),delta=center.clone().subtract(o),nearest=o.clone();
  for(String axis:List.of("x","y","z")){
   var q=body.bone(prefix+axis).worldPosition();Vector v=new Vector(q.x,q.y,q.z).subtract(o);double length=v.length();v.normalize();nearest.add(v.clone().multiply(Math.clamp(delta.dot(v),0,length)));
  }
  return nearest.distance(center)-5.25/16;
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
  double p=progress(tick,shot.kick,shot.impact);Location next=shotStart.clone().add(forward().multiply(42*p));if(shot!=InazumaShot.DEATH_SWORD)next.add(0,(1.1-(shotStart.getY()-origin.getY()))*p,0);
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
  if(shot==InazumaShot.DEATH_SWORD)trace("ball level tick="+tick+" height="+(ballAt.getY()-origin.getY()));
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
