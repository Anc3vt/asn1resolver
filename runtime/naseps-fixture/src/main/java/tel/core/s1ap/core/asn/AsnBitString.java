package tel.core.s1ap.core.asn;

import java.util.Arrays;
import java.util.Objects;

/** PER encoder/decoder for ASN.1 BIT STRING values. */
public final class AsnBitString {
    private AsnBitString() {
    }

    public static void encode(BitOutput out, Value value, Integer minSize, Integer maxSize) {
        int bitLength = value.getBitLength();
        if (minSize != null && maxSize != null && Objects.equals(minSize, maxSize)) {
            int unusedBits = (8 - bitLength % 8) & 7;
            for (int i = 0; i < bitLength; i++) {
                out.writeBit(value.bitAt(i));
            }
            if (unusedBits != 0) {
                out.writeZeroBits(unusedBits);
            }
        } else {
            if (minSize == null || maxSize == null) {
                throw new IllegalArgumentException("Variable BIT STRING requires minSize and maxSize");
            }
            AsnInteger.encode(out, bitLength, minSize, maxSize);
            for (int i = 0; i < bitLength; i++) {
                out.writeBit(value.bitAt(i));
            }
        }
    }

    public static Value decode(BitInput in, Integer minSize, Integer maxSize) {
        int bitLength;
        boolean fixedSized = false;
        if (minSize != null && maxSize != null && Objects.equals(minSize, maxSize)) {
            bitLength = minSize;
            fixedSized = true;
        } else if (minSize != null && maxSize != null) {
            bitLength = AsnInteger.decode(in, minSize, maxSize);
        } else if (minSize != null) {
            bitLength = minSize;
        } else {
            throw new IllegalArgumentException("Unconstrained BIT STRING is not supported");
        }

        byte[] data = new byte[(bitLength + 7) / 8];
        for (int i = 0; i < bitLength; i++) {
            if (in.readBit()) {
                data[i / 8] |= (byte) (1 << (7 - i % 8));
            }
        }
        if (fixedSized) {
            int unusedBits = (8 - bitLength % 8) & 7;
            if (unusedBits > 0) {
                in.skipBits(unusedBits);
            }
        }
        return new Value(data, bitLength);
    }

    public static final class Value {
        private final byte[] bytes;
        private final int bitLength;

        public Value(byte[] bytes, int bitLength) {
            if (bitLength < 0) {
                throw new IllegalArgumentException("bitLength must be >= 0");
            }
            int requiredBytes = (bitLength + 7) / 8;
            if (bytes.length != requiredBytes) {
                throw new IllegalArgumentException(
                        "BIT STRING size mismatch: expected %d bytes, got %d"
                                .formatted(requiredBytes, bytes.length));
            }
            this.bytes = bytes.clone();
            this.bitLength = bitLength;
        }

        public int getBitLength() {
            return bitLength;
        }

        public byte[] getBytes() {
            return bytes.clone();
        }

        public boolean bitAt(int index) {
            if (index < 0 || index >= bitLength) {
                throw new IndexOutOfBoundsException(index);
            }
            return (((bytes[index / 8] & 0xFF) >> (7 - index % 8)) & 1) == 1;
        }

        public static Value fromBytes(byte[] bytes, int bitLength) {
            int requiredBytes = (bitLength + 7) / 8;
            if (bitLength < 0 || bytes.length != requiredBytes) {
                throw new IllegalArgumentException("BIT STRING size mismatch");
            }
            int unusedBits = requiredBytes * 8 - bitLength;
            if (unusedBits > 0 && (bytes[requiredBytes - 1] & ((1 << unusedBits) - 1)) != 0) {
                throw new IllegalArgumentException("Unused bits must be zero");
            }
            return new Value(bytes, bitLength);
        }

        public static Value fromLong(long value, int bitLength) {
            if (bitLength < 0 || bitLength > 64) {
                throw new IllegalArgumentException("bitLength must be between 0 and 64");
            }
            byte[] bytes = new byte[(bitLength + 7) / 8];
            for (int i = 0; i < bitLength; i++) {
                if (((value >>> (bitLength - 1 - i)) & 1) == 1) {
                    bytes[i / 8] |= (byte) (1 << (7 - i % 8));
                }
            }
            return new Value(bytes, bitLength);
        }

        public static Value fromInt(int value, int bitLength) {
            if (bitLength < 0 || bitLength > 32) {
                throw new IllegalArgumentException("bitLength must be between 0 and 32");
            }
            if (bitLength < 32 && (value >>> bitLength) != 0) {
                throw new IllegalArgumentException("value does not fit into %d bits".formatted(bitLength));
            }
            return fromLong(value & 0xFFFF_FFFFL, bitLength);
        }

        public static Value fromBits(boolean... bits) {
            Builder builder = builder();
            for (boolean bit : bits) {
                builder.appendBit(bit);
            }
            return builder.build();
        }

        public int toInt() {
            if (bitLength > 32) {
                throw new IllegalArgumentException("BIT STRING too long for int: %d".formatted(bitLength));
            }
            return (int) toLong();
        }

        public long toLong() {
            if (bitLength > 64) {
                throw new IllegalArgumentException("BIT STRING too long for long: %d".formatted(bitLength));
            }
            long result = 0;
            for (int i = 0; i < bitLength; i++) {
                if (bitAt(i)) {
                    result |= 1L << (bitLength - 1 - i);
                }
            }
            return result;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            StringBuilder value = new StringBuilder(bitLength);
            for (int i = 0; i < bitLength; i++) {
                value.append(bitAt(i) ? '1' : '0');
            }
            return "Value{" + value + ", bitLength=" + bitLength + '}';
        }

        public static final class Builder {
            private byte[] buffer = new byte[1];
            private int bitLength;

            private Builder() {
            }

            public Builder appendBit(boolean bit) {
                ensureCapacity(bitLength + 1);
                if (bit) {
                    buffer[bitLength / 8] |= (byte) (1 << (7 - bitLength % 8));
                }
                bitLength++;
                return this;
            }

            public Builder appendBits(long value, int bitCount) {
                if (bitCount < 0 || bitCount > 64) {
                    throw new IllegalArgumentException("bitCount must be between 0 and 64");
                }
                for (int i = bitCount - 1; i >= 0; i--) {
                    appendBit(((value >>> i) & 1) == 1);
                }
                return this;
            }

            public Builder appendBits(Value other) {
                for (int i = 0; i < other.bitLength; i++) {
                    appendBit(other.bitAt(i));
                }
                return this;
            }

            public int getBitLength() {
                return bitLength;
            }

            public Value build() {
                return new Value(Arrays.copyOf(buffer, (bitLength + 7) / 8), bitLength);
            }

            private void ensureCapacity(int requestedBitLength) {
                int requiredBytes = (requestedBitLength + 7) / 8;
                if (requiredBytes > buffer.length) {
                    buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, requiredBytes));
                }
            }
        }
    }
}
