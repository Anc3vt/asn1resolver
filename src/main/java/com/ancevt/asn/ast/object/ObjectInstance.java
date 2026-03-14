package com.ancevt.asn.ast.object;

import com.ancevt.asn.ast.classdef.ClassType;

import java.util.*;

public final class ObjectInstance {

    private final String name;
    private final String className;
    private final Map<String, Object> values = new LinkedHashMap<>();

    private ClassType resolvedClass;

    public ObjectInstance(String name, String className) {
        this.name = Objects.requireNonNull(name);
        this.className = Objects.requireNonNull(className);
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public void putValue(String classFieldName, Object value) {
        values.put(classFieldName, value);
    }

    public Optional<Object> getValue(String classFieldName) {
        return Optional.ofNullable(values.get(classFieldName));
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public void setResolvedClass(ClassType clazz) {
        this.resolvedClass = clazz;
    }

    public ClassType getResolvedClass() {
        return resolvedClass;
    }
}