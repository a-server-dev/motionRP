package dev.aserver.motionrp;

import dev.aserver.motionrp.core.RasenganTimeline;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.*;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.tracker.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.logging.Level;

/** Cosmetic only: the clone and orb are display packets, not damageable entities. */
public final class RasenganScene implements AutoCloseable {
    private final MotionPlugin plugin;
    private final Player owner;
    private final String playerModel;
    private final Map<UUID, Player> viewers = new LinkedHashMap<>();
    private DummyTracker clone;
    private DummyTracker orb;
    private BukkitTask task;
    private int tick;
    private boolean closed;
    private RasenganTimeline.Phase lastPhase;
    private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(90, 224, 255), .65f);
    private static final Particle.DustOptions WHITE = new Particle.DustOptions(Color.fromRGB(220, 255, 255), .45f);

    public RasenganScene(MotionPlugin plugin, Player owner, String playerModel) {
        this.plugin = plugin;
        this.owner = owner;
        this.playerModel = playerModel;
    }

    public void start() { task = Bukkit.getScheduler().runTaskTimer(plugin, this::advance, 0, 1); }

    private void advance() {
        if (closed) return;
        try {
            if (!owner.isOnline() || owner.isDead() || tick >= RasenganTimeline.END) {
                plugin.playback().stop(owner.getUniqueId());
                close();
                return;
            }
            var frame = RasenganTimeline.at(tick);
            if (tick % 5 == 0) refreshViewers();
            Location anchor = owner.getLocation().clone();
            anchor.setPitch(0);
            Location ball = relative(anchor, .34, frame.forward(), 1.28);
            Location assistant = relative(anchor, 1.04, .55, 0);
            var toward = ball.toVector().subtract(assistant.toVector());
            assistant.setYaw((float) Math.toDegrees(Math.atan2(-toward.getX(), toward.getZ())));
            if (frame.cloneVisible() && clone == null) {
                clone = Objects.requireNonNull(BetterModel.limbOrNull(playerModel))
                        .create(BukkitAdapter.adapt(assistant), ModelProfile.of(BukkitAdapter.adapt(owner)));
                clone.animate("rasengan_helper", AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
                spawnForViewers(clone);
                smoke(assistant.clone().add(0, .85, 0));
                sound(assistant, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, .65f, 1.4f);
            }
            if (!frame.cloneVisible() && clone != null) {
                smoke(cloneLocation(anchor));
                clone.close(); clone = null;
                sound(anchor, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, .4f, 1.8f);
            }
            if (clone != null) clone.location(BukkitAdapter.adapt(assistant));
            if (frame.orbVisible() && orb == null) {
                orb = Objects.requireNonNull(BetterModel.modelOrNull(BundledModels.ORB)).create(BukkitAdapter.adapt(ball),
                        TrackerModifier.DEFAULT, t -> t.update(TrackerUpdateAction.brightness(15, 15)));
                orb.animate("spin", AnimationModifier.builder().type(AnimationIterator.Type.LOOP).build());
                spawnForViewers(orb);
                sound(ball, Sound.BLOCK_BEACON_POWER_SELECT, .5f, 1.8f);
            }
            if (!frame.orbVisible() && orb != null) { orb.close(); orb = null; }
            if (orb != null) {
                orb.location(BukkitAdapter.adapt(ball));
                orb.scaler(ModelScaler.value((float) frame.scale()));
            }
            if (frame.orbVisible() && tick % 2 == 0) spiral(ball, frame.scale());
            if (tick >= 20 && tick < 60 && tick % 12 == 0) sound(ball, Sound.BLOCK_BEACON_AMBIENT, .12f, 1.9f);
            if (tick == RasenganTimeline.THRUST_START) sound(ball, Sound.ENTITY_PLAYER_ATTACK_SWEEP, .8f, 1.2f);
            if (tick == RasenganTimeline.BURST) {
                sound(ball, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, .75f, .8f);
                for (Player viewer : viewers.values()) {
                    viewer.spawnParticle(Particle.DUST, ball, 24, .22, .22, .22, 0, CYAN);
                    viewer.spawnParticle(Particle.END_ROD, ball, 8, .16, .16, .16, .025);
                }
            }
            if (lastPhase != frame.phase()) {
                lastPhase = frame.phase();
                if (plugin.getConfig().getBoolean("debug-input", false)) {
                    plugin.getLogger().info("Rasengan phase: " + lastPhase + " tick=" + tick);
                }
            }
            tick++;
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "Rasengan scene failed; cleaning up owned displays", failure);
            plugin.playback().stop(owner.getUniqueId());
            close();
        }
    }

    private Location cloneLocation(Location anchor) { return relative(anchor, 1.04, .55, .85); }

    private static Location relative(Location origin, double right, double forward, double up) {
        double yaw = Math.toRadians(origin.getYaw());
        return origin.clone().add(-Math.cos(yaw) * right - Math.sin(yaw) * forward, up,
                -Math.sin(yaw) * right + Math.cos(yaw) * forward);
    }

    private void refreshViewers() {
        Set<UUID> eligible = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != owner.getWorld() || !player.canSee(owner)
                    || player.getLocation().distanceSquared(owner.getLocation()) > 48 * 48) continue;
            eligible.add(player.getUniqueId());
            if (viewers.putIfAbsent(player.getUniqueId(), player) == null) {
                if (clone != null) clone.spawn(BukkitAdapter.adapt(player));
                if (orb != null) orb.spawn(BukkitAdapter.adapt(player));
            }
        }
        var iterator = viewers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (eligible.contains(entry.getKey())) continue;
            if (clone != null) clone.remove(BukkitAdapter.adapt(entry.getValue()));
            if (orb != null) orb.remove(BukkitAdapter.adapt(entry.getValue()));
            iterator.remove();
        }
    }

    private void spawnForViewers(DummyTracker tracker) {
        for (Player viewer : viewers.values()) tracker.spawn(BukkitAdapter.adapt(viewer));
    }

    private void smoke(Location at) {
        for (Player viewer : viewers.values()) viewer.spawnParticle(Particle.POOF, at, 16, .23, .5, .23, .025);
    }

    private void sound(Location at, Sound sound, float volume, float pitch) {
        for (Player viewer : viewers.values()) viewer.playSound(at, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private void spiral(Location ball, double scale) {
        // Model rings provide the main effect. Bounded particles add moving wisps.
        for (int strand = 0; strand < 2; strand++) for (int i = 0; i < 4; i++) {
            double angle = tick * .65 + i * Math.PI / 2 + strand * Math.PI;
            double radius = (.12 + .07 * strand) * scale;
            Location point = ball.clone().add(Math.cos(angle) * radius, Math.sin(angle * 1.5) * radius, Math.sin(angle) * radius);
            for (Player viewer : viewers.values()) viewer.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, strand == 0 ? CYAN : WHITE);
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (task != null) task.cancel();
        try { if (clone != null && !clone.isClosed()) clone.close(); }
        finally {
            if (orb != null && !orb.isClosed()) orb.close();
            clone = null; orb = null; viewers.clear();
        }
    }
}
