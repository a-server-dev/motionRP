package dev.aserver.motionrp.core;

import java.util.Arrays;
import java.util.Objects;

/** The exact single deposited item, not a regenerated copy of its definition. */
public final class StoredMotion {
    public static final int MAX_ITEM_BYTES = 65_536;
    private final String id;
    private final byte[] item;

    public StoredMotion(String id, byte[] item) {
        if (id == null || !id.matches("[a-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid motion id");
        }
        Objects.requireNonNull(item, "item");
        if (item.length == 0 || item.length > MAX_ITEM_BYTES) {
            throw new IllegalArgumentException("Invalid item payload length");
        }
        this.id = id;
        this.item = item.clone();
    }

    public String id() { return id; }
    public byte[] item() { return item.clone(); }

    @Override public boolean equals(Object other) {
        return other instanceof StoredMotion m && id.equals(m.id) && Arrays.equals(item, m.item);
    }
    @Override public int hashCode() { return 31 * id.hashCode() + Arrays.hashCode(item); }
}
