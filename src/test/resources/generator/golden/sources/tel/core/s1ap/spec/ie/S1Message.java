// Generated from S1AP ASN.1 type <inline>, ProtocolIE-ID 225.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tel.core.s1ap.core.asn.AsnOctetString;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class S1Message implements InformationElement {
    private final byte[] bytes;

    public S1Message(BitInput in) {
        try {
            this.bytes = aperOctets(in, null, false, true);
        } catch (RuntimeException e) {
            throw aperProtocol(e);
        }
    }

    public S1Message(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        aperSize(bytes.length, null, false, true);
        this.bytes = bytes.clone();
    }

    public byte[] getBytes() { return bytes.clone(); }

    @Override
    public void encode(BitOutput out) {
        aperOctets(out, bytes, null, false, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "bytes=" + bytes.length + '}';
    }

    private static final BigInteger APER_ZERO = BigInteger.ZERO;

    private static final BigInteger APER_ONE = BigInteger.ONE;

    private static final class AperRange {
        private final List<BigInteger> endpoints;
        public AperRange(String intervals) {
            List<BigInteger> values = new ArrayList<>();
            for (String interval : intervals.split("\\|")) {
                String[] pair = interval.split(":");
                if (pair.length != 2) throw new IllegalArgumentException("Invalid interval");
                BigInteger min = new BigInteger(pair[0]), max = new BigInteger(pair[1]);
                if (min.compareTo(max) > 0) throw new IllegalArgumentException("Invalid interval bounds");
                if (!values.isEmpty() && min.compareTo(values.get(values.size() - 1)) <= 0)
                    throw new IllegalArgumentException("Intervals must be ordered and disjoint");
                values.add(min); values.add(max);
            }
            endpoints = List.copyOf(values);
        }
        public BigInteger min() { return endpoints.get(0); }
        public BigInteger max() { return endpoints.get(endpoints.size() - 1); }
        public boolean contains(BigInteger value) {
            for (int i = 0; i < endpoints.size(); i += 2)
                if (value.compareTo(endpoints.get(i)) >= 0 && value.compareTo(endpoints.get(i + 1)) <= 0) return true;
            return false;
        }
        public boolean contains(int value) { return contains(BigInteger.valueOf(value)); }
    }

    private static BigInteger aperValidate(BigInteger value, AperRange range, boolean extensible, boolean known) {
        Objects.requireNonNull(value, "value");
        if (range != null && !range.contains(value) && !(extensible && known))
            throw new IllegalArgumentException("Value outside supported root constraint: " + value);
        return value;
    }

    private static void aperSize(int size, AperRange range, boolean extensible, boolean known) {
        if (size < 0) throw new IllegalArgumentException("Negative length");
        aperValidate(BigInteger.valueOf(size), range, extensible, known);
    }

    private static S1apException aperProtocol(RuntimeException e) {
        return e instanceof S1apException s ? s : new S1apException("Invalid APER value: " + e.getMessage());
    }

    private static boolean aperExtension(BitInput in, boolean extensible, boolean known) {
        boolean result = extensible && in.readBit();
        if (result && !known) throw new S1apException("Extension value rejected by root-only policy");
        return result;
    }

    private static boolean aperExtension(BitOutput out, BigInteger value, AperRange range, boolean extensible, boolean known) {
        aperValidate(value, range, extensible, known);
        boolean result = extensible && range != null && !range.contains(value);
        if (extensible) out.writeBit(result);
        return result;
    }

    private static void aperConstrained(BitOutput out, BigInteger value, BigInteger min, BigInteger max) {
        BigInteger offset = value.subtract(min), range = max.subtract(min);
        if (range.signum() < 0 || offset.signum() < 0 || offset.compareTo(range) > 0)
            throw new IllegalArgumentException("Constrained INTEGER out of range");
        int bits = range.bitLength();
        if (range.compareTo(BigInteger.valueOf(255)) < 0) aperWriteBig(out, offset, bits);
        else if (bits <= 16) { out.align(); aperWriteBig(out, offset, bits == 8 ? 8 : 16); }
        else {
            int maxOctets = (bits + 7) / 8, octets = Math.max(1, (offset.bitLength() + 7) / 8);
            aperConstrained(out, BigInteger.valueOf(octets), APER_ONE, BigInteger.valueOf(maxOctets));
            out.align(); aperWriteBig(out, offset, octets * 8);
        }
    }

    private static BigInteger aperConstrained(BitInput in, BigInteger min, BigInteger max) {
        BigInteger range = max.subtract(min);
        if (range.signum() < 0) throw new IllegalArgumentException("Invalid range");
        int bits = range.bitLength(); BigInteger offset;
        if (range.compareTo(BigInteger.valueOf(255)) < 0) offset = aperReadBig(in, bits);
        else if (bits <= 16) { in.align(); offset = aperReadBig(in, bits == 8 ? 8 : 16); }
        else {
            int octets = aperConstrained(in, APER_ONE, BigInteger.valueOf((bits + 7) / 8)).intValueExact();
            in.align(); offset = aperReadBig(in, octets * 8);
        }
        if (offset.compareTo(range) > 0) throw new S1apException("Unused constrained INTEGER code");
        return offset.add(min);
    }

    private static void aperWriteBig(BitOutput out, BigInteger value, int bits) {
        for (int i = bits - 1; i >= 0; i--) out.writeBit(value.testBit(i));
    }

    private static BigInteger aperReadBig(BitInput in, int bits) {
        BigInteger result = APER_ZERO;
        for (int i = 0; i < bits; i++) result = result.shiftLeft(1).or(in.readBit() ? APER_ONE : APER_ZERO);
        return result;
    }

    private static void aperOctets(BitOutput out, byte[] bytes, AperRange range, boolean extensible, boolean known) {
        boolean ext = aperExtension(out, BigInteger.valueOf(bytes.length), range, extensible, known);
        if (aperGeneral(range, ext)) { AsnOctetString.encode(out, bytes); return; }
        int min = range.min().intValueExact(), max = range.max().intValueExact();
        if (min != max) aperConstrained(out, BigInteger.valueOf(bytes.length), range.min(), range.max());
        if (bytes.length == 0) return;
        AsnOctetString.encode(out, bytes, bytes.length, bytes.length, min != max || max > 2);
    }

    private static byte[] aperOctets(BitInput in, AperRange range, boolean extensible, boolean known) {
        boolean ext = aperExtension(in, extensible, known);
        byte[] result;
        if (aperGeneral(range, ext)) result = AsnOctetString.decode(in);
        else {
            int min = range.min().intValueExact(), max = range.max().intValueExact();
            int length = min == max ? min : aperConstrained(in, range.min(), range.max()).intValueExact();
            result = length == 0 ? new byte[0] : AsnOctetString.decode(in, length, length, min != max || max > 2);
        }
        if (!ext && range != null && !range.contains(result.length)) throw new S1apException("OCTET STRING root size");
        return result;
    }

    private static boolean aperGeneral(AperRange range, boolean extension) {
        return extension || range == null || range.max().compareTo(BigInteger.valueOf(65536)) >= 0;
    }

}
