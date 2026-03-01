package com.ancevt.asn.model.type;

import java.util.Objects;

public final class Field {

    private final String name;
    private final AsnType type;
    private final boolean optional;

    public Field(String name, AsnType type, boolean optional) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.optional = optional;
    }

    public String getName() {
        return name;
    }

    public AsnType getType() {
        return type;
    }

    public boolean isOptional() {
        return optional;
    }
}