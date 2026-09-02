package com.ancevt.asn1.model;

/** Exact half-open location of a model element in the input text. */
public record SourceRange(
        String sourceName,
        int startOffset,
        int endOffset,
        int startLine,
        int startColumn,
        int endLine,
        int endColumn
) {
    public SourceRange {
        if (sourceName == null) sourceName = "<memory>";
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Invalid source offsets");
        }
    }

    public String textFrom(String sourceText) {
        return sourceText.substring(startOffset, Math.min(endOffset, sourceText.length()));
    }

    @Override
    public String toString() {
        return sourceName + ":" + startLine + ":" + startColumn;
    }
}
