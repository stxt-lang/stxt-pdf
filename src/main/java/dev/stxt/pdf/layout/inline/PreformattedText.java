package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Breaks into lines a text with {@code Preformatted: true} or {@code wrap}
 * (reference/common/text-formatting.md): the spaces and the line breaks of the text are kept.
 *
 * <p>Each line of the text is a line of the box, the empty ones too (a blank line does not
 * separate paragraphs: the whole text is a single one for widows and orphans). A tab moves
 * to the next column that is a multiple of {@code Tab size}, counting the characters from the
 * start of the line; a final line break does not add an empty line. A forced line break
 * ({@link TextRun#LINE_SEPARATOR}, which a Markdown line break becomes) ends the line like any other line break
 * (reference/components/markdown.md). With {@code true} the
 * lines are not broken, so one wider than the box overflows; with {@code wrap} they are broken
 * between words (the spaces at the break are lost: they are not visible) and a word wider than the
 * box overflows.</p>
 *
 * <p>The first line of the text, and only it, starts after the {@code First line indent} of the box
 * (reference/common/typography.md): with {@code wrap} it is broken at what the indent leaves, and the indent counts
 * in the intrinsic sizes.</p>
 */
final class PreformattedText {

    private PreformattedText() {
    }

    /** The lines of the text for an available width, without a first line indent. */
    static List<LineBreaker.Line> lines(List<TextRun> runs, int tabSize, boolean wrap, double available,
                                        TextMeasurer measurer, FontFace font, double fontSize,
                                        double letterSpacing) {
        return lines(runs, tabSize, wrap, available, 0, measurer, font, fontSize, letterSpacing);
    }

    /**
     * The lines of the text for an available width, the first of them {@code firstLineIndent} shorter (longer, if it
     * is negative).
     */
    static List<LineBreaker.Line> lines(List<TextRun> runs, int tabSize, boolean wrap, double available,
                                        double firstLineIndent, TextMeasurer measurer, FontFace font,
                                        double fontSize, double letterSpacing) {
        List<LineBreaker.Line> lines = new ArrayList<>();
        for (List<TextRun> source : sourceLines(runs, tabSize)) {
            if (wrap) {
                double first = lines.isEmpty() ? available - firstLineIndent : available;
                lines.addAll(wrap(source, first, available, measurer, font, fontSize, letterSpacing));
            } else {
                lines.add(new LineBreaker.Line(source));
            }
        }
        return lines;
    }

    /** Narrowest width and natural width of a text without a first line indent. */
    static IntrinsicSizes intrinsics(List<TextRun> runs, int tabSize, boolean wrap, TextMeasurer measurer,
                                     FontFace font, double fontSize, double letterSpacing) {
        return intrinsics(runs, tabSize, wrap, 0, measurer, font, fontSize, letterSpacing);
    }

    /**
     * Narrowest width and natural width: with {@code true}, the widest line; with {@code wrap}, the
     * widest word and the widest line. The first line, and with {@code wrap} its first word, count with the
     * {@code firstLineIndent} added.
     */
    static IntrinsicSizes intrinsics(List<TextRun> runs, int tabSize, boolean wrap, double firstLineIndent,
                                     TextMeasurer measurer, FontFace font, double fontSize, double letterSpacing) {
        double min = 0;
        double max = 0;
        // What the next line or word starts after: the indent, until the first one is measured.
        double indent = firstLineIndent;
        for (List<TextRun> source : sourceLines(runs, tabSize)) {
            double width = LinePlacer.width(source, measurer, font, fontSize, letterSpacing);
            max = Math.max(max, width + indent);
            if (!wrap) {
                min = max;
                indent = 0;
                continue;
            }
            String text = TextRun.text(source);
            int[] starts = starts(source);
            for (int[] word : words(text)) {
                min = Math.max(min, LinePlacer.width(slice(source, starts, text, word[0], word[1]), measurer, font,
                        fontSize, letterSpacing) + indent);
                indent = 0;
            }
            indent = 0;
        }
        // With a negative indent, a single line is narrower than its widest word: the box is not.
        return new IntrinsicSizes(min, Math.max(min, max));
    }

    /**
     * The lines of the text with the tabs already expanded, each one as its runs. An empty line
     * has no runs.
     */
    static List<List<TextRun>> sourceLines(List<TextRun> runs, int tabSize) {
        List<List<TextRun>> lines = new ArrayList<>();
        TextRun.Joiner line = new TextRun.Joiner();
        int column = 0;
        for (TextRun run : runs) {
            String text = run.text();
            StringBuilder piece = new StringBuilder();
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (cp == '\n' || cp == TextRun.LINE_SEPARATOR) {
                    add(line, piece, run);
                    lines.add(line.build());
                    line = new TextRun.Joiner();
                    column = 0;
                } else if (cp == '\t') {
                    int columns = tabSize - column % tabSize;
                    piece.append(" ".repeat(columns));
                    column += columns;
                } else if (cp != '\r') {
                    piece.appendCodePoint(cp);
                    column++;
                }
            }
            add(line, piece, run);
        }
        lines.add(line.build());
        if (lines.size() > 1 && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    /**
     * The characters of a preformatted text that are not painted as they are: the space, the tab (spaces), the line
     * breaks, with the {@link TextRun#LINE_SEPARATOR}, and the carriage return, which is dropped
     * ({@link #sourceLines}). Any other white-space character stays in its line and is painted with its glyph.
     */
    static boolean isSpace(int cp) {
        return cp == ' ' || cp == '\t' || cp == '\n' || cp == '\r' || cp == TextRun.LINE_SEPARATOR;
    }

    private static void add(TextRun.Joiner line, StringBuilder piece, TextRun run) {
        if (piece.length() > 0) {
            line.add(piece.toString(), run.format());
            piece.setLength(0);
        }
    }

    /**
     * Breaks a {@code wrap} line with the greedy algorithm: a word moves to the next
     * line if, with it, the line does not fit and already had another word. The spaces before the
     * first word (the indent) are kept; those at the break are not. The first of its lines has {@code first} points
     * and the others, {@code available}.
     */
    private static List<LineBreaker.Line> wrap(List<TextRun> source, double first, double available,
                                               TextMeasurer measurer, FontFace font, double fontSize,
                                               double letterSpacing) {
        if (fits(LinePlacer.width(source, measurer, font, fontSize, letterSpacing), first)) {
            return List.of(new LineBreaker.Line(source));
        }
        String text = TextRun.text(source);
        int[] starts = starts(source);
        List<LineBreaker.Line> lines = new ArrayList<>();
        int lineStart = 0;
        int lastEnd = 0;
        for (int[] word : words(text)) {
            if (lastEnd > lineStart && !fits(LinePlacer.width(slice(source, starts, text, lineStart, word[1]),
                    measurer, font, fontSize, letterSpacing), lines.isEmpty() ? first : available)) {
                lines.add(new LineBreaker.Line(slice(source, starts, text, lineStart, lastEnd)));
                lineStart = word[0];
            }
            lastEnd = word[1];
        }
        lines.add(new LineBreaker.Line(slice(source, starts, text, lineStart, text.length())));
        return lines;
    }

    /** Where each run starts in the text of the line, and where the last one ends. */
    private static int[] starts(List<TextRun> runs) {
        int[] starts = new int[runs.size() + 1];
        for (int i = 0; i < runs.size(); i++) {
            starts[i + 1] = starts[i] + runs.get(i).text().length();
        }
        return starts;
    }

    /** The runs of the text between two positions, each one with its format. */
    private static List<TextRun> slice(List<TextRun> runs, int[] starts, String text, int from, int to) {
        return new Words.RunCursor(runs, starts).parts(text, from, to);
    }

    /** The words of the line (what is not a space), as {@code [start, end)}. */
    private static List<int[]> words(String text) {
        List<int[]> words = new ArrayList<>();
        for (int i = 0; i < text.length(); ) {
            if (text.charAt(i) == ' ') {
                i++;
                continue;
            }
            int j = i;
            while (j < text.length() && text.charAt(j) != ' ') {
                j++;
            }
            words.add(new int[]{i, j});
            i = j;
        }
        return words;
    }
}
