package com.ancevt.asn1.model;

import java.util.Objects;

public final class ObjectSetAssignment extends Assignment {
    private final SymbolReference objectClassReference;
    private final ObjectSetExpression expression;

    public ObjectSetAssignment(String name, SymbolReference objectClassReference, ObjectSetExpression expression,
                               SourceRange sourceRange, String sourceText) {
        super(name, sourceRange, sourceText);
        this.objectClassReference = Objects.requireNonNull(objectClassReference, "objectClassReference");
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public SymbolReference getObjectClassReference() { return objectClassReference; }

    public ObjectClassAssignment getObjectClass() {
        return objectClassReference.targetAs(ObjectClassAssignment.class).orElse(null);
    }

    public ObjectSetExpression getExpression() { return expression; }

    @Override public SymbolKind getSymbolKind() { return SymbolKind.OBJECT_SET; }
}
