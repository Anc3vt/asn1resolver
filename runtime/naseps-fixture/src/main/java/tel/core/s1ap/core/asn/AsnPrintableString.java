package tel.core.s1ap.core.asn;

import java.nio.charset.StandardCharsets;

/** PER encoder/decoder for constrained ASN.1 PrintableString values. */
public final class AsnPrintableString {
    private AsnPrintableString() {
    }

    public static void encode(BitOutput out, String string, int minSize, int maxSize) {
        byte[] bytes = string.getBytes(StandardCharsets.US_ASCII);
        out.writeBit(false);
        AsnInteger.encode(out, bytes.length, minSize, maxSize);
        out.align();
        out.writeBytes(bytes);
    }

    public static String decode(BitInput in, int minSize, int maxSize) {
        if (in.readBit()) {
            throw new UnsupportedOperationException("PRINTABLE STRING extension not supported");
        }
        int length = AsnInteger.decode(in, minSize, maxSize);
        in.align();
        return new String(in.readBytes(length), StandardCharsets.US_ASCII);
    }
}
