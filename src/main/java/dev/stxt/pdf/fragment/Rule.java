package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.LineStyle;

/**
 * A line that a fragment draws over the backgrounds of what it contains and under its text: the merged borders of a
 * table (reference/components/table.md#merged-borders). It is a rectangle, relative to the content box of the
 * fragment, like its children: filled if the line is solid and, if not, with the pattern of its style along it.
 *
 * @param x      left edge
 * @param y      top edge
 * @param width  width
 * @param height height
 * @param color  its color ({@code #rrggbb})
 * @param style  how it is drawn ({@code Border style})
 */
public record Rule(double x, double y, double width, double height, String color, LineStyle style) {

    /** A solid line. */
    public Rule(double x, double y, double width, double height, String color) {
        this(x, y, width, height, color, LineStyle.SOLID);
    }

    /** Whether the line runs from top to bottom: it is taller than it is wide. */
    public boolean vertical() {
        return height > width;
    }
}
