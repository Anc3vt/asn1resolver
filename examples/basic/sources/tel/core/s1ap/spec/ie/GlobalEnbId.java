// Generated from S1AP ASN.1 type Global-ENB-ID, ProtocolIE-ID 59.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

/**
 * Global-ENB-ID information element.
 * <p>See <a href="https://www.etsi.org/deliver/etsi_ts/136400_136499/136413/15.03.00_60/ts_136413v150300p.pdf">
 * TS 136 413 V15.3.0: 9.2.1.37 Global eNB ID</a>.
 */
public final class GlobalEnbId implements InformationElement {
    private final PlmnIdentity plmnIdentity;
    private final EnbId enbId;
    private final GlobalEnbIdIeExtensions ieExtensions;

    /**
     * Decodes Global-ENB-ID from {@link BitInput}.
     * @param in source {@link BitInput}
     */
    public GlobalEnbId(BitInput in) {
        try {
            boolean hasExtensions = in.readBit();
            if (hasExtensions) throw new S1apException("Sequence extensions are disabled");
            boolean hasIeExtensions = in.readBit();
            this.plmnIdentity = new PlmnIdentity(in);
            this.enbId = new EnbId(in);
            this.ieExtensions = hasIeExtensions ? new GlobalEnbIdIeExtensions(in) : null;
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    /**
     * Creates Global-ENB-ID.
     * <p>See <a href="https://www.etsi.org/deliver/etsi_ts/136400_136499/136413/15.03.00_60/ts_136413v150300p.pdf">
     * TS 136 413 V15.3.0: 9.2.1.37 Global eNB ID</a>.
     */
    public GlobalEnbId(PlmnIdentity plmnIdentity, EnbId enbId, GlobalEnbIdIeExtensions ieExtensions) {
        Objects.requireNonNull(plmnIdentity, "plmnIdentity");
        this.plmnIdentity = plmnIdentity;
        Objects.requireNonNull(enbId, "enbId");
        this.enbId = enbId;
        this.ieExtensions = ieExtensions;
    }

    public PlmnIdentity getPlmnIdentity() { return plmnIdentity; }
    public EnbId getEnbId() { return enbId; }
    public GlobalEnbIdIeExtensions getIeExtensions() { return ieExtensions; }

    @Override
    public void encode(BitOutput out) {
        boolean hasExtensions = false;
        out.writeBit(hasExtensions);
        out.writeBit(ieExtensions != null);
        plmnIdentity.encode(out);
        enbId.encode(out);
        if (ieExtensions != null) ieExtensions.encode(out);
    }

    @Override
    public String toString() {
        return "GlobalEnbId{" + '}';
    }
}
