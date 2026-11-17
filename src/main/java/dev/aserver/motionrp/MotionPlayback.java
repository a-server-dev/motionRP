package dev.aserver.motionrp;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerModifier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

/** Owns only trackers created by MotionRP; never closes another plugin's model. */
public final class MotionPlayback {
    private record Playing(EntityTracker tracker, BukkitTask timeout, RasenganScene scene) {}
    private final MotionPlugin plugin;
    private final Map<UUID, Playing> playing = new HashMap<>();

    public MotionPlayback(MotionPlugin plugin) { this.plugin = plugin; }

    public String unavailable(MotionCatalog.Definition definition) {
        var model = BetterModel.limbOrNull(definition.model());
        if (model == null) return "プレイヤーモデルが未読込: " + definition.model();
        if (model.animation(definition.animation()).isEmpty()) return "アニメーションが未登録: " + definition.animation();
        if (definition.effect().equals("rasengan")) {
            if (model.animation("rasengan_helper").isEmpty()) return "分身アニメーションが未登録: rasengan_helper";
            var orb = BetterModel.modelOrNull(BundledModels.ORB);
            if (orb == null || orb.animation("spin").isEmpty()) return "螺旋丸モデルが未読込。BetterModelを再読み込みしてください。";
        }
        return null;
    }

    public boolean active(UUID player) {
        var current = playing.get(player);
        return current != null && !current.tracker.isClosed();
    }

    public void play(Player player, MotionCatalog.Definition definition) {
        String error = unavailable(definition);
        if (error != null) { plugin.tell(player, error); return; }
        if (player.isDead() || player.isInsideVehicle() || player.isGliding() || player.isSleeping()) {
            plugin.tell(player, "現在の状態ではモーションを再生できません。"); return;
        }
        stop(player.getUniqueId());
        var registry = BetterModel.registryOrNull(player.getUniqueId());
        if (registry != null && !registry.trackers().isEmpty()) {
            plugin.tell(player, "別のモデル表示を使用中です。先に解除してください。"); return;
        }
        EntityTracker tracker = null;
        try {
            // Same player-limb path used by BetterModel's own /bm play command.
            // No per-viewer filter: BetterModel manages visibility for nearby players.
            tracker = Objects.requireNonNull(BetterModel.limbOrNull(definition.model()))
                    .create(BukkitAdapter.adapt(player), TrackerModifier.DEFAULT);
            EntityTracker owned = tracker;
            UUID uuid = player.getUniqueId();
            var timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> finish(uuid, owned), definition.maxTicks());
            RasenganScene scene = definition.effect().equals("rasengan") ? new RasenganScene(plugin, player, definition.model()) : null;
            playing.put(uuid, new Playing(owned, timeout, scene));
            boolean started = owned.animate(definition.animation(), AnimationModifier.DEFAULT_WITH_PLAY_ONCE, () -> {
                if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> finish(uuid, owned));
            });
            if (!started) { stop(uuid); plugin.tell(player, "モーションを開始できませんでした。"); return; }
            if (scene != null) scene.start();
            plugin.tell(player, "再生: " + definition.name());
        } catch (RuntimeException failure) {
            stop(player.getUniqueId());
            if (tracker != null && !tracker.isClosed()) tracker.close();
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Motion playback failed for " + player.getUniqueId(), failure);
            plugin.tell(player, "モーション再生に失敗しました。管理者に連絡してください。");
        }
    }

    private void finish(UUID uuid, EntityTracker expected) {
        var current = playing.get(uuid);
        if (current != null && current.tracker == expected) stop(uuid);
    }

    public void stop(UUID player) {
        var current = playing.remove(player);
        if (current == null) return;
        current.timeout.cancel();
        try { if (current.scene != null) current.scene.close(); }
        finally { if (!current.tracker.isClosed()) current.tracker.close(); }
    }

    public void stopAll() { new ArrayList<>(playing.keySet()).forEach(this::stop); }
}
