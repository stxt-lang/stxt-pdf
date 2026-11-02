package dev.stxt.pdf.model;

/**
 * A band of a box that its text cannot use: the room a side box takes beside it
 * (reference/components/block.md). The lines whose vertical extent overlaps the band start {@code left} points after
 * the left edge of the box and end {@code right} points before its right edge.
 *
 * <p>The measures are relative to the border box of the box that receives the exclusion: {@code top} and
 * {@code bottom}, from its top edge ({@code top} can be negative, when the side box started above the box), and the
 * intrusions, from its left and its right edges (0 if the side box is on the other side). The box turns them into
 * measures of its content box, which is where the lines go. Being a {@code record}, it is part of the key of the
 * layout cache.</p>
 *
 * @param top    where the band starts, from the top of the border box of the box
 * @param bottom where it ends
 * @param left   points taken from the left edge of the border box, or 0
 * @param right  points taken from the right edge, or 0
 */
public record Exclusion(double top, double bottom, double left, double right) {

    /** Whether the band overlaps the vertical extent from {@code from} to {@code to}. */
    public boolean overlaps(double from, double to) {
        return top < to - Tolerance.EPSILON && bottom > from + Tolerance.EPSILON;
    }

    /** The same band measured from a reference {@code dy} points below and {@code dx} points to the right. */
    public Exclusion shifted(double dx, double dy, double width, double newWidth) {
        return new Exclusion(top - dy, bottom - dy, Math.max(0, left - dx),
                Math.max(0, right - (width - dx - newWidth)));
    }

    /** Whether the band takes room on some side. */
    public boolean intrudes() {
        return left > Tolerance.EPSILON || right > Tolerance.EPSILON;
    }
}
