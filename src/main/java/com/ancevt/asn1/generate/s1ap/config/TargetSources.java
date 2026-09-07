package com.ancevt.asn1.generate.s1ap.config;

import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Read-only Java syntax inspection using the JDK parser, never regex constructor extraction. */
public final class TargetSources {
    public record Api(Path path, String packageName, String className, List<List<String>> constructors,
                      boolean decoder, boolean encoder, boolean informationElement, String hash) { }
    private final Map<String, Api> classes = new TreeMap<>();
    private final Map<String, Integer> constants = new TreeMap<>();
    private final boolean runtime;
    public TargetSources(Path sourceRoot) throws IOException {
        if (ToolProvider.getSystemJavaCompiler() == null) throw new GenerationException("MISSING_RUNTIME_CAPABILITY", 6, "Run with JDK, not JRE");
        List<Path> files;
        try (var paths = Files.walk(sourceRoot)) { files = paths.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var fm = compiler.getStandardFileManager(null, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, fm, d -> { }, List.of("-proc:none"), null,
                    fm.getJavaFileObjectsFromPaths(files));
            for (CompilationUnitTree unit : task.parse()) {
                String pkg = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
                Path path = Path.of(unit.getSourceFile().toUri());
                for (Tree declaration : unit.getTypeDecls()) if (declaration instanceof ClassTree cls) {
                    List<List<String>> constructors = new ArrayList<>(); boolean decoder = false, encoder = false;
                    for (Tree member : cls.getMembers()) {
                        if (member instanceof MethodTree m && m.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.PUBLIC)) {
                            List<String> parameters = m.getParameters().stream().map(p -> p.getType().toString()).toList();
                            if (m.getName().contentEquals("<init>")) {
                                constructors.add(parameters);
                                if (parameters.size() == 1 && simple(parameters.get(0)).equals("BitInput")) decoder = true;
                            }
                            if (m.getName().contentEquals("encode") && parameters.size() == 1 && simple(parameters.get(0)).equals("BitOutput")) encoder = true;
                        }
                        if (pkg.equals("tel.core.s1ap.spec") && cls.getSimpleName().contentEquals("ProtocolIeId")
                                && member instanceof VariableTree v && v.getInitializer() instanceof NewClassTree n
                                && n.getArguments().size() == 1 && n.getArguments().get(0) instanceof LiteralTree l
                                && l.getValue() instanceof Number number) constants.put(v.getName().toString(), number.intValue());
                    }
                    String name = cls.getSimpleName().toString();
                    boolean ie = cls.getImplementsClause().stream().anyMatch(t -> simple(t.toString()).equals("InformationElement"));
                    classes.put(pkg + "." + name, new Api(path, pkg, name, List.copyOf(constructors), decoder, encoder, ie,
                            Json.hash(Files.readAllBytes(path))));
                }
            }
        }
        Api helper = classes.get("tel.core.s1ap.core.asn.AsnAper");
        runtime = helper != null && Files.readString(helper.path()).contains("\"AsnAper-v1\"");
    }
    private static String simple(String type) { return type.substring(type.lastIndexOf('.') + 1); }
    public Api find(String pkg, String name) { return classes.get(pkg + "." + name); }
    public Map<String, Integer> constants() { return Collections.unmodifiableMap(constants); }
    public boolean runtime() { return runtime; }
}
