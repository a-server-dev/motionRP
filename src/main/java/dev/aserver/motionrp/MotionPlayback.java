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
    private record Playing(EntityTracker tracker, BukkitTask timeout, MotionScene scene, boolean filtered) {}
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
        if(definition.effect().equals("road_roller"))for(String side:java.util.List.of("left","right")){var arm=BetterModel.limbOrNull("motionrp_player_arm_"+side);if(arm==null||arm.animation("rapid_punch").isEmpty())return "プレイヤーの腕の残像モデルが未読込";}
        if(definition.effect().startsWith("gate_of_babylon_") && BetterModel.modelOrNull(SummonAssets.MASS_GATE).animation("spin").isEmpty()) return "大規模門の回転が未登録";
        if(definition.effect().equals("gate_of_babylon_enuma") && (BetterModel.modelOrNull(SummonAssets.ENUMA_RING).animation("spin").isEmpty() || BetterModel.modelOrNull(SummonAssets.EA).animation("spin").isEmpty() || BetterModel.modelOrNull(SummonAssets.EA).animation("level").isEmpty() || BetterModel.modelOrNull(SummonAssets.ENUMA).animation("vortex").isEmpty())) return "エアの回転・射出が未登録";
        if(definition.effect().equals("excalibur") && (BetterModel.modelOrNull(SummonAssets.EXCALIBUR).animation("release").isEmpty() || BetterModel.modelOrNull(SummonAssets.EXCALIBUR_LIGHT).animation("flow").isEmpty())) return "聖剣の解放が未登録";
        if(definition.effect().equals("excalibur") && (BetterModel.modelOrNull(SummonAssets.EXCALIBUR_CHARGE).animation("cleave").isEmpty() || BetterModel.modelOrNull(SummonAssets.EXCALIBUR_WAVE).animation("rush").isEmpty())) return "聖剣の光刃・奔流が未登録";
        if(definition.effect().equals("gae_bolg") && BetterModel.modelOrNull(SummonAssets.GAE_BOLG).animation("throw_sequence").isEmpty()) return "魔槍の刺突が未登録";
        if(definition.effect().equals("zangetsu_bankai") && (BetterModel.modelOrNull(SummonAssets.ZANGETSU).animation("point").isEmpty() || BetterModel.modelOrNull(SummonAssets.TENSA).animation("reveal").isEmpty() || BetterModel.modelOrNull(SummonAssets.BANKAI_COCOON).animation("swirl").isEmpty())) return "卍解の変形アニメーションが未登録";
        if(definition.effect().equals("susanoo") && (BetterModel.modelOrNull(SummonAssets.ARMOR).animation("slash").isEmpty() || BetterModel.modelOrNull(SummonAssets.ARMOR).animation("unfurl").isEmpty()))
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

    public boolean followsMovement(Player player){var current=playing.get(player.getUniqueId());return current!=null&&current.scene!=null&&current.scene.followsMovement();}

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
            // Apply the condition during construction, before BetterModel can spawn displays.
            var castOrigin=BukkitAdapter.adapt(player.getLocation());
            boolean fixedAudience=Set.of("susanoo","kamehameha","genki_dama").contains(definition.effect());
            tracker = Objects.requireNonNull(BetterModel.limbOrNull(definition.model()))
                    .create(BukkitAdapter.adapt(player), TrackerModifier.DEFAULT,t->{
                        if(!definition.effect().equals("performance"))t.spawnCondition(p->{
                            var center=fixedAudience?castOrigin:t.location();
                            return plugin.visibility().enabled(p.uuid()) && p.location().world().equals(center.world())
                                    && (p.uuid().equals(player.getUniqueId()) || p.location().distanceSquared(center)<=Math.pow(plugin.visibility().range(256),2));
                        });
                    });
            EntityTracker owned = tracker;
            UUID uuid = player.getUniqueId();
            var timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> finish(uuid, owned), definition.maxTicks());
            MotionScene scene = switch (definition.effect()) {
                case "road_roller","kokuryuha","kokujo_tengen_myoo","chibaku_tensei" -> new EpicScene(plugin,player,owned,definition.effect());
                case "final_battle_naruto","final_battle_sasuke" -> new FinalBattleScene(plugin,player,owned,definition.effect().equals("final_battle_naruto"));
                case "fusion_left","fusion_right" -> new FusionScene(plugin,player,owned,definition.effect().equals("fusion_left"));
                case "rho_aias","getsuga_tensho" -> new BarrierSlashScene(plugin,player,owned,definition.effect().equals("rho_aias"));
                case "performance" -> new PerformanceScene(plugin,player,owned,java.util.Arrays.stream(dev.aserver.motionrp.core.Performance.values()).filter(p->p.id().equals(definition.animation())).findFirst().orElseThrow());
                case "rasengan" -> new RasenganScene(plugin, player, definition.model());
                case "rasenshuriken" -> new RasenshurikenScene(plugin, player, definition.model());
                case "kamehameha" -> new DragonBallScene(plugin,player,owned,false);
                case "genki_dama" -> new DragonBallScene(plugin,player,owned,true);
                case "excalibur" -> new NobleScene(plugin,player,owned,true);
                case "gae_bolg" -> new GaeBolgScene(plugin,player,owned);
                case "zangetsu_bankai" -> new BankaiScene(plugin,player,owned);
                case "gate_of_babylon_100" -> new TreasuryScene(plugin,player,owned,false);
                case "gate_of_babylon_enuma" -> new TreasuryScene(plugin,player,owned,true);
                case "gate_of_babylon" -> new GateOfBabylonScene(plugin,player);
                case "susanoo" -> new SusanooScene(plugin,player);
                case "senbonzakura" -> new SenbonzakuraScene(plugin,player);
                case "titan_transform" -> new TitanScene(plugin,player,definition.model(),owned,Objects.requireNonNull(titan));
                default -> null;
            };
            playing.put(uuid, new Playing(owned, timeout, scene,!definition.effect().equals("performance")));
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
        if(plugin.fusion()!=null)plugin.fusion().stopping(player);
        if (current == null) return;
        current.timeout.cancel();
        try { if (current.scene != null) current.scene.close(); }
        finally { if (!current.tracker.isClosed()) current.tracker.close(); }
    }

    void refreshVisibility(Player viewer){
        for(var active:playing.values()){
            var tracker=active.tracker;
            if(!active.filtered)continue;
            if(active.scene!=null)active.scene.refreshAudience();
            if(!plugin.visibility().enabled(viewer.getUniqueId()))tracker.remove(BukkitAdapter.adapt(viewer));
            else tracker.refresh();
        }
    }

    public void stopAll() { new ArrayList<>(playing.keySet()).forEach(this::stop); }
}
