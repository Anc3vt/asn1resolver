// Generated from S1AP ASN.1 type E-RABSetupListCtxtSURes, ProtocolIE-ID 51.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import tel.core.s1ap.core.asn.AsnOpenType;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class ERabSetupList implements InformationElement {
    private static final AperRange RANGE = new AperRange("1:256");
    private final List<ERabSetupItem> values;

    private static ERabSetupItem readItem(BitInput in) {
        return (ERabSetupItem) aperField(in, new int[] {50}, new int[] {1},
                List.of(ERabSetupItem::new)).value();
    }
    public ERabSetupList(BitInput in) {
        try {
            this.values = aperList(in, RANGE, false, true, ERabSetupList::readItem);
        } catch (RuntimeException e) {
            throw aperProtocol(e);
        }
    }

    public ERabSetupList(List<ERabSetupItem> values) {
        this.values = List.copyOf(values);
        aperSize(values.size(), RANGE, false, true);
    }
    public ERabSetupList(ERabSetupItem... values) { this(List.of(values)); }
    public List<ERabSetupItem> getValues() { return values; }

    @Override
    public void encode(BitOutput out) {
        aperList(out, values, RANGE, false, true,
                (target, value) -> aperField(target, new AperField(50, 1, value)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "size=" + values.size() + '}';
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

    private static <T> List<T> aperList(BitInput in, AperRange range, boolean extensible, boolean known, Function<BitInput, T> reader) {
        boolean ext = aperExtension(in, extensible, known); List<T> values = new ArrayList<>();
        int part;
        do {
            part = aperGeneral(range, ext) ? aperReadLength(in) : aperConstrained(in, range.min(), range.max()).intValueExact();
            for (int i = 0; i < Math.abs(part); i++) values.add(reader.apply(in));
        } while (part < 0);
        if (!ext && range != null && !range.contains(values.size())) throw new S1apException("List root size");
        return List.copyOf(values);
    }

    private static <T> void aperList(BitOutput out, List<T> values, AperRange range, boolean extensible, boolean known, BiConsumer<BitOutput, T> writer) {
        boolean ext = aperExtension(out, BigInteger.valueOf(values.size()), range, extensible, known); int offset = 0;
        do {
            int count;
            if (aperGeneral(range, ext)) count = aperWriteLength(out, values.size() - offset);
            else { count = values.size(); aperConstrained(out, BigInteger.valueOf(count), range.min(), range.max()); }
            for (int i = 0; i < count; i++) writer.accept(out, values.get(offset + i));
            offset += count;
            if (!aperGeneral(range, ext) || count < 16384) break;
        } while (true);
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

    private static <T> T aperOpenValue(BitInput in, Function<BitInput, T> reader) {
        BitInput content = aperOpen(in); T value = reader.apply(content); aperFinishOpen(content); return value;
    }

    public record AperField(int id, int criticality, InformationElement value) {
        public AperField {
            if (id < 0 || id > 65535 || criticality < 0 || criticality > 2) throw new IllegalArgumentException("Invalid protocol field");
            Objects.requireNonNull(value, "value");
        }
    }

    private static AperField aperField(BitInput in, int[] ids, int[] criticalities, List<Function<BitInput, ? extends InformationElement>> readers) {
        int id = aperConstrained(in, APER_ZERO, BigInteger.valueOf(65535)).intValueExact();
        int criticality = aperIndex(in, 3, false, false);
        for (int i = 0; i < ids.length; i++) if (ids[i] == id) {
            if (criticalities[i] != criticality) throw new S1apException("Unexpected criticality for ID " + id);
            return new AperField(id, criticality, aperOpenValue(in, readers.get(i)));
        }
        throw new S1apException("Unknown protocol field ID " + id);
    }

    private static void aperField(BitOutput out, AperField field) {
        aperConstrained(out, BigInteger.valueOf(field.id()), APER_ZERO, BigInteger.valueOf(65535));
        aperIndex(out, field.criticality(), false, 3, false, false); aperOpen(out, field.value());
    }

}
