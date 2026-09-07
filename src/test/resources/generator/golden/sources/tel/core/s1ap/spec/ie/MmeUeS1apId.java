// Generated from S1AP ASN.1 type MME-UE-S1AP-ID, ProtocolIE-ID 0.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.math.BigInteger;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class MmeUeS1apId implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("0:4294967295");
    private final long value;

    public MmeUeS1apId(BitInput in) {
        try {
            this.value = AsnAper.integer(in, RANGE, false, true).longValueExact();
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public MmeUeS1apId(long value) {
        AsnAper.validate(BigInteger.valueOf(value), RANGE, false, true);
        this.value = value;
    }

    public long getValue() { return value; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.integer(out, BigInteger.valueOf(value), RANGE, false, true);
    }

    @Override
    public String toString() {
        return "MmeUeS1apId{" + value + '}';
    }
}
