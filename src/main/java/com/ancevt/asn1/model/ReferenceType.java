package com.ancevt.asn1.model;

import java.util.List;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReferenceType extends AsnType {
    private final SymbolReference reference;
    private final List<ActualParameter> actualParameters;

    public ReferenceType(SymbolReference reference, List<ActualParameter> actualParameters,
                         List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.reference = Objects.requireNonNull(reference, "reference");
        this.actualParameters = List.copyOf(actualParameters);
    }

    public SymbolReference getReference() { return reference; }
    public String getName() { return reference.getName(); }
    public List<ActualParameter> getActualParameters() { return actualParameters; }
    public Map<FormalParameter, ActualParameter> getParameterBindings() {
        Map<FormalParameter, ActualParameter> bindings = new LinkedHashMap<>();
        for (ActualParameter actual : actualParameters) {
            if (actual.getFormalParameter() != null) bindings.put(actual.getFormalParameter(), actual);
        }
        return Map.copyOf(bindings);
    }
    public TypeAssignment getTarget() { return reference.targetAs(TypeAssignment.class).orElse(null); }
    @Override public String toString() { return reference.getQualifiedName() + (actualParameters.isEmpty() ? "" : actualParameters); }
}
