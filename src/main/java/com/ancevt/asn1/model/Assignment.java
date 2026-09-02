package com.ancevt.asn1.model;

import java.util.Objects;

public abstract class Assignment implements NamedElement {
    private final String name;
    private final SourceRange sourceRange;
    private final String sourceText;
    private Asn1Module module;

    protected Assignment(String name, SourceRange sourceRange, String sourceText) {
        this.name = Objects.requireNonNull(name, "name");
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
        this.sourceText = sourceText;
    }

    @Override public final String getName() { return name; }

    @Override public final SourceRange getSourceRange() { return sourceRange; }

    public final String getSourceText() { return sourceText; }

    public final Asn1Module getModule() { return module; }

    final void attachTo(Asn1Module module) { this.module = Objects.requireNonNull(module, "module"); }

    public abstract SymbolKind getSymbolKind();

    @Override public String toString() { return name + " [" + getSymbolKind() + "]"; }
}
