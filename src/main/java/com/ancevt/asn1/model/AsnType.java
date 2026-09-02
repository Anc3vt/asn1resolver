package com.ancevt.asn1.model;

import java.util.List;
import java.util.Objects;

public abstract class AsnType {
    private final SourceRange sourceRange;
    private final List<Constraint> constraints;

    protected AsnType(SourceRange sourceRange, List<Constraint> constraints) {
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
        this.constraints = List.copyOf(constraints);
    }

    public SourceRange getSourceRange() { return sourceRange; }

    public List<Constraint> getConstraints() { return constraints; }

    /** Follows reference aliases until a concrete type or a cycle is reached. */
    public AsnType dereference() {
        AsnType current = this;
        java.util.Set<AsnType> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (current instanceof ReferenceType ref && ref.getTarget() != null && visited.add(current)) {
            current = ref.getTarget().getType();
        }
        return current;
    }
}
