package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.NumberStyle;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las secciones (referencia/componentes/document.md): páginas de 300 × 200 con márgenes de 20 (área útil de
 * 260 × 160). Con {@code FixedMetrics} y {@code Size: 10}, cada carácter mide 5 pt.
 */
class SectionsTest {

    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n"
            + "\tMedia:\n\t\tWidth: 300\n\t\tHeight: 200\n\t\tMargin: 20\n"
            + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

    private static LaidOutDocument lay(Object... lines) {
        return layWith(HEADER, lines);
    }

    /** Las líneas del documento: cada parte es una línea ({@code String}) o varias ({@code String[]}). */
    private static LaidOutDocument layWith(String header, Object... parts) {
        String body = java.util.Arrays.stream(parts)
                .flatMap(p -> p instanceof String[] many ? java.util.Arrays.stream(many) : java.util.stream.Stream.of((String) p))
                .collect(Collectors.joining("\n"));
        return new Engine().layOutDocument(new DocumentReader().read(header + body));
    }

    /** Una página entera: una caja de 160 pt, el alto del área útil. */
    private static String[] fullPage(String id) {
        return new String[] {"\t\tBlock:", "\t\t\tId: " + id, "\t\t\tBox:", "\t\t\t\tHeight: 160"};
    }

    private static String[] join(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    private static Fragment find(Fragment f, String id) {
        if (id.equals(f.box().id())) {
            return f;
        }
        for (Fragment child : f.children()) {
            Fragment found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** El fragmento con ese {@code Id} en el flujo o en los {@code Fixed} de la página; {@code null} si no sale. */
    private static Fragment on(Page page, String id) {
        Fragment found = find(page.content(), id);
        for (Fragment fixed : page.fixed()) {
            found = found != null ? found : find(fixed, id);
        }
        return found;
    }

    private static List<String> fixedIds(Page page) {
        return page.fixed().stream().map(f -> f.box().id()).collect(Collectors.toList());
    }

    /** El texto de la caja con ese {@code Id}, o el de sus descendientes si ella no tiene líneas. */
    private static String text(Page page, String id) {
        return text(on(page, id));
    }

    private static String text(Fragment f) {
        String own = f.lines().stream().map(l -> l.text()).collect(Collectors.joining("|"));
        return own + f.children().stream().map(SectionsTest::text).collect(Collectors.joining("|"));
    }

    @Test
    void eachSectionStartsANewPageWithItsOwnMedia() {
        LaidOutDocument d = layWith("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tPaper: A4\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n",
                "\tSection:", "\t\tBlock:", "\t\t\tId: a",
                "\tSection:", "\t\tMedia:", "\t\t\tOrientation: landscape", "\t\tBlock:", "\t\t\tId: b",
                "\tSection:", "\t\tMedia:", "\t\t\tMargin: 10", "\t\tBlock:", "\t\t\tId: c",
                "\tSection:", "\t\tMedia:", "\t\t\tPaper: Letter", "\t\t\tMargin: 0", "\t\tBlock:", "\t\t\tId: d");
        assertEquals(4, d.pages().size());
        // Sin Media propio, el del documento.
        assertEquals(595, d.pages().get(0).media().width(), 1e-9);
        assertEquals(842, d.pages().get(0).media().height(), 1e-9);
        // Solo Orientation: el papel del documento girado, con sus márgenes.
        assertEquals(842, d.pages().get(1).media().width(), 1e-9);
        assertEquals(595, d.pages().get(1).media().height(), 1e-9);
        assertEquals(40, d.pages().get(1).media().margin().left(), 1e-9);
        // Solo Margin: el tamaño del documento.
        assertEquals(595, d.pages().get(2).media().width(), 1e-9);
        assertEquals(10, d.pages().get(2).media().margin().top(), 1e-9);
        // Un tamaño propio y un margen propio.
        assertEquals(612, d.pages().get(3).media().width(), 1e-9);
        assertEquals(0, d.pages().get(3).media().margin().left(), 1e-9);
        // El área útil de cada página es la de su medio: el ancho de la caja de la sección.
        assertEquals(515, d.pages().get(0).content().width(), 1e-9);
        assertEquals(762, d.pages().get(1).content().width(), 1e-9);
        assertEquals(575, d.pages().get(2).content().width(), 1e-9);
        assertEquals(612, d.pages().get(3).content().width(), 1e-9);
        assertEquals(List.of(0, 1, 2, 3), d.pages().stream().map(Page::section).toList());
        assertEquals(List.of(1, 2, 3, 4), d.pages().stream().map(Page::number).toList());
    }

    @Test
    void aSectionAlwaysStartsANewPage() {
        LaidOutDocument d = lay("\tSection:", "\t\tBlock:", "\t\t\tId: a", "\t\t\tBox:", "\t\t\t\tHeight: 10",
                "\tSection:", "\t\tBlock:", "\t\t\tId: b", "\t\t\tBox:", "\t\t\t\tHeight: 10");
        assertEquals(2, d.pages().size());
        assertNotNull(on(d.pages().get(0), "a"));
        assertNull(on(d.pages().get(0), "b"));
        assertNotNull(on(d.pages().get(1), "b"));
    }

    @Test
    void fixedBoxesOfTheDocumentGoEverywhereAndThoseOfASectionOnlyInIt() {
        LaidOutDocument d = lay(
                "\tFixed:", "\t\tId: foot", "\t\tOffset: auto 0 0 0", "\t\tBox:", "\t\t\tHeight: 10",
                join(new String[] {"\tSection:", "\t\tFixed:", "\t\t\tId: head-a", "\t\t\tPages: first",
                        "\t\t\tBox:", "\t\t\t\tHeight: 10"}, fullPage("a1"), fullPage("a2")),
                new String[] {"\tSection:", "\t\tInherit fixed: false", "\t\tFixed:", "\t\t\tId: head-b", "\t\t\tBox:",
                        "\t\t\t\tHeight: 10", "\t\tBlock:", "\t\t\tId: b"},
                new String[] {"\tSection:", "\t\tBlock:", "\t\t\tId: c"});
        assertEquals(4, d.pages().size());
        // Pages: first cuenta dentro de la sección: solo su primera página.
        assertEquals(List.of("foot", "head-a"), fixedIds(d.pages().get(0)));
        assertEquals(List.of("foot"), fixedIds(d.pages().get(1)));
        // Inherit fixed: false deja la sección sin los del documento.
        assertEquals(List.of("head-b"), fixedIds(d.pages().get(2)));
        assertEquals(List.of("foot"), fixedIds(d.pages().get(3)));
    }

    @Test
    void pagesOfAFixedOfTheDocumentCountTheWholeDocument() {
        LaidOutDocument d = lay(
                "\tFixed:", "\t\tId: not-first", "\t\tPages: not-first", "\t\tBox:", "\t\t\tHeight: 10",
                "\tFixed:", "\t\tId: last", "\t\tPages: last", "\t\tBox:", "\t\t\tHeight: 10",
                join(new String[] {"\tSection:"}, fullPage("a")),
                join(new String[] {"\tSection:"}, fullPage("b1"), fullPage("b2")));
        assertEquals(3, d.pages().size());
        assertEquals(List.of(), fixedIds(d.pages().get(0)));
        assertEquals(List.of("not-first"), fixedIds(d.pages().get(1)));
        assertEquals(List.of("not-first", "last"), fixedIds(d.pages().get(2)));
    }

    @Test
    void anUnnumberedCoverDoesNotCountAndTheNumberingCanRestart() {
        LaidOutDocument d = lay(
                "\tFixed:", "\t\tId: num", "\t\tOffset: auto 0 0 0", "\t\tPages: not-first", "\t\tText: Página {page} de {pages}",
                join(new String[] {"\tSection:", "\t\tInherit fixed: false", "\t\tPage numbering: none"}, fullPage("cover")),
                join(new String[] {"\tSection:", "\t\tInherit fixed: true"}, fullPage("b1"), fullPage("b2")),
                join(new String[] {"\tSection:", "\t\tPage numbering: restart"}, fullPage("annex")));
        assertEquals(4, d.pages().size());
        assertEquals(List.of(0, 1, 2, 1), d.pages().stream().map(Page::printed).toList());
        assertFalse(d.pages().get(0).numbered());
        assertEquals(List.of(), fixedIds(d.pages().get(0)));
        // La primera página interior es la 1 de 2, aunque sea la segunda del documento; el anexo
        // reinicia el contador, así que es la 1 de 1 y no cuenta para el total del cuerpo.
        assertEquals("Página 1 de 2", text(d.pages().get(1), "num"));
        assertEquals("Página 2 de 2", text(d.pages().get(2), "num"));
        assertEquals("Página 1 de 1", text(d.pages().get(3), "num"));
    }

    /**
     * Un prefacio en romanos y un cuerpo que vuelve a empezar en 5: {@code {page}}, {@code {pages}} y {@code {page of:
     * id}} se escriben con el {@code Page style} de la página donde salen o a la que se refieren
     * (referencia/componentes/document.md).
     */
    @Test
    void romanPagesAndARestartThatStartsAtFive() {
        LaidOutDocument d = lay(
                "\tFixed:", "\t\tId: num", "\t\tOffset: auto 0 0 0", "\t\tText: Página {page} de {pages}",
                join(new String[] {"\tSection:", "\t\tPage style: lower-roman", "\t\tText: El cuerpo empieza en la "
                        + "{page of: b1} y el prefacio en la {page of: p1}.", "\t\t\tId: ref"}, fullPage("p1"),
                        fullPage("p2")),
                join(new String[] {"\tSection:", "\t\tPage numbering: restart", "\t\tStart: 5"}, fullPage("b1"),
                        fullPage("b2")));
        assertEquals(List.of(1, 2, 3, 5, 6), d.pages().stream().map(Page::printed).toList());
        assertEquals(List.of(NumberStyle.LOWER_ROMAN, NumberStyle.LOWER_ROMAN, NumberStyle.LOWER_ROMAN,
                NumberStyle.DECIMAL, NumberStyle.DECIMAL), d.pages().stream().map(Page::style).toList());
        assertEquals("Página i de iii", text(d.pages().get(0), "num"));
        assertEquals("Página iii de iii", text(d.pages().get(2), "num"));
        assertEquals("Página 5 de 6", text(d.pages().get(3), "num"));
        assertEquals("El cuerpo empieza en la 5 y el prefacio en la i.", text(d.pages().get(0), "ref"));
    }

    @Test
    void aSectionThatContinuesFollowsTheCounterOfTheOnesBefore() {
        LaidOutDocument d = lay(
                "\tFixed:", "\t\tId: num", "\t\tText: {page}/{pages}",
                join(new String[] {"\tSection:"}, fullPage("a")),
                join(new String[] {"\tSection:", "\t\tPage numbering: none", "\t\tInherit fixed: false"}, fullPage("hole")),
                join(new String[] {"\tSection:", "\t\tInherit fixed: false"}, fullPage("b")));
        assertEquals(List.of(1, 0, 2), d.pages().stream().map(Page::printed).toList());
    }

    @Test
    void thePageOfABoxIsItsPrintedNumberAndASectionCanBeReferenced() {
        LaidOutDocument d = lay(
                join(new String[] {"\tSection:", "\t\tPage numbering: none"}, fullPage("cover")),
                join(new String[] {"\tSection:", "\t\tText: Mira la página {page of: target} y la {page of: second}.",
                        "\t\t\tId: ref"}, fullPage("b1")),
                join(new String[] {"\tSection:", "\t\tId: second"}, new String[] {"\t\tBlock:", "\t\t\tId: target",
                        "\t\t\tBox:", "\t\t\t\tHeight: 10"}));
        // cover = física 1 (sin número), cuerpo = física 2 (la 1) y 3 (la 2), segunda sección = física 4 (la 3).
        assertEquals(4, d.pages().size());
        assertEquals("Mira la página 3 y la 3.", text(d.pages().get(1), "ref"));
    }

    @Test
    void pagesCountsOnlyTheNumberedPagesOfTheRun() {
        // {pages} cuenta las páginas numeradas de la tanda, no las del documento.
        LaidOutDocument d = lay(
                "\tFixed:", "\t\tId: n", "\t\tPages: last", "\t\tText: {pages}",
                join(new String[] {"\tSection:", "\t\tInherit fixed: false", "\t\tPage numbering: none"}, fullPage("c1"), fullPage("c2")),
                join(new String[] {"\tSection:"}, fullPage("b")));
        assertEquals(3, d.pages().size());
        assertEquals("1", text(d.pages().get(2), "n"));
    }

    /**
     * {@code Begin on: odd}: si la sección anterior acaba en impar, acaba con una página en blanco de ella, sin
     * contenido ni {@code Fixed}, que cuenta para el número y para {@code {pages}} pero no para el {@code Pages} de los
     * {@code Fixed} de su sección (referencia/componentes/document.md).
     */
    @Test
    void aSectionThatBeginsOnAnOddPageLeavesABlankPageBefore() {
        LaidOutDocument d = lay(
                "\tFixed:", "\t\tId: num", "\t\tOffset: auto 0 0 0", "\t\tText: Página {page} de {pages}",
                join(new String[] {"\tSection:", "\t\tFixed:", "\t\t\tId: last-a", "\t\t\tPages: last",
                        "\t\t\tBox:", "\t\t\t\tHeight: 10"}, fullPage("a")),
                new String[] {"\tSection:", "\t\tBegin on: odd", "\t\tText: Empieza en la {page of: b}.",
                        "\t\t\tId: ref", "\t\tBlock:", "\t\t\tId: b"});
        assertEquals(3, d.pages().size());
        Page blank = d.pages().get(1);
        assertTrue(blank.blank());
        assertEquals(0, blank.section());
        assertEquals(List.of(), blank.content().children());
        assertEquals(List.of(), fixedIds(blank));
        // Pages: last de la sección es su última página con contenido, no la página en blanco.
        assertEquals(List.of("num", "last-a"), fixedIds(d.pages().get(0)));
        assertEquals(List.of(1, 2, 3), d.pages().stream().map(Page::printed).toList());
        assertEquals("Página 1 de 3", text(d.pages().get(0), "num"));
        assertEquals("Página 3 de 3", text(d.pages().get(2), "num"));
        assertEquals("Empieza en la 3.", text(d.pages().get(2), "ref"));
        assertFalse(d.pages().get(2).blank());
    }

    @Test
    void aSectionThatAlreadyBeginsOnAnOddPageNeedsNoBlankPage() {
        LaidOutDocument d = lay(
                join(new String[] {"\tSection:"}, fullPage("a1"), fullPage("a2")),
                join(new String[] {"\tSection:", "\t\tBegin on: odd"}, fullPage("b")),
                join(new String[] {"\tSection:", "\t\tBegin on: odd"}, fullPage("c")));
        // a: 1 y 2; b: 3; la página en blanco, 4; c: 5.
        assertEquals(5, d.pages().size());
        assertEquals(List.of(false, false, false, true, false), d.pages().stream().map(Page::blank).toList());
        assertEquals(List.of(0, 0, 1, 1, 2), d.pages().stream().map(Page::section).toList());
        assertNotNull(on(d.pages().get(4), "c"));
    }

    /**
     * La página en blanco es de la sección anterior: tras una portada sin números no tiene número, y la paridad es la
     * de la página física, no la del número impreso.
     */
    @Test
    void theBlankPageFollowsTheNumberingOfItsSection() {
        LaidOutDocument d = lay(
                join(new String[] {"\tSection:", "\t\tPage numbering: none", "\t\tMedia:", "\t\t\tMargin: 0"},
                        new String[] {"\t\tBlock:", "\t\t\tId: cover"}),
                join(new String[] {"\tSection:", "\t\tBegin on: odd", "\t\tPage style: lower-roman"},
                        fullPage("p")),
                join(new String[] {"\tSection:", "\t\tBegin on: odd", "\t\tPage numbering: restart"},
                        fullPage("b")));
        assertEquals(List.of(0, 0, 1, 2, 1), d.pages().stream().map(Page::printed).toList());
        assertEquals(List.of(false, true, false, true, false), d.pages().stream().map(Page::blank).toList());
        // La página en blanco tiene el medio de su sección: la portada, sin márgenes.
        assertEquals(0, d.pages().get(1).media().margin().top(), 1e-9);
        assertEquals(NumberStyle.LOWER_ROMAN, d.pages().get(3).style());
    }

    @Test
    void beginOnOddInTheFirstSectionHasNoEffect() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> lay("\tSection:", "\t\tBegin on: odd", "\t\tBlock:"));
        assertEquals(ErrorCode.BEGIN_ON_FIRST_SECTION, e.errorCode());
        assertEquals(14, e.line());
        // any, el valor por defecto, se puede escribir.
        assertEquals(1, lay("\tSection:", "\t\tBegin on: any", "\t\tBlock:").pages().size());
    }

    @Test
    void theClassOfASectionGivesItsContentATypography() {
        LaidOutDocument d = layWith(HEADER + "\t\tFor class: annex\n\t\t\tTypography:\n\t\t\t\tSize: 20\n",
                "\tSection:", "\t\tText: ab", "\t\t\tId: normal",
                "\tSection:", "\t\tClass: annex", "\t\tText: ab", "\t\t\tId: big");
        // Cada carácter mide la mitad del tamaño: 5 pt con Size 10 y 10 pt con Size 20.
        assertEquals(5, on(d.pages().get(0), "normal").lines().get(0).width() / 2, 1e-9);
        assertEquals(10, on(d.pages().get(1), "big").lines().get(0).width() / 2, 1e-9);
    }

    @Test
    void theIdOfASectionIsAUniqueId() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> lay("\tSection:", "\t\tId: a", "\t\tBlock:", "\t\t\tId: b", "\tSection:", "\t\tId: b", "\t\tBlock:"));
        assertEquals(ErrorCode.DUPLICATE_ID, e.errorCode());
    }

    @Test
    void headingsAndContentsGoAcrossSections() {
        LaidOutDocument d = lay(
                "\tSection:", "\t\tHeading 1:", "\t\t\tId: h1", "\t\t\tText: Uno",
                join(new String[] {"\tSection:", "\t\tHeading 1:", "\t\t\tId: h2", "\t\t\tText: Dos", "\t\tContents:", "\t\t\tId: toc"}));
        assertEquals(2, d.pages().size());
        assertTrue(on(d.pages().get(1), "toc").children().size() >= 2);
    }

    @Test
    void elementAndIdRulesReachASectionToo() {
        LaidOutDocument d = layWith(HEADER + "\t\tElement: Section\n\t\t\tTypography:\n\t\t\t\tSize: 20\n"
                        + "\t\tFor id: small\n\t\t\tTypography:\n\t\t\t\tSize: 10\n",
                "\tSection:", "\t\tText: ab", "\t\t\tId: t1",
                "\tSection:", "\t\tId: small", "\t\tText: ab", "\t\t\tId: t2");
        // Element: Section da 10 pt por carácter; la regla del Id de la segunda, que gana, vuelve a 5.
        assertEquals(10, on(d.pages().get(0), "t1").lines().get(0).width() / 2, 1e-9);
        assertEquals(5, on(d.pages().get(1), "t2").lines().get(0).width() / 2, 1e-9);
    }

    @Test
    void aHeadingOfAnUnnumberedSectionCannotBeListedInAContents() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tHeading 1:", "\t\t\tText: Portada",
                "\tSection:", "\t\tContents:"));
        assertEquals(ErrorCode.REFERENCE_NOT_PLACEABLE, e.errorCode());
        // Con In contents: false, el encabezado sigue siéndolo pero no se lista; el índice necesita otro.
        LaidOutDocument d = lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tHeading 1:", "\t\t\tIn contents: false", "\t\t\tText: Portada",
                "\tSection:", "\t\tHeading 1:", "\t\t\tText: Uno", "\t\tContents:", "\t\t\tId: toc");
        assertEquals(2, d.pages().size());
    }

    // Casos que faltaban según las pruebas de mutación.

    /** Una portada sin números no es un error si su contenido no escribe {page}: solo lo es lo que lo escribe. */
    @Test
    void anUnnumberedPageWithoutPageNumbersIsFine() {
        LaidOutDocument d = lay("\tSection:", "\t\tPage numbering: none", "\t\tText: Portada",
                "\tSection:", "\t\tText: Página {page}");
        assertEquals(2, d.pages().size());
    }

    @Test
    void theMessageNamesTheTextWithPageNumbers() {
        InvalidModelException named = assertThrows(InvalidModelException.class, () -> lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tText: Hay {pages}", "\t\t\tId: t"));
        assertEquals(ErrorCode.PAGE_NUMBER_UNAVAILABLE, named.errorCode());
        assertTrue(named.getMessage().contains("El texto de la caja t tiene {page}"), named.getMessage());
        InvalidModelException anonymous = assertThrows(InvalidModelException.class, () -> lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tText: Hay {pages}"));
        assertTrue(anonymous.getMessage().contains("El texto de la caja (sin Id) tiene"), anonymous.getMessage());
    }

    /** {@code {number of}} no necesita página: puede nombrar una caja de una sección sin números. */
    @Test
    void numberReferencesDoNotNeedAPage() {
        LaidOutDocument d = lay("\tSection:", "\t\tPage numbering: none", "\t\tList:", "\t\t\tMarker: decimal",
                "\t\t\tBox:", "\t\t\t\tPadding: 0 0 0 20", "\t\t\tList item:", "\t\t\t\tId: n", "\t\t\t\tText: uno",
                "\tSection:", "\t\tText: Ver el punto {number of: n}");
        assertEquals(2, d.pages().size());
    }

    /** El error señala la referencia a la caja sin página, no la primera del documento. */
    @Test
    void theErrorPointsToTheReferenceOfTheUnplaceableBox() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tText: Portada", "\t\t\tId: portada",
                "\tSection:", "\t\tText: a", "\t\t\tId: other", "\t\tText: Ver {page of: other}",
                "\t\tText: Ver {page of: portada}"));
        assertEquals(ErrorCode.REFERENCE_NOT_PLACEABLE, e.errorCode());
        // Cabecera de 12 líneas: la referencia a la portada es la 9.ª del cuerpo.
        assertEquals(21, e.line());
    }
}
