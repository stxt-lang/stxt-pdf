package dev.stxt.pdf.layout.inline;

import java.util.List;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.tree.Style;

/**
 * Short forms of the line breaking for the tests: a text in one line width, plain text, no letter spacing, no
 * hyphenation, a line placed at the {@code Size} of its style and in the whole content box. The engine uses the full
 * forms ({@link LineBreaker#breakLines(Paragraph, int, LineWidths, LineBreaking)},
 * {@link LineBreaker#intrinsics(List, TextMeasurer, FontFace, double, double, Hyphenator)} and
 * {@link LinePlacer#place}), to which these delegate.
 */
final class Lines {

    private Lines() {
    }

    // --- Line breaking ------------------------------------------------------------------------------------------------

    /** Lines of the text for an available width, with the greedy algorithm. */
    static List<String> breakLines(String text, double available, TextMeasurer measurer, FontFace font,
                                   double fontSize) {
        return breakLines(text, available, measurer, font, fontSize, LineBreaking.SIMPLE);
    }

    /** Lines of a plain text for an available width. */
    static List<String> breakLines(String text, double available, TextMeasurer measurer, FontFace font,
                                   double fontSize, LineBreaking wrap) {
        return breakLines(text, available, measurer, font, fontSize, wrap, null);
    }

    /** Lines of a plain text for an available width, hyphenating with {@code hyphenator}. */
    static List<String> breakLines(String text, double available, TextMeasurer measurer, FontFace font,
                                   double fontSize, LineBreaking wrap, Hyphenator hyphenator) {
        return breakLines(TextRun.plain(text), available, measurer, font, fontSize, 0, wrap, hyphenator).stream()
                .map(LineBreaker.Line::text).toList();
    }

    /** Lines of the text for an available width, without letter spacing. */
    static List<LineBreaker.Line> breakLines(List<TextRun> runs, double available, TextMeasurer measurer,
                                             FontFace font, double fontSize, LineBreaking wrap) {
        return breakLines(runs, available, measurer, font, fontSize, 0, wrap);
    }

    /** Lines of the text for an available width, without hyphenation. */
    static List<LineBreaker.Line> breakLines(List<TextRun> runs, double available, TextMeasurer measurer,
                                             FontFace font, double fontSize, double letterSpacing,
                                             LineBreaking wrap) {
        return breakLines(runs, available, measurer, font, fontSize, letterSpacing, wrap, null);
    }

    /**
     * Lines of the text for an available width, from its start and with every line equally wide. {@code font} is the
     * font of the box; each run changes it with its format.
     */
    static List<LineBreaker.Line> breakLines(List<TextRun> runs, double available, TextMeasurer measurer,
                                             FontFace font, double fontSize, double letterSpacing,
                                             LineBreaking wrap, Hyphenator hyphenator) {
        Paragraph paragraph = Paragraph.of(runs, measurer, font, fontSize, letterSpacing, hyphenator);
        return paragraph == null ? List.of()
                : LineBreaker.breakLines(paragraph, 0, LineWidths.uniform(available), wrap);
    }

    // --- Intrinsic sizes ----------------------------------------------------------------------------------------------

    /** Narrowest and natural width of a plain text. */
    static IntrinsicSizes intrinsics(String text, TextMeasurer measurer, FontFace font, double fontSize) {
        return intrinsics(TextRun.plain(text), measurer, font, fontSize);
    }

    /** Narrowest and natural width of the text, without letter spacing. */
    static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize) {
        return intrinsics(runs, measurer, font, fontSize, 0);
    }

    /** Narrowest and natural width of the text, without hyphenation. */
    static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                                     double letterSpacing) {
        return LineBreaker.intrinsics(runs, measurer, font, fontSize, letterSpacing, null);
    }

    // --- Words --------------------------------------------------------------------------------------------------------

    /** The words of the text, with the break points of their soft hyphens only. */
    static List<Words.Word> words(List<TextRun> runs) {
        return Words.words(runs, null);
    }

    // --- Placed lines -------------------------------------------------------------------------------------------------

    /** A plain line placed in the whole content box, at the {@code Size} of {@code s} and not hyphenated. */
    static TextLine place(String text, TextMeasurer measurer, Style s, double available, boolean justify, double y,
                          double height) {
        return place(TextRun.plain(text), measurer, s, available, justify, y, height);
    }

    /** A line placed in the whole content box, at the {@code Size} of {@code s} and not hyphenated. */
    static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double available, boolean justify,
                          double y, double height) {
        return LinePlacer.place(runs, measurer, s, s.typography().fontSize(), 0, 0, available, justify, y, height,
                false);
    }
}
