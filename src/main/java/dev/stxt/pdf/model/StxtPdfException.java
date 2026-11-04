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
 *   <li>{@link Kind#CANCELLED}: the caller asked to stop before the end ({@link Cancellation}: a deadline that
 *       passed, a thread that was interrupted). Neither the document's fault nor the environment's, and nothing
 *       has been written.</li>
 * </ul>
 *
 * <p>Input and output errors when writing to the destination are not part of this family: they are the
 * {@link java.io.IOException} that the writing methods declare.</p>
 */
public abstract class StxtPdfException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Whose fault an error is. */
    public enum Kind {
        /** The document or its styles are not valid; fixing it is up to the author. */
        INVALID_DOCUMENT,
        /** The resources folder is not usable or a file cannot be read; it belongs to the environment of the caller. */
        RESOURCE,
        /** The document is valid but asks for more than the {@link Limits} allow. */
        LIMIT,
        /** A failure of the engine, not of the document or the environment; it is reported as a defect. */
        INTERNAL,
        /** The caller asked to stop ({@link Cancellation}) before the conversion finished. */
        CANCELLED
    }

    /** The value of {@link #line()} when the error does not belong to any line of the document. */
    public static final int NO_LINE = 0;

    /** Whose fault the error is. */
    private final Kind kind;
    /** The line of the offending node, or {@link #NO_LINE}. */
    private final int line;
    /** Whether {@link #line} belongs to the separate styles document. */
    private final boolean inStyles;
    /** The stable code of the error. */
    private final String code;

    /**
     * An error whose line, if it has one, belongs to the document.
     *
     * @param kind whose fault the error is
     * @param line the line of the offending node, or {@link #NO_LINE}
     * @param code the stable code of the error, in capitals ({@code BOX_OVERFLOW})
     * @param message the message, which names the line itself if there is one
     * @param cause the original error, or {@code null}
     */
    protected StxtPdfException(Kind kind, int line, String code, String message, Throwable cause) {
        this(kind, line, false, code, message, cause);
    }

    /**
     * {@code inStyles}: the line belongs to the separate styles document, not to the document.
     *
     * @param kind whose fault the error is
     * @param line the line of the offending node, or {@link #NO_LINE}
     * @param inStyles {@code true} if {@code line} is a line of the separate styles document
     * @param code the stable code of the error, in capitals ({@code BOX_OVERFLOW})
     * @param message the message, which names the line itself if there is one
     * @param cause the original error, or {@code null}
     */
    protected StxtPdfException(Kind kind, int line, boolean inStyles, String code, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.line = line;
        this.inStyles = inStyles;
        this.code = code;
    }

    /** {@return whose fault the error is: the document, the resources, a limit, the engine or a cancellation} */
    public Kind kind() {
        return kind;
    }

    /**
     * {@return the line of the offending node, or {@link #NO_LINE} if the error does not belong to a line} It is a
     * line of the document, or of the separate styles document if {@link #inStyles()}.
     */
    public int line() {
        return line;
    }

    /** {@return whether {@link #line()} is a line of the separate styles document (reference/common/styles.md)} */
    public boolean inStyles() {
        return inStyles;
    }

    /**
     * {@return the stable code of the error, as text} That of {@code ErrorCode} ({@code BOX_OVERFLOW}), that of the
     * {@code stxt-core} library ({@code CHILD_NOT_DECLARED}), {@code RESOURCE}, {@code IMAGE_FORMAT},
     * {@code CANCELLED} or {@code INTERNAL}.
     */
    public String code() {
        return code;
    }
}
