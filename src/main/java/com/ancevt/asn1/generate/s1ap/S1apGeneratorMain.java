package com.ancevt.asn1.generate.s1ap;

import com.ancevt.asn1.Asn1;
import com.ancevt.asn1.generate.s1ap.catalog.*;
import com.ancevt.asn1.generate.s1ap.cli.Options;
import com.ancevt.asn1.generate.s1ap.config.*;
import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.generate.s1ap.docs.Documentation;
import com.ancevt.asn1.generate.s1ap.naming.JavaNames;
import com.ancevt.asn1.generate.s1ap.normalize.*;
import com.ancevt.asn1.generate.s1ap.render.*;
import com.ancevt.asn1.model.Asn1Document;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Selective S1AP IE generator. All writes are staged logically below --output. */
public final class S1apGeneratorMain {
    public static final String VERSION = "1.0.0";
    private S1apGeneratorMain() { }
    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Options options = null;
        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> diagnostics = new ArrayList<>();
        report.put("reportSchemaVersion", 1); report.put("generatorVersion", VERSION); report.put("diagnostics", diagnostics);
        int exit = 0;
        try {
            try { options = Options.parse(args); }
            catch (GenerationException | IOException | IllegalArgumentException e) {
                throw new GenerationException("CLI_ARGUMENT_ERROR", 2, e.getMessage());
            }
            if (options == null) { out.println(help()); return 0; }
            report.put("command", options.command());
            Overrides overrides;
            try { overrides = options.overrides() == null ? Overrides.empty() : Overrides.read(options.overrides()); }
            catch (IOException | IllegalArgumentException e) { throw new GenerationException("INVALID_OVERRIDE", 8, e.getMessage()); }
            String hash; Asn1Document document;
            try { hash = Json.hash(Files.readAllBytes(options.asn())); document = Asn1.read(options.asn()); }
            catch (IOException | com.ancevt.asn1.parse.Asn1ParseException e) { throw new GenerationException("ASN_PARSE_ERROR", 3, e.getMessage()); }
            String version = options.asnVersion();
            JsonNode baseline = resource("baseline.json");
            if (version == null && hash.equals(baseline.path("asnSha256").asText())) version = baseline.path("version").asText();
            report.put("asn", map("path", options.asn().toAbsolutePath().normalize().toString(), "sha256", hash,
                    "sourceName", document.getSourceName(), "version", version, "modules", document.getModules().size(),
                    "assignments", document.getAssignmentCount(), "errors", document.getErrorCount()));
            if (document.getErrorCount() != 0) throw new GenerationException("ASN_SEMANTIC_ERROR", 4, document.getDiagnostics().toString());
            Documentation docs = null;
            if (options.docsIndex() != null) {
                if (version == null) throw new GenerationException("DOC_VERSION_MISMATCH", 8, "Unknown ASN version; pass --asn-version with independently verified version");
                try { docs = new Documentation(options.docsIndex(), hash, version); }
                catch (IOException | IllegalArgumentException e) { throw new GenerationException("DOC_VERSION_MISMATCH", 8, e.getMessage()); }
            }
            if (options.docs().equals("required") && docs == null) throw new GenerationException("DOC_MAPPING_MISSING", 8, "--docs-index required");
            Map<String, String> configHashes = new TreeMap<>();
            if (options.overrides() != null) configHashes.put("overrides", Json.hash(Files.readAllBytes(options.overrides())));
            if (options.docsIndex() != null) configHashes.put("docsIndex", Json.hash(Files.readAllBytes(options.docsIndex())));
            report.put("configHashes", configHashes);
            JavaNames names = new JavaNames(overrides.acronyms());
            IeCatalog catalog = new IeCatalog(document, overrides, names);
            report.put("catalog", map("declared", catalog.descriptors().size(), "mapped", catalog.descriptors().stream().filter(IeDescriptor::mapped).count()));
            if (options.command().equals("list")) {
                String kindFilter = options.kind();
                for (IeDescriptor d : catalog.descriptors()) {
                    if (options.mappedOnly() && !d.mapped()) continue;
                    if (!kindFilter.equals("all") && d.usages().stream().noneMatch(u -> u.kind().equalsIgnoreCase(kindFilter))) continue;
                    out.printf("%5d  %-48s %-48s %s%n", d.id(), d.idName(), d.asnTypeName(), d.javaName());
                }
            } else if (options.command().equals("validate")) out.println("Valid ASN.1: " + document.getModules().size() + " modules, " + catalog.descriptors().size() + " IDs; version=" + version + "; SHA-256=" + hash);
            else {
                TargetSources target = options.target() == null ? null : new TargetSources(options.target());
                if (target != null && !target.runtime() && options.command().equals("generate"))
                    throw new GenerationException("MISSING_RUNTIME_CAPABILITY", 6, "Target requires AsnAper-v1 prerequisite; see runtime/README.md");
                Map<String, Integer> constants = new TreeMap<>();
                if (target != null) constants.putAll(target.constants());
                else resource("protocol-ie-constants.json").fields().forEachRemaining(e -> constants.put(e.getKey(), e.getValue().asInt()));
                report.put("runtimeCapabilities", List.of("AsnAper-v1"));
                report.put("runtimeValidation", target == null ? "Bundled prerequisite required; no target supplied" : "Target source capability and public API inspected with javac");
                report.put("extensionPolicy", options.extensions()); report.put("dependencyPolicy", options.dependencies());
                List<Map<String, Object>> selections = new ArrayList<>(); report.put("selectors", selections);
                LinkedHashMap<String, String> files = new LinkedHashMap<>();
                Map<String, GenType> outputs = new LinkedHashMap<>(); Set<Integer> seenIds = new HashSet<>();
                SnippetRenderer snippets = new SnippetRenderer(options.packageName());
                JavaRenderer renderer = new JavaRenderer(options.packageName(), options.extensions().equals("known-additions"), options.docs().equals("off") ? null : docs);
                for (String selector : options.selectors()) {
                    try {
                        IeCatalog.Resolution resolution = catalog.resolve(selector, options.allMatches());
                        for (IeDescriptor d : resolution.matches()) {
                            if (!seenIds.add(d.id())) continue;
                            Map<String, Object> selected = map("selector", selector, "resolutionMethod", resolution.method(), "id", d.id(),
                                    "idAssignment", d.idName(), "asnType", d.asnTypeName(), "javaClass", d.javaName(), "factoryMethod", d.factoryName(), "usages", d.usages());
                            selections.add(selected);
                            if (options.command().equals("describe")) { out.println(Json.write(selected)); continue; }
                            if (d.usages().isEmpty()) Overrides.invalid("Manually mapped ID " + d.id() + " requires kind: Value or Extension for snippets");
                            TypeNormalizer normalizer = new TypeNormalizer(overrides, names);
                            GenType root = normalizer.root(d.asnTypeName(), d.javaName(), d.type(), d.id());
                            selected.put("cycles", normalizer.cycles());
                            String constant = constant(d, overrides, constants); selected.put("protocolIeConstant", constant);
                            List<String> refs = docs == null ? List.of() : docs.references(d.id(), d.asnTypeName(), overrides);
                            if (!options.docs().equals("off") && refs.isEmpty()) {
                                if (options.docs().equals("required")) throw new GenerationException("DOC_MAPPING_MISSING", 8, "No docs mapping for ID " + d.id());
                                diagnostics.add(diagnostic("DOC_MAPPING_MISSING", "WARNING", "No docs mapping", selector, null));
                            }
                            selected.put("documentationSections", refs); selected.put("appliedOverrides", overrides.merged(d.asnTypeName(), d.id()));
                            LinkedHashMap<String, GenType> closure = new LinkedHashMap<>();
                            collect(root.javaName(), normalizer.types(), closure);
                            List<Map<String, Object>> dependencies = new ArrayList<>(); selected.put("dependencies", dependencies);
                            List<GenType> generated = new ArrayList<>();
                            Deque<String> pending = new ArrayDeque<>(List.of(root.javaName()));
                            Set<String> planned = new HashSet<>();
                            while (!pending.isEmpty()) {
                                String name = pending.removeFirst();
                                if (!planned.add(name)) continue;
                                GenType t = closure.get(name);
                                boolean isRoot = t.javaName().equals(root.javaName());
                                JsonNode config = overrides.merged(t.asnName(), isRoot ? d.id() : null);
                                boolean custom = config.path("existing").asBoolean() || config.has("reuse") || config.path("representation").asText().equals("custom");
                                TargetSources.Api api = target == null ? null : target.find(options.packageName(), t.javaName());
                                boolean reuse = custom || !isRoot && !options.dependencies().equals("closure") && api != null;
                                if (reuse || !isRoot && options.dependencies().equals("none")) {
                                    if (api == null || !api.decoder() || !api.encoder() || !api.informationElement())
                                        throw new GenerationException("DEPENDENCY_NOT_FOUND", 6, "Need public InformationElement " + t.javaName() + "(BitInput) and encode(BitOutput)");
                                    dependencies.add(map("class", t.javaName(), "status", custom ? "custom" : "existing", "evidence", api));
                                } else {
                                    GenType previous = outputs.get(t.javaName());
                                    if (previous != null && !renderer.render(previous, null, List.of()).source().equals(renderer.render(t, null, List.of()).source()))
                                        throw new GenerationException("JAVA_NAME_COLLISION", 6, "Incompatible generated class " + t.javaName());
                                    generated.add(t); dependencies.add(map("class", t.javaName(), "status", "generated", "sourceRange", t.sourceRange()));
                                    pending.addAll(t.dependencies());
                                }
                            }
                            JavaRenderer.Rendered renderedRoot = renderer.render(root, d.id(), refs);
                            List<JavaRenderer.Constructor> exposed = exposed(root, renderedRoot.constructors(), overrides, target, options.packageName(), !generated.contains(root));
                            // Snippet validation precedes committing the successful selector to the file plan.
                            snippets.add(d, constant, exposed, overrides, options.docs().equals("off") ? null : docs, refs);
                            for (GenType t : generated) outputs.putIfAbsent(t.javaName(), t);
                            selected.put("ir", List.copyOf(closure.values()));
                            selected.put("extensionOnlyBuilderSuppressed", !d.valueIe());
                            files.put("sources/" + options.packageName().replace('.', '/') + "/" + root.javaName() + ".java",
                                    generated.contains(root) ? renderedRoot.source() : "");
                        }
                    } catch (GenerationException e) {
                        if (exit == 0) exit = e.exitCode(); diagnostics.add(diagnostic(e.code(), "ERROR", e.getMessage(), selector, e));
                    }
                }
                if (options.command().equals("generate")) {
                    files.values().removeIf(String::isEmpty);
                    for (GenType t : outputs.values().stream().sorted(Comparator.comparing(GenType::javaName)).toList()) {
                        String path = "sources/" + options.packageName().replace('.', '/') + "/" + t.javaName() + ".java";
                        if (!files.containsKey(path)) {
                            List<String> refs = docs == null ? List.of() : docs.references(null, t.asnName(), overrides);
                            files.put(path, renderer.render(t, null, refs).source());
                        }
                    }
                    files.putAll(snippets.files());
                    List<Map<String, Object>> fileReport = new ArrayList<>(); report.put("files", fileReport);
                    for (var entry : files.entrySet()) {
                        Path file = safePath(options.output(), entry.getKey());
                        Map<String, Object> row = map("path", entry.getKey(), "sha256", Json.hash(bytes(entry.getValue())), "status", "planned");
                        if (Files.exists(file)) {
                            row.put("previousSha256", Json.hash(Files.readAllBytes(file)));
                            if (options.conflicts().equals("fail")) {
                                diagnostics.add(diagnostic("OUTPUT_FILE_EXISTS", "ERROR", entry.getKey(), null, null));
                                row.put("status", "conflicting"); if (exit == 0) exit = 7;
                            } else if (options.conflicts().equals("skip")) { row.put("status", "skipped"); row.put("sha256", row.get("previousSha256")); }
                        }
                        fileReport.add(row);
                    }
                    if ((exit == 0 || options.partial()) && !options.dryRun()) {
                        for (Map<String, Object> row : fileReport) {
                            if (Set.of("skipped", "conflicting").contains(row.get("status"))) continue;
                            String path = (String) row.get("path"); write(safePath(options.output(), path), files.get(path)); row.put("status", "created");
                        }
                    }
                    out.println((options.dryRun() ? "Dry run" : "Generation") + ": " + selections.size() + " IE(s), " + outputs.size() + " source(s), " + diagnostics.size() + " diagnostic(s). Output: " + options.output());
                }
            }
        } catch (GenerationException e) { exit = e.exitCode(); diagnostics.add(diagnostic(e.code(), "ERROR", e.getMessage(), null, e)); }
        catch (IOException e) { exit = 9; diagnostics.add(diagnostic("GENERATOR_IO_ERROR", "ERROR", e.toString(), null, null)); }
        catch (RuntimeException e) { exit = 9; diagnostics.add(diagnostic("GENERATOR_INTERNAL_ERROR", "ERROR", e.toString(), null, null)); }
        report.put("exitCode", exit);
        for (var d : diagnostics) err.println(d.get("severity") + " " + d.get("code") + ": " + d.get("message"));
        if (options != null && options.output() != null) {
            try {
                String json = Json.write(report);
                if (!options.report().equals("text")) write(safePath(options.output(), "reports/generation-report.json"), json);
                if (!options.report().equals("json")) write(safePath(options.output(), "reports/generation-report.txt"), "S1AP IE generation report v1\n" + json);
            } catch (IOException | GenerationException e) { err.println("GENERATOR_IO_ERROR: " + e.getMessage()); return 9; }
        }
        return exit;
    }
    private static String optionsKind(Options o) { return o.kind(); }
    private static List<JavaRenderer.Constructor> exposed(GenType root, List<JavaRenderer.Constructor> defaults, Overrides overrides,
                                                         TargetSources target, String pkg, boolean reused) {
        JsonNode config = overrides.constructors(root.javaName());
        TargetSources.Api api = target == null ? null : target.find(pkg, root.javaName());
        if (config.isMissingNode()) {
            if (reused) {
                if (api == null || defaults.stream().anyMatch(c -> !hasConstructor(api, c.parameters().stream().map(JavaRenderer.Parameter::type).toList())))
                    Overrides.invalid("Reused root " + root.javaName() + " needs explicit constructors.expose matching its public API");
            }
            return defaults;
        }
        List<JavaRenderer.Constructor> result = new ArrayList<>();
        for (JsonNode ctor : config.path("expose")) {
            List<JavaRenderer.Parameter> params = new ArrayList<>();
            ctor.path("parameters").forEach(p -> params.add(new JavaRenderer.Parameter(p.path("type").asText(), p.path("name").asText())));
            if (params.stream().anyMatch(p -> p.type().endsWith("BitInput"))) Overrides.invalid("Decode constructor cannot be exposed");
            List<String> types = params.stream().map(JavaRenderer.Parameter::type).toList();
            boolean exists = defaults.stream().anyMatch(c -> c.parameters().stream().map(JavaRenderer.Parameter::type).toList().equals(types));
            if (reused ? api == null || !hasConstructor(api, types) : !exists)
                Overrides.invalid("Constructor not found: " + root.javaName() + types);
            result.add(new JavaRenderer.Constructor(params, "new " + root.javaName() + "(" + JavaRenderer.arguments(params) + ")", ""));
        }
        return result;
    }
    private static boolean hasConstructor(TargetSources.Api api, List<String> types) {
        java.util.function.Function<String, String> normalize = type -> type.replace(api.packageName() + ".", "")
                .replace(api.className() + ".", "").replace("java.util.", "").replace("java.lang.", "").replace(" ", "");
        List<String> requested = types.stream().map(normalize).toList();
        return api.constructors().stream().anyMatch(c -> c.stream().map(normalize).toList().equals(requested));
    }
    private static String constant(IeDescriptor d, Overrides overrides, Map<String, Integer> constants) {
        String configured = overrides.ie(d.id()).path("protocolIeConstant").asText(null);
        if (configured != null) {
            if (!constants.containsKey(configured)) throw new GenerationException("PROTOCOL_IE_CONSTANT_MISSING", 8, configured);
            if (constants.get(configured) != d.id()) throw new GenerationException("PROTOCOL_IE_CONSTANT_VALUE_MISMATCH", 8, configured);
            return configured;
        }
        List<String> matches = constants.entrySet().stream().filter(e -> e.getValue() == d.id()).map(Map.Entry::getKey).toList();
        if (matches.size() != 1) throw new GenerationException("PROTOCOL_IE_CONSTANT_MISSING", 6, "Need unique ProtocolIeId constant for ID " + d.id());
        return matches.get(0);
    }
    private static void collect(String name, Map<String, GenType> all, Map<String, GenType> closure) {
        if (closure.containsKey(name)) return;
        GenType type = all.get(name);
        if (type == null) throw new GenerationException("DEPENDENCY_NOT_FOUND", 6, name);
        closure.put(name, type); for (String dep : type.dependencies()) collect(dep, all, closure);
    }
    private static JsonNode resource(String name) throws IOException {
        try (InputStream in = S1apGeneratorMain.class.getResourceAsStream("/s1ap-generator/" + name)) {
            if (in == null) throw new IOException("Missing generator resource " + name);
            return Json.MAPPER.readTree(in);
        }
    }
    private static Map<String, Object> diagnostic(String code, String severity, String message, String selector, GenerationException e) {
        return map("code", code, "severity", severity, "message", message, "selector", selector,
                "sourceRange", e == null ? null : e.range(), "relatedSourceRanges", e == null ? List.of() : e.related(),
                "suggestedAction", "Resolve the reported input/configuration issue and rerun; see GENERATOR_README.md diagnostics.");
    }
    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i].toString(), pairs[i + 1]);
        return result;
    }
    private static byte[] bytes(String value) { return (value.stripTrailing() + "\n").getBytes(StandardCharsets.UTF_8); }
    private static Path safePath(Path output, String relative) throws IOException {
        Path root = output.toAbsolutePath().normalize(); Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || path.equals(root)) throw new GenerationException("INVALID_OVERRIDE", 8, "Output path escapes root");
        // Reject junctions/symlinks at every existing path component, including the output root.
        for (Path p = path; p != null; p = p.getParent()) {
            if (Files.isSymbolicLink(p)) throw new GenerationException("INVALID_OVERRIDE", 8, "Output symlink is not allowed: " + p);
            if (Files.exists(p) && !p.toRealPath().equals(p.toAbsolutePath().normalize()))
                throw new GenerationException("INVALID_OVERRIDE", 8, "Output redirected path is not allowed: " + p);
        }
        return path;
    }
    private static void write(Path file, String value) throws IOException {
        Files.createDirectories(file.getParent()); Path temp = Files.createTempFile(file.getParent(), ".s1ap-", ".tmp");
        try {
            Files.write(temp, bytes(value));
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private static String help() {
        return "S1AP IE generator " + VERSION + "\n"
                + "Commands: list, describe <selector>, validate, generate\n"
                + "Required: --asn FILE; generate also requires --output DIR and --ie SELECTOR (repeatable) or --ie-list FILE\n"
                + "Options: --target-source-root DIR --package NAME --overrides JSON --docs-index JSON --asn-version X.Y.Z\n"
                + "  --docs off|best-effort|required --extension-policy root-only|known-additions\n"
                + "  --dependency-policy none|missing|closure --conflict-policy fail|skip|overwrite\n"
                + "  --report-format text|json|both --kind value|extension|all --mapped-only --all-matches --allow-partial --dry-run\n";
    }
}
