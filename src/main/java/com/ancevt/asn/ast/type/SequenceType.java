package com.ancevt.asn.ast.type;

import java.util.List;
import java.util.Objects;

public final class SequenceType extends AbstractType {

    private final List<Field> fields;

    public SequenceType(List<Field> fields) {
        this.fields = List.copyOf(Objects.requireNonNull(fields));
    }

    public List<Field> getFields() {
        return fields;
    }
}
