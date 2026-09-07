package com.ancevt.asn1.generate.s1ap.diagnostic;

import com.ancevt.asn1.model.SourceRange;
import java.util.List;

/** A recoverable, stable diagnostic at a generator pipeline boundary. */
public final class GenerationException extends RuntimeException {
    private final String code;
    private final int exitCode;
    private final SourceRange range;
    private final List<SourceRange> related;

    public GenerationException(String code, int exitCode, String message) {
        this(code, exitCode, message, null, List.of());
    }

    public GenerationException(String code, int exitCode, String message, SourceRange range,
                               List<SourceRange> related) {
        super(message);
        this.code = code;
        this.exitCode = exitCode;
        this.range = range;
        this.related = List.copyOf(related);
    }

    public String code() { return code; }
    public int exitCode() { return exitCode; }
    public SourceRange range() { return range; }
    public List<SourceRange> related() { return related; }
}
