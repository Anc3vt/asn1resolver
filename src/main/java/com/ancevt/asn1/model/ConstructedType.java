package com.ancevt.asn1.model;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class ConstructedType extends AsnType {
    private final Kind kind;
    private final List<Component> components;
    private final boolean extensible;

    public ConstructedType(Kind kind, List<Component> components, boolean extensible,
                           List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.kind = kind;
        this.components = List.copyOf(components);
        this.extensible = extensible;
    }

    public Kind getKind() { return kind; }
    public List<Component> getComponents() { return components; }
    public boolean isExtensible() { return extensible; }
    public Optional<Component> findComponent(String name) {
        return components.stream().filter(c -> c.getName().equals(name)).findFirst();
    }
    public Component requireComponent(String name) {
        return findComponent(name).orElseThrow(() -> new NoSuchElementException(kind + " component not found: " + name));
    }
    @Override public String toString() { return kind + "{" + components.size() + (extensible ? ", ..." : "") + "}"; }

    public enum Kind { SEQUENCE, SET, CHOICE }
}
