package com.ancevt.asn1.generate.s1ap.render;

import com.ancevt.asn1.generate.s1ap.catalog.IeDescriptor;
import com.ancevt.asn1.generate.s1ap.config.Overrides;
import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.generate.s1ap.docs.Documentation;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.stream.Collectors;

public final class SnippetRenderer {
    private final String pkg;
    public SnippetRenderer(String pkg) { this.pkg = pkg; }
    private final StringBuilder decoder = new StringBuilder(), factory = new StringBuilder(), builder = new StringBuilder();
    private final Set<String> signatures = new HashSet<>();
    public void add(IeDescriptor d, String constant, List<JavaRenderer.Constructor> constructors, Overrides overrides,
                    Documentation docs, List<String> refs) {
        int decoderLength = decoder.length(), factoryLength = factory.length(), builderLength = builder.length();
        Set<String> previousSignatures = new HashSet<>(signatures);
        try { addValidated(d, constant, constructors, overrides, docs, refs); }
        catch (RuntimeException e) {
            decoder.setLength(decoderLength); factory.setLength(factoryLength); builder.setLength(builderLength);
            signatures.clear(); signatures.addAll(previousSignatures); throw e;
        }
    }
    private void addValidated(IeDescriptor d, String constant, List<JavaRenderer.Constructor> constructors, Overrides overrides,
                              Documentation docs, List<String> refs) {
        JsonNode config = overrides.merged(d.asnTypeName(), d.id()); Set<String> suppress = new HashSet<>();
        config.path("suppressSnippets").forEach(s -> suppress.add(s.asText()));
        if (d.valueIe() && !suppress.contains("decoder")) decoder.append("register(ProtocolIeId.").append(constant).append(", ").append(d.javaName()).append("::new);\n");
        for (JavaRenderer.Constructor c : constructors) {
            List<JavaRenderer.Parameter> params = c.parameters().stream().map(p -> new JavaRenderer.Parameter(
                    qualified(p.type(), d.javaName()), p.name())).toList();
            String method = d.factoryName() + c.suffix();
            String signature = method + "(" + params.stream().map(p -> p.type().replaceAll("<.*>", "").replace("...", "[]")).collect(Collectors.joining(",")) + ")";
            if (!signatures.add(signature)) throw new GenerationException("FACTORY_SIGNATURE_COLLISION", 6, signature);
            String decl = JavaRenderer.declarations(params), args = JavaRenderer.arguments(params);
            if (!suppress.contains("factory")) {
                if (docs != null && !refs.isEmpty()) factory.append(docs.comment("Creates " + d.asnTypeName(), refs, ""));
                factory.append("public static ").append(d.javaName()).append(' ').append(method).append('(').append(decl)
                        .append(") {\n    return ").append(c.invocation()).append(";\n}\n\n");
            }
            if (d.valueIe() && !suppress.contains("builder")) {
                if (suppress.contains("factory")) throw new GenerationException("INVALID_OVERRIDE", 8, "Suppress builder when suppressing its factory");
                if (docs != null && !refs.isEmpty()) builder.append(docs.comment("Adds " + d.asnTypeName(), refs, ""));
                builder.append("public MessageBuilder ").append(method).append("(Criticality criticality")
                        .append(decl.isEmpty() ? "" : ", " + decl).append(") {\n    return addField(ProtocolIeId.")
                        .append(constant).append(", criticality,\n            InformationElements.").append(method)
                        .append('(').append(args).append("));\n}\n\n");
            }
        }
        boolean alreadyTyped = constructors.stream().anyMatch(c -> c.suffix().isEmpty() && c.parameters().size() == 1
                && c.parameters().get(0).type().equals(d.javaName()));
        if (d.valueIe() && !suppress.contains("builder") && !alreadyTyped) {
            if (docs != null && !refs.isEmpty()) builder.append(docs.comment("Adds " + d.asnTypeName(), refs, ""));
            builder.append("public MessageBuilder ").append(d.factoryName()).append("(Criticality criticality, ")
                    .append(d.javaName()).append(" value) {\n    return addField(ProtocolIeId.").append(constant)
                    .append(", criticality, value);\n}\n\n");
        }
    }
    private String qualified(String type, String root) {
        if (type.equals("Criticality")) return pkg + ".Criticality";
        return type.equals("Value") ? root + ".Value" : type.replaceAll("(?<![\\w.])List(?=\\s*<)", "java.util.List")
                .replaceAll("(?<![\\w.])BigInteger\\b", "java.math.BigInteger")
                .replaceAll("(?<![\\w.])AsnBitString\\.", "tel.core.s1ap.core.asn.AsnBitString.")
                .replaceAll("(?<![\\w.])AsnAper\\.", "tel.core.s1ap.core.asn.AsnAper.");
    }
    public Map<String, String> files() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("snippets/decoder-registrations.txt", decoder.toString());
        files.put("snippets/InformationElements.methods.txt", factory.toString());
        files.put("snippets/MessageBuilder.methods.txt", builder.toString());
        return files;
    }
}
