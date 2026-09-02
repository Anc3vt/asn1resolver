package com.ancevt.asn1.model;

import java.util.ArrayList;
import java.util.List;

public final class WithSyntax {
    private final List<Element> elements;
    private final List<FieldPattern> fieldPatterns;
    private final SourceRange sourceRange;

    public WithSyntax(List<Element> elements, SourceRange sourceRange) {
        this.elements = List.copyOf(elements);
        this.sourceRange = sourceRange;
        List<FieldPattern> patterns = new ArrayList<>();
        flatten(this.elements, false, new ArrayList<>(), patterns);
        this.fieldPatterns = List.copyOf(patterns);
    }

    public List<Element> getElements() { return elements; }
    public List<FieldPattern> getFieldPatterns() { return fieldPatterns; }
    public SourceRange getSourceRange() { return sourceRange; }

    private static void flatten(List<Element> source, boolean optional, List<String> literals,
                                List<FieldPattern> output) {
        List<String> pending = new ArrayList<>(literals);
        for (Element element : source) {
            if (element instanceof Literal literal) {
                pending.add(literal.text());
            } else if (element instanceof Field field) {
                output.add(new FieldPattern(field.fieldName(), List.copyOf(pending), optional));
                pending.clear();
            } else if (element instanceof OptionalGroup group) {
                flatten(group.elements(), true, new ArrayList<>(pending), output);
                pending.clear();
            }
        }
    }

    public sealed interface Element permits Literal, Field, OptionalGroup { }
    public record Literal(String text, SourceRange sourceRange) implements Element { }
    public record Field(String fieldName, SourceRange sourceRange) implements Element { }
    public record OptionalGroup(List<Element> elements, SourceRange sourceRange) implements Element {
        public OptionalGroup { elements = List.copyOf(elements); }
    }
    public record FieldPattern(String fieldName, List<String> leadingLiterals, boolean optional) {
        public FieldPattern { leadingLiterals = List.copyOf(leadingLiterals); }
    }
}
