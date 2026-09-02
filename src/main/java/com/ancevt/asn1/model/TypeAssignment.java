package com.ancevt.asn1.model;

import java.util.List;
import java.util.Objects;

public final class TypeAssignment extends Assignment {
    private final List<FormalParameter> formalParameters;
    private final AsnType type;

    public TypeAssignment(String name, List<FormalParameter> formalParameters, AsnType type,
                          SourceRange sourceRange, String sourceText) {
        super(name, sourceRange, sourceText);
        this.formalParameters = List.copyOf(formalParameters);
        this.type = Objects.requireNonNull(type, "type");
    }

    public List<FormalParameter> getFormalParameters() { return formalParameters; }

    public AsnType getType() { return type; }

    @Override public SymbolKind getSymbolKind() { return SymbolKind.TYPE; }
}
