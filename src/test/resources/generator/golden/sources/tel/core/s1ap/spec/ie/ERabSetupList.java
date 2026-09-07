// Generated from S1AP ASN.1 type E-RABSetupListCtxtSURes, ProtocolIE-ID 51.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.List;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class ERabSetupList implements InformationElement {
    private static final AsnAper.Range RANGE = new AsnAper.Range("1:256");
    private final List<ERabSetupItem> values;

    private static ERabSetupItem readItem(BitInput in) {
        return (ERabSetupItem) AsnAper.field(in, new int[] {50}, new int[] {1},
                List.of(ERabSetupItem::new)).value();
    }
    public ERabSetupList(BitInput in) {
        try {
            this.values = AsnAper.list(in, RANGE, false, true, ERabSetupList::readItem);
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public ERabSetupList(List<ERabSetupItem> values) {
        this.values = List.copyOf(values);
        AsnAper.size(values.size(), RANGE, false, true);
    }
    public ERabSetupList(ERabSetupItem... values) { this(List.of(values)); }
    public List<ERabSetupItem> getValues() { return values; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.list(out, values, RANGE, false, true,
                (target, value) -> AsnAper.field(target, new AsnAper.Field(50, 1, value)));
    }

    @Override
    public String toString() {
        return "ERabSetupList{" + "size=" + values.size() + '}';
    }
}
