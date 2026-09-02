package com.ancevt.asn1.model;

import java.util.Objects;

public final class ObjectClassAssignment extends Assignment {
    private final ObjectClassDefinition definition;

    public ObjectClassAssignment(String name, ObjectClassDefinition definition,
                                 SourceRange sourceRange, String sourceText) {
        super(name, sourceRange, sourceText);
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public ObjectClassDefinition getDefinition() { return definition; }

    @Override public SymbolKind getSymbolKind() { return SymbolKind.OBJECT_CLASS; }
}
