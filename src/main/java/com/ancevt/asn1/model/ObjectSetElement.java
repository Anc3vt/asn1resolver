package com.ancevt.asn1.model;

import java.util.Objects;

public final class ObjectSetElement {
    private final Kind kind;
    private final ObjectDefinition inlineObject;
    private final SymbolReference reference;
    private final SourceRange sourceRange;

    private ObjectSetElement(Kind kind, ObjectDefinition inlineObject,
                             SymbolReference reference, SourceRange sourceRange) {
        this.kind = kind;
        this.inlineObject = inlineObject;
        this.reference = reference;
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    public static ObjectSetElement inline(ObjectDefinition value, SourceRange range) {
        return new ObjectSetElement(Kind.INLINE_OBJECT, value, null, range);
    }
    public static ObjectSetElement reference(SymbolReference reference, SourceRange range) {
        return new ObjectSetElement(Kind.REFERENCE, null, reference, range);
    }
    public static ObjectSetElement extension(SourceRange range) {
        return new ObjectSetElement(Kind.EXTENSION_MARKER, null, null, range);
    }
    public Kind getKind() { return kind; }
    public ObjectDefinition getInlineObject() { return inlineObject; }
    public SymbolReference getReference() { return reference; }
    public SourceRange getSourceRange() { return sourceRange; }
    public enum Kind { INLINE_OBJECT, REFERENCE, EXTENSION_MARKER }
}
