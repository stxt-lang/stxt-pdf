package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.StxtPdfException;

/**
 * Invalid or missing resource: a path that leaves the resources folder, a
 * file that does not exist, an unknown font... (reference/common/resources.md). Whoever knows the
 * line of the document turns it into a {@link dev.stxt.pdf.tree.InvalidModelException}; one that reaches
 * the caller is a problem of its resources folder ({@link Kind#RESOURCE}), without a line.
 */
public class ResourceException extends StxtPdfException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "RESOURCE";

    public ResourceException(String message) {
        this(CODE, message, null);
    }

    public ResourceException(String message, Throwable cause) {
        this(CODE, message, cause);
    }

    /** For resource errors with a code of their own ({@link ImageFormatException}). */
    protected ResourceException(String code, String message, Throwable cause) {
        super(Kind.RESOURCE, NO_LINE, code, message, cause);
    }
}
