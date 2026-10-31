package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.resources.Resources;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.pdf.model.Tolerance.fits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Line-breaking properties with random texts and widths, with both algorithms:
 * no word is lost or duplicated and they stay in order (every gap counts as a space, also those that contain
 * a blank line: the text is a single paragraph), no line is empty and every line of more than one word fits. With
 * {@link FixedMetrics} each character measures 5 pt with {@code Size: 10}. The seed is fixed: a failure repeats.
 */
class LineBreakerPropertiesTest {

    private static final FontFace FONT = new FontFace("IBM Plex Sans", FontWeight.NORMAL, FontStyle.NORMAL);
    private static final FixedMetrics METRICS = new FixedMetrics();
    private static final String[] GAPS = {" ", " ", " ", "  ", "\t", "\n", " \n", "\n\n", "\n \t\n", " \n\n\n "};
    private static final Pattern WORD = Pattern.compile("\\S+");

    private static String randomText(Random random) {
        StringBuilder sb = new StringBuilder();
        int words = 1 + random.nextInt(60);
        for (int i = 0; i < words; i++) {
            if (i > 0) {
                sb.append(GAPS[random.nextInt(GAPS.length)]);
            }
            int length = 1 + random.nextInt(random.nextInt(10) == 0 ? 30 : 9);
            for (int c = 0; c < length; c++) {
                sb.append((char) ('a' + random.nextInt(26)));
            }
        }
        return sb.toString();
    }

