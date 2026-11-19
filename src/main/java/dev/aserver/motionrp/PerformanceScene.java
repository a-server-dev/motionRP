package dev.aserver.motionrp;

import dev.aserver.motionrp.core.Performance;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;

/** Small landing cues; movement is authored in the skin model and never teleports the player. */
final class PerformanceScene extends SummonScene {
    private final EntityTracker body;private final Performance performance;
    PerformanceScene(MotionPlugin plugin,Player owner,EntityTracker body,Performance performance){
        super(plugin,owner,"Performance-"+performance.id(),performance.ticks());this.body=body;this.performance=performance;
        body.rotation(()->new ModelRotation(0,origin.getYaw()));
    }
    @Override String phase(){return tick<performance.highlight()?"PREPARE":tick<performance.ticks()-20?"CELEBRATE":"RECOVER";}
    @Override void update(){
        body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
        if(tick==performance.highlight() || (performance==Performance.YAMAKAWA && tick==52)){
            if(performance==Performance.RONALDO || performance==Performance.MBAPPE || performance==Performance.YAMAKAWA){particle(origin,Particle.CLOUD,6,.2,.02);sound(origin,Sound.ENTITY_PLAYER_SMALL_FALL,.65f,.9f);}
            else sound(origin,Sound.ENTITY_PLAYER_ATTACK_NODAMAGE,.5f,1.2f);
            trace("signature tick="+tick);
        }
    }
}
