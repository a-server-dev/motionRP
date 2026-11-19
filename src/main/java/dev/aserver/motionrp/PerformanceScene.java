package dev.aserver.motionrp;

import dev.aserver.motionrp.core.Performance;
import dev.aserver.motionrp.core.PerformanceTravel;
import org.bukkit.util.Vector;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;

/** Celebrations with owned, collision-checked travel and a recovery journal. */
final class PerformanceScene extends SummonScene {
    private final EntityTracker body;private final Performance performance;
    private final AerialAnchor travel;private Location takeoff;
    PerformanceScene(MotionPlugin plugin,Player owner,EntityTracker body,Performance performance){
        super(plugin,owner,"Performance-"+performance.id(),performance.ticks());this.body=body;this.performance=performance;travel=new AerialAnchor(plugin,owner);
        body.rotation(()->new ModelRotation(0,origin.getYaw()));
        body.update(TrackerUpdateAction.moveDuration(0));
    }
    @Override String phase(){return tick<performance.highlight()?"PREPARE":tick<performance.ticks()-(performance==Performance.KAZU?10:20)?"CELEBRATE":"RECOVER";}
    @Override public boolean ownsMove(Location from,Location to){return travel.owns(from,to);}
    @Override void onClose(){travel.close();}
    @Override void update(){
        if(PerformanceTravel.end(performance)>0 && tick<=PerformanceTravel.end(performance) && !moveActor()){
            plugin.tell(owner,"進行先に障害物や段差があるため、パフォーマンスを終了しました。");
            plugin.playback().stop(owner.getUniqueId());return;
        }
        if(performance==Performance.KNEESLIDE && tick>=20 && tick<=50 && tick%3==0){
            Location floor=owner.getLocation().clone().subtract(0,.1,0);var block=floor.getBlock();
            if(block.getType().isSolid())for(Player viewer:audience.players())viewer.spawnParticle(Particle.BLOCK,owner.getLocation(),5,.2,.05,.2,.02,block.getBlockData());
        }
        body.update(TrackerUpdateAction.togglePart(false),b->java.util.Set.of("pri_right_item","pli_left_item").contains(b.name().rawName()));
        if(tick==performance.highlight() || (performance==Performance.YAMAKAWA && tick==52)){
            if(performance==Performance.RONALDO || performance==Performance.MBAPPE || performance==Performance.YAMAKAWA){particle(owner.getLocation(),Particle.CLOUD,6,.2,.02);sound(owner.getLocation(),Sound.ENTITY_PLAYER_SMALL_FALL,.65f,.9f);}
            else sound(origin,Sound.ENTITY_PLAYER_ATTACK_NODAMAGE,.5f,1.2f);
            trace("signature tick="+tick);
        }
    }
    private boolean moveActor(){
        Location desired=at(0,PerformanceTravel.distance(performance,tick),0);
        boolean airborne=performance==Performance.RONALDO && tick>24 && tick<46;
        Location floor=ground(desired);
        if(airborne){
            if(takeoff==null)return false;
            desired.setY(takeoff.getY()+PerformanceTravel.height(performance,tick));
        }else{
            if(floor==null || Math.abs(floor.getY()-.12-owner.getY())>.65)return false;
            desired.setY(floor.getY()-.12);
            if(performance==Performance.RONALDO && tick==24)takeoff=desired.clone();
        }
        Location from=owner.getLocation();Vector delta=desired.toVector().subtract(from.toVector());
        int steps=Math.max(1,(int)Math.ceil(delta.length()/.15));
        for(int i=1;i<=steps;i++){
            Vector shift=delta.clone().multiply(i/(double)steps);Location point=from.clone().add(shift);
            if(!point.getWorld().isChunkLoaded(point.getBlockX()>>4,point.getBlockZ()>>4)
                || !point.getWorld().getWorldBorder().isInside(point) || owner.wouldCollideUsing(owner.getBoundingBox().shift(shift)))return false;
            if(!airborne && !(performance==Performance.RONALDO && tick==46)){
                Location g=ground(point);if(g==null || Math.abs(g.getY()-.12-point.getY())>.65)return false;
            }
        }
        desired.setYaw(origin.getYaw()+(float)PerformanceTravel.yaw(performance,tick));desired.setPitch(owner.getPitch());
        if(!travel.moveTo(desired,origin))return false;
        if(tick==PerformanceTravel.end(performance)){
            travel.finishAtCurrent();trace("travel complete distance="+PerformanceTravel.distance(performance,tick)+" yaw="+PerformanceTravel.yaw(performance,tick));
        }
        if(performance==Performance.RONALDO && tick==35)trace("jump height="+(owner.getY()-takeoff.getY()));
        return true;
    }

}
