package com.ancevt.asn.model.type;

import java.util.List;

public final class IntegerType extends AbstractType {

    private final List<NamedNumber> namedNumbers;

    public IntegerType(List<NamedNumber> namedNumbers) {
        this.namedNumbers = List.copyOf(namedNumbers);
    }

    public List<NamedNumber> getNamedNumbers() {
        return namedNumbers;
    }
}
