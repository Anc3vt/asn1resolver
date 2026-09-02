package com.ancevt.asn1.model;

import java.util.List;
import java.util.Objects;

public final class TaggedType extends AsnType {
    private final String tagClass;
    private final String tagNumber;
    private final Mode mode;
    private final AsnType type;

    public TaggedType(String tagClass, String tagNumber, Mode mode, AsnType type,
                      List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.tagClass = tagClass;
        this.tagNumber = Objects.requireNonNull(tagNumber, "tagNumber");
        this.mode = mode;
        this.type = Objects.requireNonNull(type, "type");
    }
    public String getTagClass() { return tagClass; }
    public String getTagNumber() { return tagNumber; }
    public Mode getMode() { return mode; }
    public AsnType getType() { return type; }
    public enum Mode { EXPLICIT, IMPLICIT, DEFAULT }
}