    /** The words of the text. */
    private static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            words.add(m.group());
        }
        return words;
    }

    @Test
    void noWordIsLostOrDuplicatedAndEveryLineFits() {
        Random random = new Random(20261029);
        for (int i = 0; i < 3000; i++) {
            String text = randomText(random);
            double available = 5 + random.nextInt(400);
            for (LineBreaking wrap : LineBreaking.values()) {
                String where = wrap + ", " + available + " pt: \"" + text.replace("\n", "\\n") + "\"";
                List<String> words = new ArrayList<>();
                for (String line : LineBreaker.breakLines(text, available, METRICS, FONT, 10, wrap)) {
                    assertFalse(line.isEmpty(), "empty line with " + where);
                    assertEquals(line.strip(), line, "spaces at the ends with " + where);
                    List<String> inLine = Arrays.asList(line.split(" "));
                    assertTrue(inLine.size() == 1 || fits(line.length() * 5.0, available),
                            "\"" + line + "\" does not fit with " + where);
                    words.addAll(inLine);
                }
                assertEquals(words(text), words, "words with " + where);
            }
        }
    }

    /** Formats of the runs of {@link #theGreedyBreakIsTheSameAsMeasuringEveryLine}: each one measures differently. */
    private static final TextFormat[] FORMATS = {TextFormat.PLAIN, TextFormat.PLAIN,
            new TextFormat(true, false, false, false), new TextFormat(false, true, false, false),
            new TextFormat(true, true, false, false), TextFormat.PLAIN.withSmallCaps(true)};
    /** Pieces of words: with {@code fi}, {@code fl} and {@code ff}, which are ligatures of IBM Plex Sans. */
    private static final String[] SYLLABLES = {"a", "ma", "fi", "fl", "ff", "ffi", "ti", "on", "W", "AV", "é", "ñ"};

    /** A text of formatted runs: the format changes fall inside the words and on the spaces. */
    private static List<TextRun> randomRuns(Random random) {
        TextRun.Joiner runs = new TextRun.Joiner();
        int words = 1 + random.nextInt(40);
        for (int i = 0; i < words; i++) {
            if (i > 0) {
                runs.add(random.nextInt(15) == 0 ? "\n\n" : " ", FORMATS[random.nextInt(FORMATS.length)]);
            }
            int parts = 1 + random.nextInt(4);
            for (int k = 0; k < parts; k++) {
                TextFormat format = random.nextInt(3) == 0 ? FORMATS[random.nextInt(FORMATS.length)] : TextFormat.PLAIN;
                runs.add(SYLLABLES[random.nextInt(SYLLABLES.length)], format);
            }
        }
        return runs.build();
    }

    /** The greedy line breaking from before adding up the advances: with each word, the line is built and measured. */
    private static List<LineBreaker.Line> measuringEveryLine(List<TextRun> runs, double available,
            TextMeasurer metrics, FontFace font, double size, double spacing) {
        List<LineBreaker.Line> lines = new ArrayList<>();
        List<LineBreaker.Word> current = new ArrayList<>();
        for (LineBreaker.Word word : LineBreaker.words(runs)) {
            current.add(word);
            if (current.size() > 1 && !fits(LineBreaker.width(LineBreaker.line(current).runs(), metrics, font,
                    size, spacing), available)) {
                current.remove(current.size() - 1);
                lines.add(LineBreaker.line(current));
                current.clear();
                current.add(word);
            }
        }
        lines.add(LineBreaker.line(current));
        return lines;
    }

    /**
     * Greedy line breaking adds up the advances of the words and the spaces and gives the same lines as measuring the
     * whole line with each word, as before: with the real metrics (bold, italic, small caps and ligatures),
     * with letter spacing and with widths that are exactly that of a line, where the rounding of the sum could decide.
     */
    @Test
    void theGreedyBreakIsTheSameAsMeasuringEveryLine() {
        PdfMetrics metrics = new PdfMetrics(new Resources());
        Random random = new Random(20261030);
        double[] nudges = {0, 0, 1e-12, -1e-12, 1e-9, -1e-9, 0.004, -0.004};
        for (int i = 0; i < 3000; i++) {
            List<TextRun> runs = randomRuns(random);
            FontFace font = new FontFace("IBM Plex Sans", FontWeight.NORMAL, FontStyle.NORMAL, random.nextBoolean());
            double size = 6 + random.nextInt(9) + (random.nextBoolean() ? 0.5 : 0);
            double spacing = random.nextInt(3) == 0 ? (random.nextInt(9) - 4) * 0.25 : 0;
            double available;
            if (random.nextBoolean()) {
                available = 20 + random.nextInt(300);
            } else {
                // The width of the first words, exactly at the limit.
                List<LineBreaker.Word> words = LineBreaker.words(runs);
                List<LineBreaker.Word> first = words.subList(0, 1 + random.nextInt(words.size()));
                available = LineBreaker.width(LineBreaker.line(first).runs(), metrics, font, size, spacing)
                        + nudges[random.nextInt(nudges.length)];
            }
            assertEquals(measuringEveryLine(runs, available, metrics, font, size, spacing),
                    LineBreaker.breakLines(runs, available, metrics, font, size, spacing, LineBreaking.SIMPLE),
                    available + " pt, " + size + " pt, letter spacing " + spacing + ": " + runs);
        }
    }

    /** The words from before, with {@code \\S+}: the reference of {@link #theWordsAreTheSameAsWithTheRegex}. */
    private static List<LineBreaker.Word> wordsWithTheRegex(List<TextRun> runs) {
        String text = TextRun.text(runs);
        int[] starts = new int[runs.size() + 1];
        for (int i = 0; i < runs.size(); i++) {
            starts[i + 1] = starts[i] + runs.get(i).text().length();
        }
        LineBreaker.RunCursor cursor = new LineBreaker.RunCursor(runs, starts);
        List<LineBreaker.Word> result = new ArrayList<>();
        List<TextRun> previous = null;
        int previousEnd = 0;
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            if (previous != null) {
                result.add(new LineBreaker.Word(previous, cursor.format(previousEnd)));
            }
            TextRun.Joiner parts = new TextRun.Joiner();
            for (int i = m.start(); i < m.end(); ) {
                TextFormat f = cursor.format(i);
                int run = 0;
                while (starts[run + 1] <= i) {
                    run++;
                }
                int stop = Math.min(m.end(), starts[run + 1]);
                parts.add(text.substring(i, stop), f);
                i = stop;
            }
            previous = parts.build();
            previousEnd = m.end();
        }
        if (previous != null) {
            result.add(new LineBreaker.Word(previous, TextFormat.PLAIN));
        }
        return result;
    }

    /**
     * The words are extracted by walking the text and are the same as with the regular expression
     * from before: with all the white space of {@code \\s}, other characters that are not white space ({@code U+00A0})
     * and format changes inside the words and the gaps.
     */
    @Test
    void theWordsAreTheSameAsWithTheRegex() {
        String[] pieces = {"a", "bc", "fi", "é", "\u00A0", "\uD83D\uDE00", " ", "  ", "\t", "\n", "\n\n", " \n \n", "\u000B",
                "\f", "\r", "\r\n"};
        Random random = new Random(20261031);
        for (int i = 0; i < 20000; i++) {
            TextRun.Joiner runs = new TextRun.Joiner();
            int count = 1 + random.nextInt(30);
            for (int k = 0; k < count; k++) {
                runs.add(pieces[random.nextInt(pieces.length)], FORMATS[random.nextInt(FORMATS.length)]);
            }
            List<TextRun> text = runs.build();
            assertEquals(wordsWithTheRegex(text), LineBreaker.words(text), text.toString());
        }
    }
}
