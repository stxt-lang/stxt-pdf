package dev.stxt.boxes.layout;

import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los desbordamientos (ESPECIFICACION.md §5.12): el culpable que señala {@link Overflow} y las medidas de su mensaje.
 * El catálogo de errores comprueba el código y la línea de cada caso; estos tests, con {@code FixedMetrics} (cada
 * carácter mide 5 con {@code Size: 10} y una línea 12), lo que el catálogo no mira: qué caja se nombra y qué medidas
 * da el mensaje (QA-08, mutación).
 */
class OverflowTest {

    private static final double PAGE = 180;

    private static InvalidModelException overflow(ErrorCode code, InvalidModelException e) {
        assertEquals(code, e.errorCode(), e.getMessage());
        return e;
    }

    private static void assertContains(String expected, InvalidModelException e) {
        assertTrue(e.getMessage().contains(expected), "«" + expected + "» no está en: " + e.getMessage());
    }

    /** La línea de la caja con ese {@code Id}, que da su nombre en el mensaje («A (línea 12)»). */
    private static int boxLine(String id, InvalidModelException e) {
        Matcher m = Pattern.compile(Pattern.quote(id) + " \\(línea (\\d+)\\)").matcher(e.getMessage());
        assertTrue(m.find(), e.getMessage());
        return Integer.parseInt(m.group(1));
    }

