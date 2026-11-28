package dev.aserver.motionrp;

import dev.aserver.motionrp.core.*;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.io.IOException;

/** Escrow and vanilla inventory are persisted together in the same player.dat. */
public final class PlayerLoadouts {
    private final NamespacedKey key;
    private final NamespacedKey favoritesKey;
    private final LoadoutCodec codec = new LoadoutCodec();
    private final MotionItems items;

    public PlayerLoadouts(MotionPlugin plugin, MotionItems items) {
        favoritesKey=new NamespacedKey(plugin,"favorites_v1");
        key = new NamespacedKey(plugin, "loadout_v1");
        this.items = items;
    }

    public Loadout read(Player player) {
        ItemStack held=player.getInventory().getItemInMainHand();byte[] bytes;
        if(items.embedded(held)){
            if(items.setId(held)==null)throw new IllegalStateException("Invalid controller identity");
            bytes=items.setData(held);
        }else{
            var pdc=player.getPersistentDataContainer();if(!pdc.has(key))return Loadout.empty();
            bytes=pdc.get(key,PersistentDataType.BYTE_ARRAY);
        }
        if(bytes==null)throw new IllegalStateException("Invalid loadout data type");
        try {
            Loadout state = codec.decode(bytes);
            // Validate all saved items before allowing any transfer or replacement.
            for (int i = 0; i < Loadout.SIZE; i++) if (state.slot(i) != null) item(state.slot(i));
            return state;
        } catch (IOException | IllegalArgumentException invalid) {
            throw new IllegalStateException("Cannot read saved loadout; data retained", invalid);
        }
    }

    public java.util.LinkedHashSet<String> favorites(Player player,Loadout state){
        var held=player.getInventory().getItemInMainHand();
        var pdc=items.embedded(held)?held.getItemMeta().getPersistentDataContainer():player.getPersistentDataContainer();
        String raw=pdc.getOrDefault(favoritesKey,PersistentDataType.STRING,"");
        var available=new java.util.HashSet<String>();for(int i=0;i<Loadout.SIZE;i++)if(state.slot(i)!=null)available.add(state.slot(i).id());
        var result=new java.util.LinkedHashSet<String>();
        for(String id:raw.split(","))if(available.contains(id)&&result.size()<9)result.add(id);
        return result;
    }
    public void favorites(Player player,java.util.Set<String> ids){
        if(ids.size()>9||ids.stream().anyMatch(id->!id.matches("[a-z0-9_-]{1,64}")))throw new IllegalArgumentException("Invalid favorites");
        var held=player.getInventory().getItemInMainHand();String raw=String.join(",",ids);
        if(items.embedded(held)){var meta=held.getItemMeta();meta.getPersistentDataContainer().set(favoritesKey,PersistentDataType.STRING,raw);held.setItemMeta(meta);player.getInventory().setItemInMainHand(held);}
        else player.getPersistentDataContainer().set(favoritesKey,PersistentDataType.STRING,raw);
        player.saveData();
    }

    public ItemStack item(StoredMotion slot) {
        ItemStack stack = ItemStack.deserializeBytes(slot.item());
        if (stack.getAmount() != 1 || !slot.id().equals(items.motionId(stack))) {
            throw new IllegalArgumentException("Saved item does not match slot");
        }
        items.refreshName(stack);
        return stack;
    }

    public StoredMotion stored(ItemStack source) {
        String id = items.motionId(source);
        if (id == null) throw new IllegalArgumentException("Not a motion item");
        ItemStack one = source.clone();
        one.setAmount(1);
        return new StoredMotion(id, one.serializeAsBytes());
    }

    /** Call after the corresponding inventory mutation, on the server thread only. */
    public void write(Player player, Loadout state) {
        var held=player.getInventory().getItemInMainHand();byte[] bytes=codec.encode(state);
        if(items.embedded(held)){items.setData(held,bytes);player.getInventory().setItemInMainHand(held);}
        else player.getPersistentDataContainer().set(key,PersistentDataType.BYTE_ARRAY,bytes);
    }

    public void save(Player player, Loadout state) {
        write(player, state);
        player.saveData();
    }
}
