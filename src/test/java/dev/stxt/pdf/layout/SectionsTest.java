package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.NumberFormat;
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
 * Sections (reference/components/document.md): pages of 300 × 200 with margins of 20 (page area of
 * 260 × 160). With {@code FixedMetrics} and {@code Size: 10}, each character measures 5 pt.
 */
class SectionsTest {

    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n"
            + "\tPage:\n\t\tWidth: 300\n\t\tHeight: 200\n\t\tMargin: 20\n"
            + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

    private static LaidOutDocument lay(Object... lines) {
        return layWith(HEADER, lines);
    }

    /** The lines of the document: each part is one line ({@code String}) or several ({@code String[]}). */
    private static LaidOutDocument layWith(String header, Object... parts) {
        String body = java.util.Arrays.stream(parts)
                .flatMap(p -> p instanceof String[] many ? java.util.Arrays.stream(many) : java.util.stream.Stream.of((String) p))
                .collect(Collectors.joining("\n"));
        return new Engine().layOutDocument(new DocumentReader().read(header + body));
    }

    /** A whole page: a box of 160 pt, the height of the page area. */
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

    /**
     * The fragment with that {@code Id} in the flow or in the {@code Page overlay} of the page; {@code null} if absent.
     */
    private static Fragment on(Page page, String id) {
        Fragment found = find(page.content(), id);
        for (Fragment overlay : page.pageOverlays()) {
            found = found != null ? found : find(overlay, id);
        }
        return found;
    }

    private static List<String> pageOverlayIds(Page page) {
        return page.pageOverlays().stream().map(f -> f.box().id()).collect(Collectors.toList());
    }

    /** The text of the box with that {@code Id}, or that of its descendants if it has no lines. */
    private static String text(Page page, String id) {
        return text(on(page, id));
    }

    private static String text(Fragment f) {
        String own = f.lines().stream().map(l -> l.text()).collect(Collectors.joining("|"));
        return own + f.children().stream().map(SectionsTest::text).collect(Collectors.joining("|"));
    }

