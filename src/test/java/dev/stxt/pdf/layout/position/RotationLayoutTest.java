package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Rotate} en el layout (referencia/comun/capas-y-giro.md, referencia/comun/cortes.md y
 * referencia/comun/desbordamiento.md): un posicionado girado no mueve nada, una caja girada en flujo ocupa el
 * rectángulo que la envuelve, una caja girada no se parte, y lo que se comprueba contra la página es lo que se pinta.
 */
class RotationLayoutTest {

    /**
     * La caja girada ocupa su sitio sin girar: su rectángulo y el de su hermana son los de siempre.
     * Va detrás de un bloque de 100 porque, girada 30 grados, sube unos 72 pt sobre su centro.
     */
    @Test
    void rotationDoesNotChangeTheLayout() {
        LayoutFixture l = LayoutFixture.layOut("\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tRelative:", "\t\tId: R", "\t\tRotate: 30", "\t\tBox:", "\t\t\tWidth: 200", "\t\t\tHeight: 50",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(new Rect(40, 140, 200, 50), l.rect("R"));
        assertEquals(new Rect(40, 190, 515, 10), l.rect("B"));
    }

    /**
     * Área útil de 100: tras un bloque de 60 quedan 40, y el {@code Relative} girado mide 60. Sin
     * girar se partiría entre sus dos bloques; girado pasa entero a la página siguiente.
     */
    @Test
    void aRotatedRelativeIsNotSplit() {
        String[] content = {
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 60",
                "\tRelative:", "\t\tId: R", "{}", "\t\tBlock:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tHeight: 30",
                "\t\tBlock:", "\t\t\tId: Z", "\t\t\tBox:", "\t\t\t\tHeight: 30"};
        LayoutFixture plain = LayoutFixture.layOutPaged(180, with(content, "\t\tLayer: 1"));
        assertTrue(plain.has(1, "R"));
        assertTrue(plain.has(1, "A"));
        assertTrue(plain.has(2, "Z"));

        LayoutFixture turned = LayoutFixture.layOutPaged(180, with(content, "\t\tRotate: 180"));
        assertFalse(turned.has(1, "R"));
        assertEquals(new Rect(40, 40, 515, 60), turned.rect(2, "R"));
        assertEquals(new Rect(40, 70, 515, 30), turned.rect(2, "Z"));
    }

    /** Al principio de una página no hay adónde pasar: si cabe, se coloca entera. */
    @Test
    void aRotatedRelativeThatFitsStaysOnItsPage() {
        LayoutFixture l = LayoutFixture.layOutPaged(180,
                "\tRelative:", "\t\tId: R", "\t\tRotate: -5", "\t\tBox:", "\t\t\tHeight: 100");
        assertEquals(1, l.pages());
        assertEquals(new Rect(40, 40, 515, 100), l.rect("R"));
    }

    /**
     * Un {@code Absolute} de 60 × 100 en y = 760 de la página se sale por abajo sin girar (llega a
     * 860 de 842), pero girado 90 grados alrededor de su centro (30, 50) ocupa de 780 a 840: cabe.
     */
    @Test
    void aRotatedBoxIsCheckedAsItIsPainted() {
        String[] lines = {"\tAbsolute:", "\t\tId: S", "\t\tOffset: 720 auto auto 0", "{}",
                "\t\tBox:", "\t\t\tWidth: 60", "\t\t\tHeight: 100"};
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> LayoutFixture.layOut(with(lines, "\t\tLayer: 1")));
        assertEquals(ErrorCode.BOX_OVERFLOW, e.errorCode());
        LayoutFixture l = LayoutFixture.layOut(with(lines, "\t\tRotate: 90"));
        assertEquals(new Rect(40, 760, 60, 100), l.rect("S"));
    }

