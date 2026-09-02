package com.ancevt.asn1.model;

import java.math.BigInteger;

public final class LiteralValue extends AsnValue {
    private final Kind kind;
    private final Object value;

    public LiteralValue(Kind kind, Object value, String sourceText, SourceRange sourceRange) {
        super(sourceText, sourceRange);
        this.kind = kind;
        this.value = value;
    }

    public static LiteralValue integer(BigInteger value, String text, SourceRange range) {
        return new LiteralValue(Kind.INTEGER, value, text, range);
    }

    public Kind getKind() { return kind; }
    public Object getValue() { return value; }
    public BigInteger getIntegerValue() { return value instanceof BigInteger number ? number : null; }
    public enum Kind { INTEGER, REAL, STRING, BIT_STRING, HEX_STRING, BOOLEAN, NULL }
}
