// Generated from S1AP ASN.1 type ENB-ID.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tel.core.s1ap.core.asn.AsnOpenType;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class EnbId implements InformationElement {
    public enum Choice { MACROENBID, HOMEENBID, SHORTMACROENBID, LONGMACROENBID }
    private final Choice choice;
    private final EnbIdMacroEnbId macroEnbId;
    private final EnbIdHomeEnbId homeEnbId;
    private final EnbIdShortMacroEnbId shortMacroEnbId;
    private final EnbIdLongMacroEnbId longMacroEnbId;
    public EnbId(BitInput in) {
        try {
            int index = aperIndex(in, 2, true, false);
            BitInput content = index < 0 ? aperOpen(in) : in;
            this.choice = switch (index) {
                case 0 -> Choice.MACROENBID;
                case 1 -> Choice.HOMEENBID;
                case -1 -> Choice.SHORTMACROENBID;
                case -2 -> Choice.LONGMACROENBID;
                default -> throw new S1apException("Unknown CHOICE index");
            };
            this.macroEnbId = choice == Choice.MACROENBID ? new EnbIdMacroEnbId(content) : null;
            this.homeEnbId = choice == Choice.HOMEENBID ? new EnbIdHomeEnbId(content) : null;
            this.shortMacroEnbId = choice == Choice.SHORTMACROENBID ? new EnbIdShortMacroEnbId(content) : null;
            this.longMacroEnbId = choice == Choice.LONGMACROENBID ? new EnbIdLongMacroEnbId(content) : null;
            if (index < 0) aperFinishOpen(content);
        } catch (RuntimeException e) {
            throw aperProtocol(e);
        }
    }

    private EnbId(Choice choice, 
            EnbIdMacroEnbId macroEnbId,
            EnbIdHomeEnbId homeEnbId,
            EnbIdShortMacroEnbId shortMacroEnbId,
            EnbIdLongMacroEnbId longMacroEnbId) {
        this.choice = choice;
        this.macroEnbId = macroEnbId;
        this.homeEnbId = homeEnbId;
        this.shortMacroEnbId = shortMacroEnbId;
        this.longMacroEnbId = longMacroEnbId;
    }

    public static EnbId macroEnbId(EnbIdMacroEnbId value) {
        return new EnbId(Choice.MACROENBID, Objects.requireNonNull(value, "value"), null, null, null);
    }
    public static EnbId homeEnbId(EnbIdHomeEnbId value) {
        return new EnbId(Choice.HOMEENBID, null, Objects.requireNonNull(value, "value"), null, null);
    }
    public static EnbId shortMacroEnbId(EnbIdShortMacroEnbId value) {
        throw new IllegalArgumentException("CHOICE extensions are disabled");
    }
    public static EnbId longMacroEnbId(EnbIdLongMacroEnbId value) {
        throw new IllegalArgumentException("CHOICE extensions are disabled");
    }
    public Choice getChoice() { return choice; }
    public EnbIdMacroEnbId getMacroEnbId() { return macroEnbId; }
    public EnbIdHomeEnbId getHomeEnbId() { return homeEnbId; }
    public EnbIdShortMacroEnbId getShortMacroEnbId() { return shortMacroEnbId; }
    public EnbIdLongMacroEnbId getLongMacroEnbId() { return longMacroEnbId; }

    @Override
    public void encode(BitOutput out) {
        int populated = (macroEnbId == null ? 0 : 1) + (homeEnbId == null ? 0 : 1) + (shortMacroEnbId == null ? 0 : 1) + (longMacroEnbId == null ? 0 : 1);
        if (populated != 1) throw new IllegalStateException("CHOICE must contain exactly one value");
        switch (choice) {
            case MACROENBID -> {
                aperIndex(out, 0, false, 2, true, false);
                macroEnbId.encode(out);
            }
            case HOMEENBID -> {
                aperIndex(out, 1, false, 2, true, false);
                homeEnbId.encode(out);
            }
            case SHORTMACROENBID -> {
                aperIndex(out, 0, true, 2, true, false);
                aperOpen(out, shortMacroEnbId);
            }
            case LONGMACROENBID -> {
                aperIndex(out, 1, true, 2, true, false);
                aperOpen(out, longMacroEnbId);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + choice + '}';
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

    private static BitInput aperOpen(BitInput in) { return new BitInput(AsnOpenType.decode(in)); }

    private static void aperOpen(BitOutput out, InformationElement value) {
        BitOutput content = new BitOutput(); value.encode(content); byte[] bytes = content.toByteArray();
        AsnOpenType.encode(out, bytes.length == 0 ? new byte[1] : bytes);
    }

    private static void aperFinishOpen(BitInput in) {
        int remaining = in.getSourceData().length * 8 - in.getBytePos() * 8 - in.getBitPos();
        if (remaining > 7 && !(remaining == 8 && in.getBytePos() == 0 && in.getBitPos() == 0))
            throw new S1apException("Trailing open type data");
        while (remaining-- > 0) if (in.readBit()) throw new S1apException("Nonzero open type padding");
    }

}
