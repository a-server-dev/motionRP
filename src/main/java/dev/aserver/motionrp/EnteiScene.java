package dev.aserver.motionrp;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
/** Great flame circle gathers into a held sun, then a bounded cosmetic throw. */
final class EnteiScene extends SummonScene {
 private final EntityTracker body;private DummyTracker sun;private Location center;
 EnteiScene(MotionPlugin plugin,Player owner,EntityTracker body){super(plugin,owner,"Entei",190,144,true);this.body=body;body.rotation(()->new ModelRotation(0,origin.getYaw()));}
 @Override String phase(){return tick<40?"GREAT_FLAME_CIRCLE":tick<90?"GATHER_SUN":tick<110?"HOLD":tick<124?"THROW":tick<158?"FLIGHT":"IMPACT";}
 private double smooth(int a,int b){double p=Math.clamp((tick-a)/(double)(b-a),0,1);return p*p*(3-2*p);}
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  if(tick==4)sound(origin,Sound.ENTITY_BLAZE_AMBIENT,1,.65f);
  if(tick<110&&tick%2==0){
   double radius=2+8*smooth(0,65);
   for(int ring=0;ring<2;ring++)for(int j=0;j<36;j++){
    double a=j*Math.PI/18+tick*.08+ring*.15,r=radius-ring*.7;Location p=at(Math.cos(a)*r,Math.sin(a)*r,.15+ring*.25);
    fx(p,Particle.FLAME,2,.12,.035);if(j%3==0)embers(p,new Particle.DustOptions(Color.fromRGB(255,108,8),1.8f),1,.12);
   }
   if(tick>=30&&tick<90)for(int j=0;j<20;j++){
    double h=j*.15,angle=tick*.17+j*.45,r=2.2*(1-j/22.);fx(at(r*Math.cos(angle),r*Math.sin(angle),h),Particle.FLAME,2,.1,.035);
   }
   if(tick%20==0)sound(origin,Sound.BLOCK_FIRE_AMBIENT,1,.7f);
  }
  if(tick==40){sun=model("motionrp_entei_sun",at(0,0,3),.5f,"held_sun");sun.update(TrackerUpdateAction.composite(TrackerUpdateAction.moveDuration(1),TrackerUpdateAction.viewRange(5)));loop(sun,"burn");}
  if(sun!=null){
   double radius=.5+12.5*smooth(40,90);center=at(0,0,2.5+radius);
   if(tick>=124){double q=smooth(124,158);center=at(0,64*q,15.5-2.5*q+7*Math.sin(q*Math.PI));}
   sun.scaler(ModelScaler.value((float)radius));sun.location(BukkitAdapter.adapt(center));sun.forceUpdate(true);
   if(tick%3==0){
    for(int j=0;j<32;j++){double a=j*Math.PI/16+tick*.08,p=(j%7-3)*.2;fx(center.clone().add(Math.cos(a)*radius*Math.cos(p),radius*Math.sin(p),Math.sin(a)*radius*Math.cos(p)),Particle.FLAME,3,.4,.06);}
    fx(center,Particle.LAVA,8,radius*.7,.01);
   }
   if(tick==90)trace("solar sphere ready radius=13.0 held above raised hand");
   if(tick==124){trace("sun thrown after windup");sound(origin,Sound.ENTITY_BLAZE_SHOOT,3,.5f);}
   if(tick==158){audience.hide(sun);sun=null;flash(center);fx(center,Particle.EXPLOSION_EMITTER,3,5,0);fx(center,Particle.FLAME,220,12,.25);fx(center,Particle.CLOUD,120,9,.18);sound(center,Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,5.5f,.5f);trace("sun impact; no terrain or entity damage");}
  }
  if(tick>=158&&tick<182&&tick%2==0){
   double r=3+(tick-158)*1.25;Location impact=at(0,64,.2);
   for(int j=0;j<48;j++){double a=j*Math.PI/24;Location p=impact.clone().add(r*Math.cos(a),.1,r*Math.sin(a));fx(p,Particle.FLAME,3,.45,.06);embers(p,new Particle.DustOptions(Color.fromRGB(255,175,30),3),2,.4);}
  }
 }
 private void fx(Location p,Particle type,int count,double spread,double speed){for(Player viewer:audience.players())viewer.spawnParticle(type,p,count,spread,spread,spread,speed,null,true);}
 private void embers(Location p,Particle.DustOptions color,int count,double spread){for(Player viewer:audience.players())viewer.spawnParticle(Particle.DUST,p,count,spread,spread,spread,0,color,true);}
 private void flash(Location p){for(Player viewer:audience.players())viewer.spawnParticle(Particle.FLASH,p,1,0,0,0,0,Color.fromRGB(255,220,145),true);}
}
