package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.Box;

/**
 * What a canvas does with the meaning of what it paints, apart from drawing it: the text sequences of the tagged PDF
 * (reference/implementation/pdf.md) and the areas of the links (reference/common/links.md). Each {@link Canvas} gives
 * its own ({@link Canvas#tagging()}): that of {@link PdfCanvas} is {@link PdfTagging}; that of a canvas that only
 * draws, {@link #NONE}, which does nothing. It does not change what is seen.
 *
 * <p>Every method does nothing by default: a canvas that needs part of the tagging overrides only that part, as
 * {@link ContrastCheck} does to know which box the painted text belongs to.</p>
 */
public interface Tagging {

    /** That of a canvas that only draws: it neither marks the text nor creates links. */
    Tagging NONE = new Tagging() {
    };

    /**
     * Starts the text of a fragment of the box: the lines that follow, until {@link #endText()},
     * are its content (the tagged PDF marks it, reference/implementation/pdf.md).
     */
    default void beginText(Box box) {
    }

    /**
     * Starts the text of a link inside the text of the box (reference/implementation/pdf.md): like
     * {@link #beginText(Box)}, but what follows, until {@link #endText()}, is the content of a
     * {@code Link} of the tagged PDF. The clickable areas of that text ({@link #link}) are requested
     * after starting it.
     */
    default void beginLinkText(Box box) {
    }

    /**
     * Starts the call of a footnote inside the text of the box (reference/components/footnote.md): the number, which
     * the tagged PDF marks as a {@code Reference} to the note with that {@code Id}, until {@link #endText()}.
     */
    default void beginFootnoteCall(Box box, String footnote) {
    }

    /**
     * Starts the number that opens a footnote, inside the text of the box, which is the first text of the note: the
     * tagged PDF marks it as the label ({@code Lbl}) of the note, until {@link #endText()}.
     */
    default void beginFootnoteLabel(Box box) {
    }

    /** Ends the text started with {@link #beginText(Box)}, {@link #beginLinkText(Box)} or the footnote ones. */
    default void endText() {
    }

    /**
     * What is painted until {@link #endActualText()}, inside a started text, is extracted as {@code text}
     * (reference/implementation/pdf.md): text with letter spacing, whose separated letters would make extractors guess
     * spaces, and the end of a line split inside a word, whose hyphen is read as a soft hyphen.
     */
    default void beginActualText(String text) {
    }

    /** Ends what was started with {@link #beginActualText(String)}. */
    default void endActualText() {
    }

    /**
     * A clickable area (reference/common/links.md): the area {@code area} of the page leads to
     * {@code destination} ({@link dev.stxt.pdf.model.Links}). {@code box} is the box it belongs to
     * (the tagged PDF hangs the link from it) and {@code description} the text read in its place,
     * which may be {@code null}.
     */
    default void link(Box box, Rect area, String destination, String description) {
    }
}
