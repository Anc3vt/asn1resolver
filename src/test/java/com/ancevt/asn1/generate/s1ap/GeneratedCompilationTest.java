package com.ancevt.asn1.generate.s1ap;

import com.ancevt.asn1.Asn1;
import com.ancevt.asn1.generate.s1ap.catalog.IeCatalog;
import com.ancevt.asn1.generate.s1ap.config.*;
import com.ancevt.asn1.generate.s1ap.naming.JavaNames;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedCompilationTest {
    @TempDir static Path temp;
    static final Path TARGET = Path.of(System.getProperty("naseps.sourceRoot", Files.isDirectory(Path.of("../naseps/src/main/java"))
            ? "../naseps/src/main/java" : "runtime/naseps-fixture/src/main/java")).toAbsolutePath().normalize();
    static Path output;
    static URLClassLoader loader;
    static final String PKG = "tel.core.s1ap.spec.ie.";
    @BeforeAll static void generateAndCompileAllMappedIds() throws Exception {
        assertTrue(Files.isDirectory(TARGET), "Compilation tests require naseps sources; set -Dnaseps.sourceRoot=...");
        IeCatalog catalog = new IeCatalog(Asn1.read(Path.of("s1ap.asn")), Overrides.empty(), new JavaNames(Map.of()));
        Path selectors = temp.resolve("ids.txt");
        Files.write(selectors, catalog.descriptors().stream().filter(d -> d.mapped()).map(d -> Integer.toString(d.id())).toList());
        output = temp.resolve("generated");
        assertEquals(0, GeneratorTest.cli(output, "--ie-list", selectors.toString(), "--dependency-policy", "closure",
                "--overrides", "config/s1ap-overrides.json", "--extension-policy", "known-additions", "--target-source-root", TARGET.toString()));
        List<Path> sources;
        try (var paths = Files.walk(output.resolve("sources"))) { sources = new ArrayList<>(paths.filter(p -> p.toString().endsWith(".java")).toList()); }
        try (var paths = Files.list(TARGET.resolve("tel/core/s1ap/core/asn"))) { sources.addAll(paths.filter(p -> p.toString().endsWith(".java")).toList()); }
        for (String p : List.of("core/model/InformationElement.java", "core/model/Criticality.java", "core/model/InformationElementDecoderRegistry.java",
                "core/error/S1apException.java", "spec/ProtocolIeId.java")) sources.add(TARGET.resolve("tel/core/s1ap/" + p));
        String common = "package tel.core.s1ap.spec;\nimport tel.core.s1ap.spec.ie.*;\nimport tel.core.s1ap.core.model.Criticality;\nimport tel.core.s1ap.core.model.InformationElement;\n";
        Path facades = temp.resolve("facades"); Files.createDirectories(facades);
        Path info = facades.resolve("InformationElements.java");
        Files.writeString(info, common + "import static tel.core.s1ap.core.model.InformationElementDecoderRegistry.register;\n"
                + "public final class InformationElements { public static void init() {} static {\n"
                + Files.readString(output.resolve("snippets/decoder-registrations.txt")) + "}\n"
                + Files.readString(output.resolve("snippets/InformationElements.methods.txt")) + "}\n");
        Path builder = facades.resolve("MessageBuilder.java");
        Files.writeString(builder, common + "public final class MessageBuilder {\n"
                + "public MessageBuilder addField(ProtocolIeId id, Criticality criticality, InformationElement value) { return this; }\n"
                + Files.readString(output.resolve("snippets/MessageBuilder.methods.txt")) + "}\n");
        sources.add(info); sources.add(builder);
        Path classes = temp.resolve("classes"); Files.createDirectories(classes);
        var compiler = ToolProvider.getSystemJavaCompiler(); assertNotNull(compiler);
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            StringWriter errors = new StringWriter();
            boolean success = compiler.getTask(errors, manager, null, List.of("--release", "21", "-d", classes.toString(), "-proc:none"), null,
                    manager.getJavaFileObjectsFromPaths(sources)).call();
            assertTrue(success, errors.toString());
        }
        loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }
    @AfterAll static void close() throws Exception { if (loader != null) loader.close(); }
    @Test void externalVectorsDecodeAndEncodeByteExactly() throws Exception {
        JsonNode data = Json.read(Path.of("src/test/resources/generator/aper-vectors.json"));
        assertEquals(Json.hash(Files.readAllBytes(Path.of("s1ap.asn"))), data.path("asnSha256").asText());
        assertTrue(data.path("vectors").size() >= 30);
        Class<?> input = loader.loadClass("tel.core.s1ap.core.asn.BitInput");
        for (JsonNode v : data.path("vectors")) {
            byte[] expected = HexFormat.of().parseHex(v.path("hex").asText());
            Object in = input.getConstructor(byte[].class).newInstance((Object) expected);
            Object value;
            try { value = loader.loadClass(PKG + v.path("javaClass").asText()).getConstructor(input).newInstance(in); }
            catch (InvocationTargetException e) { throw new AssertionError(v.path("name").asText(), e.getCause()); }
            assertArrayEquals(expected, (byte[]) value.getClass().getMethod("encode").invoke(value), v.path("name").asText());
        }
    }
    @Test void valueConstructorsAgreeWithIndependentVectors() throws Exception {
        assertEquals("03805465737420654e42", encode(make("EnbName", "Test eNB")));
        assertEquals("c0ffffffff", encode(make("MmeUeS1apId", 4294967295L)));
        assertEquals("12345678", encode(make("GtpTeid", (Object) HexFormat.of().parseHex("12345678"))));
        Class<?> bitValue = loader.loadClass("tel.core.s1ap.core.asn.AsnBitString$Value");
        Object bits = bitValue.getMethod("fromLong", long.class, int.class).invoke(null, 0x12345L, 20);
        Object enbBits = make("EnbIdMacroEnbId", bits);
        Object choice = loader.loadClass(PKG + "EnbId").getMethod("macroEnbId", enbBits.getClass()).invoke(null, enbBits);
        assertEquals("0052f09900123450", encode(make("GlobalEnbId", make("PlmnIdentity", (Object) HexFormat.of().parseHex("52f099")), choice, null)));
    }
    @Test void constructorsRejectInvalidValuesAndCopyArrays() throws Exception {
        assertThrows(InvocationTargetException.class, () -> make("MmeUeS1apId", -1L));
        assertThrows(InvocationTargetException.class, () -> make("EnbName", "Not_printable"));
        assertThrows(InvocationTargetException.class, () -> make("GtpTeid", (Object) new byte[3]));
        byte[] source = {1, 2, 3, 4}; Object teid = make("GtpTeid", (Object) source); source[0] = 0;
        byte[] copy = (byte[]) teid.getClass().getMethod("getBytes").invoke(teid); copy[1] = 0;
        assertEquals("01020304", encode(teid));
    }
    @Test void fullDryRunIsDeterministic() throws Exception {
        JsonNode before = Json.read(output.resolve("reports/generation-report.json"));
        assertEquals(0, GeneratorTest.cli(output, "--ie-list", temp.resolve("ids.txt").toString(), "--dependency-policy", "closure",
                "--overrides", "config/s1ap-overrides.json", "--extension-policy", "known-additions", "--target-source-root", TARGET.toString(),
                "--dry-run", "--conflict-policy", "overwrite"));
        JsonNode after = Json.read(output.resolve("reports/generation-report.json"));
        assertEquals(271, after.path("selectors").size());
        assertEquals(before.path("files").size(), after.path("files").size());
        for (int i = 0; i < before.path("files").size(); i++)
            assertEquals(before.path("files").get(i).path("sha256"), after.path("files").get(i).path("sha256"));
    }
    @Test void goldenSourcesAndSnippets() throws Exception {
        Path golden = Path.of("src/test/resources/generator/golden");
        try (var paths = Files.walk(golden)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                Path relative = golden.relativize(file);
                assertEquals(Files.readString(file), Files.readString(output.resolve(relative)), relative.toString());
            }
        }
    }
    @Test void documentedRootOnlyExamplesCompileAndRejectKnownExtensions() throws Exception {
        Path directory = temp.resolve("root-only");
        assertEquals(0, GeneratorTest.cli(directory, "--ie", "60", "--ie", "59", "--ie", "51", "--dependency-policy", "closure",
                "--overrides", "config/s1ap-overrides.json", "--docs", "required", "--docs-index", "config/spec-index-15.3.0.json"));
        try (var paths = Files.walk(Path.of("examples/basic"))) {
            for (Path file : paths.filter(Files::isRegularFile).filter(p -> !p.toString().contains("reports")).toList()) {
                if (!file.toString().endsWith(".java") && !file.toString().endsWith(".txt")) continue;
                assertEquals(Files.readString(file), Files.readString(directory.resolve(Path.of("examples/basic").relativize(file))));
            }
        }
        try (URLClassLoader rootLoader = compileExtra(directory.resolve("sources"))) {
            Class<?> input = rootLoader.loadClass("tel.core.s1ap.core.asn.BitInput");
            for (JsonNode vector : Json.read(Path.of("src/test/resources/generator/aper-vectors.json")).path("vectors")) {
                if (!Set.of("EnbName", "GlobalEnbId", "ERabSetupList").contains(vector.path("javaClass").asText())) continue;
                byte[] bytes = HexFormat.of().parseHex(vector.path("hex").asText());
                Constructor<?> ctor = rootLoader.loadClass(PKG + vector.path("javaClass").asText()).getConstructor(input);
                Object in = input.getConstructor(byte[].class).newInstance((Object) bytes);
                if (vector.path("extension").asBoolean()) {
                    InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> ctor.newInstance(in));
                    assertEquals("S1apException", error.getCause().getClass().getSimpleName());
                } else {
                    Object value = ctor.newInstance(in);
                    assertArrayEquals(bytes, (byte[]) value.getClass().getMethod("encode").invoke(value));
                }
            }
        }
    }
    @Test void generatedSequenceAdditionsDefaultAndOptionalCollectionCompile() throws Exception {
        var doc = Asn1.parse("""
                M DEFINITIONS ::= BEGIN
                X ::= SEQUENCE { a INTEGER (0..7), ..., b INTEGER (0..255) OPTIONAL, c OCTET STRING OPTIONAL }
                Bits ::= BIT STRING (SIZE(16))
                L ::= SEQUENCE (SIZE(1..3)) OF Bits
                D ::= SEQUENCE { number INTEGER (0..15) DEFAULT 3, values L OPTIONAL }
                END
                """);
        Overrides overrides = new Overrides(Json.MAPPER.readTree("{\"schemaVersion\":1,\"types\":{\"Bits\":{\"representation\":\"int\"},\"D\":{\"optionalCollection\":\"empty\"}}}"), "fixture");
        var normalizer = new com.ancevt.asn1.generate.s1ap.normalize.TypeNormalizer(overrides, new JavaNames(Map.of()));
        normalizer.root("X", "X", doc.requireModule("M").requireType("X").getType(), null);
        normalizer.root("D", "D", doc.requireModule("M").requireType("D").getType(), null);
        Path sources = temp.resolve("fixture-sources"); Files.createDirectories(sources);
        var renderer = new com.ancevt.asn1.generate.s1ap.render.JavaRenderer("fixture", true, null);
        for (var type : normalizer.types().values()) Files.writeString(sources.resolve(type.javaName() + ".java"), renderer.render(type, null, List.of()).source());
        try (URLClassLoader fixtures = compileExtra(sources)) {
            Class<?> input = fixtures.loadClass("tel.core.s1ap.core.asn.BitInput");
            Object x = fixtures.loadClass("fixture.X").getConstructor(input).newInstance(input.getConstructor(byte[].class)
                    .newInstance((Object) HexFormat.of().parseHex("d03801ff020112")));
            assertEquals("d03801ff020112", HexFormat.of().formatHex((byte[]) x.getClass().getMethod("encode").invoke(x)));
            Class<?> d = fixtures.loadClass("fixture.D"), number = fixtures.loadClass("fixture.DNumber");
            Object value = d.getConstructor(number, List.class).newInstance(null, List.of());
            assertEquals("00", HexFormat.of().formatHex((byte[]) d.getMethod("encode").invoke(value)));
            assertEquals(3, number.getMethod("getValue").invoke(d.getMethod("getNumber").invoke(value)));
            Object bitmap = fixtures.loadClass("fixture.Bits").getConstructor(int.class).newInstance(65535);
            assertEquals("ffff", HexFormat.of().formatHex((byte[]) bitmap.getClass().getMethod("encode").invoke(bitmap)));
        }
    }
    @Test void enumMapUnknownAndExtensionCodesPreserveWireValues() throws Exception {
        var doc = Asn1.parse("M DEFINITIONS ::= BEGIN E ::= ENUMERATED { a, b, ..., c, d } END");
        var normalizer = new com.ancevt.asn1.generate.s1ap.normalize.TypeNormalizer(Overrides.empty(), new JavaNames(Map.of()));
        var type = normalizer.root("E", "EnumFixture", doc.requireModule("M").requireType("E").getType(), null);
        Path sources = temp.resolve("enum-sources"); Files.createDirectories(sources);
        for (boolean known : List.of(false, true)) {
            String pkg = known ? "enumknown" : "enumroot";
            Path file = sources.resolve(pkg).resolve("EnumFixture.java"); Files.createDirectories(file.getParent());
            Files.writeString(file, new com.ancevt.asn1.generate.s1ap.render.JavaRenderer(pkg, known, null).render(type, null, List.of()).source());
        }
        try (URLClassLoader fixtures = compileExtra(sources)) {
            Class<?> input = fixtures.loadClass("tel.core.s1ap.core.asn.BitInput");
            Class<?> known = fixtures.loadClass("enumknown.EnumFixture"), values = fixtures.loadClass("enumknown.EnumFixture$Value");
            Method lookup = values.getDeclaredMethod("valueOf", int.class); lookup.setAccessible(true);
            Object unknown = values.getField("UNKNOWN").get(null);
            assertSame(unknown, lookup.invoke(null, -1));
            assertSame(unknown, lookup.invoke(null, 99));
            for (int i = 0; i < 4; i++) {
                Object value = lookup.invoke(null, i);
                assertEquals(i, values.getMethod("getCode").invoke(value));
                Object element = known.getConstructor(values).newInstance(value);
                String hex = List.of("00", "40", "80", "81").get(i);
                assertEquals(hex, HexFormat.of().formatHex((byte[]) known.getMethod("encode").invoke(element)));
                Object decoded = known.getConstructor(input).newInstance(input.getConstructor(byte[].class)
                        .newInstance((Object) HexFormat.of().parseHex(hex)));
                assertSame(value, known.getMethod("getValue").invoke(decoded));
            }
            Object decoded = known.getConstructor(input).newInstance(input.getConstructor(byte[].class)
                    .newInstance((Object) new byte[] {(byte) 0x82}));
            assertSame(unknown, known.getMethod("getValue").invoke(decoded));
            InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> known.getMethod("encode").invoke(decoded));
            assertInstanceOf(IllegalStateException.class, error.getCause());
            Class<?> root = fixtures.loadClass("enumroot.EnumFixture"), rootValues = fixtures.loadClass("enumroot.EnumFixture$Value");
            assertThrows(InvocationTargetException.class, () -> root.getConstructor(rootValues).newInstance(rootValues.getField("C").get(null)));
            error = assertThrows(InvocationTargetException.class, () -> root.getConstructor(input).newInstance(input.getConstructor(byte[].class)
                    .newInstance((Object) new byte[] {(byte) 0x82})));
            assertEquals("S1apException", error.getCause().getClass().getSimpleName());
        }
    }
    private static URLClassLoader compileExtra(Path sources) throws Exception {
        Path classes = Files.createTempDirectory(temp, "extra-classes"); List<Path> files;
        try (var paths = Files.walk(sources)) { files = paths.filter(p -> p.toString().endsWith(".java")).toList(); }
        var compiler = ToolProvider.getSystemJavaCompiler(); StringWriter errors = new StringWriter();
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            assertTrue(compiler.getTask(errors, manager, null, List.of("--release", "21", "-classpath", temp.resolve("classes").toString(), "-d", classes.toString()),
                    null, manager.getJavaFileObjectsFromPaths(files)).call(), errors.toString());
        }
        return new URLClassLoader(new URL[] {classes.toUri().toURL(), temp.resolve("classes").toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }
    static Object make(String name, Object... args) throws Exception {
        Class<?> type = loader.loadClass(PKG + name);
        for (Constructor<?> c : type.getConstructors()) {
            Class<?>[] params = c.getParameterTypes(); if (params.length != args.length) continue;
            boolean matches = true;
            for (int i = 0; i < params.length; i++) {
                if (args[i] == null) { if (params[i].isPrimitive()) matches = false; }
                else if (!params[i].isInstance(args[i]) && !(params[i] == long.class && args[i] instanceof Long)
                        && !(params[i] == int.class && args[i] instanceof Integer)) matches = false;
            }
            if (matches) return c.newInstance(args);
        }
        throw new NoSuchMethodException(name + Arrays.toString(args));
    }
    static String encode(Object value) throws Exception { return HexFormat.of().formatHex((byte[]) value.getClass().getMethod("encode").invoke(value)); }
}
