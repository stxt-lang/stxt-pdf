package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.TextFormat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Case}, {@code Letter spacing} y {@code Small caps} (referencia/comun/formato-del-texto.md). Con {@link
 * FixedMetrics}, cada carácter mide 5 pt con {@code Size: 10}.
 */
class TextTypographyTest {

    /** Las líneas de un {@code Text} de 100 pt de ancho con estas propiedades en su {@code Typography}. */
    private static List<TextLine> lines(String text, String... typography) {
        List<String> doc = new ArrayList<>(List.of("\tText: " + text, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100",
                "\t\tTypography:"));
        for (String p : typography) {
            doc.add("\t\t\t" + p);
        }
        return layOut(doc.toArray(String[]::new)).fragment("T").lines();
    }

    @Test
    void uppercaseChangesWhatIsDrawnAndTheCharacterCount() {
        List<TextLine> lines = lines("Straße", "Case: uppercase");
        assertEquals("STRASSE", lines.get(0).text());
        assertEquals(35, lines.get(0).width(), 1e-9);
    }

    @Test
    void transformIsInherited() {
        LayoutFixture l = layOut("\tBlock:", "\t\tTypography:", "\t\t\tCase: uppercase",
                "\t\tText: hola", "\t\t\tId: A",
                "\t\tText: hola", "\t\t\tId: B", "\t\t\tTypography:", "\t\t\t\tCase: capitalize");
        assertEquals("HOLA", l.fragment("A").lines().get(0).text());
        assertEquals("Hola", l.fragment("B").lines().get(0).text());
    }

    @Test
    void theTextOfTheBoxIsNotChanged() {
        LayoutFixture l = layOut("\tText: hola", "\t\tId: T", "\t\tTypography:", "\t\t\tCase: uppercase");
        assertEquals("hola", l.fragment("T").box().text());
    }

    @Test
    void letterSpacingWidensTheLineExceptAfterTheLastCharacter() {
        TextLine line = lines("abcd", "Letter spacing: 2").get(0);
        assertEquals(20 + 3 * 2, line.width(), 1e-9);
        assertEquals(26, line.runs().get(0).width(), 1e-9);
    }

    @Test
    void letterSpacingCanBeNegative() {
        assertEquals(20 - 3, lines("abcd", "Letter spacing: -1").get(0).width(), 1e-9);
    }

    @Test
    void letterSpacingChangesTheBreaks() {
        // «aaaa bbbb» mide 45 y cabe en 45; con 1 pt de interletraje mide 53.
        LayoutFixture fits = layOut("\tText: aaaa bbbb", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 45");
        assertEquals(1, fits.fragment("T").lines().size());
        LayoutFixture wider = layOut("\tText: aaaa bbbb", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 45",
                "\t\tTypography:", "\t\t\tLetter spacing: 1");
        assertEquals(List.of("aaaa", "bbbb"), wider.fragment("T").lines().stream().map(TextLine::text).toList());
    }

    @Test
    void letterSpacingCountsInTheIntrinsicWidth() {
        // Un ítem flex sin ancho mide su ancho natural: «abcd» con 2 de interletraje, 26.
        LayoutFixture l = layOut("\tFlex:", "\t\tItem:", "\t\t\tText: abcd", "\t\t\t\tId: T",
                "\t\t\t\tTypography:", "\t\t\t\t\tLetter spacing: 2");
        assertEquals(26, l.rect("T").width(), 1e-9);
    }

    @Test
    void alignmentUsesTheWidthWithoutTheLastSpacing() {
        TextLine line = lines("ab", "Letter spacing: 2", "Align: right").get(0);
        assertEquals(12, line.width(), 1e-9);
        assertEquals(88, line.x(), 1e-9);
    }

    @Test
    void justifyDistributesTheRestAfterTheSpacing() {
        TextLine line = lines("aaaa bbbb cccc dddd", "Letter spacing: 1", "Align: justify").get(0);
        // «aaaa bbbb cccc» mide 70 + 13 = 83 y, justificada, se llena hasta 100.
        assertEquals("aaaa bbbb cccc", line.text());
        assertEquals(100, line.width(), 1e-9);
    }

    @Test
    void smallCapsAreDrawnAtEightyPercent() {
        TextLine line = lines("Ab", "Small caps: true").get(0);
        assertEquals("AB", line.text());
        assertEquals(5 + 4, line.width(), 1e-9);
        assertEquals(2, line.runs().size());
        assertFalse(line.runs().get(0).format().smallCaps());
        assertTrue(line.runs().get(1).format().smallCaps());
        assertEquals(TextFormat.SMALL_CAPS_SCALE, line.runs().get(1).format().scale(), 1e-9);
    }

    @Test
    void smallCapsKeepTheSpacesAtFullSize() {
        // a (4) + espacio (5) + b (4)
        assertEquals(13, lines("a b", "Small caps: true").get(0).width(), 1e-9);
    }

    @Test
    void aPageMarkerIsNumberedAfterTheTransform() {
        LayoutFixture l = layOutPaged(180, "\tPage overlay:", "\t\tOffset: auto 0 -30 0", "\t\tTypography:",
                "\t\t\tCase: uppercase",
                "\t\tText: Página {page} de {pages}", "\t\t\tId: footer",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 150");
        assertEquals(2, l.pages());
        assertEquals("PÁGINA 1 DE 2", l.fragment(1, "footer").lines().get(0).text());
        assertEquals("PÁGINA 2 DE 2", l.fragment(2, "footer").lines().get(0).text());
    }
}
