package com.ancevt.asn.model.type;

import java.util.Objects;

public final class SizeConstraint implements Constraint {

    private final ValueSetConstraint inner;

    public SizeConstraint(ValueSetConstraint inner) {
        this.inner = Objects.requireNonNull(inner);
    }

    public ValueSetConstraint getInner() {
        return inner;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "inner=" + inner +
                '}';
    }
}