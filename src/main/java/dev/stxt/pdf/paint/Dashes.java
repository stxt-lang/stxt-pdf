package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.LineStyle;
import dev.stxt.pdf.model.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * The pattern of a line that is not solid (reference/common/box.md#border-style): the dashes of {@code dashed} and
 * the dots of {@code dotted}, which depend on the width of the line and are fitted to its length, so that the line
 * starts and ends with a whole dash or dot. Each canvas strokes the result its own way, with the dash pattern of its
 * format, so that the PDF and the SVG paint the same.
 */
final class Dashes {

    /** Length of a dash, in widths of the line; the gap between two dashes is about as long. */
    static final double DASH = 3;
    /** Distance between the centers of two dots, in widths of the line, before fitting. */
    static final double DOT_PITCH = 2;

    private Dashes() {
    }

    /**
     * A dash pattern along a line: {@code on} painted and {@code off} empty, repeated, starting {@code phase} into
     * it. A dot is a dash of length 0 with round ends, which paint a circle as wide as the line.
     *
     * @param on    length of each dash; 0 in a dot
     * @param off   length of each gap
     * @param phase how far into the pattern the line starts
     * @param round whether the ends of each dash are round
     */
    record Pattern(double on, double off, double phase, boolean round) {

        /** The same pattern for a line that starts {@code start} further along. */
        Pattern from(double start) {
            return new Pattern(on, off, (phase + start) % (on + off), round);
        }
    }

    /**
     * The pattern of a line of {@code length} with two ends: a dash, or a dot, at each of them and the rest evenly
     * spaced. {@code null} if the line is solid: its style, or a dashed line too short for two dashes.
     */
    static Pattern open(LineStyle style, double length, double width) {
        if (style == LineStyle.DOTTED) {
            long dots = Math.max(1, Math.round((length - width) / (DOT_PITCH * width)) + 1);
            if (dots == 1) {
                // A single dot, in the middle.
                double off = length + width;
                return new Pattern(0, off, off - length / 2, true);
            }
            double pitch = (length - width) / (dots - 1);
            return new Pattern(0, pitch, pitch - width / 2, true);
        }
        if (style == LineStyle.DASHED) {
            double dash = DASH * width;
            long dashes = Math.max(1, Math.round((length + dash) / (2 * dash)));
            double gap = dashes < 2 ? 0 : (length - dashes * dash) / (dashes - 1);
            return gap > width / 100 ? new Pattern(dash, gap, 0, false) : null;
        }
        return null;
    }

    /**
     * The pattern of a closed line of {@code length}: a whole number of dashes, or dots, evenly spaced, so that the
     * pattern meets itself where the line closes. {@code null} if the line is solid.
     */
    static Pattern closed(LineStyle style, double length, double width) {
        if (style == LineStyle.DOTTED) {
            double pitch = length / Math.max(1, Math.round(length / (DOT_PITCH * width)));
            return new Pattern(0, pitch, 0, true);
        }
        if (style == LineStyle.DASHED) {
            double dash = DASH * width;
            double gap = length / Math.max(1, Math.round(length / (2 * dash))) - dash;
            return gap > width / 100 ? new Pattern(dash, gap, 0, false) : null;
        }
        return null;
    }

    /**
     * A straight line to stroke: from ({@code x1}, {@code y1}) to ({@code x2}, {@code y2}), {@code width} wide and
     * with flat ends.
     *
     * @param pattern its dashes or dots; {@code null} if it is solid
     */
    record Segment(double x1, double y1, double x2, double y2, double width, Pattern pattern) {
    }

    /**
     * The line that fills the rectangle {@code r}, along its height if it is {@code vertical} and along its width if
     * not, with the pattern of {@code style} fitted to its whole length. {@code trimStart} and {@code trimEnd} leave
     * out that much of each end without moving the pattern: a dash is cut there, and a dot that does not fit whole
     * in what is left is not painted. {@code null} if nothing is left.
     */
    static Segment segment(Rect r, boolean vertical, LineStyle style, double trimStart, double trimEnd) {
        double length = vertical ? r.height() : r.width();
        double width = vertical ? r.width() : r.height();
        Pattern pattern = open(style, length, width);
        double start = trimStart;
        double end = length - trimEnd;
        if (pattern != null && pattern.round()) {
            // A dot is painted whole if its center is on the line: the line ends where the center of the last dot
            // that fits can be.
            double slack = width / 100;
            start = trimStart > 0 ? trimStart + width / 2 - slack : 0;
            end = trimEnd > 0 ? length - trimEnd - width / 2 + slack : length;
        }
        if (end <= start) {
            return null;
        }
        Pattern from = pattern == null ? null : pattern.from(start);
        if (vertical) {
            double x = r.x() + width / 2;
            return new Segment(x, r.y() + start, x, r.y() + end, width, from);
        }
        double y = r.y() + width / 2;
        return new Segment(r.x() + start, y, r.x() + end, y, width, from);
    }

    /** The line of a side of a border, with the side it belongs to, which gives its color. */
    record SideSegment(Shapes.Side side, Segment segment) {
    }

    /**
     * The sides of a border with square corners that is not solid: one line per side with a width, each with its
     * pattern fitted to the whole side, so that there is a dash or a dot at each corner. The corner where two sides
     * meet belongs to the horizontal one: the vertical one starts after it, so that two sides do not paint the same
     * place twice.
     */
    static List<SideSegment> border(Rect r, Insets w, LineStyle style) {
        List<SideSegment> sides = new ArrayList<>();
        add(sides, Shapes.Side.TOP, w.top(), new Rect(r.x(), r.y(), r.width(), w.top()), false, style, 0, 0);
        add(sides, Shapes.Side.BOTTOM, w.bottom(), new Rect(r.x(), r.bottom() - w.bottom(), r.width(), w.bottom()),
                false, style, 0, 0);
        add(sides, Shapes.Side.LEFT, w.left(), new Rect(r.x(), r.y(), w.left(), r.height()), true, style, w.top(),
                w.bottom());
        add(sides, Shapes.Side.RIGHT, w.right(), new Rect(r.right() - w.right(), r.y(), w.right(), r.height()), true,
                style, w.top(), w.bottom());
        return sides;
    }

    private static void add(List<SideSegment> sides, Shapes.Side side, double width, Rect r, boolean vertical,
                            LineStyle style, double trimStart, double trimEnd) {
        if (width > 0) {
            Segment segment = segment(r, vertical, style, trimStart, trimEnd);
            if (segment != null) {
                sides.add(new SideSegment(side, segment));
            }
        }
    }
}
