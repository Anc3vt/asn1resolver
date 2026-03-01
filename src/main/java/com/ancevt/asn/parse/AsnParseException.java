package com.ancevt.asn.parse;

public class AsnParseException extends RuntimeException {
    public AsnParseException(String message) {
        super(message);
    }

    public AsnParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsnParseException(Throwable cause) {
        super(cause);
    }

    public AsnParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public AsnParseException() {
    }
}
