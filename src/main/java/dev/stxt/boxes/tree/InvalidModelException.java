package dev.stxt.boxes.tree;

/**
 * Regla del modelo incumplida que el template STXT no puede expresar (ESPECIFICACION.md §6),
 * con la línea del documento donde se produce.
 */
public class InvalidModelException extends RuntimeException {

    private final int line;

    public InvalidModelException(int line, String message) {
        super("línea " + line + ": " + message);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
