package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Reference;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * References and page numbers written as text (reference/common/references.md): inside another pair of braces in any
 * text, {@code {{page}}}, and with an escaped brace in Markdown. The laid out lines have the text as it is read, and
 * measure what it measures (each character, 5 pt with the fixed metrics).
 */
class LiteralReferencesTest {

    private static String text(LayoutFixture l, String id) {
        return text(l, 1, id);
    }

    private static String text(LayoutFixture l, int page, String id) {
        return l.fragment(page, id).lines().stream().map(TextLine::text).reduce((a, b) -> a + "|" + b).orElse("");
    }

    @Test
    void aPageNumberInsideAnotherPairOfBracesIsText() {
        LayoutFixture l = layOut("\tText: Pág. {page} de {pages}, no {{page}} de {{pages}}", "\t\tId: T");
        assertEquals("Pág. 1 de 1, no {page} de {pages}", text(l, "T"));
        // It measures what is read: 33 characters.
        assertEquals(33 * 5, l.fragment("T").lines().get(0).width(), 1e-9);
        TextLine.Run run = l.fragment("T").lines().get(0).runs().get(0);
        assertEquals("Pág. 1 de 1, no {page} de {pages}", run.text());
    }

    @Test
    void aReferenceInsideAnotherPairOfBracesIsTextAndPointsToNothing() {
        // None of them is checked: there is no box with those Id, and a running head only goes in a Page overlay.
        LayoutFixture l = layOut("\tText: {{page of: nada}}, {{number of: nada}} y {{heading 2}}", "\t\tId: T");
        assertEquals("{page of: nada}, {number of: nada} y {heading 2}", text(l, "T"));
    }

    @Test
    void onlyOnePairOfBracesIsRemovedAndOtherBracesAreLeft() {
        LayoutFixture l = layOut("\tText: {{{page}}} {{nombre}} {page}} {{page}", "\t\tId: T");
        assertEquals("{{page}} {{nombre}} 1} {1", text(l, "T"));
    }

    @Test
    void itIsTheSameInPreformattedTextAndInACell() {
        LayoutFixture l = layOut(
                "\tText:", "\t\tId: P", "\t\tTypography:", "\t\t\tPreformatted: true",
                "\t\tContent >>", "\t\t\tuno {{page}}", "\t\t\t  dos {page}",
                "\tTable:", "\t\tColumn sizes: *", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: {{pages}} y {pages}",
                "\t\t\t\t\tId: C");
        assertEquals("uno {page}|  dos 1", text(l, "P"));
        assertEquals("{pages} y 1", text(l, "C"));
    }

    @Test
    void inMarkdownAnEscapedBraceIsTextToo() {
        LayoutFixture l = layOut(
                "\tText: \\{page\\} y \\{page}, {{page}} y `{{page}}`, pero {page} y **{page}**", "\t\tId: T",
                "\t\tTypography:", "\t\t\tMarkup: markdown", "\t\t\tCode font: Mono");
        assertEquals("{page} y {page}, {page} y {page}, pero 1 y 1", text(l, "T"));
        // In inline code the backslash is text, and so is the brace after it: the page number is replaced.
        LayoutFixture code = layOut("\tText: `\\{page}`", "\t\tId: T", "\t\tTypography:", "\t\t\tMarkup: markdown",
                "\t\t\tCode font: Mono");
        assertEquals("\\1", text(code, "T"));
        // One way or the other, not both: each escaped brace is a brace.
        LayoutFixture both = layOut("\tText: \\{\\{page\\}\\}", "\t\tId: T", "\t\tTypography:",
                "\t\t\tMarkup: markdown");
        assertEquals("{{page}}", text(both, "T"));
    }

    @Test
    void inAMarkdownElementToo() {
        LayoutFixture l = layOut(
                "\tMarkdown:", "\t\tId: M", "\t\tTypography:", "\t\t\tCode font: Mono", "\t\tContent >>",
                "\t\t\t# Uso de \\{page\\} {#uso}",
                "\t\t\t",
                "\t\t\tSe escribe {{page of: uso}} y da {page of: uso}; \\{number of: x} no es nada.",
                "\t\t\t",
                "\t\t\t```",
                "\t\t\t{{page}} da {page}",
                "\t\t\t```");
        java.util.List<String> texts = new java.util.ArrayList<>();
        collect(l.fragment("M"), texts);
        assertEquals(java.util.List.of("Uso de {page}", "Se escribe {page of: uso} y da 1; {number of: x} no es nada.",
                "{page} da 1"), texts);
    }

    private static void collect(dev.stxt.pdf.fragment.Fragment f, java.util.List<String> texts) {
        f.lines().forEach(line -> texts.add(line.text()));
        f.children().forEach(child -> collect(child, texts));
    }

    @Test
    void theTextOfAHeadingIsCopiedWithItsLiteralBraces() {
        // The table of contents and the running head copy the text of the heading: its {page} is still text there,
        // on a page whose number is 2.
        LayoutFixture l = layOutPaged(200,
                "\tPage overlay:", "\t\tOffset: 0 0 auto 0", "\t\tText: {heading 1} · {page}", "\t\t\tId: R",
                "\tTable of contents:", "\t\tId: I", "\t\tBreak:", "\t\t\tAfter: page",
                "\tHeading 1:", "\t\tText: Uso de {{page}}", "\t\t\tId: H");
        assertEquals(2, l.pages());
        assertEquals("Uso de {page}", text(l, 2, "H"));
        assertEquals("Uso de {page} · 2", text(l, 2, "R"));
        java.util.List<String> entry = new java.util.ArrayList<>();
        collect(l.fragment(1, "I"), entry);
        assertEquals(java.util.List.of("Uso de {page}", "2"), entry);
    }

    @Test
    void anEscapedTextIsShownAsItIs() {
        String data = "{page} de {pages}, {{page}}, {page of: a} y {heading 1: first}";
        LayoutFixture l = layOut("\tText: " + Reference.escape(data), "\t\tId: T");
        assertEquals(data, text(l, "T"));
    }
}
