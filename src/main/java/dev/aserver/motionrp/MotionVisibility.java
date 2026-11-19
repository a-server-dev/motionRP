package dev.aserver.motionrp;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Opt-in for the original MotionRP scenes; sports celebrations are independent. */
final class MotionVisibility implements Listener {
    private final NamespacedKey key;
    private final Set<UUID> enabled=ConcurrentHashMap.newKeySet();
    private volatile double range;
    private final MotionPlugin plugin;
    private boolean creatingLightning;
    private final Set<org.bukkit.entity.LightningStrike> lightning=new HashSet<>();
    org.bukkit.entity.LightningStrike lightning(Location at,Collection<Player> viewers){
        creatingLightning=true;org.bukkit.entity.LightningStrike bolt;
        try{bolt=at.getWorld().strikeLightningEffect(at);}finally{creatingLightning=false;}
        bolt.setSilent(true);lightning.removeIf(b->!b.isValid());lightning.add(bolt);
        for(Player p:viewers)p.showEntity(plugin,bolt);return bolt;
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void lightning(org.bukkit.event.weather.LightningStrikeEvent e){if(creatingLightning){e.getLightning().setVisibleByDefault(false);e.getLightning().setSilent(true);}}

    MotionVisibility(MotionPlugin plugin){this.plugin=plugin;key=new NamespacedKey(plugin,"view_enabled");reload(plugin);Bukkit.getOnlinePlayers().forEach(this::load);}
    void reload(MotionPlugin plugin){double r=plugin.getConfig().getDouble("visibility.range",48);range=Double.isFinite(r)?Math.clamp(r,8,256):48;}
    boolean enabled(UUID id){return enabled.contains(id);}
    double range(double sceneRange){return Math.min(range,sceneRange);}
    void set(Player player,boolean value){
        if(value)enabled.add(player.getUniqueId());else enabled.remove(player.getUniqueId());
        player.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)(value?1:0));player.saveData();
        lightning.removeIf(b->!b.isValid());
        for(var bolt:lightning){if(value && bolt.getWorld()==player.getWorld() && bolt.getLocation().distanceSquared(player.getLocation())<=range*range)player.showEntity(plugin,bolt);else player.hideEntity(plugin,bolt);}

    }
    private void load(Player p){if(p.getPersistentDataContainer().getOrDefault(key,PersistentDataType.BYTE,(byte)0)==1)enabled.add(p.getUniqueId());else enabled.remove(p.getUniqueId());}
    @EventHandler public void join(PlayerJoinEvent e){load(e.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent e){enabled.remove(e.getPlayer().getUniqueId());}
}
