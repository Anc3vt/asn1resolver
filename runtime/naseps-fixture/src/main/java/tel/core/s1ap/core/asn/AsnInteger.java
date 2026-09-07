package tel.core.s1ap.core.asn;

import tel.core.s1ap.core.error.S1apException;

/** PER encoder/decoder for constrained ASN.1 INTEGER values. */
public final class AsnInteger {
    private AsnInteger() {
    }

    public static void encode(BitOutput out, long value, long min, long max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "INTEGER value out of range: %d (expected %d..%d)".formatted(value, min, max));
        }
        out.writeBits(value - min, bitsRequired(max - min));
    }

    public static void encode(BitOutput out, int value, int min, int max) {
        encode(out, (long) value, min, max);
    }

    public static long decode(BitInput in, long min, long max) {
        return in.readBits(bitsRequired(max - min)) + min;
    }

    public static int decode(BitInput in, int min, int max) {
        return (int) decode(in, (long) min, max);
    }

    /**
     * Encodes a constrained whole number using the aligned PER cases from
     * ITU-T X.691 clause 11.5.7.
     */
    public static void encodeAligned(BitOutput out, long value, long min, long max) {
        validate(value, min, max);
        long offsetRange = max - min;
        long offset = value - min;

        if (offsetRange <= 254) {
            out.writeBits(offset, bitsRequired(offsetRange));
        } else if (offsetRange == 255) {
            out.align();
            out.writeBits(offset, Byte.SIZE);
        } else if (offsetRange <= 65_535) {
            out.align();
            out.writeBits(offset, Short.SIZE);
        } else {
            int maximumOctets = octetsRequired(offsetRange);
            int valueOctets = octetsRequired(offset);
            encode(out, valueOctets, 1, maximumOctets);
            out.align();
            out.writeBits(offset, valueOctets * Byte.SIZE);
        }
    }

    public static void encodeAligned(BitOutput out, int value, int min, int max) {
        encodeAligned(out, (long) value, min, max);
    }

    /** Decodes a constrained whole number encoded with aligned PER. */
    public static long decodeAligned(BitInput in, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("INTEGER lower bound exceeds upper bound");
        }
        long offsetRange = max - min;
        long offset;

        if (offsetRange <= 254) {
            offset = in.readBits(bitsRequired(offsetRange));
        } else if (offsetRange == 255) {
            in.align();
            offset = in.readBits(Byte.SIZE);
        } else if (offsetRange <= 65_535) {
            in.align();
            offset = in.readBits(Short.SIZE);
        } else {
            int maximumOctets = octetsRequired(offsetRange);
            int valueOctets = decode(in, 1, maximumOctets);
            in.align();
            offset = in.readBits(valueOctets * Byte.SIZE);
        }

        if (offset > offsetRange) {
            throw new S1apException(
                    "Decoded INTEGER offset %d exceeds allowed range 0..%d"
                            .formatted(offset, offsetRange));
        }
        return offset + min;
    }

    public static int decodeAligned(BitInput in, int min, int max) {
        return (int) decodeAligned(in, (long) min, max);
    }

    private static void validate(long value, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("INTEGER lower bound exceeds upper bound");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "INTEGER value out of range: %d (expected %d..%d)".formatted(value, min, max));
        }
    }

    private static int octetsRequired(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("INTEGER value must be non-negative");
        }
        int bits = value == 0 ? 1 : Long.SIZE - Long.numberOfLeadingZeros(value);
        return (bits + Byte.SIZE - 1) / Byte.SIZE;
    }

    private static int bitsRequired(long range) {
        if (range < 0) {
            throw new S1apException("range is negative");
        }
        return range == 0 ? 0 : Long.SIZE - Long.numberOfLeadingZeros(range);
    }
}
