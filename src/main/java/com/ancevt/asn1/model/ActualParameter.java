package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActualParameter {
    private final String sourceText;
    private final SourceRange sourceRange;
    private final List<SymbolReference> references;
    private FormalParameter formalParameter;

    public ActualParameter(String sourceText, SourceRange sourceRange, List<SymbolReference> references) {
        this.sourceText = sourceText;
        this.sourceRange = sourceRange;
        this.references = new ArrayList<>(references);
    }

    public String getSourceText() { return sourceText; }

    public SourceRange getSourceRange() { return sourceRange; }

    public List<SymbolReference> getReferences() { return Collections.unmodifiableList(references); }

    public FormalParameter getFormalParameter() { return formalParameter; }

    public void bindTo(FormalParameter formalParameter) { this.formalParameter = formalParameter; }

    @Override public String toString() { return sourceText; }
}
