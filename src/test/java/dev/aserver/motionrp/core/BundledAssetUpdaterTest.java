package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BundledAssetUpdaterTest {
    @TempDir Path root;
    private final BundledAssetUpdater updater = new BundledAssetUpdater();
    private byte[] bytes(String value) { return value.getBytes(java.nio.charset.StandardCharsets.UTF_8); }

    @Test void installsMissingAssetAndRepeatedInstallationIsUnchanged() throws Exception {
        Path target = root.resolve("models/player.bbmodel");
        assertEquals(BundledAssetUpdater.Result.INSTALLED, updater.install(target, bytes("new"), Set.of(), root.resolve("backups"), root.resolve("updates")));
        assertEquals(BundledAssetUpdater.Result.UNCHANGED, updater.install(target, bytes("new"), Set.of(), root.resolve("backups"), root.resolve("updates")));
        assertArrayEquals(bytes("new"), Files.readAllBytes(target));
        assertFalse(Files.exists(root.resolve("backups")));
    }

    @Test void knownShippedAssetIsBackedUpBeforeUpgradeAndNeverBackedUpAgain() throws Exception {
        Path target = root.resolve("player.bbmodel");
        Files.write(target, bytes("old shipped"));
        String hash = BundledAssetUpdater.sha256(bytes("old shipped"));
        assertEquals(BundledAssetUpdater.Result.UPDATED, updater.install(target, bytes("fixed"), Set.of(hash), root.resolve("backups"), root.resolve("updates")));
        assertArrayEquals(bytes("old shipped"), Files.readAllBytes(root.resolve("backups/player.bbmodel." + hash + ".bak")));
        assertArrayEquals(bytes("fixed"), Files.readAllBytes(target));
        assertEquals(BundledAssetUpdater.Result.UNCHANGED, updater.install(target, bytes("fixed"), Set.of(hash), root.resolve("backups"), root.resolve("updates")));
        try (var files = Files.list(root.resolve("backups"))) { assertEquals(1, files.count()); }
    }

    @Test void customEditsArePreservedAndNewAssetIsExportedForReview() throws Exception {
        Path target = root.resolve("player.bbmodel");
        Files.write(target, bytes("custom pose"));
        assertEquals(BundledAssetUpdater.Result.CUSTOM_PRESERVED, updater.install(target, bytes("fixed"), Set.of("different"), root.resolve("backups"), root.resolve("updates")));
        assertArrayEquals(bytes("custom pose"), Files.readAllBytes(target));
        String hash = BundledAssetUpdater.sha256(bytes("fixed"));
        assertArrayEquals(bytes("fixed"), Files.readAllBytes(root.resolve("updates/player.bbmodel." + hash + ".update")));
        assertFalse(Files.exists(root.resolve("backups")));
    }

    @Test void conflictingBackupBlocksReplacementInsteadOfDestroyingEitherFile() throws Exception {
        Path target = root.resolve("player.bbmodel");
        Files.write(target, bytes("old"));
        String hash = BundledAssetUpdater.sha256(bytes("old"));
        Path backup = root.resolve("backups/player.bbmodel." + hash + ".bak");
        Files.createDirectories(backup.getParent()); Files.write(backup, bytes("user file"));
        assertThrows(IOException.class, () -> updater.install(target, bytes("fixed"), Set.of(hash), root.resolve("backups"), root.resolve("updates")));
        assertArrayEquals(bytes("old"), Files.readAllBytes(target));
        assertArrayEquals(bytes("user file"), Files.readAllBytes(backup));
    }
}
