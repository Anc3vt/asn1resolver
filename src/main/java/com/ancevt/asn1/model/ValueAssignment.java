package com.ancevt.asn1.model;

import java.util.Objects;

public final class ValueAssignment extends Assignment {
    private final AsnType governor;
    private final AsnValue value;

    public ValueAssignment(String name, AsnType governor, AsnValue value,
                           SourceRange sourceRange, String sourceText) {
        super(name, sourceRange, sourceText);
        this.governor = Objects.requireNonNull(governor, "governor");
        this.value = Objects.requireNonNull(value, "value");
    }

    public AsnType getGovernor() { return governor; }

    public AsnValue getValue() { return value; }

    @Override public SymbolKind getSymbolKind() { return SymbolKind.VALUE; }
}
