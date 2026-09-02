package com.ancevt.asn1.parse;

import com.ancevt.asn1.link.Asn1Linker;
import com.ancevt.asn1.model.Asn1Document;

/** Public parser entry point for callers that want to provide source text. */
public final class Asn1Parser {
    private Asn1Parser() { }

    public static Asn1Document parse(String source) {
        return parse(source, "<memory>");
    }

    public static Asn1Document parse(String source, String sourceName) {
        var tokens = new Lexer(source, sourceName).lex();
        Asn1Document document = new SyntaxParser(source, sourceName, tokens).parseDocument();
        return new Asn1Linker(document).link();
    }

    /** Parses without semantic linking, useful for custom resolver experiments. */
    public static Asn1Document parseUnlinked(String source, String sourceName) {
        var tokens = new Lexer(source, sourceName).lex();
        return new SyntaxParser(source, sourceName, tokens).parseDocument();
    }
}
