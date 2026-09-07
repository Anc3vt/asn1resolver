package tel.core.s1ap.core.asn;

/** PER encoder/decoder for ASN.1 OPEN TYPE. */
public final class AsnOpenType {
    private AsnOpenType() {
    }

    /** Encodes an ASN.1 OPEN TYPE value using PER. */
    public static void encode(BitOutput out, byte[] valueBytes) {
        AsnOctetString.encode(out, valueBytes);
    }

    /** Decodes an ASN.1 OPEN TYPE value from PER encoding. */
    public static byte[] decode(BitInput in) {
        return AsnOctetString.decode(in);
    }
}
