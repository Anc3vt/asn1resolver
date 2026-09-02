package com.ancevt.asn1.model;

import java.util.Objects;

public final class FormalParameter implements NamedElement {
    private final String name;
    private final String governorText;
    private final SymbolReference governorReference;
    private final SymbolKind parameterKind;
    private final SourceRange sourceRange;

    public FormalParameter(String name, String governorText, SymbolReference governorReference,
                           SymbolKind parameterKind, SourceRange sourceRange) {
        this.name = Objects.requireNonNull(name, "name");
        this.governorText = governorText;
        this.governorReference = governorReference;
        this.parameterKind = Objects.requireNonNull(parameterKind, "parameterKind");
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    @Override public String getName() { return name; }

    @Override public SourceRange getSourceRange() { return sourceRange; }

    public String getGovernorText() { return governorText; }

    public SymbolReference getGovernorReference() { return governorReference; }

    public SymbolKind getParameterKind() { return parameterKind; }

    @Override public String toString() { return name + " : " + governorText; }
}
