package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.exceptions.LimitException;
import dev.stxt.exceptions.ParseException;

/**
 * Error de sintaxis STXT o del schema que detecta la librería {@code stxt-core} (referencia/comun/errores.md, nivel 1),
 * con el código de la librería ({@code CHILD_NOT_DECLARED}, {@code INVALID_VALUE}…) y su línea. Es el mismo error
 * de la librería sin su tipo: {@link #getCause()} guarda el original. Un {@link LimitException} de la librería
 * (documento demasiado anidado o con líneas demasiado largas) es un {@link Kind#LIMIT}; el resto, un documento
 * no válido. Si viene del documento de estilos aparte, el mensaje lo dice y {@link #inStyles()} es {@code true}.
 */
public class DocumentSyntaxException extends StxtPdfException {

    public DocumentSyntaxException(ParseException cause) {
        this(cause, false);
    }

    /** {@code inStyles}: el error es del documento de estilos aparte. */
    public DocumentSyntaxException(ParseException cause, boolean inStyles) {
        super(cause instanceof LimitException ? Kind.LIMIT : Kind.INVALID_DOCUMENT, cause.getLine(), inStyles,
                cause.getCode(), message(cause, inStyles), cause);
    }

    private static String message(ParseException cause, boolean inStyles) {
        String message = cause.getMessage();
        return cause.getLine() == NO_LINE ? message : new SourceLine(cause.getLine(), inStyles) + ": " + message;
    }
}
