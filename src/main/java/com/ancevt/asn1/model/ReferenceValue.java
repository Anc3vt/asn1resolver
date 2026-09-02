package com.ancevt.asn1.model;

import java.util.Objects;

public final class ReferenceValue extends AsnValue {
    private final SymbolReference reference;

    public ReferenceValue(SymbolReference reference, String sourceText, SourceRange sourceRange) {
        super(sourceText, sourceRange);
        this.reference = Objects.requireNonNull(reference, "reference");
    }

    public SymbolReference getReference() { return reference; }
}
