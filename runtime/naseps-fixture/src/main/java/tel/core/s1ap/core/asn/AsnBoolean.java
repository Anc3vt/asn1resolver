package tel.core.s1ap.core.asn;

/** PER encoder/decoder for ASN.1 BOOLEAN. */
public final class AsnBoolean {
    private AsnBoolean() {
    }

    public static void encode(BitOutput out, boolean value) {
        out.writeBit(value);
    }

    public static boolean decode(BitInput in) {
        return in.readBit();
    }
}
