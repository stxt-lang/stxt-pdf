package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.exceptions.LimitException;
import dev.stxt.exceptions.ParseException;

/**
 * STXT syntax or schema error detected by the {@code stxt-core} library (reference/common/errors.md, level 1),
 * with the code of the library ({@code CHILD_NOT_DECLARED}, {@code INVALID_VALUE}...) and its line. It is the same
 * error of the library without its type: {@link #getCause()} keeps the original. A {@link LimitException} of the
 * library (document nested too deeply or with lines that are too long) is a {@link Kind#LIMIT}; the rest, an invalid
 * document. If it comes from a separate styles document, the message says so, {@link #inStyles()} is {@code true} and
 * {@link #stylesDocument()} says which one it is.
 */
public class DocumentSyntaxException extends StxtPdfException {

    private static final long serialVersionUID = 1L;

    public DocumentSyntaxException(ParseException cause) {
        this(cause, false);
    }

    /** {@code inStyles}: the error belongs to the separate styles document, the only one. */
    public DocumentSyntaxException(ParseException cause, boolean inStyles) {
        this(cause, inStyles ? SourceLine.inStyles(NO_LINE) : SourceLine.of(NO_LINE));
    }

    /** The error belongs to the document of {@code source}, whose line number does not count. */
    public DocumentSyntaxException(ParseException cause, SourceLine source) {
        super(cause instanceof LimitException ? Kind.LIMIT : Kind.INVALID_DOCUMENT, cause.getLine(), source.styles(),
                cause.getCode(), message(cause, source), cause);
    }

    private static String message(ParseException cause, SourceLine source) {
        String message = cause.getMessage();
        return cause.getLine() == NO_LINE ? message : source.at(cause.getLine()) + ": " + message;
    }
}
