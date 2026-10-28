package dev.stxt.boxes.tree;

/**
 * Regla del modelo incumplida que el schema STXT no puede expresar (ESPECIFICACION.md §6),
 * con su código y la línea del documento donde está el nodo culpable. Si el nodo está en el
 * documento de estilos aparte ({@code Styles}, §4.10), la línea es la de ese documento y el
 * mensaje lo dice.
 */
public class InvalidModelException extends RuntimeException {

    private final int line;
    private final ErrorCode code;
    private final String detail;

    public InvalidModelException(int line, ErrorCode code, String message) {
        this(line, code, message, null);
    }

    /** {@code source} es el documento donde está la línea ({@code los estilos}); {@code null}, el principal. */
    public InvalidModelException(int line, ErrorCode code, String message, String source) {
        super("línea " + line + (source == null ? "" : " de " + source) + ": " + message);
        this.line = line;
        this.code = code;
        this.detail = message;
    }

    /** El mismo error, con su línea referida a otro documento. */
    public InvalidModelException inSource(String source) {
        return new InvalidModelException(line, code, detail, source);
    }

    public int line() {
        return line;
    }

    public ErrorCode code() {
        return code;
    }
}
