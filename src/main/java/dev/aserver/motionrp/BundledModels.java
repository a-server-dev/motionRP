package dev.aserver.motionrp;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import org.bukkit.Bukkit;
import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import dev.aserver.motionrp.core.BundledAssetUpdater;

/** Installs/migrates known shipped sample assets; custom files are never overwritten. */
public final class BundledModels {
    public static final String PLAYER = "motionrp_rasengan";
    public static final String ORB = "motionrp_rasengan_orb";
    public static final String SHURIKEN_PLAYER = "motionrp_rasenshuriken";
    public static final String SHURIKEN_ORB = "motionrp_rasenshuriken_orb";
    private static final String SHURIKEN_030_SHA = "497bcb980eef3288e8ece86779fa8651c6fb97f40ef49cde598b78232c30dce1";
    private static final String PLAYER_020_SHA = "5c6a2b0425861d8fd648dfeacc5b61592ca2ef1c46da3ae6dfa55d08e46916d6";
    private static final String PLAYER_021_SHA = "e62db71a5d780a4ae7a3a3771e281e90c3b0fa4795c121e331ebf616b56d4c5d";
    private static final String PLAYER_023_SHA = "2e2fba7179fb111f67963a2619896c7cca7a2f434d241a1679f3a50f93bafc2e";
    private static final java.util.Map<String,String> SUMMON_040_SHA=java.util.Map.of(
            "players/motionrp_gate_of_babylon.bbmodel","ce7a2a55f8c02aa33a9050dfd8793d6c8cd35e6498d1f5b9e6d0edd7fd070f9c",
            "models/motionrp_susanoo_armor.bbmodel","d054d6147211c1cc72e6281ee5b9cc1a3ddcda8d9da6548e4472affedb16040c",
            "models/motionrp_susanoo_ribs.bbmodel","74993bf47a065b5da8cb50a3ceccfbfc6bf6fa5c13d6bd14c2c804773e1db912");
    private static final java.util.Map<String,String> SUMMON_050_SHA=java.util.Map.of(
            "players/motionrp_gate_of_babylon.bbmodel","2e0bae16f4570116a383ccaa91e09319564665870b34c7cc03a9c156c34c2272",
            "players/motionrp_senbonzakura.bbmodel","7a38d287afad202d93644f1d54ba0267baa33eff01315860675bd940c5944afe",
            "models/motionrp_senbonzakura_blade.bbmodel","aebce2d6a72045a193e54ebaad4e7cf8fd579ccc19aba3182b659c5f5ed44d70");
    private final MotionPlugin plugin;
    public BundledModels(MotionPlugin plugin) { this.plugin = plugin; }

