package com.ancevt.asn1.model;

import java.util.Objects;

public final class Component implements NamedElement {
    private final String name;
    private final AsnType type;
    private final boolean optional;
    private final AsnValue defaultValue;
    private final boolean extensionAddition;
    private final boolean componentsOf;
    private final SourceRange sourceRange;

    public Component(String name, AsnType type, boolean optional, AsnValue defaultValue,
                     boolean extensionAddition, boolean componentsOf, SourceRange sourceRange) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.extensionAddition = extensionAddition;
        this.componentsOf = componentsOf;
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    @Override public String getName() { return name; }
    @Override public SourceRange getSourceRange() { return sourceRange; }
    public AsnType getType() { return type; }
    public boolean isOptional() { return optional; }
    public AsnValue getDefaultValue() { return defaultValue; }
    public boolean isExtensionAddition() { return extensionAddition; }
    public boolean isComponentsOf() { return componentsOf; }
    @Override public String toString() { return name + " " + type + (optional ? " OPTIONAL" : ""); }
}
