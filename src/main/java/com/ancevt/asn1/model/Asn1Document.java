package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class Asn1Document {
    private final String sourceName;
    private final String sourceText;
    private final List<Asn1Module> modules = new ArrayList<>();
    private final Map<String, Asn1Module> modulesByName = new LinkedHashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final ReferenceIndex referenceIndex = new ReferenceIndex();

    public Asn1Document(String sourceName, String sourceText) {
        this.sourceName = sourceName == null ? "<memory>" : sourceName;
        this.sourceText = sourceText;
    }

    public String getSourceName() { return sourceName; }

    public String getSourceText() { return sourceText; }

    public List<Asn1Module> getModules() { return Collections.unmodifiableList(modules); }

    public Map<String, Asn1Module> getModulesByName() { return Collections.unmodifiableMap(modulesByName); }

    public Optional<Asn1Module> findModule(String name) { return Optional.ofNullable(modulesByName.get(name)); }

    public Asn1Module requireModule(String name) {
        Asn1Module module = modulesByName.get(name);
        if (module == null) throw new NoSuchElementException("ASN.1 module not found: " + name);
        return module;
    }

    public List<Diagnostic> getDiagnostics() { return Collections.unmodifiableList(diagnostics); }

    public ReferenceIndex getReferenceIndex() { return referenceIndex; }

    public long getErrorCount() {
        return diagnostics.stream().filter(d -> d.severity() == Diagnostic.Severity.ERROR).count();
    }

    public int getAssignmentCount() {
        return modules.stream().mapToInt(m -> m.getAssignments().size()).sum();
    }

    public String sourceText(SourceRange range) { return range.textFrom(sourceText); }

    public void addModule(Asn1Module module) {
        modules.add(module);
        modulesByName.put(module.getName(), module);
    }

    public void addDiagnostic(Diagnostic diagnostic) { diagnostics.add(diagnostic); }

    @Override public String toString() {
        return "Asn1Document{" + modules.size() + " modules, " + getAssignmentCount() + " assignments}";
    }
}
