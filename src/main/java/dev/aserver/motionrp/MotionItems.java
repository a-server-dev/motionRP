package dev.aserver.motionrp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public final class MotionItems {
    private final NamespacedKey kind;
    private final NamespacedKey motion;

    public MotionItems(MotionPlugin plugin) {
        kind = new NamespacedKey(plugin, "kind");
        motion = new NamespacedKey(plugin, "motion_id");
    }

    public ItemStack controller(MotionCatalog catalog) {
        ItemStack stack = new ItemStack(Material.CARROT_ON_A_STICK);
        var meta = stack.getItemMeta();
        meta.displayName(text(catalog.controllerName(), NamedTextColor.GOLD));
        meta.lore(List.of(text("右クリック: 招待の承諾 / 登録GUI", NamedTextColor.GRAY),
                text("左クリック: 再生・二人用招待 / 再クリック: 終了", NamedTextColor.GRAY),
                text("シフト＋左クリック: 次のモーション", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(kind, PersistentDataType.STRING, "controller");
        meta.setMaxStackSize(1);
        if (catalog.controllerModel() != null) meta.setItemModel(catalog.controllerModel());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack token(MotionCatalog.Definition definition, int amount) {
        if (amount < 1 || amount > 64) throw new IllegalArgumentException("Amount must be 1..64");
        ItemStack stack = new ItemStack(Material.PAPER, amount);
        var meta = stack.getItemMeta();
        meta.displayName(text(definition.name(), NamedTextColor.AQUA));
        var lore=new java.util.ArrayList<Component>(List.of(text("GUIでこのアイテムをクリックして1個登録", NamedTextColor.GRAY),
                text("モーション: " + definition.id(), NamedTextColor.DARK_GRAY)));
        if(dev.aserver.motionrp.core.PairMotion.fromEffect(definition.effect()).isPresent())lore.add(text("二人用: 左クリックで最寄りの相手へ招待（8ブロック以内）",NamedTextColor.YELLOW));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(kind, PersistentDataType.STRING, "motion");
        meta.getPersistentDataContainer().set(motion, PersistentDataType.STRING, definition.id());
        // No unique serial or owner tag: identical tokens remain stackable.
        meta.setMaxStackSize(64);
        if (definition.itemModel() != null) meta.setItemModel(definition.itemModel());
        stack.setItemMeta(meta);
        return stack;
    }

    public void refreshName(ItemStack stack) {
        String id=motionId(stack);if(id==null)return;
        var meta=stack.getItemMeta();if(meta.displayName()==null)return;
        String old=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        String next=MotionNames.updated(id,old);
        if(!old.equals(next)){meta.displayName(text(next,NamedTextColor.AQUA));stack.setItemMeta(meta);}
    }
    public void refreshNames(org.bukkit.entity.Player player) {
        for(ItemStack stack:player.getInventory().getContents())if(stack!=null)refreshName(stack);
    }

    public boolean controller(ItemStack stack) { return isKind(stack, "controller"); }
    public String motionId(ItemStack stack) {
        if (!isKind(stack, "motion")) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(motion, PersistentDataType.STRING);
    }
    private boolean isKind(ItemStack stack, String expected) {
        return stack != null && !stack.isEmpty() && stack.hasItemMeta()
                && expected.equals(stack.getItemMeta().getPersistentDataContainer().get(kind, PersistentDataType.STRING));
    }
    public static Component text(String value, NamedTextColor color) {
        return Component.text(value, color).decoration(TextDecoration.ITALIC, false);
    }
}
