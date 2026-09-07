// Generated from S1AP ASN.1 type ENBname, ProtocolIE-ID 60.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.AsnPrintableString;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

/**
 * ENBname information element.
 * <p>See <a href="https://www.etsi.org/deliver/etsi_ts/136400_136499/136413/15.03.00_60/ts_136413v150300p.pdf">
 * TS 136 413 V15.3.0: 9.1.8.4 S1 SETUP REQUEST</a>.
 */
public final class EnbName implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("1:150");
    private final String value;


    /**
     * Decodes ENBname from {@link BitInput}.
     * @param in source {@link BitInput}
     */
    public EnbName(BitInput in) {
        try {
            this.value = AsnPrintableString.decodeAper(in, RANGE, true, false);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    /**
     * Creates ENBname.
     * <p>See <a href="https://www.etsi.org/deliver/etsi_ts/136400_136499/136413/15.03.00_60/ts_136413v150300p.pdf">
     * TS 136 413 V15.3.0: 9.1.8.4 S1 SETUP REQUEST</a>.
     */
    public EnbName(String value) {
        Objects.requireNonNull(value, "value");
        AsnAper.printable(value);
        AsnAper.size(value.length(), RANGE, true, false);
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public void encode(BitOutput out) {
        AsnPrintableString.encodeAper(out, value, RANGE, true, false);
    }

    @Override
    public String toString() {
        return "EnbName{" + "length=" + value.length() + '}';
    }
}
