package com.ancevt.asn.model.type;

public final class BuiltinType extends AbstractType {

    private final String name;

    public BuiltinType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}