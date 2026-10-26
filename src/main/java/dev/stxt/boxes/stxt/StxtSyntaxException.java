package dev.stxt.boxes.stxt;

/** Error de sintaxis STXT con origen y línea. */
public class StxtSyntaxException extends RuntimeException {

    private final String source;
    private final int line;

    public StxtSyntaxException(String source, int line, String message) {
        super(source + ":" + line + ": " + message);
        this.source = source;
        this.line = line;
    }

    public String source() {
        return source;
    }

    public int line() {
        return line;
    }
}
