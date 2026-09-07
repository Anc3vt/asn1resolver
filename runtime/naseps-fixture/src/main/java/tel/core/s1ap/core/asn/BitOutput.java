package tel.core.s1ap.core.asn;

import java.io.ByteArrayOutputStream;

/** Bit-level output writer for PER encoding. */
public final class BitOutput {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final BitCursor cursor = new BitCursor();
    private int currentByte;

    public void writeBit(boolean value) {
        if (value) {
            currentByte |= 1 << (7 - cursor.bitIndex());
        }
        cursor.advanceBit();
        if (cursor.bitIndex() == 0) {
            flushByte();
        }
    }

    private void flushByte() {
        out.write(currentByte);
        currentByte = 0;
    }

    private void flushPartialByte() {
        if (cursor.bitIndex() > 0) {
            out.write(currentByte);
            currentByte = 0;
            cursor.alignToByte();
        }
    }

    public void writeBits(long value, int bitCount) {
        if (bitCount < 0 || bitCount > Long.SIZE) {
            throw new IllegalArgumentException("bitCount must be between 0 and 64");
        }
        for (int i = bitCount - 1; i >= 0; i--) {
            writeBit(((value >>> i) & 1) == 1);
        }
    }

    public void writeZeroBits(int bitCount) {
        writeBits(0, bitCount);
    }

    public void writeBytes(int... bytes) {
        for (int value : bytes) {
            writeBits(value & 0xFFL, 8);
        }
    }

    public void writeBytes(byte[] bytes) {
        for (byte value : bytes) {
            writeBits(value & 0xFFL, 8);
        }
    }

    public void align() {
        if (cursor.bitIndex() != 0) {
            flushByte();
            cursor.alignToByte();
        }
    }

    public byte[] toByteArray() {
        flushPartialByte();
        return out.toByteArray();
    }

    public byte[] getWrittenBytes() {
        return out.toByteArray();
    }

    public int getBytePos() {
        return cursor.byteIndex();
    }

    public int getBitPos() {
        return cursor.bitIndex();
    }
}
