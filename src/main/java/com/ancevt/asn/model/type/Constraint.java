package com.ancevt.asn.model.type;

public sealed interface Constraint permits RangeConstraint, RawConstraint, SizeConstraint, ValueSetConstraint {
}