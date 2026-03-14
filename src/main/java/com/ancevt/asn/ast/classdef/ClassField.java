package com.ancevt.asn.ast.classdef;


import com.ancevt.asn.ast.type.AsnType;

import java.util.Objects;

public final class ClassField {

    private final String name;
    private final AsnType type;
    private final boolean unique;

    public ClassField(String name, AsnType type, boolean unique) {
        this.name = Objects.requireNonNull(name);
        this.type = type;
        this.unique = unique;
    }

    public String getName() {
        return name;
    }

    public AsnType getType() {
        return type;
    }

    public boolean isUnique() {
        return unique;
    }
}
