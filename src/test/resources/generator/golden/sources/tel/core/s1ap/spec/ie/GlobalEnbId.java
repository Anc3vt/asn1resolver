// Generated from S1AP ASN.1 type Global-ENB-ID, ProtocolIE-ID 59.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class GlobalEnbId implements InformationElement {
    private final PlmnIdentity plmnIdentity;
    private final EnbId enbId;
    private final GlobalEnbIdIeExtensions ieExtensions;
    public GlobalEnbId(BitInput in) {
        try {
            boolean hasExtensions = in.readBit();
            boolean hasIeExtensions = in.readBit();
            this.plmnIdentity = new PlmnIdentity(in);
            this.enbId = new EnbId(in);
            this.ieExtensions = hasIeExtensions ? new GlobalEnbIdIeExtensions(in) : null;
            BitInput[] additions = hasExtensions ? AsnAper.extensions(in, 0) : new BitInput[0];
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

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
        return getClass().getSimpleName() + "{" + '}';
    }
}
