package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.Box;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coste de un texto largo: un solo {@code Text} de muchas
 * páginas. {@link InlineLayout} lo parte en líneas una sola vez y no en cada página, así que el
 * trabajo crece de forma lineal con las páginas, no cuadrática. Se mide por las veces que se
 * mide el texto.
 */
class LongTextTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String SENTENCE = "El motor maqueta cajas en páginas de tamaño fijo y parte el texto en "
            + "líneas con las métricas de la fuente, respetando viudas y huérfanas al cortar. ";

    /** {@link TextMeasurer} que cuenta las mediciones. */
    private static final class Counting implements TextMeasurer {
        private final TextMeasurer metrics;
        long calls;

        Counting(TextMeasurer metrics) {
            this.metrics = metrics;
        }

        @Override
        public double width(String text, FontFace font, double fontSize) {
            calls++;
            return metrics.width(text, font, fontSize);
        }

        @Override
        public void validate(Box root) {
            metrics.validate(root);
        }
    }

    /** Un {@code Text} con {@code paragraphs} párrafos de cuatro frases. */
    private static String document(int paragraphs, String wrap) {
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.boxes):", "\tLanguage: es", "\tTitle: Prueba", "\tMedia:", "\t\tWidth: 595",
                "\t\tHeight: 842", "\t\tMargin: 40", "\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\t\t\t\tText wrap: " + wrap, "\tText:", "\t\tContent >>"));
        for (int p = 0; p < paragraphs; p++) {
            if (p > 0) {
                lines.add("");
            }
            lines.add("\t\t\t" + (p + 1) + ". " + SENTENCE.repeat(4).strip());
        }
        return String.join("\n", lines);
    }

    private record Run(int pages, long measurements, double millis) {
    }

    private static Run run(TextMeasurer metrics, int paragraphs, String wrap) {
        var doc = READER.read(Docs.sectioned(document(paragraphs, wrap)));
        Counting counting = new Counting(metrics);
        long start = System.nanoTime();
        LaidOutDocument laidOut = new Engine(counting).layOutDocument(doc);
        double millis = (System.nanoTime() - start) / 1e6;
        return new Run(laidOut.pages().size(), counting.calls, millis);
    }

    /** Con el doble de páginas, el doble de mediciones (antes, el cuádruple). */
    @Test
    void measurementsGrowLinearlyWithThePages() {
        Run small = run(new FixedMetrics(), 100, "auto");
        Run large = run(new FixedMetrics(), 200, "auto");
        assertTrue(large.pages() >= 2 * small.pages() - 1, large.pages() + " páginas, " + small.pages());
        double ratio = (double) large.measurements() / small.measurements();
        assertTrue(ratio < 2.5, "Con el doble de texto, " + ratio + " veces más mediciones");
    }

    /**
     * Tiempos con las dos métricas y los dos repartos, de 25 a 100 páginas. No está en la suite
     * normal ({@code perf}): {@code mvn test -Dgroups=perf -DexcludedGroups=none -Dtest=LongTextTest}.
     */
    @Tag("perf")
    @Test
    void measureLongText() {
        TextMeasurer[] metrics = {new FixedMetrics(), new PdfMetrics()};
        String[] names = {"FixedMetrics", "PdfMetrics"};
        for (int m = 0; m < metrics.length; m++) {
            for (String wrap : List.of("auto", "pretty")) {
                run(metrics[m], 50, wrap); // calentamiento de la JVM
                for (int paragraphs : new int[]{260, 520, 1040}) {
                    Run r = run(metrics[m], paragraphs, wrap);
                    System.out.printf(Locale.ROOT, "%-12s %-6s %4d párrafos %4d páginas %,12d mediciones %9.1f ms%n",
                            names[m], wrap, paragraphs, r.pages(), r.measurements(), r.millis());
                    assertTrue(r.pages() > 1);
                }
            }
        }
    }
}
