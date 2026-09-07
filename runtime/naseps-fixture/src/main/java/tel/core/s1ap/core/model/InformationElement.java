package tel.core.s1ap.core.model;

import tel.core.s1ap.core.asn.BitOutput;

/** Actual data associated with one field of an S1AP message. */
public interface InformationElement {
    void encode(BitOutput out);

    default byte[] encode() {
        BitOutput out = new BitOutput();
        encode(out);
        return out.toByteArray();
    }
}
