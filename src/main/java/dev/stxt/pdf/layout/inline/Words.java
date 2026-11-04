package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.List;

/**
 * The words of a text (reference/common/typography.md), the first step of the line breaking of {@link LineBreaker}
 * and of {@link PreformattedText}, which only uses {@link RunCursor}.
 *
 * <p>Handling of white space: the line breaks of the {@code Text} block and the sequences of spaces count as
 * one space ({@link #isSpace}). A word can have parts from several runs ({@code **neg**rita}); the space between two
 * words has the format of the run that holds the first of its white-space characters ({@link Word#gap()}). The
 * {@link TextRun#LINE_SEPARATOR} (U+2028), which a Markdown line break becomes, separates words like a space and marks
 * the word before it ({@link Word#forced()}).</p>
 *
 * <p>Each word carries its break points ({@link Word#breaks()}): where a soft hyphen ({@link LineBreaker#SOFT_HYPHEN})
 * was written, which is removed from the word, or, with a {@link Hyphenator}, where the patterns of the language allow
 * it ({@link #breaks}).</p>
 */
final class Words {

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

    private Words() {
    }

    /**
     * The words of the text (see the class description): what lies between the white-space characters
     * ({@link #isSpace}, the {@code \s} of regular expressions, and the {@link TextRun#LINE_SEPARATOR}), with one pass
     * over the text, each one with its break points: those of its soft hyphens, which are removed from it, or, if it
     * has none and there is a {@code hyphenator}, those of the patterns of the language ({@link #breaks}). A word with
     * a line separator in the white space after it, and a word after it, is {@linkplain Word#forced() forced}.
     *
     * @param hyphenator the patterns of the language of the text; {@code null} splits words only at their soft hyphens
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
        if (text.indexOf(LineBreaker.SOFT_HYPHEN) >= 0) {
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
            for (int at = text.indexOf(LineBreaker.SOFT_HYPHEN); at >= 0;
                 at = text.indexOf(LineBreaker.SOFT_HYPHEN, from)) {
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
     * {@code {page}}. Among its letters, an apostrophe is a letter in the languages whose patterns have it
     * ({@link Hyphenator#isLetter}: {@code l'in-for-ma-tion} in French); at the ends it is punctuation, like a closing
     * quotation mark.
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
            if (!hyphenator.isLetter(cp)) {
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
        runs.add(LineBreaker.HYPHEN, hyphenFormat(parts, offset));
        return runs.build();
    }

    /** The words in a line, separated by a space with the format of the previous word. */
    static LineBreaker.Line line(List<Word> words) {
        TextRun.Joiner runs = new TextRun.Joiner();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                runs.add(" ", words.get(i - 1).gap());
            }
            for (TextRun part : words.get(i).parts()) {
                runs.add(part.text(), part.format());
            }
        }
        return new LineBreaker.Line(runs.build());
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
}
