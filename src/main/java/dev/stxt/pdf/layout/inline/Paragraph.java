package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Tolerance;

import java.util.Arrays;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * The words of a text with their measures and the positions where a line may end: the start of each word, each
 * of its break points and the end of the text, in order. A line goes from one position to a later one; if it
 * ends at a break point, it ends with a hyphen. Advances are sums of those of the whole words, the spaces and the
 * parts of a split word, each one measured once ({@link LinePlacer#advance}). Both algorithms of {@link LineBreaker}
 * choose among the same positions.
 */
final class Paragraph {
    private final List<Words.Word> words;
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

    Paragraph(List<Words.Word> words, TextMeasurer measurer, FontFace font, double fontSize, double letterSpacing) {
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
            Words.Word word = words.get(w);
            wholeWords[w + 1] = wholeWords[w]
                    + LinePlacer.advance(word.parts(), measurer, font, fontSize, letterSpacing);
            gaps[w + 1] = gaps[w]
                    + (w + 1 < n ? LinePlacer.advance(" ", word.gap(), measurer, font, fontSize, letterSpacing) : 0);
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

    /**
     * The words of the text with their measures and positions, ready to be broken into lines any number of times,
     * from any position and with any line widths ({@link LineBreaker#breakLines(Paragraph, int, LineWidths,
     * dev.stxt.pdf.model.LineBreaking)}); {@code null} if the text has no words.
     *
     * @param hyphenator the patterns of the language of the text; {@code null} splits words only at their soft hyphens
     */
    static Paragraph of(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                        double letterSpacing, Hyphenator hyphenator) {
        List<Words.Word> words = Words.words(runs, hyphenator);
        return words.isEmpty() ? null : new Paragraph(words, measurer, font, fontSize, letterSpacing);
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
        TextFormat last = hyphen(b) ? Words.hyphenFormat(words.get(posWord[b]).parts(), posOffset[b])
                : lastPart(words.get(posWord[b] - 1)).format();
        return last.letterSpacing(letterSpacing);
    }

    private static TextRun lastPart(Words.Word word) {
        return word.parts().get(word.parts().size() - 1);
    }

    /**
     * Whether the line from {@code a} to {@code b} fits, measuring it whole when its sum is within
     * {@link Tolerance#MODEL} of the width.
     */
    boolean fitsMeasured(int a, int b, double available) {
        double width = width(a, b);
        if (Math.abs(width - available) <= Tolerance.MODEL) {
            width = LinePlacer.width(line(a, b).runs(), measurer, font, fontSize, letterSpacing);
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
        Words.Word word = words.get(w);
        if (from == 0 && to == word.length()) {
            return wholeWords[w + 1] - wholeWords[w];
        }
        return LinePlacer.advance(Words.slice(word.parts(), from, to), measurer, font, fontSize, letterSpacing);
    }

    /**
     * Advance of the part of word {@code w} from {@code from} to break position {@code to}, with its hyphen,
     * measured together as the line paints them: with kerning, the last letter and the hyphen are a pair.
     */
    private double withHyphen(int w, int from, int to) {
        List<TextRun> parts = words.get(w).parts();
        return LinePlacer.advance(Words.hyphenated(Words.slice(parts, from, to), parts, to), measurer, font, fontSize,
                letterSpacing);
    }

    /** The line from {@code a} to {@code b}: its words separated by a space with the format of the previous one. */
    LineBreaker.Line line(int a, int b) {
        int first = posWord[a];
        int last = lastWord(b);
        TextRun.Joiner runs = new TextRun.Joiner();
        for (int w = first; w <= last; w++) {
            Words.Word word = words.get(w);
            if (w > first) {
                runs.add(" ", words.get(w - 1).gap());
            }
            int from = w == first ? posOffset[a] : 0;
            int to = w == last ? lastOffset(b) : word.length();
            for (TextRun part : from == 0 && to == word.length() ? word.parts() : Words.slice(word.parts(), from, to)) {
                runs.add(part.text(), part.format());
            }
        }
        if (hyphen(b)) {
            runs.add(LineBreaker.HYPHEN, Words.hyphenFormat(words.get(last).parts(), posOffset[b]));
        }
        return new LineBreaker.Line(runs.build(), hyphen(b), a, forced(b));
    }
}
