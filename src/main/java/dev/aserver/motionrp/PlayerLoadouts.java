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
    private final LoadoutCodec codec = new LoadoutCodec();
    private final MotionItems items;

    public PlayerLoadouts(MotionPlugin plugin, MotionItems items) {
        key = new NamespacedKey(plugin, "loadout_v1");
        this.items = items;
    }

    public Loadout read(Player player) {
        var pdc = player.getPersistentDataContainer();
        if (!pdc.has(key)) return Loadout.empty();
        byte[] bytes = pdc.get(key, PersistentDataType.BYTE_ARRAY);
        if (bytes == null) throw new IllegalStateException("Invalid loadout data type");
        try {
            Loadout state = codec.decode(bytes);
            // Validate all saved items before allowing any transfer or replacement.
            for (int i = 0; i < Loadout.SIZE; i++) if (state.slot(i) != null) item(state.slot(i));
            return state;
        } catch (IOException | IllegalArgumentException invalid) {
            throw new IllegalStateException("Cannot read saved loadout; data retained", invalid);
        }
    }

    public ItemStack item(StoredMotion slot) {
        ItemStack stack = ItemStack.deserializeBytes(slot.item());
        if (stack.getAmount() != 1 || !slot.id().equals(items.motionId(stack))) {
            throw new IllegalArgumentException("Saved item does not match slot");
        }
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
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE_ARRAY, codec.encode(state));
    }

    public void save(Player player, Loadout state) {
        write(player, state);
        player.saveData();
    }
}
