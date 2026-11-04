package dev.stxt.pdf.model;

/**
 * What a conversion tells about its progress, for a progress bar or a log (reference/implementation/api.md). The
 * caller gives one with each conversion ({@code Converter.toPdf(document, target, attachments, cancellation,
 * progress)}) and the engine calls it, on the thread that converts, as the work advances: a page of the flow laid out,
 * the layout finished with the number of pages known, a page painted, and the conversion done. Every method has an
 * empty default, so an implementation overrides only what it listens to; {@link #NONE} listens to nothing.
 *
 * <p>Laying out can start over: a document whose references ({@code {page of: id}}) or whose total of pages
 * ({@code {pages}}) change what has been laid out is paginated again (reference/common/references.md), and
 * {@link #pageLaidOut} counts from 1 again in each pass. {@link #laidOut} is called once, at the end.</p>
 *
 * <p>An implementation must not throw: what it throws ends the conversion as an
 * {@link InternalException}.</p>
 */
public interface Progress {

    /** A conversion that tells nothing. */
    Progress NONE = new Progress() {
    };

    /**
     * A page of the flow has been laid out: {@code number}, from 1, within the current pagination pass.
     *
     * @param number the number of the page in this pass, from 1
     */
    default void pageLaidOut(int number) {
    }

    /**
     * The layout has finished, with {@code pages} pages; nothing has been painted yet.
     *
     * @param pages the number of pages of the document, final
     */
    default void laidOut(int pages) {
    }

    /**
     * Page {@code number} (from 1) of {@code pages} has been painted, to the PDF or to its SVG.
     *
     * @param number the page just painted, from 1
     * @param pages the number of pages of the document
     */
    default void pagePainted(int number, int pages) {
    }

    /** The conversion has finished: the PDF has been written, or added to its document, or every SVG is made. */
    default void done() {
    }
}
