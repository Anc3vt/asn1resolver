package com.ancevt.asn.constraint;

public final class SingleValueElement implements ConstraintElement {
    private String value;

    public SingleValueElement(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "value=" + value +
                '}';
    }
}
