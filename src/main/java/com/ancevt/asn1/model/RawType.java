package com.ancevt.asn1.model;

import java.util.List;

/** Lossless fallback for a valid construct that is not yet structurally interpreted. */
public final class RawType extends AsnType {
    private final String sourceText;

    public RawType(String sourceText, List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.sourceText = sourceText;
    }

    public String getSourceText() { return sourceText; }
    @Override public String toString() { return sourceText; }
}
