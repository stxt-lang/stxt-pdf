package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.boxes.model.Tolerance.fits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Propiedades del reparto en líneas con textos y anchos al azar (ROADMAP-QA-ARQ.md, QA-08), con los dos algoritmos:
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
            for (Style.TextWrap wrap : Style.TextWrap.values()) {
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
}
