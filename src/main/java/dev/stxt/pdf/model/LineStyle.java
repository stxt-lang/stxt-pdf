package dev.stxt.pdf.model;

/**
 * How a line is drawn: {@code Border style}, of the border of a box (reference/common/box.md#border-style), and
 * {@code Separator style}, of the line between the columns of a {@code Columns} (reference/components/columns.md).
 * The pattern of a line that is not solid depends on its width alone: it is not declared.
 */
public enum LineStyle {
    /** A continuous line (by default). */
    SOLID,
    /** Dashes three times as long as the line is wide, with a gap of about the same length. */
    DASHED,
    /** Round dots as wide as the line, about two widths apart from center to center. */
    DOTTED
}
