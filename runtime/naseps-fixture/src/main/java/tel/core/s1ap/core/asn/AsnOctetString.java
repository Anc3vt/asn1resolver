package tel.core.s1ap.core.asn;

import java.io.ByteArrayOutputStream;

/**
 * Encodes and decodes ASN.1 {@code OCTET STRING} values using Packed Encoding
 * Rules (PER).
 *
 * <p>The utility supports both size-constrained values, including fixed-size
 * strings, and unconstrained values. An unconstrained value is encoded with a
 * PER length determinant. Values of 16K octets or more are split into 16K to
 * 64K fragments and are terminated by a determinant for the remaining length.
 * This follows ITU-T X.691, clauses 11.9 and 17.</p>
 *
 * <p>Methods without an {@code aligned} argument use the aligned PER variant.
 * Pass {@code false} to an overload that accepts {@code aligned} when encoding
 * or decoding unaligned PER.</p>
 */
public final class AsnOctetString {
    private static final int SHORT_LENGTH_LIMIT = 128;
    private static final int FRAGMENT_UNIT = 16 * 1024;
    private static final int MAX_FRAGMENT_UNITS = 4;

    private AsnOctetString() {
    }

    /**
     * Encodes an unconstrained {@code OCTET STRING} using aligned PER.
     *
     * @param out destination to which the encoded length and contents are written
     * @param value octets to encode
     * @throws NullPointerException if {@code out} or {@code value} is {@code null}
     */
    public static void encode(BitOutput out, byte[] value) {
        encode(out, value, true);
    }

    /**
     * Encodes an unconstrained {@code OCTET STRING}.
     *
     * <p>Lengths below 128 use one octet, lengths below 16K use two octets,
     * and larger values are fragmented into blocks of 16K to 64K octets as
     * required by ITU-T X.691, clauses 11.9 and 17.</p>
     *
     * @param out destination to which the encoded length and contents are written
     * @param value octets to encode
     * @param aligned {@code true} for aligned PER; {@code false} for unaligned PER
     * @throws NullPointerException if {@code out} or {@code value} is {@code null}
     */
    public static void encode(BitOutput out, byte[] value, boolean aligned) {
        if (aligned) {
            out.align();
        }

        int offset = 0;
        int remaining = value.length;
        while (remaining >= FRAGMENT_UNIT) {
            int units = Math.min(MAX_FRAGMENT_UNITS, remaining / FRAGMENT_UNIT);
            int fragmentLength = units * FRAGMENT_UNIT;
            out.writeBits(0xC0 | units, Byte.SIZE);
            writeBytes(out, value, offset, fragmentLength);
            offset += fragmentLength;
            remaining -= fragmentLength;
        }

        if (remaining < SHORT_LENGTH_LIMIT) {
            out.writeBits(remaining, Byte.SIZE);
        } else {
            out.writeBits(0x8000 | remaining, Short.SIZE);
        }
        writeBytes(out, value, offset, remaining);
    }

    /**
     * Encodes a size-constrained {@code OCTET STRING} using aligned PER.
     *
     * <p>If {@code minSize == maxSize}, the size is fixed and no length
     * determinant is written. Otherwise, the actual length is encoded as a
     * constrained whole number in the inclusive range
     * {@code minSize..maxSize}.</p>
     *
     * @param out destination to which the encoded value is written
     * @param value octets to encode
     * @param minSize minimum permitted number of octets, inclusive
     * @param maxSize maximum permitted number of octets, inclusive
     * @throws ArrayIndexOutOfBoundsException if the value length is outside the
     *         permitted range
     * @throws NullPointerException if {@code out} or {@code value} is {@code null}
     */
    public static void encode(BitOutput out, byte[] value, int minSize, int maxSize) {
        encode(out, value, minSize, maxSize, true);
    }

    /**
     * Encodes a size-constrained {@code OCTET STRING}.
     *
     * <p>If {@code minSize == maxSize}, the size is fixed and no length
     * determinant is written. Otherwise, the actual length is encoded as a
     * constrained whole number in the inclusive range
     * {@code minSize..maxSize}.</p>
     *
     * @param out destination to which the encoded value is written
     * @param value octets to encode
     * @param minSize minimum permitted number of octets, inclusive
     * @param maxSize maximum permitted number of octets, inclusive
     * @param aligned whether a fixed-size value is aligned to the next octet
     *        boundary before its contents are written
     * @throws ArrayIndexOutOfBoundsException if the value length is outside the
     *         permitted range
     * @throws NullPointerException if {@code out} or {@code value} is {@code null}
     */
    public static void encode(BitOutput out, byte[] value, int minSize, int maxSize, boolean aligned) {
        if (value.length < minSize || value.length > maxSize) {
            throw new ArrayIndexOutOfBoundsException(
                    "OCTET STRING size out of bounds. value.length=%d, minSize=%d, maxSize=%d"
                            .formatted(value.length, minSize, maxSize));
        }
        if (minSize == maxSize) {
            if (aligned) {
                out.align();
            }
        } else {
            AsnInteger.encode(out, value.length, minSize, maxSize);
        }
        out.writeBytes(value);
    }

