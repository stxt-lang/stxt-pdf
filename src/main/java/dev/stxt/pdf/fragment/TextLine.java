package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.TextFormat;

import java.util.List;

/**
 * A placed line box, relative to the content box of its fragment.
 *
 * @param x           horizontal offset due to the alignment
 * @param y           top of the line
 * @param width       width of the text of the line, including the extra spacing of {@code justify}
 * @param height      height of the line ({@code Line height})
 * @param fontSize    size the line is measured and painted with: the {@code Size} of the box or, with
 *                    {@code Overflow: shrink}, the one it was reduced to (reference/common/overflow.md); a small caps
 *                    run scales it
 * @param text        content of the line; the words are separated by a single space
 * @param wordSpacing space added to each space between words ({@code justify}); 0 otherwise
 * @param runs        runs of the line in order (reference/common/text-formatting.md); their joined text is
 *                    {@code text}
 * @param hyphenated  whether the line ends inside a word, with a hyphen that the line breaking added
 *                    (reference/common/typography.md): the last run ends with it, the text that is read does not
 *                    (reference/implementation/pdf.md)
 * @param left        room taken from the left of the content box by a side box or a drop cap
 *                    (reference/components/block.md), already included in {@code x}; 0 in a line that has the whole
 *                    width
 * @param right       room taken from the right of the content box by a side box; 0 otherwise. The line box is the
 *                    content width minus {@code left} and {@code right}, and the line is aligned within it
 */
public record TextLine(double x, double y, double width, double height, double fontSize, String text,
                       double wordSpacing, List<Run> runs, boolean hyphenated, double left, double right) {

    /**
     * A run of the line with its format.
     *
     * @param x     offset from the start of the line
     * @param width width, including the extra {@code justify} spacing of its spaces
     */
    public record Run(double x, double width, String text, TextFormat format) {
    }

    public TextLine {
        runs = List.copyOf(runs);
    }

    /** A line with the whole width of the content box. */
    public TextLine(double x, double y, double width, double height, double fontSize, String text,
                    double wordSpacing, List<Run> runs, boolean hyphenated) {
        this(x, y, width, height, fontSize, text, wordSpacing, runs, hyphenated, 0, 0);
    }

    /** A line that ends between words. */
    public TextLine(double x, double y, double width, double height, double fontSize, String text, double wordSpacing,
                    List<Run> runs) {
        this(x, y, width, height, fontSize, text, wordSpacing, runs, false);
    }

    /** Unformatted line: a single run with all the text. */
    public TextLine(double x, double y, double width, double height, double fontSize, String text,
                    double wordSpacing) {
        this(x, y, width, height, fontSize, text, wordSpacing, List.of(new Run(0, width, text, TextFormat.PLAIN)));
    }

    /** Line without format or extra spacing. */
    public TextLine(double x, double y, double width, double height, double fontSize, String text) {
        this(x, y, width, height, fontSize, text, 0);
    }

    /** Whether the line has no words: a line beside a side box where no word fits (reference/components/block.md). */
    public boolean isEmpty() {
        return text.isEmpty();
    }
}
