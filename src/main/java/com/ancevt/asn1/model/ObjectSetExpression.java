package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ObjectSetExpression {
    private final List<ObjectSetElement> elements;
    private final SourceRange sourceRange;

    public ObjectSetExpression(List<ObjectSetElement> elements, SourceRange sourceRange) {
        this.elements = List.copyOf(elements);
        this.sourceRange = sourceRange;
    }

    public List<ObjectSetElement> getElements() { return elements; }
    public SourceRange getSourceRange() { return sourceRange; }

    /** Returns inline objects plus objects reached through linked object/object-set references. */
    public List<ObjectDefinition> getAllObjects() {
        List<ObjectDefinition> result = new ArrayList<>();
        collect(this, result, Collections.newSetFromMap(new IdentityHashMap<>()));
        return List.copyOf(result);
    }

    private static void collect(ObjectSetExpression expression, List<ObjectDefinition> output,
                                Set<ObjectSetExpression> visited) {
        if (!visited.add(expression)) return;
        for (ObjectSetElement element : expression.elements) {
            if (element.getInlineObject() != null) output.add(element.getInlineObject());
            if (element.getReference() != null) {
                NamedElement target = element.getReference().getTarget();
                if (target instanceof ObjectAssignment object) output.add(object.getDefinition());
                if (target instanceof ObjectSetAssignment set) collect(set.getExpression(), output, visited);
            }
        }
    }

    @Override public String toString() { return "ObjectSet{" + elements.size() + " elements}"; }
}
