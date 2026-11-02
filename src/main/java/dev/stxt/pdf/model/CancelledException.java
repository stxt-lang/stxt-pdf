package dev.stxt.pdf.model;

/**
 * The conversion stopped before it finished because its {@link Cancellation} asked for it
 * (reference/implementation/api.md): a deadline that passed, a thread that was interrupted, a client that left. It
 * is neither the document's fault nor the environment's, so it has its own kind, {@link Kind#CANCELLED}, and it
 * belongs to no line. Nothing of the PDF has been written to the destination when it comes out.
 */
public class CancelledException extends StxtPdfException {

    private static final long serialVersionUID = 1L;

    /** The {@link #code()} of every cancellation. */
    public static final String CODE = "CANCELLED";

    public CancelledException() {
        super(Kind.CANCELLED, NO_LINE, CODE,
                "The conversion was cancelled before it finished: its Cancellation asked it to stop", null);
    }
}
