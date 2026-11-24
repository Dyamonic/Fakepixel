package club.abelix.network.pipeline.exception;

/**
 * @author WhileInside
 */
public class InstantException extends Exception {

    public InstantException() {
    }

    public InstantException(String message) {
        super(message);
    }

    public InstantException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstantException(Throwable cause) {
        super(cause);
    }

    @Override
    public Throwable initCause(Throwable cause) {
        return this;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
