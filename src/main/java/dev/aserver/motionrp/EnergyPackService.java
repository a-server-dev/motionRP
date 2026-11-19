package dev.aserver.motionrp;

import dev.aserver.motionrp.pack.EnergyPackLighting;
import java.nio.file.*;
import java.util.concurrent.*;

/** Apply full-bright energy models to the server's manual pack as well as automatic delivery. */
final class EnergyPackService implements AutoCloseable {
    private final ScheduledExecutorService worker;private Path file;private long size=-1,modified=-1,lastWarning;
    EnergyPackService(MotionPlugin plugin){
        file=org.bukkit.Bukkit.getPluginManager().getPlugin("BetterModel").getDataFolder().toPath().resolve("build.zip");
        worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"MotionRP-energy-pack");t.setDaemon(true);return t;});
        worker.scheduleWithFixedDelay(()->{
            try{if(!Files.isRegularFile(file))return;var a=Files.readAttributes(file,java.nio.file.attribute.BasicFileAttributes.class);if(a.size()==size && a.lastModifiedTime().toMillis()==modified)return;
                if(System.currentTimeMillis()-a.lastModifiedTime().toMillis()<500)return;
                boolean changed=EnergyPackLighting.prepare(file);a=Files.readAttributes(file,java.nio.file.attribute.BasicFileAttributes.class);size=a.size();modified=a.lastModifiedTime().toMillis();
                if(changed)plugin.getLogger().info("Energy pack lighting prepared: full-bright MotionRP energy models");
            }catch(Exception e){long now=System.currentTimeMillis();if(now-lastWarning>30000){lastWarning=now;plugin.getLogger().warning("Energy pack preparation will retry: "+e.getMessage());}}
        },2,1,TimeUnit.SECONDS);
    }
    @Override public void close(){worker.shutdownNow();}
}
