package dev.stxt.pdf.model;

/**
 * A failure of the engine, not of the document or the environment: an internal guarantee that does not hold, or an
 * unexpected error of a library. {@link #getCause()} keeps the original when there is one.
 */
public class InternalException extends StxtPdfException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "INTERNAL";

    public InternalException(String message) {
        this(message, null);
    }

    public InternalException(String message, Throwable cause) {
        super(Kind.INTERNAL, NO_LINE, CODE, message, cause);
    }
}
