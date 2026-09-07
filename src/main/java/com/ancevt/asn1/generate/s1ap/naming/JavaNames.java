package com.ancevt.asn1.generate.s1ap.naming;

import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The only ASN.1 to Java spelling policy used by the generator. */
public final class JavaNames {
    private final Map<String, String> acronyms = new LinkedHashMap<>();
    public JavaNames(Map<String, String> overrides) {
        for (String token : List.of("S1AP", "MME", "ENB", "UE", "GTP", "TEID", "PLMN", "TAI", "TAC",
                "CGI", "CSG", "NAS", "RRC", "LTE", "NR", "QOS", "DRX", "ID", "IE")) {
            acronyms.put(token, token.substring(0, 1) + token.substring(1).toLowerCase(Locale.ROOT));
        }
        acronyms.put("E-RAB", "ERab");
        acronyms.putAll(overrides);
    }

    public String className(String source) {
        StringBuilder out = new StringBuilder();
        List<String> keys = new ArrayList<>(acronyms.keySet());
        keys.sort(Comparator.comparingInt(String::length).reversed().thenComparing(s -> s));
        for (int i = 0; i < source.length();) {
            if (!Character.isLetterOrDigit(source.charAt(i))) { i++; continue; }
            boolean matched = false;
            for (String key : keys) {
                // Acronyms are recognized at word boundaries, or in their ASN.1 uppercase spelling.
                if (source.regionMatches(true, i, key, 0, key.length())
                        && (source.substring(i, i + key.length()).chars().anyMatch(Character::isUpperCase)
                        || i + key.length() == source.length() || !Character.isLowerCase(source.charAt(i + key.length())))
                        && (i == 0 || !Character.isLowerCase(source.charAt(i - 1))
                        || Character.isUpperCase(source.charAt(i)))) {
                    out.append(acronyms.get(key)); i += key.length(); matched = true; break;
                }
            }
            if (matched) continue;
            int end = i + 1;
            while (end < source.length() && Character.isLowerCase(source.charAt(end))) end++;
            out.append(Character.toUpperCase(source.charAt(i))).append(source, i + 1, end);
            i = end;
        }
        return identifier(out.toString());
    }

    public static String method(String name) {
        return identifier(Character.toLowerCase(name.charAt(0)) + name.substring(1));
    }
    public String field(String name) { return method(className(name)); }
    public String constant(String name) {
        String result = name.replace('-', '_')
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2").toUpperCase(Locale.ROOT);
        return identifier(result);
    }
    public static String identifier(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name)
                || List.of("CON", "PRN", "AUX", "NUL", "CLOCK$", "RECORD", "SEALED", "PERMITS", "VAR", "YIELD").contains(upper)
                || upper.matches("(?:COM|LPT)[1-9]")) {
            throw new GenerationException("JAVA_NAME_COLLISION", 8, "Invalid Java/Windows identifier: " + name);
        }
        return name;
    }
    public static String packageName(String name) {
        for (String part : name.split("\\.", -1)) identifier(part);
        return name;
    }
    public static String normalized(String name) { return name.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT); }
}
