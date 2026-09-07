package com.ancevt.asn1.generate.s1ap.render;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import javax.tools.*;

/** Emits the transitive set of private codec members needed by a generated IE. */
final class InlineAper {
    private record Member(String name, String source, Set<String> references) { }
    private static final List<Member> MEMBERS = readTemplate();
    private InlineAper() { }

    static String members(String source) {
        Set<String> required = new HashSet<>();
        for (Member member : MEMBERS)
            if (Pattern.compile("\\b" + member.name() + "\\b").matcher(source).find()) required.add(member.name());
        boolean changed;
        do {
            changed = false;
            for (Member member : MEMBERS)
                if (required.contains(member.name())) changed |= required.addAll(member.references());
        } while (changed);
        StringBuilder result = new StringBuilder();
        for (Member member : MEMBERS) if (required.contains(member.name()))
            result.append('\n').append(member.source().indent(4));
        return result.toString();
    }

    private static List<Member> readTemplate() {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Generation requires a JDK (17 or newer)");
        try (InputStream input = InlineAper.class.getResourceAsStream("/s1ap-generator/inline-aper.java.txt");
             var manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            if (input == null) throw new IllegalStateException("Missing inline APER template");
            String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///InlineAperTemplate.java"), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
            };
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null, List.of(file));
            CompilationUnitTree unit = task.parse().iterator().next();
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR))
                throw new IllegalStateException("Invalid inline APER template: " + diagnostics.getDiagnostics());
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            List<Member> members = new ArrayList<>();
            for (Tree tree : ((ClassTree) unit.getTypeDecls().get(0)).getMembers()) {
                String name = tree instanceof MethodTree m ? m.getName().toString()
                        : tree instanceof VariableTree v ? v.getName().toString()
                        : ((ClassTree) tree).getSimpleName().toString();
                Set<String> references = new HashSet<>();
                new TreeScanner<Void, Void>() {
                    @Override public Void visitIdentifier(IdentifierTree node, Void unused) {
                        references.add(node.getName().toString());
                        return super.visitIdentifier(node, unused);
                    }
                }.scan(tree, null);
                int start = (int) positions.getStartPosition(unit, tree), end = (int) positions.getEndPosition(unit, tree);
                members.add(new Member(name, ("    " + source.substring(start, end)).stripIndent(), references));
            }
            return List.copyOf(members);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
