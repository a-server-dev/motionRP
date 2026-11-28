package dev.aserver.motionrp;

import dev.aserver.motionrp.core.BundledAssetUpdater;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.event.*;
import org.bukkit.Bukkit;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Supplies only MotionRP's namespaced item assets to BetterModel's own pack builder. */
final class ControllerAssets implements AutoCloseable {
 private final ModelEventListener listener;
 ControllerAssets(MotionPlugin plugin){
  var files=new LinkedHashMap<String,byte[]>();
  Path root=Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("BetterModel")).getDataFolder().toPath().resolve("motionrp-assets");
  try{
   for(String file:List.of("items/controller.json","models/item/controller.json","textures/item/controller.png")){
    String name="assets/motionrp/"+file;byte[] bytes;
    try(var in=Objects.requireNonNull(plugin.getResource("controller/"+name))){bytes=in.readAllBytes();}
    Path target=root.resolve(name);
    new BundledAssetUpdater().install(target,bytes,Set.of(),plugin.getDataFolder().toPath().resolve("model-backups"),plugin.getDataFolder().toPath().resolve("model-updates"));
    files.put(name,Files.readAllBytes(target));
   }
  }catch(IOException e){throw new UncheckedIOException("Cannot install controller assets",e);}
  listener=BetterModel.platform().eventBus().subscribe(plugin::isEnabled,PluginStartReloadEvent.class,event->{
   // Read the installed files on each rebuild so server-local edits are respected.
   for(var file:files.entrySet()){
    byte[] bytes;
    try{bytes=Files.readAllBytes(root.resolve(file.getKey()));}catch(IOException e){plugin.getLogger().warning("Controller asset unavailable: "+file.getKey()+"; using bundled copy");bytes=file.getValue();}
    final byte[] content=bytes;event.zipper().assets().add(file.getKey(),content.length,()->content);
   }
  });
 }
 @Override public void close(){listener.unregister();}
}
