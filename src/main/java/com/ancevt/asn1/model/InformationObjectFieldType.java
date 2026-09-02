package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class InformationObjectFieldType extends AsnType {
    private final SymbolReference objectClassReference;
    private final String fieldName;
    private final Constraint tableConstraint;
    private ObjectClassField field;
    private final List<OpenTypeAlternative> alternatives = new ArrayList<>();

    public InformationObjectFieldType(SymbolReference objectClassReference, String fieldName,
                                      Constraint tableConstraint, List<Constraint> constraints,
                                      SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.objectClassReference = Objects.requireNonNull(objectClassReference, "objectClassReference");
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
        this.tableConstraint = tableConstraint;
    }

    public SymbolReference getObjectClassReference() { return objectClassReference; }
    public ObjectClassAssignment getObjectClass() {
        return objectClassReference.targetAs(ObjectClassAssignment.class).orElse(null);
    }
    public String getFieldName() { return fieldName; }
    public ObjectClassField getField() { return field; }
    /** Fixed governor type, or null for an open type field such as &Value. */
    public AsnType getFixedType() { return field == null ? null : field.getGovernor(); }
    public Constraint getTableConstraint() { return tableConstraint; }
    public List<OpenTypeAlternative> getAlternatives() { return Collections.unmodifiableList(alternatives); }
    public void linkField(ObjectClassField field) { this.field = Objects.requireNonNull(field, "field"); }
    public void addAlternative(OpenTypeAlternative alternative) {
        if (!alternatives.contains(alternative)) alternatives.add(alternative);
    }
    @Override public String toString() { return objectClassReference.getQualifiedName() + ".&" + fieldName; }
}
