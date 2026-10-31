package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException;

/**
 * Regla del modelo incumplida que el schema STXT no puede expresar (reference/common/errors.md),
 * con su código y la línea del documento donde está el nodo culpable. Si el nodo está en el
 * documento de estilos aparte ({@code Styles}, reference/common/styles.md), la línea es la de ese documento, el
 * mensaje lo dice y {@link #inStyles()} es {@code true}. Su {@link #kind()} es el del código ({@link
 * ErrorCode#kind()}): casi siempre un documento no válido, pero un límite superado ({@link ErrorCode#PAGE_LIMIT}) es un
 * {@link Kind#LIMIT}.
 */
public class InvalidModelException extends StxtPdfException {

    private final ErrorCode errorCode;
    private final String detail;

    /** Un error de una línea del documento principal. */
    public InvalidModelException(int line, ErrorCode code, String message) {
        this(SourceLine.of(line), code, message);
    }

    /** Un error de una línea del documento principal o del de estilos. */
    public InvalidModelException(SourceLine line, ErrorCode code, String message) {
        super(code.kind(), line.number(), line.inStyles(), code.name(), where(line) + message, null);
        this.errorCode = code;
        this.detail = message;
    }

    private static String where(SourceLine line) {
        return line.number() == NO_LINE ? "" : line + ": ";
    }

    /** El mismo error, con su línea referida al documento de estilos aparte. */
    public InvalidModelException inStylesDocument() {
        return new InvalidModelException(SourceLine.inStyles(line()), errorCode, detail);
    }

    /** El código como valor del enum, para compararlo; {@link #code()} lo da en texto. */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