    /** Al revés: cabe sin girar, pero girada sale por encima de la página. */
    @Test
    void aRotatedBoxThatLeavesThePageIsAnOverflow() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> LayoutFixture.layOut(
                "\tRelative:", "\t\tRotate: -90", "\t\tRotate origin: top left", "\t\tBox:", "\t\t\tWidth: 100",
                "\t\t\tHeight: 10"));
        assertEquals(ErrorCode.BOX_OVERFLOW, e.errorCode());
        assertTrue(e.getMessage().contains("girada"), e.getMessage());
    }

    // Giro en flujo: Block, Text, Paragraph e Image ocupan el rectángulo que los envuelve girados. Con FixedMetrics y
    // Size 10, cada carácter mide 5 pt y cada línea, 12.

    /**
     * Un {@code Text} girado −90 sin {@code Width} mide su texto en una línea, 25 × 12, y ocupa 12 × 25: lo siguiente
     * va 25 más abajo. Sus líneas no se parten.
     */
    @Test
    void aTextRotatedInFlowTakesTheRectangleAroundIt() {
        LayoutFixture l = LayoutFixture.layOut("\tText: Lunes", "\t\tId: T", "\t\tRotate: -90",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(new Rect(40, 40, 12, 25), l.rect("T"));
        assertEquals(new Rect(40, 65, 515, 10), l.rect("B"));
        // Su caja sin girar, centrada en la que ocupa.
        assertEquals(new Rect(-6.5, 6.5, 25, 12), l.fragment("T").turned());
        assertEquals(1, l.fragment("T").lines().size());
    }

    /** Con {@code Width}, la caja girada reparte su texto en ese ancho: dos líneas de 50 ocupan 24 × 50. */
    @Test
    void aRotatedBoxWithAWidthWrapsItsText() {
        LayoutFixture l = LayoutFixture.layOut("\tText: aaa bbb ccc", "\t\tId: T", "\t\tRotate: 90", "\t\tBox:",
                "\t\t\tWidth: 50");
        assertEquals(new Rect(40, 40, 24, 50), l.rect("T"));
        assertEquals(2, l.fragment("T").lines().size());
    }

    /** Lo que aporta a su contenedor es el ancho girado: un ítem de Flex con una caja girada dentro mide 12. */
    @Test
    void theIntrinsicWidthOfARotatedBoxIsItsTurnedWidth() {
        LayoutFixture l = LayoutFixture.layOut("\tFlex:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: I",
                "\t\t\t\tText: Lunes", "\t\t\t\t\tRotate: -90");
        assertEquals(new Rect(40, 40, 12, 25), l.rect("I"));
    }

    /** Los márgenes {@code auto} la centran por lo que ocupa: (515 − 12) / 2. */
    @Test
    void autoMarginsCenterTheTurnedRectangle() {
        LayoutFixture l = LayoutFixture.layOut("\tText: Lunes", "\t\tId: T", "\t\tRotate: 90", "\t\tBox:",
                "\t\t\tMargin: 0 auto");
        assertEquals(new Rect(291.5, 40, 12, 25), l.rect("T"));
    }

    /**
     * Un {@code Block} de 100 × 20 girado 90 ocupa 20 × 100 en (40, 40), y su caja sin girar está en (0, 80). Es el
     * contenedor de referencia del {@code Absolute} de dentro, que se coloca en su caja sin girar; el margen de su
     * hijo no sale de él.
     */
    @Test
    void aRotatedBlockContainsItsAbsolutesAndItsMargins() {
        LayoutFixture l = LayoutFixture.layOut("\tBlock:", "\t\tId: R", "\t\tRotate: 90", "\t\tBox:",
                "\t\t\tWidth: 100",
                "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tMargin: 10 0 0 0",
                "\t\tAbsolute:", "\t\t\tId: A", "\t\t\tOffset: 0 0 auto auto", "\t\t\tBox:", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10");
        assertEquals(new Rect(40, 40, 20, 100), l.rect("R"));
        assertEquals(new Rect(0, 90, 100, 10), l.rect("C"));
        assertEquals(new Rect(90, 80, 10, 10), l.rect("A"));
    }

    /** No se parte: con 40 libres y 50 de alto girada, pasa entera a la página siguiente. */
    @Test
    void aBoxRotatedInFlowIsNotSplit() {
        LayoutFixture l = LayoutFixture.layOutPaged(180, "\tBlock:", "\t\tBox:", "\t\t\tHeight: 60",
                "\tText: aaa bbb ccc", "\t\tId: T", "\t\tRotate: 90", "\t\tBox:", "\t\t\tWidth: 50");
        assertFalse(l.has(1, "T"));
        assertEquals(new Rect(40, 40, 24, 50), l.rect(2, "T"));
    }

    /** Sin {@code Width}, un texto largo no parte sus líneas: girado poco, no cabe a lo ancho. */
    @Test
    void aLongRotatedTextWithoutAWidthIsTooWide() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> LayoutFixture.layOut(
                "\tText: " + "palabra ".repeat(20).strip(), "\t\tRotate: 5"));
        assertEquals(ErrorCode.BOX_OVERFLOW, e.errorCode());
        assertTrue(e.getMessage().contains("dale un Width"), e.getMessage());
    }

    /** Las líneas del documento con {@code {}} sustituida por {@code line}. */
    private static String[] with(String[] lines, String line) {
        return java.util.Arrays.stream(lines).map(s -> s.equals("{}") ? line : s).toArray(String[]::new);
    }
}
