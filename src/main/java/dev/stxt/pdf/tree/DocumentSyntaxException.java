package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.exceptions.LimitException;
import dev.stxt.exceptions.ParseException;

/**
 * STXT syntax or schema error detected by the {@code stxt-core} library (reference/common/errors.md, level 1),
 * with the code of the library ({@code CHILD_NOT_DECLARED}, {@code INVALID_VALUE}...) and its line. It is the same
 * error of the library without its type: {@link #getCause()} keeps the original. A {@link LimitException} of the
 * library (document nested too deeply or with lines that are too long) is a {@link Kind#LIMIT}; the rest, an invalid
 * document. If it comes from the separate styles document, the message says so and {@link #inStyles()} is
 * {@code true}.
 */
public class DocumentSyntaxException extends StxtPdfException {

    public DocumentSyntaxException(ParseException cause) {
        this(cause, false);
    }

    /** {@code inStyles}: the error belongs to the separate styles document. */
    public DocumentSyntaxException(ParseException cause, boolean inStyles) {
        super(cause instanceof LimitException ? Kind.LIMIT : Kind.INVALID_DOCUMENT, cause.getLine(), inStyles,
                cause.getCode(), message(cause, inStyles), cause);
    }

    private static String message(ParseException cause, boolean inStyles) {
        String message = cause.getMessage();
        return cause.getLine() == NO_LINE ? message : new SourceLine(cause.getLine(), inStyles) + ": " + message;
    }
}
