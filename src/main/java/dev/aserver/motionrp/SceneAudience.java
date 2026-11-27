package dev.aserver.motionrp;

import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.DummyTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

/** Owns the bounded recipients and display trackers of a cosmetic scene. */
final class SceneAudience implements AutoCloseable {
    private final MotionPlugin plugin=org.bukkit.plugin.java.JavaPlugin.getPlugin(MotionPlugin.class);
    private final java.util.Set<org.bukkit.entity.Entity> nativeEntities=new java.util.LinkedHashSet<>();
    private final Player owner;
    private MotionVisibility visibility;
    SceneAudience filtered(MotionVisibility value){visibility=value;return this;}
    private org.bukkit.Location anchor;private final double range;
    private final Set<UUID> participants=new HashSet<>();
    SceneAudience focus(org.bukkit.Location p){anchor=p.clone();return this;}
    SceneAudience include(UUID id){participants.add(Objects.requireNonNull(id));return this;}
    private final Map<UUID, Player> viewers = new LinkedHashMap<>();
    // BetterModel Tracker.equals/hashCode use only the model name. Two clones
    // of the same model must still be owned and closed independently.
    private final Set<DummyTracker> trackers = Collections.newSetFromMap(new IdentityHashMap<>());
    SceneAudience(Player owner) {this(owner,null,48);}
    SceneAudience(Player owner,org.bukkit.Location anchor,double range) {this.owner=owner;this.anchor=anchor;this.range=range;}
    Collection<Player> players() { return viewers.values().stream().filter(p->ModelViewers.ready(p)&&(visibility==null || visibility.enabled(p.getUniqueId()))).toList(); }
    void refresh() {
        Set<UUID> eligible = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!ModelViewers.ready(p) || (visibility!=null && !visibility.enabled(p.getUniqueId())) || p.getWorld() != owner.getWorld() || !p.canSee(owner)
                    || (!p.getUniqueId().equals(owner.getUniqueId()) && !participants.contains(p.getUniqueId()) && p.getLocation().distanceSquared(anchor==null?owner.getLocation():anchor) > Math.pow(visibility==null?range:visibility.range(range),2))) continue;
            eligible.add(p.getUniqueId());
            Player old = viewers.put(p.getUniqueId(), p);
            if (old != null && old != p){for (var t : trackers) t.remove(BukkitAdapter.adapt(old));for(var e:nativeEntities)if(old.isOnline())old.hideEntity(plugin,e);}
        }
        viewers.entrySet().removeIf(e -> {
            if (eligible.contains(e.getKey())) return false;
            for (var t : trackers) t.remove(BukkitAdapter.adapt(e.getValue()));
            for(var entity:nativeEntities)if(e.getValue().isOnline())e.getValue().hideEntity(plugin,entity);
            return true;
        });
        for (var t : trackers) for (var p : viewers.values()) spawn(t, p);
        for(var e:nativeEntities)for(var p:viewers.values())p.showEntity(plugin,e);
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
    void showNative(org.bukkit.entity.Entity e){nativeEntities.add(e);for(var p:players())p.showEntity(plugin,e);}
    void hideNative(org.bukkit.entity.Entity e){nativeEntities.remove(e);e.remove();}
    void hide(DummyTracker tracker) {
        if (tracker == null) return;
        trackers.remove(tracker);
        tracker.close();
    }
    private void spawn(DummyTracker tracker, Player player) {
        if (!ModelViewers.ready(player) || tracker.isSpawned(player.getUniqueId())) return;
        var adapted = BukkitAdapter.adapt(player);
        tracker.spawn(adapted); tracker.forceUpdate(true);
    }
    @Override public void close() {
        RuntimeException first = null;
        for (var t : trackers) try { t.close(); } catch (RuntimeException e) { if (first == null) first = e; }
        for(var e:nativeEntities)e.remove();nativeEntities.clear();
        trackers.clear(); viewers.clear();
        if (first != null) throw first;
    }
}
