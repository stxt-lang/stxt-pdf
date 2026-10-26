package dev.stxt.boxes.tree;

/**
 * Regla del modelo incumplida que el template STXT no puede expresar (ESPECIFICACION.md §6),
 * con su código y la línea del documento donde está el nodo culpable.
 */
public class InvalidModelException extends RuntimeException {

    private final int line;
    private final ErrorCode code;

    public InvalidModelException(int line, ErrorCode code, String message) {
        super("línea " + line + ": " + message);
        this.line = line;
        this.code = code;
    }

    public int line() {
        return line;
    }

    public ErrorCode code() {
        return code;
    }
}
