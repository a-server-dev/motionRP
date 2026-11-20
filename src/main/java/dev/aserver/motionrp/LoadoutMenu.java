package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

public final class LoadoutMenu implements Listener {
    private final MotionPlugin plugin;
    private final PlayerLoadouts loadouts;
    private final MotionItems items;

    public LoadoutMenu(MotionPlugin plugin, PlayerLoadouts loadouts, MotionItems items) {
        this.plugin = plugin;
        this.loadouts = loadouts;
        this.items = items;
    }

    public static final class Holder implements InventoryHolder {
        private final UUID owner;
        private final String setId;
        private final int heldSlot;
        private final Inventory inventory;
        private Holder(Player player,String setId) {
            this.setId=setId;heldSlot=player.getInventory().getHeldItemSlot();
            owner = player.getUniqueId();
            inventory = Bukkit.createInventory(this, Loadout.SIZE, Component.text("モーション登録（下のアイテムで登録）"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    public void open(Player player) {
        if (!player.hasPermission("motionrp.use")) { plugin.tell(player, "この操作の権限がありません。"); return; }
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof Holder) return;
        if (!player.getItemOnCursor().isEmpty()) {
            plugin.tell(player, "カーソルのアイテムを所持品に戻してから開いてください。"); return;
        }
        try {
            Loadout state = loadouts.read(player);
            plugin.playback().stop(player.getUniqueId());
            Holder holder = new Holder(player,items.setId(player.getInventory().getItemInMainHand()));
            render(holder, state);
            player.openInventory(holder.inventory);
            plugin.tell(player, "下のモーションアイテムをクリック: 1個登録 / 上の登録枠をクリック: 返却");
        } catch (RuntimeException invalid) { plugin.dataError(player, invalid); }
    }

    private void render(Holder holder, Loadout state) {
        for (int index = 0; index < Loadout.SIZE; index++) {
            var stored = state.slot(index);
            ItemStack display;
            if (stored == null) {
                display = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                var meta = display.getItemMeta();
                meta.displayName(MotionItems.text((index + 1) + ": 空き枠", NamedTextColor.GRAY));
                display.setItemMeta(meta);
            } else {
                display = loadouts.item(stored);
                var meta = display.getItemMeta();
                List<Component> lore = new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of()));
                lore.add(MotionItems.text("クリックで所持品へ返却", NamedTextColor.YELLOW));
                if (index == state.selected()) lore.add(MotionItems.text("▶ 選択中", NamedTextColor.GREEN));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            holder.inventory.setItem(index, display);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        // Every vanilla move is denied, including double-click, number keys, drops,
        // offhand swap and creative clone. Display items are never transferable.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) return;
        if (!player.hasPermission("motionrp.use")) return;
        if (!player.getItemOnCursor().isEmpty()) { plugin.tell(player, "先にカーソルのアイテムを戻してください。"); return; }
        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) return;
        try {
            if(holder.heldSlot!=player.getInventory().getHeldItemSlot()||!Objects.equals(holder.setId,items.setId(player.getInventory().getItemInMainHand()))){player.closeInventory();plugin.tell(player,"持ち替えたため登録GUIを閉じました。セットを持って開き直してください。");return;}
            Loadout state = loadouts.read(player);
            if (event.getClickedInventory() == holder.inventory) {
                int index = event.getSlot();
                if (state.slot(index) == null) return;
                ItemStack returned = loadouts.item(state.slot(index));
                if (!fits(player.getInventory(), returned)) { plugin.tell(player, "所持品に空きがありません。"); return; }
                transact(player, state, state.withdraw(index), () -> {
                    if (!player.getInventory().addItem(returned).isEmpty()) throw new IllegalStateException("Inventory capacity changed");
                });
            } else if (event.getClickedInventory() == player.getInventory()) {
                int slot = event.getSlot();
                if (slot < 0 || slot >= 36) return;
                ItemStack source = player.getInventory().getItem(slot);
                String id = items.motionId(source);
                if (id == null) return;
                if (!plugin.catalog().motions().containsKey(id)) { plugin.tell(player, "このモーションは現在設定されていません。"); return; }
                int target = state.firstEmpty();
                if (target < 0) { plugin.tell(player, "登録枠が満杯です。先に上のアイテムを取り外してください。"); return; }
                Loadout next = state.deposit(target, loadouts.stored(source));
                ItemStack remainder = source.clone();
                remainder.setAmount(source.getAmount() - 1);
                transact(player, state, next, () -> player.getInventory().setItem(slot, remainder.isEmpty() ? null : remainder));
            }
            render(holder, loadouts.read(player));
        } catch (RuntimeException invalid) { plugin.dataError(player, invalid); }
    }

    private void transact(Player player, Loadout before, Loadout after, Runnable changeInventory) {
        ItemStack[] inventory = Arrays.stream(player.getInventory().getStorageContents())
                .map(i -> i == null ? null : i.clone()).toArray(ItemStack[]::new);
        try {
            changeInventory.run();
            loadouts.save(player, after);
        } catch (RuntimeException failure) {
            player.getInventory().setStorageContents(inventory);
            loadouts.write(player, before);
            throw failure;
        }
    }

    static boolean fits(PlayerInventory inventory, ItemStack item) {
        int capacity = 0;
        for (ItemStack existing : inventory.getStorageContents()) {
            if (existing == null || existing.isEmpty()) capacity += item.getMaxStackSize();
            else if (existing.isSimilar(item)) capacity += Math.max(0, existing.getMaxStackSize() - existing.getAmount());
            if (capacity >= item.getAmount()) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @EventHandler public void close(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) event.getInventory().clear();
    }
}
