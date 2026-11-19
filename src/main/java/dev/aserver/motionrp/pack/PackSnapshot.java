package dev.aserver.motionrp.pack;

import com.google.gson.JsonParser;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** A validated immutable copy; a build in progress is never uploaded. */
public record PackSnapshot(byte[] bytes,String sha1,String sha256) {
    public static final int MAX_BYTES=100*1024*1024;
    public PackSnapshot { bytes=bytes.clone(); }
    @Override public byte[] bytes() { return bytes.clone(); }
    public static PackSnapshot read(Path path) throws IOException {
        EnergyPackLighting.prepare(path);
        var before=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class);
        if(before.size()>MAX_BYTES) throw new IOException("Resource pack exceeds 100 MiB");
        byte[] bytes;
        try(var input=Files.newInputStream(path)) { bytes=input.readNBytes(MAX_BYTES+1); }
        if(bytes.length>MAX_BYTES) throw new IOException("Resource pack exceeds 100 MiB");
        try(var central=new ZipFile(path.toFile())) {
            if(central.getEntry("pack.mcmeta")==null) throw new IOException("pack.mcmeta is missing");
        }
        boolean metadata=false;long expanded=0;
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;) {
                byte[] buffer=new byte[8192];var meta=new ByteArrayOutputStream();
                for(int n;(n=zip.read(buffer))!=-1;) {
                    expanded+=n;if(expanded>512L*1024*1024) throw new IOException("Expanded pack exceeds 512 MiB");
                    if(entry.getName().equals("pack.mcmeta")) {
                        if(meta.size()+n>64*1024) throw new IOException("pack.mcmeta is too large");meta.write(buffer,0,n);
                    }
                }
                if(entry.getName().equals("pack.mcmeta")) {
                    try { metadata=JsonParser.parseString(meta.toString(java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().has("pack"); }
                    catch(RuntimeException invalid) { throw new IOException("Invalid pack.mcmeta"); }
                }
            }
        }
        var after=Files.readAttributes(path,java.nio.file.attribute.BasicFileAttributes.class);
        if(!metadata || before.size()!=after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime()))
            throw new IOException("Pack is incomplete or still changing");
        return new PackSnapshot(bytes,digest("SHA-1",bytes),digest("SHA-256",bytes));
    }
    public static String digest(String algorithm,byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes)); }
        catch(NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    public String tag() { return "motionrp-pack-"+sha256; }
    public static String repository(String value) {
        if(value==null || !value.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+") || value.contains(".."))
            throw new IllegalArgumentException("resource-pack.repository must be owner/repository");
        return value;
    }
    public String url(String repository) { return "https://github.com/"+repository(repository)+"/releases/download/"+tag()+"/mortionRP.zip"; }
}
