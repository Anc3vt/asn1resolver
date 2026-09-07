package com.ancevt.asn1.generate.s1ap;

import com.ancevt.asn1.Asn1;
import com.ancevt.asn1.generate.s1ap.catalog.*;
import com.ancevt.asn1.generate.s1ap.config.*;
import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.generate.s1ap.docs.Documentation;
import com.ancevt.asn1.generate.s1ap.naming.JavaNames;
import com.ancevt.asn1.generate.s1ap.normalize.*;
import com.ancevt.asn1.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    @TempDir Path temp;
    static Asn1Document document;
    static IeCatalog catalog;
    static final JavaNames NAMES = new JavaNames(Map.of());
    @BeforeAll static void readSchema() throws Exception {
        document = Asn1.read(Path.of("s1ap.asn")); catalog = new IeCatalog(document, Overrides.empty(), NAMES);
    }
    @Test void catalogBaselineAndInlineType() {
        assertEquals(279, catalog.descriptors().size());
        assertEquals(271, catalog.descriptors().stream().filter(IeDescriptor::mapped).count());
        assertEquals(272, catalog.descriptors().stream().mapToLong(d -> d.usages().stream().map(IeDescriptor.Usage::kind).distinct().count()).sum());
        assertInstanceOf(BuiltinType.class, catalog.resolve("225", false).matches().get(0).type());
        assertEquals(2, catalog.resolve("60", false).matches().get(0).usages().size());
        assertEquals(List.of("S1SetupRequest"), catalog.resolve("60", false).matches().get(0).usages().get(0).messages());
    }
    @Test void selectorPrecedenceAmbiguityAndUnmapped() {
        for (String s : List.of("60", "id-eNBname", "ENBname", "EnbName", "enbName", "enb-name"))
            assertEquals(60, catalog.resolve(s, false).matches().get(0).id());
        assertEquals("AMBIGUOUS_SELECTOR", assertThrows(GenerationException.class, () -> catalog.resolve("PagingDRX", false)).code());
        assertEquals(List.of(44, 137), catalog.resolve("PagingDRX", true).matches().stream().map(IeDescriptor::id).toList());
        for (String s : List.of("3", "38", "49", "55", "57", "63", "103", "126"))
            assertEquals("ID_DECLARED_BUT_UNMAPPED", assertThrows(GenerationException.class, () -> catalog.resolve(s, false)).code());
        assertEquals("UNKNOWN_SELECTOR", assertThrows(GenerationException.class, () -> catalog.resolve("does-not-exist", false)).code());
    }
    @Test void namesAndUnsafeIdentifiers() {
        Map<String, String> names = Map.of("MME-UE-S1AP-ID", "MmeUeS1apId", "ENBname", "EnbName", "E-RABLevelQoSParameters", "ERabLevelQosParameters",
                "GTP-TEID", "GtpTeid", "UEAggregateMaximumBitrate", "UeAggregateMaximumBitrate", "PLMNidentity", "PlmnIdentity");
        names.forEach((asn, java) -> assertEquals(java, NAMES.className(asn)));
        for (String invalid : List.of("class", "CON", "LPT1", "../Bad", "x/y", "C:\\Bad", "record"))
            assertThrows(GenerationException.class, () -> JavaNames.identifier(invalid));
    }
    @Test void linkedIntegerChainsAndCycles() {
        Asn1Document doc = Asn1.parse("M DEFINITIONS ::= BEGIN A ::= INTEGER a A ::= 7 b A ::= a c A ::= b END");
        assertEquals(BigInteger.valueOf(7), IntegerValues.evaluate((ValueAssignment) doc.requireModule("M").getAssignments().get(3)));
        Asn1Document cyclic = Asn1.parse("M DEFINITIONS ::= BEGIN a INTEGER ::= b b INTEGER ::= a END");
        assertThrows(GenerationException.class, () -> IntegerValues.evaluate((ValueAssignment) cyclic.requireModule("M").getAssignments().get(0)));
    }
    @Test void effectiveRangesUnionsAndSymbolicSizes() {
        Asn1Document doc = Asn1.parse("M DEFINITIONS ::= BEGIN lim INTEGER ::= 32 A ::= INTEGER (0..100) B ::= A (5..10 | 20..30) C ::= OCTET STRING (SIZE(1..lim)) END");
        TypeNormalizer n = new TypeNormalizer(Overrides.empty(), NAMES);
        GenType b = n.root("B", "B", doc.requireModule("M").requireType("B").getType(), null);
        assertEquals(2, b.bounds().intervals().size()); assertEquals(BigInteger.valueOf(5), b.bounds().min());
        GenType c = n.root("C", "C", doc.requireModule("M").requireType("C").getType(), null);
        assertEquals(BigInteger.valueOf(32), c.bounds().max());
        assertThrows(UnsupportedOperationException.class, () -> b.dependencies().add("X"));
    }
    @Test void recursiveDependencyGraphDoesNotOverflow() {
        Asn1Document doc = Asn1.parse("M DEFINITIONS ::= BEGIN Node ::= SEQUENCE { child Node OPTIONAL } END");
        TypeNormalizer n = new TypeNormalizer(Overrides.empty(), NAMES);
        n.root("Node", "Node", doc.requireModule("M").requireType("Node").getType(), null);
        assertEquals(1, n.types().size()); assertFalse(n.cycles().isEmpty());
    }
    @Test void incompatibleMappingsReportEverySourceRange() {
        String source = """
                M DEFINITIONS ::= BEGIN
                ProtocolIE-ID ::= INTEGER (0..65535)
                Criticality ::= ENUMERATED { reject, ignore, notify }
                Presence ::= ENUMERATED { optional, mandatory }
                S1AP-PROTOCOL-IES ::= CLASS {
                    &id ProtocolIE-ID UNIQUE, &criticality Criticality, &Value, &presence Presence
                } WITH SYNTAX { ID &id CRITICALITY &criticality TYPE &Value PRESENCE &presence }
                id-Test ProtocolIE-ID ::= 1
                A ::= INTEGER (0..255)
                B ::= OCTET STRING
                First S1AP-PROTOCOL-IES ::= { { ID id-Test CRITICALITY ignore TYPE A PRESENCE optional } }
                Second S1AP-PROTOCOL-IES ::= { { ID id-Test CRITICALITY ignore TYPE B PRESENCE optional } }
                END
                """;
        Asn1Document doc = Asn1.parse(source);
        assertEquals(0, doc.getErrorCount());
        GenerationException error = assertThrows(GenerationException.class, () -> new IeCatalog(doc, Overrides.empty(), NAMES));
        assertEquals("IE_ID_TYPE_CONFLICT", error.code()); assertNotNull(error.range()); assertEquals(1, error.related().size());
        IeCatalog dedup = new IeCatalog(Asn1.parse(source.replace("TYPE B", "TYPE A")), Overrides.empty(), NAMES);
        assertEquals(1, dedup.descriptors().size()); assertEquals(2, dedup.descriptors().get(0).usages().size());
    }
    @Test void optionalCollectionAndLosslessBitmapOverrides() throws Exception {
        Asn1Document doc = Asn1.parse("M DEFINITIONS ::= BEGIN Bits ::= BIT STRING (SIZE(16)) L ::= SEQUENCE (SIZE(1..3)) OF Bits S ::= SEQUENCE { values L OPTIONAL } END");
        Overrides overrides = new Overrides(Json.MAPPER.readTree("{\"schemaVersion\":1,\"types\":{\"Bits\":{\"representation\":\"int\"},\"S\":{\"optionalCollection\":\"empty\"}}}"), "test");
        TypeNormalizer normalizer = new TypeNormalizer(overrides, NAMES);
        GenType s = normalizer.root("S", "S", doc.requireModule("M").requireType("S").getType(), null);
        assertEquals("Bits", s.fields().get(0).emptyAbsentElementType());
        assertEquals("int", normalizer.types().get("Bits").representation());
        String code = new com.ancevt.asn1.generate.s1ap.render.JavaRenderer("test", false, null).render(s, null, List.of()).source();
        assertTrue(code.contains("List.copyOf(values)")); assertTrue(code.contains("!values.isEmpty()"));
        Asn1Document bad = Asn1.parse("M DEFINITIONS ::= BEGIN Bits ::= BIT STRING (SIZE(16)) L ::= SEQUENCE (SIZE(0..3)) OF Bits S ::= SEQUENCE { values L OPTIONAL } END");
        assertEquals("INVALID_OVERRIDE", assertThrows(GenerationException.class, () -> new TypeNormalizer(overrides, NAMES)
                .root("S", "S", bad.requireModule("M").requireType("S").getType(), null)).code());
    }
    @Test void failClosedOnRawConstraintAndAliasCycles() {
        Asn1Document raw = Asn1.parse("M DEFINITIONS ::= BEGIN A ::= INTEGER (ALL EXCEPT 4) END");
        assertThrows(GenerationException.class, () -> new TypeNormalizer(Overrides.empty(), NAMES).root("A", "A", raw.requireModule("M").requireType("A").getType(), null));
        Asn1Document cyclic = Asn1.parse("M DEFINITIONS ::= BEGIN A ::= B B ::= A END");
        assertEquals("UNRESOLVED_TYPE_REFERENCE", assertThrows(GenerationException.class, () -> new TypeNormalizer(Overrides.empty(), NAMES)
                .root("A", "A", cyclic.requireModule("M").requireType("A").getType(), null)).code());
    }
    @Test void strictOverridesAndManualMapping() throws Exception {
        assertThrows(GenerationException.class, () -> new Overrides(Json.MAPPER.readTree("{\"schemaVersion\":1,\"typo\":{}}"), "test"));
        Overrides o = new Overrides(Json.MAPPER.readTree("{\"schemaVersion\":1,\"types\":{\"PagingDRX\":{\"javaClass\":\"Drx\"}},\"ies\":{\"137\":{\"javaClass\":\"DefaultPagingDrx\"},\"3\":{\"asnType\":\"ENBname\"}}}"), "test");
        IeCatalog c = new IeCatalog(document, o, NAMES);
        assertEquals("DefaultPagingDrx", c.resolve("137", false).matches().get(0).javaName());
        assertEquals("ENBname", c.resolve("3", false).matches().get(0).asnTypeName());
    }
    @Test void docsVersionAndHashLock() throws Exception {
        String hash = Json.hash(Files.readAllBytes(Path.of("s1ap.asn")));
        Documentation docs = new Documentation(Path.of("config/spec-index-15.3.0.json"), hash, "15.3.0");
        assertEquals(List.of("s1-setup-request"), docs.references(60, "ENBname", Overrides.empty()));
        assertThrows(GenerationException.class, () -> new Documentation(Path.of("config/spec-index-15.3.0.json"), "bad", "15.3.0"));
        assertThrows(GenerationException.class, () -> new Documentation(Path.of("config/spec-index-15.3.0.json"), hash, "15.11.0"));
    }
    @Test void cliNoOverwriteAndAtomicErrorRun() throws Exception {
        Path output = temp.resolve("atomic");
        assertEquals(5, cli(output, "--ie", "60", "--ie", "PagingDRX"));
        assertFalse(Files.exists(output.resolve("sources")));
        assertTrue(Files.exists(output.resolve("reports/generation-report.json")));
        assertEquals(0, cli(output, "--ie", "60"));
        Path file = output.resolve("sources/tel/core/s1ap/spec/ie/EnbName.java");
        String original = Files.readString(file);
        assertEquals(7, cli(output, "--ie", "60")); assertEquals(original, Files.readString(file));
        assertEquals(0, cli(output, "--ie", "60", "--conflict-policy", "skip"));
        assertEquals(0, cli(output, "--ie", "60", "--conflict-policy", "overwrite"));
        assertEquals(original, Files.readString(file));
    }
    @Test void listFileDeduplicationAndIndependentErrors() throws Exception {
        Path list = temp.resolve("list.txt"); Files.writeString(list, "# requested\n60\n\n60\nid-eNBname\n");
        Path output = temp.resolve("list"); assertEquals(0, cli(output, "--ie-list", list.toString()));
        assertEquals(1, Json.read(output.resolve("reports/generation-report.json")).path("selectors").size());
        assertEquals(5, cli(temp.resolve("errors"), "--ie", "bad", "--ie", "PagingDRX", "--ie", "3"));
        assertEquals(3, Json.read(temp.resolve("errors/reports/generation-report.json")).path("diagnostics").size());
    }
    @Test void defaultDependencyNoneFailsWithoutTarget() {
        assertEquals(6, cli(temp.resolve("none"), "--ie", "59", "--dependency-policy", "none"));
    }
    @Test void missingRequiredDocsDoesNotWriteSources() {
        Path output = temp.resolve("docs"); assertEquals(8, cli(output, "--ie", "65", "--docs", "required", "--docs-index", "config/spec-index-15.3.0.json"));
        assertFalse(Files.exists(output.resolve("sources")));
    }
    static int cli(Path output, String... extra) {
        List<String> args = new ArrayList<>(List.of("generate", "--asn", "s1ap.asn", "--output", output.toString())); args.addAll(List.of(extra));
        try (PrintStream quiet = new PrintStream(new ByteArrayOutputStream())) { return S1apGeneratorMain.run(args.toArray(String[]::new), quiet, quiet); }
    }
}
