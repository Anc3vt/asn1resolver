// Generated from S1AP ASN.1 type E-RABUsageReportItem, ProtocolIE-ID 267.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnOpenType;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class ERabUsageReportItem implements InformationElement {
    private final ERabUsageReportItemStartTimestamp startTimestamp;
    private final ERabUsageReportItemEndTimestamp endTimestamp;
    private final ERabUsageReportItemUsageCountUL usageCountUL;
    private final ERabUsageReportItemUsageCountDL usageCountDL;
    private final ERabUsageReportItemIeExtensions ieExtensions;
    public ERabUsageReportItem(BitInput in) {
        try {
            boolean hasExtensions = in.readBit();
            boolean hasIeExtensions = in.readBit();
            this.startTimestamp = new ERabUsageReportItemStartTimestamp(in);
            this.endTimestamp = new ERabUsageReportItemEndTimestamp(in);
            this.usageCountUL = new ERabUsageReportItemUsageCountUL(in);
            this.usageCountDL = new ERabUsageReportItemUsageCountDL(in);
            this.ieExtensions = hasIeExtensions ? new ERabUsageReportItemIeExtensions(in) : null;
            BitInput[] additions = hasExtensions ? aperExtensions(in, 0) : new BitInput[0];
        } catch (RuntimeException e) {
            throw aperProtocol(e);
        }
    }

    public ERabUsageReportItem(
            ERabUsageReportItemStartTimestamp startTimestamp,
            ERabUsageReportItemEndTimestamp endTimestamp,
            ERabUsageReportItemUsageCountUL usageCountUL,
            ERabUsageReportItemUsageCountDL usageCountDL,
            ERabUsageReportItemIeExtensions ieExtensions) {
        Objects.requireNonNull(startTimestamp, "startTimestamp");
        this.startTimestamp = startTimestamp;
        Objects.requireNonNull(endTimestamp, "endTimestamp");
        this.endTimestamp = endTimestamp;
        Objects.requireNonNull(usageCountUL, "usageCountUL");
        this.usageCountUL = usageCountUL;
        Objects.requireNonNull(usageCountDL, "usageCountDL");
        this.usageCountDL = usageCountDL;
        this.ieExtensions = ieExtensions;
    }

    public ERabUsageReportItemStartTimestamp getStartTimestamp() { return startTimestamp; }
    public ERabUsageReportItemEndTimestamp getEndTimestamp() { return endTimestamp; }
    public ERabUsageReportItemUsageCountUL getUsageCountUL() { return usageCountUL; }
    public ERabUsageReportItemUsageCountDL getUsageCountDL() { return usageCountDL; }
    public ERabUsageReportItemIeExtensions getIeExtensions() { return ieExtensions; }

    @Override
    public void encode(BitOutput out) {
        boolean hasExtensions = false;
        out.writeBit(hasExtensions);
        out.writeBit(ieExtensions != null);
        startTimestamp.encode(out);
        endTimestamp.encode(out);
        usageCountUL.encode(out);
        usageCountDL.encode(out);
        if (ieExtensions != null) ieExtensions.encode(out);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + '}';
    }

    private static S1apException aperProtocol(RuntimeException e) {
        return e instanceof S1apException s ? s : new S1apException("Invalid APER value: " + e.getMessage());
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

    private static BitInput aperOpen(BitInput in) { return new BitInput(AsnOpenType.decode(in)); }

    private static void aperOpen(BitOutput out, InformationElement value) {
        BitOutput content = new BitOutput(); value.encode(content); byte[] bytes = content.toByteArray();
        AsnOpenType.encode(out, bytes.length == 0 ? new byte[1] : bytes);
    }

    private static BitInput[] aperExtensions(BitInput in, int knownCount) {
        int count = in.readBit() ? aperReadLength(in) : in.readBitsInt(6) + 1;
        if (count < 1 || count > 65536) throw new S1apException("Extension bitmap too large");
        boolean[] present = new boolean[count];
        boolean any = false;
        for (int i = 0; i < count; i++) { present[i] = in.readBit(); any |= present[i]; }
        if (!any) throw new S1apException("Extension-present bit with no present additions");
        BitInput[] values = new BitInput[knownCount];
        for (int i = 0; i < count; i++) if (present[i]) {
            if (i >= knownCount) throw new S1apException("Unknown extension addition");
            values[i] = aperOpen(in);
        }
        return values;
    }

    private static void aperExtensions(BitOutput out, InformationElement... values) {
        if (values.length == 0) throw new IllegalArgumentException("Empty extension bitmap");
        if (values.length <= 64) { out.writeBit(false); out.writeBits(values.length - 1, 6); }
        else {
            if (values.length >= 16384) throw new IllegalArgumentException("Extension bitmap fragmentation is unsupported");
            out.writeBit(true); aperWriteLength(out, values.length);
        }
        for (InformationElement value : values) out.writeBit(value != null);
        for (InformationElement value : values) if (value != null) aperOpen(out, value);
    }

}
