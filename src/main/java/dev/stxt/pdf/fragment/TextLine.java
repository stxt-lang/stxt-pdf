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
 *                    (reference/components/block.md), and in the first line of a paragraph, by its
 *                    {@code First line indent} (reference/common/typography.md), with which it can be negative: the
 *                    line box starts in the left padding. Already included in {@code x}; 0 in a line that has the
 *                    whole width
 * @param right       room taken from the right of the content box by a side box; 0 otherwise. The line box is the
 *                    content width minus {@code left} and {@code right}, and the line is aligned within it
 * @param splitsWord  whether the line ends inside a word, which goes on in the next line
 *                    (reference/common/typography.md): with the hyphen of {@code hyphenated}, with a hyphen written
 *                    in the word or, with {@code Break long words}, with none. The text that is read joins the two
 *                    parts (reference/implementation/pdf.md)
 */
public record TextLine(double x, double y, double width, double height, double fontSize, String text,
                       double wordSpacing, List<Run> runs, boolean hyphenated, double left, double right,
                       boolean splitsWord) {

    /**
     * A run of the line with its format.
     *
     * @param x      offset from the start of the line
     * @param width  width, including the extra {@code justify} spacing of its spaces
     * @param text   its part of the text of the line
     * @param format how it differs from the text of the box: bold, italic, link, footnote call, styled run...
     */
    public record Run(double x, double width, String text, TextFormat format) {
    }

    /**
     * Copies {@code runs} into an unmodifiable list; it cannot be {@code null}.
     *
     * @param x           horizontal offset due to the alignment, in points relative to the content box of the fragment
     * @param y           top of the line, relative to the content box of the fragment
     * @param width       width of the text, including the extra spacing of {@code justify}
     * @param height      height of the line ({@code Line height})
     * @param fontSize    size the line is measured and painted with
     * @param text        content of the line; the words are separated by a single space
     * @param wordSpacing space added to each space between words ({@code justify}); 0 otherwise
     * @param runs        runs of the line in order; their joined text is {@code text}
     * @param hyphenated  whether the line ends inside a word, with a hyphen that the line breaking added
     * @param left        room taken from the left of the content box by a side box or a drop cap, already in {@code x}
     * @param right       room taken from the right of the content box by a side box
     * @param splitsWord  whether the line ends inside a word, which goes on in the next line
     */
    public TextLine {
        runs = List.copyOf(runs);
    }

    /**
     * A line that, if it ends inside a word, ends with the hyphen that the line breaking added.
     *
     * @param x           horizontal offset due to the alignment, in points relative to the content box of the fragment
     * @param y           top of the line, relative to the content box of the fragment
     * @param width       width of the text, including the extra spacing of {@code justify}
     * @param height      height of the line ({@code Line height})
     * @param fontSize    size the line is measured and painted with
     * @param text        content of the line; the words are separated by a single space
     * @param wordSpacing space added to each space between words ({@code justify}); 0 otherwise
     * @param runs        runs of the line in order; their joined text is {@code text}
     * @param hyphenated  whether the line ends inside a word, with a hyphen that the line breaking added
     * @param left        room taken from the left of the content box by a side box or a drop cap, already in {@code x}
     * @param right       room taken from the right of the content box by a side box
     */
    public TextLine(double x, double y, double width, double height, double fontSize, String text,
                    double wordSpacing, List<Run> runs, boolean hyphenated, double left, double right) {
        this(x, y, width, height, fontSize, text, wordSpacing, runs, hyphenated, left, right, hyphenated);
    }

    /**
     * A line with the whole width of the content box.
     *
     * @param x           horizontal offset due to the alignment, in points relative to the content box of the fragment
     * @param y           top of the line, relative to the content box of the fragment
     * @param width       width of the text, including the extra spacing of {@code justify}
     * @param height      height of the line ({@code Line height})
     * @param fontSize    size the line is measured and painted with
     * @param text        content of the line; the words are separated by a single space
     * @param wordSpacing space added to each space between words ({@code justify}); 0 otherwise
     * @param runs        runs of the line in order; their joined text is {@code text}
     * @param hyphenated  whether the line ends inside a word, with a hyphen that the line breaking added
     */
    public TextLine(double x, double y, double width, double height, double fontSize, String text,
                    double wordSpacing, List<Run> runs, boolean hyphenated) {
        this(x, y, width, height, fontSize, text, wordSpacing, runs, hyphenated, 0, 0);
    }

    /**
     * A line that ends between words.
     *
     * @param x           horizontal offset due to the alignment, in points relative to the content box of the fragment
     * @param y           top of the line, relative to the content box of the fragment
     * @param width       width of the text, including the extra spacing of {@code justify}
     * @param height      height of the line ({@code Line height})
     * @param fontSize    size the line is measured and painted with
     * @param text        content of the line; the words are separated by a single space
     * @param wordSpacing space added to each space between words ({@code justify}); 0 otherwise
     * @param runs        runs of the line in order; their joined text is {@code text}
     */
    public TextLine(double x, double y, double width, double height, double fontSize, String text, double wordSpacing,
                    List<Run> runs) {
        this(x, y, width, height, fontSize, text, wordSpacing, runs, false);
    }

    /**
     * Unformatted line: a single run with all the text.
     *
     * @param x           horizontal offset due to the alignment, in points relative to the content box of the fragment
     * @param y           top of the line, relative to the content box of the fragment
     * @param width       width of the text, including the extra spacing of {@code justify}; also that of its run
     * @param height      height of the line ({@code Line height})
     * @param fontSize    size the line is measured and painted with
     * @param text        content of the line; the words are separated by a single space
     * @param wordSpacing space added to each space between words ({@code justify}); 0 otherwise
     */
    public TextLine(double x, double y, double width, double height, double fontSize, String text,
                    double wordSpacing) {
        this(x, y, width, height, fontSize, text, wordSpacing, List.of(new Run(0, width, text, TextFormat.PLAIN)));
    }

    /**
     * Line without format or extra spacing.
     *
     * @param x        horizontal offset due to the alignment, in points relative to the content box of the fragment
     * @param y        top of the line, relative to the content box of the fragment
     * @param width    width of the text
     * @param height   height of the line ({@code Line height})
     * @param fontSize size the line is measured and painted with
     * @param text     content of the line; the words are separated by a single space
     */
    public TextLine(double x, double y, double width, double height, double fontSize, String text) {
        this(x, y, width, height, fontSize, text, 0);
    }

    /**
     * {@return whether the line has no words: a line beside a side box where no word fits
     * (reference/components/block.md)}
     */
    public boolean isEmpty() {
        return text.isEmpty();
    }
}
