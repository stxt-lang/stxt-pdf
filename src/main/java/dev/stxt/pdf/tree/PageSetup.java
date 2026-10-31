package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;

/**
 * The output medium (reference/components/document.md): a sequence of pages of fixed size. It defines the initial
 * reference container: the page area.
 *
 * @param width  width of the page, in pt
 * @param height height of the page, in pt
 * @param margin page margins, in pt
 */
public record PageSetup(double width, double height, Insets margin) {

    /** Page area: the initial reference container and the one of {@code fixed} boxes. */
    public Rect pageArea() {
        return new Rect(margin.left(), margin.top(), width - margin.horizontal(), height - margin.vertical());
    }

    /**
     * The named paper sizes of {@code Paper} (reference/components/document.md), in portrait and in whole points: the
     * ISO 216 ones rounded, as they are always used (A4 = 595 × 842), and the US ones.
     */
    public enum Paper {
        A3(842, 1191), A4(595, 842), A5(420, 595), A6(298, 420), B4(709, 1001), B5(499, 709),
        LETTER(612, 792), LEGAL(612, 1008);

        /** Width and height in portrait, in pt. */
        public final double width;
        public final double height;

        Paper(double width, double height) {
            this.width = width;
            this.height = height;
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
