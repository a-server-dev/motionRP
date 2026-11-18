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
                    "players/" + SHURIKEN_PLAYER + ".bbmodel", "models/" + SHURIKEN_ORB + ".bbmodel", "LICENSE-MotionRP-models.txt"));
            for(String effect:java.util.List.of("gate_of_babylon","susanoo","senbonzakura","titan_transform")) {
                files.add("players/motionrp_"+effect+".bbmodel");
                for(String model:SummonAssets.models(effect)) files.add("models/"+model+".bbmodel");
            }
            for (String file : files) {
                Path target = root.resolve(file);
                String resource = file.startsWith("LICENSE") ? "bettermodel/LICENSE-BetterModel.txt" : "bettermodel/" + file;
                try (InputStream source = Objects.requireNonNull(plugin.getResource(resource), resource)) {
                    var result = new BundledAssetUpdater().install(target, source.readAllBytes(),
                            file.equals("players/" + PLAYER + ".bbmodel") ? Set.of(PLAYER_020_SHA, PLAYER_021_SHA, PLAYER_023_SHA)
                                    : file.equals("players/" + SHURIKEN_PLAYER + ".bbmodel") ? Set.of(SHURIKEN_030_SHA)
                                    : file.equals("players/motionrp_gate_of_babylon.bbmodel") ? Set.of(SUMMON_040_SHA.get(file),"308b4f2762d09705d8c7852b215092556d1a36e46e9aa99b7956ea7fd780ea7d",SUMMON_050_SHA.get(file))
                                    : file.equals("players/motionrp_senbonzakura.bbmodel") ? Set.of(SUMMON_050_SHA.get(file),"cfa5f6ce32f52c32fa3a6d1a75de6371f5b013883445afc29c6bae6add12ded5")
                                    : file.equals("players/motionrp_titan_transform.bbmodel") ? Set.of("bfbc83f290b4252a4088fd825e0fca0bed7a4a019e74f4305d33a791d3f20857")
                                    : SUMMON_050_SHA.containsKey(file) ? Set.of(SUMMON_050_SHA.get(file))
                                    : SUMMON_040_SHA.containsKey(file) ? Set.of(SUMMON_040_SHA.get(file)) : Set.of(),
                            plugin.getDataFolder().toPath().resolve("model-backups"),
                            plugin.getDataFolder().toPath().resolve("model-updates"));
                    if (result == BundledAssetUpdater.Result.INSTALLED || result == BundledAssetUpdater.Result.UPDATED) changed = true;
                    if (result == BundledAssetUpdater.Result.UPDATED) plugin.getLogger().info("Updated bundled model (backup retained): " + file);
                    if (result == BundledAssetUpdater.Result.CUSTOM_PRESERVED) {
                        plugin.getLogger().warning("Custom model kept: " + file + "; bundled update available in MotionRP/model-updates.");
                    }
                }
            }
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
