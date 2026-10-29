package dev.stxt.boxes.layout.multicol;

import dev.stxt.boxes.layout.LayoutFixture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.boxes.layout.LayoutFixture.layOutPaged;
import static dev.stxt.boxes.layout.multicol.MulticolLayoutTest.assertRect;
import static dev.stxt.boxes.layout.multicol.MulticolLayoutTest.block;
import static dev.stxt.boxes.layout.multicol.MulticolLayoutTest.multicol;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Bloques en columnas en páginas de 180 pt con márgenes de 40: área útil de 515 × 100 desde
 * (40, 40). Con {@code Count: 2} y {@code Column gap: 15}, la segunda columna empieza en x = 305.
 */
class MulticolFragmentationTest {

    private static final double PAGE = 180;
    private static final List<String> TWO = List.of("Count: 2", "Column gap: 15");

    /** Bloques de 25 con {@code Id} A, B, C…: caben cuatro en una columna. */
    private static List<String>[] blocks(int n) {
        return blocks(n, false);
    }

    /** Ídem; {@code whole}: con {@code Inside: avoid}. */
    @SuppressWarnings("unchecked")
    private static List<String>[] blocks(int n, boolean whole) {
        List<List<String>> blocks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            blocks.add(block(String.valueOf((char) ('A' + i)), 25, whole));
        }
        return blocks.toArray(List[]::new);
    }

    @Test
    void fillsTheColumnsOfThePageAndBalancesTheLastOne() {
        LayoutFixture l = layOutPaged(PAGE, multicol(TWO, blocks(10)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "M"));
        assertRect(40, 115, 250, 25, l.rect(1, "D"));
        assertRect(305, 40, 250, 25, l.rect(1, "E"));
        assertRect(305, 115, 250, 25, l.rect(1, "H"));
        // Página 2: I y J, equilibrados en dos columnas de 25.
        assertRect(40, 40, 515, 25, l.rect(2, "M"));
        assertRect(40, 40, 250, 25, l.rect(2, "I"));
        assertRect(305, 40, 250, 25, l.rect(2, "J"));
    }

    @Test
    void fillAutoDoesNotBalanceTheLastPage() {
        LayoutFixture l = layOutPaged(PAGE, multicol(List.of("Count: 2", "Column gap: 15", "Fill: auto"), blocks(10)));
        assertRect(40, 40, 515, 50, l.rect(2, "M"));
        assertRect(40, 65, 250, 25, l.rect(2, "J"));
    }

    @Test
    void pageBreakLeavesTheRemainingColumns() {
        LayoutFixture l = layOutPaged(PAGE, multicol(TWO, block("A", 25, false),
                block("B", 25, false, "\t\t\tBreak:", "\t\t\t\tBefore: page"), block("C", 25, false)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 25, l.rect(1, "M"));
        assertFalse(l.has(1, "B"));
        assertRect(40, 40, 250, 25, l.rect(2, "B"));
        assertRect(305, 40, 250, 25, l.rect(2, "C"));
    }

    @Test
    void columnBreakInTheLastColumnGoesToTheNextPage() {
        List<String> column = List.of("\t\t\tBreak:", "\t\t\t\tBefore: column");
        LayoutFixture l = layOutPaged(PAGE, multicol(TWO, block("A", 25, false),
                block("B", 25, false, column.toArray(String[]::new)),
                block("C", 25, false, "\t\t\tBreak:", "\t\t\t\tBefore: column", "\t\t\t\tInside: avoid")));
        assertEquals(2, l.pages());
        assertRect(305, 40, 250, 25, l.rect(1, "B"));
        assertRect(40, 40, 250, 25, l.rect(2, "C"));
    }

    /** La primera columna no puede colocar ninguna línea en los 10 pt que quedan: el Multicol pasa entero a la página 2. */
    @Test
    void multicolThatCannotStartMovesToTheNextPage() {
        LayoutFixture l = layOutPaged(PAGE, "\tBlock:", "\t\tBox:", "\t\t\tHeight: 90",
                "\tMulticol:", "\t\tId: M", "\t\tCount: 2", "\t\tText: Hola");
        assertFalse(l.has(1, "M"));
        assertRect(40, 40, 515, 12, l.rect(2, "M"));
    }

    /** Partir un bloque con {@code Inside: avoid} es el peor corte: antes se corta delante del Multicol. */
    @Test
    void avoidInsideInTheFirstColumnMovesTheMulticol() {
        List<String> lines = new ArrayList<>(List.of("\tBlock:", "\t\tBox:", "\t\t\tHeight: 90"));
        lines.addAll(List.of(multicol(TWO, block("A", 25, true))));
        LayoutFixture l = layOutPaged(PAGE, lines.toArray(String[]::new));
        assertFalse(l.has(1, "M"));
        assertRect(40, 40, 250, 25, l.rect(2, "A"));
    }

    /** Un {@code Height} de 150 es el total: 100 en la página 1 y 50 en la 2, que se llenan en orden. */
    @Test
    void heightAcrossPages() {
        LayoutFixture l = layOutPaged(PAGE, multicol(List.of("Count: 2", "Column gap: 15", "Fill: auto", "Box:",
                "\tHeight: 150"), blocks(10)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "M"));
        assertRect(305, 115, 250, 25, l.rect(1, "H"));
        assertRect(40, 40, 515, 50, l.rect(2, "M"));
        assertRect(40, 65, 250, 25, l.rect(2, "J"));
    }

    @Test
    void cloneDecorationRepeatsBorderAndPadding() {
        LayoutFixture l = layOutPaged(PAGE, multicol(List.of("Count: 2", "Column gap: 15", "Box:", "\tPadding: 5",
                "Break:", "\tDecoration: clone"), blocks(8, true)));
        // Columnas de 90 entre los 5 de arriba y de abajo, y de 245 de ancho: tres bloques enteros
        // por columna en la página 1.
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 85, l.rect(1, "M"));
        assertRect(305, 45, 245, 25, l.rect(1, "D"));
        assertRect(305, 95, 245, 25, l.rect(1, "F"));
        assertRect(40, 40, 515, 35, l.rect(2, "M"));
        assertRect(45, 45, 245, 25, l.rect(2, "G"));
        assertRect(305, 45, 245, 25, l.rect(2, "H"));
    }

    // Casos que faltaban según las pruebas de mutación (ROADMAP-QA-ARQ.md, QA-08).

    /**
     * Height: 160 con clone y relleno de 5: en la página 1 las columnas miden 90 (las limita la página); en la 2 les
     * quedan 160 − 10 − 90 = 60, dos bloques de 25 por columna.
     */
    @Test
    void cloneWithHeightLeavesTheRestToTheLastPage() {
        LayoutFixture l = layOutPaged(PAGE, multicol(List.of("Count: 2", "Column gap: 15", "Fill: auto", "Box:",
                "\tHeight: 160", "\tPadding: 5", "Break:", "\tDecoration: clone"), blocks(10, true)));
        assertEquals(2, l.pages());
        assertRect(305, 95, 245, 25, l.rect(1, "F"));
        assertRect(45, 70, 245, 25, l.rect(2, "H"));
        assertRect(305, 45, 245, 25, l.rect(2, "I"));
        assertRect(305, 70, 245, 25, l.rect(2, "J"));
    }

    /** El contenido cabe en la página 1, pero Height: 150 sigue: la página 2 tiene el resto de la caja, vacío. */
    @Test
    void theHeightGoesOnAfterTheContent() {
        LayoutFixture l = layOutPaged(PAGE, multicol(List.of("Count: 2", "Column gap: 15", "Box:", "\tHeight: 150"),
                blocks(2)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "M"));
        assertRect(40, 40, 515, 50, l.rect(2, "M"));
    }
}
