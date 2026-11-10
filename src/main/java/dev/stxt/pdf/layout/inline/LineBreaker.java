package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.Tolerance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Breaks a text into lines (reference/common/typography.md). With {@code Line breaking: simple},
 * with the greedy algorithm (<em>first-fit</em>) of browsers; with {@code optimal}, with
 * Knuth-Plass ({@link #optimal}).
 *
 * <p>The line breaking goes in three steps, each one in its class: {@link Words} splits the text into words (the
 * white space counts as one space, and the text is a single paragraph: a blank line is an error when reading it,
 * except in a preformatted text, which {@link PreformattedText} breaks); {@link Paragraph} measures them and gives the
 * positions where a line may end; this class chooses among those positions; and {@link LinePlacer} places each line in
 * the content box. A word wider than the line takes a line of its own and overflows.</p>
 *
 * <p><strong>Forced line breaks</strong> (reference/components/markdown.md). The
 * {@link TextRun#LINE_SEPARATOR} (U+2028), which a Markdown line break becomes, ends the line there: both algorithms
 * end a line at the start of the word after it and no line goes past it ({@link Paragraph#forced}). The line it ends
 * is never justified ({@link Line#forced()}), like the last one.</p>
 *
 * <p><strong>Hyphenation</strong> ({@code Hyphenate}, reference/common/typography.md). A word may be split at its
 * break points ({@link Words}). A line that ends inside a word ends with the {@link #HYPHEN} of the font, in the
 * format of the letter before it, and {@link Line#hyphenated()} says so. The places where a line may end are the
 * <em>positions</em> of the text: the start of each word and each of its break points ({@link Paragraph}); both
 * algorithms choose among them. The greedy one adds whole words while they fit and, when one does not, takes the
 * longest part of it that fits with its hyphen; Knuth-Plass charges each hyphen the penalty of TeX
 * ({@value #HYPHEN_PENALTY}, squared) and two consecutive hyphenated lines {@value #DOUBLE_HYPHEN_DEMERITS} more.</p>
 */
public final class LineBreaker {

    /** Demerits of each line besides those of its badness: it favors line breakings with fewer lines. */
    private static final double LINE_PENALTY = 10;
    /** Badness of a line that cannot be stretched enough or that overflows. */
    private static final double MAX_BADNESS = 10_000;
    /** How much each space can stretch, in proportion to its width (TeX's value). */
    private static final double STRETCH = 0.5;
    /** Penalty of a line that ends with a hyphen, added squared to its demerits (TeX's {@code \hyphenpenalty}). */
    private static final double HYPHEN_PENALTY = 50;
    /** Demerits added when two consecutive lines end with a hyphen (TeX's {@code \doublehyphendemerits}). */
    private static final double DOUBLE_HYPHEN_DEMERITS = 10_000;
    /** The hyphen painted where a word is split: U+002D, the one every font has. */
    public static final String HYPHEN = "-";
    /** The soft hyphen of the text: a break point written by the author, which is never painted. */
    public static final char SOFT_HYPHEN = '\u00AD';

    /**
     * A line of the line breaking: its runs, with consecutive ones of the same format joined, whether it ends
     * inside a word, with a hyphen (its last run ends with {@link #HYPHEN}), the position of the text where it
     * starts ({@link Paragraph}), from which the rest of the text can be broken again
     * (reference/components/block.md), and whether a forced line break ends it. A line without runs is an empty one:
     * a line beside a side box where no word fits, which is left blank.
     */
    public record Line(List<TextRun> runs, boolean hyphenated, int start, boolean forced) {

        public Line {
            runs = List.copyOf(runs);
        }

        /** A line that ends between words, at the start of the text. */
        public Line(List<TextRun> runs) {
            this(runs, false, 0);
        }

        /** A line that no forced line break ends. */
        public Line(List<TextRun> runs, boolean hyphenated, int start) {
            this(runs, hyphenated, start, false);
        }

        /** An empty line at position {@code start}: no word of the text fits in it. */
        static Line empty(int start) {
            return new Line(List.of(), false, start);
        }

        public String text() {
            return TextRun.text(runs);
        }

        /** Whether the line has no words. */
        public boolean isEmpty() {
            return runs.isEmpty();
        }
    }

    private LineBreaker() {
    }

    /**
     * Lines of the text from position {@code from} (0 is its start) with the width of each line given by
     * {@code widths} (reference/components/block.md): with the greedy algorithm or with Knuth-Plass. In a line narrower
     * than the ones after it, a word that does not fit alone leaves it empty and goes to the next one, instead of
     * overflowing.
     */
    static List<Line> breakLines(Paragraph p, int from, LineWidths widths, LineBreaking wrap) {
        return wrap == LineBreaking.OPTIMAL ? optimal(p, from, widths) : greedy(p, from, widths);
    }

    /**
     * The greedy line breaking: each word goes to the current line if it fits and, if not, starts another; if it
     * has break points, the longest part of it that fits, with its hyphen, ends the line and the rest starts the
     * next one. The width of a candidate line is the sum of the advances of its words, spaces and hyphen, each one
     * measured once; only when the sum is within {@link Tolerance#MODEL} of the available width is the line built
     * and measured whole, so the decision is the same as measuring it every time, regardless of the rounding of the
     * sum. A word that does not fit alone at the start of a line, not even split, takes the line and overflows,
     * unless a later line is wider (a side box ends): then the line is left empty and the word goes to the next. A
     * word followed by a forced line break ends its line.
     */
    private static List<Line> greedy(Paragraph p, int from, LineWidths widths) {
        List<Line> lines = new ArrayList<>();
        int end = p.positions() - 1;
        // The line in progress starts at position a; lastFit is the last word end at which it still fits (-1 if
        // nothing has been added to it yet).
        int a = from;
        int lastFit = -1;
        int w = p.word(from);
        while (true) {
            if (w == p.words()) {
                lines.add(p.line(a, end));
                return lines;
            }
            double available = widths.width(lines.size());
            int whole = p.start(w + 1);
            if (p.fitsMeasured(a, whole, available)) {
                lastFit = whole;
                w++;
                if (p.forced(whole)) {
                    // A forced line break follows the word: the line ends with it.
                    lines.add(p.line(a, whole));
                    a = whole;
                    lastFit = -1;
                }
                continue;
            }
            // The word does not fit whole: the longest part of it that fits with its hyphen.
            int cut = -1;
            for (int q = whole - 1; q > a && p.word(q) == w; q--) {
                if (p.fitsMeasured(a, q, available)) {
                    cut = q;
                    break;
                }
            }
            if (cut >= 0) {
                lines.add(p.line(a, cut));
                a = cut;
                lastFit = -1;
                continue;
            }
            if (lastFit > a) {
                // Nothing of the word fits on this line: it ends with the previous word and the word is tried again
                // at the start of the next one.
                lines.add(p.line(a, lastFit));
                a = lastFit;
                lastFit = -1;
                continue;
            }
            if (widths.widerLater(lines.size())) {
                // Alone at the start of a line beside a side box and it does not fit: the line is left empty.
                lines.add(Line.empty(a));
                continue;
            }
            // Alone at the start of the line and it does not fit, not even split: it takes the line and overflows.
            lastFit = whole;
            w++;
            if (p.forced(whole)) {
                lines.add(p.line(a, whole));
                a = whole;
                lastFit = -1;
            }
        }
    }

    /**
     * Simplified Knuth-Plass: the breaks of the text that minimize the sum of the demerits
     * of its lines, by dynamic programming over the positions of the text. Without shrinking of the spaces
     * (justified lines are not compressed):
     *
     * <ul>
     *   <li>{@code badness = 100 × r³}, with {@code r} = leftover / stretch of the line
     *       (each space stretches up to half its width), up to {@value #MAX_BADNESS}.
     *       A line without spaces that does not fill the width, or a word that overflows, has the
     *       maximum badness. The last line has no badness.</li>
     *   <li>{@code demerits = (line penalty + badness)²}, plus {@value #HYPHEN_PENALTY}² if the line ends with a
     *       hyphen and {@value #DOUBLE_HYPHEN_DEMERITS} more if the previous one did too.</li>
     * </ul>
     *
     * <p>The width of a line is the sum of those of its words, spaces and hyphen.</p>
     *
     * <p>When the lines have different widths (reference/components/block.md), the state of the dynamic programming
     * is the position and the number of the line that starts there, up to {@link LineWidths#varying()}: from that
     * line on, every line has the same width and the number no longer matters. In a line narrower than the ones
     * after it, a single piece of a word that does not fit may not overflow: the line is left empty, with the
     * demerits of a line without badness, and the piece goes to the next one.</p>
     *
     * <p>No line goes past a forced line break, and the line that ends at one has no badness, like the last.</p>
     */
    static List<Line> optimal(Paragraph p, int from, LineWidths widths) {
        int positions = p.positions();
        int end = positions - 1;
        int states = widths.varying() + 1;
        int uniform = states - 1;
        // best[j][k]: minimum demerits of the lines that end at position j with line k the next to start there
        // (k capped at the first uniform line); fromPos and fromLine, the state the last of those lines came from.
        double[][] best = new double[positions][states];
        int[][] fromPos = new int[positions][states];
        int[][] fromLine = new int[positions][states];
        for (double[] row : best) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        best[from][0] = 0;
        skipEmpty(p, from, widths, best, fromPos, fromLine);
        for (int j = from + 1; j <= end; j++) {
            for (int next = 0; next < states; next++) {
                // The lines that end at j with next as the following one started at a line k whose successor is next.
                for (int k = Math.max(0, next - 1); k <= Math.min(next, uniform); k++) {
                    if (k + 1 != next && !(k == uniform && next == uniform)) {
                        continue;
                    }
                    double available = widths.width(k);
                    for (int i = j - 1; i >= from; i--) {
                        if (i + 1 < j && p.forced(i + 1)) {
                            // Starting earlier, the line would go past a forced line break.
                            break;
                        }
                        if (best[i][k] == Double.POSITIVE_INFINITY) {
                            continue;
                        }
                        double width = p.width(i, j);
                        boolean single = p.single(i, j);
                        if (!single && !fits(width, available)) {
                            // Starting earlier it would be even wider; a single piece of a word may overflow.
                            break;
                        }
                        if (single && !fits(width, available) && widths.widerLater(k)) {
                            // Beside a side box, the piece goes to a wider line instead of overflowing.
                            continue;
                        }
                        double d = best[i][k] + demerits(available - width, p.spaces(i, j) * STRETCH,
                                j == end || p.forced(j));
                        if (p.hyphen(j)) {
                            d += HYPHEN_PENALTY * HYPHEN_PENALTY;
                            if (p.hyphen(i)) {
                                d += DOUBLE_HYPHEN_DEMERITS;
                            }
                        }
                        if (d < best[j][next]) {
                            best[j][next] = d;
                            fromPos[j][next] = i;
                            fromLine[j][next] = k;
                        }
                    }
                }
            }
            skipEmpty(p, j, widths, best, fromPos, fromLine);
        }
        int last = 0;
        for (int k = 1; k < states; k++) {
            if (best[end][k] < best[end][last]) {
                last = k;
            }
        }
        List<Line> lines = new ArrayList<>();
        for (int j = end, k = last; j > from || k > 0; ) {
            int i = fromPos[j][k];
            int line = fromLine[j][k];
            lines.add(0, i == j ? Line.empty(j) : p.line(i, j));
            j = i;
            k = line;
        }
        return lines;
    }

    /**
     * The empty lines that may start at position {@code at}: beside a side box, if the smallest piece of the text
     * from there does not fit in line {@code k} and a later line is wider, line {@code k} may be left empty and the
     * text go on in line {@code k + 1}, with the demerits of a line without badness.
     */
    private static void skipEmpty(Paragraph p, int at, LineWidths widths, double[][] best, int[][] fromPos,
                                  int[][] fromLine) {
        if (at >= p.positions() - 1) {
            return;
        }
        for (int k = 0; k < widths.varying(); k++) {
            if (best[at][k] == Double.POSITIVE_INFINITY || !widths.widerLater(k)) {
                continue;
            }
            double piece = p.width(at, at + 1);
            if (fits(piece, widths.width(k))) {
                continue;
            }
            double d = best[at][k] + LINE_PENALTY * LINE_PENALTY;
            if (d < best[at][k + 1]) {
                best[at][k + 1] = d;
                fromPos[at][k + 1] = at;
                fromLine[at][k + 1] = k;
            }
        }
    }

    /** Demerits of a line with this leftover and this possible stretch. */
    private static double demerits(double slack, double stretch, boolean last) {
        double badness;
        if (slack < -EPSILON) {
            badness = MAX_BADNESS;
        } else if (last || slack <= EPSILON) {
            badness = 0;
        } else if (stretch <= 0) {
            badness = MAX_BADNESS;
        } else {
            badness = Math.min(MAX_BADNESS, 100 * Math.pow(slack / stretch, 3));
        }
        return (LINE_PENALTY + badness) * (LINE_PENALTY + badness);
    }

    /**
     * Narrowest width (the widest word or, hyphenating, the widest part of a word that cannot be split further, with
     * its hyphen if it is not the last part) and natural width (the whole text unbroken, or, with forced line breaks,
     * its widest line).
     *
     * @param hyphenator the patterns of the language of the text; {@code null} splits words only at their soft hyphens
     */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize, double letterSpacing, Hyphenator hyphenator) {
        List<Words.Word> words = Words.words(runs, hyphenator);
        if (words.isEmpty()) {
            return IntrinsicSizes.ZERO;
        }
        double min = 0;
        for (Words.Word word : words) {
            if (word.breaks().length == 0) {
                min = Math.max(min, LinePlacer.width(word.parts(), measurer, font, fontSize, letterSpacing));
                continue;
            }
            int from = 0;
            for (int k = 0; k <= word.breaks().length; k++) {
                int to = k < word.breaks().length ? word.breaks()[k] : word.length();
                List<TextRun> part = Words.slice(word.parts(), from, to);
                List<TextRun> measured = k < word.breaks().length ? Words.hyphenated(part, word.parts(), to) : part;
                min = Math.max(min, LinePlacer.width(measured, measurer, font, fontSize, letterSpacing));
                from = to;
            }
        }
        double natural = 0;
        int from = 0;
        for (int w = 0; w < words.size(); w++) {
            if (words.get(w).forced() || w == words.size() - 1) {
                List<TextRun> line = Words.line(words.subList(from, w + 1)).runs();
                natural = Math.max(natural, LinePlacer.width(line, measurer, font, fontSize, letterSpacing));
                from = w + 1;
            }
        }
        return new IntrinsicSizes(min, natural);
    }
}
