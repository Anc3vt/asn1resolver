// Generated from S1AP ASN.1 type ENBname, ProtocolIE-ID 60.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.AsnPrintableString;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class EnbName implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("1:150");
    private final String value;

    public EnbName(BitInput in) {
        try {
            this.value = AsnPrintableString.decodeAper(in, RANGE, true, true);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public EnbName(String value) {
        Objects.requireNonNull(value, "value");
        AsnAper.printable(value);
        AsnAper.size(value.length(), RANGE, true, true);
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public void encode(BitOutput out) {
        AsnPrintableString.encodeAper(out, value, RANGE, true, true);
    }

    @Override
    public String toString() {
        return "EnbName{" + "length=" + value.length() + '}';
    }
}
