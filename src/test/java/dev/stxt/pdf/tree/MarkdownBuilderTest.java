package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.InlineImage;
import dev.stxt.pdf.model.RunStyle;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * The translation of {@code Markdown} into STXT nodes (reference/components/markdown.md). The {@code Markdown}
 * is on line 8 of the document, its properties on the following ones and then {@code Content}:
 * without properties, the first line of the Markdown is line 10.
 */
class MarkdownBuilderTest {

    private static final DocumentReader READER = new DocumentReader();

    /** A {@code Markdown} with these properties and this content, translated. */
    private static MarkdownBuilder.Translation translate(List<String> properties, String... content) {
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.pdf):", "\tLanguage: es", "\tTitle: Prueba", "\tPage:", "\t\tWidth: 595",
                "\t\tHeight: 842", "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10", "\tMarkdown:"));
        properties.forEach(p -> lines.add("\t\t" + p));
        lines.add("\t\tContent >>");
        for (String c : content) {
            lines.add(c.isEmpty() ? "" : "\t\t\t" + c);
        }
        InlineNode doc = READER.read(Docs.sectioned(Docs.mergeStyles(String.join("\n", lines))));
        return MarkdownBuilder.translate((InlineNode) ((InlineNode) doc.getChild("section")).getChild("markdown"));
    }

    private static MarkdownBuilder.Translation translate(String... content) {
        return translate(List.of(), content);
    }

    /** {@code Name: value} of a node and its children, with two spaces per level, and the line. */
    private static List<String> outline(Node n) {
        List<String> out = new ArrayList<>();
        outline(n, "", out);
        return out;
    }

    private static void outline(Node n, String indent, List<String> out) {
        String value = n.getText();
        out.add(indent + n.getName() + (value.isEmpty() ? "" : ": " + value) + " @" + n.getLine());
        if (n instanceof InlineNode i) {
            i.getChildren().forEach(c -> outline(c, indent + "  ", out));
        }
    }

    @Test
    void headingsAndParagraphsWithTheirRuns() {
        MarkdownBuilder.Translation t = translate("# Título **grande**", "", "Uno *dos*", "tres ~sin~ tachar");
        assertEquals(2, t.elements().size());
        assertEquals(List.of("Heading 1 @15", "  Text: Título grande @15"), outline(t.elements().get(0)));
        // A single line break is a space; a lone tilde does not strike through.
        assertEquals(List.of("Paragraph: Uno dos tres ~sin~ tachar @17"), outline(t.elements().get(1)));
        Node text = t.elements().get(0).getChildren().get(0);
        assertEquals(List.of(run("Título "), run("grande", "b")), t.runs().get(text));
        assertEquals(List.of(run("Uno "), run("dos", "i"), run(" tres ~sin~ tachar")),
                t.runs().get(t.elements().get(1)));
    }

    @Test
    void formatsCombine() {
        MarkdownBuilder.Translation t = translate("***todo*** **a ++b++** ~~c~~ ++d++");
        assertEquals(List.of(run("todo", "bi"), run(" "), run("a ", "b"), run("b", "bu"), run(" "), run("c", "s"),
                run(" "), run("d", "u")), t.runs().get(t.elements().get(0)));
    }

    @Test
    void aLineBreakIsALineSeparatorInTheText() {
        // \ at the end of the line (STXT removes the spaces at the end of the lines of a block, so two spaces are not
        // a line break); the separator takes the format around the break.
        String br = String.valueOf(TextRun.LINE_SEPARATOR);
        MarkdownBuilder.Translation t = translate("Joan **Costa**\\", "Director\\", "Acme");
        assertEquals(List.of("Paragraph: Joan Costa" + br + "Director" + br + "Acme @15"),
                outline(t.elements().get(0)));
        assertEquals(List.of(run("Joan "), run("Costa", "b"), run(br + "Director" + br + "Acme")),
                t.runs().get(t.elements().get(0)));
    }

    @Test
    void anImageAloneInItsParagraphIsAnImage() {
        MarkdownBuilder.Translation t = translate("![Un *paisaje*](test/wide.png)", "", "![](logos/acme.png)",
                "", "Texto.");
        // The alternative text loses its formatting; without it, the image is decorative.
        assertEquals(List.of("Image: test/wide.png @15", "  Alt: Un paisaje @15"), outline(t.elements().get(0)));
        assertEquals(List.of("Image: logos/acme.png @17"), outline(t.elements().get(1)));
        assertEquals(List.of("Paragraph: Texto. @19"), outline(t.elements().get(2)));
    }

    /**
     * A footnote call, {@code [^id]} (reference/components/footnote.md): a superscript run with the number of the
     * note, in the format of the text around it, numbered in reading order.
     */
    @Test
    void footnoteCallsAreNumberedSuperscriptRuns() {
        MarkdownBuilder.Translation t = translate("Uno[^a] y **dos[^b-2]**.");
        assertEquals(List.of("Paragraph: Uno1 y dos2. @15"), outline(t.elements().get(0)));
        assertEquals(List.of(run("Uno"), new TextRun("1", TextFormat.PLAIN.withFootnote("a")), run(" y "),
                run("dos", "b"), new TextRun("2", run("", "b").format().withFootnote("b-2")), run(".")),
                t.runs().get(t.elements().get(0)));
        // Inline, the same, and the counter is that of the document, not of the text.
        Footnotes footnotes = new Footnotes();
        assertEquals(List.of(run("Tres"), new TextRun("1", TextFormat.PLAIN.withFootnote("c"))),
                MarkdownBuilder.inline("Tres[^c]", 5, footnotes));
        assertEquals(List.of(new TextRun("2", TextFormat.PLAIN.withFootnote("d")), run(" cuatro")),
                MarkdownBuilder.inline("[^d] cuatro", 6, footnotes));
        // A second call to the same note is an error, with the line of the text.
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> MarkdownBuilder.inline("Otra[^c]", 9, footnotes));
        assertEquals(ErrorCode.FOOTNOTE_CALL_DUPLICATE, e.errorCode());
        assertEquals(9, e.line());
    }

    @Test
    void plainTextHasNoRuns() {
        MarkdownBuilder.Translation t = translate("Sin formato.");
        assertEquals(Map.of(), t.runs());
    }

    @Test
    void listsTakeTheirMarkerFromTheLevel() {
        MarkdownBuilder.Translation t = translate(List.of("Bullet markers: bullet dash", "Number markers: lower-roman"),
                "- a", "  - b", "    - c", "", "3. x");
        assertEquals(List.of("List @17",
                "  Marker: bullet @14",
                "  List item @17",
                "    Paragraph: a @17",
                "    List @18",
                "      Marker: dash @14",
                "      List item @18",
                "        Paragraph: b @18",
                "        List @19",
                // The last marker serves the deeper levels.
                "          Marker: dash @14",
                "          List item @19",
                "            Paragraph: c @19"), outline(t.elements().get(0)));
        assertEquals(List.of("List @21", "  Marker: lower-roman @15", "  First number: 3 @21", "  List item @21",
                "    Paragraph: x @21"), outline(t.elements().get(1)));
    }

    @Test
    void anItemCanHaveSeveralParagraphs() {
        MarkdownBuilder.Translation t = translate(List.of("Number markers: decimal"), "1. uno", "", "   sigue");
        assertEquals(List.of("List @16", "  Marker: decimal @14", "  List item @16", "    Paragraph: uno @16",
                "    Paragraph: sigue @18"), outline(t.elements().get(0)));
    }

    @Test
    void tablesWithHeadAlignmentAndEmptyCells() {
        MarkdownBuilder.Translation t = translate(List.of("Table column sizes: equal"),
                "| Mes | Total |", "|-----|------:|", "| Ene | **120** |", "| Feb |");
        assertEquals(List.of("Table @16",
                "  Column sizes: repeat(2, *) @14",
                "  Header rows @16",
                "    Row @16",
                "      Cell: Mes @16",
                "      Cell: Total @16",
                "        Typography @16",
                "          Align: right @16",
                "  Body rows @18",
                "    Row @18",
                "      Cell: Ene @18",
                "      Cell: 120 @18",
                "        Typography @18",
                "          Align: right @18",
                "    Row @19",
                "      Cell: Feb @19",
                // GFM completes the row with an empty cell.
                "      Cell @19"), outline(t.elements().get(0)));
        InlineNode body = (InlineNode) t.elements().get(0).getChild("body-rows");
        Node cell = ((InlineNode) body.getChildren().get(0)).getChildren().get(1);
        assertEquals(List.of(run("120", "b")), t.runs().get(cell));
    }

    /**
     * A row with more cells than the header is an error, on the line of the row (reference/components/markdown.md);
     * an escaped bar does not separate cells, and the ones at the ends are optional.
     */
    @Test
    void rowsWithMoreCellsThanTheHeadAreAnError() {
        translate(List.of("Table column sizes: auto"), "A | B", "--|--", "1 \\| 2 | 3", "| x | y |");
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> translate(
                List.of("Table column sizes: auto"), "| A | B |", "|---|---|", "| 1 | 2 |", "3 | 4 | 5"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
        assertEquals(19, e.line());
    }

    @Test
    void autoColumns() {
        MarkdownBuilder.Translation t = translate(List.of("Table column sizes: auto"), "| A |", "|---|", "| 1 |");
        assertEquals("repeat(1, auto)", t.elements().get(0).getChild("column-sizes").getText());
    }

    @Test
    void unusedPropertiesAreAllowed() {
        // Bullet markers, Number markers and Table column sizes are only required if there are lists or tables (K6).
        MarkdownBuilder.Translation t = translate(List.of("Bullet markers: bullet", "Table column sizes: auto"), "Hola.");
        assertEquals(1, t.elements().size());
        assertNull(t.runs().get(t.elements().get(0)));
    }

    @Test
    void theRunsAreTheTextOfTheNode() {
        MarkdownBuilder.Translation t = translate("Hola **mundo**, *adiós*.");
        List<TextRun> runs = t.runs().get(t.elements().get(0));
        assertEquals(t.elements().get(0).getText(), TextRun.text(runs));
        assertFalse(runs.isEmpty());
    }

    /**
     * A link is an underlined run with its destination; the formatting inside is kept (reference/common/links.md).
     */
    @Test
    void linksAreUnderlinedRunsWithTheirDestination() {
        MarkdownBuilder.Translation t = translate("Mira [la **web**](https://stxt.dev) y <mailto:a@b.c>.");
        assertEquals("Mira la web y mailto:a@b.c.", t.elements().get(0).getText());
        assertEquals(List.of(run("Mira "), link("la ", "u", "https://stxt.dev"), link("web", "bu", "https://stxt.dev"),
                run(" y "), link("mailto:a@b.c", "u", "mailto:a@b.c"), run(".")), t.runs().get(t.elements().get(0)));
    }

    @Test
    void linksToABoxAndLinksInTablesAndHeadings() {
        MarkdownBuilder.Translation t = translate(List.of("Table column sizes: auto"), "# Ver [anexo](#anexo)", "",
                "| A | B |", "|---|---|", "| [uno](https://a.example) | dos |");
        assertEquals(List.of(run("Ver "), link("anexo", "u", "#anexo")),
                t.runs().get(t.elements().get(0).getChildren().get(0)));
        // Table: Columns, Header rows, Body rows; the first cell of the first row of the body.
        Node body = t.elements().get(1).getChildren().get(2);
        Node cell = ((InlineNode) ((InlineNode) body).getChildren().get(0)).getChildren().get(0);
        assertEquals(List.of(link("uno", "u", "https://a.example")), t.runs().get(cell));
    }

    @Test
    void sameDestinationRunsJoinAndDifferentOnesDoNot() {
        MarkdownBuilder.Translation t = translate("[a](https://x.example)[b](https://y.example)[c](https://y.example)");
        assertEquals(List.of(link("a", "u", "https://x.example"), link("bc", "u", "https://y.example")),
                t.runs().get(t.elements().get(0)));
    }

    @Test
    void aLinkWithATitleOrAnUnsupportedDestinationIsAnError() {
        InvalidModelException title = assertThrows(InvalidModelException.class,
                () -> translate("Un [enlace](https://stxt.dev \"web\")."));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, title.errorCode());
        assertEquals(15, title.line());
        InvalidModelException scheme = assertThrows(InvalidModelException.class,
                () -> translate("", "Un [enlace](javascript:alert(1))."));
        assertEquals(ErrorCode.LINK_SCHEME, scheme.errorCode());
        assertEquals(16, scheme.line());
    }

    @Test
    void linkReferenceDefinitionsStayUnsupported() {
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, assertThrows(InvalidModelException.class,
                () -> translate("[a][r]", "", "[r]: https://stxt.dev")).errorCode());
    }

    /**
     * An image inside a text (reference/common/text-formatting.md) is a run that is the image, in the format of the
     * text around it; one alone in its paragraph is still an {@code Image}; a title is still an error.
     */
    @Test
    void anImageInsideATextIsARun() {
        MarkdownBuilder.Translation t = translate("Ver ![el logo](test/wide.png) y [![Web](test/wide.png)](https://stxt.dev) **![](test/wide.png)**");
        InlineNode p = t.elements().get(0);
        assertEquals(List.of("Paragraph: Ver \uFFFC y \uFFFC \uFFFC @15"), outline(p));
        List<TextRun> runs = t.runs().get(p);
        assertEquals(new TextRun(TextRun.IMAGE, TextFormat.PLAIN.withImage(new InlineImage("test/wide.png",
                "el logo"))), runs.get(1));
        assertEquals(new TextRun(TextRun.IMAGE, new TextFormat(false, false, true, false).withLink("https://stxt.dev")
                .withImage(new InlineImage("test/wide.png", "Web"))), runs.get(3));
        assertEquals(new TextRun(TextRun.IMAGE, new TextFormat(true, false, false, false)
                .withImage(new InlineImage("test/wide.png", null))), runs.get(5));
        InvalidModelException title = assertThrows(InvalidModelException.class,
                () -> translate("Ver ![logo](test/wide.png \"Logo\")"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, title.errorCode());
    }

    /**
     * An image inside a styled run does not close it, and the style does not apply to the image
     * (reference/common/text-formatting.md): the text before and after it has the style, the image does not.
     */
    @Test
    void anImageInsideAStyledRunKeepsTheRunOpen() {
        TextFormat s = TextFormat.PLAIN.withStyle(RunStyle.named("s"));
        assertEquals(List.of(new TextRun("a ", s),
                        new TextRun(TextRun.IMAGE, TextFormat.PLAIN.withImage(new InlineImage("test/check.svg", "b"))),
                        new TextRun(" d", s), run(" e")),
                MarkdownBuilder.inline("[a ![b](test/check.svg) d]{s} e", 1));
        // The alternative text of the image, with its own marks, does not change the run around it either.
        assertEquals(List.of(new TextRun("a ", s), new TextRun(TextRun.IMAGE,
                        TextFormat.PLAIN.withImage(new InlineImage("test/check.svg", "b c"))), new TextRun(" d", s)),
                MarkdownBuilder.inline("[a ![b **c**](test/check.svg) d]{s}", 1));
    }

    /** {@code {#id}} at the end of a heading gives it its {@code Id} and is removed from the text. */
    @Test
    void aHeadingTakesTheIdAtTheEndOfItsText() {
        MarkdownBuilder.Translation t = translate("# Ventas del año {#ventas}", "", "## **Costes** {#costes}", "",
                "## Sin id");
        assertEquals(List.of("Heading 1 @15", "  Id: ventas @15", "  Text: Ventas del año @15"),
                outline(t.elements().get(0)));
        InlineNode costes = t.elements().get(1);
        assertEquals("costes", costes.getChild("id").getText());
        assertEquals(List.of(new TextRun("Costes", new TextFormat(true, false, false, false))),
                t.runs().get(costes.getChild("text")));
        assertEquals(List.of("Heading 2 @19", "  Text: Sin id @19"), outline(t.elements().get(2)));
    }

    @Test
    void aHeadingIdWithSpacesOrEmptyIsAnError() {
        InvalidModelException spaces = assertThrows(InvalidModelException.class,
                () -> translate("# Ventas {#mis ventas}"));
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, spaces.errorCode());
        assertEquals(15, spaces.line());
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED,
                assertThrows(InvalidModelException.class, () -> translate("# Ventas {#}")).errorCode());
        assertEquals(ErrorCode.TEXT_CONTENT,
                assertThrows(InvalidModelException.class, () -> translate("# {#solo}")).errorCode());
    }

    private static TextRun link(String text, String format, String destination) {
        return dev.stxt.pdf.tree.TextRuns.link(text, format, destination);
    }

    // --- Code --------------------------------------------------------------------------------------

    /** Code is a code run; it receives its family, that of {@code Code font}, when its box is built. */
    @Test
    void inlineCodeIsACodeRun() {
        MarkdownBuilder.Translation t = translate("Usa `mvn test` ya.");
        InlineNode paragraph = t.elements().get(0);
        assertEquals("Usa mvn test ya.", paragraph.getText());
        assertEquals(List.of(new TextRun("Usa ", TextFormat.PLAIN),
                new TextRun("mvn test", TextFormat.PLAIN.asCode()),
                new TextRun(" ya.", TextFormat.PLAIN)), t.runs().get(paragraph));
    }

    @Test
    void inlineCodeKeepsTheFormatAroundIt() {
        MarkdownBuilder.Translation t = translate("**negrita y `código`** y [`enlace`](https://example.com/a)");
        List<TextRun> runs = t.runs().get(t.elements().get(0));
        TextFormat boldCode = new TextFormat(true, false, false, false).asCode();
        assertEquals(new TextRun("código", boldCode), runs.get(1));
        TextRun link = runs.get(runs.size() - 1);
        assertEquals("enlace", link.text());
        assertTrue(link.format().code());
        assertEquals("https://example.com/a", link.format().link());
        assertTrue(link.format().underline());
    }

    @Test
    void aFencedCodeBlockIsAPreformattedText() {
        MarkdownBuilder.Translation t = translate("Antes.", "", "```", "a  b", "  c", "```", "", "Después.");
        assertEquals(List.of("Paragraph", "Text", "Paragraph"), t.elements().stream().map(Node::getName).toList());
        assertEquals(List.of(
                "Text @17",
                "  Typography @17",
                "    Preformatted: true @17",
                "  Content: a  b\n  c @17"), outline(t.elements().get(1)));
        assertEquals(Set.of(t.elements().get(1)), t.codeBlocks());
    }

    @Test
    void anIndentedCodeBlockKeepsItsRelativeIndentation() {
        MarkdownBuilder.Translation t = translate("Antes.", "", "    uno", "      dos");
        InlineNode code = t.elements().get(1);
        assertEquals("uno\n  dos", code.getChild("content").getText());
        // The Content >> goes on the line before the first line of the code (line 17).
        assertEquals(16, code.getChild("content").getLine());
    }

    @Test
    void theLanguageLabelOfAFenceIsNotSupported() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> translate("```java", "int x;", "```"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
        assertEquals(15, e.line());
    }

    @Test
    void anEmptyCodeBlockIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> translate("```", "```"));
        assertEquals(ErrorCode.TEXT_CONTENT, e.errorCode());
    }

    @Test
    void codeInATableCellAndInAListItem() {
        MarkdownBuilder.Translation t = translate(List.of("Bullet markers: bullet", "Table column sizes: auto"),
                "| A |", "|---|", "| `x` |", "", "- uno", "", "  ```", "  y", "  ```");
        InlineNode cell = (InlineNode) ((InlineNode) ((InlineNode) t.elements().get(0).getChild("body-rows")).getChildren()
                .get(0)).getChildren().get(0);
        assertTrue(t.runs().get(cell).get(0).format().code());
        InlineNode item = (InlineNode) t.elements().get(1).getChild("list-item");
        assertEquals("Text", item.getChildren().get(1).getName());
    }
}
