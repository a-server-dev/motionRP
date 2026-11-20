package dev.aserver.motionrp;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;
import static dev.aserver.motionrp.core.BrolyTimeline.*;
/** Cosmetic, consenting two-player grab/flight/rock impact. No terrain or health changes. */
final class BrolyScene extends SummonScene {
 private final EntityTracker body;private final boolean attacker;private boolean focused;private String clip="";
 private DummyTracker wall,crater;private final DummyTracker[] debris=new DummyTracker[14];
 private final Map<Sound,Set<Player>> heard=new HashMap<>();
 private static final Particle.DustOptions KI=new Particle.DustOptions(Color.fromRGB(131,240,53),1.2f),STONE=new Particle.DustOptions(Color.fromRGB(115,108,94),1.4f);
 BrolyScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean attacker){super(plugin,owner,attacker?"BrolyAttacker":"BrolyVictim",END,96,false);this.body=body;this.attacker=attacker;body.rotation(()->new ModelRotation(0,owner.getLocation().getYaw()));}
 @Override public boolean ownsMove(Location a,Location b){return plugin.fusion().ownsMove(owner.getUniqueId(),a,b);}
 @Override String phase(){var state=plugin.fusion().brolyState(owner.getUniqueId());return state==null?"FINISHED":dev.aserver.motionrp.core.BrolyTimeline.phase(tick,state.impactTick);}
 private void cue(Location p,Sound s,float volume,float pitch){heard.computeIfAbsent(s,k->new HashSet<>()).addAll(audience.players());sound(p,s,volume,pitch);}
 @Override void onClose(){for(var e:heard.entrySet())for(Player p:e.getValue())if(p.isOnline())p.stopSound(e.getKey(),SoundCategory.PLAYERS);}
 private void locate(DummyTracker t,Location p){t.location(BukkitAdapter.adapt(p));t.forceUpdate(true);}
 private void pose(String next){if(clip.equals(next))return;if(!clip.isEmpty())body.stopAnimation(clip);clip=next;body.animate(clip,AnimationModifier.builder().type(Set.of("flight","flee").contains(next)?AnimationIterator.Type.LOOP:AnimationIterator.Type.PLAY_ONCE).build());}
 private Location face(Location p,BrolyState state){var q=p.clone();q.setDirection(state.direction);return q;}
 @Override void update(){
  var state=plugin.fusion().brolyState(owner.getUniqueId());UUID partner=plugin.fusion().partner(owner.getUniqueId());if(state==null||partner==null){plugin.playback().stop(owner.getUniqueId());return;}
  if(!focused){audience.focus(state.origin).include(partner);focused=true;}
  body.update(TrackerUpdateAction.togglePart(false),b->Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  if(attacker){
   var target=Bukkit.getPlayer(partner);if(target==null){plugin.playback().stop(owner.getUniqueId());return;}
   if(tick==LAUNCH&&state.contact(state.victim,MAX_DISTANCE)==null){wall=model("motionrp_broly_wall",face(state.victim.clone().add(state.direction.clone().multiply(MAX_DISTANCE+.55)).add(0,1.3,0),state),3.4f,"fallback_rock_wall");trace("maximum-distance fallback rock wall prepared");}
   if(!state.advance(tick,owner,target)){plugin.tell(owner,state.failure);plugin.playback().stop(owner.getUniqueId());return;}
   if(!plugin.fusion().moveBroly(owner.getUniqueId(),state.attacker,state.victim)){plugin.tell(owner,"進行先に障害物があるため演出を終了しました。");plugin.playback().stop(owner.getUniqueId());return;}
   if(tick==TURN)trace("victim escaped from acceptance point distance="+state.victim.distance(state.victimStart)+"; turns back");
   if(tick==CHARGE){cue(state.attacker,Sound.ENTITY_BREEZE_WIND_BURST,2,.8f);trace("attacker rushes the turned victim");}
   if(tick>=CHARGE&&tick<GRAB&&tick%2==0){particle(state.attacker.clone().add(0,1,0),Particle.CLOUD,3,.25,.03);dust(state.attacker.clone().add(0,1,0),KI,3,.4);}
   if(tick==LAUNCH){cue(state.attacker,Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,2,.6f);cue(state.attacker,Sound.ENTITY_BREEZE_WIND_BURST,2,.7f);trace("consenting victim launched; swept block collision enabled");}
   if(tick>=LAUNCH&&state.impactTick<0&&tick%2==0){particle(state.victim.clone().add(0,1,0),Particle.CLOUD,4,.35,.02);particle(state.attacker.clone().add(0,1,0),Particle.END_ROD,2,.2,.02);dust(state.attacker.clone().add(0,1,0),KI,3,.5);}
   if(state.impactTick==tick){
    if(state.realBlock&&wall!=null){audience.hide(wall);wall=null;}
    if(!state.realBlock&&wall==null)wall=model("motionrp_broly_wall",face(state.impact,state),3.4f,"fallback_rock_wall");
    crater=model("motionrp_broly_crater",face(state.impact,state),2.0f,"impact_crater");
    for(int i=0;i<debris.length;i++)debris[i]=model("motionrp_gravity_rock",state.impact,.18f+(i%4)*.06f,"rock_fragment"+i);
    particle(state.impact,Particle.EXPLOSION,3,.7,0);particle(state.impact,Particle.GUST,12,1.4,.1);particle(state.impact,Particle.CLOUD,70,1.5,.2);particle(state.impact,Particle.DUST_PLUME,50,1.2,.12);
    cue(state.impact,Sound.ENTITY_GENERIC_EXPLODE,3,.6f);cue(state.impact,Sound.BLOCK_STONE_BREAK,3,.6f);trace("rock impact cause="+(state.realBlock?"BLOCK":"DISTANCE")+" distance="+state.distance+" victim="+state.victim.toVector());
   }
   if(state.impactTick>=0){int age=tick-state.impactTick;Vector right=new Vector(state.direction.getZ(),0,-state.direction.getX());
    for(int i=0;i<debris.length;i++)if(debris[i]!=null){double a=i*2.399963,t=age/20.0;var p=state.impact.clone().add(right.clone().multiply(Math.cos(a)*t*(2+i%3))).subtract(state.direction.clone().multiply(t*(1+i%4))).add(0,Math.sin(a)*t*2+3*t-2*t*t,0);locate(debris[i],p);final int index=i;debris[i].rotation(()->new ModelRotation(age*13+index*7,age*17));if(age>=30){audience.hide(debris[i]);debris[i]=null;}}
    if(age<18&&age%3==0)dust(state.impact,STONE,12,1.4);
    if(tick>=END-20){float fade=(float)Math.max(.01,(END-tick)/20.0);if(wall!=null)wall.scaler(ModelScaler.value(3.4f*fade));if(crater!=null)crater.scaler(ModelScaler.value(2*fade));}
   }
  }
  String next=state.impactTick>=0?(tick-state.impactTick<20?"impact":"recover"):tick>=LAUNCH?"flight":tick>=GRAB?"grapple":tick>=CHARGE?(attacker?"charge":"fright"):tick>=TURN?(attacker?"stand":"turn"):(attacker?"stand":state.fleeBlocked?"fright":"flee");pose(next);
 }
}
