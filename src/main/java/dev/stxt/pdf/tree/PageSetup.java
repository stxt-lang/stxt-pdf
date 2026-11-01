package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Value;

/**
 * The output medium (reference/components/document.md): a sequence of pages of fixed size. It defines the initial
 * reference container: the page area.
 *
 * @param width  width of the page, in pt
 * @param height height of the page, in pt
 * @param margin page margins, in pt
 */
public record PageSetup(double width, double height, Insets margin) {

    /** Page area: the initial reference container and the one of {@code Page overlay} boxes. */
    public Rect pageArea() {
        return new Rect(margin.left(), margin.top(), width - margin.horizontal(), height - margin.vertical());
    }

    /**
     * The named paper sizes of {@code Paper} (reference/components/document.md), in portrait: the ISO 216 ones, exact
     * in millimeters (A4 = 210 × 297 mm, 595.28 × 841.89 pt), and the US ones, exact in points.
     */
    public enum Paper {
        A3(Value.mm(297), Value.mm(420)), A4(Value.mm(210), Value.mm(297)), A5(Value.mm(148), Value.mm(210)),
        A6(Value.mm(105), Value.mm(148)), B4(Value.mm(250), Value.mm(353)), B5(Value.mm(176), Value.mm(250)),
        LETTER(Value.pt(612), Value.pt(792)), LEGAL(Value.pt(612), Value.pt(1008));

        /** Width and height in portrait, in pt. */
        public final double width;
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
        PORTRAIT, LANDSCAPE
    }
}
