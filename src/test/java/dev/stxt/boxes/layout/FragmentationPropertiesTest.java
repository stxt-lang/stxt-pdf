package dev.stxt.boxes.layout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Propiedad de la fragmentación con documentos y páginas al azar: los fragmentos de un
 * párrafo repartido en varias páginas suman lo que mide entero, así que no se pierde ni se repite ninguna línea, y
 * cada uno empieza en la página en que acaba el anterior o en la siguiente. La semilla es fija: un fallo se repite.
 */
class FragmentationPropertiesTest {

    private static String randomParagraph(Random random) {
        StringBuilder sb = new StringBuilder();
        int words = 1 + random.nextInt(120);
        for (int i = 0; i < words; i++) {
            sb.append(i > 0 ? " " : "").append("w".repeat(1 + random.nextInt(12)));
        }
        return sb.toString();
    }

    @Test
    void theFragmentsOfAParagraphAddUpToItsHeight() {
        Random random = new Random(20261029);
        for (int i = 0; i < 150; i++) {
            int paragraphs = 1 + random.nextInt(8);
            List<String> lines = new ArrayList<>();
            for (int p = 0; p < paragraphs; p++) {
                lines.add("\tParagraph: " + randomParagraph(random));
                lines.add("\t\tId: P" + p);
            }
            String[] document = lines.toArray(String[]::new);
            double height = 150 + random.nextInt(300);
            LayoutFixture whole = LayoutFixture.layOutPaged(1_000_000, document);
            LayoutFixture paged = LayoutFixture.layOutPaged(height, document);
            int last = 1;
            for (int p = 0; p < paragraphs; p++) {
                String id = "P" + p;
                double sum = 0;
                int first = 0;
                for (int page = 1; page <= paged.pages(); page++) {
                    if (paged.has(page, id)) {
                        first = first == 0 ? page : first;
                        sum += paged.rect(page, id).height();
                    }
                }
                String where = id + " con páginas de " + height + " pt (caso " + i + ")";
                assertEquals(whole.rect(id).height(), sum, 1e-9, where);
                assertEquals(true, first == last || first == last + 1, where + ": empieza en la página " + first);
                for (int page = first; page <= paged.pages() && paged.has(page, id); page++) {
                    last = page;
                }
            }
        }
    }
}
