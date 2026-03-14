package com.ancevt.asn.constraint;

public sealed interface Constraint permits RangeConstraint, RawConstraint, SizeConstraint, ValueSetConstraint {
}