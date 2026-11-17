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
    private static final String PLAYER_020_SHA = "5c6a2b0425861d8fd648dfeacc5b61592ca2ef1c46da3ae6dfa55d08e46916d6";
    private static final String PLAYER_021_SHA = "e62db71a5d780a4ae7a3a3771e281e90c3b0fa4795c121e331ebf616b56d4c5d";
    private static final String PLAYER_023_SHA = "2e2fba7179fb111f67963a2619896c7cca7a2f434d241a1679f3a50f93bafc2e";
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
            for (String file : new String[]{"players/" + PLAYER + ".bbmodel", "models/" + ORB + ".bbmodel",
                    "players/" + SHURIKEN_PLAYER + ".bbmodel", "models/" + SHURIKEN_ORB + ".bbmodel", "LICENSE-MotionRP-models.txt"}) {
                Path target = root.resolve(file);
                String resource = file.startsWith("LICENSE") ? "bettermodel/LICENSE-BetterModel.txt" : "bettermodel/" + file;
                try (InputStream source = Objects.requireNonNull(plugin.getResource(resource), resource)) {
                    var result = new BundledAssetUpdater().install(target, source.readAllBytes(),
                            file.equals("players/" + PLAYER + ".bbmodel") ? Set.of(PLAYER_020_SHA, PLAYER_021_SHA, PLAYER_023_SHA) : Set.of(),
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
