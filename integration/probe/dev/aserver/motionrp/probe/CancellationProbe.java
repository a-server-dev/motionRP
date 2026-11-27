package dev.aserver.motionrp.probe;
import dev.aserver.motionrp.MotionPlugin;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
/** Only installed in the disposable integration fixture, never in the product JAR. */
public final class CancellationProbe extends JavaPlugin implements org.bukkit.event.Listener {
 private final java.util.Map<java.util.UUID,Object> saved=new java.util.HashMap<>();
 @SuppressWarnings("unchecked") private java.util.Map<java.util.UUID,Object> channels(){try{var manager=BetterModel.platform().manager(kr.toxicity.model.api.manager.PlayerManager.class);var field=manager.getClass().getDeclaredField("playerMap");field.setAccessible(true);return (java.util.Map<java.util.UUID,Object>)field.get(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
 @Override public void onDisable(){saved.forEach((id,handler)->channels().putIfAbsent(id,handler));saved.clear();}
 private final java.util.Set<java.util.UUID> rotate=new java.util.HashSet<>();
 @Override public void onEnable(){Bukkit.getPluginManager().registerEvents(this,this);}
 @org.bukkit.event.EventHandler(priority=org.bukkit.event.EventPriority.HIGHEST,ignoreCancelled=true)
 public void rotate(PlayerTeleportEvent e){if(rotate.contains(e.getPlayer().getUniqueId())&&e.getCause()==PlayerTeleportEvent.TeleportCause.PLUGIN){var to=e.getTo().clone();to.setYaw(to.getYaw()+15);to.setPitch(5);e.setTo(to);}}

 @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){
  if(sender instanceof Player||args.length!=2)return false;
  Player p=Bukkit.getPlayerExact(args[1]);if(p==null)return false;var plugin=JavaPlugin.getPlugin(MotionPlugin.class);
  switch(args[0]){
   case "orphan" -> {Object old=channels().remove(p.getUniqueId());if(old==null)throw new IllegalStateException("Viewer was not registered");saved.put(p.getUniqueId(),old);}
   case "restore" -> {Object old=saved.remove(p.getUniqueId());if(old==null)throw new IllegalStateException("No saved handler");channels().putIfAbsent(p.getUniqueId(),old);}
   case "duplicate" -> {boolean reproduced=false;try{BetterModel.platform().manager(kr.toxicity.model.api.manager.PlayerManager.class).player(kr.toxicity.model.api.bukkit.platform.BukkitAdapter.adapt(p));}catch(IllegalArgumentException e){if(!e.getMessage().contains("Duplicate handler name: bettermodel_channel_handler"))throw e;reproduced=true;}if(!reproduced)throw new IllegalStateException("Expected duplicate handler error");getLogger().info("CHANNEL_DUPLICATE_REPRODUCED");}
   case "zero" -> Bukkit.getPluginManager().callEvent(new EntityDamageEvent(p,EntityDamageEvent.DamageCause.CUSTOM,0));
   case "hurt" -> Bukkit.getPluginManager().callEvent(new EntityDamageEvent(p,EntityDamageEvent.DamageCause.CUSTOM,1));
   case "sameheld" -> Bukkit.getPluginManager().callEvent(new PlayerItemHeldEvent(p,p.getInventory().getHeldItemSlot(),p.getInventory().getHeldItemSlot()));
   case "tiny" -> p.teleport(p.getLocation().add(.02,0,0),PlayerTeleportEvent.TeleportCause.PLUGIN);
   case "look" -> {var at=p.getLocation();at.setYaw(at.getYaw()+10);p.teleport(at,PlayerTeleportEvent.TeleportCause.PLUGIN);}
   case "far" -> p.teleport(p.getLocation().add(2,0,0),PlayerTeleportEvent.TeleportCause.PLUGIN);
   case "fast" -> {var registry=BetterModel.registryOrNull(p.getUniqueId());if(registry!=null)for(var t:registry.trackers())t.replace("entei","entei",AnimationModifier.builder().speed(()->100f).build());}
   case "close" -> {var registry=BetterModel.registryOrNull(p.getUniqueId());if(registry!=null)for(var t:java.util.List.copyOf(registry.trackers()))t.close();}
   case "rotate" -> rotate.add(p.getUniqueId());
   case "unrotate" -> rotate.remove(p.getUniqueId());
   case "state" -> {}
   default -> {return false;}
  }
  getLogger().info("CANCEL_PROBE "+args[0]+" "+p.getName()+" active="+plugin.playback().active(p.getUniqueId())+" last="+plugin.playback().lastStop(p.getUniqueId()));return true;
 }
}
