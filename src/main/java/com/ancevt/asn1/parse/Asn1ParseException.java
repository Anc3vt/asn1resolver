package com.ancevt.asn1.parse;

import java.util.Objects;

/** Reports a lexical or syntactic error at an exact source position. */
public final class Asn1ParseException extends RuntimeException {
    private final String sourceName;
    private final int offset;
    private final int line;
    private final int column;

    public Asn1ParseException(
            String message,
            String sourceName,
            int offset,
            int line,
            int column
    ) {
        this(message, sourceName, offset, line, column, null);
    }

    public Asn1ParseException(
            String message,
            String sourceName,
            int offset,
            int line,
            int column,
            Throwable cause
    ) {
        super(formatMessage(message, sourceName, line, column), cause);
        this.sourceName = sourceName == null ? "<memory>" : sourceName;
        this.offset = offset;
        this.line = line;
        this.column = column;

        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("line and column must be positive");
        }
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getOffset() {
        return offset;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    private static String formatMessage(String message, String sourceName, int line, int column) {
        Objects.requireNonNull(message, "message");
        String actualSourceName = sourceName == null ? "<memory>" : sourceName;
        return actualSourceName + ':' + line + ':' + column + ": " + message;
    }
}
