package com.ancevt.asn1.model;

import java.util.List;

public final class CollectionValue extends AsnValue {
    private final List<Entry> entries;

    public CollectionValue(List<Entry> entries, String sourceText, SourceRange sourceRange) {
        super(sourceText, sourceRange);
        this.entries = List.copyOf(entries);
    }

    public List<Entry> getEntries() { return entries; }

    public record Entry(String name, AsnValue value, SourceRange sourceRange) { }
}
