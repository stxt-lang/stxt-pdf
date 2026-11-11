package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.Exclusion;

import java.util.List;

import static dev.stxt.pdf.model.Tolerance.EPSILON;

/**
 * The line box of each line of a text: where it starts in the content box and how wide it is
 * (reference/components/block.md). The lines beside a side box, or beside the drop cap of the paragraph
 * (reference/components/text.md), are shorter than the content box; from {@link #varying()} on, every line has the
 * whole width. The first line of a paragraph with {@code First line indent} (reference/common/typography.md) starts
 * that much later, or earlier if it is negative, and is that much shorter or longer.
 *
 * <p>Line {@code k} takes the vertical extent from {@code k × lineHeight} to {@code (k + 1) × lineHeight} of the
 * content box; the exclusions that overlap it take their room from its left and its right. The drop cap takes
 * {@code dropCapWidth} more from the left of its {@code dropCapLines} first lines, after the exclusions, and the first
 * line indent is added to the left of line 0, after them too: with a negative one, {@link #left} is negative if
 * nothing else takes room there, and the line starts in the left padding of the box.</p>
 */
public final class LineWidths {

    private final double contentWidth;
    private final double lineHeight;
    /** Exclusions measured from the content box: top, bottom, left intrusion and right intrusion. */
    private final List<Exclusion> exclusions;
    private final int dropCapLines;
    private final double dropCapWidth;
    /** {@code First line indent}: what line 0 starts after the others; negative, before them. */
    private final double firstLineIndent;
    /** The first line that no side box and no drop cap shortens. */
    private final int shortened;
    private final int varying;

    private LineWidths(double contentWidth, double lineHeight, List<Exclusion> exclusions, int dropCapLines,
                       double dropCapWidth, double firstLineIndent) {
        this.contentWidth = contentWidth;
        this.lineHeight = lineHeight;
        this.exclusions = List.copyOf(exclusions);
        this.dropCapLines = dropCapLines;
        this.dropCapWidth = dropCapWidth;
        this.firstLineIndent = firstLineIndent;
        double bottom = 0;
        for (Exclusion e : exclusions) {
            if (e.intrudes()) {
                bottom = Math.max(bottom, e.bottom());
            }
        }
        int fromExclusions = lineHeight > 0 && bottom > EPSILON ? (int) Math.ceil(bottom / lineHeight - EPSILON) : 0;
        this.shortened = Math.max(fromExclusions, dropCapWidth > 0 ? dropCapLines : 0);
        this.varying = Math.max(shortened, firstLineIndent != 0 ? 1 : 0);
    }

    /** Every line with the whole width of the content box. */
    public static LineWidths uniform(double contentWidth) {
        return new LineWidths(contentWidth, 1, List.of(), 0, 0, 0);
    }

    /**
     * The lines of a content box {@code contentWidth} wide, with lines {@code lineHeight} tall, beside these
     * {@code exclusions}, which are measured from the content box.
     */
    public static LineWidths of(double contentWidth, double lineHeight, List<Exclusion> exclusions) {
        return new LineWidths(contentWidth, lineHeight, exclusions, 0, 0, 0);
    }

    /** The same lines, with a drop cap {@code width} wide beside the first {@code lines} of them. */
    public LineWidths withDropCap(int lines, double width) {
        return new LineWidths(contentWidth, lineHeight, exclusions, lines, width, firstLineIndent);
    }

    /**
     * The same lines, with line 0 starting {@code indent} points later and that much shorter
     * ({@code First line indent}, reference/common/typography.md); with a negative one, earlier and longer.
     */
    public LineWidths withFirstLineIndent(double indent) {
        return new LineWidths(contentWidth, lineHeight, exclusions, dropCapLines, dropCapWidth, indent);
    }

    /** The first line that has the whole width, like all the ones after it; 0 if every line has it. */
    public int varying() {
        return varying;
    }

    /** Whether some line is not as wide as the content box. */
    public boolean varies() {
        return varying > 0;
    }

    /**
     * Whether a side box or a drop cap shortens some line: what the rest of the text, broken again on the next page,
     * no longer has beside it. The first line indent does not count: the lines after the first are not broken again.
     */
    public boolean shortened() {
        return shortened > 0;
    }

    /**
     * Room taken from the left of the content box in line {@code line}; in line 0, with the first line indent, which
     * makes it negative if it is negative and greater than what the side boxes take.
     */
    public double left(int line) {
        if (line >= varying) {
            return 0;
        }
        double left = 0;
        for (Exclusion e : exclusions) {
            if (e.overlaps(line * lineHeight, (line + 1) * lineHeight)) {
                left = Math.max(left, e.left());
            }
        }
        if (line < dropCapLines) {
            left += dropCapWidth;
        }
        return Math.min(left + indent(line), contentWidth);
    }

    /** Room taken from the right of the content box in line {@code line}. */
    public double right(int line) {
        if (line >= varying) {
            return 0;
        }
        double right = 0;
        for (Exclusion e : exclusions) {
            if (e.overlaps(line * lineHeight, (line + 1) * lineHeight)) {
                right = Math.max(right, e.right());
            }
        }
        return Math.min(right, contentWidth);
    }

    /** Width of the line box of line {@code line}: the content box minus what is taken from its two sides. */
    public double width(int line) {
        return Math.max(0, contentWidth - left(line) - right(line));
    }

    /** The first line indent of line {@code line}: that of the box in line 0, and 0 in the others. */
    private double indent(int line) {
        return line == 0 ? firstLineIndent : 0;
    }

    /**
     * Whether some line after {@code line} and before {@link #varying()} is wider than it because a side box or a drop
     * cap ends. The first line indent does not count: a word that does not fit in the indented line overflows, it
     * does not leave the line empty (reference/common/typography.md).
     */
    public boolean widerLater(int line) {
        double width = width(line) + indent(line);
        for (int k = line + 1; k <= varying; k++) {
            if (width(k) + indent(k) > width + EPSILON) {
                return true;
            }
        }
        return false;
    }
}
