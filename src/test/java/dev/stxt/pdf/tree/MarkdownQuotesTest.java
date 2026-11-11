package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;

/**
 * The block quotes ({@code > ...}) and the horizontal rules ({@code ---}) of a {@code Markdown} in the box tree
 * (reference/components/markdown.md): the generated elements {@code Block quote} and {@code Divider}, the line the
 * divider has by default and the rules that give them their look.
 */
class MarkdownQuotesTest {

    private static final String HEADER = Docs.header(595, 842) + Docs.TYPOGRAPHY;

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(Docs.read(HEADER, lines)).sections().get(0).box();
    }

    /** The children of a {@code Markdown} with this content, after these lines of the document. */
    private static List<Box> markdown(List<String> before, String... content) {
        List<String> lines = new java.util.ArrayList<>(before);
        lines.addAll(List.of("\tMarkdown:", "\t\tBullet markers: bullet", "\t\tTable column sizes: auto", "\t\tContent >>"));
        for (String c : content) {
            lines.add(c.isEmpty() ? "" : "\t\t\t" + c);
        }
        Box root = build(lines.toArray(String[]::new));
        return root.children().get(root.children().size() - 2).children();
    }

    private static List<Box> markdown(String... content) {
        return markdown(List.of(), content);
    }

    private static InvalidModelException error(List<String> before, String... content) {
        return assertThrows(InvalidModelException.class, () -> markdown(before, content));
    }

    private static Sides<Value> sides(double top, double right, double bottom, double left) {
        return new Sides<>(Value.pt(top), Value.pt(right), Value.pt(bottom), Value.pt(left));
    }

    @Test
    void aQuoteIsABlockQuoteWithItsContent() {
        List<Box> md = markdown("Antes.", "", "> Una cita", "> en dos líneas.", ">", "> Otro párrafo.", "", "Después.");
        assertEquals(List.of(Element.PARAGRAPH, Element.BLOCK_QUOTE, Element.PARAGRAPH),
                md.stream().map(Box::element).toList());
        Box quote = md.get(1);
        assertEquals(Element.Role.BLOCK_QUOTE, quote.role());
        assertEquals(List.of(Element.PARAGRAPH, Element.PARAGRAPH), quote.children().stream().map(Box::element).toList());
        assertEquals("Una cita en dos líneas.", quote.children().get(0).text());
        // Its line is that of the first line of the quote, and its paragraphs have theirs.
        assertEquals(19, quote.line());
        assertEquals(22, quote.children().get(1).line());
    }

    @Test
    void aBlockQuoteHasNoLookOfItsOwn() {
        // Like a heading or a list: without its rule it is a block with nothing around its content.
        Box quote = markdown("> Una cita").get(0);
        assertEquals(sides(0, 0, 0, 0), quote.style().box().padding());
        assertEquals(sides(0, 0, 0, 0), quote.style().box().border());
    }

    @Test
    void aQuoteHoldsListsCodeTablesAndOtherQuotes() {
        List<Box> md = markdown(List.of("\tStyles:", "\t\tFor element: Code block", "\t\t\tTypography:",
                        "\t\t\t\tCode font: IBM Plex Mono"),
                "> - uno", "> - dos", ">", "> ```", "> código", "> ```", ">", "> | A | B |", "> |---|---|",
                "> | 1 | 2 |", ">", "> > Dentro.");
        Box quote = md.get(0);
        assertEquals(List.of(Element.LIST, Element.CODE_BLOCK, Element.TABLE, Element.BLOCK_QUOTE),
                quote.children().stream().map(Box::element).toList());
        assertEquals("Dentro.", quote.children().get(3).children().get(0).text());
    }

    @Test
    void aTableRowWithExtraCellsInsideAQuoteIsStillAnError() {
        InvalidModelException e = error(List.of(), "> | A | B |", "> |---|---|", "> | 1 | 2 | 3 |");
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
    }

    @Test
    void aHeadingInsideAQuoteIsNotAllowed() {
        InvalidModelException e = error(List.of(), "> # Título", ">", "> Texto.");
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
        assertTrue(e.getMessage().contains("block quote"), e.getMessage());
        assertEquals(17, e.line());
    }

    @Test
    void theThreeWaysOfWritingARuleAreADivider() {
        List<Box> md = markdown("Uno.", "", "---", "", "Dos.", "", "***", "", "Tres.", "", "___", "", "Cuatro.");
        assertEquals(List.of(Element.PARAGRAPH, Element.DIVIDER, Element.PARAGRAPH, Element.DIVIDER, Element.PARAGRAPH,
                Element.DIVIDER, Element.PARAGRAPH), md.stream().map(Box::element).toList());
        Box divider = md.get(1);
        assertEquals(Element.Role.DIVIDER, divider.role());
        assertTrue(divider.children().isEmpty());
        assertFalse(divider.hasText());
        assertEquals(sides(ElementReader.DIVIDER_BORDER, 0, 0, 0), divider.style().box().border());
        assertEquals(19, divider.line());
    }

    @Test
    void aRuleGivesTheQuoteItsLookAndChangesTheLineOfTheDivider() {
        List<Box> md = markdown(List.of("\tStyles:", "\t\tFor element: Block quote", "\t\t\tBox:",
                        "\t\t\t\tPadding: 4 8", "\t\t\t\tBackground: #F4F2EC", "\t\t\tTypography:", "\t\t\t\tItalic: true",
                        "\t\tFor element: Divider", "\t\t\tBox:", "\t\t\t\tBorder: 2 0 0 0", "\t\t\t\tMargin: 6 0"),
                "> Una cita", "", "---");
        Box quote = md.get(0);
        // The quote has what its rule gives it and nothing else.
        assertEquals(sides(4, 8, 4, 8), quote.style().box().padding());
        assertEquals(sides(0, 0, 0, 0), quote.style().box().border());
        // The Typography of the quote is inherited by what it contains.
        assertEquals(dev.stxt.pdf.model.FontStyle.ITALIC, quote.children().get(0).style().typography().fontStyle());
        Box divider = md.get(1);
        assertEquals(sides(2, 0, 0, 0), divider.style().box().border());
        assertEquals(sides(6, 0, 6, 0), divider.style().box().margin());
    }

    @Test
    void theRuleOfABlockDoesNotReachThem() {
        List<Box> md = markdown(List.of("\tStyles:", "\t\tFor element: Block", "\t\t\tBox:", "\t\t\t\tPadding: 30"),
                "> Una cita", "", "---");
        assertEquals(sides(0, 0, 0, 0), md.get(0).style().box().padding());
        assertEquals(sides(0, 0, 0, 0), md.get(1).style().box().padding());
    }

    @Test
    void aNestedRuleReachesThemInsideTheirMarkdown() {
        List<Box> md = markdown(List.of("\tStyles:", "\t\tFor element: Markdown", "\t\t\tFor element: Block quote",
                "\t\t\t\tBox:", "\t\t\t\t\tBorder: 0 0 0 3", "\t\t\tFor element: Divider", "\t\t\t\tBox:",
                "\t\t\t\t\tBorder: 1 0 0 0"), "> Una cita", "", "---");
        assertEquals(sides(0, 0, 0, 3), md.get(0).style().box().border());
        assertEquals(sides(1, 0, 0, 0), md.get(1).style().box().border());
    }

    @Test
    void theirRulesTakeNoElementPropertyAndContainNoRules() {
        for (String element : List.of("Block quote", "Divider")) {
            InvalidModelException property = error(List.of("\tStyles:", "\t\tFor element: " + element,
                    "\t\t\tTrim margins: both"), "Texto.");
            assertEquals(ErrorCode.STYLE_NOT_ALLOWED, property.errorCode(), element);
            InvalidModelException nested = error(List.of("\tStyles:", "\t\tFor element: " + element,
                    "\t\t\tFor element: Paragraph", "\t\t\t\tBox:", "\t\t\t\t\tPadding: 1"), "Texto.");
            assertEquals(ErrorCode.STYLE_NOT_ALLOWED, nested.errorCode(), element);
        }
    }

    @Test
    void aDividerTakesNoTypography() {
        InvalidModelException e = error(List.of("\tStyles:", "\t\tFor element: Divider", "\t\t\tTypography:",
                "\t\t\t\tColor: #FF0000"), "Texto.");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(13, e.line());
    }

    @Test
    void inATextWithMarkupMarkdownTheyAreStillText() {
        Box root = build("\tText: > no es una cita", "\t\tTypography:", "\t\t\tMarkup: markdown");
        assertEquals("> no es una cita", root.children().get(0).text());
    }

    @Test
    void aFootnoteCannotStartWithAQuote() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(
                "\tParagraph: Texto[^n].", "\t\tTypography:", "\t\t\tMarkup: markdown",
                "\tFootnote:", "\t\tId: n", "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\t> Una cita"));
        assertEquals(ErrorCode.FOOTNOTE_CONTENT, e.errorCode());
    }
}