    /**
     * R, un ítem que no encoge, mide 600 y se sale; no declara ancho, así que se baja a lo que se sale de la página:
     * S, desde el contenido de R (borde 4 y relleno 6) y su margen de 4, ocupa de 14 a 517 en un área de 515.
     */
    @Test
    void tooWideNamesTheInnermostBoxOutsideThePage() {
        InvalidModelException e = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class, () -> layOut(
                "\tFlex:", "\t\tItem:", "\t\t\tBasis: 600", "\t\t\tShrink: 0",
                "\t\t\tBlock:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 4", "\t\t\t\t\tPadding: 6",
                "\t\t\t\tBlock:", "\t\t\t\t\tId: S", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 503", "\t\t\t\t\t\tHeight: 10",
                "\t\t\t\t\t\tMargin: 0 0 0 4")));
        assertContains("La caja S (línea", e);
        assertContains("no cabe a lo ancho: mide 503 y el contenido de su contenedor mide 515", e);
    }

    /** Si lo de dentro no se sale de la página, la culpable es la caja que se sale (S, de 14 a 507). */
    @Test
    void tooWideNamesTheBoxWhenItsContentFits() {
        InvalidModelException e = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class, () -> layOut(
                "\tFlex:", "\t\tItem:", "\t\t\tBasis: 600", "\t\t\tShrink: 0",
                "\t\t\tBlock:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 4", "\t\t\t\t\tPadding: 6",
                "\t\t\t\tBlock:", "\t\t\t\t\tId: S", "\t\t\t\t\tBox:", "\t\t\t\t\t\tWidth: 493", "\t\t\t\t\t\tHeight: 10",
                "\t\t\t\t\t\tMargin: 0 0 0 4")));
        assertContains("La caja R (línea", e);
        assertContains("mide 600 y el contenido de su contenedor mide 515", e);
    }

    @Test
    void tooTallGivesWhereTheContentEnds() {
        // Dos líneas (24) en una caja de 20.
        InvalidModelException text = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: aaaa bbbb cccc", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tHeight: 20")));
        assertContains("El contenido de la caja T (línea", text);
        assertContains("su texto llega a 24 y la caja deja 20", text);
        // C, de 30 detrás de un bloque de 5, en una caja de 20: llega a 35.
        InvalidModelException child = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tBlock:", "\t\tId: H", "\t\tBox:", "\t\t\tHeight: 20",
                        "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 5",
                        "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 30")));
        assertContains("El contenido de la caja H (línea", child);
        assertContains("la caja C (línea", child);
        assertContains("llega a 35 y la caja deja 20", child);
    }

    /**
     * Una línea de 120 no cabe en un área útil de 100: está en el contenido de P (borde 3 y relleno 7), así que acaba
     * en 130, 30 más allá. El culpable es su Line height.
     */
    @Test
    void pageOverflowGivesHowFarItGoes() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                        "\t\tText: a", "\t\t\tId: T", "\t\t\tTypography:", "\t\t\t\tLine height: 120")));
        assertContains("La caja T (línea", e);
        assertContains("mide 120 de alto y llega 30 más allá del área útil", e);
        assertEquals(boxLine("T", e) + 3, e.line());
    }

    /** Borde y relleno de 60 cada uno no caben en 100: con los dos iguales, el culpable es el relleno. */
    @Test
    void pageOverflowOfTheBorderAndPadding() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tBorder: 30", "\t\t\tPadding: 30")));
        assertContains("mide 120 de alto y llega 20 más allá del área útil", e);
        assertEquals(boxLine("B", e) + 4, e.line());
        InvalidModelException border = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tBorder: 40", "\t\t\tPadding: 20")));
        assertEquals(boxLine("B", border) + 3, border.line());
    }

    @Test
    void aRotatedBoxDoesNotBreak() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tRelative:", "\t\tId: R", "\t\tRotate: 180", "\t\tBox:", "\t\t\tHeight: 120")));
        assertContains("La caja R (línea", e);
        assertContains("está girada, así que no se parte entre páginas, y no cabe en una: mide 120 de alto y llega 20 "
                + "más allá del área útil", e);
    }

    @Test
    void rotatedBoxesOutsideThePage() {
        InvalidModelException own = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tRelative:", "\t\tId: R", "\t\tRotate: -30", "\t\tRotate origin: bottom left",
                        "\t\tBox:", "\t\t\tHeight: 40")));
        assertContains("La caja R (línea", own);
        assertContains("girada se sale de la página: revisa su Rotate, su Rotate origin, su posición", own);
        InvalidModelException inside = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tRelative:", "\t\tRotate: 180", "\t\tBox:", "\t\t\tHeight: 100",
                        "\t\tAbsolute:", "\t\t\tId: A", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tWidth: 20",
                        "\t\t\t\tHeight: 150")));
        assertContains("La caja A (línea", inside);
        // Sin «su su»: lo que hay que revisar empieza por «su» en los dos casos.
        assertContains(", dentro de una caja girada, se sale de la página: revisa su posición y su tamaño", inside);
    }

    @Test
    void aPreformattedLineThatDoesNotFit() {
        InvalidModelException e = overflow(ErrorCode.TEXT_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: " + "a ".repeat(60), "\t\tTypography:", "\t\t\tWhite space: pre")));
        assertContains("Una línea de un texto con White space: pre", e);
        assertContains("la línea mide 595 y la caja deja 515", e);
    }

    /** Los márgenes negativos dejan la caja de borde fuera de su contenedor, pero no la de margen: no desborda. */
    @Test
    void negativeMarginsAreNotOverflows() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: N", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tMargin: 0 -10");
        assertEquals(535, l.rect("N").width(), 1e-9);
    }

    /**
     * El Flex tiene 10 de relleno (su contenido mide 495) y un primer ítem de 20: R empieza en 30 y su contenido en 40;
     * S, un Relative desplazado 60 (que no cuenta), ocupa de 44 a 504, dentro de la página: la culpable es R.
     */
    @Test
    void tooWideMeasuresFromTheContainerAndIgnoresRelativeOffsets() {
        InvalidModelException e = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class, () -> layOut(
                "\tFlex:", "\t\tBox:", "\t\t\tPadding: 10",
                "\t\tItem:", "\t\t\tShrink: 0", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 20", "\t\t\t\t\tHeight: 10",
                "\t\tItem:", "\t\t\tBasis: 600", "\t\t\tShrink: 0",
                "\t\t\tBlock:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 4", "\t\t\t\t\tPadding: 6",
                "\t\t\t\tRelative:", "\t\t\t\t\tId: S", "\t\t\t\t\tOffset: auto auto auto 60", "\t\t\t\t\tBox:",
                "\t\t\t\t\t\tWidth: 460", "\t\t\t\t\t\tHeight: 10", "\t\t\t\t\t\tMargin: 0 0 0 4")));
        assertContains("La caja R (línea", e);
        assertContains("mide 600 y el contenido de su contenedor mide 495", e);
    }

    /** P empieza 10 más abajo (su margen): la línea de 120 acaba en 10 + 10 + 120 = 140, 40 más allá. */
    @Test
    void pageOverflowCountsWhereTheBoxStarts() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                        "\t\t\tMargin: 10 0 0 0",
                        "\t\tText: a", "\t\t\tId: T", "\t\t\tTypography:", "\t\t\t\tLine height: 120")));
        assertContains("La caja T (línea", e);
        assertContains("llega 40 más allá del área útil", e);
    }

    /**
     * T va en un Relative desplazado 8 hacia abajo dentro de P: el desbordamiento se mide en su sitio, sin el
     * desplazamiento (§5.12), así que la línea de 120 acaba en 10 + 120 = 130, 30 más allá.
     */
    @Test
    void pageOverflowIgnoresRelativeOffsets() {
        InvalidModelException e = overflow(ErrorCode.PAGE_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOutPaged(PAGE, "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tBorder: 3", "\t\t\tPadding: 7",
                        "\t\tRelative:", "\t\t\tOffset: 8 auto auto auto",
                        "\t\t\tText: a", "\t\t\t\tId: T", "\t\t\t\tTypography:", "\t\t\t\t\tLine height: 120")));
        assertContains("La caja T (línea", e);
        assertContains("llega 30 más allá del área útil", e);
    }

    @Test
    void theContentBoxLeavesBorderAndPadding() {
        // 50 − 2 − 2 − 8 − 8 = 30 para la palabra de 35.
        InvalidModelException wide = overflow(ErrorCode.TEXT_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: aaaaaaa", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tBorder: 2", "\t\t\tPadding: 0 8")));
        assertContains("la línea mide 35 y la caja deja 30", wide);
        // Dos líneas (24) en 30 − 2 − 2 − 5 − 5 = 16.
        InvalidModelException tall = overflow(ErrorCode.BOX_OVERFLOW, assertThrows(InvalidModelException.class,
                () -> layOut("\tText: aaaa bbbb cccc", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tHeight: 30",
                        "\t\t\tBorder: 2", "\t\t\tPadding: 5 0")));
        assertContains("su texto llega a 24 y la caja deja 16", tall);
    }

    @Test
    void headAndFootThatLeaveNoRoomForTheBody() {
        // Cabecera de 60 y pie de 50 en un área útil de 100.
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOutPaged(PAGE,
                "\tTable:", "\t\tId: X", "\t\tColumns: 1fr",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 60",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: b",
                "\t\tFoot:", "\t\t\tRow:", "\t\t\t\tCell: c", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 50"));
        assertContains("miden 110 y no dejan sitio al cuerpo", e);
    }
}
