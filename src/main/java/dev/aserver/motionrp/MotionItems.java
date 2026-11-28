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
    private static final String CONTROLLER_HINT="/motionrp give controller で専用セットアイテムを入手できます";
    private final MotionPlugin plugin;
    private final NamespacedKey kind;
    private final NamespacedKey motion;
    private final NamespacedKey controllerData,controllerId;

    public MotionItems(MotionPlugin plugin) {
        this.plugin=plugin;
        kind = new NamespacedKey(plugin, "kind");
        motion = new NamespacedKey(plugin, "motion_id");
        controllerData=new NamespacedKey(plugin,"controller_loadout_v1");controllerId=new NamespacedKey(plugin,"controller_set_id");
    }

    public ItemStack controller(MotionCatalog catalog) {
        ItemStack stack = new ItemStack(Material.AMETHYST_SHARD);
        var meta = stack.getItemMeta();
        meta.displayName(text(catalog.controllerName(), NamedTextColor.GOLD));
        meta.lore(List.of(text("右クリック: 招待の承諾 / 登録GUI", NamedTextColor.GRAY),
                text("左クリック: 再生・二人用招待 / 再クリック: 終了", NamedTextColor.GRAY),
                text("シフト＋右クリック: 選択一覧・お気に入り", NamedTextColor.AQUA),
                text("シフト＋左クリック: 次のモーション", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(kind, PersistentDataType.STRING, "controller");
        meta.setMaxStackSize(1);
        meta.setItemModel(catalog.controllerModel()==null?NamespacedKey.fromString("motionrp:controller"):catalog.controllerModel());
        stack.setItemMeta(meta);
        return stack;
    }

    public List<ItemStack> completeControllers(MotionCatalog catalog){
        var definitions=new java.util.ArrayList<>(catalog.motions().values().stream().filter(MotionCatalog.Definition::obtainable).toList());var result=new java.util.ArrayList<ItemStack>();
        int pages=Math.max(1,(definitions.size()+dev.aserver.motionrp.core.Loadout.SIZE-1)/dev.aserver.motionrp.core.Loadout.SIZE);
        for(int page=0;page<pages;page++){
            var subset=new java.util.LinkedHashMap<String,MotionCatalog.Definition>();
            for(var d:definitions.subList(page*dev.aserver.motionrp.core.Loadout.SIZE,Math.min(definitions.size(),(page+1)*dev.aserver.motionrp.core.Loadout.SIZE)))subset.put(d.id(),d);
            result.add(completeController(new MotionCatalog(subset,catalog.controllerName()+(pages>1?"（"+(page+1)+"/"+pages+"）":""),catalog.controllerModel())));
        }
        return result;
    }
    public ItemStack completeController(MotionCatalog catalog) {
        if(catalog.motions().values().stream().filter(MotionCatalog.Definition::obtainable).count()>dev.aserver.motionrp.core.Loadout.SIZE)throw new IllegalArgumentException("全種セットは54種類までです。現在の設定数: "+catalog.motions().size());
        var state=dev.aserver.motionrp.core.Loadout.empty();
        for(var d:catalog.motions().values())if(d.obtainable())state=state.deposit(state.firstEmpty(),new dev.aserver.motionrp.core.StoredMotion(d.id(),token(d,1).serializeAsBytes()));
        ItemStack stack=controller(catalog);var meta=stack.getItemMeta();
        meta.displayName(text(catalog.controllerName()+"（全部入り）",NamedTextColor.GOLD));
        var lore=new java.util.ArrayList<Component>(meta.lore());lore.add(text("このアイテム専用の登録内容 / 全種プリセット",NamedTextColor.YELLOW));meta.lore(lore);
        meta.getPersistentDataContainer().set(controllerId,PersistentDataType.STRING,java.util.UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(controllerData,PersistentDataType.BYTE_ARRAY,new dev.aserver.motionrp.core.LoadoutCodec().encode(state));stack.setItemMeta(meta);return stack;
    }
    boolean embedded(ItemStack stack){return controller(stack)&&(stack.getItemMeta().getPersistentDataContainer().has(controllerData)||stack.getItemMeta().getPersistentDataContainer().has(controllerId));}
    String setId(ItemStack stack){return embedded(stack)?stack.getItemMeta().getPersistentDataContainer().get(controllerId,PersistentDataType.STRING):null;}
    byte[] setData(ItemStack stack){return stack.getItemMeta().getPersistentDataContainer().get(controllerData,PersistentDataType.BYTE_ARRAY);}
    void setData(ItemStack stack,byte[] data){
        if(setId(stack)==null)throw new IllegalStateException("Invalid controller identity");
        var meta=stack.getItemMeta();meta.getPersistentDataContainer().set(controllerData,PersistentDataType.BYTE_ARRAY,data);stack.setItemMeta(meta);
    }

    public ItemStack token(MotionCatalog.Definition definition, int amount) {
        if (amount < 1 || amount > 64) throw new IllegalArgumentException("Amount must be 1..64");
        ItemStack stack = new ItemStack(Material.PAPER, amount);
        var meta = stack.getItemMeta();
        meta.displayName(text(definition.name(), NamedTextColor.AQUA));
        var lore=new java.util.ArrayList<Component>(List.of(text("GUIでこのアイテムをクリックして1個登録", NamedTextColor.GRAY),
                text("モーション: " + definition.id(), NamedTextColor.DARK_GRAY)));
        if(dev.aserver.motionrp.core.PairMotion.fromEffect(definition.effect()).isPresent())lore.add(text("二人用: 左クリックで最寄りの相手へ招待（8ブロック以内）",NamedTextColor.YELLOW));
        lore.add(text(CONTROLLER_HINT,NamedTextColor.YELLOW));
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
        if(controller(stack)){
            var original=stack.getItemMeta();
            stack.setType(Material.AMETHYST_SHARD);
            var model=plugin.catalog().controllerModel();original.setItemModel(model==null?NamespacedKey.fromString("motionrp:controller"):model);
            original.setMaxStackSize(1);
            // Retain the original name, identity, escrow bytes and any custom tags.
            original.lore(controller(plugin.catalog()).getItemMeta().lore());
            stack.setItemMeta(original);return;
        }
        String id=motionId(stack);if(id==null)return;
        var meta=stack.getItemMeta();
        var lore=new java.util.ArrayList<Component>(meta.lore()==null?List.of():meta.lore());
        if(lore.stream().noneMatch(line->net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line).equals(CONTROLLER_HINT)))lore.add(text(CONTROLLER_HINT,NamedTextColor.YELLOW));
        meta.lore(lore);stack.setItemMeta(meta);if(meta.displayName()==null)return;
        String old=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        String next=MotionNames.updated(id,old);
        if(!old.equals(next)){meta.displayName(text(next,NamedTextColor.AQUA));stack.setItemMeta(meta);}
    }
    public void refreshNames(org.bukkit.entity.Player player) {
        for(int i=0;i<player.getInventory().getSize();i++){var stack=player.getInventory().getItem(i);if(stack!=null){refreshName(stack);player.getInventory().setItem(i,stack);}}
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
