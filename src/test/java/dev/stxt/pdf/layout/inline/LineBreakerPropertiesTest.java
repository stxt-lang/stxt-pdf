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
 * Propiedades del reparto en líneas con textos y anchos al azar, con los dos algoritmos:
 * no se pierde ni se duplica ninguna palabra y quedan en orden, los párrafos son los del texto (una línea en blanco
 * los separa), ninguna línea está vacía y toda línea de más de una palabra cabe. Con {@link FixedMetrics} cada
 * carácter mide 5 pt con {@code Size: 10}. La semilla es fija: un fallo se repite.
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

    /** Las palabras de cada párrafo: un hueco con dos saltos de línea o más los separa. */
    private static List<List<String>> paragraphs(String text) {
        List<List<String>> paragraphs = new ArrayList<>();
        List<String> current = new ArrayList<>();
        Matcher m = WORD.matcher(text);
        int previousEnd = -1;
        while (m.find()) {
            if (previousEnd >= 0 && text.substring(previousEnd, m.start()).chars().filter(c -> c == '\n').count() > 1) {
                paragraphs.add(current);
                current = new ArrayList<>();
            }
            current.add(m.group());
            previousEnd = m.end();
        }
        paragraphs.add(current);
        return paragraphs;
    }

    @Test
    void noWordIsLostOrDuplicatedAndEveryLineFits() {
        Random random = new Random(20261029);
        for (int i = 0; i < 3000; i++) {
            String text = randomText(random);
            double available = 5 + random.nextInt(400);
            for (LineBreaking wrap : LineBreaking.values()) {
                String where = wrap + ", " + available + " pt: \"" + text.replace("\n", "\\n") + "\"";
                List<List<String>> lines = LineBreaker.breakParagraphs(text, available, METRICS, FONT, 10, wrap);
                List<List<String>> expected = paragraphs(text);
                assertEquals(expected.size(), lines.size(), "párrafos con " + where);
                for (int p = 0; p < expected.size(); p++) {
                    List<String> words = new ArrayList<>();
                    for (String line : lines.get(p)) {
                        assertFalse(line.isEmpty(), "línea vacía con " + where);
                        assertEquals(line.strip(), line, "espacios en los extremos con " + where);
                        List<String> inLine = Arrays.asList(line.split(" "));
                        assertTrue(inLine.size() == 1 || fits(line.length() * 5.0, available),
                                "«" + line + "» no cabe con " + where);
                        words.addAll(inLine);
                    }
                    assertEquals(expected.get(p), words, "palabras del párrafo " + p + " con " + where);
                }
            }
        }
    }

    /** Formatos de los tramos de {@link #theGreedyBreakIsTheSameAsMeasuringEveryLine}: cada uno mide distinto. */
    private static final TextFormat[] FORMATS = {TextFormat.PLAIN, TextFormat.PLAIN,
            new TextFormat(true, false, false, false), new TextFormat(false, true, false, false),
            new TextFormat(true, true, false, false), TextFormat.PLAIN.withSmallCaps(true)};
    /** Trozos de palabra: con {@code fi}, {@code fl} y {@code ff}, que son ligaduras de IBM Plex Sans. */
    private static final String[] SYLLABLES = {"a", "ma", "fi", "fl", "ff", "ffi", "ti", "on", "W", "AV", "é", "ñ"};

    /** Un texto de tramos con formato: los cambios de formato caen dentro de las palabras y en los espacios. */
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

    /** El reparto voraz de antes de sumar los avances: con cada palabra, la línea se construye y se mide entera. */
    private static List<List<LineBreaker.Line>> measuringEveryLine(List<TextRun> runs, double available,
            TextMeasurer metrics, FontFace font, double size, double spacing) {
        List<List<LineBreaker.Line>> result = new ArrayList<>();
        for (List<LineBreaker.Word> paragraph : LineBreaker.paragraphs(runs)) {
            List<LineBreaker.Line> lines = new ArrayList<>();
            List<LineBreaker.Word> current = new ArrayList<>();
            for (LineBreaker.Word word : paragraph) {
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
            result.add(lines);
        }
        return result;
    }

    /**
     * El reparto voraz suma los avances de las palabras y los espacios y da las mismas líneas que midiendo la
     * línea entera con cada palabra, como antes: con las métricas reales (negrita, cursiva, versalitas y ligaduras),
     * con interletraje y con anchos que son justo el de una línea, donde el redondeo de la suma podría decidir.
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
                // El ancho de las primeras palabras de un párrafo, justo en el límite.
                List<List<LineBreaker.Word>> paragraphs = LineBreaker.paragraphs(runs);
                List<LineBreaker.Word> words = paragraphs.get(random.nextInt(paragraphs.size()));
                List<LineBreaker.Word> first = words.subList(0, 1 + random.nextInt(words.size()));
                available = LineBreaker.width(LineBreaker.line(first).runs(), metrics, font, size, spacing)
                        + nudges[random.nextInt(nudges.length)];
            }
            assertEquals(measuringEveryLine(runs, available, metrics, font, size, spacing),
                    LineBreaker.breakParagraphs(runs, available, metrics, font, size, spacing, LineBreaking.SIMPLE),
                    available + " pt, " + size + " pt, interletraje " + spacing + ": " + runs);
        }
    }

    /** Las palabras de antes, con {@code \\S+}: la referencia de {@link #theWordsAreTheSameAsWithTheRegex}. */
    private static List<List<LineBreaker.Word>> paragraphsWithTheRegex(List<TextRun> runs) {
        String text = TextRun.text(runs);
        int[] starts = new int[runs.size() + 1];
        for (int i = 0; i < runs.size(); i++) {
            starts[i + 1] = starts[i] + runs.get(i).text().length();
        }
        LineBreaker.RunCursor cursor = new LineBreaker.RunCursor(runs, starts);
        List<List<LineBreaker.Word>> result = new ArrayList<>();
        List<LineBreaker.Word> paragraph = new ArrayList<>();
        List<TextRun> previous = null;
        int previousEnd = 0;
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            if (previous != null) {
                String gap = text.substring(previousEnd, m.start());
                boolean blankLine = gap.indexOf('\n') != gap.lastIndexOf('\n');
                paragraph.add(new LineBreaker.Word(previous, blankLine ? TextFormat.PLAIN : cursor.format(previousEnd)));
                if (blankLine) {
                    result.add(paragraph);
                    paragraph = new ArrayList<>();
                }
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
            paragraph.add(new LineBreaker.Word(previous, TextFormat.PLAIN));
            result.add(paragraph);
        }
        return result;
    }

    /**
     * Las palabras y los párrafos se sacan recorriendo el texto y son los mismos que con la expresión regular
     * de antes: con todos los espacios en blanco de {@code \\s}, otros que no lo son ({@code U+00A0}) y cambios de
     * formato dentro de las palabras y de los huecos.
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
            assertEquals(paragraphsWithTheRegex(text), LineBreaker.paragraphs(text), text.toString());
        }
    }
}
