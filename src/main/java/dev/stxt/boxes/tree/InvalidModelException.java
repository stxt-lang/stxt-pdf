package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.StxtPdfException;

/**
 * Regla del modelo incumplida que el schema STXT no puede expresar (referencia/comun/errores.md),
 * con su código y la línea del documento donde está el nodo culpable. Si el nodo está en el
 * documento de estilos aparte ({@code Styles}, referencia/comun/estilos.md), la línea es la de ese documento y el
 * mensaje lo dice. Su {@link #kind()} es el del código ({@link ErrorCode#kind()}): casi siempre un
 * documento no válido, pero un límite superado ({@link ErrorCode#PAGE_LIMIT}) es un {@link Kind#LIMIT}.
 */
public class InvalidModelException extends StxtPdfException {

    private final ErrorCode errorCode;
    private final String detail;

    public InvalidModelException(int line, ErrorCode code, String message) {
        this(line, code, message, null);
    }

    /** {@code source} es el documento donde está la línea ({@code los estilos}); {@code null}, el principal. */
    public InvalidModelException(int line, ErrorCode code, String message, String source) {
        super(code.kind(), line, code.name(), where(line, source) + message, null);
        this.errorCode = code;
        this.detail = message;
    }

    private static String where(int line, String source) {
        if (line == NO_LINE) {
            return "";
        }
        return "línea " + line + (source == null ? "" : " de " + source) + ": ";
    }

    /** El mismo error, con su línea referida a otro documento. */
    public InvalidModelException inSource(String source) {
        return new InvalidModelException(line(), errorCode, detail, source);
    }

    /** El código como valor del enum, para compararlo; {@link #code()} lo da en texto. */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
