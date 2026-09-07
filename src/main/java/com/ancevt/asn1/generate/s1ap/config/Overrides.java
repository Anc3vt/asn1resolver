package com.ancevt.asn1.generate.s1ap.config;

import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.generate.s1ap.naming.JavaNames;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Strict, versioned JSON overrides. No executable config or Java source fragments. */
public final class Overrides {
    private final JsonNode data;
    private final String provenance;
    public Overrides(JsonNode data, String provenance) {
        this.data = data;
        this.provenance = provenance;
        keys(data, Set.of("schemaVersion", "acronyms", "types", "ies", "constructors", "docs"));
        if (data.path("schemaVersion").asInt() != 1) invalid("schemaVersion must be 1");
        for (String group : Set.of("types", "ies")) {
            data.path(group).fields().forEachRemaining(e -> {
                if (group.equals("ies") && (!e.getKey().matches("\\d+") || Long.parseLong(e.getKey()) > 65535))
                    invalid("Invalid IE ID " + e.getKey());
                JsonNode v = e.getValue();
                keys(v, Set.of("javaClass", "factoryMethod", "protocolIeConstant", "existing", "representation",
                        "reuse", "asnType", "fields", "enumAliases", "choiceStrategy", "optionalCollection",
                        "suppressSnippets", "template", "kind", "criticality", "presence"));
                for (String key : Set.of("javaClass", "factoryMethod", "protocolIeConstant", "reuse"))
                    if (v.has(key)) JavaNames.identifier(v.get(key).asText());
                for (String key : Set.of("fields", "enumAliases"))
                    v.path(key).fields().forEachRemaining(a -> JavaNames.identifier(a.getValue().asText()));
                if (v.has("existing") && !v.get("existing").isBoolean()) invalid("existing must be boolean");
                if (v.has("kind") && !Set.of("Value", "Extension").contains(v.get("kind").asText())) invalid("kind must be Value or Extension");
                if (v.has("choiceStrategy") && !Set.of("structural", "custom").contains(v.get("choiceStrategy").asText()))
                    invalid("choiceStrategy must be structural or custom");
                if ((v.path("choiceStrategy").asText().equals("custom") || v.has("template")
                        || v.path("representation").asText().equals("custom")) && !v.path("existing").asBoolean() && !v.has("reuse"))
                    invalid("Custom representation/template requires existing:true or explicit reuse");
                if (v.has("optionalCollection") && !Set.of("nullable", "empty").contains(v.get("optionalCollection").asText()))
                    invalid("optionalCollection must be nullable or empty");
                if (v.has("representation") && !Set.of("int", "long", "BigInteger", "String", "byte[]",
                        "AsnBitString.Value", "custom").contains(v.get("representation").asText())) invalid("Unknown representation");
                if (v.has("template") && !Set.of("plmn-identity", "gtp-teid", "transport-address", "nas-pdu").contains(v.get("template").asText()))
                    invalid("Unknown semantic template");
                if (v.has("suppressSnippets")) {
                    if (!v.get("suppressSnippets").isArray()) invalid("suppressSnippets must be array");
                    v.get("suppressSnippets").forEach(s -> {
                        if (!Set.of("decoder", "factory", "builder").contains(s.asText())) invalid("Unknown snippet " + s);
                    });
                }
            });
        }
        data.path("constructors").fields().forEachRemaining(e -> {
            JavaNames.identifier(e.getKey()); keys(e.getValue(), Set.of("expose"));
            if (!e.getValue().path("expose").isArray()) invalid("constructors.expose must be array");
            e.getValue().get("expose").forEach(c -> {
                keys(c, Set.of("parameters"));
                if (!c.path("parameters").isArray()) invalid("parameters must be array");
                c.get("parameters").forEach(p -> { keys(p, Set.of("type", "name")); JavaNames.identifier(p.path("name").asText()); });
            });
        });
        data.path("docs").fields().forEachRemaining(e -> keys(e.getValue(), Set.of("primary", "refs")));
        data.path("acronyms").fields().forEachRemaining(e -> JavaNames.identifier(e.getValue().asText()));
    }
    public static Overrides empty() { return new Overrides(Json.MAPPER.createObjectNode().put("schemaVersion", 1), "defaults"); }
    public static Overrides read(Path path) throws IOException { return new Overrides(Json.read(path), path.toString()); }
    public static void keys(JsonNode node, Set<String> allowed) {
        if (!node.isObject()) invalid("Expected JSON object: " + node);
        node.fieldNames().forEachRemaining(k -> { if (!allowed.contains(k)) invalid("Unknown configuration key: " + k); });
    }
    public static void invalid(String message) { throw new GenerationException("INVALID_OVERRIDE", 8, message); }
    public JsonNode type(String name) { return data.path("types").path(name); }
    public JsonNode ie(int id) { return data.path("ies").path(Integer.toString(id)); }
    public JsonNode docs(int id) { return data.path("docs").path(Integer.toString(id)); }
    public JsonNode constructors(String name) { return data.path("constructors").path(name); }
    public JsonNode data() { return data; }
    public String provenance() { return provenance; }
    public JsonNode merged(String type, Integer id) {
        ObjectNode result = Json.MAPPER.createObjectNode();
        if (type(type).isObject()) result.setAll((ObjectNode) type(type));
        if (id != null && ie(id).isObject()) result.setAll((ObjectNode) ie(id));
        return result;
    }
    public Map<String, String> acronyms() {
        Map<String, String> result = new LinkedHashMap<>();
        data.path("acronyms").fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText()));
        return result;
    }
}
