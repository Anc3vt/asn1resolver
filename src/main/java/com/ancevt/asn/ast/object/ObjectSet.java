package com.ancevt.asn.ast.object;

import java.util.*;

public final class ObjectSet {

    private final String name;
    private final String className;

    private final List<ObjectInstance> inlineObjects;
    private final List<String> referencedSetNames;

    private List<ObjectSet> resolvedReferences = List.of();

    public ObjectSet(String name,
                     String className,
                     List<ObjectInstance> inlineObjects,
                     List<String> referencedSetNames) {

        this.name = Objects.requireNonNull(name);
        this.className = Objects.requireNonNull(className);
        this.inlineObjects = new ArrayList<>(inlineObjects);
        this.referencedSetNames = List.copyOf(referencedSetNames);
    }

    public void addInlineObject(ObjectInstance obj) {
        this.inlineObjects.add(obj);
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public List<ObjectInstance> getInlineObjects() {
        return inlineObjects;
    }

    public List<String> getReferencedSetNames() {
        return referencedSetNames;
    }

    public void setResolvedReferences(List<ObjectSet> refs) {
        this.resolvedReferences = List.copyOf(refs);
    }

    public List<ObjectSet> getResolvedReferences() {
        return resolvedReferences;
    }
}