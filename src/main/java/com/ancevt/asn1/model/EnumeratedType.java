package com.ancevt.asn1.model;

import java.util.List;
import java.util.Optional;

public final class EnumeratedType extends AsnType {
    private final List<EnumerationItem> items;
    private final boolean extensible;

    public EnumeratedType(List<EnumerationItem> items, boolean extensible,
                          List<Constraint> constraints, SourceRange sourceRange) {
        super(sourceRange, constraints);
        this.items = List.copyOf(items);
        this.extensible = extensible;
    }

    public List<EnumerationItem> getItems() { return items; }
    public boolean isExtensible() { return extensible; }
    public Optional<EnumerationItem> findItem(String name) {
        return items.stream().filter(v -> v.getName().equals(name)).findFirst();
    }
    @Override public String toString() { return "ENUMERATED{" + items.size() + (extensible ? ", ..." : "") + "}"; }
}
