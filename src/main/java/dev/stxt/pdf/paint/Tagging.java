package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.Box;

/**
 * What a canvas does with the meaning of what it paints, apart from drawing it: the text sequences of the tagged PDF
 * (reference/implementation/pdf.md) and the areas of the links (reference/common/links.md). Each {@link Canvas} gives
 * its own ({@link Canvas#tagging()}): that of {@link PdfCanvas} is {@link PdfTagging}; that of a canvas that only
 * draws, {@link #NONE}, which does nothing. It does not change what is seen.
 */
public interface Tagging {

    /** That of a canvas that only draws: it neither marks the text nor creates links. */
    Tagging NONE = new Tagging() {
        @Override
        public void beginText(Box box) {
        }

        @Override
        public void beginLinkText(Box box) {
        }

        @Override
        public void endText() {
        }

        @Override
        public void beginActualText(String text) {
        }

        @Override
        public void endActualText() {
        }

        @Override
        public void link(Box box, Rect area, String destination, String description) {
        }
    };

    /**
     * Starts the text of a fragment of the box: the lines that follow, until {@link #endText()},
     * are its content (the tagged PDF marks it, reference/implementation/pdf.md).
     */
    void beginText(Box box);

    /**
     * Starts the text of a link inside the text of the box (reference/implementation/pdf.md): like
     * {@link #beginText(Box)}, but what follows, until {@link #endText()}, is the content of a
     * {@code Link} of the tagged PDF. The clickable areas of that text ({@link #link}) are requested
     * after starting it.
     */
    void beginLinkText(Box box);

    /** Ends the text started with {@link #beginText(Box)} or {@link #beginLinkText(Box)}. */
    void endText();

    /**
     * What is painted until {@link #endActualText()}, inside a started text, is extracted as {@code text}
     * (reference/implementation/pdf.md): text with letter spacing, whose separated letters would make extractors guess
     * spaces, and the end of a line split inside a word, whose hyphen is read as a soft hyphen.
     */
    void beginActualText(String text);

    /** Ends what was started with {@link #beginActualText(String)}. */
    void endActualText();

    /**
     * A clickable area (reference/common/links.md): the area {@code area} of the page leads to
     * {@code destination} ({@link dev.stxt.pdf.model.Links}). {@code box} is the box it belongs to
     * (the tagged PDF hangs the link from it) and {@code description} the text read in its place,
     * which may be {@code null}.
     */
    void link(Box box, Rect area, String destination, String description);
}
