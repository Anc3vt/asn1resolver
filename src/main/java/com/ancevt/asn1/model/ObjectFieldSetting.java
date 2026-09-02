package com.ancevt.asn1.model;

import java.util.Objects;

public final class ObjectFieldSetting implements NamedElement {
    private final String name;
    private final Kind kind;
    private final String sourceText;
    private final SourceRange sourceRange;
    private final SymbolReference reference;
    private ObjectClassField field;
    private AsnType typeValue;

    public ObjectFieldSetting(String name, Kind kind, String sourceText, SourceRange sourceRange,
                              SymbolReference reference) {
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.sourceText = sourceText;
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
        this.reference = reference;
    }

    @Override public String getName() { return name; }
    @Override public SourceRange getSourceRange() { return sourceRange; }
    public Kind getKind() { return kind; }
    public String getSourceText() { return sourceText; }
    public SymbolReference getReference() { return reference; }
    public NamedElement getValueTarget() { return reference == null ? null : reference.getTarget(); }
    public AsnType getTypeValue() { return typeValue; }
    public ObjectClassField getField() { return field; }
    public void linkField(ObjectClassField field) { this.field = Objects.requireNonNull(field, "field"); }
    public void setTypeValue(AsnType typeValue) { this.typeValue = typeValue; }
    @Override public String toString() { return "&" + name + " = " + sourceText; }
    public enum Kind { TYPE, VALUE, OBJECT, OBJECT_SET, RAW }
}
