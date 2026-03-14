package com.ancevt.asn.constraint;

public final class RangeElement implements ConstraintElement {

    String lower;
    String upper;

    public RangeElement(String lower, String upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public String getLower() {
        return lower;
    }

    public String getUpper() {
        return upper;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "lower=" + lower +
                ", upper=" + upper +
                '}';
    }
}