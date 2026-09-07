package com.ancevt.asn1.generate.s1ap.render;

import com.ancevt.asn1.generate.s1ap.docs.Documentation;
import com.ancevt.asn1.generate.s1ap.normalize.GenType;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Deterministic renderer: its input is exclusively immutable generator IR. */
public final class JavaRenderer {
    public record Parameter(String type, String name) { }
    public record Constructor(List<Parameter> parameters, String invocation, String suffix) {
        public Constructor { parameters = List.copyOf(parameters); }
    }
    public record Rendered(String source, List<Constructor> constructors) {
        public Rendered { constructors = List.copyOf(constructors); }
    }
    private final String pkg;
    private final boolean known;
    private final Documentation docs;
    public JavaRenderer(String pkg, boolean known, Documentation docs) { this.pkg = pkg; this.known = known; this.docs = docs; }
    private static final class Code {
        private final StringBuilder b = new StringBuilder();
        void line(String s) { b.append(s).append('\n'); }
        @Override public String toString() { return b.toString(); }
    }
    public Rendered render(GenType t, Integer id, List<String> refs) {
        Code code = new Code(); List<Constructor> ctors = new ArrayList<>();
        if (docs != null && !refs.isEmpty()) code.line(docs.comment(t.asnName() + " information element", refs, "").stripTrailing());
        code.line("public final class " + t.javaName() + " implements InformationElement {");
        if (t.bounds() != null) {
            String ranges = t.bounds().intervals().stream().map(i -> i.min() + ":" + i.max()).collect(Collectors.joining("|"));
            code.line("    private static final AsnAper.Range RANGE = new AsnAper.Range(\"" + ranges + "\");");
        }
        switch (t.kind()) {
            case INTEGER, ENUMERATED, OCTET_STRING, BIT_STRING, PRINTABLE_STRING -> primitive(code, t, ctors, refs);
            case SEQUENCE -> sequence(code, t, ctors, refs);
            case CHOICE -> choice(code, t, ctors, refs);
            case COLLECTION -> collection(code, t, ctors, refs);
            case CONTAINER -> container(code, t, ctors, refs);
            case NULL -> {
                code.line("    public " + t.javaName() + "() { }");
                decodeDoc(code, t, refs);
                code.line("    public " + t.javaName() + "(BitInput in) { Objects.requireNonNull(in, \"in\"); }");
                code.line("    @Override\n    public void encode(BitOutput out) { Objects.requireNonNull(out, \"out\"); }");
                ctors.add(new Constructor(List.of(), "new " + t.javaName() + "()", ""));
            }
        }
        code.line("\n    @Override");
        code.line("    public String toString() {");
        String summary = switch (t.kind()) {
            case OCTET_STRING -> " + \"bytes=\" + bytes.length";
            case BIT_STRING -> " + \"bits=\" + " + (t.representation().equals("AsnBitString.Value") ? "value.getBitLength()" : t.bounds().max());
            case INTEGER, ENUMERATED -> " + value";
            case PRINTABLE_STRING -> " + \"length=\" + value.length()";
            case COLLECTION -> " + \"size=\" + values.size()";
            case CHOICE -> " + choice";
            default -> "";
        };
        code.line("        return \"" + t.javaName() + "{\"" + summary + " + '}';");
        code.line("    }\n}");
        String body = code.toString();
        TreeSet<String> imports = new TreeSet<>();
        for (String name : List.of("java.math.BigInteger", "java.util.List", "java.util.Objects",
                "tel.core.s1ap.core.asn.AsnAper", "tel.core.s1ap.core.asn.AsnBitString",
                "tel.core.s1ap.core.asn.AsnPrintableString", "tel.core.s1ap.core.asn.BitInput", "tel.core.s1ap.core.asn.BitOutput",
                "tel.core.s1ap.core.error.S1apException", "tel.core.s1ap.core.model.InformationElement")) {
            String simple = name.substring(name.lastIndexOf('.') + 1);
            if (Pattern.compile("\\b" + simple + "\\b").matcher(body).find()) imports.add(name);
        }
        String header = "// Generated from S1AP ASN.1 type " + t.asnName() + (id == null ? "" : ", ProtocolIE-ID " + id) + ".\n"
                + "// Review before adding to production sources.\npackage " + pkg + ";\n\n"
                + imports.stream().map(i -> "import " + i + ";\n").collect(Collectors.joining()) + "\n";
        return new Rendered(header + body, ctors);
    }
    private void decodeDoc(Code c, GenType t, List<String> refs) {
        if (docs != null && !refs.isEmpty()) {
            c.line("\n    /**");
            c.line("     * Decodes " + Documentation.escape(t.asnName()) + " from {@link BitInput}.");
            c.line("     * @param in source {@link BitInput}"); c.line("     */");
        }
    }
    private void valueDoc(Code c, GenType t, List<String> refs) {
        if (docs != null && !refs.isEmpty()) c.line(docs.comment("Creates " + t.asnName(), refs, "    ").stripTrailing());
    }
    private String range(GenType t) { return t.bounds() == null ? "null" : "RANGE"; }
    private String args(GenType t) { return range(t) + ", " + t.extensible() + ", " + known; }
    private void beginDecode(Code c, GenType t, List<String> refs) {
        decodeDoc(c, t, refs); c.line("    public " + t.javaName() + "(BitInput in) {"); c.line("        try {");
    }
    private void endDecode(Code c) {
        c.line("        } catch (RuntimeException e) {"); c.line("            throw AsnAper.protocol(e);"); c.line("        }\n    }\n");
    }
    private void encodeStart(Code c) { c.line("\n    @Override\n    public void encode(BitOutput out) {"); }
    private static String cap(String name) { return Character.toUpperCase(name.charAt(0)) + name.substring(1); }
    private static String big(GenType t, String value) { return t.representation().equals("BigInteger") ? value : "BigInteger.valueOf(" + value + ")"; }
    private void primitive(Code c, GenType t, List<Constructor> ctors, List<String> refs) {
        boolean octets = t.kind() == GenType.Kind.OCTET_STRING;
        String field = octets ? "bytes" : "value", type = t.representation();
        boolean array = type.equals("byte[]");
        boolean scalarBit = t.kind() == GenType.Kind.BIT_STRING && !type.equals("AsnBitString.Value");
        if (t.kind() == GenType.Kind.ENUMERATED) enumeration(c, t);
        if (t.kind() == GenType.Kind.BIT_STRING) for (GenType.Item bit : t.items())
            c.line("    public static final int " + bit.javaName() + " = " + bit.index() + ";");
        c.line("    private final " + type + " " + field + ";\n");
        beginDecode(c, t, refs);
        String decode = switch (t.kind()) {
            case INTEGER -> "AsnAper.integer(in, " + args(t) + ")" + (type.equals("int") ? ".intValueExact()" : type.equals("long") ? ".longValueExact()" : "");
            case ENUMERATED -> "Value.fromIndex(AsnAper.index(in, " + rootCount(t) + ", " + t.extensible() + ", " + known + "))";
            case OCTET_STRING -> "AsnAper.octets(in, " + args(t) + ")";
            case BIT_STRING -> "AsnAper.bits(in, " + args(t) + ")" + (scalarBit ? type.equals("int") ? ".toInt()" : type.equals("long") ? ".toLong()" : ".getBytes()" : "");
            case PRINTABLE_STRING -> "AsnPrintableString.decodeAper(in, " + args(t) + ")";
            default -> throw new IllegalStateException();
        };
        c.line("            this." + field + " = " + decode + ";"); endDecode(c);
        valueDoc(c, t, refs);
        c.line("    public " + t.javaName() + "(" + type + " " + field + ") {");
        switch (t.kind()) {
            case INTEGER -> c.line("        AsnAper.validate(" + big(t, field) + ", " + args(t) + ");");
            case ENUMERATED -> {
                c.line("        Objects.requireNonNull(value, \"value\");");
                if (!known) c.line("        if (value.extensionAddition) throw new IllegalArgumentException(\"Extension enum is disabled\");");
            }
            default -> {
                if (!type.equals("int") && !type.equals("long")) c.line("        Objects.requireNonNull(" + field + ", \"" + field + "\");");
                if (t.kind() == GenType.Kind.PRINTABLE_STRING) c.line("        AsnAper.printable(value);");
                if (scalarBit) c.line("        " + bitValue(t) + ";");
                else {
                    String size = octets ? "bytes.length" : t.kind() == GenType.Kind.BIT_STRING ? "value.getBitLength()" : "value.length()";
                    c.line("        AsnAper.size(" + size + ", " + args(t) + ");");
                }
            }
        }
        c.line("        this." + field + " = " + field + (array ? ".clone()" : "") + ";\n    }");
        ctors.add(new Constructor(List.of(new Parameter(type, field)), "new " + t.javaName() + "(" + field + ")", ""));
        c.line("\n    public " + type + " get" + cap(field) + "() { return " + field + (array ? ".clone()" : "") + "; }");
        if (t.kind() == GenType.Kind.ENUMERATED) c.line("    public int getCode() { return value.index; }");
        encodeStart(c);
        String encode = switch (t.kind()) {
            case INTEGER -> "AsnAper.integer(out, " + big(t, field) + ", " + args(t) + ");";
            case ENUMERATED -> "AsnAper.index(out, value.index, value.extensionAddition, " + rootCount(t) + ", " + t.extensible() + ", " + known + ");";
            case OCTET_STRING -> "AsnAper.octets(out, bytes, " + args(t) + ");";
            case BIT_STRING -> "AsnAper.bits(out, " + (scalarBit ? bitValue(t) : "value") + ", " + args(t) + ");";
            case PRINTABLE_STRING -> "AsnPrintableString.encodeAper(out, value, " + args(t) + ");";
            default -> throw new IllegalStateException();
        };
        c.line("        " + encode); c.line("    }");
    }
    private String bitValue(GenType t) {
        return t.representation().equals("byte[]") ? "AsnBitString.Value.fromBytes(value, " + t.bounds().max() + ")"
                : "AsnAper.bitmap(" + (t.representation().equals("int") ? "Integer.toUnsignedLong(value)" : "value") + ", " + t.bounds().max() + ")";
    }
    private long rootCount(GenType t) { return t.items().stream().filter(i -> !i.extension()).count(); }
    private void enumeration(Code c, GenType t) {
        c.line("    public enum Value {");
        for (int n = 0; n < t.items().size(); n++) {
            GenType.Item i = t.items().get(n);
            c.line("        " + i.javaName() + "(" + i.index() + ", " + i.extension() + ")" + (n == t.items().size() - 1 ? ";" : ","));
        }
        c.line("\n        private final int index;\n        private final boolean extensionAddition;");
        c.line("        Value(int index, boolean extensionAddition) {\n            this.index = index;\n            this.extensionAddition = extensionAddition;\n        }");
        c.line("        public int getCode() { return index; }");
        c.line("        public boolean isExtensionAddition() { return extensionAddition; }");
        c.line("        public static Value fromRootIndex(int index) {\n            if (index < 0) throw new S1apException(\"Negative root index\");\n            return fromIndex(index);\n        }");
        c.line("        public static Value fromExtensionIndex(int index) {\n            if (index < 0) throw new S1apException(\"Negative extension index\");\n            return fromIndex(-index - 1);\n        }");
        c.line("        private static Value fromIndex(int code) {\n            return switch (code) {");
        for (GenType.Item i : t.items()) c.line("                case " + (i.extension() ? -i.index() - 1 : i.index()) + " -> " + i.javaName() + ";");
        c.line("                default -> throw new S1apException(\"Unknown ENUMERATED index: \" + code);\n            };\n        }\n    }\n");
    }
    private void fields(Code c, GenType t) {
        for (GenType.Field f : t.fields()) c.line("    private final " + f.publicType() + " " + f.javaName() + ";");
    }
    private void getters(Code c, GenType t) {
        for (GenType.Field f : t.fields()) c.line("    public " + f.publicType() + " get" + cap(f.javaName()) + "() { return " + f.javaName() + "; }");
    }
    private void sequence(Code c, GenType t, List<Constructor> ctors, List<String> refs) {
        fields(c, t); beginDecode(c, t, refs);
        List<GenType.Field> root = t.fields().stream().filter(f -> !f.extension()).toList();
        List<GenType.Field> ext = t.fields().stream().filter(GenType.Field::extension).toList();
        if (t.extensible()) {
            c.line("            boolean hasExtensions = in.readBit();");
            if (!known) c.line("            if (hasExtensions) throw new S1apException(\"Sequence extensions are disabled\");");
        }
        for (GenType.Field f : root) if (f.optional()) c.line("            boolean has" + cap(f.javaName()) + " = in.readBit();");
        for (GenType.Field f : root) {
            String absent = f.emptyAbsentElementType() != null ? "List.of()" : f.defaultExpression() == null ? "null" : f.defaultExpression();
            c.line("            this." + f.javaName() + " = " + (f.optional() ? "has" + cap(f.javaName()) + " ? " : "")
                    + "new " + f.javaType() + "(in)" + (f.emptyAbsentElementType() != null ? ".getValues()" : "") + (f.optional() ? " : " + absent : "") + ";");
        }
        if (known && t.extensible()) c.line("            BitInput[] additions = hasExtensions ? AsnAper.extensions(in, " + ext.size() + ") : new BitInput[" + ext.size() + "];");
        for (int i = 0; i < ext.size(); i++) {
            GenType.Field f = ext.get(i);
            c.line("            this." + f.javaName() + " = " + (known ? "additions[" + i + "] == null ? null : new " + f.javaType() + "(additions[" + i + "])" : "null") + ";");
            if (known) c.line("            if (additions[" + i + "] != null) AsnAper.finishOpen(additions[" + i + "]);");
        }
        endDecode(c);
        List<Parameter> params = t.fields().stream().map(f -> new Parameter(f.publicType(), f.javaName())).toList();
        valueDoc(c, t, refs);
        c.line("    public " + t.javaName() + "(" + declarations(params) + ") {");
        for (GenType.Field f : t.fields()) {
            if (!f.optional() && !f.extension()) c.line("        Objects.requireNonNull(" + f.javaName() + ", \"" + f.javaName() + "\");");
            if (f.extension() && !known) c.line("        if (" + f.javaName() + " != null) throw new IllegalArgumentException(\"Sequence extensions are disabled\");");
            if (f.emptyAbsentElementType() != null) {
                c.line("        this." + f.javaName() + " = List.copyOf(" + f.javaName() + ");");
                c.line("        if (!this." + f.javaName() + ".isEmpty()) new " + f.javaType() + "(this." + f.javaName() + ");");
            } else c.line("        this." + f.javaName() + " = " + (f.defaultExpression() != null ? f.javaName() + " == null ? " + f.defaultExpression() + " : " : "") + f.javaName() + ";");
        }
        c.line("    }\n"); getters(c, t);
        ctors.add(new Constructor(params, "new " + t.javaName() + "(" + arguments(params) + ")", ""));
        encodeStart(c);
        if (t.extensible()) {
            String presence = ext.stream().map(f -> f.javaName() + " != null").collect(Collectors.joining(" || "));
            c.line("        boolean hasExtensions = " + (known && !presence.isEmpty() ? presence : "false") + ";");
            c.line("        out.writeBit(hasExtensions);");
        }
        for (GenType.Field f : root) if (f.optional()) c.line("        out.writeBit(" + present(f) + ");");
        for (GenType.Field f : root) c.line("        " + (f.optional() ? "if (" + present(f) + ") " : "")
                + (f.emptyAbsentElementType() != null ? "new " + f.javaType() + "(" + f.javaName() + ")" : f.javaName()) + ".encode(out);");
        if (known && !ext.isEmpty()) c.line("        if (hasExtensions) AsnAper.extensions(out, " + ext.stream().map(GenType.Field::javaName).collect(Collectors.joining(", ")) + ");");
        c.line("    }");
    }
    private String present(GenType.Field f) {
        if (f.emptyAbsentElementType() != null) return "!" + f.javaName() + ".isEmpty()";
        if (f.defaultExpression() == null) return f.javaName() + " != null";
        return "!Objects.equals(" + f.javaName() + ".getValue(), " + f.defaultExpression() + ".getValue())";
    }
    private void choice(Code c, GenType t, List<Constructor> ctors, List<String> refs) {
        c.line("    public enum Choice { " + t.fields().stream().map(f -> f.javaName().toUpperCase(Locale.ROOT)).collect(Collectors.joining(", ")) + " }");
        c.line("    private final Choice choice;"); fields(c, t);
        int roots = (int) t.fields().stream().filter(f -> !f.extension()).count();
        beginDecode(c, t, refs);
        c.line("            int index = AsnAper.index(in, " + roots + ", " + t.extensible() + ", " + known + ");");
        c.line("            BitInput content = index < 0 ? AsnAper.open(in) : in;");
        c.line("            this.choice = switch (index) {");
        int ri = 0, ei = 0;
        for (GenType.Field f : t.fields()) c.line("                case " + (f.extension() ? -++ei : ri++) + " -> Choice." + f.javaName().toUpperCase(Locale.ROOT) + ";");
        c.line("                default -> throw new S1apException(\"Unknown CHOICE index\");\n            };");
        for (GenType.Field f : t.fields()) c.line("            this." + f.javaName() + " = choice == Choice." + f.javaName().toUpperCase(Locale.ROOT) + " ? new " + f.javaType() + "(content) : null;");
        c.line("            if (index < 0) AsnAper.finishOpen(content);"); endDecode(c);
        List<Parameter> params = t.fields().stream().map(f -> new Parameter(f.javaType(), f.javaName())).toList();
        c.line("    private " + t.javaName() + "(Choice choice, " + declarations(params) + ") {\n        this.choice = choice;");
        for (GenType.Field f : t.fields()) c.line("        this." + f.javaName() + " = " + f.javaName() + ";");
        c.line("    }\n");
        for (GenType.Field f : t.fields()) {
            valueDoc(c, t, refs);
            c.line("    public static " + t.javaName() + " " + f.javaName() + "(" + f.javaType() + " value) {");
            if (f.extension() && !known) c.line("        throw new IllegalArgumentException(\"CHOICE extensions are disabled\");");
            else c.line("        return new " + t.javaName() + "(Choice." + f.javaName().toUpperCase(Locale.ROOT) + ", "
                    + t.fields().stream().map(other -> other == f ? "Objects.requireNonNull(value, \"value\")" : "null").collect(Collectors.joining(", ")) + ");");
            c.line("    }");
            if (!f.extension() || known) ctors.add(new Constructor(List.of(new Parameter(f.javaType(), "value")),
                    t.javaName() + "." + f.javaName() + "(value)", cap(f.javaName())));
        }
        c.line("    public Choice getChoice() { return choice; }"); getters(c, t); encodeStart(c);
        c.line("        int populated = " + t.fields().stream().map(f -> "(" + f.javaName() + " == null ? 0 : 1)").collect(Collectors.joining(" + ")) + ";");
        c.line("        if (populated != 1) throw new IllegalStateException(\"CHOICE must contain exactly one value\");");
        c.line("        switch (choice) {"); ri = 0; ei = 0;
        for (GenType.Field f : t.fields()) {
            c.line("            case " + f.javaName().toUpperCase(Locale.ROOT) + " -> {");
            c.line("                AsnAper.index(out, " + (f.extension() ? ei++ : ri++) + ", " + f.extension() + ", " + roots + ", " + t.extensible() + ", " + known + ");");
            c.line("                " + (f.extension() ? "AsnAper.open(out, " + f.javaName() + ");" : f.javaName() + ".encode(out);") + "\n            }");
        }
        c.line("        }\n    }");
    }
    private void collection(Code c, GenType t, List<Constructor> ctors, List<String> refs) {
        String element = t.elementType(); boolean field = !t.alternatives().isEmpty();
        c.line("    private final List<" + element + "> values;\n");
        if (field) {
            GenType.OpenAlternative a = t.alternatives().get(0);
            c.line("    private static " + element + " readItem(BitInput in) {\n        return (" + element + ") AsnAper.field(in, new int[] {" + a.id() + "}, new int[] {" + criticality(a.criticality()) + "},");
            c.line("                List.of(" + element + "::new)).value();\n    }");
        }
        beginDecode(c, t, refs);
        c.line("            this.values = AsnAper.list(in, " + args(t) + ", " + (field ? t.javaName() + "::readItem" : element + "::new") + ");"); endDecode(c);
        valueDoc(c, t, refs);
        c.line("    public " + t.javaName() + "(List<" + element + "> values) {\n        this.values = List.copyOf(values);");
        c.line("        AsnAper.size(values.size(), " + args(t) + ");\n    }");
        valueDoc(c, t, refs);
        c.line("    public " + t.javaName() + "(" + element + "... values) { this(List.of(values)); }");
        c.line("    public List<" + element + "> getValues() { return values; }");
        ctors.add(new Constructor(List.of(new Parameter("List<" + element + ">", "values")), "new " + t.javaName() + "(values)", ""));
        ctors.add(new Constructor(List.of(new Parameter(element + "...", "values")), "new " + t.javaName() + "(values)", ""));
        encodeStart(c);
        String writer = "(target, value) -> value.encode(target)";
        if (field) { GenType.OpenAlternative a = t.alternatives().get(0);
            writer = "(target, value) -> AsnAper.field(target, new AsnAper.Field(" + a.id() + ", " + criticality(a.criticality()) + ", value))"; }
        c.line("        AsnAper.list(out, values, " + args(t) + ",\n                " + writer + ");\n    }");
    }
    private void container(Code c, GenType t, List<Constructor> ctors, List<String> refs) {
        boolean single = t.representation().equals("single"); String type = single ? "AsnAper.Field" : "List<AsnAper.Field>";
        c.line("    private final " + type + " values;");
        c.line("    private static AsnAper.Field readField(BitInput in) {\n        return AsnAper.field(in,");
        c.line("                new int[] {" + t.alternatives().stream().map(a -> Integer.toString(a.id())).collect(Collectors.joining(", ")) + "},");
        c.line("                new int[] {" + t.alternatives().stream().map(a -> Integer.toString(criticality(a.criticality()))).collect(Collectors.joining(", ")) + "},");
        c.line("                List.of(" + t.alternatives().stream().map(a -> a.javaType() + "::new").collect(Collectors.joining(", ")) + "));\n    }");
        c.line("    private static void validateField(AsnAper.Field field) {");
        c.line("        Objects.requireNonNull(field, \"field\");");
        for (GenType.OpenAlternative a : t.alternatives()) c.line("        if (field.id() == " + a.id() + " && field.criticality() == " + criticality(a.criticality())
                + " && field.value() instanceof " + a.javaType() + ") return;");
        c.line("        throw new IllegalArgumentException(\"Value does not belong to the container object set\");\n    }");
        beginDecode(c, t, refs);
        c.line("            this.values = " + (single ? "readField(in)" : "AsnAper.list(in, " + args(t) + ", " + t.javaName() + "::readField)") + ";"); endDecode(c);
        valueDoc(c, t, refs);
        c.line("    public " + t.javaName() + "(" + type + " values) {");
        if (single) c.line("        validateField(values);\n        this.values = values;");
        else c.line("        this.values = List.copyOf(values);\n        AsnAper.size(values.size(), " + args(t) + ");\n        this.values.forEach(" + t.javaName() + "::validateField);");
        c.line("    }");
        c.line("    public " + type + " getValues() { return values; }");
        for (GenType.OpenAlternative a : t.alternatives()) c.line("    public static AsnAper.Field field" + a.id() + "(" + a.javaType() + " value) {\n        return new AsnAper.Field(" + a.id() + ", " + criticality(a.criticality()) + ", value);\n    }");
        ctors.add(new Constructor(List.of(new Parameter(type, "values")), "new " + t.javaName() + "(values)", ""));
        encodeStart(c);
        c.line("        " + (single ? "AsnAper.field(out, values);" : "AsnAper.list(out, values, " + args(t) + ", AsnAper::field);") + "\n    }");
    }
    public static int criticality(String value) {
        return switch (value) { case "reject" -> 0; case "ignore" -> 1; case "notify" -> 2; default -> throw new IllegalArgumentException("Unknown criticality: " + value); };
    }
    public static String declarations(List<Parameter> params) {
        List<String> parts = params.stream().map(p -> p.type() + " " + p.name()).toList();
        String singleLine = String.join(", ", parts);
        return singleLine.length() <= 80 ? singleLine : "\n            " + String.join(",\n            ", parts);
    }
    public static String arguments(List<Parameter> params) { return params.stream().map(Parameter::name).collect(Collectors.joining(", ")); }
}
