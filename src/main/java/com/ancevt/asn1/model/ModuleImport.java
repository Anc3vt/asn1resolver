package com.ancevt.asn1.model;

import java.util.List;
import java.util.Objects;

public final class ModuleImport {
    private final String moduleName;
    private final List<ImportedSymbol> symbols;
    private final List<OidComponent> assignedIdentifier;
    private final SourceRange sourceRange;
    private Asn1Module sourceModule;

    public ModuleImport(String moduleName, List<ImportedSymbol> symbols,
                        List<OidComponent> assignedIdentifier, SourceRange sourceRange) {
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.symbols = List.copyOf(symbols);
        this.assignedIdentifier = List.copyOf(assignedIdentifier);
        this.sourceRange = Objects.requireNonNull(sourceRange, "sourceRange");
    }

    public String getModuleName() { return moduleName; }

    public List<ImportedSymbol> getSymbols() { return symbols; }

    public List<OidComponent> getAssignedIdentifier() { return assignedIdentifier; }

    public SourceRange getSourceRange() { return sourceRange; }

    public Asn1Module getSourceModule() { return sourceModule; }

    public boolean isResolved() { return sourceModule != null; }

    public void linkTo(Asn1Module sourceModule) {
        this.sourceModule = Objects.requireNonNull(sourceModule, "sourceModule");
    }

    @Override public String toString() { return "FROM " + moduleName + " (" + symbols.size() + ")"; }
}
