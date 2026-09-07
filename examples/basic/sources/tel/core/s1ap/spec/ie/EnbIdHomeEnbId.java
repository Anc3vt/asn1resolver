// Generated from S1AP ASN.1 type EnbIdHomeEnbId.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.AsnBitString;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class EnbIdHomeEnbId implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("28:28");
    private final AsnBitString.Value value;

    public EnbIdHomeEnbId(BitInput in) {
        try {
            this.value = AsnAper.bits(in, RANGE, false, false);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public EnbIdHomeEnbId(AsnBitString.Value value) {
        Objects.requireNonNull(value, "value");
        AsnAper.size(value.getBitLength(), RANGE, false, false);
        this.value = value;
    }

    public AsnBitString.Value getValue() { return value; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.bits(out, value, RANGE, false, false);
    }

    @Override
    public String toString() {
        return "EnbIdHomeEnbId{" + "bits=" + value.getBitLength() + '}';
    }
}
