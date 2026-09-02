package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class Asn1Module implements NamedElement {
    private final String name;
    private final List<OidComponent> definitiveIdentifier;
    private final TaggingMode taggingMode;
    private final boolean extensibilityImplied;
    private final SourceRange sourceRange;
    private final List<ModuleImport> imports = new ArrayList<>();
    private final List<Assignment> assignments = new ArrayList<>();
    private final Map<String, Assignment> assignmentsByName = new LinkedHashMap<>();

    public Asn1Module(String name, List<OidComponent> definitiveIdentifier,
                      TaggingMode taggingMode, boolean extensibilityImplied, SourceRange sourceRange) {
        this.name = name;
        this.definitiveIdentifier = List.copyOf(definitiveIdentifier);
        this.taggingMode = taggingMode;
        this.extensibilityImplied = extensibilityImplied;
        this.sourceRange = sourceRange;
    }

    @Override public String getName() { return name; }

    @Override public SourceRange getSourceRange() { return sourceRange; }

    public List<OidComponent> getDefinitiveIdentifier() { return definitiveIdentifier; }

    public TaggingMode getTaggingMode() { return taggingMode; }

    public boolean isExtensibilityImplied() { return extensibilityImplied; }

    public List<ModuleImport> getImports() { return Collections.unmodifiableList(imports); }

    public List<Assignment> getAssignments() { return Collections.unmodifiableList(assignments); }

    public Map<String, Assignment> getAssignmentsByName() { return Collections.unmodifiableMap(assignmentsByName); }

    public List<TypeAssignment> getTypeAssignments() { return assignmentsOf(TypeAssignment.class); }
    public List<ValueAssignment> getValueAssignments() { return assignmentsOf(ValueAssignment.class); }
    public List<ObjectClassAssignment> getObjectClassAssignments() { return assignmentsOf(ObjectClassAssignment.class); }
    public List<ObjectAssignment> getObjectAssignments() { return assignmentsOf(ObjectAssignment.class); }
    public List<ObjectSetAssignment> getObjectSetAssignments() { return assignmentsOf(ObjectSetAssignment.class); }

    public Optional<Assignment> findAssignment(String name) { return Optional.ofNullable(assignmentsByName.get(name)); }

    public Assignment requireAssignment(String name) {
        Assignment assignment = assignmentsByName.get(name);
        if (assignment == null) throw new NoSuchElementException("Assignment not found in " + this.name + ": " + name);
        return assignment;
    }

    public Optional<TypeAssignment> findType(String name) {
        return findAssignment(name).filter(TypeAssignment.class::isInstance).map(TypeAssignment.class::cast);
    }

    public TypeAssignment requireType(String name) {
        return findType(name).orElseThrow(() -> new NoSuchElementException("Type not found in " + this.name + ": " + name));
    }

    public Optional<ValueAssignment> findValue(String name) {
        return findAssignment(name).filter(ValueAssignment.class::isInstance).map(ValueAssignment.class::cast);
    }

    public Optional<ObjectClassAssignment> findObjectClass(String name) {
        return findAssignment(name).filter(ObjectClassAssignment.class::isInstance).map(ObjectClassAssignment.class::cast);
    }

    public Optional<ObjectAssignment> findObject(String name) {
        return findAssignment(name).filter(ObjectAssignment.class::isInstance).map(ObjectAssignment.class::cast);
    }

    public Optional<ObjectSetAssignment> findObjectSet(String name) {
        return findAssignment(name).filter(ObjectSetAssignment.class::isInstance).map(ObjectSetAssignment.class::cast);
    }

    public void addImport(ModuleImport moduleImport) { imports.add(moduleImport); }

    public void addAssignment(Assignment assignment) {
        assignment.attachTo(this);
        assignments.add(assignment);
        assignmentsByName.put(assignment.getName(), assignment);
    }

    private <T extends Assignment> List<T> assignmentsOf(Class<T> type) {
        return assignments.stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Override public String toString() { return name + " (" + assignments.size() + " assignments)"; }
}
