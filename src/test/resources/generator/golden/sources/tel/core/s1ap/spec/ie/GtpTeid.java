// Generated from S1AP ASN.1 type GTP-TEID.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class GtpTeid implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("4:4");
    private final byte[] bytes;

    public GtpTeid(BitInput in) {
        try {
            this.bytes = AsnAper.octets(in, RANGE, false, true);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public GtpTeid(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        AsnAper.size(bytes.length, RANGE, false, true);
        this.bytes = bytes.clone();
    }

    public byte[] getBytes() { return bytes.clone(); }

    @Override
    public void encode(BitOutput out) {
        AsnAper.octets(out, bytes, RANGE, false, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "bytes=" + bytes.length + '}';
    }
}
