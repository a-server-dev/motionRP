package dev.aserver.motionrp.core;

import java.util.Arrays;
import java.util.Objects;

/** Immutable escrow contents. Empty slots are skipped when cycling selection. */
public final class Loadout {
    public static final int SIZE = 54;
    private final StoredMotion[] slots;
    private final int selected;

    public Loadout(StoredMotion[] slots, int selected) {
        if (slots.length != SIZE || selected < 0 || selected >= SIZE) {
            throw new IllegalArgumentException("Invalid loadout shape or selection");
        }
        this.slots = slots.clone();
        this.selected = selected;
    }

    public static Loadout empty() { return new Loadout(new StoredMotion[SIZE], 0); }
    public int selected() { return selected; }
    public StoredMotion slot(int slot) { return slots[Objects.checkIndex(slot, SIZE)]; }
    public StoredMotion current() { return slots[selected]; }
    public int firstEmpty() {
        for (int i = 0; i < SIZE; i++) if (slots[i] == null) return i;
        return -1;
    }
    public int count() { return (int) Arrays.stream(slots).filter(Objects::nonNull).count(); }

    public Loadout deposit(int index, StoredMotion motion) {
        Objects.checkIndex(index, SIZE);
        Objects.requireNonNull(motion, "motion");
        if (slots[index] != null) throw new IllegalStateException("Slot occupied");
        StoredMotion[] copy = slots.clone();
        copy[index] = motion;
        return new Loadout(copy, current() == null ? firstOccupied(copy, 0) : selected);
    }

    public Loadout withdraw(int index) {
        Objects.checkIndex(index, SIZE);
        if (slots[index] == null) throw new IllegalStateException("Slot empty");
        StoredMotion[] copy = slots.clone();
        copy[index] = null;
        int next = copy[selected] == null ? firstOccupied(copy, selected) : selected;
        return new Loadout(copy, next);
    }

    public Loadout select(int index) {
        if (slot(index) == null) throw new IllegalArgumentException("Empty motion slot");
        return new Loadout(slots, index);
    }

    public Loadout next() {
        return new Loadout(slots, firstOccupied(slots, (selected + 1) % SIZE));
    }

    private static int firstOccupied(StoredMotion[] values, int start) {
        for (int i = 0; i < SIZE; i++) {
            int index = (start + i) % SIZE;
            if (values[index] != null) return index;
        }
        return 0;
    }

    @Override public boolean equals(Object other) {
        return other instanceof Loadout l && selected == l.selected && Arrays.equals(slots, l.slots);
    }
    @Override public int hashCode() { return 31 * Arrays.hashCode(slots) + selected; }
}
