package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <p>The text is a list of formatted runs ({@link TextRun}). A word can have
 * parts from several runs ({@code **neg**rita}) and measures the sum of its parts, each one with its
 * font; the space between two words has the format of the run that holds the first of its
 * white-space characters. A text without formatting is a single run and is measured the same as a string.</p>
 *
 * <p>Each character advances its width plus the letter spacing ({@code Letter spacing},
 * reference/common/text-formatting.md); the last one of the line does not carry it, so the width of a line is the
 * sum of its characters minus one letter spacing. A run in small caps is measured at
 * {@link TextFormat#SMALL_CAPS_SCALE} of the size.</p>
 */
public final class LineBreaker {

    /** Demerits of each line besides those of its badness: it favors line breakings with fewer lines. */
    private static final double LINE_PENALTY = 10;
    /** Badness of a line that cannot be stretched enough or that overflows. */
    private static final double MAX_BADNESS = 10_000;
    /** How much each space can stretch, in proportion to its width (TeX's value). */
    private static final double STRETCH = 0.5;

    /**
     * A word: its parts, with consecutive runs of the same format joined, and the format
     * of the space that separates it from the next one.
     */
    record Word(List<TextRun> parts, TextFormat gap) {
    }

    /** A line of the line breaking: its runs, with consecutive ones of the same format joined. */
    public record Line(List<TextRun> runs) {

        public Line {
            runs = List.copyOf(runs);
        }

        public String text() {
            return TextRun.text(runs);
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
        List<Word> words = words(runs);
        if (words.isEmpty()) {
            return List.of();
        }
        return wrap == LineBreaking.BALANCED
                ? optimal(words, available, measurer, font, fontSize, letterSpacing)
                : greedy(words, available, measurer, font, fontSize, letterSpacing);
    }

    /**
     * The greedy line breaking: each word goes to the current line if it fits and, if not, starts another. The width of
     * the current line is kept by adding up the advances of its words and spaces, each one measured once; only
     * when the sum is within {@link Tolerance#MODEL} of the available width is the line built and measured
     * whole, so the decision is the same as measuring it every time, regardless of the rounding of the sum.
     */
    private static List<Line> greedy(List<Word> words, double available, TextMeasurer measurer, FontFace font,
                                     double fontSize, double letterSpacing) {
        Map<TextFormat, Double> spaces = new HashMap<>();
        List<Line> lines = new ArrayList<>();
        int start = 0;
        // Advance of the words start..i-1 and of their spaces, with the letter spacing of all their characters.
        double advance = 0;
        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            double own = advance(word.parts(), measurer, font, fontSize, letterSpacing);
            if (i == start) {
                advance = own;
                continue;
            }
            double candidate = advance + spaces.computeIfAbsent(words.get(i - 1).gap(),
                    f -> advance(" ", f, measurer, font, fontSize, letterSpacing)) + own;
            double width = candidate - letterSpacing;
            if (Math.abs(width - available) <= Tolerance.MODEL) {
                width = width(line(words.subList(start, i + 1)).runs(), measurer, font, fontSize, letterSpacing);
            }
            if (fits(width, available)) {
                advance = candidate;
            } else {
                lines.add(line(words.subList(start, i)));
                start = i;
                advance = own;
            }
        }
        if (start < words.size()) {
            lines.add(line(words.subList(start, words.size())));
        }
        return lines;
    }

    /**
     * Simplified Knuth-Plass: the breaks of the text that minimize the sum of the demerits
     * of its lines, by dynamic programming. Without hyphens or shrinking of the spaces
     * (justified lines are not compressed):
     *
     * <ul>
     *   <li>{@code badness = 100 × r³}, with {@code r} = leftover / stretch of the line
     *       (each space stretches up to half its width), up to {@value #MAX_BADNESS}.
     *       A line without spaces that does not fill the width, or a word that overflows, has the
     *       maximum badness. The last line has no badness.</li>
     *   <li>{@code demerits = (line penalty + badness)²}.</li>
     * </ul>
     *
     * <p>The width of a line is the sum of those of its words and spaces.</p>
     */
    static List<Line> optimal(List<Word> words, double available, TextMeasurer measurer,
                              FontFace font, double fontSize, double letterSpacing) {
        int n = words.size();
        Map<TextFormat, Double> spaces = new HashMap<>();
        // prefix[i]: advance of the first i words; gaps[i]: that of the first i spaces (both,
        // with the letter spacing of each character, the last one's included).
        double[] prefix = new double[n + 1];
        double[] gaps = new double[n];
        for (int i = 0; i < n; i++) {
            Word w = words.get(i);
            prefix[i + 1] = prefix[i] + advance(w.parts(), measurer, font, fontSize, letterSpacing);
            if (i + 1 < n) {
                gaps[i + 1] = gaps[i] + spaces.computeIfAbsent(w.gap(),
                        f -> advance(" ", f, measurer, font, fontSize, letterSpacing));
            }
        }
        // best[j]: minimum demerits of the lines with the first j words; from[j]: first
        // word of the last of those lines.
        double[] best = new double[n + 1];
        int[] from = new int[n + 1];
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        best[0] = 0;
        for (int j = 1; j <= n; j++) {
            for (int i = j - 1; i >= 0; i--) {
                boolean spaced = j - i > 1;
                double space = gaps[j - 1] - gaps[i];
                double width = prefix[j] - prefix[i] + space - letterSpacing;
                if (spaced && !fits(width, available)) {
                    // With more words it would be even wider; a single one may overflow.
                    break;
                }
                double d = best[i] + demerits(available - width, space * STRETCH, j == n);
                if (d < best[j]) {
                    best[j] = d;
                    from[j] = i;
                }
            }
        }
        List<Line> lines = new ArrayList<>();
        for (int j = n; j > 0; j = from[j]) {
            lines.add(0, line(words.subList(from[j], j)));
        }
        return lines;
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

    /** Narrowest width (widest word) and natural width (the whole text unbroken). */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize) {
        return intrinsics(runs, measurer, font, fontSize, 0);
    }

    /** Like {@link #intrinsics(List, TextMeasurer, FontFace, double)}, with letter spacing. */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize, double letterSpacing) {
        List<Word> words = words(runs);
        if (words.isEmpty()) {
            return IntrinsicSizes.ZERO;
        }
        double min = 0;
        for (Word word : words) {
            min = Math.max(min, width(word.parts(), measurer, font, fontSize, letterSpacing));
        }
        return new IntrinsicSizes(min, width(line(words).runs(), measurer, font, fontSize, letterSpacing));
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

    /**
     * A line placed in the content box (reference/common/text-formatting.md): each run measured with the
     * font of {@code s} and its format, the line aligned in {@code available} and, if justified, with the extra space
     * between words added to its width and to that of each run.
     *
     * @param runs    runs of the line, with the words separated by a space
     * @param justify whether the line can be justified: it is not the last one of the text (when numbering
     *                the pages, whether it was already justified)
     */
    public static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double available,
                                 boolean justify, double y, double height) {
        FontFace font = s.typography().fontFace();
        double[] widths = new double[runs.size()];
        double natural = 0;
        for (int i = 0; i < widths.length; i++) {
            TextRun r = runs.get(i);
            widths[i] = advance(r.text(), r.format(), measurer, font, s.typography().fontSize(), s.typography().letterSpacing());
            natural += widths[i];
        }
        // The last character of the line carries no letter spacing (an empty line, of a preformatted text, has none).
        if (!runs.isEmpty()) {
            natural -= s.typography().letterSpacing();
        }
        String text = TextRun.text(runs);
        double x = offset(natural, available, s.typography().textAlign());
        double spacing = wordSpacing(text, natural, available, s.typography().textAlign(), !justify);
        List<TextLine.Run> placed = new ArrayList<>(runs.size());
        double at = 0;
        for (int i = 0; i < widths.length; i++) {
            TextRun r = runs.get(i);
            double w = widths[i] + spacing * spaces(r.text()) - (i == widths.length - 1 ? s.typography().letterSpacing() : 0);
            placed.add(new TextLine.Run(at, w, r.text(), r.format()));
            at += w;
        }
        return new TextLine(x, y, natural + spacing * spaces(text), height, text, spacing, placed);
    }

    /**
     * Width of some runs as a line: each one with its font and its size, and the letter spacing between
     * their characters but not after the last one.
     */
    static double width(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                        double letterSpacing) {
        return advance(runs, measurer, font, fontSize, letterSpacing) - letterSpacing;
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

    /** Advance of a text with that format. */
    static double advance(String text, TextFormat format, TextMeasurer measurer, FontFace font, double fontSize,
                          double letterSpacing) {
        return measurer.width(text, format.face(font), fontSize * format.scale())
                + letterSpacing * text.codePointCount(0, text.length());
    }

    /**
     * The words of the text (see the class description): what lies between the white-space characters
     * ({@link #isSpace}, the {@code \s} of regular expressions), with one pass over the text.
     */
    static List<Word> words(List<TextRun> runs) {
        String text = TextRun.text(runs);
        // starts[i]: position in the text where run i starts.
        int[] starts = new int[runs.size() + 1];
        for (int i = 0; i < runs.size(); i++) {
            starts[i + 1] = starts[i] + runs.get(i).text().length();
        }
        RunCursor cursor = new RunCursor(runs, starts);
        List<Word> result = new ArrayList<>();
        List<TextRun> previous = null;
        int previousEnd = 0;
        int n = text.length();
        int i = 0;
        while (true) {
            while (i < n && isSpace(text.charAt(i))) {
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
                result.add(new Word(previous, cursor.format(previousEnd)));
            }
            previous = cursor.parts(text, start, i);
            previousEnd = i;
        }
        if (previous != null) {
            result.add(new Word(previous, TextFormat.PLAIN));
        }
        return result;
    }

    /** The white-space characters that separate words: those of {@code \s} ({@code [ \t\n\x0B\f\r]}). */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
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
