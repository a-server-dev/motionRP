package dev.aserver.motionrp;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import static dev.aserver.motionrp.core.FusionTimeline.*;
final class FusionScene extends SummonScene {
 private final EntityTracker body;private final boolean left;private DummyTracker warrior;
 FusionScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean left){super(plugin,owner,left?"FusionLeft":"FusionRight",END);this.body=body;this.left=left;body.rotation(()->new ModelRotation(0,origin.getYaw()));}
 @Override String phase(){return tick<36?"ARMS":tick<80?"STEPS":tick<92?"KNEE":tick<CONTACT?"POINT":tick<FUSED?"CONTACT":tick<FADE?"FUSED":"FADE";}
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  var center=plugin.fusion().center(owner.getUniqueId());if(center==null){plugin.playback().stop(owner.getUniqueId());return;}
  if(left&&tick==CONTACT){
   particle(center.clone().add(0,1.28,0),Particle.FIREWORK,150,.7,.16);
   particle(center.clone().add(0,1.28,0),Particle.END_ROD,100,.8,.12);
   sound(center,Sound.BLOCK_BEACON_ACTIVATE,2,1.3f);
   sound(center,Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,1.3f,.8f);
   trace("fingertips meet; two-player flash");
  }
  if(left&&tick>=CONTACT&&tick<=FUSED+16)fusionLight(center);
  if(tick==FUSED){body.update(TrackerUpdateAction.togglePart(false));if(left){warrior=model("motionrp_fusion_warrior",center,1.0f,"fusion_warrior");loop(warrior,"fusion_idle");particle(center.clone().add(0,1,0),Particle.CLOUD,60,1.4,.06);trace("temporary fused warrior revealed");}}
  if(tick>=FUSED)body.update(TrackerUpdateAction.togglePart(false));
  if(left&&tick>=FUSED&&tick<FADE&&tick%4==0){
   dust(center.clone().add(0,1,0),new Particle.DustOptions(Color.fromRGB(255,225,91),1.3f),14,.8);
   particle(center.clone().add(0,1,0),Particle.FIREWORK,8,.7,.025);
   for(int i=0;i<8;i++){
    double angle=tick*.09+i*Math.PI/4;
    particle(center.clone().add(Math.cos(angle)*.85,.15+(tick+i*3)%36/18.0,Math.sin(angle)*.85),Particle.END_ROD,1,0,0);
   }
  }
  if(tick==FADE){audience.hide(warrior);warrior=null;if(left)particle(center.clone().add(0,1,0),Particle.CLOUD,50,1.1,.05); }
 }
 private void fusionLight(Location center){
  int elapsed=tick-CONTACT;
  if(elapsed==0||elapsed==4||tick==FUSED||tick==FUSED+6){
   for(Player p:audience.players())p.spawnParticle(Particle.FLASH,center.clone().add(0,1.28,0),1,0,0,0,0,Color.fromRGB(255,253,230));
  }
  if(elapsed%2!=0)return;
  double radius=.35+elapsed*.12;
  var gold=new Particle.DustOptions(Color.fromRGB(255,235,125),1.6f);
  for(int i=0;i<24;i++){
   double angle=i*Math.PI/12+elapsed*.08;
   var ring=center.clone().add(Math.cos(angle)*radius,1.05+Math.sin(angle*2)*.3,Math.sin(angle)*radius);
   dust(ring,gold,1,.025);
   particle(ring,Particle.FIREWORK,1,.025,.025);
  }
  particle(center.clone().add(0,1.25,0),Particle.END_ROD,12,.6,.035);
 }
}
