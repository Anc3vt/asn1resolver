package com.ancevt.asn1.model;

/** One concrete type admitted by an information-object field/table constraint. */
public record OpenTypeAlternative(
        String selectorValue,
        Assignment selectorTarget,
        TypeAssignment typeTarget,
        ObjectDefinition sourceObject
) {
}
