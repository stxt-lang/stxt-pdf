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
 * referencia/comun/desbordamiento.md): no mueve nada, una caja girada no se parte, y lo que se comprueba contra la
 * página es lo que se pinta.
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

    /** Las líneas del documento con {@code {}} sustituida por {@code line}. */
    private static String[] with(String[] lines, String line) {
        return java.util.Arrays.stream(lines).map(s -> s.equals("{}") ? line : s).toArray(String[]::new);
    }
}
