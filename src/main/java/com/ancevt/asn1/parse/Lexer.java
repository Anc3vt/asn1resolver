package com.ancevt.asn1.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class Lexer {
    private final String source;
    private final String sourceName;

    private int offset;
    private int line;
    private int column;

    Lexer(String source, String sourceName) {
        this.source = Objects.requireNonNull(source, "source");
        this.sourceName = sourceName == null ? "<memory>" : sourceName;
    }

    List<Token> lex() {
        offset = 0;
        line = 1;
        column = 1;

        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipIgnored();
            if (atEnd()) {
                tokens.add(new Token(
                        TokenKind.EOF,
                        "",
                        sourceName,
                        offset,
                        offset,
                        line,
                        column,
                        line,
                        column
                ));
                return List.copyOf(tokens);
            }

            int startOffset = offset;
            int startLine = line;
            int startColumn = column;
            char current = peek();

            if (isIdentifierStart(current)) {
                lexIdentifier();
                tokens.add(token(TokenKind.IDENTIFIER, startOffset, startLine, startColumn));
                continue;
            }
            if (isDigit(current)) {
                lexNumber();
                tokens.add(token(TokenKind.NUMBER, startOffset, startLine, startColumn));
                continue;
            }
            if (current == '"') {
                lexQuotedString(startOffset, startLine, startColumn);
                tokens.add(token(TokenKind.STRING, startOffset, startLine, startColumn));
                continue;
            }
            if (current == '\'') {
                TokenKind kind = lexBinaryOrHexString(startOffset, startLine, startColumn);
                tokens.add(token(kind, startOffset, startLine, startColumn));
                continue;
            }

            TokenKind kind = lexOperatorOrPunctuation(startOffset, startLine, startColumn);
            tokens.add(token(kind, startOffset, startLine, startColumn));
        }
    }

    private void skipIgnored() {
        boolean consumed;
        do {
            consumed = false;

            while (!atEnd() && isWhitespace(peek())) {
                advance();
                consumed = true;
            }

            if (startsWith("--")) {
                skipLineComment();
                consumed = true;
            } else if (startsWith("/*")) {
                skipBlockComment();
                consumed = true;
            }
        } while (consumed);
    }

    private void skipLineComment() {
        advance();
        advance();
        while (!atEnd() && peek() != '\r' && peek() != '\n') {
            advance();
        }
    }

    private void skipBlockComment() {
        int startOffset = offset;
        int startLine = line;
        int startColumn = column;

        advance();
        advance();
        while (!atEnd()) {
            if (startsWith("*/")) {
                advance();
                advance();
                return;
            }
            advance();
        }

        throw error("Unterminated block comment", startOffset, startLine, startColumn);
    }

    private void lexIdentifier() {
        advance();
        while (!atEnd()) {
            char current = peek();
            if (isIdentifierPart(current)) {
                advance();
                continue;
            }
            if (current == '-'
                    && peek(1) != '-'
                    && isIdentifierPart(peek(1))) {
                advance();
                continue;
            }
            return;
        }
    }

    private void lexNumber() {
        do {
            advance();
        } while (!atEnd() && isDigit(peek()));
    }

    private void lexQuotedString(int startOffset, int startLine, int startColumn) {
        advance();
        while (!atEnd()) {
            if (peek() != '"') {
                advance();
                continue;
            }

            advance();
            if (!atEnd() && peek() == '"') {
                advance();
                continue;
            }
            return;
        }

        throw error("Unterminated quoted string", startOffset, startLine, startColumn);
    }

    private TokenKind lexBinaryOrHexString(int startOffset, int startLine, int startColumn) {
        advance();
        int contentOffset = offset;
        int contentLine = line;
        int contentColumn = column;
        while (!atEnd() && peek() != '\'') {
            advance();
        }
        if (atEnd()) {
            throw error("Unterminated binary or hexadecimal string", startOffset, startLine, startColumn);
        }

        int contentEndOffset = offset;
        advance();
        if (atEnd()) {
            throw error("Expected B or H suffix after apostrophe string", startOffset, startLine, startColumn);
        }

        char suffix = peek();
        if (suffix == 'B' || suffix == 'b') {
            validateRadixString(
                    TokenKind.BIT_STRING,
                    contentOffset,
                    contentEndOffset,
                    contentLine,
                    contentColumn
            );
            advance();
            return TokenKind.BIT_STRING;
        }
        if (suffix == 'H' || suffix == 'h') {
            validateRadixString(
                    TokenKind.HEX_STRING,
                    contentOffset,
                    contentEndOffset,
                    contentLine,
                    contentColumn
            );
            advance();
            return TokenKind.HEX_STRING;
        }
        throw error("Expected B or H suffix after apostrophe string", offset, line, column);
    }

    private void validateRadixString(
            TokenKind kind,
            int contentOffset,
            int contentEndOffset,
            int contentLine,
            int contentColumn
    ) {
        int checkedOffset = contentOffset;
        int checkedLine = contentLine;
        int checkedColumn = contentColumn;

        while (checkedOffset < contentEndOffset) {
            char value = source.charAt(checkedOffset);
            boolean valid = isWhitespace(value)
                    || kind == TokenKind.BIT_STRING && (value == '0' || value == '1')
                    || kind == TokenKind.HEX_STRING && isHexDigit(value);
            if (!valid) {
                String literalKind = kind == TokenKind.BIT_STRING ? "bit" : "hexadecimal";
                throw error(
                        "Invalid character " + printable(value) + " in " + literalKind + " string",
                        checkedOffset,
                        checkedLine,
                        checkedColumn
                );
            }

            checkedOffset++;
            if (value == '\r') {
                if (checkedOffset < contentEndOffset && source.charAt(checkedOffset) == '\n') {
                    checkedOffset++;
                }
                checkedLine++;
                checkedColumn = 1;
            } else if (value == '\n') {
                checkedLine++;
                checkedColumn = 1;
            } else {
                checkedColumn++;
            }
        }
    }

    private TokenKind lexOperatorOrPunctuation(int startOffset, int startLine, int startColumn) {
        if (startsWith("::=")) {
            advance();
            advance();
            advance();
            return TokenKind.ASSIGN;
        }
        if (startsWith("...")) {
            advance();
            advance();
            advance();
            return TokenKind.ELLIPSIS;
        }
        if (startsWith("..")) {
            advance();
            advance();
            return TokenKind.RANGE;
        }

        char current = peek();
        TokenKind kind = switch (current) {
            case '{' -> TokenKind.LBRACE;
            case '}' -> TokenKind.RBRACE;
            case '(' -> TokenKind.LPAREN;
            case ')' -> TokenKind.RPAREN;
            case '[' -> TokenKind.LBRACKET;
            case ']' -> TokenKind.RBRACKET;
            case ',' -> TokenKind.COMMA;
            case ';' -> TokenKind.SEMICOLON;
            case '|' -> TokenKind.PIPE;
            case '&' -> TokenKind.AMPERSAND;
            case '.' -> TokenKind.DOT;
            case '@' -> TokenKind.AT;
            case ':' -> TokenKind.COLON;
            case '+' -> TokenKind.PLUS;
            case '-' -> TokenKind.MINUS;
            case '*' -> TokenKind.STAR;
            case '/' -> TokenKind.SLASH;
            case '!' -> TokenKind.EXCLAMATION;
            case '<' -> TokenKind.LESS_THAN;
            case '>' -> TokenKind.GREATER_THAN;
            case '^' -> TokenKind.CARET;
            default -> throw error(
                    "Unexpected character " + printable(current),
                    startOffset,
                    startLine,
                    startColumn
            );
        };
        advance();
        return kind;
    }

    private Token token(TokenKind kind, int startOffset, int startLine, int startColumn) {
        return new Token(
                kind,
                source.substring(startOffset, offset),
                sourceName,
                startOffset,
                offset,
                startLine,
                startColumn,
                line,
                column
        );
    }

    private Asn1ParseException error(String message, int errorOffset, int errorLine, int errorColumn) {
        return new Asn1ParseException(message, sourceName, errorOffset, errorLine, errorColumn);
    }

    private boolean atEnd() {
        return offset >= source.length();
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int lookahead) {
        int position = offset + lookahead;
        return position >= source.length() ? '\0' : source.charAt(position);
    }

    private boolean startsWith(String expected) {
        return source.startsWith(expected, offset);
    }

    private void advance() {
        char current = source.charAt(offset++);
        if (current == '\r') {
            if (!atEnd() && source.charAt(offset) == '\n') {
                offset++;
            }
            line++;
            column = 1;
        } else if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    private static boolean isWhitespace(char value) {
        return Character.isWhitespace(value) || value == '\ufeff';
    }

    private static boolean isIdentifierStart(char value) {
        return isAsciiLetter(value);
    }

    private static boolean isIdentifierPart(char value) {
        return isAsciiLetter(value) || isDigit(value);
    }

    private static boolean isAsciiLetter(char value) {
        return value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z';
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isHexDigit(char value) {
        return isDigit(value)
                || value >= 'A' && value <= 'F'
                || value >= 'a' && value <= 'f';
    }

    private static String printable(char value) {
        return switch (value) {
            case '\r' -> "'\\r'";
            case '\n' -> "'\\n'";
            case '\t' -> "'\\t'";
            default -> "'" + value + "'";
        };
    }
}
