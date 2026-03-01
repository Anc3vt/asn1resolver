package com.ancevt.asn.model.type;

import java.util.List;

public final class ValueSetConstraint implements Constraint {

    private final List<ConstraintElement> elements;
    private final boolean extensible;

    public ValueSetConstraint(List<ConstraintElement> elements, boolean extensible) {
        this.elements = List.copyOf(elements);
        this.extensible = extensible;
    }

    public List<ConstraintElement> getElements() {
        return elements;
    }

    public boolean isExtensible() {
        return extensible;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "elements=" + elements +
                ", extensible=" + extensible +
                '}';
    }

}