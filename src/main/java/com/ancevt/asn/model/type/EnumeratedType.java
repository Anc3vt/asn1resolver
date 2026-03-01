package com.ancevt.asn.model.type;

import java.util.List;

public final class EnumeratedType extends AbstractType {

    private final List<String> values;

    public EnumeratedType(List<String> values) {
        this.values = List.copyOf(values);
    }

    public List<String> getValues() {
        return values;
    }
}