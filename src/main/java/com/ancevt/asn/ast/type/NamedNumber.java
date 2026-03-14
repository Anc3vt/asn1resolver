package com.ancevt.asn.ast.type;

public final class NamedNumber {

    private final String name;
    private final String value;

    public NamedNumber(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {return name;}

    public String getValue() {return value;}
}