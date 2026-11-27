package dev.aserver.motionrp;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
/** Only the leader emits the shared crystal and light, both actors use the same clock. */
final class BalseScene extends SummonScene {
 private final EntityTracker body;private final boolean leader;private DummyTracker crystal,light;
 BalseScene(MotionPlugin plugin,Player owner,EntityTracker body,boolean leader){super(plugin,owner,"Balse",140,96,true);this.body=body;this.leader=leader;body.rotation(()->new ModelRotation(0,origin.getYaw()));}
 @Override String phase(){return tick<38?"HANDS":tick<64?"CHARGE":tick<104?"BALSE":"FADE";}
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  Location center=plugin.fusion().center(owner.getUniqueId());if(center==null){plugin.playback().stop(owner.getUniqueId());return;}center.add(0,1.35,0);
  double yaw=Math.toRadians(center.getYaw());center.add(-Math.sin(yaw)*.65,0,Math.cos(yaw)*.65);
  if(tick==54){var v=body.bone("balse_palm").worldPosition();var palm=owner.getLocation().add(v.x,v.y,v.z);trace("palm contact leader="+leader+" gap="+palm.distance(center));}
  if(!leader)return;
  if(tick==36){crystal=model("motionrp_balse_crystal",center,.7f,"flying_stone");sound(center,Sound.BLOCK_AMETHYST_BLOCK_RESONATE,1,1.2f);}
  if(tick>=38&&tick<64&&tick%3==0){dust(center,new Particle.DustOptions(Color.fromRGB(100,230,255),1.8f),18,.4);particle(center,Particle.END_ROD,12,.5,.015);}
  if(tick==64){light=model("motionrp_balse_light",center,.1f,"blinding_light");loop(light,"radiance");sound(center,Sound.BLOCK_BEACON_ACTIVATE,3,.5f);sound(center,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,2,1.5f);trace("shared flash released");}
  if(light!=null){double q=Math.clamp((tick-64)/18.,0,1),fade=1-Math.clamp((tick-104)/28.,0,1);light.scaler(ModelScaler.value((float)Math.max(.01,(.1+q*18)*fade)));light.forceUpdate(true);
   if(tick<=100&&tick%3==1){for(Player p:audience.players())p.spawnParticle(Particle.FLASH,center,1,0,0,0,0,Color.fromRGB(235,255,255));}
   if(tick<=108&&tick%2==0){particle(center,Particle.END_ROD,120,6,.35);dust(center,new Particle.DustOptions(Color.fromRGB(225,255,255),4),120,5);}
   if(tick<=108&&tick%2==0){
    particle(center,Particle.FIREWORK,220,6,.25);
    for(Player viewer:audience.players()){
     Location eye=viewer.getEyeLocation(),glare=eye.clone().add(eye.getDirection().multiply(1.2));
     viewer.spawnParticle(Particle.FIREWORK,glare,70,.65,.55,.65,.035,null,true);
     viewer.spawnParticle(Particle.DUST,glare,120,.6,.5,.6,0,new Particle.DustOptions(Color.WHITE,4),true);
     viewer.spawnParticle(Particle.END_ROD,glare,50,.7,.55,.7,.025,null,true);
     if(tick%4==0)viewer.spawnParticle(Particle.FLASH,glare,1,0,0,0,0,Color.WHITE,true);
    }
    if(tick==64||tick==80)sound(center,Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,2,.8f);
    if(tick==80)trace("whiteout: fireworks and eye-level white veil");
   }
   if(tick>=68&&tick<=104&&tick%3==0){double radius=(tick-64)*.4;for(int j=0;j<32;j++){double a=j*Math.PI/16;dust(center.clone().add(radius*Math.cos(a),0,radius*Math.sin(a)),new Particle.DustOptions(Color.fromRGB(165,245,255),2.5f),1,0);}}
  }
 }
}
