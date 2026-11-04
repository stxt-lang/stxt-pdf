package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of a line (reference/common/text-formatting.md): the measures of its runs, its alignment, the extra
 * space between words with {@code justify} and the line placed in the content box ({@link #place}).
 *
 * <p>Each character advances its width plus the letter spacing ({@code Letter spacing}), that of its run if it is a
 * styled run; the last one of the line does not carry it, so the width of a line is the sum of its characters minus
 * the letter spacing of the last one ({@link #width}). A run in small caps is measured at
 * {@link TextFormat#SMALL_CAPS_SCALE} of the size, and a run that is an image, at the width its proportion gives.</p>
 */
final class LinePlacer {

    private LinePlacer() {
    }

    /**
     * A line placed in the content box: each run measured with the font of {@code s} and its format, at
     * {@code fontSize} (the {@code Size} of {@code s} or the one {@code Overflow: shrink} reduced it to,
     * reference/common/overflow.md, which the line keeps in {@link TextLine#fontSize()}), the line aligned in a line
     * box that starts {@code left} points after the content box and ends {@code right} points before its end
     * (reference/components/block.md) and, if justified, with the extra space between words added to its width and
     * to that of each run.
     *
     * @param runs       runs of the line, with the words separated by a space
     * @param available  width of the line box
     * @param justify    whether the line can be justified: it is not the last one of the text (when numbering
     *                   the pages, whether it was already justified)
     * @param hyphenated whether the line ends inside a word, with a hyphen ({@link LineBreaker.Line#hyphenated()})
     */
    static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double fontSize, double left,
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

    /** Horizontal offset of a line according to the alignment. */
    static double offset(double lineWidth, double available, TextAlign textAlign) {
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
    static double wordSpacing(String line, double lineWidth, double available, TextAlign textAlign, boolean last) {
        if (textAlign != TextAlign.JUSTIFY || last) {
            return 0;
        }
        long spaces = spaces(line);
        return spaces == 0 ? 0 : Math.max(0, available - lineWidth) / spaces;
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
        if (format.image() != null) {
            // An image is as tall as the size the line is set at, and as wide as its proportion gives; it has no
            // letter spacing (reference/common/text-formatting.md).
            return measurer.imageWidth(format.image(), fontSize);
        }
        return measurer.width(text, format.face(font), fontSize * format.scale())
                + format.letterSpacing(letterSpacing) * text.codePointCount(0, text.length());
    }

    private static long spaces(String text) {
        return text.chars().filter(c -> c == ' ').count();
    }
}
