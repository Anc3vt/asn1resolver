package com.ancevt.asn1.parse;

import java.util.Objects;

/**
 * A token and its exact half-open source range. Offsets are zero-based Java
 * string offsets; line and column numbers are one-based. End coordinates point
 * immediately after the token.
 */
record Token(
        TokenKind kind,
        String lexeme,
        String sourceName,
        int startOffset,
        int endOffset,
        int startLine,
        int startColumn,
        int endLine,
        int endColumn
) {
    Token {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(lexeme, "lexeme");
        sourceName = sourceName == null ? "<memory>" : sourceName;

        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid token offsets");
        }
        if (startLine < 1 || startColumn < 1 || endLine < 1 || endColumn < 1) {
            throw new IllegalArgumentException("Line and column numbers must be positive");
        }
    }

    boolean isIdentifier(String expected) {
        return kind == TokenKind.IDENTIFIER && lexeme.equals(expected);
    }

    @Override
    public String toString() {
        return kind + "('" + lexeme + "') at " + sourceName + ':' + startLine + ':' + startColumn;
    }
}
