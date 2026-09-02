package com.ancevt.asn1.model;

import java.util.Objects;

public record Diagnostic(Severity severity, String message, SourceRange sourceRange) {
    public Diagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }

    public enum Severity { INFO, WARNING, ERROR }
}
