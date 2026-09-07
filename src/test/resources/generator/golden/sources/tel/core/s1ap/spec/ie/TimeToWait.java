// Generated from S1AP ASN.1 type TimeToWait, ProtocolIE-ID 65.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Objects;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.error.S1apException;
import tel.core.s1ap.core.model.InformationElement;

public final class TimeToWait implements InformationElement {
    public enum Value {
        V1S(0, false),
        V2S(1, false),
        V5S(2, false),
        V10S(3, false),
        V20S(4, false),
        V60S(5, false);

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
                case 0 -> V1S;
                case 1 -> V2S;
                case 2 -> V5S;
                case 3 -> V10S;
                case 4 -> V20S;
                case 5 -> V60S;
                default -> throw new S1apException("Unknown ENUMERATED index: " + code);
            };
        }
    }

    private final Value value;

    public TimeToWait(BitInput in) {
        try {
            this.value = Value.fromIndex(AsnAper.index(in, 6, true, true));
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public TimeToWait(Value value) {
        Objects.requireNonNull(value, "value");
        this.value = value;
    }

    public Value getValue() { return value; }
    public int getCode() { return value.index; }

    @Override
    public void encode(BitOutput out) {
        AsnAper.index(out, value.index, value.extensionAddition, 6, true, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + value + '}';
    }
}
