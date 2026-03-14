package com.ancevt.asn.constraint;

import java.util.Objects;

public final class RangeConstraint implements Constraint {

    private final String lowerBound;
    private final String upperBound;

    public RangeConstraint(String lowerBound, String upperBound) {
        this.lowerBound = Objects.requireNonNull(lowerBound);
        this.upperBound = Objects.requireNonNull(upperBound);
    }

    public String getLowerBound() {
        return lowerBound;
    }

    public String getUpperBound() {
        return upperBound;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "lowerBound=" + lowerBound +
                ", upperBound=" + upperBound +
                '}';
    }
}