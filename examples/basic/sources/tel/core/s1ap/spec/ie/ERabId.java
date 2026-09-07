// Generated from S1AP ASN.1 type E-RAB-ID.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.math.BigInteger;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class ERabId implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("0:15");
    private final BigInteger value;

    public ERabId(BitInput in) {
        try {
            this.value = AsnAper.integer(in, RANGE, true, false);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public ERabId(BigInteger value) {
        AsnAper.validate(value, RANGE, true, false);
        this.value = value;
    }

    public BigInteger getValue() { return value; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.integer(out, value, RANGE, true, false);
    }

    @Override
    public String toString() {
        return "ERabId{" + value + '}';
    }
}
