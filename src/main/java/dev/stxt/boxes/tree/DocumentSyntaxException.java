package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.StxtPdfException;
import dev.stxt.exceptions.LimitException;
import dev.stxt.exceptions.ParseException;

/**
 * Error de sintaxis STXT o del schema que detecta la librería {@code stxt-core} (ESPECIFICACION.md §6, nivel 1),
 * con el código de la librería ({@code CHILD_NOT_DECLARED}, {@code INVALID_VALUE}…) y su línea. Es el mismo error
 * de la librería sin su tipo: {@link #getCause()} guarda el original. Un {@link LimitException} de la librería
 * (documento demasiado anidado o con líneas demasiado largas) es un {@link Kind#LIMIT}; el resto, un documento
 * no válido.
 */
public class DocumentSyntaxException extends StxtPdfException {

    public DocumentSyntaxException(ParseException cause) {
        super(cause instanceof LimitException ? Kind.LIMIT : Kind.INVALID_DOCUMENT, cause.getLine(), cause.getCode(),
                message(cause), cause);
    }

    private static String message(ParseException cause) {
        String message = cause.getMessage();
        return cause.getLine() == NO_LINE ? message : "línea " + cause.getLine() + ": " + message;
    }
}
