package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Breaks a text into lines (reference/common/typography.md). With {@code Line breaking: simple},
 * with the greedy algorithm (<em>first-fit</em>) of browsers; with {@code balanced}, with
 * Knuth-Plass ({@link #optimal}).
 *
 * <p>Handling of white space: the line breaks of the {@code Text} block and the sequences of spaces count as
 * one space, and the text is a single paragraph (a blank line is an error when reading it, except in a
 * preformatted text, which {@link PreformattedText} breaks). A word wider than the line takes a line of its own and
 * overflows.</p>
 *
 * <p><strong>Forced line breaks</strong> (reference/components/markdown.md). The
 * {@link TextRun#LINE_SEPARATOR} (U+2028), which a Markdown line break becomes, separates words like a space and ends
 * the line there: both algorithms end a line at the start of the word after it and no line goes past it
 * ({@link Word#forced()}). The line it ends is never justified ({@link Line#forced()}), like the last one.</p>
 *
 * <p>The text is a list of formatted runs ({@link TextRun}). A word can have
 * parts from several runs ({@code **neg**rita}) and measures the sum of its parts, each one with its
 * font; the space between two words has the format of the run that holds the first of its
 * white-space characters. A text without formatting is a single run and is measured the same as a string.</p>
 *
 * <p><strong>Hyphenation</strong> ({@code Hyphenate}, reference/common/typography.md). A word may be split at its
 * {@linkplain Word#breaks() break points}: where a soft hyphen (U+00AD) was written, or, with a
 * {@link Hyphenator}, where the patterns of the language allow it. A line that ends inside a word ends with the
 * {@link #HYPHEN} of the font, in the format of the letter before it, and {@link Line#hyphenated()} says so. The
 * places where a line may end are the <em>positions</em> of the text: the start of each word and each of its break
 * points ({@link Paragraph}); both algorithms choose among them. The greedy one adds whole words while they fit and,
 * when one does not, takes the longest part of it that fits with its hyphen; Knuth-Plass charges each hyphen the
 * penalty of TeX ({@value #HYPHEN_PENALTY}, squared) and two consecutive hyphenated lines
 * {@value #DOUBLE_HYPHEN_DEMERITS} more.</p>
 *
 * <p>Each character advances its width plus the letter spacing ({@code Letter spacing},
 * reference/common/text-formatting.md), that of its run if it is a styled run; the last one of the line does not carry
 * it, so the width of a line is the sum of its characters minus the letter spacing of the last one. A run in small caps is measured at
 * {@link TextFormat#SMALL_CAPS_SCALE} of the size.</p>
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
    /** A forced line break of the text ({@link TextRun#LINE_SEPARATOR}). */
    private static final char LINE_SEPARATOR = TextRun.LINE_SEPARATOR;
    private static final int[] NO_BREAKS = new int[0];

    /**
     * A word: its parts, with consecutive runs of the same format joined, the format
     * of the space that separates it from the next one, the positions (characters before them) where it may be
     * split with a hyphen, in order, never at its start or its end (empty if it cannot be split), and whether a
     * forced line break follows it ({@link TextRun#LINE_SEPARATOR}).
     */
    record Word(List<TextRun> parts, TextFormat gap, int[] breaks, boolean forced) {

        Word(List<TextRun> parts, TextFormat gap) {
            this(parts, gap, NO_BREAKS);
        }

        Word(List<TextRun> parts, TextFormat gap, int[] breaks) {
            this(parts, gap, breaks, false);
        }

        String text() {
            return TextRun.text(parts);
        }

        int length() {
            return text().length();
        }
    }

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

        /** A line at the start of the text. */
        public Line(List<TextRun> runs, boolean hyphenated) {
            this(runs, hyphenated, 0);
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

    /** Lines of the text for an available width, with the greedy algorithm. */
    public static List<String> breakLines(String text, double available, TextMeasurer measurer,
                                          FontFace font, double fontSize) {
        return breakLines(text, available, measurer, font, fontSize, LineBreaking.SIMPLE);
    }

    /** Like {@link #breakLines(List, double, TextMeasurer, FontFace, double, LineBreaking)}, for plain text. */
    public static List<String> breakLines(String text, double available, TextMeasurer measurer,
                                          FontFace font, double fontSize, LineBreaking wrap) {
        return breakLines(TextRun.plain(text), available, measurer, font, fontSize, wrap).stream()
                .map(Line::text).toList();
    }

    /** Like {@link #breakLines(String, double, TextMeasurer, FontFace, double, LineBreaking)}, hyphenating. */
    public static List<String> breakLines(String text, double available, TextMeasurer measurer,
                                          FontFace font, double fontSize, LineBreaking wrap, Hyphenator hyphenator) {
        return breakLines(TextRun.plain(text), available, measurer, font, fontSize, 0, wrap, hyphenator).stream()
                .map(Line::text).toList();
    }

    /**
     * Lines of the text for an available width. {@code font} is the font of the box; each run changes it with its
     * format.
     */
    public static List<Line> breakLines(List<TextRun> runs, double available, TextMeasurer measurer,
                                        FontFace font, double fontSize, LineBreaking wrap) {
        return breakLines(runs, available, measurer, font, fontSize, 0, wrap);
    }

    /** Like {@link #breakLines(List, double, TextMeasurer, FontFace, double, LineBreaking)}, with letter spacing. */
    public static List<Line> breakLines(List<TextRun> runs, double available, TextMeasurer measurer,
                                        FontFace font, double fontSize, double letterSpacing, LineBreaking wrap) {
        return breakLines(runs, available, measurer, font, fontSize, letterSpacing, wrap, null);
    }

    /**
     * Like {@link #breakLines(List, double, TextMeasurer, FontFace, double, double, LineBreaking)}, splitting words
     * with {@code hyphenator} where its patterns allow it; with {@code null}, only where the text has a soft hyphen.
     */
    public static List<Line> breakLines(List<TextRun> runs, double available, TextMeasurer measurer,
                                        FontFace font, double fontSize, double letterSpacing, LineBreaking wrap,
                                        Hyphenator hyphenator) {
        Paragraph paragraph = paragraph(runs, measurer, font, fontSize, letterSpacing, hyphenator);
        return paragraph == null ? List.of() : breakLines(paragraph, 0, LineWidths.uniform(available), wrap);
    }

    /**
     * The words of the text with their measures and positions ({@link Paragraph}), ready to be broken into lines
     * any number of times, from any position and with any line widths; {@code null} if the text has no words.
     */
    static Paragraph paragraph(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                               double letterSpacing, Hyphenator hyphenator) {
        List<Word> words = words(runs, hyphenator);
        return words.isEmpty() ? null : new Paragraph(words, measurer, font, fontSize, letterSpacing);
    }

    /**
     * Lines of the text from position {@code from} (0 is its start) with the width of each line given by
     * {@code widths} (reference/components/block.md): with the greedy algorithm or with Knuth-Plass. In a line narrower
     * than the ones after it, a word that does not fit alone leaves it empty and goes to the next one, instead of
     * overflowing.
     */
    static List<Line> breakLines(Paragraph p, int from, LineWidths widths, LineBreaking wrap) {
        return wrap == LineBreaking.BALANCED ? optimal(p, from, widths) : greedy(p, from, widths);
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

    /** Like {@link #intrinsics(List, TextMeasurer, FontFace, double)}, for plain text. */
    public static IntrinsicSizes intrinsics(String text, TextMeasurer measurer, FontFace font, double fontSize) {
        return intrinsics(TextRun.plain(text), measurer, font, fontSize);
    }

    /**
     * Narrowest width (widest word) and natural width (the whole text unbroken, or, with forced line breaks, its
     * widest line).
     */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize) {
        return intrinsics(runs, measurer, font, fontSize, 0);
    }

    /** Like {@link #intrinsics(List, TextMeasurer, FontFace, double)}, with letter spacing. */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize, double letterSpacing) {
        return intrinsics(runs, measurer, font, fontSize, letterSpacing, null);
    }

    /**
     * Like {@link #intrinsics(List, TextMeasurer, FontFace, double, double)}, hyphenating: the narrowest width is the
     * widest part of a word that cannot be split further, with its hyphen if it is not the last part.
     */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize, double letterSpacing, Hyphenator hyphenator) {
        List<Word> words = words(runs, hyphenator);
        if (words.isEmpty()) {
            return IntrinsicSizes.ZERO;
        }
        double min = 0;
        for (Word word : words) {
            if (word.breaks().length == 0) {
                min = Math.max(min, width(word.parts(), measurer, font, fontSize, letterSpacing));
                continue;
            }
            int from = 0;
            for (int k = 0; k <= word.breaks().length; k++) {
                int to = k < word.breaks().length ? word.breaks()[k] : word.length();
                List<TextRun> part = slice(word.parts(), from, to);
                min = Math.max(min, width(k < word.breaks().length ? hyphenated(part, word.parts(), to) : part,
                        measurer, font, fontSize, letterSpacing));
                from = to;
            }
        }
        double natural = 0;
        int from = 0;
        for (int w = 0; w < words.size(); w++) {
            if (words.get(w).forced() || w == words.size() - 1) {
                natural = Math.max(natural, width(line(words.subList(from, w + 1)).runs(), measurer, font, fontSize,
                        letterSpacing));
                from = w + 1;
            }
        }
        return new IntrinsicSizes(min, natural);
    }

    /** Horizontal offset of a line according to the alignment. */
    public static double offset(double lineWidth, double available, TextAlign textAlign) {
        double remaining = Math.max(0, available - lineWidth);
        return switch (textAlign) {
            case LEFT, JUSTIFY -> 0;
            case RIGHT -> remaining;
            case CENTER -> remaining / 2;
        };
    }

    /**
     * Extra space for each space between words with {@code justify}: the leftover of the line
     * distributed among its spaces. The last line of the text and the lines of a single
     * word are not justified (they are aligned to the left), and a line that overflows is not
     * compressed.
     *
     * @param line      text of the line, with the words separated by a space
     * @param lineWidth natural width of the line
     * @param last      it is the last line of the text
     */
    public static double wordSpacing(String line, double lineWidth, double available, TextAlign textAlign,
                                     boolean last) {
        if (textAlign != TextAlign.JUSTIFY || last) {
            return 0;
        }
        long spaces = spaces(line);
        return spaces == 0 ? 0 : Math.max(0, available - lineWidth) / spaces;
    }

    /** Like {@link #place(List, TextMeasurer, Style, double, boolean, double, double)}, for plain text. */
    public static TextLine place(String text, TextMeasurer measurer, Style s, double available, boolean justify,
                                 double y, double height) {
        return place(TextRun.plain(text), measurer, s, available, justify, y, height);
    }

    /** Like {@link #place(List, TextMeasurer, Style, double, boolean, double, double, boolean)}, not hyphenated. */
    public static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double available,
                                 boolean justify, double y, double height) {
        return place(runs, measurer, s, available, justify, y, height, false);
    }

    /**
     * A line placed in the content box (reference/common/text-formatting.md): each run measured with the
     * font of {@code s} and its format, the line aligned in {@code available} and, if justified, with the extra space
     * between words added to its width and to that of each run.
     *
     * @param runs       runs of the line, with the words separated by a space
     * @param justify    whether the line can be justified: it is not the last one of the text (when numbering
     *                   the pages, whether it was already justified)
     * @param hyphenated whether the line ends inside a word, with a hyphen ({@link Line#hyphenated()})
     */
    public static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double available,
                                 boolean justify, double y, double height, boolean hyphenated) {
        return place(runs, measurer, s, s.typography().fontSize(), available, justify, y, height, hyphenated);
    }

    /**
     * Like {@link #place(List, TextMeasurer, Style, double, boolean, double, double, boolean)}, with the text at
     * {@code fontSize} instead of at the {@code Size} of {@code s}: the size {@code Overflow: shrink} reduced it to
     * (reference/common/overflow.md). The line keeps it ({@link TextLine#fontSize()}).
     */
    public static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double fontSize, double available,
                                 boolean justify, double y, double height, boolean hyphenated) {
        return place(runs, measurer, s, fontSize, 0, 0, available, justify, y, height, hyphenated);
    }

    /**
     * Like {@link #place(List, TextMeasurer, Style, double, double, boolean, double, double, boolean)}, in a line box
     * that starts {@code left} points after the content box and ends {@code right} points before its end
     * (reference/components/block.md): {@code available} is the width of that line box, and the line is aligned in it.
     */
    public static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double fontSize, double left,
                                 double right, double available, boolean justify, double y, double height,
                                 boolean hyphenated) {
        FontFace font = s.typography().fontFace();
        double letterSpacing = s.typography().letterSpacing();
        double[] widths = new double[runs.size()];
        double natural = 0;
        for (int i = 0; i < widths.length; i++) {
            TextRun r = runs.get(i);
            widths[i] = advance(r.text(), r.format(), measurer, font, fontSize, letterSpacing);
            natural += widths[i];
        }
        // The last character of the line carries no letter spacing (an empty line, of a preformatted text, has none).
        double trailing = trailing(runs, letterSpacing);
        natural -= trailing;
        String text = TextRun.text(runs);
        double x = offset(natural, available, s.typography().textAlign());
        double spacing = wordSpacing(text, natural, available, s.typography().textAlign(), !justify);
        List<TextLine.Run> placed = new ArrayList<>(runs.size());
        double at = 0;
        for (int i = 0; i < widths.length; i++) {
            TextRun r = runs.get(i);
            double w = widths[i] + spacing * spaces(r.text()) - (i == widths.length - 1 ? trailing : 0);
            placed.add(new TextLine.Run(at, w, r.text(), r.format()));
            at += w;
        }
        return new TextLine(left + x, y, natural + spacing * spaces(text), height, fontSize, text, spacing, placed,
                hyphenated, left, right);
    }

    /**
     * Width of some runs as a line: each one with its font and its size, and the letter spacing between
     * their characters but not after the last one.
     */
    static double width(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                        double letterSpacing) {
        return advance(runs, measurer, font, fontSize, letterSpacing) - trailing(runs, letterSpacing);
    }

    /**
     * The letter spacing of the last character of some runs, which a line does not carry: that of its run
     * ({@link TextFormat#letterSpacing(double)}, with {@code letterSpacing} that of the box); 0 without runs.
     */
    static double trailing(List<TextRun> runs, double letterSpacing) {
        return runs.isEmpty() ? 0 : runs.get(runs.size() - 1).format().letterSpacing(letterSpacing);
    }

    /** Advance of some runs: their width plus the letter spacing of each character, the last one's included. */
    static double advance(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                          double letterSpacing) {
        double width = 0;
        for (TextRun r : runs) {
            width += advance(r.text(), r.format(), measurer, font, fontSize, letterSpacing);
        }
        return width;
    }

    /**
     * Advance of a text with that format: with the letter spacing of its run, {@code letterSpacing} (that of the box)
     * if its style does not declare one.
     */
    static double advance(String text, TextFormat format, TextMeasurer measurer, FontFace font, double fontSize,
                          double letterSpacing) {
        return measurer.width(text, format.face(font), fontSize * format.scale())
                + format.letterSpacing(letterSpacing) * text.codePointCount(0, text.length());
    }

    /** The words of the text, with the break points of their soft hyphens only. */
    static List<Word> words(List<TextRun> runs) {
        return words(runs, null);
    }

    /**
     * The words of the text (see the class description): what lies between the white-space characters
     * ({@link #isSpace}, the {@code \s} of regular expressions, and the {@link TextRun#LINE_SEPARATOR}), with one pass
     * over the text, each one with its break points: those of its soft hyphens, which are removed from it, or, if it
     * has none and there is a {@code hyphenator}, those of the patterns of the language ({@link #breaks}). A word with
     * a line separator in the white space after it, and a word after it, is {@linkplain Word#forced() forced}.
     */
    static List<Word> words(List<TextRun> runs, Hyphenator hyphenator) {
        String text = TextRun.text(runs);
        // starts[i]: position in the text where run i starts.
        int[] starts = new int[runs.size() + 1];
        for (int i = 0; i < runs.size(); i++) {
            starts[i + 1] = starts[i] + runs.get(i).text().length();
        }
        RunCursor cursor = new RunCursor(runs, starts);
        List<Word> result = new ArrayList<>();
        Word previous = null;
        int previousEnd = 0;
        int n = text.length();
        int i = 0;
        while (true) {
            boolean forced = false;
            while (i < n && isSpace(text.charAt(i))) {
                forced |= text.charAt(i) == LINE_SEPARATOR;
                i++;
            }
            if (i == n) {
                break;
            }
            int start = i;
            while (i < n && !isSpace(text.charAt(i))) {
                i++;
            }
            if (previous != null) {
                result.add(new Word(previous.parts(), cursor.format(previousEnd), previous.breaks(), forced));
            }
            previous = word(cursor.parts(text, start, i), hyphenator);
            previousEnd = i;
        }
        if (previous != null) {
            result.add(previous);
        }
        return result;
    }

    /** A word from its parts, with its break points and without its soft hyphens; its gap is not known yet. */
    private static Word word(List<TextRun> parts, Hyphenator hyphenator) {
        String text = TextRun.text(parts);
        if (text.indexOf(SOFT_HYPHEN) >= 0) {
            return softHyphenated(parts);
        }
        if (hyphenator == null) {
            return new Word(parts, TextFormat.PLAIN);
        }
        return new Word(parts, TextFormat.PLAIN, breaks(text, hyphenator));
    }

    /**
     * The word without its soft hyphens, which become its break points: an author who writes them decides where the
     * word may be split, and the patterns do not add more (a soft hyphen at the start or the end of the word, or two
     * together, add no break point).
     */
    private static Word softHyphenated(List<TextRun> parts) {
        TextRun.Joiner clean = new TextRun.Joiner();
        List<Integer> breaks = new ArrayList<>();
        int length = 0;
        for (TextRun part : parts) {
            String text = part.text();
            int from = 0;
            for (int at = text.indexOf(SOFT_HYPHEN); at >= 0; at = text.indexOf(SOFT_HYPHEN, from)) {
                if (at > from) {
                    clean.add(text.substring(from, at), part.format());
                    length += at - from;
                }
                if (length > 0 && (breaks.isEmpty() || breaks.get(breaks.size() - 1) != length)) {
                    breaks.add(length);
                }
                from = at + 1;
            }
            if (from < text.length()) {
                clean.add(text.substring(from), part.format());
                length += text.length() - from;
            }
        }
        if (!breaks.isEmpty() && breaks.get(breaks.size() - 1) == length) {
            breaks.remove(breaks.size() - 1);
        }
        return new Word(clean.build(), TextFormat.PLAIN, breaks.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * The break points of a word by the patterns of the language: those of its letters, which have to be all
     * together (what goes before them, like an opening quotation mark, and after them, like a period, stays with the
     * first and the last part and does not count for the minima). A word with something that is not a letter among
     * its letters (a digit, a hyphen, a middle dot) is not split, nor is one with a brace, which may hold
     * {@code {page}}.
     */
    static int[] breaks(String text, Hyphenator hyphenator) {
        if (text.indexOf('{') >= 0 || text.indexOf('}') >= 0) {
            return NO_BREAKS;
        }
        int from = 0;
        while (from < text.length() && !Character.isLetter(text.codePointAt(from))) {
            from += Character.charCount(text.codePointAt(from));
        }
        int to = text.length();
        while (to > from && !Character.isLetter(text.codePointBefore(to))) {
            to -= Character.charCount(text.codePointBefore(to));
        }
        if (from >= to) {
            return NO_BREAKS;
        }
        for (int i = from; i < to; ) {
            int cp = text.codePointAt(i);
            if (!Character.isLetter(cp)) {
                return NO_BREAKS;
            }
            i += Character.charCount(cp);
        }
        int[] breaks = hyphenator.breaks(text.substring(from, to));
        for (int i = 0; i < breaks.length; i++) {
            breaks[i] += from;
        }
        return breaks;
    }

    /**
     * The white-space characters that separate words: those of {@code \s} ({@code [ \t\n\x0B\f\r]}) and the
     * {@link TextRun#LINE_SEPARATOR}.
     */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r' || c == LINE_SEPARATOR;
    }

    /** The runs of the characters {@code from} to {@code to} of some runs, each part with its format. */
    static List<TextRun> slice(List<TextRun> parts, int from, int to) {
        TextRun.Joiner out = new TextRun.Joiner();
        int at = 0;
        for (TextRun part : parts) {
            int start = Math.max(from, at);
            int end = Math.min(to, at + part.text().length());
            if (start < end) {
                out.add(part.text().substring(start - at, end - at), part.format());
            }
            at += part.text().length();
        }
        return out.build();
    }

    /**
     * The format of the hyphen that ends a line after character {@code offset} of a word: that of the letter before
     * it, without small caps (a hyphen has no case and is painted at the size of the box).
     */
    static TextFormat hyphenFormat(List<TextRun> parts, int offset) {
        int at = 0;
        for (TextRun part : parts) {
            at += part.text().length();
            if (offset <= at) {
                return part.format().withSmallCaps(false);
            }
        }
        return parts.get(parts.size() - 1).format().withSmallCaps(false);
    }

    /**
     * A part of a word that ends at break position {@code offset} of its {@code parts}, followed by its hyphen: joined
     * to the last run if it has the format of the hyphen, as {@link Paragraph#line} joins them.
     */
    static List<TextRun> hyphenated(List<TextRun> part, List<TextRun> parts, int offset) {
        TextRun.Joiner runs = new TextRun.Joiner();
        for (TextRun run : part) {
            runs.add(run.text(), run.format());
        }
        runs.add(HYPHEN, hyphenFormat(parts, offset));
        return runs.build();
    }

    /** Walks the runs forward: the positions asked of it never go back. */
    static final class RunCursor {
        private final List<TextRun> runs;
        private final int[] starts;
        private int run;

        RunCursor(List<TextRun> runs, int[] starts) {
            this.runs = runs;
            this.starts = starts;
        }

        /** Format of the character at position {@code index}. */
        TextFormat format(int index) {
            if (runs.size() == 1) {
                // Plain text, the usual case: there is no run to look for.
                return runs.get(0).format();
            }
            while (starts[run + 1] <= index) {
                run++;
            }
            return runs.get(run).format();
        }

        /** The parts of the text between {@code start} and {@code end}, each one with its format. */
        List<TextRun> parts(String text, int start, int end) {
            TextFormat first = format(start);
            if (end <= starts[run + 1]) {
                // All in one run, the usual case: nothing to join.
                return List.of(new TextRun(text.substring(start, end), first));
            }
            TextRun.Joiner parts = new TextRun.Joiner();
            for (int i = start; i < end; ) {
                TextFormat f = format(i);
                int stop = Math.min(end, starts[run + 1]);
                parts.add(text.substring(i, stop), f);
                i = stop;
            }
            return parts.build();
        }
    }

    /**
     * The words of a text with their measures and the positions where a line may end: the start of each word, each
     * of its break points and the end of the text, in order. A line goes from one position to a later one; if it
     * ends at a break point, it ends with a hyphen. Advances are sums of those of the whole words, the spaces and the
     * parts of a split word, each one measured once.
     */
    static final class Paragraph {
        private final List<Word> words;
        private final TextMeasurer measurer;
        private final FontFace font;
        private final double fontSize;
        private final double letterSpacing;
        /** wholeWords[w]: advance of the words 0 to w-1. */
        private final double[] wholeWords;
        /** gaps[w]: advance of the spaces after the words 0 to w-1. */
        private final double[] gaps;
        /** The word of each position and the characters of it before the position (0 = its start). */
        private final int[] posWord;
        private final int[] posOffset;
        /** start[w]: the position of the start of word w; start[n], the end of the text. */
        private final int[] start;
        /** Memorized advances of the part of the word before a break position, with its hyphen, and after it. */
        private final double[] head;
        private final double[] tail;

        Paragraph(List<Word> words, TextMeasurer measurer, FontFace font, double fontSize, double letterSpacing) {
            this.words = words;
            this.measurer = measurer;
            this.font = font;
            this.fontSize = fontSize;
            this.letterSpacing = letterSpacing;
            int n = words.size();
            wholeWords = new double[n + 1];
            gaps = new double[n + 1];
            start = new int[n + 1];
            int positions = 1;
            for (int w = 0; w < n; w++) {
                Word word = words.get(w);
                wholeWords[w + 1] = wholeWords[w] + LineBreaker.advance(word.parts(), measurer, font, fontSize, letterSpacing);
                gaps[w + 1] = gaps[w] + (w + 1 < n ? LineBreaker.advance(" ", word.gap(), measurer, font, fontSize, letterSpacing) : 0);
                start[w] = positions - 1;
                positions += 1 + word.breaks().length;
            }
            start[n] = positions - 1;
            posWord = new int[positions];
            posOffset = new int[positions];
            int p = 0;
            for (int w = 0; w < n; w++) {
                posWord[p] = w;
                posOffset[p++] = 0;
                for (int b : words.get(w).breaks()) {
                    posWord[p] = w;
                    posOffset[p++] = b;
                }
            }
            posWord[p] = n;
            posOffset[p] = 0;
            head = new double[positions];
            tail = new double[positions];
            Arrays.fill(head, Double.NaN);
            Arrays.fill(tail, Double.NaN);
        }

        int words() {
            return words.size();
        }

        int positions() {
            return posWord.length;
        }

        /** The position of the start of word {@code w}; with {@code w} = the number of words, the end of the text. */
        int start(int w) {
            return start[w];
        }

        /** The word position {@code p} is in (the number of words, at the end of the text). */
        int word(int p) {
            return posWord[p];
        }

        /** Whether position {@code p} is inside a word: a line that ends there ends with a hyphen. */
        boolean hyphen(int p) {
            return posOffset[p] > 0;
        }

        /** Whether a forced line break comes right before position {@code p}: a line has to end there. */
        boolean forced(int p) {
            int w = posWord[p];
            return posOffset[p] == 0 && w > 0 && w < words.size() && words.get(w - 1).forced();
        }

        /** The last word of a line that ends at {@code b}, and how many of its characters the line has. */
        private int lastWord(int b) {
            return hyphen(b) ? posWord[b] : posWord[b] - 1;
        }

        private int lastOffset(int b) {
            return hyphen(b) ? posOffset[b] : words.get(posWord[b] - 1).length();
        }

        /** Whether the line from {@code a} to {@code b} is a single part of a single word, which may overflow. */
        boolean single(int a, int b) {
            return posWord[a] == lastWord(b);
        }

        /** Advance of the spaces of the line from {@code a} to {@code b}. */
        double spaces(int a, int b) {
            int last = lastWord(b);
            return last > posWord[a] ? gaps[last] - gaps[posWord[a]] : 0;
        }

        /**
         * Advance of the line from {@code a} to {@code b}: its words, spaces and hyphen, with the letter spacing of
         * every character, the last one's included.
         */
        double advance(int a, int b) {
            int first = posWord[a];
            int last = lastWord(b);
            if (first == last) {
                return hyphen(b) ? withHyphen(first, posOffset[a], posOffset[b])
                        : piece(first, posOffset[a], lastOffset(b));
            }
            double sum = posOffset[a] == 0 ? wholeWords[first + 1] - wholeWords[first] : tail(a);
            sum += wholeWords[last] - wholeWords[first + 1];
            sum += gaps[last] - gaps[first];
            sum += hyphen(b) ? head(b) : wholeWords[last + 1] - wholeWords[last];
            return sum;
        }

        /** Width of the line from {@code a} to {@code b}: its advance without the letter spacing of its last character. */
        double width(int a, int b) {
            return advance(a, b) - trailing(b);
        }

        /**
         * The letter spacing of the last character of a line that ends at {@code b}: that of the run of the hyphen, if
         * it ends with one, or of the last part of its last word.
         */
        private double trailing(int b) {
            TextFormat last = hyphen(b) ? hyphenFormat(words.get(posWord[b]).parts(), posOffset[b])
                    : lastPart(words.get(posWord[b] - 1)).format();
            return last.letterSpacing(letterSpacing);
        }

        private static TextRun lastPart(Word word) {
            return word.parts().get(word.parts().size() - 1);
        }

        /**
         * Whether the line from {@code a} to {@code b} fits, measuring it whole when its sum is within
         * {@link Tolerance#MODEL} of the width.
         */
        boolean fitsMeasured(int a, int b, double available) {
            double width = width(a, b);
            if (Math.abs(width - available) <= Tolerance.MODEL) {
                width = LineBreaker.width(line(a, b).runs(), measurer, font, fontSize, letterSpacing);
            }
            return fits(width, available);
        }

        /** Advance of the part of its word before break position {@code b}, with its hyphen. */
        private double head(int b) {
            if (Double.isNaN(head[b])) {
                head[b] = withHyphen(posWord[b], 0, posOffset[b]);
            }
            return head[b];
        }

        /** Advance of the part of its word after break position {@code a}. */
        private double tail(int a) {
            if (Double.isNaN(tail[a])) {
                tail[a] = piece(posWord[a], posOffset[a], words.get(posWord[a]).length());
            }
            return tail[a];
        }

        private double piece(int w, int from, int to) {
            Word word = words.get(w);
            if (from == 0 && to == word.length()) {
                return wholeWords[w + 1] - wholeWords[w];
            }
            return LineBreaker.advance(slice(word.parts(), from, to), measurer, font, fontSize, letterSpacing);
        }

        /**
         * Advance of the part of word {@code w} from {@code from} to break position {@code to}, with its hyphen,
         * measured together as the line paints them: with kerning, the last letter and the hyphen are a pair.
         */
        private double withHyphen(int w, int from, int to) {
            List<TextRun> parts = words.get(w).parts();
            return LineBreaker.advance(hyphenated(slice(parts, from, to), parts, to), measurer, font, fontSize,
                    letterSpacing);
        }

        /** The line from {@code a} to {@code b}: its words separated by a space with the format of the previous one. */
        Line line(int a, int b) {
            int first = posWord[a];
            int last = lastWord(b);
            TextRun.Joiner runs = new TextRun.Joiner();
            for (int w = first; w <= last; w++) {
                Word word = words.get(w);
                if (w > first) {
                    runs.add(" ", words.get(w - 1).gap());
                }
                int from = w == first ? posOffset[a] : 0;
                int to = w == last ? lastOffset(b) : word.length();
                for (TextRun part : from == 0 && to == word.length() ? word.parts() : slice(word.parts(), from, to)) {
                    runs.add(part.text(), part.format());
                }
            }
            if (hyphen(b)) {
                runs.add(HYPHEN, hyphenFormat(words.get(last).parts(), posOffset[b]));
            }
            return new Line(runs.build(), hyphen(b), a, forced(b));
        }
    }

    /** The words in a line, separated by a space with the format of the previous word. */
    static Line line(List<Word> words) {
        TextRun.Joiner runs = new TextRun.Joiner();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                runs.add(" ", words.get(i - 1).gap());
            }
            for (TextRun part : words.get(i).parts()) {
                runs.add(part.text(), part.format());
            }
        }
        return new Line(runs.build());
    }

    private static long spaces(String text) {
        return text.chars().filter(c -> c == ' ').count();
    }
}
