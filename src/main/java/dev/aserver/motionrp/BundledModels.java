package dev.aserver.motionrp;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import org.bukkit.Bukkit;
import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.logging.Level;

/** Installs only missing, namespaced sample assets; custom files are never overwritten. */
public final class BundledModels {
    public static final String PLAYER = "motionrp_rasengan";
    public static final String ORB = "motionrp_rasengan_orb";
    private final MotionPlugin plugin;
    public BundledModels(MotionPlugin plugin) { this.plugin = plugin; }

    public void install() {
        Path root = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("BetterModel")).getDataFolder().toPath();
        boolean changed = false;
        try {
            for (String file : new String[]{"players/" + PLAYER + ".bbmodel", "models/" + ORB + ".bbmodel", "LICENSE-MotionRP-models.txt"}) {
                Path target = root.resolve(file);
                if (Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                String resource = file.startsWith("LICENSE") ? "bettermodel/LICENSE-BetterModel.txt" : "bettermodel/" + file;
                try (InputStream source = Objects.requireNonNull(plugin.getResource(resource), resource)) {
                    Files.copy(source, target);
                }
                changed = true;
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
