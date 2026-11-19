package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.util.Objects;
import java.util.logging.Level;
import static dev.aserver.motionrp.core.RasenshurikenTimeline.*;

/** Two assistants, overhead weapon, actual jump and packet-only thrown burst. */
final class RasenshurikenScene implements MotionScene {
    private static final TrackerModifier MODIFIER = TrackerModifier.builder()
            .sightTrace(false).damageAnimation(false).damageTint(false).build();
    private final MotionPlugin plugin;
    private final Player owner;
    private final String playerModel;
    private final SceneAudience audience;
    private final DummyTracker[] clones = new DummyTracker[2];
    private DummyTracker weapon;
    private BukkitTask task;
    private Location launch, throwFrom, target, projectile, movingFrom, movingTo, lastOwned;
    private int tick;
    private boolean closed, burst;
    private Phase lastPhase;

    RasenshurikenScene(MotionPlugin plugin, Player owner, String playerModel) {
        this.plugin=plugin;this.owner=owner;this.playerModel=playerModel;
        audience=new SceneAudience(owner).filtered(plugin.visibility());
    }
    @Override public void refreshAudience(){audience.refresh();}
    @Override public void start() { task=Bukkit.getScheduler().runTaskTimer(plugin, this::advance, 0, 1); }
    @Override public boolean ownsMove(Location from, Location to) {
        return movingFrom != null && movingFrom.equals(from) && movingTo.equals(to);
    }
    private void advance() {
        if (closed) return;
        try {
            if (!owner.isOnline() || owner.isDead() || tick >= END) { plugin.playback().stop(owner.getUniqueId()); return; }
            if (tick % 5 == 0) audience.refresh();
            if (tick >= STEP && tick < JUMP) {
                Location next=relative(owner.getLocation(), 0, .0875, 0);
                Location floor=ground(next);
                if (floor == null || Math.abs(floor.getY()-next.getY())>.1 || !move(next)) { plugin.playback().stop(owner.getUniqueId()); return; }
            }
            if (tick == JUMP) launch=owner.getLocation().clone();
            if (tick >= JUMP && tick <= LAND) {
                Location next=launch.clone().add(0, height(tick), 0);
                next.setYaw(owner.getLocation().getYaw()); next.setPitch(owner.getLocation().getPitch());
                if (!move(next)) { plugin.playback().stop(owner.getUniqueId()); return; }
                owner.setFallDistance(0);
            }
            if (closed) return;
            Location anchor=owner.getLocation();
            anchor.setPitch(0);
            var hand=RasenganHandPath.shurikenAt(tick);
            Location ball=relative(anchor, hand.right(), hand.forward(), hand.height());
            for (int i=0; i<2; i++) {
                Location assistant=relative(anchor, i==0 ? 1.1 : -1.1, .6, 0);
                Vector toward=ball.toVector().subtract(assistant.toVector());
                assistant.setYaw((float)Math.toDegrees(Math.atan2(-toward.getX(),toward.getZ())));
                assistant.setPitch(0);
                if (tick == CLONES) {
                    clones[i]=Objects.requireNonNull(BetterModel.limbOrNull(playerModel)).create(BukkitAdapter.adapt(assistant),
                            ModelProfile.of(BukkitAdapter.adapt(owner)), MODIFIER);
                    clones[i].animate("rasengan_helper", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
                    audience.show(clones[i], plugin, "clone"+(i+1));
                    smoke(assistant.clone().add(0,.9,0));
                }
                if (tick == RAISE && clones[i] != null) {
                    smoke(assistant.clone().add(0,.9,0)); audience.hide(clones[i]); clones[i]=null;
                }
                if (clones[i] != null) clones[i].location(BukkitAdapter.adapt(assistant));
            }
            if (tick == CLONES || tick == RAISE) sound(anchor,Sound.ENTITY_ILLUSIONER_MIRROR_MOVE,.6f,1.3f);
            if (tick == ORB) {
                weapon=Objects.requireNonNull(BetterModel.modelOrNull(BundledModels.SHURIKEN_ORB))
                        .create(BukkitAdapter.adapt(ball),MODIFIER,t -> {
                            t.update(TrackerUpdateAction.brightness(15,15));t.scaler(ModelScaler.value(.15f));
                        });
                weapon.animate("spin",AnimationModifier.builder().type(AnimationIterator.Type.LOOP).build());
                audience.show(weapon,plugin,"weapon");
            }
            if (tick >= ORB && tick < THROW) {
                float scale=(float)(.15+1.35*Math.clamp((tick-ORB)/(double)(RAISE-ORB),0,1));
                weapon.location(BukkitAdapter.adapt(ball));weapon.scaler(ModelScaler.value(scale));
                WindEffects.swirl(audience.players(),ball,tick, .8);
                WindEffects.swirl(audience.players(),anchor.clone().add(0,.8,0),tick,1.1);
                if (tick % 24 == 0) sound(ball,Sound.ENTITY_BREEZE_WHIRL,.3f,1.1f);
            }
            if (tick >= ORB && tick < RAISE && (tick-ORB)%12 == 0)
                sound(ball,Sound.ENTITY_BREEZE_CHARGE,.7f,.9f);
            if (tick == RAISE) stopChargeSound();
            if (tick == JUMP) sound(anchor,Sound.ENTITY_BREEZE_JUMP,.8f,1);
            if (tick == THROW) {
                throwFrom=ball.clone();projectile=ball.clone();
                Location aim=relative(launch,0,RANGE,0);
                var hit=ground(aim);
                if (hit == null) { plugin.playback().stop(owner.getUniqueId()); return; }
                target=hit.add(0,.15,0);
                sound(ball,Sound.ENTITY_BREEZE_SHOOT,1,.9f);
            }
            if (tick >= THROW && tick <= IMPACT && !burst) {
                double progress=(tick-THROW)/(double)(IMPACT-THROW);
                Location next=throwFrom.clone().add(target.toVector().subtract(throwFrom.toVector()).multiply(progress));
                next.add(0,.3*Math.sin(progress*Math.PI),0);
                Vector segment=next.toVector().subtract(projectile.toVector());
                var hit=segment.lengthSquared()<1e-10 ? null : next.getWorld().rayTraceBlocks(projectile,
                        segment.clone().normalize(),segment.length(),FluidCollisionMode.NEVER,true);
                if (hit != null) next=hit.getHitPosition().toLocation(next.getWorld());
                projectile=next;
                weapon.location(BukkitAdapter.adapt(next));
                WindEffects.swirl(audience.players(),next,tick,.9);
                if (hit != null || tick == IMPACT) {
                    burst=true;audience.hide(weapon);weapon=null;
                    WindEffects.burst(audience.players(),next);
                    sound(next,Sound.ENTITY_BREEZE_WIND_BURST,1.4f,.7f);
                    sound(next,Sound.ENTITY_GENERIC_EXPLODE,1.4f,.75f);
                    sound(next,Sound.ENTITY_WIND_CHARGE_WIND_BURST,1.1f,1.15f);
                    trace("burst x="+next.getX()+" y="+next.getY()+" z="+next.getZ());
                }
            }
            if (tick == LAND) sound(owner.getLocation(),Sound.ENTITY_BREEZE_LAND,.6f,1);
            Phase phase=phase(tick);
            if (phase != lastPhase) { lastPhase=phase;trace("phase: "+phase+" tick="+tick); }
            tick++;
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING,"Rasenshuriken scene failed",failure);
            plugin.playback().stop(owner.getUniqueId());
        }
    }
    private Location ground(Location at) {
        var world=at.getWorld();
        if (!world.isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4)) return null;
        var hit=world.rayTraceBlocks(at.clone().add(0,2,0),new Vector(0,-1,0),8,FluidCollisionMode.NEVER,true);
        return hit==null ? null : hit.getHitPosition().toLocation(world);
    }
    private boolean move(Location to) {
        Location from=owner.getLocation();Vector delta=to.toVector().subtract(from.toVector());
        int steps=Math.max(1,(int)Math.ceil(delta.length()/.15));
        for (int i=1;i<=steps;i++) {
            Vector offset=delta.clone().multiply(i/(double)steps);Location next=from.clone().add(offset);
            for (double x:new double[]{-.4,.4}) for (double z:new double[]{-.4,.4}) {
                if (!to.getWorld().isChunkLoaded((int)Math.floor(next.getX()+x)>>4,(int)Math.floor(next.getZ()+z)>>4)) return false;
            }
            if (!to.getWorld().getWorldBorder().isInside(next) || owner.wouldCollideUsing(owner.getBoundingBox().shift(offset))) return false;
        }
        if (delta.lengthSquared()<1e-10) return true;
        movingFrom=from;movingTo=to;
        try {
            boolean success=owner.teleport(to,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            if (success) lastOwned=to.clone();
            return success && !closed;
        } finally { movingFrom=null;movingTo=null; }
    }
    private static Location relative(Location origin,double right,double forward,double up) {
        double yaw=Math.toRadians(origin.getYaw());
        return origin.clone().add(-Math.cos(yaw)*right-Math.sin(yaw)*forward,up,-Math.sin(yaw)*right+Math.cos(yaw)*forward);
    }
    private void sound(Location at,Sound sound,float volume,float pitch) {
        for (Player p:audience.players()) p.playSound(at,sound,SoundCategory.PLAYERS,volume,pitch);
    }
    private void stopChargeSound() {
        for (Player p:audience.players()) p.stopSound(Sound.ENTITY_BREEZE_CHARGE,SoundCategory.PLAYERS);
    }
    private void smoke(Location at) { for (Player p:audience.players()) p.spawnParticle(Particle.POOF,at,20,.3,.5,.3,.03); }
    private void trace(String message) { if (plugin.getConfig().getBoolean("debug-input",false)) plugin.getLogger().info("Rasenshuriken "+message); }
    @Override public void close() {
        if (closed) return;closed=true;
        if (task!=null) task.cancel();
        try { stopChargeSound(); }
        finally {
            try { audience.close(); }
            finally {
                // A cancelled cinematic jump must not leave the player suspended.
                if (launch!=null && lastOwned!=null && owner.isOnline() && !owner.isDead()
                        && owner.getWorld()==launch.getWorld() && owner.getLocation().distanceSquared(lastOwned)<.01
                        && owner.getLocation().getY()>launch.getY()+.05 && !owner.collidesAt(launch)) {
                    owner.teleport(launch,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
                owner.setFallDistance(0);
            }
        }
    }
}
