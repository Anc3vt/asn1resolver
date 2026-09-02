package com.ancevt.asn1.model;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.math.BigInteger;

public final class Constraint {
    private final Kind kind;
    private final String sourceText;
    private final SourceRange sourceRange;
    private final List<Constraint> children;
    private final List<SymbolReference> references;
    private final List<String> selectorPath;
    private final boolean extensible;
    private final BigInteger literalInteger;
    private final List<NamedElement> selectorTargets = new ArrayList<>();

    public Constraint(Kind kind, String sourceText, SourceRange sourceRange,
                      List<Constraint> children, List<SymbolReference> references,
                      List<String> selectorPath) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.sourceText = sourceText;
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
        this.children = List.copyOf(children);
        this.references = List.copyOf(references);
        this.selectorPath = List.copyOf(selectorPath);
        this.extensible = sourceText != null && sourceText.contains("...");
        this.literalInteger = parseInteger(kind, sourceText);
    }

    public Kind getKind() { return kind; }
    public String getSourceText() { return sourceText; }
    public SourceRange getSourceRange() { return sourceRange; }
    public List<Constraint> getChildren() { return children; }
    public List<SymbolReference> getReferences() { return references; }
    public List<String> getSelectorPath() { return selectorPath; }
    public boolean isExtensible() { return extensible; }
    public BigInteger getLiteralInteger() { return literalInteger; }
    public List<NamedElement> getSelectorTargets() { return Collections.unmodifiableList(selectorTargets); }
    public void addSelectorTarget(NamedElement target) {
        Objects.requireNonNull(target, "target");
        if (!selectorTargets.contains(target)) selectorTargets.add(target);
    }
    @Override public String toString() { return sourceText; }

    public enum Kind { SIZE, RANGE, UNION, INTERSECTION, TABLE, SINGLE_VALUE, CONTENTS, RAW }

    private static BigInteger parseInteger(Kind kind, String text) {
        if (kind != Kind.SINGLE_VALUE || text == null) return null;
        String candidate = text.trim();
        try {
            return candidate.matches("[+-]?\\d+") ? new BigInteger(candidate) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
