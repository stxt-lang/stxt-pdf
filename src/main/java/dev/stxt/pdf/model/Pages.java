package dev.stxt.pdf.model;

/**
 * The pages on which a {@code Page overlay} appears (reference/components/frame-and-overlays.md); page 1 is the
 * first page of the document.
 */
public enum Pages {
    ALL, FIRST, NOT_FIRST, ODD, EVEN, LAST;

    /** Whether a {@code Page overlay} with this value appears on page {@code page} of a document of {@code total}. */
    public boolean includes(int page, int total) {
        return switch (this) {
            case ALL -> true;
            case FIRST -> page == 1;
            case NOT_FIRST -> page != 1;
            case ODD -> page % 2 == 1;
            case EVEN -> page % 2 == 0;
            case LAST -> page == total;
        };
    }
}
