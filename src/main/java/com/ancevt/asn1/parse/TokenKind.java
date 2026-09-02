package com.ancevt.asn1.parse;

enum TokenKind {
    IDENTIFIER,
    NUMBER,
    STRING,
    BIT_STRING,
    HEX_STRING,

    ASSIGN,
    ELLIPSIS,
    RANGE,

    LBRACE,
    RBRACE,
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    COMMA,
    SEMICOLON,
    PIPE,
    AMPERSAND,
    DOT,
    AT,
    COLON,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    EXCLAMATION,
    LESS_THAN,
    GREATER_THAN,
    CARET,

    EOF
}
