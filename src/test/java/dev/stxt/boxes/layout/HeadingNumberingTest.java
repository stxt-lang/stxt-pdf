package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.Fragment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La numeración de los encabezados, {@code Numbered} (referencia/componentes/heading.md), ya maquetada. Con
 * {@code FixedMetrics} y {@code Size: 10}, cada carácter mide 5 pt y cada línea, 12.
 */
class HeadingNumberingTest {

    private static final String[] NUMBERED = {
            "\tStyles:", "\t\tElement: Heading 1", "\t\t\tTypography:", "\t\t\t\tNumbered: true",
            "\t\tElement: Heading 2", "\t\t\tTypography:", "\t\t\t\tNumbered: true"};

    private static String[] document(String... body) {
        return java.util.stream.Stream.concat(java.util.Arrays.stream(NUMBERED), java.util.Arrays.stream(body))
                .toArray(String[]::new);
    }

    private static String text(Fragment f) {
        return f.lines().stream().map(x -> x.text()).reduce((a, b) -> a + "|" + b).orElse("");
    }

    @Test
    void theNumberIsWrittenInTheFirstLineOfTheTitleAndMeasuredWithIt() {
        LayoutFixture l = layOut(document(
                "\tHeading 1:", "\t\tText: Resultados", "\t\t\tId: t1",
                "\tHeading 2:", "\t\tText: Ventas", "\t\t\tId: t2"));
        assertEquals("1 Resultados", text(l.fragment("t1")));
        assertEquals("1.1 Ventas", text(l.fragment("t2")));
        // Ocupa lo que escribe, número incluido: «1 Resultados» son 12 caracteres.
        assertEquals(12 * 5, l.fragment("t1").lines().get(0).width(), 1e-9);
        assertEquals(10 * 5, l.fragment("t2").lines().get(0).width(), 1e-9);
    }

    @Test
    void numberOfGivesTheNumberOfAHeading() {
        LayoutFixture l = layOut(document(
                "\tHeading 1:", "\t\tText: Uno",
                "\tHeading 1:", "\t\tText: Dos",
                "\tHeading 2:", "\t\tId: ventas", "\t\tText: Ventas",
                "\tText: Véase la sección {number of: ventas} y el capítulo {number of: cap}.", "\t\tId: ref",
                "\tHeading 1:", "\t\tId: cap", "\t\tText: Tres"));
        // La referencia puede ser hacia delante: el número sale del árbol, no de la paginación.
        assertEquals("Véase la sección 2.1 y el capítulo 3.", text(l.fragment("ref")));
    }

    @Test
    void aLevelWithoutNumberingIsWrittenAsItIs() {
        LayoutFixture l = layOut(document(
                "\tHeading 1:", "\t\tText: Uno",
                "\tHeading 2:", "\t\tText: Dos",
                "\tHeading 3:", "\t\tText: Detalle", "\t\t\tId: t3"));
        // Solo los niveles 1 y 2 tienen Numbered: true.
        assertEquals("Detalle", text(l.fragment("t3")));
    }

    @Test
    void theContentsEntriesCarryTheNumber() {
        LayoutFixture l = layOut(document(
                "\tContents:", "\t\tId: toc", "\t\tLevels: 1 2",
                "\tHeading 1:", "\t\tText: Uno",
                "\tHeading 2:", "\t\tText: Dos"));
        List<Fragment> entries = l.fragment("toc").children();
        assertEquals(List.of("1 Uno", "1.1 Dos"), entries.stream().map(e -> text(e.children().get(0))).toList());
    }
}
