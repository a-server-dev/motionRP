package dev.aserver.motionrp;

import dev.aserver.motionrp.core.FinalBattleTimeline;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.AnimationModifier;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;
import static dev.aserver.motionrp.core.FinalBattleTimeline.*;

/** Two consenting participants, one shared clock; the Naruto role owns the large VFX. */
final class FinalBattleScene extends SummonScene {
 private final EntityTracker body;private final boolean naruto;private boolean focused;
 private DummyTracker fox,susanoo,gold,dark,arrow,collision;private final DummyTracker[] clones=new DummyTracker[2],chakra=new DummyTracker[9];
 private final Map<Sound,Set<Player>> heard=new HashMap<>();
 private static final Particle.DustOptions GOLD=new Particle.DustOptions(Color.fromRGB(255,206,52),2),VIOLET=new Particle.DustOptions(Color.fromRGB(147,100,232),2),WHITE=new Particle.DustOptions(Color.fromRGB(230,253,255),2);
 FinalBattleScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean naruto){super(plugin,owner,naruto?"FinalBattleNaruto":"FinalBattleSasuke",END,256,false);this.body=body;this.naruto=naruto;body.rotation(()->new ModelRotation(0,owner.getLocation().getYaw()));}
 @Override String phase(){return FinalBattleTimeline.phase(tick);}
 @Override public boolean ownsMove(Location a,Location b){return plugin.fusion().ownsMove(owner.getUniqueId(),a,b);}
 @Override void onClose(){for(var e:heard.entrySet())for(Player p:e.getValue())if(p.isOnline())p.stopSound(e.getKey(),SoundCategory.PLAYERS);heard.clear();}
 private void cue(Location p,Sound s,float volume,float pitch){heard.computeIfAbsent(s,k->new HashSet<>()).addAll(audience.players());sound(p,s,volume,pitch);}
 private Location center(){return plugin.fusion().center(owner.getUniqueId());}
 private Location point(double right,double forward,double up){var c=center();double yaw=Math.toRadians(c.getYaw());return c.add(-Math.cos(yaw)*right-Math.sin(yaw)*forward,up,-Math.sin(yaw)*right+Math.cos(yaw)*forward);}
 private double aim(){return 19.2*Math.clamp((tick-CHARGE)/40.0,0,1);}
 private Location avatar(boolean n,double height,double distance){var p=FusionCoordinator.battlePosition(center(),n,height,distance);if(!n)p.setYaw((float)(p.getYaw()+aim()));return p;}
 private Location bowTail(){double a=Math.toRadians(aim());return point(9*Math.cos(a),58-9*Math.sin(a),60.5);}
 private DummyTracker cinematic(String id,Location p,float scale,String kind){var t=model(id,p,scale,kind);t.update(TrackerUpdateAction.viewRange(4));return t;}
 private void locate(DummyTracker t,Location p){if(t!=null){t.location(BukkitAdapter.adapt(p));t.rotation(()->new ModelRotation(p.getPitch(),p.getYaw()));t.forceUpdate(true);}}
 private void flash(Location p){for(Player v:audience.players())v.spawnParticle(Particle.FLASH,p,1,0,0,0,0,Color.fromRGB(241,250,255),true);}
 private void pose(DummyTracker t,String clip){if(t!=null)t.animate(clip,AnimationModifier.DEFAULT_WITH_PLAY_ONCE);}
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  if(center()==null){plugin.playback().stop(owner.getUniqueId());return;}
  if(!focused){audience.focus(center()).include(plugin.fusion().partner(owner.getUniqueId()));focused=true;}
  double scale=form(tick),lift=FinalBattleTimeline.lift(tick),distance=FinalBattleTimeline.distance(tick),height=(naruto?44:49)*scale+lift;
  if(tick>=RETURN)height=((naruto?44:49)+20)*groundReturn(tick);
  var seat=avatar(naruto,height,distance-(naruto?12:9)*scale);if(!plugin.fusion().moveBattle(owner.getUniqueId(),seat)){plugin.playback().stop(owner.getUniqueId());return;}
  if(tick<FORM&&tick%4==0)dust(owner.getLocation().clone().add(0,1,0),naruto?GOLD:VIOLET,12,.8);
  if(!naruto)return;
  Location n=avatar(true,lift,distance),s=avatar(false,lift,distance);
  if(tick==FORM){fox=cinematic("motionrp_duel_kurama",n,.05f,"kurama");susanoo=cinematic("motionrp_duel_susanoo",s,.05f,"sasuke_susanoo");loop(fox,"idle");loop(susanoo,"idle");cue(center(),Sound.ENTITY_ENDER_DRAGON_GROWL,3,.6f);}
  if(fox!=null){locate(fox,n);locate(susanoo,s);float v=(float)(.05+7.95*scale);if(tick>=AFTERMATH)v*=Math.max(.001,(RETURN-tick)/80.0);fox.scaler(ModelScaler.value(v));susanoo.scaler(ModelScaler.value(v));}
  if(tick==AVATARS){pose(fox,"exchange");pose(susanoo,"exchange");trace("Kurama and Susanoo trade giant blows");}
  if(tick==180){var p=point(0,0,32);flash(p);particle(p,Particle.GUST,35,12,.2);particle(p,Particle.ELECTRIC_SPARK,160,10,.2);cue(p,Sound.ENTITY_GENERIC_EXPLODE,5,.6f);}
  if(tick==ASCEND){for(int i=0;i<2;i++){clones[i]=cinematic("motionrp_duel_kurama",point(i==0?-28:28,-58,0),6,"kurama_clone"+i);loop(clones[i],"idle");}for(int i=0;i<9;i++){double a=i*Math.PI*2/9;chakra[i]=cinematic("motionrp_duel_chakra_spark",point(Math.cos(a)*36,58+Math.sin(a)*20,42+18*Math.sin(a*.7)),2.2f,"bijuu_chakra"+i);}trace("Kurama shadow clones and nine chakra streams appear");}
  if(tick>=ASCEND&&tick<ASURA){double p=(tick-ASCEND)/80.0;for(int i=0;i<2;i++){locate(clones[i],point((i==0?-28:28)*(1-p),-58,lift));clones[i].scaler(ModelScaler.value((float)(6*(1-p)+.01)));}}
  if(tick>=ASCEND&&tick<CHARGE){double p=Math.clamp((tick-ASCEND)/160.0,0,1);for(int i=0;i<9;i++){double a=i*Math.PI*2/9+p*5;var dst=point(Math.cos(a)*36*(1-p),58+Math.sin(a)*20*(1-p),42+18*Math.sin(a*.7)*(1-p)+lift);locate(chakra[i],dst);chakra[i].scaler(ModelScaler.value((float)(2.2*(1-p)+.05)));if(tick%5==0)dust(dst,VIOLET,8,.9);}}
  if(tick==ASURA){for(int i=0;i<2;i++){audience.hide(clones[i]);clones[i]=null;}audience.hide(fox);audience.hide(susanoo);fox=cinematic("motionrp_duel_asura_kurama",n,8,"asura_kurama");susanoo=cinematic("motionrp_duel_indra_susanoo",s,8,"indra_susanoo");loop(fox,"idle");loop(susanoo,"idle");flash(n.clone().add(0,40,0));flash(s.clone().add(0,40,0));trace("three-headed six-armed Kurama and Indra Susanoo formed");}
  if(tick==CHARGE){for(int i=0;i<9;i++){audience.hide(chakra[i]);chakra[i]=null;}pose(fox,"charge");susanoo.stopAnimation("idle");pose(susanoo,"charge");cue(s,Sound.ENTITY_WARDEN_SONIC_CHARGE,3,.5f);trace("Six Paths Ultra-Big Ball Rasenshuriken and Indra arrow charging");}
  if(tick==420){gold=cinematic("motionrp_duel_gold_rasenshuriken",point(-16,-50,82),.05f,"gold_rasenshuriken");dark=cinematic("motionrp_duel_dark_rasenshuriken",point(16,-50,82),.05f,"dark_rasenshuriken");arrow=cinematic("motionrp_duel_indra_arrow",bowTail(),.05f,"indra_arrow");loop(gold,"spin");loop(dark,"spin");loop(arrow,"surge");}
  if(tick>=420&&tick<RELEASE){float v=(float)Math.clamp((tick-420)/100.0,0.01,1);gold.scaler(ModelScaler.value(8*v));dark.scaler(ModelScaler.value(8*v));arrow.scaler(ModelScaler.value(5*v));var a=bowTail();a.setDirection(point(0,0,60).toVector().subtract(a.toVector()));locate(arrow,a);if(tick%4==0){dust(point(-16,-50,82),GOLD,12,5);dust(point(16,-50,82),VIOLET,12,5);particle(point(8.5,43,60),Particle.ELECTRIC_SPARK,35,6,.15);}}
  if(tick==RELEASE){cue(center(),Sound.ENTITY_LIGHTNING_BOLT_THUNDER,6,.65f);cue(center(),Sound.ENTITY_BREEZE_WIND_BURST,6,.5f);trace("both Rasenshuriken and Indra arrow released together");}
  if(tick>=RELEASE&&tick<COLLISION){double p=projectile(tick);locate(gold,point(-16*(1-p),-50*(1-p),82-22*p+6*Math.sin(Math.PI*p)));locate(dark,point(16*(1-p),-50*(1-p),82-22*p+6*Math.sin(Math.PI*p)));var a=point(8.5*(1-p),55.05*(1-p)+20*p,60.5-.5*p);a.setDirection(point(0,0,60).toVector().subtract(a.toVector()));locate(arrow,a);if(tick%2==0){particle(a,Particle.ELECTRIC_SPARK,25,3,.15);dust(point(0,-50*(1-p),82-22*p),WHITE,15,6);}}
  if(tick==COLLISION){audience.hide(gold);gold=null;audience.hide(dark);dark=null;audience.hide(arrow);arrow=null;var p=point(0,0,60);collision=cinematic("motionrp_duel_clash_orb",p,.1f,"chakra_collision");loop(collision,"surge");flash(p);particle(p,Particle.EXPLOSION_EMITTER,12,12,0);particle(p,Particle.CLOUD,240,18,.3);cue(p,Sound.ENTITY_GENERIC_EXPLODE,8,.5f);cue(p,Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,8,.5f);trace("Indra arrow and twin Rasenshuriken collide");}
  if(collision!=null){double p=Math.clamp((tick-COLLISION)/120.0,0,1);collision.scaler(ModelScaler.value((float)(2+34*Math.sin(Math.PI*p))));if(tick%4==0){double radius=6+42*p;for(int i=0;i<28;i++){double a=i*Math.PI*2/28+tick*.07;var q=point(Math.cos(a)*radius,Math.sin(a)*radius,60+5*Math.sin(a*3+tick*.1));particle(q,Particle.ELECTRIC_SPARK,2,.3,.08);particle(q,Particle.END_ROD,1,.4,.05);}if(tick%16==8)cue(point(0,0,60),Sound.ENTITY_BREEZE_WIND_BURST,4,.6f);}}
  if(tick==AFTERMATH){audience.hide(collision);collision=null;particle(point(0,0,2),Particle.DUST_PLUME,180,24,.2);trace("chakra storm dissipates; both participants return safely");}
  if(tick==RETURN){audience.hide(fox);fox=null;audience.hide(susanoo);susanoo=null;}
 }
}
