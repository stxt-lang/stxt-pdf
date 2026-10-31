package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Leader;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Tolerance;

import java.util.ArrayList;
import java.util.List;

/**
 * The leader of the entries of a table of contents (reference/components/table-of-contents.md): what is painted in the
 * empty box between the title and the page number. They are rectangles filled with the color of the text,
 * on the baseline of the entry.
 */
final class Leaders {

    /** Side of each dot, relative to the font size. */
    private static final double DOT = 0.09;
    /** Distance between two dots, relative to the font size. */
    private static final double STEP = 0.5;
    /** Thickness of the line, relative to the font size. */
    private static final double RULE = 0.05;
    /** Minimum thickness of a dot or of the line, in points: below it, it is not visible. */
    private static final double MIN = 0.4;

    private Leaders() {
    }

    /**
     * The rectangles of a leader in the content box {@code content}, resting on the baseline
     * {@code baseline}. The dots sit flush with the number, on the right, every half font size, and
     * the ones that fit are kept; the line takes the whole width.
     */
    static List<Rect> shapes(Leader leader, Rect content, double baseline, double fontSize) {
        List<Rect> shapes = new ArrayList<>();
        switch (leader) {
            case DOTS -> {
                double dot = Math.max(MIN, fontSize * DOT);
                double step = Math.max(2 * dot, fontSize * STEP);
                for (double x = content.right() - dot; x >= content.x() - Tolerance.EPSILON; x -= step) {
                    shapes.add(new Rect(x, baseline - dot, dot, dot));
                }
            }
            case LINE -> {
                double thickness = Math.max(MIN, fontSize * RULE);
                shapes.add(new Rect(content.x(), baseline - thickness, content.width(), thickness));
            }
            case NONE -> {
            }
        }
        return shapes;
    }
}
