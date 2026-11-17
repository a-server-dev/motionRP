package dev.aserver.motionrp;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.manager.PlayerManager;
import kr.toxicity.model.api.tracker.DummyTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

/** Owns the bounded recipients and display trackers of a cosmetic scene. */
final class SceneAudience implements AutoCloseable {
    private final Player owner;
    private final Map<UUID, Player> viewers = new LinkedHashMap<>();
    // BetterModel Tracker.equals/hashCode use only the model name. Two clones
    // of the same model must still be owned and closed independently.
    private final Set<DummyTracker> trackers = Collections.newSetFromMap(new IdentityHashMap<>());
    SceneAudience(Player owner) { this.owner = owner; }
    Collection<Player> players() { return viewers.values(); }
    void refresh() {
        Set<UUID> eligible = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld() != owner.getWorld() || !p.canSee(owner)
                    || p.getLocation().distanceSquared(owner.getLocation()) > 48*48) continue;
            eligible.add(p.getUniqueId());
            Player old = viewers.put(p.getUniqueId(), p);
            if (old != null && old != p) for (var t : trackers) t.remove(BukkitAdapter.adapt(old));
        }
        viewers.entrySet().removeIf(e -> {
            if (eligible.contains(e.getKey())) return false;
            for (var t : trackers) t.remove(BukkitAdapter.adapt(e.getValue()));
            return true;
        });
        for (var t : trackers) for (var p : viewers.values()) spawn(t, p);
    }
    void show(DummyTracker tracker, MotionPlugin plugin, String kind) {
        show(tracker,plugin,"Rasenshuriken",kind);
    }
    void show(DummyTracker tracker, MotionPlugin plugin, String prefix, String kind) {
        trackers.add(tracker);
        for (var p : viewers.values()) spawn(tracker, p);
        if (plugin.getConfig().getBoolean("debug-input", false)) plugin.getLogger().info(prefix + " displays " + kind + " ids="
                + tracker.getPipeline().stream().filter(b -> b.getDisplay() != null)
                .map(b -> Integer.toString(b.getDisplay().id())).collect(java.util.stream.Collectors.joining(",")));
    }
    void hide(DummyTracker tracker) {
        if (tracker == null) return;
        trackers.remove(tracker);
        tracker.close();
    }
    private void spawn(DummyTracker tracker, Player player) {
        if (tracker.isSpawned(player.getUniqueId())) return;
        var adapted = BukkitAdapter.adapt(player);
        BetterModel.platform().manager(PlayerManager.class).player(adapted);
        tracker.spawn(adapted); tracker.forceUpdate(true);
    }
    @Override public void close() {
        RuntimeException first = null;
        for (var t : trackers) try { t.close(); } catch (RuntimeException e) { if (first == null) first = e; }
        trackers.clear(); viewers.clear();
        if (first != null) throw first;
    }
}
