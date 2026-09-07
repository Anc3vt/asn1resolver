package com.ancevt.asn1.generate.s1ap.cli;

import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.generate.s1ap.naming.JavaNames;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public record Options(String command, Path asn, Path output, Path target, String packageName, Path overrides,
                      Path docsIndex, String docs, String extensions, String dependencies, String conflicts,
                      String report, String kind, boolean mappedOnly, boolean allMatches, boolean partial,
                      boolean dryRun, String asnVersion, List<String> selectors) {
    public Options { selectors = List.copyOf(selectors); }
    public static Options parse(String[] args) throws IOException {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("help")) return null;
        String command = args[0];
        if (!Set.of("list", "describe", "validate", "generate").contains(command)) fail("Unknown command " + command);
        Set<String> flags = Set.of("mapped-only", "all-matches", "allow-partial", "dry-run");
        Set<String> options = Set.of("asn", "output", "target-source-root", "package", "overrides", "docs-index", "docs",
                "extension-policy", "dependency-policy", "conflict-policy", "report-format", "kind", "ie", "ie-list", "asn-version");
        Map<String, String> values = new HashMap<>(); Set<String> enabled = new HashSet<>();
        LinkedHashSet<String> selectors = new LinkedHashSet<>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                if (command.equals("describe")) { selectors.add(arg); continue; }
                fail("Unexpected positional argument: " + arg);
            }
            String name = arg.substring(2);
            if (flags.contains(name)) { enabled.add(name); continue; }
            if (!options.contains(name)) fail("Unknown option " + arg);
            if (++i == args.length || args[i].startsWith("--")) fail("Value required for " + arg);
            String value = args[i];
            if (name.equals("ie")) selectors.add(value);
            else if (name.equals("ie-list")) {
                for (String line : Files.readAllLines(Path.of(value))) {
                    String s = line.strip(); if (!s.isEmpty() && !s.startsWith("#")) selectors.add(s);
                }
            } else if (values.putIfAbsent(name, value) != null) fail("Repeated option " + arg);
        }
        if (!values.containsKey("asn")) fail("--asn is required");
        if (command.equals("generate") && !values.containsKey("output")) fail("--output is required");
        if (Set.of("generate", "describe").contains(command) && selectors.isEmpty()) fail("Specify --ie, --ie-list or describe selector");
        return new Options(command, Path.of(values.get("asn")), path(values, "output"), path(values, "target-source-root"),
                JavaNames.packageName(values.getOrDefault("package", "tel.core.s1ap.spec.ie")), path(values, "overrides"), path(values, "docs-index"),
                choice(values, "docs", "off", "off", "best-effort", "required"),
                choice(values, "extension-policy", "root-only", "root-only", "known-additions"),
                choice(values, "dependency-policy", "missing", "none", "missing", "closure"),
                choice(values, "conflict-policy", "fail", "fail", "skip", "overwrite"),
                choice(values, "report-format", "both", "text", "json", "both"),
                choice(values, "kind", "all", "value", "extension", "all"), enabled.contains("mapped-only"),
                enabled.contains("all-matches"), enabled.contains("allow-partial"), enabled.contains("dry-run"),
                values.get("asn-version"), List.copyOf(selectors));
    }
    private static String choice(Map<String, String> values, String key, String fallback, String... valid) {
        String value = values.getOrDefault(key, fallback);
        if (!List.of(valid).contains(value)) fail("Invalid --" + key + ": " + value);
        return value;
    }
    private static Path path(Map<String, String> values, String key) { return values.containsKey(key) ? Path.of(values.get(key)) : null; }
    private static void fail(String message) { throw new GenerationException("CLI_ARGUMENT_ERROR", 2, message); }
}
