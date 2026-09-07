// Generated from S1AP ASN.1 type PLMNidentity.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class PlmnIdentity implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("3:3");
    private final byte[] bytes;

    public PlmnIdentity(BitInput in) {
        try {
            this.bytes = AsnAper.octets(in, RANGE, false, false);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public PlmnIdentity(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        AsnAper.size(bytes.length, RANGE, false, false);
        this.bytes = bytes.clone();
    }

    public byte[] getBytes() { return bytes.clone(); }

    @Override
    public void encode(BitOutput out) {
        AsnAper.octets(out, bytes, RANGE, false, false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "bytes=" + bytes.length + '}';
    }
}
