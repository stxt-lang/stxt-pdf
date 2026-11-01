package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.NumberFormat;

/**
 * A section of the document (reference/components/document.md): its box, which is the root of its layout (a
 * {@code Box} without a parent, like the anonymous root of a document with a single section), its medium and what it
 * says about the pages it takes up.
 *
 * @param box          the box: the page area of each of its pages; it is {@link Element#SECTION}
 * @param pageSetup        its medium: the one of the document with what the section declares on top
 * @param numbering    how it numbers its pages
 * @param pageNumberFormat    how its page numbers are written ({@code Page number format})
 * @param start        the number of its first page with {@code Page numbering: restart} ({@code Start}); 1 if it does
 *                     not declare it
 * @param inheritPageOverlays whether it carries the {@code Page overlay} elements of the document
 * @param beginOn      on which physical page it begins ({@code Begin on})
 * @param footnoteNumbering   whether its footnotes continue the numbering of the document or start again at 1
 *                            ({@code Footnote numbering}, reference/components/footnote.md)
 * @param footnoteNumberFormat how its footnotes are numbered ({@code Footnote number format})
 * @param footnotes    its footnote area ({@link Element#FOOTNOTES}): a child of the box, out of its flow, with its
 *                     {@code Footnote} boxes as children in document order
 */
public record Section(Box box, PageSetup pageSetup, PageNumbering numbering, NumberFormat pageNumberFormat, int start,
                      boolean inheritPageOverlays, BeginOn beginOn, FootnoteNumbering footnoteNumbering,
                      NumberFormat footnoteNumberFormat, Box footnotes) {

    /** {@code Footnote numbering} (reference/components/footnote.md): what the section does with the footnote counter. */
    public enum FootnoteNumbering {
        /** Its footnotes follow the counter of the document. */
        CONTINUE,
        /** Its first footnote is number 1 again. */
        RESTART
    }

    /** Whether the section has footnotes. */
    public boolean hasFootnotes() {
        return !footnotes.children().isEmpty();
    }

    /**
     * {@code Begin on} (reference/components/document.md): on which physical page the section begins. If the next page
     * is not the right one, the previous section ends with a blank page.
     */
    public enum BeginOn {
        /** On the next page, whichever it is. */
        ANY,
        /** On an odd page: on the right, when printing double-sided. */
        ODD
    }

    /** {@code Page numbering} (reference/components/document.md): what the section does with the page counter. */
    public enum PageNumbering {
        /** Its pages follow the counter of the document. */
        CONTINUE,
        /** Its first page is page 1 again. */
        RESTART,
        /** Its pages have no number and do not advance the counter. */
        NONE
    }
}
