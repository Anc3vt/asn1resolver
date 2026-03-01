package com.ancevt.asn.model.type;

public final class IoFieldRefType extends AbstractType {

    private final String className;       // только имя
    private final String classFieldName;
    private final IoSelection selection;
    private AsnType resolvedType;

    public IoFieldRefType(String className,
                          String classFieldName,
                          IoSelection selection) {

        this.className = className;
        this.classFieldName = classFieldName;
        this.selection = selection;
    }

    public String getClassName() {
        return className;
    }

    public String getClassFieldName() {
        return classFieldName;
    }

    public IoSelection getSelection() {
        return selection;
    }

    public void setResolvedType(AsnType type) {
        this.resolvedType = type;
    }

    public AsnType getResolvedType() {
        return resolvedType;
    }
}