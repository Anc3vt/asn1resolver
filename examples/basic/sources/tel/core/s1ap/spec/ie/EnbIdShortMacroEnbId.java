// Generated from S1AP ASN.1 type EnbIdShortMacroEnbId.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tel.core.s1ap.core.asn.AsnBitString;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class EnbIdShortMacroEnbId implements InformationElement {
    private static final AperRange RANGE = new AperRange("18:18");
    private final AsnBitString.Value value;

    public EnbIdShortMacroEnbId(BitInput in) {
        try {
            this.value = aperBits(in, RANGE, false, false);
        } catch (RuntimeException e) {
            throw aperProtocol(e);
        }
    }

    public EnbIdShortMacroEnbId(AsnBitString.Value value) {
        Objects.requireNonNull(value, "value");
        aperSize(value.getBitLength(), RANGE, false, false);
        this.value = value;
    }

    public AsnBitString.Value getValue() { return value; }

    @Override
    public void encode(BitOutput out) {
        aperBits(out, value, RANGE, false, false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "bits=" + value.getBitLength() + '}';
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

    private static void aperBits(BitOutput out, AsnBitString.Value value, AperRange range, boolean extensible, boolean known) {
        int size = value.getBitLength();
        boolean ext = aperExtension(out, BigInteger.valueOf(size), range, extensible, known);
        if (aperGeneral(range, ext)) {
            int offset = 0;
            do {
                int chunk = aperWriteLength(out, size - offset);
                for (int i = 0; i < chunk; i++) out.writeBit(value.bitAt(offset + i));
                offset += chunk;
                if (chunk < 16384) break;
            } while (true);
        } else {
            if (!range.min().equals(range.max())) aperConstrained(out, BigInteger.valueOf(size), range.min(), range.max());
            if (size > 0 && (!range.min().equals(range.max()) || range.max().intValueExact() > 16)) out.align();
            for (int i = 0; i < size; i++) out.writeBit(value.bitAt(i));
        }
    }

    private static AsnBitString.Value aperBits(BitInput in, AperRange range, boolean extensible, boolean known) {
        boolean ext = aperExtension(in, extensible, known);
        AsnBitString.Value.Builder b = AsnBitString.Value.builder();
        if (aperGeneral(range, ext)) {
            int part;
            do { part = aperReadLength(in); int length = Math.abs(part);
                for (int i = 0; i < length; i++) b.appendBit(in.readBit());
            } while (part < 0);
        } else {
            int length = range.min().equals(range.max()) ? range.min().intValueExact() : aperConstrained(in, range.min(), range.max()).intValueExact();
            if (length > 0 && (!range.min().equals(range.max()) || range.max().intValueExact() > 16)) in.align();
            for (int i = 0; i < length; i++) b.appendBit(in.readBit());
        }
        AsnBitString.Value result = b.build();
        if (!ext && range != null && !range.contains(result.getBitLength())) throw new S1apException("BIT STRING root size");
        return result;
    }

    private static boolean aperGeneral(AperRange range, boolean extension) {
        return extension || range == null || range.max().compareTo(BigInteger.valueOf(65536)) >= 0;
    }

    private static int aperWriteLength(BitOutput out, int length) {
        out.align();
        if (length >= 16384) { int units = Math.min(4, length / 16384); out.writeBits(0xc0 | units, 8); return units * 16384; }
        out.writeBits(length < 128 ? length : 0x8000 | length, length < 128 ? 8 : 16);
        return length;
    }

    private static int aperReadLength(BitInput in) {
        in.align(); int first = in.readBitsInt(8);
        if ((first & 0x80) == 0) return first;
        if ((first & 0x40) == 0) return ((first & 0x3f) << 8) | in.readBitsInt(8);
        int units = first & 0x3f;
        if (units < 1 || units > 4) throw new S1apException("Invalid PER fragment determinant");
        return -units * 16384;
    }

}
