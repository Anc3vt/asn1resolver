package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class ObjectDefinition {
    private final String sourceText;
    private final SourceRange sourceRange;
    private final List<String> syntaxTokens;
    private final List<SourceRange> syntaxTokenRanges;
    private final List<ObjectFieldSetting> settings = new ArrayList<>();
    private ObjectClassDefinition objectClass;

    public ObjectDefinition(String sourceText, SourceRange sourceRange,
                            List<String> syntaxTokens, List<SourceRange> syntaxTokenRanges) {
        this.sourceText = sourceText;
        this.sourceRange = sourceRange;
        this.syntaxTokens = List.copyOf(syntaxTokens);
        this.syntaxTokenRanges = List.copyOf(syntaxTokenRanges);
    }

    public String getSourceText() { return sourceText; }
    public SourceRange getSourceRange() { return sourceRange; }
    public List<String> getSyntaxTokens() { return syntaxTokens; }
    public List<SourceRange> getSyntaxTokenRanges() { return syntaxTokenRanges; }
    public ObjectClassDefinition getObjectClass() { return objectClass; }
    public List<ObjectFieldSetting> getSettings() { return Collections.unmodifiableList(settings); }
    public Optional<ObjectFieldSetting> findSetting(String name) {
        String normalized = name.startsWith("&") ? name.substring(1) : name;
        return settings.stream().filter(s -> s.getName().equals(normalized)).findFirst();
    }
    public ObjectFieldSetting requireSetting(String name) {
        return findSetting(name).orElseThrow(() -> new NoSuchElementException("Object setting not found: " + name));
    }
    public void linkClass(ObjectClassDefinition objectClass) { this.objectClass = objectClass; }
    public void addSetting(ObjectFieldSetting setting) { settings.add(setting); }
    @Override public String toString() { return "{" + settings.size() + " settings}"; }
}
