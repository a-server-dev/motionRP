package dev.aserver.motionrp.core;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import static org.junit.jupiter.api.Assertions.*;

class LoadoutCodecTest {
    private final LoadoutCodec codec = new LoadoutCodec();

    @Test void roundTripPreservesActualItemsOrderAndSelectedSlot() throws Exception {
        var original = Loadout.empty()
                .deposit(1, new StoredMotion("roll", new byte[]{0, 127, -1, 5}))
                .deposit(8, new StoredMotion("wave", new byte[]{8, 9})).next();
        assertEquals(original, codec.decode(codec.encode(original)));
        assertEquals(Loadout.empty(), codec.decode(codec.encode(Loadout.empty())));
    }

    @Test void truncatedCorruptOversizedAndNullDataFailClosed() {
        byte[] valid = codec.encode(Loadout.empty());
        for (int length = 0; length < valid.length; length++) {
            byte[] truncated = Arrays.copyOf(valid, length);
            assertThrows(IOException.class, () -> codec.decode(truncated));
        }
        byte[] corrupt = valid.clone(); corrupt[8] ^= 1;
        assertThrows(IOException.class, () -> codec.decode(corrupt));
        assertThrows(IOException.class, () -> codec.decode(new byte[2_000_001]));
        assertThrows(IOException.class, () -> codec.decode(null));
    }

    @Test void unsupportedVersionsInvalidSlotCountsAndInvalidSelectionAreRejectedEvenWithValidChecksum() {
        for (int index : new int[]{0, 4, 5, 6}) {
            byte[] data = codec.encode(Loadout.empty());
            data[index] = 99;
            resign(data);
            assertThrows(IOException.class, () -> codec.decode(data));
        }
    }

    @Test void maliciousItemLengthAndTrailingDataAreRejectedWithValidChecksum() {
        byte[] valid = codec.encode(Loadout.empty().deposit(0, new StoredMotion("roll", new byte[]{1})));
        byte[] badLength = valid.clone();
        // 7 byte header + presence + UTF length and 'roll', followed by item length.
        ByteBuffer.wrap(badLength).putInt(14, Integer.MAX_VALUE);
        resign(badLength);
        assertThrows(IOException.class, () -> codec.decode(badLength));
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        resign(trailing);
        assertThrows(IOException.class, () -> codec.decode(trailing));
    }

    @Test void tokenBoundsAndIdsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new StoredMotion("../bad", new byte[]{1}));
        assertThrows(IllegalArgumentException.class, () -> new StoredMotion("roll", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new StoredMotion("roll", new byte[StoredMotion.MAX_ITEM_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> new Loadout(new StoredMotion[8], 0));
    }

    @Test void nineSlotV1MigratesWithoutChangingEscrowBytesOrSelection() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);
        out.writeInt(0x4D525031);out.writeByte(1);out.writeByte(9);out.writeByte(8);
        for(int i=0;i<9;i++) {out.writeBoolean(i==8);if(i==8) {out.writeUTF("roll");out.writeInt(3);out.write(new byte[]{7,8,9});}}
        out.writeLong(0);byte[] old=bytes.toByteArray();resign(old);
        var migrated=codec.decode(old);assertEquals(8,migrated.selected());assertEquals(1,migrated.count());
        assertArrayEquals(new byte[]{7,8,9},migrated.slot(8).item());assertNull(migrated.slot(17));
        var expanded=migrated.deposit(17,new StoredMotion("excalibur",new byte[]{1}));
        assertEquals(expanded,codec.decode(codec.encode(expanded)));assertEquals(17,expanded.next().selected());
    }

    @Test void eighteenSlotV2PreservesEveryItemAndSelectionWhenExpandingToTwentySeven()throws Exception{
        var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);
        out.writeInt(0x4D525031);out.writeByte(2);out.writeByte(18);out.writeByte(17);
        for(int i=0;i<18;i++){out.writeBoolean(true);out.writeUTF("old_"+i);out.writeInt(2);out.write(new byte[]{(byte)i,99});}
        out.writeLong(0);byte[] old=bytes.toByteArray();resign(old);var expanded=codec.decode(old);assertEquals(18,expanded.count());assertEquals(17,expanded.selected());
        for(int i=0;i<18;i++)assertArrayEquals(new byte[]{(byte)i,99},expanded.slot(i).item());assertNull(expanded.slot(26));
        var withNew=expanded.deposit(26,new StoredMotion("road_roller",new byte[]{1}));assertEquals(withNew,codec.decode(codec.encode(withNew)));assertEquals(26,withNew.next().selected());
    }

    private static void resign(byte[] data) {
        var crc = new CRC32();
        crc.update(data, 0, data.length - 8);
        ByteBuffer.wrap(data).putLong(data.length - 8, crc.getValue());
    }
}
