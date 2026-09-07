package tel.core.s1ap.core.error;

public class S1apException extends RuntimeException {
    public S1apException(String message) {
        super(message);
    }

    public S1apException(String message, Throwable cause) {
        super(message, cause);
    }

    public S1apException(Throwable cause) {
        super(cause);
    }

    public S1apException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
