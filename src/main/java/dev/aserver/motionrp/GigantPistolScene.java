package dev.aserver.motionrp;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import org.bukkit.*;
import org.bukkit.entity.Player;
/** Cosmetic bone-inflated punch. No terrain or entity damage. */
final class GigantPistolScene extends SummonScene {
 private final EntityTracker body;private DummyTracker fist;
 GigantPistolScene(MotionPlugin plugin,Player owner,EntityTracker body){super(plugin,owner,"GigantPistol",150,72,true);this.body=body;body.rotation(()->new ModelRotation(0,origin.getYaw()));}
 @Override String phase(){return tick<12?"BITE":tick<48?"INFLATE":tick<64?"WINDUP":tick<74?"PUNCH":tick<100?"EXTENDED":"DEFLATE";}
 private double smooth(double a,double b){double q=Math.clamp((tick-a)/(b-a),0,1);return q*q*(3-2*q);}
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  if(tick==18){fist=model("motionrp_gigant_fist",at(.45,.3,1.3),.15f,"inflated_fist");fist.update(TrackerUpdateAction.moveDuration(1));sound(origin,Sound.ENTITY_BREEZE_CHARGE,1,.6f);}
  if(fist!=null){
   double inflate=smooth(18,48),shoot=smooth(64,74),shrink=1-smooth(104,134);float scale=(float)((.15+inflate*2.85)*shrink);
   fist.scaler(ModelScaler.value(Math.max(.01f,scale)));fist.location(BukkitAdapter.adapt(at(.6+inflate,1.2-1.8*smooth(48,64)+16*shoot,1.5)));
   fist.rotation(()->new ModelRotation(0,origin.getYaw()));fist.forceUpdate(true);
   body.update(TrackerUpdateAction.togglePart(tick>=134),b->java.util.Set.of("pra_right_arm","prfa_right_forearm").contains(b.name().rawName()));
   if(tick>=18&&tick<48&&tick%4==0)particle(at(.6,.5,1.5),Particle.CLOUD,5,.3,.02);
   if(tick>=65&&tick<=76&&tick%2==0)particle(at(1.6,16*shoot,1.5),Particle.SWEEP_ATTACK,2,1,0);
   if(tick==76){projectileImpact(at(1.6,20.5,1.5),true,2);particle(at(1.6,20.5,1.5),Particle.GUST,12,2,.1);sound(origin,Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,2,.6f);trace("giant fist impact");}
   if(tick==134){audience.hide(fist);fist=null;}
  }
 }
}
