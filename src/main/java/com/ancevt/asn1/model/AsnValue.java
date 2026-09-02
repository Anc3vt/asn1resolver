package com.ancevt.asn1.model;

import java.util.Objects;

public abstract class AsnValue {
    private final String sourceText;
    private final SourceRange sourceRange;

    protected AsnValue(String sourceText, SourceRange sourceRange) {
        this.sourceText = sourceText;
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    public String getSourceText() { return sourceText; }
    public SourceRange getSourceRange() { return sourceRange; }
    @Override public String toString() { return sourceText; }
}
