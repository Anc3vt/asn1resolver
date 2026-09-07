// Generated from S1AP ASN.1 type E-RABSetupItemCtxtSURes.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class ERabSetupItem implements InformationElement {
    private final ERabId eRabId;
    private final TransportLayerAddress transportLayerAddress;
    private final GtpTeid gtpTeid;
    private final ERabSetupItemIeExtensions ieExtensions;
    public ERabSetupItem(BitInput in) {
        try {
            boolean hasExtensions = in.readBit();
            if (hasExtensions) throw new S1apException("Sequence extensions are disabled");
            boolean hasIeExtensions = in.readBit();
            this.eRabId = new ERabId(in);
            this.transportLayerAddress = new TransportLayerAddress(in);
            this.gtpTeid = new GtpTeid(in);
            this.ieExtensions = hasIeExtensions ? new ERabSetupItemIeExtensions(in) : null;
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public ERabSetupItem(
            ERabId eRabId,
            TransportLayerAddress transportLayerAddress,
            GtpTeid gtpTeid,
            ERabSetupItemIeExtensions ieExtensions) {
        Objects.requireNonNull(eRabId, "eRabId");
        this.eRabId = eRabId;
        Objects.requireNonNull(transportLayerAddress, "transportLayerAddress");
        this.transportLayerAddress = transportLayerAddress;
        Objects.requireNonNull(gtpTeid, "gtpTeid");
        this.gtpTeid = gtpTeid;
        this.ieExtensions = ieExtensions;
    }

    public ERabId getERabId() { return eRabId; }
    public TransportLayerAddress getTransportLayerAddress() { return transportLayerAddress; }
    public GtpTeid getGtpTeid() { return gtpTeid; }
    public ERabSetupItemIeExtensions getIeExtensions() { return ieExtensions; }

    @Override
    public void encode(BitOutput out) {
        boolean hasExtensions = false;
        out.writeBit(hasExtensions);
        out.writeBit(ieExtensions != null);
        eRabId.encode(out);
        transportLayerAddress.encode(out);
        gtpTeid.encode(out);
        if (ieExtensions != null) ieExtensions.encode(out);
    }

    @Override
    public String toString() {
        return "ERabSetupItem{" + '}';
    }
}
