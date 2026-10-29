package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La numeración de los encabezados, {@code Numbered} (referencia/componentes/heading.md), al construir el árbol. */
class HeadingNumbersTest {

    /** Lo que {@code Element: Document} añade para numerar todos los encabezados. */
    private static final String NUMBERED = "\t\t\t\tNumbered: true\n";

    /**
     * Construye el árbol de un documento. {@code documentTypography} son líneas más de la regla
     * {@code Element: Document} (con cuatro tabuladores) y {@code rules}, reglas más de {@code Styles}
     * (con dos); {@code lines}, el contenido.
     */
    private static BoxTreeBuilder.Result build(String documentTypography, String rules, String... lines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + documentTypography + rules + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc))));
    }

    /** Los encabezados del documento, en orden. */
    private static List<Box> headings(BoxTreeBuilder.Result tree) {
        List<Box> out = new ArrayList<>();
        collect(tree.root(), out);
        return out;
    }

    private static void collect(Box box, List<Box> out) {
        if (box.headingLevel() > 0) {
            out.add(box);
        }
        box.children().forEach(c -> collect(c, out));
    }

    private static List<String> numbers(BoxTreeBuilder.Result tree) {
        return headings(tree).stream().map(h -> h.number()).toList();
    }

    private static List<String> titles(BoxTreeBuilder.Result tree) {
        return headings(tree).stream().map(Box::headingText).toList();
    }

    @Test
    void headingsAreNotNumberedByDefault() {
        BoxTreeBuilder.Result tree = build("", "", "\tHeading 1:", "\t\tText: Uno", "\tHeading 2:", "\t\tText: Dos");
        assertEquals(java.util.Arrays.asList(null, null), numbers(tree));
        assertEquals(List.of("Uno", "Dos"), titles(tree));
    }

    @Test
    void numbersAreJoinedByLevel() {
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tHeading 1:", "\t\tText: A",
                "\tHeading 2:", "\t\tText: B",
                "\tHeading 2:", "\t\tText: C",
                "\tHeading 3:", "\t\tText: D",
                "\tHeading 1:", "\t\tText: E",
                "\tHeading 2:", "\t\tText: F");
        assertEquals(List.of("1", "1.1", "1.2", "1.2.1", "2", "2.1"), numbers(tree));
    }

    @Test
    void theNumberIsWrittenInFrontOfTheTitle() {
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tHeading 1:", "\t\tText: Resultados", "\t\tText: del trimestre",
                "\tHeading 2:", "\t\tText: Ventas");
        Box h1 = headings(tree).get(0);
        assertEquals("1 Resultados", h1.children().get(0).text());
        // Solo el primer Text lleva el número.
        assertEquals("del trimestre", h1.children().get(1).text());
        assertEquals("1 Resultados del trimestre", h1.headingText());
        assertEquals("1.1 Ventas", headings(tree).get(1).headingText());
    }

    @Test
    void aRuleNumbersOnlyTheLevelsItNames() {
        BoxTreeBuilder.Result tree = build("",
                "\t\tElement: Heading 1\n\t\t\tTypography:\n\t\t\t\tNumbered: true\n"
                        + "\t\tElement: Heading 2\n\t\t\tTypography:\n\t\t\t\tNumbered: true\n",
                "\tHeading 1:", "\t\tText: A",
                "\tHeading 2:", "\t\tText: B",
                "\tHeading 3:", "\t\tText: C",
                "\tHeading 1:", "\t\tText: D");
        assertEquals(java.util.Arrays.asList("1", "1.1", null, "2"), numbers(tree));
        assertEquals(List.of("1 A", "1.1 B", "C", "2 D"), titles(tree));
    }

    @Test
    void aHeadingWithNumberedFalseNeitherCountsNorIsNumbered() {
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tHeading 1:", "\t\tText: Portada", "\t\tTypography:", "\t\t\tNumbered: false",
                "\tHeading 1:", "\t\tText: Uno",
                "\tHeading 1:", "\t\tText: Dos");
        assertEquals(java.util.Arrays.asList(null, "1", "2"), numbers(tree));
        assertEquals("Portada", headings(tree).get(0).headingText());
    }

    @Test
    void aClassRuleCanTurnNumberingOff() {
        BoxTreeBuilder.Result tree = build(NUMBERED,
                "\t\tFor class: annex\n\t\t\tTypography:\n\t\t\t\tNumbered: false\n",
                "\tHeading 1:", "\t\tText: Uno",
                "\tHeading 1:", "\t\tClass: annex", "\t\tText: Anexo",
                "\tHeading 1:", "\t\tText: Dos");
        assertEquals(java.util.Arrays.asList("1", null, "2"), numbers(tree));
    }

    @Test
    void anUnnumberedHeadingRestartsTheCountOfTheLevelsBelow() {
        BoxTreeBuilder.Result tree = build(NUMBERED,
                "\t\tFor class: plain\n\t\t\tTypography:\n\t\t\t\tNumbered: false\n",
                "\tHeading 1:", "\t\tText: A",
                "\tHeading 2:", "\t\tText: B",
                "\tHeading 2:", "\t\tClass: plain", "\t\tText: C",
                "\tHeading 1:", "\t\tText: D",
                "\tHeading 2:", "\t\tText: E");
        // Las secciones de C no se numeran, pero la E vuelve a ser 2.1 porque D abre el capítulo 2.
        assertEquals(java.util.Arrays.asList("1", "1.1", null, "2", "2.1"), numbers(tree));
    }

    @Test
    void numberingContinuesAcrossSections() {
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tSection:", "\t\tHeading 1:", "\t\t\tText: A", "\t\tHeading 2:", "\t\t\tText: B",
                "\tSection:", "\t\tHeading 2:", "\t\t\tText: C", "\t\tHeading 1:", "\t\t\tText: D");
        assertEquals(List.of("1", "1.1", "1.2", "2"), numbers(tree));
    }

    @Test
    void markdownHeadingsAreNumberedLikeTheOthers() {
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tHeading 1:", "\t\tText: Uno",
                "\tMarkdown:", "\t\tContent >>", "\t\t\t## Alfa", "\t\t\t", "\t\t\tTexto.", "\t\t\t", "\t\t\t## Beta",
                "\tHeading 1:", "\t\tText: Dos");
        assertEquals(List.of("1", "1.1", "1.2", "2"), numbers(tree));
        assertEquals(List.of("1 Uno", "1.1 Alfa", "1.2 Beta", "2 Dos"), titles(tree));
    }

    @Test
    void theNumberIsAPlainRunBeforeTheFormattedOnes() {
        BoxTreeBuilder.Result tree = build(NUMBERED + "\t\t\t\tInline: markdown\n", "",
                "\tHeading 1:", "\t\tText: Un **gran** título");
        Box text = headings(tree).get(0).children().get(0);
        assertEquals("1 Un gran título", text.text());
        assertEquals(4, text.formattedRuns().size());
        assertEquals("1 ", text.formattedRuns().get(0).text());
        assertEquals(dev.stxt.boxes.model.TextFormat.PLAIN, text.formattedRuns().get(0).format());
        assertTrue(text.formattedRuns().get(2).format().bold());
    }

    @Test
    void numberedHeadingsUnderAnUnnumberedTitleCountFromOne() {
        // El caso normal: un título (Heading 1) sin número y las secciones debajo, numeradas.
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tHeading 1:", "\t\tText: Contrato", "\t\tTypography:", "\t\t\tNumbered: false",
                "\tHeading 2:", "\t\tText: Reunidos",
                "\tHeading 2:", "\t\tText: Cláusulas",
                "\tHeading 3:", "\t\tText: Objeto",
                "\tHeading 2:", "\t\tText: Firmas");
        assertEquals(java.util.Arrays.asList(null, "1", "2", "2.1", "3"), numbers(tree));
        assertEquals(List.of("Contrato", "1 Reunidos", "2 Cláusulas", "2.1 Objeto", "3 Firmas"), titles(tree));
    }

    @Test
    void anUnnumberedLevelInTheMiddleDoesNotAddToTheNumber() {
        BoxTreeBuilder.Result tree = build(NUMBERED,
                "\t\tFor class: plain\n\t\t\tTypography:\n\t\t\t\tNumbered: false\n",
                "\tHeading 1:", "\t\tText: A",
                "\tHeading 2:", "\t\tClass: plain", "\t\tText: B",
                "\tHeading 3:", "\t\tText: C",
                "\tHeading 3:", "\t\tText: D",
                "\tHeading 2:", "\t\tClass: plain", "\t\tText: E",
                "\tHeading 3:", "\t\tText: F");
        // El Heading 3 cuenta dentro de su Heading 2, aunque este no tenga número, y empieza otra vez en cada uno.
        assertEquals(java.util.Arrays.asList("1", null, "1.1", "1.2", null, "1.1"), numbers(tree));
    }

    @Test
    void aNewUnnumberedTitleRestartsTheCountBelow() {
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tHeading 1:", "\t\tText: Parte uno", "\t\tTypography:", "\t\t\tNumbered: false",
                "\tHeading 2:", "\t\tText: A",
                "\tHeading 2:", "\t\tText: B",
                "\tHeading 1:", "\t\tText: Parte dos", "\t\tTypography:", "\t\t\tNumbered: false",
                "\tHeading 2:", "\t\tText: C");
        assertEquals(java.util.Arrays.asList(null, "1", "2", null, "1"), numbers(tree));
    }

    @Test
    void aHeadingNumberIsAvailableToReferences() {
        BoxTreeBuilder.Result tree = build(NUMBERED, "",
                "\tHeading 1:", "\t\tText: Uno",
                "\tHeading 2:", "\t\tId: ventas", "\t\tText: Ventas",
                "\tText: Véase la sección {number of: ventas}.");
        assertEquals("1.1", headings(tree).get(1).number());
    }
}
