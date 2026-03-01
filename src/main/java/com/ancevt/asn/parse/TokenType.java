package com.ancevt.asn.parse;

public enum TokenType {
    IDENTIFIER,
    NUMBER,
    DOT,
    RANGE, // ..
    ELLIPSIS, // ...
    LBRACE,     // {
    RBRACE,     // }
    LPAREN,     // (
    RPAREN,     // )
    AMP,        // &
    LBRACKET,  // [
    RBRACKET,  // ]
    AT,          // @
    SEMICOLON,
    COMMA,
    PIPE,
    COLON,
    ASSIGN,     // ::=
    KEYWORD,
    EOF
}