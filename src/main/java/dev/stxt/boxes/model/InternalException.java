package dev.stxt.boxes.model;

/**
 * Un fallo del motor, no del documento ni del entorno: una garantía interna que no se cumple, o un error
 * inesperado de una librería. {@link #getCause()} guarda el original cuando lo hay.
 */
public class InternalException extends StxtPdfException {

    public static final String CODE = "INTERNAL";

    public InternalException(String message) {
        this(message, null);
    }

    public InternalException(String message, Throwable cause) {
        super(Kind.INTERNAL, NO_LINE, CODE, message, cause);
    }
}
