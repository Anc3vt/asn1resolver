package com.ancevt.asn1.generate.s1ap.docs;

import com.ancevt.asn1.generate.s1ap.config.Json;
import com.ancevt.asn1.generate.s1ap.config.Overrides;
import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.net.URI;
import java.util.*;

public final class Documentation {
    private final JsonNode data;
    public Documentation(Path path, String asnHash, String asnVersion) throws IOException {
        data = Json.read(path);
        Overrides.keys(data, Set.of("schemaVersion", "spec", "sections", "types", "ies", "x691"));
        if (data.path("schemaVersion").asInt() != 1) fail("DOC_VERSION_MISMATCH", "Unknown docs schema version");
        JsonNode spec = data.path("spec");
        Overrides.keys(spec, Set.of("organization", "number", "version", "release", "url", "asnSha256"));
        if (!spec.path("asnSha256").asText().equalsIgnoreCase(asnHash)
                || asnVersion != null && !spec.path("version").asText().equals(asnVersion))
            fail("DOC_VERSION_MISMATCH", "Docs version/hash does not match ASN input");
        if (!spec.path("version").asText().matches("[0-9]+\\.[0-9]+\\.[0-9]+")) fail("DOC_VERSION_MISMATCH", "Exact spec version required");
        URI uri = URI.create(spec.path("url").asText());
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null) fail("DOC_VERSION_MISMATCH", "HTTPS specification URL required");
        data.path("sections").fields().forEachRemaining(e -> {
            Overrides.keys(e.getValue(), Set.of("number", "title"));
            if (!e.getValue().path("number").asText().matches("[0-9]+(?:\\.[0-9]+)+")) fail("DOC_MAPPING_MISSING", "Invalid section number");
        });
        for (String group : List.of("ies", "types")) data.path(group).fields().forEachRemaining(e -> {
            Overrides.keys(e.getValue(), Set.of("refs"));
            if (!e.getValue().path("refs").isArray()) fail("DOC_MAPPING_MISSING", "refs must be array");
            e.getValue().path("refs").forEach(r -> requireSection(r.asText()));
        });
    }
    private void requireSection(String id) { if (!data.path("sections").has(id)) fail("DOC_MAPPING_MISSING", "Unknown docs section " + id); }
    public String version() { return data.path("spec").path("version").asText(); }
    public JsonNode metadata() { return data.path("spec"); }
    public List<String> references(Integer id, String type, Overrides overrides) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        data.path("types").path(type).path("refs").forEach(r -> refs.add(r.asText()));
        if (id != null) {
            data.path("ies").path(id.toString()).path("refs").forEach(r -> refs.add(r.asText()));
            JsonNode custom = overrides.docs(id);
            if (custom.has("refs")) { refs.clear(); custom.get("refs").forEach(r -> refs.add(r.asText())); }
            if (custom.has("primary")) { refs.clear(); refs.add(custom.get("primary").asText()); }
        }
        refs.forEach(this::requireSection);
        return List.copyOf(refs);
    }
    public String comment(String action, List<String> refs, String indent) {
        StringBuilder result = new StringBuilder(indent + "/**\n" + indent + " * " + escape(action) + ".\n");
        for (String ref : refs) {
            JsonNode section = data.path("sections").path(ref);
            result.append(indent).append(" * <p>See <a href=\"").append(escape(data.path("spec").path("url").asText()))
                    .append("\">\n").append(indent).append(" * ").append(escape(data.path("spec").path("number").asText()))
                    .append(" V").append(escape(version())).append(": ").append(escape(section.path("number").asText()))
                    .append(' ').append(escape(section.path("title").asText())).append("</a>.\n");
        }
        return result.append(indent).append(" */\n").toString();
    }
    public static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("*/", "*&#47;"); }
    private static void fail(String code, String message) { throw new GenerationException(code, 8, message); }
}
