package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.Box;

/**
 * The vertical stacking of block flow (reference/components/block.md): a cursor, where the content placed so
 * far ends, and the margins waiting below it ({@link MarginStrut}), which merge with those of the next child. While
 * nothing separates the top border of the box from its children ({@code openAtTop}), the margins of the first child
 * with content come out of the box at the top ({@link #topOut}) and the child starts at 0.
 *
 * <p>It is what {@code BlockLayout} does with the children of a block and {@link FootnoteLayout} with the notes of a
 * footnote area, so that a rule of margin merging or of horizontal placement is the same in both. Where a child goes
 * and whether it fits is decided by whoever uses it; this class only keeps the account.</p>
 */
public final class Stacking {

    private boolean atTop;
    private MarginStrut topOut = MarginStrut.EMPTY;
    private MarginStrut strut = MarginStrut.EMPTY;
    private double cursor;

    /** {@code openAtTop}: nothing separates the top border of the box from its first child. */
    public Stacking(boolean openAtTop) {
        this.atTop = openAtTop;
    }

    /** Whether no child with content has been placed yet and the margins still come out of the box at the top. */
    public boolean atTop() {
        return atTop;
    }

    /** Where the content placed so far ends, without the margins below it. */
    public double cursor() {
        return cursor;
    }

    /** The margins waiting below the cursor. */
    public MarginStrut strut() {
        return strut;
    }

    /** The margins that come out of the box at the top: those of its first child with content, merged. */
    public MarginStrut topOut() {
        return topOut;
    }

    /**
     * Position of the top border of a child with these top margins, merged with those waiting below the cursor; 0 if
     * they come out of the box at the top.
     */
    public double top(MarginStrut childTop) {
        return atTop ? 0 : cursor + strut.add(childTop).resolve();
    }

    /** An empty child: its {@code margins}, the top and the bottom ones, merge with those around it; the cursor stays. */
    public void empty(MarginStrut margins) {
        strut = strut.add(margins);
    }

    /**
     * A child with content: its top margins ({@code childTop}) merge with those waiting, it ends at {@code bottom}, and
     * its bottom margins ({@code childBottom}) wait below it.
     */
    public void place(MarginStrut childTop, double bottom, MarginStrut childBottom) {
        strut = strut.add(childTop);
        if (atTop) {
            topOut = strut;
            atTop = false;
        }
        cursor = bottom;
        strut = childBottom;
    }

    /**
     * The x of the border box of {@code child}, {@code width} wide, in a content box {@code contentWidth} wide: after
     * its left margin, or centered if its side margins are {@code auto}.
     */
    public static double x(double contentWidth, double width, Box child) {
        return Sizing.horizontalMargins(contentWidth, width, child.style().box().margin().left(),
                child.style().box().margin().right())[0];
    }
}
