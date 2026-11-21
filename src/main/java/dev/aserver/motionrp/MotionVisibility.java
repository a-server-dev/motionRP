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
    private final dev.aserver.motionrp.core.ViewingNoticeGate noticeGate=new dev.aserver.motionrp.core.ViewingNoticeGate();
    void playbackStarted(Player actor,boolean filtered){
        var center=actor.getLocation();double limit=range(256);long now=System.nanoTime()/1_000_000;
        for(Player viewer:Bukkit.getOnlinePlayers()){
            if(viewer.isDead()||viewer.getWorld()!=actor.getWorld()||!viewer.canSee(actor)||viewer.getLocation().distanceSquared(center)>limit*limit)continue;
            boolean off=filtered&&!enabled(viewer.getUniqueId());var pack=plugin.packApplications().notice(viewer.getUniqueId(),plugin.currentPackHash());
            if(!off&&pack==dev.aserver.motionrp.core.PackApplicationState.Notice.NONE)continue;
            if(!noticeGate.allow(viewer.getUniqueId(),now))continue;
            var message=net.kyori.adventure.text.Component.text("[MotionRP] 近くでモーションが再生されています。",net.kyori.adventure.text.format.NamedTextColor.GOLD);
            if(off)message=message.append(net.kyori.adventure.text.Component.text(" 演出表示がOFFです。",net.kyori.adventure.text.format.NamedTextColor.YELLOW)).append(button("[表示ON]","/motionrp view on"));
            if(pack!=dev.aserver.motionrp.core.PackApplicationState.Notice.NONE)message=message.append(net.kyori.adventure.text.Component.text(pack==dev.aserver.motionrp.core.PackApplicationState.Notice.OUTDATED?" MotionRPの最新リソパが未適用です。":" MotionRP配布リソパが未適用です（適用失敗・拒否・解除を検知）。",net.kyori.adventure.text.format.NamedTextColor.YELLOW)).append(button("[リソパ再適用]","/motionrp reload"));
            viewer.sendMessage(message);
        }
    }
    private static net.kyori.adventure.text.Component button(String text,String command){return net.kyori.adventure.text.Component.text(" "+text,net.kyori.adventure.text.format.NamedTextColor.AQUA).clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command)).hoverEvent(net.kyori.adventure.text.Component.text(command));}
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
    @EventHandler public void quit(PlayerQuitEvent e){enabled.remove(e.getPlayer().getUniqueId());noticeGate.forget(e.getPlayer().getUniqueId());plugin.packApplications().forget(e.getPlayer().getUniqueId());}
}
