package com.ancevt.asn.model.type;

import java.util.Objects;

public final class SequenceOfType extends AbstractType {

    private final AsnType elementType;

    public SequenceOfType(AsnType elementType) {
        this.elementType = Objects.requireNonNull(elementType);
    }

    public AsnType getElementType() {
        return elementType;
    }
}