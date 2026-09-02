package com.ancevt.asn1.model;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class ObjectClassDefinition {
    private final List<ObjectClassField> fields;
    private final WithSyntax withSyntax;
    private final SourceRange sourceRange;

    public ObjectClassDefinition(List<ObjectClassField> fields, WithSyntax withSyntax, SourceRange sourceRange) {
        this.fields = List.copyOf(fields);
        this.withSyntax = withSyntax;
        this.sourceRange = sourceRange;
    }

    public List<ObjectClassField> getFields() { return fields; }
    public WithSyntax getWithSyntax() { return withSyntax; }
    public SourceRange getSourceRange() { return sourceRange; }
    public Optional<ObjectClassField> findField(String name) {
        String normalized = name.startsWith("&") ? name.substring(1) : name;
        return fields.stream().filter(f -> f.getName().equals(normalized)).findFirst();
    }
    public ObjectClassField requireField(String name) {
        return findField(name).orElseThrow(() -> new NoSuchElementException("Object class field not found: " + name));
    }
}
