package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Reverse links are kept outside declarations so declaration objects stay compact. */
public final class ReferenceIndex {
    private final Map<NamedElement, List<SymbolReference>> references = new IdentityHashMap<>();

    public void add(SymbolReference reference) {
        if (reference.getTarget() != null) {
            references.computeIfAbsent(reference.getTarget(), ignored -> new ArrayList<>()).add(reference);
        }
    }

    public List<SymbolReference> getReferencesTo(NamedElement target) {
        return Collections.unmodifiableList(references.getOrDefault(target, List.of()));
    }

    public int size() { return references.values().stream().mapToInt(List::size).sum(); }

    public void clear() { references.clear(); }
}
