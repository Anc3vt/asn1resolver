package com.ancevt.asn1.model;

import java.math.BigInteger;

public final class EnumerationItem implements NamedElement {
    private final String name;
    private final BigInteger numericValue;
    private final SymbolReference definedValue;
    private final boolean extensionAddition;
    private final SourceRange sourceRange;

    public EnumerationItem(String name, BigInteger numericValue, SymbolReference definedValue,
                           boolean extensionAddition, SourceRange sourceRange) {
        this.name = name;
        this.numericValue = numericValue;
        this.definedValue = definedValue;
        this.extensionAddition = extensionAddition;
        this.sourceRange = sourceRange;
    }

    @Override public String getName() { return name; }
    @Override public SourceRange getSourceRange() { return sourceRange; }
    public BigInteger getNumericValue() { return numericValue; }
    public SymbolReference getDefinedValue() { return definedValue; }
    public boolean isExtensionAddition() { return extensionAddition; }
    @Override public String toString() { return name + (numericValue == null ? "" : "(" + numericValue + ")"); }
}
