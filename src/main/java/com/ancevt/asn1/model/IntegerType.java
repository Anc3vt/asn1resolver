package com.ancevt.asn1.model;

import java.util.List;

public final class IntegerType extends AsnType {
    private final List<NamedNumber> namedNumbers;

    public IntegerType(List<NamedNumber> namedNumbers, List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.namedNumbers = List.copyOf(namedNumbers);
    }

    public List<NamedNumber> getNamedNumbers() { return namedNumbers; }

    @Override public String toString() { return "INTEGER" + (namedNumbers.isEmpty() ? "" : namedNumbers); }
}
