package com.ancevt.asn.ast;

import com.ancevt.asn.ast.type.AsnType;
import com.ancevt.asn.ast.classdef.ClassType;
import com.ancevt.asn.ast.object.ObjectInstance;
import com.ancevt.asn.ast.object.ObjectSet;

import java.util.*;

public final class ModuleDefinition {

    private final String name;

    private final Map<String, AsnType> types = new LinkedHashMap<>();
    private final Map<String, ClassType> classes = new LinkedHashMap<>();
    private final Map<String, ObjectInstance> objects = new LinkedHashMap<>();
    private final Map<String, ObjectSet> objectSets = new LinkedHashMap<>();

    private final List<ImportDefinition> imports = new ArrayList<>();

    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, List<String>> typeFormalParameters;

    public ModuleDefinition(String name) {
        this.name = Objects.requireNonNull(name);
        this.typeFormalParameters = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public void addImport(ImportDefinition def) {
        imports.add(def);
    }

    public List<ImportDefinition> getImports() {
        return Collections.unmodifiableList(imports);
    }

    public void addValue(String name, Object value) {
        values.put(name, value);
    }

    public Optional<Object> getValue(String name) {
        return Optional.ofNullable(values.get(name));
    }

    public void addType(String name, AsnType type) {
        types.put(name, type);
    }

    public Optional<AsnType> getType(String name) {
        return Optional.ofNullable(types.get(name));
    }

    public Map<String, AsnType> getTypes() {
        return Collections.unmodifiableMap(types);
    }

    public void addClass(String name, ClassType clazz) {
        classes.put(name, clazz);
    }

    public Optional<ClassType> getClass(String name) {
        return Optional.ofNullable(classes.get(name));
    }

    public Map<String, ClassType> getClasses() {
        return Collections.unmodifiableMap(classes);
    }

    public void addObject(String name, ObjectInstance object) {
        objects.put(name, object);
    }

    public Optional<ObjectInstance> getObject(String name) {
        return Optional.ofNullable(objects.get(name));
    }

    public Map<String, ObjectInstance> getObjects() {
        return Collections.unmodifiableMap(objects);
    }

    public void addObjectSet(String name, ObjectSet set) {
        objectSets.put(name, set);
    }

    public Optional<ObjectSet> getObjectSet(String name) {
        return Optional.ofNullable(objectSets.get(name));
    }

    public Map<String, ObjectSet> getObjectSets() {
        return Collections.unmodifiableMap(objectSets);
    }

    public void addTypeFormalParameters(String typeName, List<String> params) {
        typeFormalParameters.put(typeName, List.copyOf(params));
    }

    public Optional<List<String>> getTypeFormalParameters(String typeName) {
        return Optional.ofNullable(typeFormalParameters.get(typeName));
    }
}
