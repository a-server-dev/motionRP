package dev.aserver.motionrp;

import com.google.gson.JsonObject;
import dev.aserver.motionrp.pack.EnergyPackLighting;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitItemStack;
import kr.toxicity.model.api.event.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Records actual renderer item references before BetterModel writes its pack. */
final class EnergyPackAssets implements AutoCloseable {
 private final Map<String,String> targets=new ConcurrentHashMap<>();
 private final ModelEventListener start,imported;
 EnergyPackAssets(MotionPlugin plugin){
  var bus=BetterModel.platform().eventBus();
  start=bus.subscribe(plugin::isEnabled,PluginStartReloadEvent.class,event->{
   targets.clear();
   event.zipper().assets().add(EnergyPackLighting.MANIFEST,()->{
    var root=new JsonObject();root.addProperty("version",1);var items=new JsonObject();
    new TreeMap<>(targets).forEach(items::addProperty);root.add("items",items);
    return root.toString().getBytes(StandardCharsets.UTF_8);
   });
  });
  imported=bus.subscribe(plugin::isEnabled,ModelImportedEvent.class,event->{
   String kind=EnergyPackLighting.targetKind(event.renderer().name());if(kind==null)return;
   event.renderer().flatten().forEach(group->{
    if(group.getItemStack().itemStack() instanceof BukkitItemStack item){
     var stack=item.source();if(!stack.hasItemMeta())return;
     var key=stack.getItemMeta().getItemModel();if(key!=null)targets.put(key.toString(),kind);
    }
   });
  });
 }
 @Override public void close(){start.unregister();imported.unregister();targets.clear();}
}
