// Generated from S1AP ASN.1 type E-RABUsageReportItem, ProtocolIE-ID 267.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
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
            BitInput[] additions = hasExtensions ? AsnAper.extensions(in, 0) : new BitInput[0];
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
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
        return "ERabUsageReportItem{" + '}';
    }
}
