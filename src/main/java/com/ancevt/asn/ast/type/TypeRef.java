package com.ancevt.asn.ast.type;

import java.util.List;

public final class TypeRef extends AbstractType {

    private final String referencedName;
    private final List<String> actualParameters;
    private AsnType resolvedType;

    public TypeRef(String referencedName) {
        this(referencedName, List.of());
    }

    public TypeRef(String referencedName, List<String> actualParameters) {
        this.referencedName = referencedName;
        this.actualParameters = List.copyOf(actualParameters);
    }

    public List<String> getActualParameters() {
        return actualParameters;
    }

    public String getReferencedName() {
        return referencedName;
    }

    public void setResolvedType(AsnType resolvedType) {
        this.resolvedType = resolvedType;
    }

    public AsnType getResolvedType() {
        return resolvedType;
    }
}