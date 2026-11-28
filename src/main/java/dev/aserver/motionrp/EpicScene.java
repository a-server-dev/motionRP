package dev.aserver.motionrp;
import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Bounded, cosmetic summons of four iconic summons. */
final class EpicScene extends SummonScene {
 private final String effect;private final EntityTracker body;private final AerialAnchor pilot;
 private final Map<Sound,Set<Player>> heard=new HashMap<>();
 private DummyTracker main,stand,cloak;private Location lastOwnerPosition;private boolean walking;private final DummyTracker[] armGhosts=new DummyTracker[12];private final DummyTracker[] parts=new DummyTracker[EpicTimeline.ROCK_COUNT];
 private static final Particle.DustOptions PURPLE=new Particle.DustOptions(Color.fromRGB(108,20,178),1.5f),BLACK=new Particle.DustOptions(Color.fromRGB(12,5,18),1.5f),GOLD=new Particle.DustOptions(Color.fromRGB(255,220,83),1.2f);
 EpicScene(MotionPlugin plugin,Player owner,EntityTracker body,String effect){super(plugin,owner,effect,EpicTimeline.end(effect),128,!effect.equals("kokujo_tengen_myoo"));this.effect=effect;this.body=body;pilot=new AerialAnchor(plugin,owner);body.rotation(()->new ModelRotation(0,effect.equals("kokujo_tengen_myoo")?owner.getLocation().getYaw():origin.getYaw()));}
 @Override public boolean followsMovement(){return effect.equals("kokujo_tengen_myoo");}
 @Override String phase(){return EpicTimeline.phase(effect,tick);}
 @Override public boolean ownsMove(Location a,Location b){return pilot.owns(a,b);}
 @Override void onClose(){try{for(var entry:heard.entrySet())for(Player p:entry.getValue())if(p.isOnline())p.stopSound(entry.getKey(),SoundCategory.PLAYERS);}finally{heard.clear();pilot.close();}}
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),bone->Set.of("pri_right_item","pli_left_item").contains(bone.name().rawName()));
  switch(effect){case "road_roller"->roller();case "kokuryuha"->dragon();case "kokujo_tengen_myoo"->myoo();default->gravity();}
 }
 private void cue(Location p,Sound sound,float volume,float pitch){heard.computeIfAbsent(sound,k->new HashSet<>()).addAll(audience.players());sound(p,sound,volume,pitch);}
 private void stopCue(Sound sound){var listeners=heard.remove(sound);if(listeners!=null)for(Player p:listeners)if(p.isOnline())p.stopSound(sound,SoundCategory.PLAYERS);}
 private Location palm(){var p=EpicHandPath.at(effect,tick);return at(p.right(),p.forward(),p.height()).add(owner.getLocation().toVector().subtract(origin.toVector()));}
 private void locate(DummyTracker t,Location p){if(t!=null){t.location(BukkitAdapter.adapt(p));t.forceUpdate(true);}}
 private void flash(Location p,Color c){for(Player v:audience.players())v.spawnParticle(Particle.FLASH,p,1,0,0,0,0,c,true);}
 private boolean movePilot(Location p){p.setYaw(owner.getYaw());p.setPitch(owner.getPitch());if(owner.collidesAt(p)||!pilot.moveTo(p,origin)){plugin.playback().stop(owner.getUniqueId());return false;}return true;}
 private void blast(Location p,int amount){flash(p,Color.fromRGB(255,235,204));particle(p,Particle.EXPLOSION_EMITTER,amount,3,0);particle(p,Particle.DUST_PLUME,70,5,.12);particle(p,Particle.CLOUD,100,5,.15);particle(p,Particle.GUST,12,3,.1);cue(p,Sound.ENTITY_GENERIC_EXPLODE,4,.55f);}
 private void followUpperStand(){if(stand!=null){var p=owner.getLocation().clone();double yaw=Math.toRadians(origin.getYaw());p.add(Math.sin(yaw)*.25,.08,-Math.cos(yaw)*.25);locate(stand,p);}}
 private DummyTracker playerArmGhost(int side,int index){
  var renderer=Objects.requireNonNull(kr.toxicity.model.api.BetterModel.limbOrNull("motionrp_player_arm_"+(side==1?"right":"left")));
  var profile=kr.toxicity.model.api.profile.ModelProfile.of(BukkitAdapter.adapt(owner));
  var tracker=renderer.create(BukkitAdapter.adapt(owner.getLocation()),profile,MODIFIER,t->t.update(TrackerUpdateAction.brightness(15,15)));
  audience.show(tracker,plugin,prefix,"arm_afterimage"+index);return tracker;
 }
 private void roller(){
  if(tick==12){stand=model("motionrp_the_world",at(1.0,-.4,0),1.1f,"the_world");loop(stand,"world_idle");cue(origin,Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,1,.6f);trace("World humanoid scale=1.1");}
  if(tick==32){flash(origin,Color.fromRGB(255,238,155));cue(origin,Sound.BLOCK_BELL_RESONATE,2,.5f);trace("time-stop flash (cosmetic)");}
  if(tick>=40&&tick<68){double p=(tick-40)/28.0,h=16*Math.sin(p*Math.PI/2);if(!movePilot(at(-.55*p,7.1*p,h)))return;locate(stand,at(1-.45*p,7.15*p,h-.1*p));}
  if(tick==60){main=model("motionrp_road_roller",at(0,8,18.2),1.5f,"road_roller");main.update(TrackerUpdateAction.composite(TrackerUpdateAction.viewRange(4),TrackerUpdateAction.moveDuration(1)));main.rotator(ModelRotator.DEFAULT);cue(origin,Sound.ENTITY_IRON_GOLEM_REPAIR,2,.6f);trace("roller raised above both hands");}
  if(tick>=60&&tick<82){if(tick>=68){double hand=EpicHandPath.at(effect,tick).height();if(!movePilot(at(-.55,7.1,18.2-hand+.15)))return;}locate(main,tick<68?owner.getLocation().clone().add(0,2.2,0):at(0,8,18.2));main.rotation(()->new ModelRotation(24,origin.getYaw()));}
  if(tick>=82&&tick<208){double h=EpicTimeline.rollerHeight(tick),crush=Math.clamp((tick-104)/88.0,0,1),drop=Math.clamp((tick-82)/22.0,0,1);var jolt=EpicTimeline.rollerJolt(tick);locate(main,at(jolt.right(),8+jolt.forward(),h+jolt.up()));main.rotation(()->new ModelRotation((float)(24*(1-drop))+jolt.pitch(),origin.getYaw()+jolt.yaw()));double recover=Math.clamp((tick-94)/10.0,0,1),vault=Math.sin(Math.PI*recover),support=tick<94?-EpicHandPath.at(effect,tick).height()+.15:-.1*(1-recover)+1.59375*recover;double actorH=h+support+1.5*vault-.25*crush;if(!movePilot(at(-.55,7.1-2.6*vault,actorH)))return;}
  if(tick>=56&&tick<70&&stand!=null){stand.scaler(ModelScaler.value((float)(1.1*(70-tick)/14.0)));if(tick%3==0)particle(owner.getLocation().clone().add(0,1.5,0),Particle.END_ROD,3,.4,.01);}
  if(tick==70){audience.hide(stand);stand=null;trace("initial World dissolves at apex");}
  if(tick==104){stand=model("motionrp_the_world_upper",owner.getLocation(),1.1f,"the_world_upper");followUpperStand();trace("upper-body World follows player from behind");blast(at(0,8,.3),4);stand.animate("barrage",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);main.animate("crumple",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);trace("roller hits ground; synchronized player/World barrage and dent begin");}
  if(tick>=104&&tick<232)followUpperStand();
  for(int i=0;i<armGhosts.length;i++){
   int lane=i/2,side=i%2==0?1:-1,birth=104+(lane+(i%2)*2)%4;
   if(tick==birth){armGhosts[i]=playerArmGhost(side,i);loop(armGhosts[i],"rapid_punch");}
   if(armGhosts[i]!=null&&tick<168){var offset=EpicTimeline.armGhostOffset(i,tick);var p=at(offset[0],offset[1],offset[2]).add(owner.getLocation().toVector().subtract(origin.toVector()));locate(armGhosts[i],p);armGhosts[i].scaler(ModelScaler.value(1.15f+lane*.08f));}
   if(tick==168){audience.hide(armGhosts[i]);armGhosts[i]=null;}
  }

  if(tick>=220&&tick<232&&stand!=null)stand.scaler(ModelScaler.value((float)(1.1*(232-tick)/12.0)));
  if(tick>=104&&tick<168&&tick%2==0){var p=palm();particle(p,Particle.CRIT,16,.25,.08);particle(p,Particle.ELECTRIC_SPARK,12,.2,.08);var q=p.clone().add(0,.1,-.3);particle(q,Particle.CRIT,16,.25,.08);cue(p,Sound.ENTITY_PLAYER_ATTACK_STRONG,.9f,1.3f);var jolt=EpicTimeline.rollerJolt(tick);double dent=EpicTimeline.rollerDent(tick);var hit=at(((tick/2)%2==0?-.4:.4)+jolt.right(),7.44+jolt.forward(),2.4-.72*dent+jolt.up());
   particle(hit,Particle.SMALL_GUST,3,.22,.02);if(tick%4==0)particle(hit,Particle.GUST,1,.1,0);
   for(Player viewer:audience.players())viewer.spawnParticle(Particle.DUST_PILLAR,hit,10,.3,.12,.3,.06,Material.IRON_BLOCK.createBlockData());
   if(tick%8==0)trace("roller strike recoil and mace/wind impact tick="+tick);if(tick%18==14)trace("roller dent stage="+((tick-104)/18+1));}
  if(tick==184){cue(palm(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,2,.5f);trace("both fists raised; full-body finishing slam");}
  if(tick==192){var p=at(0,8,.5);blast(p,12);particle(p,Particle.DUST_PLUME,180,6,.2);particle(p,Particle.CLOUD,220,7,.22);particle(p,Particle.FLAME,100,4,.15);particle(p,Particle.ELECTRIC_SPARK,100,4,.2);particle(p,Particle.LAVA,35,3,.05);cue(p,Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,4,.5f);cue(p,Sound.ENTITY_BREEZE_WIND_BURST,4,.5f);for(int i=0;i<16;i++)parts[i]=model("motionrp_roller_debris",p,1,"roller_debris"+i);trace("final road roller blast with metal debris");}
  if(tick>=192&&tick<224){double t=(tick-192)/32.0;for(int i=0;i<16;i++){double a=i*2.399963,d=(3+i%5)*t;locate(parts[i],at(Math.cos(a)*d,8+Math.sin(a)*d,.6+10*t*(1-t)));}}
  if(tick==208){pilot.close();trace("pilot returned");}
  if(tick==224){for(int i=0;i<16;i++){audience.hide(parts[i]);parts[i]=null;}}
  if(tick==232){audience.hide(main);main=null;audience.hide(stand);stand=null;}
 }
 private Location dragonPoint(double d){var p=EpicTimeline.dragonOffset(d);return at(p[0],d,p[1]);}
 private void blackFlame(Location p,double width,int seed){
  for(int j=0;j<8;j++){
   double angle=j*Math.PI/4+tick*.18+seed,age=((tick+seed+j*3)%18)/18.0;
   var q=p.clone().add(Math.cos(angle)*width*(1-age*.65),age*width*1.7,Math.sin(angle)*width*(1-age*.65));
   dust(q,BLACK,3,.16);dust(q,new Particle.DustOptions(Color.fromRGB(28,27,31),1.6f),1,.1);
   if(j%4==0)particle(q,Particle.SMOKE,1,.1,.025);
  }
 }
 private void dragon(){
  if(tick>=16&&tick<50){blackFlame(palm(),.35,0);if(tick==16)cue(palm(),Sound.ENTITY_WARDEN_SONIC_CHARGE,1,.6f);}
  if(tick==50){stopCue(Sound.ENTITY_WARDEN_SONIC_CHARGE);cue(origin,Sound.ENTITY_ENDER_DRAGON_GROWL,2,.7f);trace("particle dragon released; no solid geometry");}
  if(tick>=50&&tick<96){
   double distance=EpicTimeline.dragonDistance(tick);
   for(int i=0;i<24;i++)if(distance>=i*.8)blackFlame(dragonPoint(distance-i*.8),Math.max(.18,1.1-i*.035),i);
  }
  if(tick==96){var p=at(0,36,.8);blast(p,5);dust(p,BLACK,180,4);trace("black dragon impact");}
  if(tick>=112&&tick<208){
   for(int i=0;i<22;i++)if(tick>=112+i){double time=tick-i;blackFlame(returningDragon(time),Math.max(.15,(1.05-i*.035)*Math.clamp((208-time)/20.,0,1)),i);}
   if(tick==112)trace("black dragon returns overhead");
  }
  if(tick==208){cue(origin,Sound.ENTITY_ENDER_DRAGON_GROWL,1,.5f);trace("black dragon devoured caster; particle flame mantle formed");}
  if(tick>=188&&tick<320){
   double strength=Math.clamp((tick-188)/20.,0,1)*Math.clamp((320-tick)/24.,0,1);
   for(int i=0;i<20;i++){
    double age=((tick+i*7)%30)/30.,angle=i*2.39996+tick*.045;
    double spread=(.55+age*1.35)*strength;
    Location p=owner.getLocation().clone().add(Math.cos(angle)*spread,.3+age*3.9*strength,Math.sin(angle)*spread*.6);
    dust(p,BLACK,5,.12);if(i%4==0)particle(p,Particle.SMOKE,1,.07,.035);
   }
  }
 }
 private Location returningDragon(double time){if(time<172){double p=Math.clamp((time-112)/60,0,1);return at(2.5*Math.sin(p*Math.PI*4)*(1-p),36*(1-p)*(1-p),1.8+18*Math.sin(Math.PI*p)+10*p);}double p=Math.clamp((time-172)/16,0,1);return owner.getLocation().clone().add(0,11.8-10.1*p,0);}
 private Location myooBase(){var p=owner.getLocation().clone();double yaw=Math.toRadians(p.getYaw());p.add(Math.sin(yaw)*6,0,-Math.cos(yaw)*6);var surface=ground(p);if(surface!=null)p.setY(surface.getY()-.12);return p;}
 private void myoo(){
  var current=owner.getLocation();boolean moving=lastOwnerPosition!=null&&lastOwnerPosition.getWorld()==current.getWorld()&&lastOwnerPosition.distanceSquared(current)>.0005;lastOwnerPosition=current;
  if(moving!=walking){walking=moving;if(walking){body.animate("myoo_walk",AnimationModifier.builder().type(kr.toxicity.model.api.animation.AnimationIterator.Type.LOOP).build());if(main!=null)loop(main,"walk");}else{body.stopAnimation("myoo_walk");body.animate("myoo_stand",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);if(main!=null){main.stopAnimation("walk");main.animate("stand_legs",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);}}}

  if(tick==16){main=model("motionrp_tengen_myoo",myooBase(),.1f,"tengen_myoo");loop(main,"idle");if(walking)loop(main,"walk");cue(owner.getLocation(),Sound.ENTITY_WITHER_SPAWN,1,.5f);}
  if(main!=null){float scale=(float)(.1+5.9*Math.clamp((tick-16)/38.0,0,1));if(tick>=196)scale*=Math.max(.01,(220-tick)/24.0);main.scaler(ModelScaler.value(scale));locate(main,myooBase());main.rotation(()->new ModelRotation(0,owner.getLocation().getYaw()));}
  if(tick==54){main.stopAnimation("idle");main.animate("slam",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);trace("giant mirrors player windup and horizontal sweep");}
  if(tick==124){cue(owner.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,4,.5f);cue(owner.getLocation(),Sound.ENTITY_IRON_GOLEM_ATTACK,4,.5f);}
  if(tick>=124&&tick<=148&&tick%3==1){double angle=Math.toRadians(owner.getLocation().getYaw()-65+(tick-124)*160/24.0);var base=myooBase();for(int j=0;j<3;j++){double radius=22+j*5;var p=ground(base.clone().add(-Math.sin(angle)*radius,0,Math.cos(angle)*radius));if(p!=null){particle(p,Particle.EXPLOSION_EMITTER,1,.4,0);particle(p,Particle.DUST_PLUME,16,2,.1);particle(p,Particle.GUST,3,1,.1);}}if(tick==124){cue(base,Sound.ENTITY_GENERIC_EXPLODE,6,.5f);trace("myoo sweeping sword impact and ground shockwave");}}
 }

 private Location rockStart(int i){double a=i*2.399963,r=6+(i%7)*1.6;return at(Math.cos(a)*r,18+Math.sin(a)*r,.4);}
 private void gravity(){
  Location core=at(0,18,22);
  if(tick==28){main=model("motionrp_gravity_core",at(0,2,2),.5f,"gravity_core");cue(origin,Sound.BLOCK_BEACON_POWER_SELECT,1,.5f);}
  if(main!=null){double p=Math.clamp((tick-28)/40.0,0,1);locate(main,at(0,2+16*p,2+20*p));}
  // Every chunk remains where it arrives. Inner strata fill first, then the outer shell.
  double fade=tick<288?1:Math.clamp((312-tick)/24.0,0,1);
  for(int i=0;i<EpicTimeline.ROCK_COUNT;i++){
   int start=56+i,age=tick-start;
   if(age==0){parts[i]=model("motionrp_gravity_rock",rockStart(i),(float)EpicTimeline.rockScale(i),"attracted_rock"+i);parts[i].update(TrackerUpdateAction.viewRange(4));particle(rockStart(i),Particle.DUST_PLUME,8,1,.06);}
   if(parts[i]!=null){
    var normal=EpicTimeline.rockNormal(i);double radius=EpicTimeline.rockRadius(i);Location dest=core.clone().add(normal[0]*radius,normal[1]*radius,normal[2]*radius);
    double p=Math.clamp(age/40.0,0,1);Location from=rockStart(i);Location point=from.clone().add(dest.toVector().subtract(from.toVector()).multiply(p*p));
    if(age<40){double swirl=Math.sin(Math.PI*p)*1.5;point.add(Math.cos(i+p*6)*swirl,0,Math.sin(i+p*6)*swirl);}else if(age==40)trace("rock retained on shell index="+i+" stratum="+EpicTimeline.rockLayer(i));
    locate(parts[i],point);parts[i].scaler(ModelScaler.value((float)(EpicTimeline.rockScale(i)*fade)));
   }
  }
  if(tick>=56&&tick<240&&tick%4==0){dust(core,PURPLE,4,.6);particle(core,Particle.SMOKE,3,.5,.02);if(tick%16==0)cue(core,Sound.BLOCK_STONE_BREAK,2,.6f);}
  if(tick==240){audience.hide(main);main=null;trace("gravity moon formed from 144 retained rocks in three strata");cue(core,Sound.ENTITY_ELDER_GUARDIAN_CURSE,2,.5f);}

 }
}
