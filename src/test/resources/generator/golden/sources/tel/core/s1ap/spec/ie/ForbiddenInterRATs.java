// Generated from S1AP ASN.1 type ForbiddenInterRATs.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class ForbiddenInterRATs implements InformationElement {
    public enum Value {
        ALL(0, false),
        GERAN(1, false),
        UTRAN(2, false),
        CDMA2000(3, false),
        GERANANDUTRAN(0, true),
        CDMA2000ANDUTRAN(1, true);

        private final int index;
        private final boolean extensionAddition;
        Value(int index, boolean extensionAddition) {
            this.index = index;
            this.extensionAddition = extensionAddition;
        }
        public int getCode() { return index; }
        public boolean isExtensionAddition() { return extensionAddition; }
        public static Value fromRootIndex(int index) {
            if (index < 0) throw new S1apException("Negative root index");
            return fromIndex(index);
        }
        public static Value fromExtensionIndex(int index) {
            if (index < 0) throw new S1apException("Negative extension index");
            return fromIndex(-index - 1);
        }
        private static Value fromIndex(int code) {
            return switch (code) {
                case 0 -> ALL;
                case 1 -> GERAN;
                case 2 -> UTRAN;
                case 3 -> CDMA2000;
                case -1 -> GERANANDUTRAN;
                case -2 -> CDMA2000ANDUTRAN;
                default -> throw new S1apException("Unknown ENUMERATED index: " + code);
            };
        }
    }

    private final Value value;

    public ForbiddenInterRATs(BitInput in) {
        try {
            this.value = Value.fromIndex(aperIndex(in, 4, true, true));
        } catch (RuntimeException e) {
            throw aperProtocol(e);
        }
    }

    public ForbiddenInterRATs(Value value) {
        Objects.requireNonNull(value, "value");
        this.value = value;
    }

    public Value getValue() { return value; }
    public int getCode() { return value.index; }

    @Override
    public void encode(BitOutput out) {
        aperIndex(out, value.index, value.extensionAddition, 4, true, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + value + '}';
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

    private static void aperNormallySmall(BitOutput out, int value) {
        if (value < 0) throw new IllegalArgumentException("Negative normally-small number");
        out.writeBit(value >= 64);
        if (value < 64) out.writeBits(value, 6);
        else {
            int octets = Math.max(1, (32 - Integer.numberOfLeadingZeros(value) + 7) / 8);
            out.align(); out.writeBits(octets, 8); out.writeBits(value, octets * 8);
        }
    }

    private static int aperNormallySmall(BitInput in) {
        if (!in.readBit()) return in.readBitsInt(6);
        in.align(); int size = in.readBitsInt(8);
        if (size < 1 || size > 4) throw new S1apException("Unsupported normally-small length");
        long value = in.readBits(size * 8);
        if (value > Integer.MAX_VALUE) throw new S1apException("Normally-small value exceeds Java index");
        return (int) value;
    }

    private static void aperIndex(BitOutput out, int index, boolean addition, int roots, boolean extensible, boolean known) {
        if (addition && (!extensible || !known)) throw new IllegalArgumentException("Extension index is disabled");
        if (extensible) out.writeBit(addition);
        if (addition) aperNormallySmall(out, index);
        else {
            if (index < 0 || index >= roots) throw new IllegalArgumentException("Invalid root index");
            aperConstrained(out, BigInteger.valueOf(index), APER_ZERO, BigInteger.valueOf(roots - 1L));
        }
    }

    private static int aperIndex(BitInput in, int roots, boolean extensible, boolean known) {
        if (aperExtension(in, extensible, known)) return -aperNormallySmall(in) - 1;
        if (roots < 1) throw new S1apException("No root alternatives");
        int value = aperConstrained(in, APER_ZERO, BigInteger.valueOf(roots - 1L)).intValueExact();
        if (value >= roots) throw new S1apException("Unused root index");
        return value;
    }

}
