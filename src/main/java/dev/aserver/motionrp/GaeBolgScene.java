package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import kr.toxicity.model.api.animation.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import static dev.aserver.motionrp.core.GaeBolgTimeline.*;

/** Physical run/jump with one thrown and returning spear; no energy-beam trackers. */
final class GaeBolgScene extends SummonScene {
    private final EntityTracker body;private final AerialAnchor travel;
    private DummyTracker spear;private Location landing,launch,impact,gripImpact,returnEnd;
    private static final Particle.DustOptions RED=new Particle.DustOptions(Color.fromRGB(209,25,41),1.2f);
    GaeBolgScene(MotionPlugin plugin,Player owner,EntityTracker body) {
        super(plugin,owner,"GaeBolg",END);this.body=body;travel=new AerialAnchor(plugin,owner);
        body.rotation(()->new ModelRotation(0,origin.getYaw()));
        body.update(TrackerUpdateAction.moveDuration(0));
    }
    @Override String phase() {return GaeBolgTimeline.phase(tick);}
    @Override public boolean ownsMove(Location from,Location to) {return travel.owns(from,to);}
    private Location hand(int frame) {
        var p=NobleHandPath.at(false,frame);
        return at(p.right(),p.forward(),p.height()).add(owner.getLocation().toVector().subtract(origin.toVector()));
    }
    @Override void update() {
        body.update(TrackerUpdateAction.togglePart(false),bone->java.util.Set.of("pri_right_item","pli_left_item").contains(bone.name().rawName()));
        if(tick>=DASH && tick<=LAND && !moveActor()) {plugin.playback().stop(owner.getUniqueId());return;}
        if(tick==DRAW) {
            spear=model(SummonAssets.GAE_BOLG,hand(tick),.7f,"weapon");
            spear.update(TrackerUpdateAction.moveDuration(0));
            spear.animate("throw_sequence",AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
        }
        if(spear==null)return;
        if(tick<THROW || tick>=CATCH) {
            Location palm=hand(tick);spear.location(BukkitAdapter.adapt(palm));spear.forceUpdate(true);
            spear.rotation(()->new ModelRotation(0,origin.getYaw()));
        }
        if(tick==DASH) sound(origin,Sound.ENTITY_BREEZE_SHOOT,1.2f,1.3f);
        if(tick==DASH_END) {trace("dash complete distance="+owner.getLocation().distance(origin));sound(owner.getLocation(),Sound.ENTITY_BREEZE_WIND_BURST,1.4f,1.2f);}
        if(tick==APEX) trace("apex height="+(owner.getY()-landing.getY()));
        if(tick==THROW) {
            launch=hand(tick);Location direction=origin.clone();direction.setPitch((float)PITCH);
            var hit=origin.getWorld().rayTraceBlocks(launch,direction.getDirection(),64,FluidCollisionMode.NEVER,true);
            if(hit==null) {plugin.tell(owner,"投擲先の地面が見つからなかったため終了します。");plugin.playback().stop(owner.getUniqueId());return;}
            impact=hit.getHitPosition().toLocation(origin.getWorld());
            gripImpact=impact.clone().subtract(direction.getDirection().multiply(TIP_LENGTH));trace("throw angle=120.0 pitch=30.0 fromY="+launch.getY()+" targetY="+impact.getY());
            sound(launch,Sound.ITEM_TRIDENT_THROW,1.7f,.7f);
        }
        if(tick>=THROW && tick<=IMPACT) {
            double t=(tick-THROW)/(double)(IMPACT-THROW);Location at=launch.clone().add(gripImpact.toVector().subtract(launch.toVector()).multiply(t));
            point(at,impact.toVector().subtract(launch.toVector()));
            if(tick%2==0) dust(at,RED,3,.07);
        }
        if(tick==IMPACT) {
            spear.update(TrackerUpdateAction.togglePart(false));
            particle(impact,Particle.EXPLOSION_EMITTER,12,3,0);particle(impact,Particle.DUST_PLUME,90,5,.08);particle(impact,Particle.CLOUD,120,5,.1);
            for(Player p:audience.players()) {p.spawnParticle(Particle.FLASH,impact,1,0,0,0,0,Color.fromRGB(255,225,211));p.spawnParticle(Particle.BLOCK,impact,180,4,1.5,4,.07,Material.SAND.createBlockData());}
            sound(impact,Sound.ENTITY_GENERIC_EXPLODE,4,.45f);sound(impact,Sound.ENTITY_DRAGON_FIREBALL_EXPLODE,3,.55f);trace("impact spear explosion tick="+tick);
        }
        if(tick==LAND) {
            travel.finishAtCurrent();trace("landed forward="+GaeBolgTimeline.distance(tick));sound(owner.getLocation(),Sound.ENTITY_IRON_GOLEM_STEP,1,.7f);
        }
        if(tick==RETURN) {spear.update(TrackerUpdateAction.togglePart(true));returnEnd=hand(CATCH);sound(impact,Sound.ITEM_TRIDENT_RETURN,2,1);}
        if(tick>=RETURN && tick<CATCH) {
            double t=(tick-RETURN)/(double)(CATCH-RETURN);Vector line=returnEnd.toVector().subtract(gripImpact.toVector());
            Location at=gripImpact.clone().add(line.clone().multiply(t)).add(0,12*t*(1-t),0);
            // The butt returns first so the sharp point stays away from the catcher.
            Vector reverse=line.clone().multiply(-1).add(new Vector(0,-12*(1-2*t),0));point(at,reverse);
            if(tick%2==0)dust(at,RED,2,.04);
        }
        if(tick==CATCH) {trace("spear caught tick="+tick);sound(hand(tick),Sound.ITEM_TRIDENT_HIT,1,1.4f);}
        if(tick==FADE) {audience.hide(spear);spear=null;}
    }
    private void point(Location at,Vector direction) {at.setDirection(direction);spear.location(BukkitAdapter.adapt(at));TreasuryScene.point(spear,at);}
    private boolean moveActor() {
        Location desired=at(0,GaeBolgTimeline.distance(tick),0);
        if(tick<=DASH_END) {
            Location floor=ground(desired);if(floor==null || Math.abs(floor.getY()-.12-owner.getY())>.65)return false;
            desired.setY(floor.getY()-.12);if(tick==DASH_END)landing=desired.clone();
        } else desired.setY(landing.getY()+GaeBolgTimeline.height(tick));
        Location from=owner.getLocation();Vector delta=desired.toVector().subtract(from.toVector());int steps=Math.max(1,(int)Math.ceil(delta.length()/.2));
        for(int i=1;i<=steps;i++) {
            Vector shift=delta.clone().multiply(i/(double)steps);Location p=from.clone().add(shift);
            if(!p.getWorld().isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4) || !p.getWorld().getWorldBorder().isInside(p)
                    || owner.wouldCollideUsing(owner.getBoundingBox().shift(shift)))return false;
            if(tick<=DASH_END) {Location g=ground(p);if(g==null || Math.abs(g.getY()-.12-p.getY())>.65)return false;}
        }
        desired.setYaw(owner.getYaw());desired.setPitch(owner.getPitch());
        return travel.moveTo(desired,origin);
    }
    @Override void onClose() {travel.close();owner.setFallDistance(0);}
}