    /**
     * Decodes a size-constrained {@code OCTET STRING} using aligned PER.
     *
     * @param in source containing the encoded value
     * @param minSize minimum permitted number of octets, inclusive
     * @param maxSize maximum permitted number of octets, inclusive
     * @return the decoded octets in a new array
     * @throws IndexOutOfBoundsException if the input does not contain the
     *         complete encoded value
     * @throws NullPointerException if {@code in} is {@code null}
     */
    public static byte[] decode(BitInput in, int minSize, int maxSize) {
        return decode(in, minSize, maxSize, true);
    }

    /**
     * Decodes a size-constrained {@code OCTET STRING}.
     *
     * <p>For a fixed-size constraint no length determinant is consumed. For a
     * variable-size constraint, the length is decoded as a constrained whole
     * number in the inclusive range {@code minSize..maxSize}.</p>
     *
     * @param in source containing the encoded value
     * @param minSize minimum permitted number of octets, inclusive
     * @param maxSize maximum permitted number of octets, inclusive
     * @param aligned whether a fixed-size value is advanced to the next octet
     *        boundary before its contents are read
     * @return the decoded octets in a new array
     * @throws IndexOutOfBoundsException if the input does not contain the
     *         complete encoded value
     * @throws NullPointerException if {@code in} is {@code null}
     */
    public static byte[] decode(BitInput in, int minSize, int maxSize, boolean aligned) {
        if (minSize == maxSize) {
            if (aligned) {
                in.align();
            }
            return in.readBytes(minSize);
        }
        return in.readBytes(AsnInteger.decode(in, minSize, maxSize));
    }

    /**
     * Decodes an unconstrained {@code OCTET STRING} using aligned PER.
     *
     * @param in source containing the encoded length and contents
     * @return the decoded octets in a new array
     * @throws IllegalArgumentException if a reserved fragmentation determinant
     *         is encountered
     * @throws IndexOutOfBoundsException if the input does not contain the
     *         complete encoded value
     * @throws NullPointerException if {@code in} is {@code null}
     */
    public static byte[] decode(BitInput in) {
        return decode(in, true);
    }

    /**
     * Decodes an unconstrained {@code OCTET STRING}, joining all PER fragments
     * into one result array.
     *
     * @param in source containing the encoded length and contents
     * @param aligned {@code true} for aligned PER; {@code false} for unaligned PER
     * @return the decoded octets in a new array
     * @throws IllegalArgumentException if a reserved fragmentation determinant
     *         is encountered
     * @throws IndexOutOfBoundsException if the input does not contain the
     *         complete encoded value
     * @throws NullPointerException if {@code in} is {@code null}
     */
    public static byte[] decode(BitInput in, boolean aligned) {
        if (aligned) {
            in.align();
        }

        ByteArrayOutputStream value = new ByteArrayOutputStream();
        while (true) {
            int firstLengthOctet = in.readBitsInt(Byte.SIZE);
            if ((firstLengthOctet & 0x80) == 0) {
                append(value, in.readBytes(firstLengthOctet));
                return value.toByteArray();
            }
            if ((firstLengthOctet & 0x40) == 0) {
                int length = ((firstLengthOctet & 0x3F) << Byte.SIZE) | in.readBitsInt(Byte.SIZE);
                append(value, in.readBytes(length));
                return value.toByteArray();
            }

            int units = firstLengthOctet & 0x3F;
            if (units < 1 || units > MAX_FRAGMENT_UNITS) {
                throw new IllegalArgumentException(
                        "Invalid fragmented OCTET STRING length determinant: 0x%02X"
                                .formatted(firstLengthOctet));
            }
            append(value, in.readBytes(units * FRAGMENT_UNIT));
        }
    }

    private static void writeBytes(BitOutput out, byte[] value, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            out.writeBits(value[i] & 0xFFL, Byte.SIZE);
        }
    }

    private static void append(ByteArrayOutputStream destination, byte[] source) {
        destination.write(source, 0, source.length);
    }
}
