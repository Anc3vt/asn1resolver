package com.ancevt.asn.parse;

public record Token(
        TokenType type,
        String text,
        int line,
        int column
) {}