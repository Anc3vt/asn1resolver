package com.ancevt.asn1.generate.s1ap.catalog;

import com.ancevt.asn1.generate.s1ap.config.Overrides;
import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.generate.s1ap.naming.JavaNames;
import com.ancevt.asn1.generate.s1ap.normalize.Bounds;
import com.ancevt.asn1.generate.s1ap.normalize.IntegerValues;
import com.ancevt.asn1.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public final class IeCatalog {
    private final List<IeDescriptor> descriptors;
    private final Map<String, List<IeDescriptor>> ids = new LinkedHashMap<>();
    private final Map<String, List<IeDescriptor>> types = new LinkedHashMap<>();
    private final Map<String, List<IeDescriptor>> aliases = new LinkedHashMap<>();
    private final Map<String, List<IeDescriptor>> normalized = new LinkedHashMap<>();
    public IeCatalog(Asn1Document document, Overrides overrides, JavaNames names) {
        Map<Integer, String> declared = new TreeMap<>();
        Map<String, TypeAssignment> typeAssignments = new LinkedHashMap<>();
        List<Assignment> assignments = document.getModules().stream().flatMap(m -> m.getAssignments().stream()).toList();
        for (Assignment a : assignments) {
            if (a instanceof TypeAssignment t) typeAssignments.put(t.getName(), t);
            if (a instanceof ValueAssignment v && v.getName().startsWith("id-")
                    && v.getGovernor() instanceof ReferenceType r && r.getName().equals("ProtocolIE-ID")) {
                java.math.BigInteger numeric = IntegerValues.evaluate(v);
                if (numeric.signum() < 0 || numeric.compareTo(java.math.BigInteger.valueOf(65535)) > 0)
                    throw new GenerationException("UNSUPPORTED_CONSTRAINT", 6, "ID out of range: " + numeric, v.getSourceRange(), List.of());
                int id = numeric.intValueExact();
                declared.put(id, v.getName());
            }
        }
        Map<Integer, List<IeDescriptor.Usage>> usages = new TreeMap<>();
        Map<Integer, AsnType> mapped = new TreeMap<>();
        Map<Integer, String> typeNames = new TreeMap<>();
        Map<Integer, String> fingerprints = new TreeMap<>();
        Set<Integer> conflicts = new TreeSet<>();
        for (Assignment a : assignments) {
            ObjectClassAssignment cls;
            List<ObjectDefinition> objects;
            if (a instanceof ObjectSetAssignment set) {
                cls = set.getObjectClass(); objects = set.getExpression().getAllObjects();
            } else if (a instanceof ObjectAssignment obj) {
                cls = obj.getObjectClass(); objects = List.of(obj.getDefinition());
            } else continue;
            if (cls == null || !Set.of("S1AP-PROTOCOL-IES", "S1AP-PROTOCOL-EXTENSION").contains(cls.getName())) continue;
            String kind = cls.getName().equals("S1AP-PROTOCOL-IES") ? "Value" : "Extension";
            List<String> messages = document.getReferenceIndex().getReferencesTo(a).stream()
                    .flatMap(ref -> assignments.stream().filter(t -> t instanceof TypeAssignment
                            && t.getSourceRange().startOffset() <= ref.getSourceRange().startOffset()
                            && t.getSourceRange().endOffset() >= ref.getSourceRange().endOffset()))
                    .map(Assignment::getName).distinct().sorted().toList();
            for (ObjectDefinition obj : objects) {
                ObjectFieldSetting idSetting = obj.requireSetting("id");
                int id = IntegerValues.id(idSetting);
                ObjectFieldSetting value = obj.requireSetting(kind);
                AsnType type = value.getValueTarget() instanceof TypeAssignment t ? t.getType() : value.getTypeValue();
                String typeName = value.getValueTarget() instanceof TypeAssignment t ? t.getName() : "<inline>";
                if (type == null) throw new GenerationException("UNRESOLVED_TYPE_REFERENCE", 6,
                        "Missing type for ID " + id, value.getSourceRange(), List.of());
                String shape = fingerprint(type, new IdentityHashMap<>());
                if (fingerprints.containsKey(id) && !fingerprints.get(id).equals(shape)) {
                    conflicts.add(id);
                }
                fingerprints.put(id, shape); mapped.put(id, type); typeNames.put(id, typeName);
                declared.putIfAbsent(id, idSetting.getSourceText().trim());
                usages.computeIfAbsent(id, unused -> new ArrayList<>()).add(new IeDescriptor.Usage(kind,
                        obj.requireSetting("criticality").getSourceText().trim(),
                        obj.requireSetting("presence").getSourceText().trim(), a.getName(), messages, obj.getSourceRange()));
            }
        }
        if (!conflicts.isEmpty()) {
            List<SourceRange> ranges = conflicts.stream().flatMap(id -> usages.get(id).stream()).map(IeDescriptor.Usage::sourceRange).toList();
            throw new GenerationException("IE_ID_TYPE_CONFLICT", 5, "IDs " + conflicts + " have incompatible types",
                    ranges.get(0), ranges.subList(1, ranges.size()));
        }
        for (Map.Entry<Integer, String> entry : declared.entrySet()) {
            int id = entry.getKey(); JsonNode config = overrides.ie(id);
            if (config.has("asnType")) {
                TypeAssignment t = typeAssignments.get(config.get("asnType").asText());
                if (t == null) Overrides.invalid("Unknown override ASN type " + config.get("asnType"));
                if (mapped.containsKey(id) && !fingerprints.get(id).equals(fingerprint(t.getType(), new IdentityHashMap<>())))
                    Overrides.invalid("Cannot replace mapped ASN type for " + id);
                mapped.put(id, t.getType()); typeNames.put(id, t.getName());
                if (!usages.containsKey(id) && config.has("kind")) usages.put(id, List.of(new IeDescriptor.Usage(
                        config.get("kind").asText(), config.path("criticality").asText("unspecified"),
                        config.path("presence").asText("unspecified"), "<override>", List.of(), t.getSourceRange())));
            }
        }
        List<IeDescriptor> result = new ArrayList<>();
        Map<String, Long> counts = new HashMap<>();
        typeNames.values().forEach(n -> counts.merge(n, 1L, Long::sum));
        for (Map.Entry<Integer, String> entry : declared.entrySet()) {
            int id = entry.getKey(); String asnName = typeNames.getOrDefault(id, "<unmapped>");
            String base = counts.getOrDefault(asnName, 0L) == 1 && !asnName.startsWith("<")
                    ? asnName : entry.getValue().replaceFirst("^id-", "");
            JsonNode config = overrides.merged(asnName, id);
            String java = config.has("reuse") ? config.get("reuse").asText() : config.has("javaClass") ? config.get("javaClass").asText() : names.className(base);
            String factory = config.has("factoryMethod") ? config.get("factoryMethod").asText() : JavaNames.method(java);
            IeDescriptor d = new IeDescriptor(id, entry.getValue(), asnName, mapped.get(id), java, factory,
                    usages.getOrDefault(id, List.of()));
            result.add(d);
            add(ids, d.idName(), d); add(types, d.asnTypeName(), d); add(aliases, java, d); add(aliases, factory, d);
            for (String s : List.of(d.idName(), d.asnTypeName(), java, factory)) add(normalized, JavaNames.normalized(s), d);
        }
        descriptors = List.copyOf(result);
    }
    private static void add(Map<String, List<IeDescriptor>> map, String key, IeDescriptor d) {
        List<IeDescriptor> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        if (!list.contains(d)) list.add(d);
    }
    public List<IeDescriptor> descriptors() { return descriptors; }
    public record Resolution(String selector, String method, List<IeDescriptor> matches) {
        public Resolution { matches = List.copyOf(matches); }
    }
    public Resolution resolve(String selector, boolean allMatches) {
        String method = "numeric ID"; List<IeDescriptor> candidates;
        if (selector.matches("[0-9]+")) candidates = descriptors.stream().filter(d -> Integer.toString(d.id()).equals(selector.replaceFirst("^0+(?!$)", ""))).toList();
        else {
            method = "id assignment"; candidates = ids.getOrDefault(selector, List.of());
            if (candidates.isEmpty()) { method = "ASN type"; candidates = types.getOrDefault(selector, List.of()); }
            if (candidates.isEmpty()) { method = "Java alias"; candidates = aliases.getOrDefault(selector, List.of()); }
            if (candidates.isEmpty()) { method = "normalized name"; candidates = normalized.getOrDefault(JavaNames.normalized(selector), List.of()); }
        }
        if (candidates.isEmpty()) throw new GenerationException("UNKNOWN_SELECTOR", 5, "Unknown selector: " + selector);
        if (candidates.size() > 1 && !allMatches) throw new GenerationException("AMBIGUOUS_SELECTOR", 5,
                selector + " -> " + candidates.stream().map(d -> d.id() + " " + d.idName()).toList() + "; use ID or --all-matches");
        for (IeDescriptor d : candidates) if (!d.mapped()) throw new GenerationException("ID_DECLARED_BUT_UNMAPPED", 5,
                d.id() + " " + d.idName() + "; supply ies.<id>.asnType override");
        return new Resolution(selector, method, candidates);
    }
    private static String fingerprint(AsnType type, IdentityHashMap<AsnType, Integer> visiting) {
        if (visiting.containsKey(type)) return "cycle:" + visiting.get(type);
        visiting.put(type, visiting.size());
        String result;
        if (type instanceof ReferenceType r && r.getTarget() != null) result = fingerprint(r.getTarget().getType(), visiting);
        else if (type instanceof BuiltinType b) result = b.getKind().name();
        else if (type instanceof IntegerType) result = "INTEGER";
        else if (type instanceof EnumeratedType e) result = "ENUM:" + e.isExtensible() + e.getItems().stream().map(i -> i.getName() + ":" + i.isExtensionAddition()).toList();
        else if (type instanceof CollectionType c) result = c.getKind() + fingerprint(c.getElementType(), visiting);
        else if (type instanceof ConstructedType c) result = c.getKind() + ":" + c.isExtensible() + c.getComponents().stream()
                .map(f -> f.getName() + f.isOptional() + f.isExtensionAddition() + fingerprint(f.getType(), visiting)).toList();
        else result = type.getClass().getSimpleName() + ":" + type;
        if (!type.getConstraints().isEmpty()) {
            try { result += ":" + Bounds.normalize(type.getConstraints()); }
            catch (GenerationException e) { result += ":" + type.getConstraints(); }
        }
        visiting.remove(type);
        return result;
    }
}
