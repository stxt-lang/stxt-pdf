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
 * @param style    how it is drawn ({@code Border style})
 * @param vertical whether the line runs from top to bottom, and its pattern with it: its thickness is then its
 *                 width, and if not, its height. Whoever builds the line says it, because a piece of a line can be
 *                 shorter than it is thick
 */
public record Rule(double x, double y, double width, double height, String color, LineStyle style,
                   boolean vertical) {

    /** A solid line, which has no pattern to run along it: it is vertical if it is taller than it is wide. */
    public Rule(double x, double y, double width, double height, String color) {
        this(x, y, width, height, color, LineStyle.SOLID, height > width);
    }

    /** How thick the line is: its width if it is vertical and its height if not. */
    public double thickness() {
        return vertical ? width : height;
    }
}
