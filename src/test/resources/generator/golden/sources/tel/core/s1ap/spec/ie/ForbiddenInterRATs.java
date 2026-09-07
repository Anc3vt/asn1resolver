// Generated from S1AP ASN.1 type ForbiddenInterRATs.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class ForbiddenInterRATs implements InformationElement {
    public enum Value {
        ALL(0, false),
        GERAN(1, false),
        UTRAN(2, false),
        CDMA2000(3, false),
        GERANANDUTRAN(0, true),
        CDMA2000ANDUTRAN(1, true);

        private final int index;
        private final boolean extensionAddition;
        Value(int index, boolean extensionAddition) {
            this.index = index;
            this.extensionAddition = extensionAddition;
        }
        public int getCode() { return index; }
        public boolean isExtensionAddition() { return extensionAddition; }
        public static Value fromRootIndex(int index) {
            if (index < 0) throw new S1apException("Negative root index");
            return fromIndex(index);
        }
        public static Value fromExtensionIndex(int index) {
            if (index < 0) throw new S1apException("Negative extension index");
            return fromIndex(-index - 1);
        }
        private static Value fromIndex(int code) {
            return switch (code) {
                case 0 -> ALL;
                case 1 -> GERAN;
                case 2 -> UTRAN;
                case 3 -> CDMA2000;
                case -1 -> GERANANDUTRAN;
                case -2 -> CDMA2000ANDUTRAN;
                default -> throw new S1apException("Unknown ENUMERATED index: " + code);
            };
        }
    }

    private final Value value;

    public ForbiddenInterRATs(BitInput in) {
        try {
            this.value = Value.fromIndex(AsnAper.index(in, 4, true, true));
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public ForbiddenInterRATs(Value value) {
        Objects.requireNonNull(value, "value");
        this.value = value;
    }

    public Value getValue() { return value; }
    public int getCode() { return value.index; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.index(out, value.index, value.extensionAddition, 4, true, true);
    }

    @Override
    public String toString() {
        return "ForbiddenInterRATs{" + value + '}';
    }
}
