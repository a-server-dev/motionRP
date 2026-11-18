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
    private record Playing(EntityTracker tracker, BukkitTask timeout, MotionScene scene) {}
    private final MotionPlugin plugin;
    private final Map<UUID, Playing> playing = new HashMap<>();

    public MotionPlayback(MotionPlugin plugin) { this.plugin = plugin; }

    public String unavailable(MotionCatalog.Definition definition) {
        var model = BetterModel.limbOrNull(definition.model());
        if (model == null) return "プレイヤーモデルが未読込: " + definition.model();
        if (model.animation(definition.animation()).isEmpty()) return "アニメーションが未登録: " + definition.animation();
        for(String required:SummonAssets.models(definition.effect())) {
            if(BetterModel.modelOrNull(required)==null) return "演出モデルが未読込: "+required;
        }
        if(definition.effect().startsWith("gate_of_babylon_") && BetterModel.modelOrNull(SummonAssets.MASS_GATE).animation("spin").isEmpty()) return "大規模門の回転が未登録";
        if(definition.effect().equals("gate_of_babylon_enuma") && (BetterModel.modelOrNull(SummonAssets.ENUMA_CHARGE).animation("spin").isEmpty() || BetterModel.modelOrNull(SummonAssets.ENUMA_RING).animation("spin").isEmpty() || BetterModel.modelOrNull(SummonAssets.EA).animation("spin").isEmpty() || BetterModel.modelOrNull(SummonAssets.EA).animation("level").isEmpty() || BetterModel.modelOrNull(SummonAssets.ENUMA).animation("vortex").isEmpty())) return "エアの回転・射出が未登録";
        if(definition.effect().equals("susanoo") && BetterModel.modelOrNull(SummonAssets.ARMOR).animation("slash").isEmpty())
            return "須佐能乎の斬撃アニメーションが未登録";
        if(definition.effect().equals("gate_of_babylon") && BetterModel.modelOrNull(SummonAssets.GATE).animation("spin").isEmpty())
            return "黄金の門の回転アニメーションが未登録";
        if(definition.effect().equals("senbonzakura") && BetterModel.modelOrNull(SummonAssets.CHERRY_PETALS).animation("swirl").isEmpty())
            return "千本桜の旋回アニメーションが未登録";
        if(definition.effect().equals("titan_transform") && (model.animation("titan_idle").isEmpty() || BetterModel.modelOrNull(SummonAssets.TITAN_BODY).animation("titan_idle").isEmpty())) return "巨人化の待機アニメーションが未登録";
        if (Set.of("rasengan", "rasenshuriken").contains(definition.effect())) {
            if (model.animation("rasengan_helper").isEmpty()) return "分身アニメーションが未登録: rasengan_helper";
            var orb = BetterModel.modelOrNull(definition.effect().equals("rasenshuriken") ? BundledModels.SHURIKEN_ORB : BundledModels.ORB);
            if (orb == null || orb.animation("spin").isEmpty()) return "演出モデルが未読込。BetterModelを再読み込みしてください。";
        }
        return null;
    }

    public boolean active(UUID player) {
        var current = playing.get(player);
        return current != null && !current.tracker.isClosed();
    }

    public boolean ownsMove(Player player, org.bukkit.Location from, org.bukkit.Location to) {
        var current = playing.get(player.getUniqueId());
        return current != null && current.scene != null && current.scene.ownsMove(from, to);
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
            TitanState titan=definition.effect().equals("titan_transform") ? new TitanState(plugin,player) : null;
            // Same player-limb path used by BetterModel's own /bm play command.
            // No per-viewer filter: BetterModel manages visibility for nearby players.
            tracker = Objects.requireNonNull(BetterModel.limbOrNull(definition.model()))
                    .create(BukkitAdapter.adapt(player), TrackerModifier.DEFAULT);
            EntityTracker owned = tracker;
            UUID uuid = player.getUniqueId();
            var timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> finish(uuid, owned), definition.maxTicks());
            MotionScene scene = switch (definition.effect()) {
                case "rasengan" -> new RasenganScene(plugin, player, definition.model());
                case "rasenshuriken" -> new RasenshurikenScene(plugin, player, definition.model());
                case "gate_of_babylon_100" -> new TreasuryScene(plugin,player,owned,false);
                case "gate_of_babylon_enuma" -> new TreasuryScene(plugin,player,owned,true);
                case "gate_of_babylon" -> new GateOfBabylonScene(plugin,player);
                case "susanoo" -> new SusanooScene(plugin,player);
                case "senbonzakura" -> new SenbonzakuraScene(plugin,player);
                case "titan_transform" -> new TitanScene(plugin,player,definition.model(),owned,Objects.requireNonNull(titan));
                default -> null;
            };
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
