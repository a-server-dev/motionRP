package dev.aserver.motionrp.core;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Replaces only exact known shipped files, preserving both backups and user edits. */
public final class BundledAssetUpdater {
    public enum Result { INSTALLED, UNCHANGED, UPDATED, CUSTOM_PRESERVED }

    public Result install(Path target, byte[] incoming, Set<String> previousHashes, Path backups, Path updates) throws IOException {
        Files.createDirectories(target.getParent());
        if (!Files.exists(target)) {
            writeAtomic(target, incoming);
            return Result.INSTALLED;
        }
        byte[] existing = Files.readAllBytes(target);
        if (Arrays.equals(existing, incoming)) return Result.UNCHANGED;
        String hash = sha256(existing);
        if (!previousHashes.contains(hash)) {
            preserve(updates.resolve(target.getFileName() + "." + sha256(incoming) + ".update"), incoming);
            return Result.CUSTOM_PRESERVED;
        }
        preserve(backups.resolve(target.getFileName() + "." + hash + ".bak"), existing);
        writeAtomic(target, incoming);
        return Result.UPDATED;
    }

    public static String sha256(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private void preserve(Path path, byte[] content) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            if (!Arrays.equals(Files.readAllBytes(path), content)) throw new IOException("Existing backup/update differs: " + path);
        } else Files.write(path, content, StandardOpenOption.CREATE_NEW);
    }

    private void writeAtomic(Path path, byte[] content) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, content);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
