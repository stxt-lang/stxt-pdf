package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.CaptionPosition;
import dev.stxt.pdf.model.FigureCounter;

/**
 * The properties of a {@code Figure} (reference/components/figure.md): where its caption goes and which counter
 * numbers it. They are not inherited. Both can be left undeclared: the builder resolves them from the content of the
 * figure ({@link Figures}) and writes the result here, so that after the tree is built neither is {@code null}.
 */
public final class FigureStyle extends StyleGroup {

    /** {@code Caption position}; {@code null} while it is not declared nor resolved. */
    private CaptionPosition captionPosition;
    /** {@code Figure counter}; {@code null} while it is not declared nor resolved. */
    private FigureCounter counter;

    FigureStyle() {
    }

    /** Where the caption goes; after the tree is built, never {@code null}. */
    public CaptionPosition captionPosition() {
        return captionPosition;
    }

    void setCaptionPosition(CaptionPosition v) {
        writable();
        captionPosition = v;
    }

    /** The counter the figure is numbered with; after the tree is built, never {@code null}. */
    public FigureCounter counter() {
        return counter;
    }

    void setCounter(FigureCounter v) {
        writable();
        counter = v;
    }
}
