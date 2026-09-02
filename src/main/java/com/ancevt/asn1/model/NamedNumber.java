package com.ancevt.asn1.model;

import java.math.BigInteger;

public final class NamedNumber implements NamedElement {
    private final String name;
    private final BigInteger numericValue;
    private final SymbolReference definedValue;
    private final SourceRange sourceRange;

    public NamedNumber(String name, BigInteger numericValue, SymbolReference definedValue, SourceRange sourceRange) {
        this.name = name;
        this.numericValue = numericValue;
        this.definedValue = definedValue;
        this.sourceRange = sourceRange;
    }

    @Override public String getName() { return name; }
    @Override public SourceRange getSourceRange() { return sourceRange; }
    public BigInteger getNumericValue() { return numericValue; }
    public SymbolReference getDefinedValue() { return definedValue; }
    @Override public String toString() { return name + "(" + (numericValue != null ? numericValue : definedValue) + ")"; }
}
