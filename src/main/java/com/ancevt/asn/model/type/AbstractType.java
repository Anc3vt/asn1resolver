package com.ancevt.asn.model.type;

public abstract sealed class AbstractType implements AsnType
        permits BuiltinType, ChoiceType, EnumeratedType, IntegerType, IoFieldRefType, SequenceOfType, SequenceType, TypeRef {

    private Constraint constraint;

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                (constraint != null ? "constraint=" + constraint : "") +
                '}';
    }
}