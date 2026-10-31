package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException;

/**
 * Broken rule of the model that the STXT schema cannot express (reference/common/errors.md),
 * with its code and the line of the document where the offending node is. If the node is in the
 * separate styles document ({@code Styles}, reference/common/styles.md), the line is that of that document, the
 * message says so and {@link #inStyles()} is {@code true}. Its {@link #kind()} is that of the code ({@link
 * ErrorCode#kind()}): almost always an invalid document, but an exceeded limit ({@link ErrorCode#PAGE_LIMIT}) is a
 * {@link Kind#LIMIT}.
 */
public class InvalidModelException extends StxtPdfException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String detail;

    /** An error on a line of the main document. */
    public InvalidModelException(int line, ErrorCode code, String message) {
        this(SourceLine.of(line), code, message);
    }

    /** An error on a line of the main document or of the styles document. */
    public InvalidModelException(SourceLine line, ErrorCode code, String message) {
        super(code.kind(), line.number(), line.inStyles(), code.name(), where(line) + message, null);
        this.errorCode = code;
        this.detail = message;
    }

    private static String where(SourceLine line) {
        return line.number() == NO_LINE ? "" : line + ": ";
    }

    /** The same error, with its line referring to the separate styles document. */
    public InvalidModelException inStylesDocument() {
        return new InvalidModelException(SourceLine.inStyles(line()), errorCode, detail);
    }

    /** The code as an enum value, to compare it; {@link #code()} gives it as text. */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
