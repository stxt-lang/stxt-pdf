package dev.stxt.pdf.fragment;

/**
 * A line that a fragment draws over the backgrounds of what it contains and under its text: the merged borders of a
 * table (reference/components/table.md#merged-borders). It is a filled rectangle, relative to the content box of the
 * fragment, like its children.
 *
 * @param x      left edge
 * @param y      top edge
 * @param width  width
 * @param height height
 * @param color  its color ({@code #rrggbb})
 */
public record Rule(double x, double y, double width, double height, String color) {
}
