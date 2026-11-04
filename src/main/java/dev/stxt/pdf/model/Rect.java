package dev.stxt.pdf.model;

/**
 * Rectangle in points.
 *
 * @param x      the left edge
 * @param y      the top edge; y grows downward
 * @param width  the width
 * @param height the height
 */
public record Rect(double x, double y, double width, double height) {

    /**
     * The rectangle grown outward by those widths, as a padding box grows into the border box.
     *
     * @param i the widths added on each side
     * @return a new rectangle
     */
    public Rect expand(Insets i) {
        return new Rect(x - i.left(), y - i.top(), width + i.horizontal(), height + i.vertical());
    }

    /**
     * The rectangle reduced inward by those widths, as a border box shrinks into the padding box.
     *
     * @param i the widths taken off each side
     * @return a new rectangle, whose width or height can be negative if the widths exceed them
     */
    public Rect shrink(Insets i) {
        return new Rect(x + i.left(), y + i.top(), width - i.horizontal(), height - i.vertical());
    }

    /**
     * The same rectangle moved.
     *
     * @param dx the horizontal shift, positive to the right
     * @param dy the vertical shift, positive downward
     * @return a new rectangle of the same size
     */
    public Rect translate(double dx, double dy) {
        return new Rect(x + dx, y + dy, width, height);
    }

    /** {@return the right edge: {@code x + width}} */
    public double right() {
        return x + width;
    }

    /** {@return the bottom edge: {@code y + height}} */
    public double bottom() {
        return y + height;
    }
}
