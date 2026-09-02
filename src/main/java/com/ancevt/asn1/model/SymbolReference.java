package com.ancevt.asn1.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A navigable reference. After linking, {@link #getTarget()} points directly at
 * the declaration represented by the spelling in the ASN.1 source.
 */
public final class SymbolReference {
    private final String moduleQualifier;
    private final String name;
    private final SymbolKind expectedKind;
    private final SourceRange sourceRange;
    private NamedElement target;

    public SymbolReference(String moduleQualifier, String name, SymbolKind expectedKind, SourceRange sourceRange) {
        this.moduleQualifier = moduleQualifier;
        this.name = Objects.requireNonNull(name, "name");
        this.expectedKind = Objects.requireNonNull(expectedKind, "expectedKind");
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    public String getModuleQualifier() { return moduleQualifier; }

    public String getName() { return name; }

    public String getQualifiedName() {
        return moduleQualifier == null ? name : moduleQualifier + "." + name;
    }

    public SymbolKind getExpectedKind() { return expectedKind; }

    public SourceRange getSourceRange() { return sourceRange; }

    public NamedElement getTarget() { return target; }

    public boolean isResolved() { return target != null; }

    public <T extends NamedElement> Optional<T> targetAs(Class<T> type) {
        return type.isInstance(target) ? Optional.of(type.cast(target)) : Optional.empty();
    }

    /** Intended for the linker; exposed to keep the model usable by custom linkers. */
    public void linkTo(NamedElement target) { this.target = Objects.requireNonNull(target, "target"); }

    @Override
    public String toString() {
        return getQualifiedName() + (target == null ? " -> ?" : " -> " + target.getName());
    }
}
