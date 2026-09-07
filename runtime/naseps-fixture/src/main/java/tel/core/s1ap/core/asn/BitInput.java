package tel.core.s1ap.core.asn;

import java.util.Arrays;

/** Bit-level input reader for PER-encoded data. */
public final class BitInput {
    private final byte[] data;
    private final BitCursor cursor = new BitCursor();

    public BitInput(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    public boolean readBit() {
        ensureAvailable(1);
        boolean value = ((data[cursor.byteIndex()] >> (7 - cursor.bitIndex())) & 1) == 1;
        cursor.advanceBit();
        return value;
    }

    public long readBits(int bitCount) {
        if (bitCount < 0 || bitCount > Long.SIZE) {
            throw new IllegalArgumentException("bitCount must be between 0 and 64");
        }
        ensureAvailable(bitCount);
        long value = 0;
        for (int i = 0; i < bitCount; i++) {
            value = (value << 1) | (readBit() ? 1 : 0);
        }
        return value;
    }

    public int readBitsInt(int bitCount) {
        if (bitCount > Integer.SIZE) {
            throw new IllegalArgumentException("bitCount must be between 0 and 32");
        }
        return (int) readBits(bitCount);
    }

    public void skipBits(int bitCount) {
        readBits(bitCount);
    }

    public void align() {
        cursor.alignToByte();
    }

    public byte[] getSourceData() {
        return Arrays.copyOf(data, data.length);
    }

    public int getBytePos() {
        return cursor.byteIndex();
    }

    public int getBitPos() {
        return cursor.bitIndex();
    }

    public byte[] readBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0");
        }
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) readBitsInt(8);
        }
        return result;
    }

    private void ensureAvailable(int bitCount) {
        long current = (long) cursor.byteIndex() * Byte.SIZE + cursor.bitIndex();
        if (current + bitCount > (long) data.length * Byte.SIZE) {
            throw new IndexOutOfBoundsException("Not enough input bits");
        }
    }
}
