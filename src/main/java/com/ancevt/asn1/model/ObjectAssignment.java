package com.ancevt.asn1.model;

import java.util.Objects;

public final class ObjectAssignment extends Assignment {
    private final SymbolReference objectClassReference;
    private final ObjectDefinition definition;

    public ObjectAssignment(String name, SymbolReference objectClassReference, ObjectDefinition definition,
                            SourceRange sourceRange, String sourceText) {
        super(name, sourceRange, sourceText);
        this.objectClassReference = Objects.requireNonNull(objectClassReference, "objectClassReference");
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public SymbolReference getObjectClassReference() { return objectClassReference; }

    public ObjectClassAssignment getObjectClass() {
        return objectClassReference.targetAs(ObjectClassAssignment.class).orElse(null);
    }

    public ObjectDefinition getDefinition() { return definition; }

    @Override public SymbolKind getSymbolKind() { return SymbolKind.OBJECT; }
}
