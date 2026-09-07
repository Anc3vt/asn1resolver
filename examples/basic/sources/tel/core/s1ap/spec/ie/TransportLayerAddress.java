// Generated from S1AP ASN.1 type TransportLayerAddress.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.AsnBitString;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class TransportLayerAddress implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("1:160");
    private final AsnBitString.Value value;

    public TransportLayerAddress(BitInput in) {
        try {
            this.value = AsnAper.bits(in, RANGE, true, false);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public TransportLayerAddress(AsnBitString.Value value) {
        Objects.requireNonNull(value, "value");
        AsnAper.size(value.getBitLength(), RANGE, true, false);
        this.value = value;
    }

    public AsnBitString.Value getValue() { return value; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.bits(out, value, RANGE, true, false);
    }

    @Override
    public String toString() {
        return "TransportLayerAddress{" + "bits=" + value.getBitLength() + '}';
    }
}
