// Generated from S1AP ASN.1 type <inline>, ProtocolIE-ID 225.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class S1Message implements InformationElement {
    private final byte[] bytes;

    public S1Message(BitInput in) {
        try {
            this.bytes = AsnAper.octets(in, null, false, true);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public S1Message(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        AsnAper.size(bytes.length, null, false, true);
        this.bytes = bytes.clone();
    }

    public byte[] getBytes() { return bytes.clone(); }

    @Override
    public void encode(BitOutput out) {
        AsnAper.octets(out, bytes, null, false, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "bytes=" + bytes.length + '}';
    }
}
