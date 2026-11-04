package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Value;

/**
 * The output medium (reference/components/document.md): a sequence of pages of fixed size. It defines the initial
 * reference container: the page area.
 *
 * <p>With {@code Mirror margins}, {@code margin} is the one of the odd physical pages (the right-hand ones when
 * printing double-sided) and the even pages swap its left and right: {@link #forPage(int)} gives the medium of each
 * page. The page area measures the same on both sides, so the flow does not depend on the side.</p>
 *
 * @param width         width of the page, in pt
 * @param height        height of the page, in pt
 * @param margin        page margins, in pt: those of the odd pages if {@code mirrorMargins}
 * @param mirrorMargins whether the even pages swap the left and right margins ({@code Mirror margins})
 */
public record PageSetup(double width, double height, Insets margin, boolean mirrorMargins) {

    /**
     * A medium without {@code Mirror margins}.
     *
     * @param width  width of the page, in pt
     * @param height height of the page, in pt
     * @param margin page margins, in pt, the same on every page
     */
    public PageSetup(double width, double height, Insets margin) {
        this(width, height, margin, false);
    }

    /**
     * {@return the page area: the initial reference container and the one of {@code Page overlay} boxes; the page
     * minus its margins, in page coordinates}
     */
    public Rect pageArea() {
        return new Rect(margin.left(), margin.top(), width - margin.horizontal(), height - margin.vertical());
    }

    /**
     * The medium of the physical page {@code number} (from 1): with {@code Mirror margins}, an even page swaps the
     * left and right margins; otherwise it is this one.
     *
     * @param number physical page number, from 1, across the whole document
     * @return the medium of that page: this one, or a copy with the left and right margins swapped
     */
    public PageSetup forPage(int number) {
        if (!mirrorMargins || number % 2 != 0) {
            return this;
        }
        return new PageSetup(width, height, new Insets(margin.top(), margin.left(), margin.bottom(), margin.right()),
                true);
    }

    /**
     * The named paper sizes of {@code Paper} (reference/components/document.md), in portrait: the ISO 216 ones, exact
     * in millimeters (A4 = 210 × 297 mm, 595.28 × 841.89 pt), and the US ones, exact in points.
     */
    public enum Paper {
        /** ISO A3, 297 x 420 mm. */
        A3(Value.mm(297), Value.mm(420)),
        /** ISO A4, 210 x 297 mm. */
        A4(Value.mm(210), Value.mm(297)),
        /** ISO A5, 148 x 210 mm. */
        A5(Value.mm(148), Value.mm(210)),
        /** ISO A6, 105 x 148 mm. */
        A6(Value.mm(105), Value.mm(148)),
        /** ISO B4, 250 x 353 mm. */
        B4(Value.mm(250), Value.mm(353)),
        /** ISO B5, 176 x 250 mm. */
        B5(Value.mm(176), Value.mm(250)),
        /** US Letter, 8.5 x 11 in (612 x 792 pt). */
        LETTER(Value.pt(612), Value.pt(792)),
        /** US Legal, 8.5 x 14 in (612 x 1008 pt). */
        LEGAL(Value.pt(612), Value.pt(1008));

        /** Width in portrait, in pt. */
        public final double width;
        /** Height in portrait, in pt. */
        public final double height;

        Paper(Value width, Value height) {
            this.width = ((Value.Points) width).pt();
            this.height = ((Value.Points) height).pt();
        }
    }

    /**
     * {@code Orientation} (reference/components/document.md): {@code landscape} swaps the width and the height of the
     * paper.
     */
    public enum Orientation {
        /** The width and the height of the paper as {@link Paper} gives them. */
        PORTRAIT,
        /** The width and the height of the paper swapped. */
        LANDSCAPE
    }
}
