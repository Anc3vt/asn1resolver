package tel.core.s1ap.core.model;

import java.util.Arrays;

/** Error handling policy carried by an S1AP message or information element. */
public enum Criticality {
    REJECT(0),
    IGNORE(1),
    NOTIFY(2);

    public static final int MAX_CODE = 2;
    private final int code;

    Criticality(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Criticality valueOf(int code) {
        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElse(null);
    }
}
