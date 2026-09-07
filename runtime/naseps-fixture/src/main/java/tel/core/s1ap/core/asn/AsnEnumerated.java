package tel.core.s1ap.core.asn;

/** PER encoder/decoder for constrained ASN.1 ENUMERATED values. */
public final class AsnEnumerated {
    private AsnEnumerated() {
    }

    public static void encode(BitOutput out, int value, int maxValue) {
        if (value < 0 || value > maxValue) {
            throw new IllegalArgumentException(
                    "value outside allowed range (0..%d): %d".formatted(maxValue, value));
        }
        out.writeBits(value, bitsRequired(maxValue));
    }

    public static int decode(BitInput in, int maxValue) {
        return in.readBitsInt(bitsRequired(maxValue));
    }

    private static int bitsRequired(int maxValue) {
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
        return maxValue == 0 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(maxValue);
    }
}
