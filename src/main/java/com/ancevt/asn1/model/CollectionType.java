package com.ancevt.asn1.model;

import java.util.List;
import java.util.Objects;

public final class CollectionType extends AsnType {
    private final Kind kind;
    private final String elementName;
    private final AsnType elementType;

    public CollectionType(Kind kind, String elementName, AsnType elementType,
                          List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.elementName = elementName;
        this.elementType = Objects.requireNonNull(elementType, "elementType");
    }

    public Kind getKind() { return kind; }
    public String getElementName() { return elementName; }
    public AsnType getElementType() { return elementType; }
    @Override public String toString() { return kind + " OF " + elementType; }
    public enum Kind { SEQUENCE_OF, SET_OF }
}
