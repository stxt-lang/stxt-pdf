package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Tolerance;

import java.text.BreakIterator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * The words of a text with their measures and the positions where a line may end: the start of each word, each
 * of its break points and the end of the text, in order. A line goes from one position to a later one. Advances are
 * sums of those of the whole words, the spaces and the parts of a split word, each one measured once
 * ({@link LinePlacer#advance}). Both algorithms of {@link LineBreaker} choose among the same positions.
 *
 * <p>A line that ends at a break point ends inside a word ({@link #inside}), in one of three ways
 * (reference/common/typography.md): with a hyphen that the line breaking adds, at a soft hyphen or where the patterns
 * of the language allow it ({@link #hyphen}); with the hyphen written in the word, after which nothing is added; or,
 * with {@code Break long words}, at a cut of last resort ({@link #lastResort}), with no hyphen at all.</p>
 *
 * <p><strong>Cuts of last resort.</strong> A part of a word that cannot be split further and is wider than
 * {@code longWord}, the width of a whole line of the box, gets a break point between each two of its characters
 * ({@link #cutLongParts}), so that the line breaking can end a line at the last one that fits; with
 * {@code new-line} the part starts a line of its own, so only a line that starts in it ends at one of them
 * ({@link #reaches}), and with {@code same-line}, any line. A word that fits in a
 * whole line has none: it goes to the next line, as always. The first part of the first word has no line before the
 * one it starts in, the first one of the paragraph, which a {@code First line indent} makes shorter: it is cut from
 * {@code firstPart}, the width of that line.</p>
 */
final class Paragraph {
    /** How a line that ends at a position ends: between words, or inside a word in one of its three ways. */
    private static final byte BETWEEN_WORDS = 0;
    private static final byte HYPHENATED = 1;
    private static final byte WRITTEN_HYPHEN = 2;
    private static final byte LAST_RESORT = 3;

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
    /** How a line that ends at each position ends. */
    private final byte[] posKind;
    /**
     * For a cut of last resort of a part that starts a line of its own ({@code Break long words: new-line}), the
     * position where the part starts; for the others, 0.
     */
    private final int[] posPart;
    /** start[w]: the position of the start of word w; start[n], the end of the text. */
    private final int[] start;
    /**
     * Memorized advances of the part of the word before a break position, with its hyphen if the line breaking adds
     * one, and after it.
     */
    private final double[] head;
    private final double[] tail;

    Paragraph(List<Words.Word> words, TextMeasurer measurer, FontFace font, double fontSize, double letterSpacing) {
        this(words, measurer, font, fontSize, letterSpacing, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                false);
    }

    /**
     * @param longWord the width from which a part of a word that cannot be split further is cut between any two of
     *                 its characters ({@code Break long words}): that of a whole line of the box; infinite to never
     *                 cut one
     * @param firstPart the same width for the first part of the first word: that of the first line of the paragraph,
     *                  with its {@code First line indent}; never greater than {@code longWord}
     * @param newLine  whether a part that is cut starts a line of its own ({@code Break long words: new-line}) or
     *                 the line in progress, after the words before it ({@code same-line})
     */
    Paragraph(List<Words.Word> words, TextMeasurer measurer, FontFace font, double fontSize, double letterSpacing,
              double longWord, double firstPart, boolean newLine) {
        this.words = words;
        this.measurer = measurer;
        this.font = font;
        this.fontSize = fontSize;
        this.letterSpacing = letterSpacing;
        int n = words.size();
        wholeWords = new double[n + 1];
        gaps = new double[n + 1];
        start = new int[n + 1];
        // The break points of each word and how a line ends at each one: those of the word and, in its long parts,
        // the cuts of last resort.
        int[][] breaks = new int[n][];
        byte[][] kinds = new byte[n][];
        int positions = 1;
        for (int w = 0; w < n; w++) {
            Words.Word word = words.get(w);
            double advance = LinePlacer.advance(word.parts(), measurer, font, fontSize, letterSpacing);
            wholeWords[w + 1] = wholeWords[w] + advance;
            gaps[w + 1] = gaps[w]
                    + (w + 1 < n ? LinePlacer.advance(" ", word.gap(), measurer, font, fontSize, letterSpacing) : 0);
            breaks[w] = word.breaks();
            kinds[w] = kinds(word);
            // A word that fits whole in a line has no part that does not.
            double first = w == 0 ? firstPart : longWord;
            if (first < Double.POSITIVE_INFINITY
                    && !fits(advance - lastPart(word).format().letterSpacing(letterSpacing), first)) {
                Cuts cuts = cutLongParts(word, kinds[w], longWord, first);
                breaks[w] = cuts.offsets();
                kinds[w] = cuts.kinds();
            }
            start[w] = positions - 1;
            positions += 1 + breaks[w].length;
        }
        start[n] = positions - 1;
        posWord = new int[positions];
        posOffset = new int[positions];
        posKind = new byte[positions];
        posPart = new int[positions];
        int p = 0;
        for (int w = 0; w < n; w++) {
            int part = p;
            posWord[p] = w;
            posOffset[p++] = 0;
            for (int k = 0; k < breaks[w].length; k++) {
                if (kinds[w][k] != LAST_RESORT) {
                    part = p;
                }
                posWord[p] = w;
                posKind[p] = kinds[w][k];
                posPart[p] = newLine && kinds[w][k] == LAST_RESORT ? part : 0;
                posOffset[p++] = breaks[w][k];
            }
        }
        posWord[p] = n;
        posOffset[p] = 0;
        head = new double[positions];
        tail = new double[positions];
        Arrays.fill(head, Double.NaN);
        Arrays.fill(tail, Double.NaN);
    }

    /** How a line ends at each break point of a word: with an added hyphen or with the one written before it. */
    private static byte[] kinds(Words.Word word) {
        byte[] kinds = new byte[word.breaks().length];
        for (int k = 0; k < kinds.length; k++) {
            kinds[k] = word.written(k) ? WRITTEN_HYPHEN : HYPHENATED;
        }
        return kinds;
    }

    /** The break points of a word, with how a line ends at each one. */
    private record Cuts(int[] offsets, byte[] kinds) {
    }

    /**
     * The break points of a word with the cuts of last resort of its long parts ({@code Break long words},
     * reference/common/typography.md): each part between two of its break points that, as a line of its own (with
     * its hyphen, if the line breaking adds one), is wider than {@code longWord} ({@code first}, the first one) gets
     * a break point between each two of its characters. A character is what the reader takes for one: a letter is not separated from its combining
     * marks, nor are the two halves of a surrogate pair. A word with a brace, which may hold {@code {page}} or a
     * reference, gets none, like with the patterns ({@link Words#breaks}).
     */
    private Cuts cutLongParts(Words.Word word, byte[] kinds, double longWord, double first) {
        int[] breaks = word.breaks();
        String text = word.text();
        if (text.indexOf('{') >= 0 || text.indexOf('}') >= 0) {
            return new Cuts(breaks, kinds);
        }
        int[] offsets = new int[text.length()];
        byte[] cut = new byte[text.length()];
        int n = 0;
        BreakIterator characters = null;
        int from = 0;
        for (int k = 0; k <= breaks.length; k++) {
            int to = k < breaks.length ? breaks[k] : text.length();
            boolean whole = from == 0 && to == text.length();
            List<TextRun> part = whole ? word.parts() : Words.slice(word.parts(), from, to);
            if (k < breaks.length && kinds[k] == HYPHENATED) {
                part = Words.hyphenated(part, word.parts(), to);
            }
            if (!fits(LinePlacer.width(part, measurer, font, fontSize, letterSpacing), k == 0 ? first : longWord)) {
                if (characters == null) {
                    characters = BreakIterator.getCharacterInstance(Locale.ROOT);
                    characters.setText(text);
                }
                for (int c = characters.following(from); c != BreakIterator.DONE && c < to; c = characters.next()) {
                    offsets[n] = c;
                    cut[n++] = LAST_RESORT;
                }
            }
            if (k < breaks.length) {
                offsets[n] = to;
                cut[n++] = kinds[k];
            }
            from = to;
        }
        return new Cuts(Arrays.copyOf(offsets, n), Arrays.copyOf(cut, n));
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
        return of(runs, measurer, font, fontSize, letterSpacing, hyphenator, Double.POSITIVE_INFINITY);
    }

    /**
     * The words of the text with their measures and positions, with the cuts of last resort of the parts of its
     * words wider than {@code longWord} ({@code Break long words}); {@code null} if the text has no words.
     *
     * @param hyphenator the patterns of the language of the text; {@code null} splits words only at their soft hyphens
     * @param longWord   the width of a whole line of the box; infinite to never cut a part of a word that cannot be
     *                   split further
     */
    static Paragraph of(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                        double letterSpacing, Hyphenator hyphenator, double longWord) {
        return of(runs, measurer, font, fontSize, letterSpacing, hyphenator, longWord, longWord, true);
    }

    /**
     * The same, with the first line of the paragraph shorter than the rest ({@code First line indent}): the first
     * part of the first word is cut from {@code firstPart}, the width of that line; with {@code newLine}, a part
     * that is cut starts a line of its own.
     */
    static Paragraph of(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                        double letterSpacing, Hyphenator hyphenator, double longWord, double firstPart,
                        boolean newLine) {
        List<Words.Word> words = Words.words(runs, hyphenator);
        return words.isEmpty() ? null
                : new Paragraph(words, measurer, font, fontSize, letterSpacing, longWord, firstPart, newLine);
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

    /** Whether position {@code p} is inside a word: a line that ends there splits the word. */
    boolean inside(int p) {
        return posOffset[p] > 0;
    }

    /** Whether a line that ends at position {@code p} ends with a hyphen that the line breaking adds. */
    boolean hyphen(int p) {
        return posKind[p] == HYPHENATED;
    }

    /** Whether a line that ends at position {@code p} ends inside a word with a hyphen, added or written. */
    boolean endsWithHyphen(int p) {
        return posKind[p] == HYPHENATED || posKind[p] == WRITTEN_HYPHEN;
    }

    /**
     * Whether position {@code p} is a cut of last resort ({@code Break long words}): a line ends there only if it
     * cannot hold one character more.
     */
    boolean lastResort(int p) {
        return posKind[p] == LAST_RESORT;
    }

    /**
     * Whether a line from {@code a} may end at position {@code b}: at a cut of last resort of a part that starts a
     * line of its own ({@code Break long words: new-line}), only if it starts in that part, at its start or at an
     * earlier cut of it, after the line of the words and the parts of its word before it
     * (reference/common/typography.md).
     */
    boolean reaches(int a, int b) {
        return a >= posPart[b];
    }

    /** Whether a forced line break comes right before position {@code p}: a line has to end there. */
    boolean forced(int p) {
        int w = posWord[p];
        return posOffset[p] == 0 && w > 0 && w < words.size() && words.get(w - 1).forced();
    }

    /** The last word of a line that ends at {@code b}, and how many of its characters the line has. */
    private int lastWord(int b) {
        return inside(b) ? posWord[b] : posWord[b] - 1;
    }

    private int lastOffset(int b) {
        return inside(b) ? posOffset[b] : words.get(posWord[b] - 1).length();
    }

    /**
     * Whether the line from {@code a} to {@code b} is a single piece of the text, between two consecutive positions:
     * a word or a part of a word that cannot be split further, which may overflow.
     */
    boolean single(int a, int b) {
        return b == a + 1;
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
        sum += inside(b) ? head(b) : wholeWords[last + 1] - wholeWords[last];
        return sum;
    }

    /** Width of the line from {@code a} to {@code b}: its advance without the letter spacing of its last character. */
    double width(int a, int b) {
        return advance(a, b) - trailing(b);
    }

    /**
     * The letter spacing of the last character of a line that ends at {@code b}: that of the run of the hyphen, if
     * the line breaking adds one, or of the last character of the line.
     */
    private double trailing(int b) {
        TextFormat last;
        if (hyphen(b)) {
            last = Words.hyphenFormat(words.get(posWord[b]).parts(), posOffset[b]);
        } else if (inside(b)) {
            last = Words.formatBefore(words.get(posWord[b]).parts(), posOffset[b]);
        } else {
            last = lastPart(words.get(posWord[b] - 1)).format();
        }
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
        return fitsMeasured(a, b, width(a, b), available);
    }

    /** {@link #fitsMeasured(int, int, double)} for a line whose {@link #width} is already known. */
    boolean fitsMeasured(int a, int b, double width, double available) {
        if (Math.abs(width - available) <= Tolerance.MODEL) {
            width = LinePlacer.width(line(a, b).runs(), measurer, font, fontSize, letterSpacing);
        }
        return fits(width, available);
    }

    /**
     * How much a line may be wider than another that ends later in the same word: the width of a line grows with the
     * position where it ends, except for the hyphen that only some positions add and for the kerning with it, which
     * together are less than the size of the text. A line this much wider than the available width tells that no
     * later position of the word fits either.
     */
    double hyphenMargin() {
        return fontSize;
    }

    /** Advance of the part of its word before break position {@code b}, with its hyphen if a line that ends there has one. */
    private double head(int b) {
        if (Double.isNaN(head[b])) {
            head[b] = hyphen(b) ? withHyphen(posWord[b], 0, posOffset[b]) : piece(posWord[b], 0, posOffset[b]);
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

    /**
     * The line from {@code a} to {@code b}: its words separated by a space with the format of the previous one and,
     * if it ends where the line breaking adds a hyphen, the hyphen.
     */
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
        return new LineBreaker.Line(runs.build(), hyphen(b), a, forced(b), inside(b));
    }
}
