package dev.aserver.motionrp;

import dev.aserver.motionrp.pack.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;

/** Publishes completed ZIPs off-thread; delivery requires each player's explicit reload. */
final class AutomaticResourcePack implements Listener,AutoCloseable {
    private static final UUID PACK_ID=UUID.nameUUIDFromBytes("MotionRP:server-resource-pack".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private final MotionPlugin plugin;
    private final boolean enabled,required;
    private final String repository,prompt;
    private final Path source,folder;
    private final ScheduledExecutorService worker;
    private final GitHubPackPublisher publisher;
    private final Set<UUID> requested=new HashSet<>();
    private volatile PackSnapshot current;
    private volatile String status;
    private volatile boolean closed;
    private Stamp candidate,processed;
    private long retryAfter;
    private int failures;
    private boolean forcePublish;
    private record Stamp(long size,FileTime modified) {}

    AutomaticResourcePack(MotionPlugin plugin) {
        this.plugin=plugin;var config=plugin.getConfig();
        enabled=config.getBoolean("resource-pack.enabled",false);
        required=config.getBoolean("resource-pack.required",false);
        repository=PackSnapshot.repository(config.getString("resource-pack.repository","a-server-dev/motionRP"));
        prompt=config.getString("resource-pack.prompt","MotionRPのモーション表示用リソースパックです。");
        source=Path.of(config.getString("resource-pack.source-file","plugins/BetterModel/build.zip")).toAbsolutePath().normalize();
        folder=plugin.getDataFolder().toPath();
        if(!enabled) { worker=null;publisher=null;status="無効（resource-pack.enabled=false）";return; }
        publisher=new GitHubPackPublisher();
        worker=Executors.newSingleThreadScheduledExecutor(r->{var thread=new Thread(r,"MotionRP-resource-pack");thread.setDaemon(true);return thread;});
        Bukkit.getPluginManager().registerEvents(this,plugin);status="生成済みパックを確認中";
        worker.execute(()->{ loadCache();check(); });
        worker.scheduleWithFixedDelay(this::check,5,5,TimeUnit.SECONDS);
    }
    String status() { return status; }
    boolean publishNow() {
        if(!enabled || closed) return false;
        worker.execute(()->{ forcePublish=true;processed=null;candidate=null;retryAfter=0;check(); });return true;
    }
    void reload(Player player) {
        if(!enabled || closed){plugin.tell(player,"リソースパック配布が無効です。管理者に連絡してください。");return;}
        if(current!=null){offer(player);return;}
        requested.add(player.getUniqueId());
        plugin.tell(player,"リソースパックの準備待ちです。準備後にあなたへ適用します。");
        worker.execute(this::check);
    }
    private void deliverRequested() {
        for(UUID id:new HashSet<>(requested)){Player player=Bukkit.getPlayer(id);if(player!=null)offer(player);}
        requested.clear();
    }
    private void check() {
        if(closed || System.currentTimeMillis()<retryAfter) return;
        try {
            String token=token();
            if(token.isEmpty()) {
                setStatus("GitHub認証未設定（MOTIONRP_GITHUB_TOKEN または plugins/MotionRP/github-token.txt）");return;
            }
            if(!Files.isRegularFile(source)) { setStatus("BetterModelのZIP生成待ち: "+source.getFileName());return; }
            var attributes=Files.readAttributes(source,java.nio.file.attribute.BasicFileAttributes.class);
            var stamp=new Stamp(attributes.size(),attributes.lastModifiedTime());
            if(stamp.equals(processed)) return;
            if(!stamp.equals(candidate)) { candidate=stamp;return; } // Two equal samples, five seconds apart.
            PackSnapshot snapshot=PackSnapshot.read(source);
            if(!forcePublish && current!=null && current.sha256().equals(snapshot.sha256())) { processed=stamp;setStatus("配布可能: "+snapshot.sha1());return; }
            setStatus("mortionRP.zip をGitHubへ公開中");
            publisher.publish(repository,token,snapshot);
            if(closed) return;
            var latest=Files.readAttributes(source,java.nio.file.attribute.BasicFileAttributes.class);
            if(!stamp.equals(new Stamp(latest.size(),latest.lastModifiedTime()))) {
                candidate=null;processed=null;setStatus("新しいZIPの生成を検知しました。最新版の公開を待ちます。");return;
            }
            saveCache(snapshot);
            current=snapshot;processed=stamp;failures=0;retryAfter=0;
            forcePublish=false;
            setStatus("配布可能: "+snapshot.sha1());
            onMain(this::deliverRequested);
        } catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        catch(Exception failure) {
            if(closed) return;
            failures=Math.min(failures+1,5);retryAfter=System.currentTimeMillis()+Math.min(300,15L*(1L<<failures))*1000;
            // Messages contain only our own paths/status codes; never exception bodies or token values.
            String detail=failure instanceof GitHubPackPublisher.Failure ? failure.getMessage() : failure.getClass().getSimpleName();
            setStatus("自動公開失敗（"+detail+"）。再試行します。");
        }
    }
    private String token() throws IOException {
        String token=System.getenv("MOTIONRP_GITHUB_TOKEN");
        if(token==null || token.isBlank()) {
            Path path=folder.resolve("github-token.txt");
            if(!Files.exists(path)) return "";
            if(Files.size(path)>8192) throw new IOException("Invalid token file");
            token=Files.readString(path);
        }
        token=token.trim();
        if(token.contains("\n") || token.contains("\r")) throw new IOException("Invalid token file");
        return token;
    }
    private void setStatus(String next) {
        if(!next.equals(status)) { status=next;plugin.getLogger().info("Resource pack: "+next); }
    }
    private void loadCache() {
        try {
            Path manifest=folder.resolve("pack-publication.properties");if(!Files.exists(manifest)) return;
            var info=new Properties();try(var input=Files.newInputStream(manifest)) { info.load(input); }
            if(!repository.equals(info.getProperty("repository"))) return;
            var pack=PackSnapshot.read(folder.resolve("mortionRP.zip"));
            if(!pack.sha1().equals(info.getProperty("sha1")) || !pack.sha256().equals(info.getProperty("sha256"))) return;
            current=pack;setStatus("前回公開したパックを配布可能: "+pack.sha1());
            onMain(this::deliverRequested);
        } catch(Exception invalid) { setStatus("前回のパック情報を使用できません。再公開を待ちます。"); }
    }
    private void saveCache(PackSnapshot snapshot) throws IOException {
        Files.createDirectories(folder);
        atomicWrite(folder.resolve("mortionRP.zip"),snapshot.bytes());
        var info=new Properties();info.setProperty("repository",repository);info.setProperty("sha1",snapshot.sha1());info.setProperty("sha256",snapshot.sha256());
        var bytes=new ByteArrayOutputStream();info.store(bytes,"Public pack metadata only; no credentials");
        atomicWrite(folder.resolve("pack-publication.properties"),bytes.toByteArray());
    }
    private static void atomicWrite(Path path,byte[] bytes) throws IOException {
        Path temp=Files.createTempFile(path.getParent(),"pack-",".tmp");
        try {
            Files.write(temp,bytes);
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException fallback) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private void onMain(Runnable action) {
        if(closed || !plugin.isEnabled()) return;
        try { Bukkit.getScheduler().runTask(plugin,()->{ if(!closed) action.run(); }); }
        catch(org.bukkit.plugin.IllegalPluginAccessException stopped) { if(!closed) throw stopped; }
    }
    private void offer(Player player) {
        PackSnapshot pack=current;
        if(closed || pack==null || !player.isOnline()) return;
        player.removeResourcePack(PACK_ID); // Leave server.properties and other plugins' packs alone.
        player.addResourcePack(PACK_ID,pack.url(repository),HexFormat.of().parseHex(pack.sha1()),prompt,required);
        requested.remove(player.getUniqueId());
    }
    @EventHandler public void quit(PlayerQuitEvent event) { requested.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void packStatus(PlayerResourcePackStatusEvent event) {
        if(!PACK_ID.equals(event.getID())) return;
        String state=event.getStatus().name();
        if(state.equals("FAILED_DOWNLOAD") || state.equals("FAILED_RELOAD") || state.equals("INVALID_URL") || state.equals("DECLINED")) {
            plugin.tell(event.getPlayer(),"パックの適用に失敗・拒否されました。サーバーリソースパックを有効にして /motionrp reload を実行してください。");
            plugin.getLogger().warning("Resource pack "+state+" for "+event.getPlayer().getUniqueId());
        }
    }
    @Override public void close() {
        closed=true;HandlerList.unregisterAll(this);
        if(worker!=null) worker.shutdownNow();
        if(publisher!=null) publisher.close();
        requested.clear();
    }
}
