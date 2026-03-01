package com.ancevt.asn.parse;

import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {

    private final String input;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    private final String[] lines;

    public Tokenizer(String input) {
        this.input = input;
        this.lines = input.split("\n");
    }

    private AsnParseException error(String message) {

        StringBuilder sb = new StringBuilder();

        sb.append(message)
                .append(" at line ")
                .append(line)
                .append(", column ")
                .append(column)
                .append("\n");

        if (line - 1 < lines.length) {
            String lineText = lines[line - 1];
            sb.append(lineText).append("\n");

            // правильно считаем отступ с учетом табов
            int visualColumn = 0;

            for (int i = 0; i < column - 1 && i < lineText.length(); i++) {
                if (lineText.charAt(i) == '\t') {
                    visualColumn += 4; // или 8, если хочешь
                } else {
                    visualColumn += 1;
                }
            }

            //sb.append(" ".repeat(Math.max(0, visualColumn))).append("^\n");
        }

        return new AsnParseException(sb.toString());
    }

    private void advance() {
        if (pos >= input.length()) {
            return;
        }

        if (input.charAt(pos) == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }

        pos++;
    }

    public List<Token> tokenize() {

        List<Token> tokens = new ArrayList<>();

        while (true) {

            skipWhitespaceAndComments();

            if (pos >= input.length()) {
                tokens.add(new Token(TokenType.EOF, "", line, column));
                return tokens;
            }

            char c = input.charAt(pos);

            switch (c) {

                // -------------------------------------------------
                // DOT / RANGE / ELLIPSIS
                // -------------------------------------------------
                case '.' -> {

                    int startLine = line;
                    int startColumn = column;

                    if (peek("...")) {
                        advance();
                        advance();
                        advance();
                        tokens.add(new Token(TokenType.ELLIPSIS, "...", startLine, startColumn));
                    } else if (peek("..")) {
                        advance();
                        advance();
                        tokens.add(new Token(TokenType.RANGE, "..", startLine, startColumn));
                    } else {
                        advance();
                        tokens.add(new Token(TokenType.DOT, ".", startLine, startColumn));
                    }
                }

                // -------------------------------------------------
                // Braces
                // -------------------------------------------------
                case '{' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.LBRACE, "{", startLine, startColumn));
                }

                case '@' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.AT, "@", startLine, startColumn));
                }

                case '}' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.RBRACE, "}", startLine, startColumn));
                }

                case '[' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.LBRACKET, "[", startLine, startColumn));
                }

                case ']' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.RBRACKET, "]", startLine, startColumn));
                }

                case '(' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.LPAREN, "(", startLine, startColumn));
                }

                case ')' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.RPAREN, ")", startLine, startColumn));
                }

                case ',' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.COMMA, ",", startLine, startColumn));
                }

                case '|' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.PIPE, "|", startLine, startColumn));
                }

                case '&' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.AMP, "&", startLine, startColumn));
                }

                case ';' -> {
                    int startLine = line;
                    int startColumn = column;
                    advance();
                    tokens.add(new Token(TokenType.SEMICOLON, ";", startLine, startColumn));
                }

                // -------------------------------------------------
                // ASSIGN ::= or COLON
                // -------------------------------------------------
                case ':' -> {

                    int startLine = line;
                    int startColumn = column;

                    if (peek("::=")) {
                        advance();
                        advance();
                        advance();
                        tokens.add(new Token(TokenType.ASSIGN, "::=", startLine, startColumn));
                    } else {
                        advance();
                        tokens.add(new Token(TokenType.COLON, ":", startLine, startColumn));
                    }
                }

                // -------------------------------------------------
                // IDENTIFIER / NUMBER
                // -------------------------------------------------
                default -> {

                    if (Character.isLetter(c) || c == '-') {
                        tokens.add(readIdentifier());
                    } else if (Character.isDigit(c)) {
                        tokens.add(readNumber());
                    } else {
                        throw error("Unexpected char '" + c + "'");
                    }
                }
            }
        }
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (Character.isWhitespace(c)) {
                advance();
                continue;
            }

            if (peek("--")) {
                advance();
                advance();
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    advance();
                }
                continue;
            }

            break;
        }
    }

    private boolean peek(String s) {
        return input.startsWith(s, pos);
    }

    private Token readIdentifier() {

        int startLine = line;
        int startColumn = column;
        int start = pos;

        while (pos < input.length() &&
                (Character.isLetterOrDigit(input.charAt(pos)) ||
                        input.charAt(pos) == '-' ||
                        input.charAt(pos) == '_')) {
            advance();
        }

        String text = input.substring(start, pos);

        return new Token(
                switch (text) {
                    case "CLASS",
                         "WITH",
                         "SYNTAX",
                         "UNIQUE",
                         "DEFAULT",
                         "IMPORTS",
                         "FROM",
                         "DEFINITIONS",
                         "BEGIN",
                         "END",
                         "ENUMERATED",
                         "SEQUENCE",
                         "OF",
                         "CHOICE",
                         "INTEGER",
                         "BOOLEAN",
                         "OCTET",
                         "BIT",
                         "STRING",
                         "UTF8String",
                         "OPTIONAL",
                         "AUTOMATIC",
                         "EXPLICIT",
                         "IMPLICIT",
                         "TAGS",
                         "OBJECT",
                         "IDENTIFIER",
                         "SIZE",
                         "NULL",
                         "PrintableString",
                         "IA5String",
                         "VisibleString",
                         "NumericString",
                         "BMPString",
                         "GeneralizedTime",
                         "UTCTime",
                         "REAL" -> TokenType.KEYWORD;
                    default -> TokenType.IDENTIFIER;
                },
                text,
                startLine,
                startColumn
        );
    }

    private Token readNumber() {

        int startLine = line;
        int startColumn = column;
        int start = pos;

        while (pos < input.length() &&
                Character.isDigit(input.charAt(pos))) {
            advance();
        }

        String text = input.substring(start, pos);

        return new Token(
                TokenType.NUMBER,
                text,
                startLine,
                startColumn
        );
    }
}