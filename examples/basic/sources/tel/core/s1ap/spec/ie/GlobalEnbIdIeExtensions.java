// Generated from S1AP ASN.1 type GlobalEnbIdIeExtensions.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.List;
import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class GlobalEnbIdIeExtensions implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("1:65535");
    private final List<AsnAper.Field> values;
    private static AsnAper.Field readField(BitInput in) {
        return AsnAper.field(in,
                new int[] {},
                new int[] {},
                List.of());
    }
    private static void validateField(AsnAper.Field field) {
        Objects.requireNonNull(field, "field");
        throw new IllegalArgumentException("Value does not belong to the container object set");
    }
    public GlobalEnbIdIeExtensions(BitInput in) {
        try {
            this.values = AsnAper.list(in, RANGE, false, false, GlobalEnbIdIeExtensions::readField);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public GlobalEnbIdIeExtensions(List<AsnAper.Field> values) {
        this.values = List.copyOf(values);
        AsnAper.size(values.size(), RANGE, false, false);
        this.values.forEach(GlobalEnbIdIeExtensions::validateField);
    }
    public List<AsnAper.Field> getValues() { return values; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.list(out, values, RANGE, false, false, AsnAper::field);
    }

    @Override
    public String toString() {
        return "GlobalEnbIdIeExtensions{" + '}';
    }
}
