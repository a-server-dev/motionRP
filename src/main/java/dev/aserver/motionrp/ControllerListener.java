package dev.aserver.motionrp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import java.util.*;

public final class ControllerListener implements Listener {
    private final MotionPlugin plugin;
    private final Map<UUID, Integer> lastInput = new HashMap<>();

    public ControllerListener(MotionPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !plugin.items().controller(event.getItem())) return;
        event.setCancelled(true);
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            right(event.getPlayer());
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            left(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interactEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && plugin.items().controller(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            right(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interactAtEntity(PlayerInteractAtEntityEvent event) { interactEntity(event); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void attack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && plugin.items().controller(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            left(player);
        }
    }

    private void right(Player player){
        if(!accept(player))return;
        if(plugin.fusion().controllerAccept(player))return;
        if(player.isSneaking())plugin.selection().open(player);else plugin.menu().open(player);
    }

    private boolean accept(Player player) {
        if (!player.hasPermission("motionrp.use")) { plugin.tell(player, "この操作の権限がありません。"); return false; }
        var held=player.getInventory().getItemInMainHand();plugin.items().refreshName(held);player.getInventory().setItemInMainHand(held);
        int now = Bukkit.getCurrentTick();
        Integer last = lastInput.get(player.getUniqueId());
        if (last != null && now - last < 4) return false;
        lastInput.put(player.getUniqueId(), now);
        return true;
    }

    private void left(Player player) {
        if (!accept(player)) return;
        try {
            var state = plugin.loadouts().read(player);
            if (plugin.getConfig().getBoolean("debug-input", false)) {
                plugin.getLogger().info("Controller input: tick=" + Bukkit.getCurrentTick()
                        + " sneaking=" + player.isSneaking() + " selected=" + state.selected());
            }
            if (player.isSneaking()) {
                var next = state.next();
                plugin.loadouts().save(player, next);
                var motion = next.current();
                if (motion == null) { plugin.tell(player, "モーションが未登録です。右クリックで登録してください。"); return; }
                var definition = plugin.catalog().motions().get(motion.id());
                String name = definition == null ? motion.id() : definition.name();
                player.sendActionBar(Component.text("選択 " + (next.selected() + 1) + "/"+dev.aserver.motionrp.core.Loadout.SIZE+": " + name, NamedTextColor.AQUA));
                return;
            }
            if (plugin.playback().active(player.getUniqueId())) { plugin.playback().stop(player.getUniqueId(),"クリックで終了"); return; }
            var motion = state.current();
            if (motion == null) { plugin.tell(player, "モーションが未登録です。右クリックで登録してください。"); return; }
            var definition = plugin.catalog().motions().get(motion.id());
            if (definition == null) { plugin.tell(player, "このモーションは現在設定されていません。GUIから取り外せます。"); return; }
            plugin.playback().play(player, definition);
        } catch (RuntimeException invalid) { plugin.dataError(player, invalid); }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || !event.hasChangedPosition()
                || !plugin.playback().active(event.getPlayer().getUniqueId())
                || plugin.playback().followsMovement(event.getPlayer())
                || plugin.playback().ownsMove(event.getPlayer(), event.getFrom(), event.getTo())) return;
        // Keep camera rotation, but reject walking/jumping without ending playback.
        var locked = event.getFrom().clone();
        locked.setYaw(event.getTo().getYaw());
        locked.setPitch(event.getTo().getPitch());
        // setTo would make Paper perform a Bukkit teleport, which would then
        // trigger the external-teleport cancellation handler. Cancelled movement
        // uses Paper's direct correction path instead.
        event.setFrom(locked);
        event.setCancelled(true);
        event.getPlayer().setVelocity(new org.bukkit.util.Vector());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void damage(EntityDamageEvent event) {
        if (event.getFinalDamage()>0 && event.getEntity() instanceof Player player) plugin.playback().stop(player.getUniqueId(),"ダメージ ("+event.getCause()+")");
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        plugin.playback().stop(event.getPlayer().getUniqueId(),"退出");
        lastInput.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void join(PlayerJoinEvent event) { plugin.items().refreshNames(event.getPlayer()); TitanState.recover(plugin,event.getPlayer());AerialAnchor.recover(plugin,event.getPlayer()); }
    @EventHandler public void respawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin,()->{TitanState.recover(plugin,event.getPlayer());AerialAnchor.recover(plugin,event.getPlayer());});
    }
    @EventHandler public void death(PlayerDeathEvent event) { plugin.playback().stop(event.getEntity().getUniqueId(),"死亡"); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) {
        var from=event.getFrom();var to=event.getTo();
        // Rotation-only teleports and tiny server corrections are not leaving the scene.
        if(from.getWorld()==to.getWorld()&&from.distanceSquared(to)<=.05*.05)return;
        if (!plugin.playback().ownsMove(event.getPlayer(), from, to)) {
            plugin.playback().externalTeleport(event.getPlayer());
            if(plugin.fusion()!=null)plugin.fusion().externalTeleport(event.getPlayer().getUniqueId());
            plugin.playback().stop(event.getPlayer().getUniqueId(),"外部テレポート ("+event.getCause()+")");
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void held(PlayerItemHeldEvent event) { if (event.getPreviousSlot()!=event.getNewSlot() && plugin.playback().active(event.getPlayer().getUniqueId())) plugin.playback().stop(event.getPlayer().getUniqueId(),"ホットバーの持ち替え"); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void swap(PlayerSwapHandItemsEvent event) { if (plugin.playback().active(event.getPlayer().getUniqueId())) plugin.playback().stop(event.getPlayer().getUniqueId(),"左右の手の持ち替え"); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void breakBlock(BlockBreakEvent event) {
        if (plugin.items().controller(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void damageBlock(BlockDamageEvent event) {
        if (plugin.items().controller(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void itemDamage(PlayerItemDamageEvent event) {
        if (plugin.items().controller(event.getItem())) event.setCancelled(true);
    }
}
