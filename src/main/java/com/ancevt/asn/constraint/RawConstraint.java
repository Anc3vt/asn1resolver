package com.ancevt.asn.constraint;

public final class RawConstraint implements Constraint {

    private final String expression;

    public RawConstraint(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "(" + expression + ")";
    }
}