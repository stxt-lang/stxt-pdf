package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException;

/**
 * Broken rule of the model that the STXT schema cannot express (reference/common/errors.md),
 * with its code and the line of the document where the offending node is. If the node is in a
 * separate styles document ({@code Styles}, reference/common/styles.md), the line is that of that document, the
 * message says so, {@link #inStyles()} is {@code true} and {@link #stylesDocument()} says which one it is. Its
 * {@link #kind()} is that of the code ({@link
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

    /** An error on a line of the main document or of a styles document. */
    public InvalidModelException(SourceLine line, ErrorCode code, String message) {
        super(code.kind(), line.number(), line.styles(), code.name(), where(line) + message, null);
        this.errorCode = code;
        this.detail = message;
    }

    private static String where(SourceLine line) {
        return line.number() == NO_LINE ? "" : line + ": ";
    }

    /** The same error, with its line referring to the separate styles document, the only one. */
    public InvalidModelException inStylesDocument() {
        return in(SourceLine.inStyles(NO_LINE));
    }

    /** The same error, with its line referring to the document of {@code source}, whose line number does not count. */
    public InvalidModelException in(SourceLine source) {
        return new InvalidModelException(source.at(line()), errorCode, detail);
    }

    /** {@return the code as an enum value, to compare it; {@link #code()} gives it as text} */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
