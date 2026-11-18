package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class LoadoutTest {
    private StoredMotion token(String id) { return new StoredMotion(id, new byte[]{1, 2, 3}); }

    @Test void firstRegistrationIsSelectedAndOneItemPerSlotIsRetained() {
        var empty = Loadout.empty();
        var first = empty.deposit(3, token("roll"));
        var second = first.deposit(7, token("dance"));
        assertEquals(0, empty.count());
        assertEquals(1, first.count());
        assertEquals(3, second.selected());
        assertEquals("roll", second.current().id());
        assertEquals("dance", second.next().current().id());
        assertEquals(first.slot(3), second.slot(3));
    }

    @Test void cyclingSkipsGapsWrapsAndHandlesZeroOrOneMotion() {
        assertEquals(Loadout.empty(), Loadout.empty().next());
        var one = Loadout.empty().deposit(8, token("roll"));
        assertEquals(one, one.next());
        var three = one.deposit(1, token("wave")).deposit(4, token("dance"));
        assertEquals(1, three.next().selected());
        assertEquals(4, three.next().next().selected());
        assertEquals(8, three.next().next().next().selected());
    }

    @Test void removingSelectionSelectsNextAndRemovingLastReturnsEmpty() {
        var state = Loadout.empty().deposit(0, token("roll")).deposit(2, token("wave"));
        var removed = state.withdraw(0);
        assertEquals(2, removed.selected());
        assertEquals("wave", removed.current().id());
        assertEquals(Loadout.empty(), removed.withdraw(2));
        assertNotNull(state.slot(0));
    }

    @Test void fullInventoryAndOccupiedSlotsCannotOverwriteDeposits() {
        var full = Loadout.empty();
        for (int i = 0; i < Loadout.SIZE; i++) full = full.deposit(i, token("roll"));
        assertEquals(-1, full.firstEmpty());
        var saved = full;
        assertThrows(IllegalStateException.class, () -> saved.deposit(0, token("other")));
        assertEquals(Loadout.SIZE, saved.count());
        assertEquals("roll", saved.slot(0).id());
        assertThrows(IllegalStateException.class, () -> Loadout.empty().withdraw(0));
    }

    @Test void savedItemBytesAndSlotArrayCannotBeMutatedByCallers() {
        byte[] bytes = {1, 2};
        var item = new StoredMotion("roll", bytes);
        bytes[0] = 9;
        item.item()[0] = 8;
        assertArrayEquals(new byte[]{1, 2}, item.item());
        var slots = new StoredMotion[Loadout.SIZE];
        slots[0] = item;
        var state = new Loadout(slots, 0);
        slots[0] = null;
        assertNotNull(state.current());
    }

    @Test void randomizedDepositsAndWithdrawalsConserveOwnedItemsAcrossSaveReloads() throws Exception {
        Random random = new Random(8312);
        int inventory = 64;
        var state = Loadout.empty();
        var codec = new LoadoutCodec();
        for (int turn = 0; turn < 2000; turn++) {
            int slot = random.nextInt(Loadout.SIZE);
            if (state.slot(slot) == null && inventory > 0) {
                state = state.deposit(slot, token("roll"));
                inventory--;
            } else if (state.slot(slot) != null) {
                state = state.withdraw(slot);
                inventory++;
            }
            if (random.nextBoolean()) state = state.next();
            state = codec.decode(codec.encode(state));
            assertEquals(64, inventory + state.count());
            if (state.count() > 0) assertNotNull(state.current());
        }
    }
}