    public void install() {
        var betterModel = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("BetterModel"));
        Path root = betterModel.getDataFolder().toPath();
        boolean changed = false;
        try {
            // A fresh BetterModel folder may not seed its examples after our
            // namespaced player model creates players/. Keep the default roll usable.
            Path steve = root.resolve("players/steve.bbmodel");
            if (!Files.exists(steve) && plugin.catalog().motions().values().stream().anyMatch(d -> d.model().equals("steve"))) {
                Files.createDirectories(steve.getParent());
                try (InputStream source = Objects.requireNonNull(betterModel.getResource("steve.bbmodel"), "BetterModel steve.bbmodel")) {
                    Files.copy(source, steve);
                }
                changed = true;
            }
            var files=new java.util.ArrayList<>(java.util.List.of("players/" + PLAYER + ".bbmodel", "models/" + ORB + ".bbmodel",
                    "players/" + SHURIKEN_PLAYER + ".bbmodel", "models/" + SHURIKEN_ORB + ".bbmodel", "LICENSE-MotionRP-models.txt", "players/motionrp_performances.bbmodel"));
            for(String effect:java.util.List.of("gate_of_babylon","susanoo","senbonzakura","titan_transform","gate_of_babylon_100","gate_of_babylon_enuma","zangetsu_bankai","excalibur","gae_bolg","kamehameha","genki_dama","rho_aias","getsuga_tensho","road_roller","kokuryuha","kokujo_tengen_myoo","chibaku_tensei","infinity_castle","fusion_left","fusion_right","broly_attacker","broly_victim","final_battle_naruto","final_battle_sasuke","fire_tornado","emperor_penguin_1","emperor_penguin_2_left","emperor_penguin_2_right")) {
                files.add("players/motionrp_"+effect+".bbmodel");
                for(String model:SummonAssets.models(effect)) files.add("models/"+model+".bbmodel");
            }
            files.add("players/motionrp_player_arm_left.bbmodel");files.add("players/motionrp_player_arm_right.bbmodel");
            files.addAll(plugin.studio().assets());
            for (String file : new java.util.LinkedHashSet<>(files)) {
                Path target = root.resolve(file);
                String resource = file.startsWith("LICENSE") ? "bettermodel/LICENSE-BetterModel.txt" : "bettermodel/" + file;
                try (InputStream source = Objects.requireNonNull(plugin.getResource(resource), resource)) {
                    byte[] bytes=source.readAllBytes();
                    var result = new BundledAssetUpdater().install(target, bytes,
                            plugin.studio().knownHashes(file,dev.aserver.motionrp.core.BundledAssetVersions.knownHashes(file,file.equals("players/" + PLAYER + ".bbmodel") ? Set.of(PLAYER_020_SHA, PLAYER_021_SHA, PLAYER_023_SHA)
                                    : file.equals("players/" + SHURIKEN_PLAYER + ".bbmodel") ? Set.of(SHURIKEN_030_SHA)
                                    : file.equals("players/motionrp_gate_of_babylon.bbmodel") ? Set.of(SUMMON_040_SHA.get(file),"308b4f2762d09705d8c7852b215092556d1a36e46e9aa99b7956ea7fd780ea7d",SUMMON_050_SHA.get(file))
                                    : file.equals("players/motionrp_senbonzakura.bbmodel") ? Set.of(SUMMON_050_SHA.get(file),"cfa5f6ce32f52c32fa3a6d1a75de6371f5b013883445afc29c6bae6add12ded5")
                                    : file.equals("players/motionrp_gate_of_babylon_enuma.bbmodel") ? Set.of("65690445a739722ab052f571ceccd0476f2fab7ce106f2795cb724c45d5a199c","6d4400edfb1decb0b5a01e2068d1769e94c22e2fe0e95a95a3fbec3be816e5ab","39e048a07961c553550c992bddf749f24df784690b9a68afcbf861d69c911700","4be25d52494f9f97b56693808dc9aef769e205679b4fed7bc7c77e5c0131c7b5")
                                    : file.equals("models/motionrp_ea.bbmodel") ? Set.of("26bf036c8d1230c4da474034a86dc1afbe88f500237d9d5872c8a58318bb3e6e","dcf8d64c65ccf7b3c50e28ccc063048368bc9bfcc88c0d2e4e8c4109496dd69d","c2b2bc17eaa70b0acf1a66d5682d5f469df9e7f1ca70fa47e703083f248857ec","97161bc3d6a447a255f60c5f42e54c15ea58007c0f28e3270f10581fbaf5dad0")
                                    : file.equals("models/motionrp_enuma_vortex.bbmodel") ? Set.of("9ddebc5469047326fbff87242a29e64723952d0147512c4cf6339643042ecb0e")
                                    : file.equals("models/motionrp_bab_ilu.bbmodel") ? Set.of("72e48d5765f3f3cd4a196ad42bdac6594cc4a19e3c5c19901b531b859124bbb2","545322bab257c47b88f2b4a96cdbe67adece3bc641c07c06ce4b16cf4a656039")
                                    : file.equals("players/motionrp_titan_transform.bbmodel") ? Set.of("bfbc83f290b4252a4088fd825e0fca0bed7a4a019e74f4305d33a791d3f20857")
                                    : file.equals("players/motionrp_zangetsu_bankai.bbmodel") ? Set.of("909515f7393027b9c35aee34ab05e89d04c718bb67e5645e3667198eb4261c33","1eef505842f410eca1b28c82c6707903c016e247f5ac4ffdd537a3b68207d822")
                                    : file.equals("models/motionrp_tensa_zangetsu.bbmodel") ? Set.of("cf68b92b1a7cde04d8b7b0c4a5f435b621d9c9760e1102675d969416c7068806")
                                    : file.equals("models/motionrp_bankai_cocoon.bbmodel") ? Set.of("d6f38cfd0592d3ce2dd6fa5154269bc340f9f4eb8f6fe528f79b04f761faa127")
                                    : file.equals("models/motionrp_zangetsu_cleaver.bbmodel") ? Set.of("de065d3da7442d5a4c74c7d1cfadaaf1352e32e294b216d28d6f91be7dc5335d","397b60c008232307f099443b633a46a4643e7ac58422441e55ca67ed7d9c6385","2b9a0b0bfc733d1fb25e099e3a0c1c795f66f129ece7971cecb80000e724b794")
                                    : file.equals("models/motionrp_susanoo_armor.bbmodel") ? Set.of(SUMMON_040_SHA.get(file),"1868e0e239fee4b7470332ba21007cbde29a12c4938a146ea08af4e00e23c098","add67777e2a776ae9b7e2344b66ffabeba44ca41baa29a6fad5b415fbdb060ee","7edbd7f936024d0c1b5d47c618d2ef04e08f59f7a435a2e6f2edf65372a0466c")
                                    : file.equals("players/motionrp_susanoo.bbmodel") ? Set.of("435fb48bd71a7c2b7357b4070ff3084a351d4884ac7810cedd4accacc7c155d9")
                                    : file.equals("models/motionrp_susanoo_ribs.bbmodel") ? Set.of(SUMMON_040_SHA.get(file),"ef9b07b05a0000b5a0f9d908a621e4543d71db9081d1bb61b06b97fa2a0cf62b")
                                    : file.equals("models/motionrp_susanoo_slash.bbmodel") ? Set.of("6255e3d0879d4442415cf00594cb7ed59928179965e7ad7d2b9ae2388a24dbf6")
                                    : file.equals("players/motionrp_gae_bolg.bbmodel") ? Set.of("233b327d4b95091c583e1d2c432ae12ef26782a51d8829eb66dc1654c23c14e4")
                                    : file.equals("models/motionrp_excalibur_blade.bbmodel") ? Set.of("f52925ae4aaa05a189851ba32a820f8ddf49888c1444d3540999d9153841b219")
                                    : file.equals("models/motionrp_gae_bolg_spear.bbmodel") ? Set.of("79a465c56e8e1599bd4e95eaffcf70427af5a5bbcffdf1cbeabf08ade160db92")
                                    : SUMMON_050_SHA.containsKey(file) ? Set.of(SUMMON_050_SHA.get(file))
                                    : SUMMON_040_SHA.containsKey(file) ? Set.of(SUMMON_040_SHA.get(file)) : Set.of())),
                            plugin.getDataFolder().toPath().resolve("model-backups"),
                            plugin.getDataFolder().toPath().resolve("model-updates"));
                    plugin.studio().installed(file,bytes,result==BundledAssetUpdater.Result.CUSTOM_PRESERVED);
                    if (result == BundledAssetUpdater.Result.INSTALLED || result == BundledAssetUpdater.Result.UPDATED) changed = true;
                    if (result == BundledAssetUpdater.Result.UPDATED) plugin.getLogger().info("Updated bundled model (backup retained): " + file);
                    if (result == BundledAssetUpdater.Result.CUSTOM_PRESERVED) {
                        plugin.getLogger().warning("Custom model kept: " + file + "; bundled update available in MotionRP/model-updates.");
                    }
                }
            }
            plugin.studio().saveHashes();
        } catch (IOException failure) { throw new UncheckedIOException("Cannot install bundled BetterModel assets", failure); }
        if (changed) reloadWhenIdle(0);
    }

    private void reloadWhenIdle(int attempt) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Model reload and pack generation use BetterModel's supported async path.
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    var result = BetterModel.platform().reload();
                    if (result instanceof BetterModelPlatform.ReloadResult.OnReload) {
                        if (attempt < 3 && plugin.isEnabled()) reloadWhenIdle(attempt + 1);
                        else plugin.getLogger().warning("BetterModel is busy; run /bettermodel reload to load bundled assets.");
                    } else if (result instanceof BetterModelPlatform.ReloadResult.Failure failed) {
                        plugin.getLogger().log(Level.SEVERE, "Bundled BetterModel asset reload failed", failed.throwable());
                    } else {
                        plugin.getLogger().info("Bundled Rasengan models loaded; redistribute BetterModel/build.zip to clients.");
                    }
                } catch (RuntimeException failure) {
                    plugin.getLogger().log(Level.SEVERE, "Cannot reload bundled BetterModel assets", failure);
                }
            });
        }, attempt == 0 ? 20 : 40);
    }
}
