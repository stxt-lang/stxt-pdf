package dev.stxt.pdf.model;

/**
 * Root of all the errors the engine gives to its user (reference/common/errors.md): a single
 * {@code catch (StxtPdfException)} is enough to handle any failure of a conversion, and {@link #kind()} says
 * whose fault it is, which is what a service needs to answer (a 4xx or a 5xx).
 *
 * <ul>
 *   <li>{@link Kind#INVALID_DOCUMENT}: the document or its styles are not valid (syntax, schema, a rule
 *       of the model, an overflow, a resource the document names that does not exist). Fixing it is up to
 *       the author, and it almost always carries {@link #line()}.</li>
 *   <li>{@link Kind#RESOURCE}: the resources folder is not usable (it does not exist, a font or an image is
 *       damaged) or the file cannot be read. It belongs to the environment of the caller.</li>
 *   <li>{@link Kind#LIMIT}: the document is valid but asks for more than is allowed (too many pages...).</li>
 *   <li>{@link Kind#INTERNAL}: a failure of the engine, not of the document or the environment. It is reported as a
 *       defect; {@link #getCause()} keeps the original.</li>
 * </ul>
 *
 * <p>Input and output errors when writing to the destination are not part of this family: they are the
 * {@link java.io.IOException} that the writing methods declare.</p>
 */
public abstract class StxtPdfException extends RuntimeException {

    /** Whose fault an error is. */
    public enum Kind {
        INVALID_DOCUMENT,
        RESOURCE,
        LIMIT,
        INTERNAL
    }

    /** The value of {@link #line()} when the error does not belong to any line of the document. */
    public static final int NO_LINE = 0;

    private final Kind kind;
    private final int line;
    private final boolean inStyles;
    private final String code;

    protected StxtPdfException(Kind kind, int line, String code, String message, Throwable cause) {
        this(kind, line, false, code, message, cause);
    }

    /** {@code inStyles}: the line belongs to the separate styles document, not to the document. */
    protected StxtPdfException(Kind kind, int line, boolean inStyles, String code, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.line = line;
        this.inStyles = inStyles;
        this.code = code;
    }

    public Kind kind() {
        return kind;
    }

    /**
     * The line of the offending node, or {@link #NO_LINE} if the error does not belong to a line. It is a line of the
     * document, or of the separate styles document if {@link #inStyles()}.
     */
    public int line() {
        return line;
    }

    /** Whether {@link #line()} is a line of the separate styles document (reference/common/styles.md). */
    public boolean inStyles() {
        return inStyles;
    }

    /**
     * Stable code of the error, as text: that of {@code ErrorCode} ({@code BOX_OVERFLOW}), that of the
     * {@code stxt-core} library ({@code CHILD_NOT_DECLARED}), {@code RESOURCE}, {@code IMAGE_FORMAT} or
     * {@code INTERNAL}.
     */
    public String code() {
        return code;
    }
}
