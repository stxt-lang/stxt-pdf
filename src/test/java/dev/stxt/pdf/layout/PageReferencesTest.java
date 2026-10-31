package dev.stxt.pdf.layout;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.layout.inline.FixedMetrics;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code {page of: id}} (reference/common/references.md). Páginas de 180 pt con márgenes de 40: área
 * útil de 515 × 100; con {@code FixedMetrics} y {@code Size: 10}, cada carácter mide 5 pt y cada
 * línea, 12.
 */
class PageReferencesTest {

    private static final double PAGE = 180;

    /** Una página entera: una caja de 100 pt. */
    private static String[] fullPages(int n) {
        String[] lines = new String[n * 4];
        for (int i = 0; i < n; i++) {
            lines[i * 4] = "\tBlock:";
            lines[i * 4 + 1] = "\t\tId: full" + (i + 1);
            lines[i * 4 + 2] = "\t\tBox:";
            lines[i * 4 + 3] = "\t\t\tHeight: 100";
        }
        return lines;
    }

    private static String[] concat(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    private static String text(LayoutFixture l, int page, String id) {
        return l.fragment(page, id).lines().stream().map(x -> x.text()).reduce((a, b) -> a + "|" + b).orElse("");
    }

    /** Una fila con {@code Id} se puede referir: su página es la de su franja (reference/components/table.md#filas). */
    @Test
    void aReferenceToARowGivesThePageOfItsBand() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tText: La fila está en la página {page of: row}.", "\t\tId: ref", "\t\tLink: #row"},
                fullPages(1),
                new String[]{"\tTable:", "\t\tColumn sizes: *", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tId: row",
                        "\t\t\t\tCell: Total"}));
        assertTrue(l.has(2, "row"));
        assertEquals("La fila está en la página 2.", text(l, 1, "ref"));
    }

    @Test
    void aReferenceToALaterBoxGivesItsPage() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tText: Ver la página {page of: target}.", "\t\tId: ref"},
                fullPages(2),
                new String[]{"\tBlock:", "\t\tId: target", "\t\tBox:", "\t\t\tHeight: 10"}));
        // Una caja con Height se parte entre páginas: 12 + 200 + 10 = 222 pt, en tres páginas.
        assertEquals(3, l.pages());
        assertTrue(l.has(3, "target"));
        assertEquals("Ver la página 3.", text(l, 1, "ref"));
        // El texto mide lo que se escribe: 16 caracteres de 5 pt.
        assertEquals(80, l.fragment("ref").lines().get(0).width(), 1e-9);
    }

    @Test
    void aReferenceToAnEarlierBoxGivesItsPage() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: target", "\t\tBox:", "\t\t\tHeight: 100"},
                new String[]{"\tText: Vuelve a la página {page of: target}.", "\t\tId: ref"}));
        assertEquals(2, l.pages());
        assertEquals("Vuelve a la página 1.", text(l, 2, "ref"));
    }

    @Test
    void aSplitBoxIsOnThePageWhereItStarts() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tText: Empieza en la {page of: story}", "\t\tId: ref"},
                new String[]{"\tBlock:", "\t\tId: story", "\t\tBox:", "\t\t\tHeight: 60"},
                new String[]{"\tBlock:", "\t\tId: other", "\t\tBox:", "\t\t\tHeight: 10"},
                new String[]{"\tBlock:", "\t\tId: split", "\t\tBox:", "\t\t\tHeight: 150"}));
        // ref (12) + story (60) + other (10) = 82; split empieza en la 1 (18 pt) y sigue en la 2.
        assertTrue(l.has(1, "split"));
        assertTrue(l.has(2, "split"));
        assertEquals("Empieza en la 1", text(l, 1, "ref"));
    }

    @Test
    void severalReferencesToTheSameBox() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tText: {page of: t} y {page of: t}", "\t\tId: ref"},
                fullPages(1),
                new String[]{"\tBlock:", "\t\tId: t", "\t\tBox:", "\t\t\tHeight: 10"}));
        assertEquals("2 y 2", text(l, 1, "ref"));
    }

    @Test
    void theTextIsRepaginatedUntilItsWidthAndItsPageAgree() {
        // El texto cabe en una línea de 62 pt con una cifra ("Ver página 5" = 60 pt), pero no con
        // el número de prueba de dos cifras ("Ver página 00" = 65 pt). Tras la primera pasada ocupa
        // dos líneas, y el bloque X (10 pt) queda en la página 2; con la página 2 sigue cabiendo
        // en una línea y X vuelve a la 1: la tercera pasada confirma que la página es la 1.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 76",
                "\tText: Ver página {page of: x}", "\t\tId: ref", "\t\tBox:", "\t\t\tWidth: 62",
                "\tBlock:", "\t\tId: x", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(1, l.pages());
        assertEquals("Ver página 1", text(l, 1, "ref"));
        assertEquals(40 + 76 + 12, l.rect("x").y(), 1e-9);
    }

    @Test
    void referencesInAPageOverlayUseTheSamePageOnEveryPage() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tPage overlay:", "\t\tText: Índice en la {page of: toc}", "\t\t\tId: head"},
                fullPages(1),
                new String[]{"\tBlock:", "\t\tId: toc", "\t\tBox:", "\t\t\tHeight: 10"}));
        assertEquals(2, l.pages());
        assertEquals("Índice en la 2", text(l, 1, "head"));
        assertEquals("Índice en la 2", text(l, 2, "head"));
    }

    @Test
    void aPageThatKeepsChangingIsReportedWithTheLineOfTheReference() {
        // Un medidor de pega: un texto con un 7 mide 100 pt y se parte en dos líneas; los demás,
        // 5 pt por carácter. Con la caja X en la página 7, suponer "7" la lleva a la 8 (el texto
        // ocupa dos líneas) y suponer "8" la devuelve a la 7: no hay página que se cumpla.
        TextMeasurer wideSevens = new TextMeasurer() {
            @Override
            public double width(String text, FontFace font, double fontSize) {
                return text.contains("7") ? 100 : new FixedMetrics().width(text, font, fontSize);
            }
        };
        StringBuilder doc = new StringBuilder("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: "
                + PAGE + "\n\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n");
        // El primer bloque es un texto con {number of: n}: el culpable es la referencia de página que cambia, no la
        // primera referencia del documento (pruebas de mutación). Mide y ocupa lo mismo que los demás.
        doc.append("\tText: Punto {number of: n}\n\t\tBox:\n\t\t\tHeight: 100\n");
        for (int i = 1; i < 6; i++) {
            doc.append("\tBlock:\n\t\tBox:\n\t\t\tHeight: 100\n");
        }
        doc.append("\tBlock:\n\t\tBox:\n\t\t\tHeight: 76\n")
                .append("\tText: Ver página {page of: x}\n\t\tBox:\n\t\t\tWidth: 62\n")
                .append("\tBlock:\n\t\tId: x\n\t\tBox:\n\t\t\tHeight: 10\n")
                .append("\tList:\n\t\tMarker: decimal\n\t\tBox:\n\t\t\tPadding: 0 0 0 20\n\t\tList item:\n\t\t\tId: n\n\t\t\tText: uno\n");
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> new Engine(wideSevens).layOutDocument(new DocumentReader().read(Docs.sectioned(doc.toString()))));
        assertEquals(ErrorCode.REFERENCE_UNSTABLE, e.errorCode());
        // Cabecera (11 líneas), seis bloques de 3, el de 76 (3) y la línea del texto.
        assertEquals(13 + 6 * 3 + 3 + 1, e.line());
    }
}
