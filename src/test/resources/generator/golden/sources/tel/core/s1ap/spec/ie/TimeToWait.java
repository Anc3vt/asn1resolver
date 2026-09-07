// Generated from S1AP ASN.1 type TimeToWait, ProtocolIE-ID 65.
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

public final class TimeToWait implements InformationElement {
    public enum Value {
        V1S(0),
        V2S(1),
        V5S(2),
        V10S(3),
        V20S(4),
        V60S(5),
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

    public TimeToWait(BitInput in) {
        try {
            int index = AsnAper.index(in, 6, true, true);
            long code = index < 0 ? 6L - index - 1 : index;
            this.value = code <= Integer.MAX_VALUE ? Value.valueOf((int) code) : Value.UNKNOWN;
        } catch (RuntimeException e) {
            throw AsnAper.protocol(e);
        }
    }

    public TimeToWait(Value value) {
        Objects.requireNonNull(value, "value");
        this.value = value;
    }

    public Value getValue() { return value; }
    public int getCode() { return value.code; }

    @Override
    public void encode(BitOutput out) {
        if (value == Value.UNKNOWN) throw new IllegalStateException("Cannot encode UNKNOWN ENUMERATED value");
        AsnAper.index(out, value.code >= 6 ? value.code - 6 : value.code, value.code >= 6, 6, true, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + value + '}';
    }
}
