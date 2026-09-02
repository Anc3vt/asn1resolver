package com.ancevt.asn1.model;

import java.util.Objects;

public final class ImportedSymbol implements NamedElement {
    private final String name;
    private final SourceRange sourceRange;
    private Assignment target;

    public ImportedSymbol(String name, SourceRange sourceRange) {
        this.name = Objects.requireNonNull(name, "name");
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    @Override public String getName() { return name; }

    @Override public SourceRange getSourceRange() { return sourceRange; }

    public Assignment getTarget() { return target; }

    public boolean isResolved() { return target != null; }

    public void linkTo(Assignment target) { this.target = Objects.requireNonNull(target, "target"); }

    @Override public String toString() { return name + (target == null ? " -> ?" : " -> " + target); }
}
