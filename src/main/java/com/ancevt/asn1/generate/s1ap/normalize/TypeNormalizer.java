package com.ancevt.asn1.generate.s1ap.normalize;

import com.ancevt.asn1.generate.s1ap.config.Overrides;
import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.generate.s1ap.naming.JavaNames;
import com.ancevt.asn1.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigInteger;
import java.util.*;

/** Builds finite graphs by reserving each Java type before following its dependencies. */
public final class TypeNormalizer {
    private final Overrides overrides;
    private final JavaNames names;
    private final LinkedHashMap<String, GenType> types = new LinkedHashMap<>();
    private final Map<String, AsnType> sources = new HashMap<>();
    private final Map<String, String> caseNames = new HashMap<>();
    private final List<List<String>> cycles = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();
    public TypeNormalizer(Overrides overrides, JavaNames names) { this.overrides = overrides; this.names = names; }
    public Map<String, GenType> types() { return Collections.unmodifiableMap(types); }
    public List<List<String>> cycles() { return List.copyOf(cycles); }
    public GenType root(String asnName, String javaName, AsnType type, Integer id) {
        normalize(asnName, javaName, type, overrides.merged(asnName, id));
        return types.get(javaName);
    }
    private String dependency(AsnType type, String fallback) {
        String asn = type instanceof ReferenceType r && r.getActualParameters().isEmpty() && r.getConstraints().isEmpty() ? r.getName() : fallback;
        JsonNode config = overrides.type(asn);
        String java = config.has("reuse") ? config.get("reuse").asText() : config.has("javaClass") ? config.get("javaClass").asText() : names.className(asn);
        return normalize(asn, java, type, config);
    }
    private String normalize(String asnName, String javaName, AsnType source, JsonNode config) {
        JavaNames.identifier(javaName);
        String previous = caseNames.putIfAbsent(javaName.toLowerCase(Locale.ROOT), javaName);
        if (previous != null && !previous.equals(javaName)) throw error("JAVA_NAME_COLLISION", source, previous + " / " + javaName);
        if (sources.containsKey(javaName)) {
            AsnType existing = sources.get(javaName);
            if (existing.dereference() != source.dereference()) throw error("JAVA_NAME_COLLISION", source, javaName + " represents different ASN types");
            if (stack.contains(javaName)) { List<String> cycle = new ArrayList<>(stack); cycle.add(javaName); cycles.add(List.copyOf(cycle)); }
            return javaName;
        }
        sources.put(javaName, source); stack.addLast(javaName);
        try {
            List<Constraint> constraints = new ArrayList<>(); List<String> aliases = new ArrayList<>();
            Set<AsnType> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
            AsnType type = source;
            while (true) {
                if (!visiting.add(type)) throw error("UNRESOLVED_TYPE_REFERENCE", type, "Alias cycle: " + aliases);
                constraints.addAll(type.getConstraints());
                if (type instanceof ReferenceType r) {
                    aliases.add(r.getName());
                    if (!r.getActualParameters().isEmpty()) {
                        GenType container = container(asnName, javaName, r, aliases, config);
                        types.put(javaName, container); return javaName;
                    }
                    if (r.getTarget() == null) throw error("UNRESOLVED_TYPE_REFERENCE", r, r.getName());
                    type = r.getTarget().getType();
                } else break;
            }
            Bounds bounds = Bounds.normalize(constraints);
            validateMemberOverrides(type, config);
            GenType.Kind kind; String representation = ""; boolean extensible = bounds != null && bounds.extensible();
            List<GenType.Field> fields = new ArrayList<>(); List<GenType.Item> items = new ArrayList<>();
            List<String> dependencies = new ArrayList<>(); String element = null;
            if (type instanceof IntegerType) {
                kind = GenType.Kind.INTEGER;
                if (bounds == null) representation = "BigInteger";
                else representation = fits(bounds, Integer.MIN_VALUE, Integer.MAX_VALUE) ? "int"
                        : fits(bounds, Long.MIN_VALUE, Long.MAX_VALUE) ? "long" : "BigInteger";
                // Extensible integers need an unbounded lossless representation in known-additions mode.
                if (extensible) representation = "BigInteger";
            } else if (type instanceof EnumeratedType e) {
                kind = GenType.Kind.ENUMERATED; extensible = e.isExtensible(); representation = "Value";
                validateEnumOrder(e);
                int root = 0, extension = 0;
                for (EnumerationItem i : e.getItems()) items.add(new GenType.Item(i.getName(),
                        config.path("enumAliases").has(i.getName()) ? config.path("enumAliases").get(i.getName()).asText() : names.constant(i.getName()),
                        i.isExtensionAddition() ? extension++ : root++, i.isExtensionAddition(), i.getSourceRange()));
            } else if (type instanceof BuiltinType b) {
                kind = switch (b.getKind()) {
                    case OCTET_STRING -> GenType.Kind.OCTET_STRING;
                    case BIT_STRING -> GenType.Kind.BIT_STRING;
                    case PRINTABLE_STRING -> GenType.Kind.PRINTABLE_STRING;
                    case NULL -> GenType.Kind.NULL;
                    default -> throw error("UNSUPPORTED_ASN_TYPE", type, b.getKind().toString());
                };
                representation = switch (kind) {
                    case OCTET_STRING -> "byte[]"; case BIT_STRING -> "AsnBitString.Value";
                    case PRINTABLE_STRING -> "String"; default -> "";
                };
                for (NamedNumber bit : b.getNamedBits()) {
                    BigInteger number = bit.getNumericValue();
                    if (number == null && bit.getDefinedValue() != null && bit.getDefinedValue().getTarget() instanceof ValueAssignment v)
                        number = IntegerValues.evaluate(v);
                    if (number == null || number.signum() < 0 || number.bitLength() > 31)
                        throw error("UNSUPPORTED_CONSTRAINT", type, "Invalid/unresolved named bit index");
                    String constant = config.path("enumAliases").has(bit.getName()) ? config.path("enumAliases").get(bit.getName()).asText() : names.constant(bit.getName());
                    items.add(new GenType.Item(bit.getName(), constant, number.intValueExact(), false, bit.getSourceRange()));
                }
            } else if (type instanceof ConstructedType c) {
                if (c.getKind() == ConstructedType.Kind.SET) throw error("UNSUPPORTED_ASN_TYPE", type, "SET canonical ordering requires tag model");
                kind = c.getKind() == ConstructedType.Kind.CHOICE ? GenType.Kind.CHOICE : GenType.Kind.SEQUENCE;
                extensible = c.isExtensible();
                for (Component f : c.getComponents()) {
                    if (f.isComponentsOf()) throw error("UNSUPPORTED_ASN_TYPE", f.getType(), "COMPONENTS OF");
                    String field = config.path("fields").has(f.getName()) ? config.path("fields").get(f.getName()).asText() : names.field(f.getName());
                    if (Set.of("in", "out", "hasExtensions", "additions", "content", "index", "choice").contains(field))
                        throw error("JAVA_NAME_COLLISION", f.getType(), "Field collides with generated API/local: " + field + "; set fields override");
                    String dep = dependency(f.getType(), javaName + names.className(f.getName()));
                    dependencies.add(dep);
                    String defaultExpression = null;
                    String emptyAbsentElement = null;
                    GenType fieldType = types.get(dep);
                    if (config.path("optionalCollection").asText().equals("empty") && f.isOptional() && !f.isExtensionAddition()
                            && fieldType != null && fieldType.kind() == GenType.Kind.COLLECTION) {
                        if (fieldType.bounds() == null || fieldType.bounds().min().signum() <= 0)
                            throw error("INVALID_OVERRIDE", f.getType(), "Empty-as-absent requires root minimum >= 1");
                        emptyAbsentElement = fieldType.elementType();
                    }
                    if (f.getDefaultValue() != null) {
                        GenType d = types.get(dep);
                        if (d == null || d.kind() != GenType.Kind.INTEGER) throw error("UNSUPPORTED_ASN_TYPE", f.getType(), "DEFAULT currently requires INTEGER");
                        BigInteger v = IntegerValues.evaluate(f.getDefaultValue());
                        defaultExpression = "new " + dep + "(" + (d.representation().equals("BigInteger")
                                ? "new java.math.BigInteger(\"" + v + "\")" : v + (d.representation().equals("long") ? "L" : "")) + ")";
                    }
                    fields.add(new GenType.Field(f.getName(), field, dep, f.isOptional() || f.getDefaultValue() != null,
                            f.isExtensionAddition(), defaultExpression, emptyAbsentElement, f.getSourceRange()));
                }
            } else if (type instanceof CollectionType c) {
                if (c.getKind() == CollectionType.Kind.SET_OF) throw error("UNSUPPORTED_ASN_TYPE", type, "SET OF ordering");
                kind = GenType.Kind.COLLECTION;
                element = dependency(c.getElementType(), javaName + "Item");
                dependencies.add(element); representation = "List<" + element + ">";
                GenType child = types.get(element);
                if (child != null && child.kind() == GenType.Kind.CONTAINER && child.alternatives().size() == 1) {
                    element = child.alternatives().get(0).javaType();
                    dependencies.add(element); representation = "List<" + element + ">";
                    types.put(javaName, new GenType(asnName, javaName, source.getSourceRange(), kind, bounds, extensible,
                            representation, fields, items, element, child.alternatives(), aliases, List.of(element),
                            List.of("AsnAper-v1"), provenance(config)));
                    return javaName;
                }
            } else throw error("UNSUPPORTED_ASN_TYPE", type, type.getClass().getSimpleName());
            if (bounds != null && kind != GenType.Kind.INTEGER && (bounds.min().signum() < 0 || bounds.max().bitLength() > 31))
                throw error("UNSUPPORTED_CONSTRAINT", type, "Size outside Java collection/array range");
            if (config.has("representation") && !config.path("representation").asText().equals("custom")) {
                String requested = config.get("representation").asText();
                if (!requested.equals(representation)) {
                    boolean integerWiden = kind == GenType.Kind.INTEGER && (requested.equals("BigInteger")
                            || requested.equals("long") && representation.equals("int"));
                    boolean bitRepresentation = kind == GenType.Kind.BIT_STRING && bounds != null && !bounds.extensible()
                            && bounds.min().equals(bounds.max()) && ((requested.equals("int") && bounds.max().intValueExact() <= 32)
                            || (requested.equals("long") && bounds.max().intValueExact() <= 64)
                            || (requested.equals("byte[]") && bounds.max().intValueExact() % 8 == 0));
                    if (!integerWiden && !bitRepresentation) throw error("INVALID_OVERRIDE", type, "Lossy/incompatible representation " + requested);
                    representation = requested;
                }
            }
            checkUnique(fields.stream().map(GenType.Field::javaName).toList(), type);
            if (kind == GenType.Kind.CHOICE) checkUnique(fields.stream().map(f -> f.javaName().toUpperCase(Locale.ROOT)).toList(), type);
            checkUnique(items.stream().map(GenType.Item::javaName).toList(), type);
            types.put(javaName, new GenType(asnName, javaName, source.getSourceRange(), kind, bounds, extensible,
                    representation, fields, items, element, List.of(), aliases, dependencies.stream().distinct().toList(),
                    List.of("AsnAper-v1"), provenance(config)));
            return javaName;
        } finally { stack.removeLast(); }
    }
    private GenType container(String asn, String java, ReferenceType ref, List<String> aliases, JsonNode config) {
        if (ref.getTarget() != null && ref.getTarget().getType() instanceof ReferenceType template
                && template.getName().equals("ProtocolIE-ContainerList")) {
            // Resolve the parameterized alias through linked model values and its concrete set.
            List<ActualParameter> bounds = template.getActualParameters();
            if (bounds.size() != 3) throw error("UNSUPPORTED_ASN_TYPE", ref, "Invalid ProtocolIE-ContainerList arity");
            BigInteger min = parameterInteger(bounds.get(0)), max = parameterInteger(bounds.get(1));
            if (min.signum() < 0 || min.compareTo(max) > 0 || max.bitLength() > 31)
                throw error("UNSUPPORTED_CONSTRAINT", ref, "Invalid container-list bounds");
            List<ObjectSetAssignment> sets = ref.getActualParameters().stream().flatMap(p -> p.getReferences().stream())
                    .map(SymbolReference::getTarget).filter(ObjectSetAssignment.class::isInstance).map(ObjectSetAssignment.class::cast).toList();
            if (sets.size() != 1) throw error("UNRESOLVED_TYPE_REFERENCE", ref, "Container-list requires concrete object set");
            List<GenType.OpenAlternative> alternatives = alternatives(sets.get(0), java);
            if (alternatives.size() != 1) throw error("UNSUPPORTED_ASN_TYPE", ref, "Container-list requires one item type");
            String element = alternatives.get(0).javaType();
            return new GenType(asn, java, ref.getSourceRange(), GenType.Kind.COLLECTION,
                    new Bounds(List.of(new Bounds.Interval(min, max)), false), false, "List<" + element + ">",
                    List.of(), List.of(), element, alternatives, aliases, List.of(element), List.of("AsnAper-v1"), provenance(config));
        }
        if (!Set.of("ProtocolIE-Container", "ProtocolIE-SingleContainer", "ProtocolExtensionContainer").contains(ref.getName()))
            throw error("UNSUPPORTED_ASN_TYPE", ref, "Parameterized type " + ref.getName());
        List<ObjectSetAssignment> sets = ref.getActualParameters().stream().flatMap(p -> p.getReferences().stream())
                .map(SymbolReference::getTarget).filter(ObjectSetAssignment.class::isInstance).map(ObjectSetAssignment.class::cast).toList();
        if (sets.size() != 1) throw error("UNRESOLVED_TYPE_REFERENCE", ref, "Container needs one concrete object set");
        List<GenType.OpenAlternative> alternatives = alternatives(sets.get(0), java);
        boolean single = ref.getName().equals("ProtocolIE-SingleContainer");
        return new GenType(asn, java, ref.getSourceRange(), GenType.Kind.CONTAINER,
                single ? null : new Bounds(List.of(new Bounds.Interval(BigInteger.valueOf(ref.getName().equals("ProtocolIE-Container") ? 0 : 1), BigInteger.valueOf(65535))), false),
                false, single ? "single" : "multiple", List.of(), List.of(), null, alternatives, aliases,
                alternatives.stream().map(GenType.OpenAlternative::javaType).distinct().toList(), List.of("AsnAper-v1"), provenance(config));
    }
    private BigInteger parameterInteger(ActualParameter parameter) {
        if (parameter.getReferences().size() == 1 && parameter.getReferences().get(0).getTarget() instanceof ValueAssignment v)
            return IntegerValues.evaluate(v);
        try { return new BigInteger(parameter.getSourceText().trim()); }
        catch (NumberFormatException e) { throw new GenerationException("UNSUPPORTED_CONSTRAINT", 6, "Unresolved container-list bound", parameter.getSourceRange(), List.of()); }
    }
    private List<GenType.OpenAlternative> alternatives(ObjectSetAssignment set, String java) {
        List<GenType.OpenAlternative> alternatives = new ArrayList<>();
        for (ObjectDefinition obj : set.getExpression().getAllObjects()) {
            ObjectFieldSetting setting = obj.findSetting("Value").orElseGet(() -> obj.requireSetting("Extension"));
            int id = IntegerValues.id(obj.requireSetting("id"));
            AsnType type = setting.getValueTarget() instanceof TypeAssignment t ? t.getType() : setting.getTypeValue();
            String name = setting.getValueTarget() instanceof TypeAssignment t ? t.getName() : java + "Item" + id;
            JsonNode override = overrides.type(name);
            String dep = normalize(name, override.has("reuse") ? override.get("reuse").asText() : override.has("javaClass") ? override.get("javaClass").asText() : names.className(name), type, override);
            if (alternatives.stream().noneMatch(a -> a.id() == id)) alternatives.add(new GenType.OpenAlternative(id,
                    obj.requireSetting("criticality").getSourceText().trim(), obj.requireSetting("presence").getSourceText().trim(),
                    dep, obj.getSourceRange()));
        }
        return alternatives;
    }
    private Map<String, String> provenance(JsonNode config) {
        Map<String, String> result = new TreeMap<>();
        config.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().toString()));
        if (!result.isEmpty()) result.put("source", overrides.provenance());
        return result;
    }
    private void validateMemberOverrides(AsnType type, JsonNode config) {
        Set<String> fields = type instanceof ConstructedType c
                ? new HashSet<>(c.getComponents().stream().map(Component::getName).toList()) : Set.of();
        config.path("fields").fieldNames().forEachRemaining(name -> {
            if (!fields.contains(name)) throw error("INVALID_OVERRIDE", type, "Unknown field override: " + name);
        });
        Set<String> constants = type instanceof EnumeratedType e ? new HashSet<>(e.getItems().stream().map(EnumerationItem::getName).toList())
                : type instanceof BuiltinType b ? new HashSet<>(b.getNamedBits().stream().map(NamedNumber::getName).toList()) : Set.of();
        config.path("enumAliases").fieldNames().forEachRemaining(name -> {
            if (!constants.contains(name)) throw error("INVALID_OVERRIDE", type, "Unknown enum/named bit override: " + name);
        });
    }
    private void validateEnumOrder(EnumeratedType type) {
        Map<EnumerationItem, BigInteger> explicit = new IdentityHashMap<>();
        Set<BigInteger> used = new HashSet<>();
        for (EnumerationItem item : type.getItems()) if (!item.isExtensionAddition()) {
            BigInteger value = item.getNumericValue();
            if (value == null && item.getDefinedValue() != null) {
                if (!(item.getDefinedValue().getTarget() instanceof ValueAssignment v))
                    throw error("UNSUPPORTED_CONSTRAINT", type, "Unresolved enumeration value");
                value = IntegerValues.evaluate(v);
            }
            if (value != null) {
                if (!used.add(value)) throw error("UNSUPPORTED_CONSTRAINT", type, "Duplicate enumeration value");
                explicit.put(item, value);
            }
        }
        BigInteger next = BigInteger.ZERO, previous = null;
        for (EnumerationItem item : type.getItems()) if (!item.isExtensionAddition()) {
            BigInteger value = explicit.get(item);
            if (value == null) {
                while (used.contains(next)) next = next.add(BigInteger.ONE);
                value = next; next = next.add(BigInteger.ONE);
            }
            if (previous != null && value.compareTo(previous) <= 0)
                throw error("UNSUPPORTED_CONSTRAINT", type, "ENUMERATED numeric order differs from source order; canonical index reordering is required");
            previous = value;
        }
    }
    private void checkUnique(List<String> names, AsnType type) {
        Set<String> seen = new HashSet<>();
        for (String n : names) if (!seen.add(n)) throw error("JAVA_NAME_COLLISION", type, n);
    }
    private static boolean fits(Bounds bounds, long min, long max) {
        return bounds.min().compareTo(BigInteger.valueOf(min)) >= 0 && bounds.max().compareTo(BigInteger.valueOf(max)) <= 0;
    }
    private static GenerationException error(String code, AsnType type, String message) {
        return new GenerationException(code, code.equals("INVALID_OVERRIDE") ? 8 : 6, message, type.getSourceRange(), List.of());
    }
}
