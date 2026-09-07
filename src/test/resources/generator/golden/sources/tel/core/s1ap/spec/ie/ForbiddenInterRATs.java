// Generated from S1AP ASN.1 type ForbiddenInterRATs.
// Review before adding to production sources.
package tel.core.s1ap.spec.ie;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import tel.core.s1ap.core.asn.AsnAper;
import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.core.asn.BitOutput;
import tel.core.s1ap.core.model.InformationElement;

public final class ForbiddenInterRATs implements InformationElement {
    public enum Value {
        ALL(0),
        GERAN(1),
        UTRAN(2),
        CDMA2000(3),
        GERANANDUTRAN(4),
        CDMA2000ANDUTRAN(5),
        UNKNOWN(-1);

        private static final Map<Integer, Value> BY_CODE = Arrays.stream(values())
                .collect(Collectors.toMap(Value::getCode, value -> value));

        private final int code;

        Value(int code) { this.code = code; }

        public int getCode() { return code; }

        static Value valueOf(int code) {
            return BY_CODE.getOrDefault(code, UNKNOWN);
        }
    }

    private final Value value;

    public ForbiddenInterRATs(BitInput in) {
        try {
            int index = AsnAper.index(in, 4, true, true);
            long code = index < 0 ? 4L - index - 1 : index;
            this.value = code <= Integer.MAX_VALUE ? Value.valueOf((int) code) : Value.UNKNOWN;
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public ForbiddenInterRATs(Value value) {
        Objects.requireNonNull(value, "value");
        this.value = value;
    }

    public Value getValue() { return value; }
    public int getCode() { return value.code; }

    @Override
    public void encode(BitOutput out) {
        if (value == Value.UNKNOWN) throw new IllegalStateException("Cannot encode UNKNOWN ENUMERATED value");
        AsnAper.index(out, value.code >= 4 ? value.code - 4 : value.code, value.code >= 4, 4, true, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + value + '}';
    }
}
