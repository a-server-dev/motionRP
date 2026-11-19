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
  if(left&&tick==CONTACT){for(Player p:audience.players())p.spawnParticle(Particle.FLASH,center.clone().add(0,1.28,0),1,0,0,0,0,Color.fromRGB(255,251,192));particle(center.clone().add(0,1,0),Particle.END_ROD,90,1.5,.12);sound(center,Sound.BLOCK_BEACON_ACTIVATE,2,1.3f);trace("fingertips meet; two-player flash");}
  if(tick==FUSED){body.update(TrackerUpdateAction.togglePart(false));if(left){warrior=model("motionrp_fusion_warrior",center,1.0f,"fusion_warrior");loop(warrior,"fusion_idle");particle(center.clone().add(0,1,0),Particle.CLOUD,60,1.4,.06);trace("temporary fused warrior revealed");}}
  if(tick>=FUSED)body.update(TrackerUpdateAction.togglePart(false));
  if(left&&tick>=FUSED&&tick<FADE&&tick%4==0){var dust=new Particle.DustOptions(Color.fromRGB(255,225,91),1.1f);dust(center.clone().add(0,1,0),dust,8,.9);}
  if(tick==FADE){audience.hide(warrior);warrior=null;if(left)particle(center.clone().add(0,1,0),Particle.CLOUD,50,1.1,.05); }
 }
}