    @Test
    void eachSectionStartsANewPageWithItsOwnPageSetup() {
        LaidOutDocument d = layWith("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tPaper: A4\n"
                + "\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n",
                "\tSection:", "\t\tBlock:", "\t\t\tId: a",
                "\tSection:", "\t\tPage:", "\t\t\tOrientation: landscape", "\t\tBlock:", "\t\t\tId: b",
                "\tSection:", "\t\tPage:", "\t\t\tMargin: 10", "\t\tBlock:", "\t\t\tId: c",
                "\tSection:", "\t\tPage:", "\t\t\tPaper: Letter", "\t\t\tMargin: 0", "\t\tBlock:", "\t\t\tId: d");
        assertEquals(4, d.pages().size());
        // Without its own Page, the one of the document.
        assertEquals(210 * 72 / 25.4, d.pages().get(0).pageSetup().width(), 1e-9);
        assertEquals(297 * 72 / 25.4, d.pages().get(0).pageSetup().height(), 1e-9);
        // Only Orientation: the paper of the document rotated, with its margins.
        assertEquals(297 * 72 / 25.4, d.pages().get(1).pageSetup().width(), 1e-9);
        assertEquals(210 * 72 / 25.4, d.pages().get(1).pageSetup().height(), 1e-9);
        assertEquals(40, d.pages().get(1).pageSetup().margin().left(), 1e-9);
        // Only Margin: the size of the document.
        assertEquals(210 * 72 / 25.4, d.pages().get(2).pageSetup().width(), 1e-9);
        assertEquals(10, d.pages().get(2).pageSetup().margin().top(), 1e-9);
        // Its own size and its own margin.
        assertEquals(612, d.pages().get(3).pageSetup().width(), 1e-9);
        assertEquals(0, d.pages().get(3).pageSetup().margin().left(), 1e-9);
        // The page area of each page is that of its medium: the width of the box of the section.
        assertEquals(210 * 72 / 25.4 - 80, d.pages().get(0).content().width(), 1e-9);
        assertEquals(297 * 72 / 25.4 - 80, d.pages().get(1).content().width(), 1e-9);
        assertEquals(210 * 72 / 25.4 - 20, d.pages().get(2).content().width(), 1e-9);
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
    void pageOverlaysOfTheDocumentGoEverywhereAndThoseOfASectionOnlyInIt() {
        LaidOutDocument d = lay(
                "\tPage overlay:", "\t\tId: foot", "\t\tOffset: auto 0 0 0", "\t\tBox:", "\t\t\tHeight: 10",
                join(new String[] {"\tSection:", "\t\tPage overlay:", "\t\t\tId: head-a", "\t\t\tPages: first",
                        "\t\t\tBox:", "\t\t\t\tHeight: 10"}, fullPage("a1"), fullPage("a2")),
                new String[] {"\tSection:", "\t\tInherit page overlays: false", "\t\tPage overlay:", "\t\t\tId: head-b", "\t\t\tBox:",
                        "\t\t\t\tHeight: 10", "\t\tBlock:", "\t\t\tId: b"},
                new String[] {"\tSection:", "\t\tBlock:", "\t\t\tId: c"});
        assertEquals(4, d.pages().size());
        // Pages: first counts within the section: only its first page.
        assertEquals(List.of("foot", "head-a"), pageOverlayIds(d.pages().get(0)));
        assertEquals(List.of("foot"), pageOverlayIds(d.pages().get(1)));
        // Inherit page overlays: false leaves the section without those of the document.
        assertEquals(List.of("head-b"), pageOverlayIds(d.pages().get(2)));
        assertEquals(List.of("foot"), pageOverlayIds(d.pages().get(3)));
    }

    @Test
    void pagesOfAPageOverlayOfTheDocumentCountTheWholeDocument() {
        LaidOutDocument d = lay(
                "\tPage overlay:", "\t\tId: not-first", "\t\tPages: not-first", "\t\tBox:", "\t\t\tHeight: 10",
                "\tPage overlay:", "\t\tId: last", "\t\tPages: last", "\t\tBox:", "\t\t\tHeight: 10",
                join(new String[] {"\tSection:"}, fullPage("a")),
                join(new String[] {"\tSection:"}, fullPage("b1"), fullPage("b2")));
        assertEquals(3, d.pages().size());
        assertEquals(List.of(), pageOverlayIds(d.pages().get(0)));
        assertEquals(List.of("not-first"), pageOverlayIds(d.pages().get(1)));
        assertEquals(List.of("not-first", "last"), pageOverlayIds(d.pages().get(2)));
    }

    @Test
    void anUnnumberedCoverDoesNotCountAndTheNumberingCanRestart() {
        LaidOutDocument d = lay(
                "\tPage overlay:", "\t\tId: num", "\t\tOffset: auto 0 0 0", "\t\tPages: not-first", "\t\tText: Página {page} de {pages}",
                join(new String[] {"\tSection:", "\t\tInherit page overlays: false", "\t\tPage numbering: none"}, fullPage("cover")),
                join(new String[] {"\tSection:", "\t\tInherit page overlays: true"}, fullPage("b1"), fullPage("b2")),
                join(new String[] {"\tSection:", "\t\tPage numbering: restart"}, fullPage("annex")));
        assertEquals(4, d.pages().size());
        assertEquals(List.of(0, 1, 2, 1), d.pages().stream().map(Page::printed).toList());
        assertFalse(d.pages().get(0).numbered());
        assertEquals(List.of(), pageOverlayIds(d.pages().get(0)));
        // The first inner page is 1 of 2, even though it is the second of the document; the appendix
        // restarts the counter, so it is 1 of 1 and does not count toward the total of the body.
        assertEquals("Página 1 de 2", text(d.pages().get(1), "num"));
        assertEquals("Página 2 de 2", text(d.pages().get(2), "num"));
        assertEquals("Página 1 de 1", text(d.pages().get(3), "num"));
    }

    /**
     * A preface in Roman numerals and a body that starts again at 5: {@code {page}}, {@code {pages}} and
     * {@code {page of: id}} are written with the {@code Page number format} of the page where they appear or to which
     * they refer (reference/components/document.md).
     */
    @Test
    void romanPagesAndARestartThatStartsAtFive() {
        LaidOutDocument d = lay(
                "\tPage overlay:", "\t\tId: num", "\t\tOffset: auto 0 0 0", "\t\tText: Página {page} de {pages}",
                join(new String[] {"\tSection:", "\t\tPage number format: lower-roman", "\t\tText: El cuerpo empieza en la "
                        + "{page of: b1} y el prefacio en la {page of: p1}.", "\t\t\tId: ref"}, fullPage("p1"),
                        fullPage("p2")),
                join(new String[] {"\tSection:", "\t\tPage numbering: restart", "\t\tStart: 5"}, fullPage("b1"),
                        fullPage("b2")));
        assertEquals(List.of(1, 2, 3, 5, 6), d.pages().stream().map(Page::printed).toList());
        assertEquals(List.of(NumberFormat.LOWER_ROMAN, NumberFormat.LOWER_ROMAN, NumberFormat.LOWER_ROMAN,
                NumberFormat.DECIMAL, NumberFormat.DECIMAL), d.pages().stream().map(Page::style).toList());
        assertEquals("Página i de iii", text(d.pages().get(0), "num"));
        assertEquals("Página iii de iii", text(d.pages().get(2), "num"));
        assertEquals("Página 5 de 6", text(d.pages().get(3), "num"));
        assertEquals("El cuerpo empieza en la 5 y el prefacio en la i.", text(d.pages().get(0), "ref"));
    }

    @Test
    void aSectionThatContinuesFollowsTheCounterOfTheOnesBefore() {
        LaidOutDocument d = lay(
                "\tPage overlay:", "\t\tId: num", "\t\tText: {page}/{pages}",
                join(new String[] {"\tSection:"}, fullPage("a")),
                join(new String[] {"\tSection:", "\t\tPage numbering: none", "\t\tInherit page overlays: false"}, fullPage("hole")),
                join(new String[] {"\tSection:", "\t\tInherit page overlays: false"}, fullPage("b")));
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
        // cover = physical 1 (no number), body = physical 2 (number 1) and 3 (number 2), second section = physical 4
        // (number 3).
        assertEquals(4, d.pages().size());
        assertEquals("Mira la página 3 y la 3.", text(d.pages().get(1), "ref"));
    }

    @Test
    void pagesCountsOnlyTheNumberedPagesOfTheRun() {
        // {pages} counts the numbered pages of the page sequence, not those of the document.
        LaidOutDocument d = lay(
                "\tPage overlay:", "\t\tId: n", "\t\tPages: last", "\t\tText: {pages}",
                join(new String[] {"\tSection:", "\t\tInherit page overlays: false", "\t\tPage numbering: none"}, fullPage("c1"), fullPage("c2")),
                join(new String[] {"\tSection:"}, fullPage("b")));
        assertEquals(3, d.pages().size());
        assertEquals("1", text(d.pages().get(2), "n"));
    }

    /**
     * {@code Begin on: odd}: if the previous section ends on an odd page, it ends with a blank page of its own, without
     * content or {@code Page overlay}, which counts for the number and for {@code {pages}} but not for the
     * {@code Pages} of the {@code Page overlay} of its section (reference/components/document.md).
     */
    @Test
    void aSectionThatBeginsOnAnOddPageLeavesABlankPageBefore() {
        LaidOutDocument d = lay(
                "\tPage overlay:", "\t\tId: num", "\t\tOffset: auto 0 0 0", "\t\tText: Página {page} de {pages}",
                join(new String[] {"\tSection:", "\t\tPage overlay:", "\t\t\tId: last-a", "\t\t\tPages: last",
                        "\t\t\tBox:", "\t\t\t\tHeight: 10"}, fullPage("a")),
                new String[] {"\tSection:", "\t\tBegin on: odd", "\t\tText: Empieza en la {page of: b}.",
                        "\t\t\tId: ref", "\t\tBlock:", "\t\t\tId: b"});
        assertEquals(3, d.pages().size());
        Page blank = d.pages().get(1);
        assertTrue(blank.blank());
        assertEquals(0, blank.section());
        assertEquals(List.of(), blank.content().children());
        assertEquals(List.of(), pageOverlayIds(blank));
        // Pages: last of the section is its last page with content, not the blank page.
        assertEquals(List.of("num", "last-a"), pageOverlayIds(d.pages().get(0)));
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
        // a: 1 and 2; b: 3; the blank page, 4; c: 5.
        assertEquals(5, d.pages().size());
        assertEquals(List.of(false, false, false, true, false), d.pages().stream().map(Page::blank).toList());
        assertEquals(List.of(0, 0, 1, 1, 2), d.pages().stream().map(Page::section).toList());
        assertNotNull(on(d.pages().get(4), "c"));
    }

    /**
     * The blank page belongs to the previous section: after a cover without numbers it has no number, and the parity is
     * that of the physical page, not that of the printed number.
     */
    @Test
    void theBlankPageFollowsTheNumberingOfItsSection() {
        LaidOutDocument d = lay(
                join(new String[] {"\tSection:", "\t\tPage numbering: none", "\t\tPage:", "\t\t\tMargin: 0"},
                        new String[] {"\t\tBlock:", "\t\t\tId: cover"}),
                join(new String[] {"\tSection:", "\t\tBegin on: odd", "\t\tPage number format: lower-roman"},
                        fullPage("p")),
                join(new String[] {"\tSection:", "\t\tBegin on: odd", "\t\tPage numbering: restart"},
                        fullPage("b")));
        assertEquals(List.of(0, 0, 1, 2, 1), d.pages().stream().map(Page::printed).toList());
        assertEquals(List.of(false, true, false, true, false), d.pages().stream().map(Page::blank).toList());
        // The blank page has the medium of its section: the cover, without margins.
        assertEquals(0, d.pages().get(1).pageSetup().margin().top(), 1e-9);
        assertEquals(NumberFormat.LOWER_ROMAN, d.pages().get(3).style());
    }

    @Test
    void beginOnOddInTheFirstSectionHasNoEffect() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> lay("\tSection:", "\t\tBegin on: odd", "\t\tBlock:"));
        assertEquals(ErrorCode.BEGIN_ON_FIRST_SECTION, e.errorCode());
        assertEquals(14, e.line());
        // any, the default value, can be written.
        assertEquals(1, lay("\tSection:", "\t\tBegin on: any", "\t\tBlock:").pages().size());
    }

    @Test
    void theClassOfASectionGivesItsContentATypography() {
        LaidOutDocument d = layWith(HEADER + "\t\tFor style: annex\n\t\t\tTypography:\n\t\t\t\tSize: 20\n",
                "\tSection:", "\t\tText: ab", "\t\t\tId: normal",
                "\tSection:", "\t\tStyle: annex", "\t\tText: ab", "\t\t\tId: big");
        // Each character measures half the size: 5 pt with Size 10 and 10 pt with Size 20.
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
                join(new String[] {"\tSection:", "\t\tHeading 1:", "\t\t\tId: h2", "\t\t\tText: Dos", "\t\tTable of contents:", "\t\t\tId: toc"}));
        assertEquals(2, d.pages().size());
        assertTrue(on(d.pages().get(1), "toc").children().size() >= 2);
    }

    @Test
    void elementAndIdRulesReachASectionToo() {
        LaidOutDocument d = layWith(HEADER + "\t\tFor element: Section\n\t\t\tTypography:\n\t\t\t\tSize: 20\n"
                        + "\t\tFor id: small\n\t\t\tTypography:\n\t\t\t\tSize: 10\n",
                "\tSection:", "\t\tText: ab", "\t\t\tId: t1",
                "\tSection:", "\t\tId: small", "\t\tText: ab", "\t\t\tId: t2");
        // For element: Section gives 10 pt per character; the rule of the Id of the second one, which wins, goes back
        // to 5.
        assertEquals(10, on(d.pages().get(0), "t1").lines().get(0).width() / 2, 1e-9);
        assertEquals(5, on(d.pages().get(1), "t2").lines().get(0).width() / 2, 1e-9);
    }

    @Test
    void aHeadingOfAnUnnumberedSectionCannotBeListedInAContents() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tHeading 1:", "\t\t\tText: Portada",
                "\tSection:", "\t\tTable of contents:"));
        assertEquals(ErrorCode.REFERENCE_NOT_PLACEABLE, e.errorCode());
        // With In table of contents: false, the heading is still one but it is not listed; the table of contents needs
        // another.
        LaidOutDocument d = lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tHeading 1:", "\t\t\tIn table of contents: false", "\t\t\tText: Portada",
                "\tSection:", "\t\tHeading 1:", "\t\t\tText: Uno", "\t\tTable of contents:", "\t\t\tId: toc");
        assertEquals(2, d.pages().size());
    }

    // Cases that were missing according to mutation testing.

    /** A cover without numbers is not an error if its content does not write {page}: only what writes it is. */
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
        assertTrue(named.getMessage().contains("The text of box t has {page}"), named.getMessage());
        InvalidModelException anonymous = assertThrows(InvalidModelException.class, () -> lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tText: Hay {pages}"));
        assertTrue(anonymous.getMessage().contains("The text of box (without Id) has"), anonymous.getMessage());
    }

    /** {@code {number of}} does not need a page: it can name a box of a section without numbers. */
    @Test
    void numberReferencesDoNotNeedAPage() {
        LaidOutDocument d = lay("\tSection:", "\t\tPage numbering: none", "\t\tList:", "\t\t\tMarker: decimal",
                "\t\t\tBox:", "\t\t\t\tPadding: 0 0 0 20", "\t\t\tList item:", "\t\t\t\tId: n", "\t\t\t\tText: uno",
                "\tSection:", "\t\tText: Ver el punto {number of: n}");
        assertEquals(2, d.pages().size());
    }

    /** The error points to the reference to the box without a page, not the first one of the document. */
    @Test
    void theErrorPointsToTheReferenceOfTheUnplaceableBox() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> lay(
                "\tSection:", "\t\tPage numbering: none", "\t\tText: Portada", "\t\t\tId: portada",
                "\tSection:", "\t\tText: a", "\t\t\tId: other", "\t\tText: Ver {page of: other}",
                "\t\tText: Ver {page of: portada}"));
        assertEquals(ErrorCode.REFERENCE_NOT_PLACEABLE, e.errorCode());
        // Header of 12 lines: the reference to the cover is the 9th of the body.
        assertEquals(21, e.line());
    }
}
