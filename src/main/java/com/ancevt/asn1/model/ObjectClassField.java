package com.ancevt.asn1.model;

import java.util.Objects;

public final class ObjectClassField implements NamedElement {
    private final String name;
    private final AsnType governor;
    private final boolean typeField;
    private final boolean unique;
    private final boolean optional;
    private final AsnValue defaultValue;
    private final SourceRange sourceRange;

    public ObjectClassField(String name, AsnType governor, boolean typeField, boolean unique,
                            boolean optional, AsnValue defaultValue, SourceRange sourceRange) {
        this.name = Objects.requireNonNull(name, "name");
        this.governor = governor;
        this.typeField = typeField;
        this.unique = unique;
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    @Override public String getName() { return name; }
    @Override public SourceRange getSourceRange() { return sourceRange; }
    public AsnType getGovernor() { return governor; }
    public boolean isTypeField() { return typeField; }
    public boolean isUnique() { return unique; }
    public boolean isOptional() { return optional; }
    public AsnValue getDefaultValue() { return defaultValue; }
    @Override public String toString() { return "&" + name + (typeField ? " TYPE" : " " + governor); }
}
