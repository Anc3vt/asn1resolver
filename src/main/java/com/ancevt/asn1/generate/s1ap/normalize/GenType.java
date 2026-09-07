package com.ancevt.asn1.generate.s1ap.normalize;

import com.ancevt.asn1.model.SourceRange;
import java.util.List;
import java.util.Map;

/** Renderer input. Contains no AST types, assignments or mutable model references. */
public record GenType(String asnName, String javaName, SourceRange sourceRange, Kind kind,
                      Bounds bounds, boolean extensible, String representation, List<Field> fields,
                      List<Item> items, String elementType, List<OpenAlternative> alternatives,
                      List<String> aliases, List<String> dependencies, List<String> requiredCapabilities,
                      Map<String, String> overrides) {
    public GenType {
        fields = List.copyOf(fields); items = List.copyOf(items); alternatives = List.copyOf(alternatives);
        aliases = List.copyOf(aliases); dependencies = List.copyOf(dependencies);
        requiredCapabilities = List.copyOf(requiredCapabilities);
        overrides = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(overrides));
    }
    public enum Kind { INTEGER, ENUMERATED, OCTET_STRING, BIT_STRING, PRINTABLE_STRING, SEQUENCE, CHOICE, COLLECTION, CONTAINER, NULL }
    public record Field(String asnName, String javaName, String javaType, boolean optional, boolean extension,
                        String defaultExpression, String emptyAbsentElementType, SourceRange sourceRange) {
        public String publicType() { return emptyAbsentElementType == null ? javaType : "List<" + emptyAbsentElementType + ">"; }
    }
    public record Item(String asnName, String javaName, int index, boolean extension, SourceRange sourceRange) { }
    public record OpenAlternative(int id, String criticality, String presence, String javaType, SourceRange sourceRange) { }
}
