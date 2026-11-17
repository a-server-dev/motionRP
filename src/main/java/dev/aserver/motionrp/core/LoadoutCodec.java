package dev.aserver.motionrp.core;

import java.io.*;
import java.util.zip.CRC32;

/** Bounded, versioned data. Corruption must never become an empty, overwritable loadout. */
public final class LoadoutCodec {
    private static final int MAGIC = 0x4D525031;
    private static final int VERSION = 1;
    private static final int MAX_BYTES = 1_000_000;

    public byte[] encode(Loadout loadout) {
        try {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.writeInt(MAGIC);
            out.writeByte(VERSION);
            out.writeByte(Loadout.SIZE);
            out.writeByte(loadout.selected());
            for (int i = 0; i < Loadout.SIZE; i++) {
                var slot = loadout.slot(i);
                out.writeBoolean(slot != null);
                if (slot != null) {
                    out.writeUTF(slot.id());
                    byte[] item = slot.item();
                    out.writeInt(item.length);
                    out.write(item);
                }
            }
            var crc = new CRC32();
            crc.update(bytes.toByteArray());
            out.writeLong(crc.getValue());
            return bytes.toByteArray();
        } catch (IOException impossible) { throw new UncheckedIOException(impossible); }
    }

    public Loadout decode(byte[] data) throws IOException {
        if (data == null || data.length < 24 || data.length > MAX_BYTES) {
            throw new IOException("Invalid loadout length");
        }
        try {
            var crc = new CRC32();
            crc.update(data, 0, data.length - Long.BYTES);
            var checksum = new DataInputStream(new ByteArrayInputStream(data, data.length - 8, 8));
            if (checksum.readLong() != crc.getValue()) throw new IOException("Loadout checksum mismatch");
            var in = new DataInputStream(new ByteArrayInputStream(data, 0, data.length - 8));
            if (in.readInt() != MAGIC || in.readUnsignedByte() != VERSION) throw new IOException("Unsupported loadout format");
            if (in.readUnsignedByte() != Loadout.SIZE) throw new IOException("Invalid slot count");
            int selected = in.readUnsignedByte();
            var slots = new StoredMotion[Loadout.SIZE];
            for (int i = 0; i < slots.length; i++) {
                if (!in.readBoolean()) continue;
                String id = in.readUTF();
                int length = in.readInt();
                if (length < 1 || length > StoredMotion.MAX_ITEM_BYTES || length > in.available()) {
                    throw new IOException("Invalid item length");
                }
                slots[i] = new StoredMotion(id, in.readNBytes(length));
            }
            if (in.available() != 0) throw new IOException("Trailing loadout data");
            return new Loadout(slots, selected);
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid loadout", invalid); }
    }
}
