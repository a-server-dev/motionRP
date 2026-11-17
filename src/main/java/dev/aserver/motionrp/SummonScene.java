package dev.aserver.motionrp;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.util.Objects;
import java.util.logging.Level;

/** Bounded packet-only summons. No blocks, physical projectiles or combat. */
abstract class SummonScene implements MotionScene {
    static final TrackerModifier MODIFIER=TrackerModifier.builder().sightTrace(false).damageAnimation(false).damageTint(false).build();
    final MotionPlugin plugin;
    final Player owner;
    final SceneAudience audience;
    final Location origin;
    final String prefix;
    private final int duration;
    private BukkitTask task;
    int tick;
    private boolean closed;
    private String previous;

    SummonScene(MotionPlugin plugin,Player owner,String prefix,int duration) {
        this.plugin=plugin;this.owner=owner;this.prefix=prefix;this.duration=duration;
        audience=new SceneAudience(owner);origin=owner.getLocation().clone();origin.setPitch(0);
    }
    @Override public final void start() { task=Bukkit.getScheduler().runTaskTimer(plugin,this::advance,0,1); }
    @Override public final boolean ownsMove(Location from,Location to) { return false; }
    private void advance() {
        if(closed) return;
        try {
            if(!owner.isOnline() || owner.isDead() || owner.getWorld()!=origin.getWorld() || tick>=duration) {
                plugin.playback().stop(owner.getUniqueId());return;
            }
            if(tick%5==0) audience.refresh();
            update();
            String phase=phase();
            if(!phase.equals(previous)) { previous=phase;trace("phase: "+phase+" tick="+tick); }
            tick++;
        } catch(RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING,prefix+" scene failed",failure);
            plugin.playback().stop(owner.getUniqueId());
        }
    }
    abstract void update();
    abstract String phase();
    final DummyTracker model(String id,Location location,float scale,String kind) {
        var tracker=Objects.requireNonNull(BetterModel.modelOrNull(id),id).create(BukkitAdapter.adapt(location),MODIFIER,t -> {
            t.update(TrackerUpdateAction.brightness(15,15));t.scaler(ModelScaler.value(scale));
        });
        audience.show(tracker,plugin,prefix,kind);return tracker;
    }
    final void loop(DummyTracker tracker,String name) {
        tracker.animate(name,AnimationModifier.builder().type(AnimationIterator.Type.LOOP).build());
    }
    final Location at(double right,double forward,double up) {
        double yaw=Math.toRadians(origin.getYaw());
        return origin.clone().add(-Math.cos(yaw)*right-Math.sin(yaw)*forward,up,
                -Math.sin(yaw)*right+Math.cos(yaw)*forward);
    }
    final void sound(Location at,Sound sound,float volume,float pitch) {
        for(Player p:audience.players()) p.playSound(at,sound,SoundCategory.PLAYERS,volume,pitch);
    }
    final void dust(Location at,Particle.DustOptions dust,int count,double spread) {
        for(Player p:audience.players()) p.spawnParticle(Particle.DUST,at,count,spread,spread,spread,0,dust);
    }
    final void particle(Location at,Particle particle,int count,double spread,double speed) {
        for(Player p:audience.players()) p.spawnParticle(particle,at,count,spread,spread,spread,speed);
    }
    final Location ground(Location aim) {
        if(!aim.getWorld().isChunkLoaded(aim.getBlockX()>>4,aim.getBlockZ()>>4)) return null;
        var hit=aim.getWorld().rayTraceBlocks(aim.clone().add(0,2,0),new Vector(0,-1,0),8,FluidCollisionMode.NEVER,true);
        return hit==null ? null : hit.getHitPosition().toLocation(aim.getWorld()).add(0,.12,0);
    }
    final void trace(String message) {
        if(plugin.getConfig().getBoolean("debug-input",false)) plugin.getLogger().info(prefix+" "+message);
    }
    @Override public final void close() {
        if(closed) return;closed=true;
        if(task!=null) task.cancel();
        audience.close();
    }
}
