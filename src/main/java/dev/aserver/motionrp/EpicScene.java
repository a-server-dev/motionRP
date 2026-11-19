package dev.aserver.motionrp;
import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Bounded, cosmetic first versions of four iconic summons. */
final class EpicScene extends SummonScene {
 private final String effect;private final EntityTracker body;private final AerialAnchor pilot;
 private final Map<Sound,Set<Player>> heard=new HashMap<>();
 private DummyTracker main,stand,moon;private final DummyTracker[] parts=new DummyTracker[96];
 private static final Particle.DustOptions PURPLE=new Particle.DustOptions(Color.fromRGB(108,20,178),1.5f),BLACK=new Particle.DustOptions(Color.fromRGB(12,5,18),1.5f),GOLD=new Particle.DustOptions(Color.fromRGB(255,220,83),1.2f);
 EpicScene(MotionPlugin plugin,Player owner,EntityTracker body,String effect){super(plugin,owner,effect,EpicTimeline.end(effect),128,true);this.effect=effect;this.body=body;pilot=new AerialAnchor(plugin,owner);body.rotation(()->new ModelRotation(0,origin.getYaw()));}
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
 private void roller(){
  if(tick==12){stand=model("motionrp_the_world",at(1.0,-.4,0),1.1f,"the_world");loop(stand,"world_idle");cue(origin,Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,1,.6f);trace("World humanoid scale=1.1");}
  if(tick==32){flash(origin,Color.fromRGB(255,238,155));cue(origin,Sound.BLOCK_BELL_RESONATE,2,.5f);trace("time-stop flash (cosmetic)");}
  if(tick>=40&&tick<70){double p=(tick-40)/30.0,h=17.74375*Math.sin(p*Math.PI/2);if(!movePilot(at(-.55*p,7.1*p,h)))return;locate(stand,at(1-.45*p,7.15*p,h-.1*p));}
  if(tick==70){main=model("motionrp_road_roller",at(0,8,16),1.5f,"road_roller");main.update(TrackerUpdateAction.viewRange(4));cue(origin,Sound.ENTITY_IRON_GOLEM_REPAIR,2,.6f);}
  if(tick>=70&&tick<190){double h=EpicTimeline.rollerHeight(tick),crush=Math.clamp((tick-104)/72.0,0,1);locate(main,at(0,8,h));locate(stand,at(.55,7.15,h+1.59375-.25*crush-.1));if(!movePilot(at(-.55,7.1,h+1.59375-.25*crush)))return;}
  if(tick==104){blast(at(0,8,.3),4);stand.stopAnimation("world_idle");stand.animate("barrage",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);main.animate("crumple",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);trace("roller hits ground; synchronized player/World barrage and dent begin");}
  if(tick>=104&&tick<176&&tick%2==0){var p=palm();particle(p,Particle.CRIT,16,.25,.08);particle(p,Particle.ELECTRIC_SPARK,12,.2,.08);var q=at(.6,7.6,2.3-.3*Math.clamp((tick-104)/72.0,0,1));particle(q,Particle.CRIT,16,.25,.08);cue(p,Sound.ENTITY_PLAYER_ATTACK_STRONG,.9f,1.3f);if(tick%18==14)trace("roller dent stage="+((tick-104)/18+1));}
  if(tick==176){var p=at(0,8,.5);blast(p,12);particle(p,Particle.DUST_PLUME,180,6,.2);particle(p,Particle.CLOUD,220,7,.22);particle(p,Particle.FLAME,100,4,.15);particle(p,Particle.ELECTRIC_SPARK,100,4,.2);particle(p,Particle.LAVA,35,3,.05);cue(p,Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,4,.5f);cue(p,Sound.ENTITY_BREEZE_WIND_BURST,4,.5f);for(int i=0;i<16;i++)parts[i]=model("motionrp_roller_debris",p,1,"roller_debris"+i);trace("final road roller blast with metal debris");}
  if(tick>=176&&tick<208){double t=(tick-176)/32.0;for(int i=0;i<16;i++){double a=i*2.399963,d=(3+i%5)*t;locate(parts[i],at(Math.cos(a)*d,8+Math.sin(a)*d,.6+10*t*(1-t)));}}
  if(tick==190){pilot.close();trace("pilot returned");}
  if(tick==208){for(int i=0;i<16;i++){audience.hide(parts[i]);parts[i]=null;}}
  if(tick==216){audience.hide(main);main=null;audience.hide(stand);stand=null;}
 }
 private Location dragonPoint(double d){return at(Math.sin(d*.35)*Math.min(1.6,d*.2),d,1.8+Math.sin(d*.5)*.7);}
 private void dragon(){
  if(tick>=16&&tick<50){dust(palm(),BLACK,5,.12);dust(palm(),PURPLE,4,.13);particle(palm(),Particle.SOUL_FIRE_FLAME,2,.1,.01);if(tick==16)cue(palm(),Sound.ENTITY_WARDEN_SONIC_CHARGE,1,.6f);}
  if(tick==50){stopCue(Sound.ENTITY_WARDEN_SONIC_CHARGE);main=model("motionrp_black_dragon_head",dragonPoint(0),2.8f,"dragon_head");for(int i=0;i<18;i++)parts[i]=model("motionrp_black_dragon_body",dragonPoint(0),(float)(1.8-i*.045),"dragon_segment"+i);cue(origin,Sound.ENTITY_ENDER_DRAGON_GROWL,2,.7f);}
  if(tick>=50&&tick<96){double distance=EpicTimeline.dragonDistance(tick);for(int i=-1;i<18;i++){double d=Math.max(0,distance-(i+1)*1.15);Location p=dragonPoint(d);p.setDirection(dragonPoint(d+.1).toVector().subtract(p.toVector()));var t=i<0?main:parts[i];locate(t,p);TreasuryScene.point(t,p);if(i%3==0){dust(p,BLACK,3,.3);dust(p,PURPLE,2,.2);particle(p,Particle.SOUL_FIRE_FLAME,1,.15,.03);}}}
  if(tick==96){audience.hide(main);main=null;for(int i=0;i<18;i++){audience.hide(parts[i]);parts[i]=null;}var p=at(0,36,.8);blast(p,5);dust(p,PURPLE,140,4);particle(p,Particle.SOUL_FIRE_FLAME,100,3,.15);trace("black dragon impact");}
 }
 private void myoo(){
  if(tick==16){main=model("motionrp_tengen_myoo",at(0,-7,0),.1f,"tengen_myoo");cue(origin,Sound.ENTITY_WITHER_SPAWN,1,.5f);}
  if(main!=null){float scale=(float)(.1+13.9*Math.clamp((tick-16)/38.0,0,1));if(tick>=196)scale*=Math.max(.01,(220-tick)/24.0);main.scaler(ModelScaler.value(scale));}
  if(tick==54){main.animate("slam",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);trace("giant mirrors sword windup");}
  if(tick==124){cue(origin,Sound.ENTITY_PLAYER_ATTACK_SWEEP,4,.5f);cue(origin,Sound.ENTITY_IRON_GOLEM_ATTACK,4,.5f);}
  if(tick>=124&&tick<=148&&tick%3==1){double d=12+(tick-124)*1.6;for(int j=-2;j<=2;j++){Location p=ground(at(j*3,d,0));if(p!=null){particle(p,Particle.EXPLOSION_EMITTER,1,.4,0);particle(p,Particle.DUST_PLUME,16,2,.1);particle(p,Particle.GUST,3,1,.1);}}if(tick==124){cue(at(0,20,0),Sound.ENTITY_GENERIC_EXPLODE,6,.5f);trace("myoo sword impact and ground shockwave");}}
 }
 private Location rockStart(int i){double a=i*2.399963,r=6+(i%7)*1.6;return at(Math.cos(a)*r,18+Math.sin(a)*r,.4);}
 private void gravity(){
  Location core=at(0,18,22);
  if(tick==28){main=model("motionrp_gravity_core",at(0,2,2),.5f,"gravity_core");cue(origin,Sound.BLOCK_BEACON_POWER_SELECT,1,.5f);}
  if(main!=null){double p=Math.clamp((tick-28)/40.0,0,1);locate(main,at(0,2+16*p,2+20*p));}
  if(tick==68){moon=model("motionrp_rock_moon",core,.05f,"rock_moon");moon.update(TrackerUpdateAction.viewRange(4));cue(core,Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,3,.5f);}
  double fade=tick<236?1:Math.clamp((260-tick)/24.0,0,1),radius=Math.max(.3,EpicTimeline.moonScale(tick));
  if(moon!=null)moon.scaler(ModelScaler.value((float)(Math.max(.05,radius)*fade)));
  for(int i=0;i<96;i++){
   int start=56+i,age=tick-start;
   if(age==0){parts[i]=model("motionrp_gravity_rock",rockStart(i),.8f+(i%4)*.3f,"attracted_rock"+i);parts[i].update(TrackerUpdateAction.viewRange(4));particle(rockStart(i),Particle.DUST_PLUME,8,1,.06);}
   if(parts[i]!=null){
    var normal=dev.aserver.motionrp.core.EpicTimeline.rockNormal(i);Location dest=core.clone().add(normal[0]*(radius*1.06+.2)*fade,normal[1]*(radius*1.06+.2)*fade,normal[2]*(radius*1.06+.2)*fade);
    double p=Math.clamp(age/42.0,0,1);Location from=rockStart(i);Location point=from.clone().add(dest.toVector().subtract(from.toVector()).multiply(p*p));
    if(age<42){double swirl=Math.sin(Math.PI*p)*2.5;point.add(Math.cos(i+p*6)*swirl,0,Math.sin(i+p*6)*swirl);}else if(age==42)trace("rock retained on shell index="+i);
    locate(parts[i],point);parts[i].scaler(ModelScaler.value((float)((.8+(i%4)*.3)*fade)));
   }
  }
  if(tick>=68&&tick<200&&tick%4==0){dust(core,PURPLE,10,2);particle(core,Particle.SMOKE,6,1.5,.05);if(tick%16==0)cue(core,Sound.BLOCK_STONE_BREAK,2,.6f);}
  if(tick==200){audience.hide(main);main=null;trace("gravity moon formed; 96 rocks retained on surface");cue(core,Sound.ENTITY_ELDER_GUARDIAN_CURSE,2,.5f);}
 }
}
