package dev.stxt.boxes.stxt;

/**
 * Error de sintaxis STXT, o de validación contra el template, con origen y línea. Los errores
 * que detecta la librería {@code stxt-core} conservan su código ({@code INVALID_VALUE},
 * {@code TOO_FEW_CHILDREN}…); los del lector mínimo no tienen código.
 */
public class StxtSyntaxException extends RuntimeException {

    private final String source;
    private final int line;
    private final String code;

    public StxtSyntaxException(String source, int line, String message) {
        this(source, line, null, message);
    }

    public StxtSyntaxException(String source, int line, String code, String message) {
        super(source + ":" + line + ": " + (code != null ? "[" + code + "] " : "") + message);
        this.source = source;
        this.line = line;
        this.code = code;
    }

    public String source() {
        return source;
    }

    public int line() {
        return line;
    }

    /** Código de error de la librería STXT, o {@code null} si el error no viene de ella. */
    public String code() {
        return code;
    }
}
