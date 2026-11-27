package dev.aserver.motionrp;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
/** The actor's actual skin arm inflates and stretches; the shoulder remains attached. */
final class GigantPistolScene extends SummonScene {
 private final EntityTracker body;
 GigantPistolScene(MotionPlugin plugin,Player owner,EntityTracker body){super(plugin,owner,"GigantPistol",150,72,true);this.body=body;body.rotation(()->new ModelRotation(0,origin.getYaw()));}
 @Override String phase(){return tick<12?"BITE":tick<48?"INFLATE":tick<64?"WINDUP":tick<74?"PUNCH":tick<100?"EXTENDED":"DEFLATE";}
 private static final double[][] PATH=readPath();
 private static double[][] readPath(){try(var in=GigantPistolScene.class.getResourceAsStream("/bettermodel/pistol-palm.csv")){return new String(java.util.Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().map(l->java.util.Arrays.stream(l.split(",")).mapToDouble(Double::parseDouble).toArray()).toArray(double[][]::new);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
 private double[] frame(){return PATH[Math.clamp(tick-4,0,PATH.length-1)];}
 private Location palm(){var p=frame();return at(p[0],p[1],p[2]);}
 @Override void update(){
  body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
  if(tick==18)sound(origin,Sound.ENTITY_BREEZE_CHARGE,1,.6f);
  if(tick>=18&&tick<48&&tick%4==0)particle(palm(),Particle.CLOUD,5,.35,.02);
  if(tick>=65&&tick<=78&&tick%2==0)particle(palm(),Particle.SWEEP_ATTACK,2,1,0);
  if(tick==80){var point=palm();projectileImpact(point,true,2);particle(point,Particle.GUST,12,2,.1);sound(origin,Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,2,.6f);trace("giant fist impact");}
  if(tick==0)trace("skin bones upper="+body.bone("right_arm").getDisplay().id()+" forearm="+body.bone("right_forearm").getDisplay().id());
  if(tick==8||tick==60||tick==84){var p=frame();trace("skin arm tick="+tick+" width="+p[3]+" reach="+p[4]);}
 }
}
