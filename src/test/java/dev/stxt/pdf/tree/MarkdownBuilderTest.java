package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La traducción de {@code Markdown} a nodos STXT (referencia/componentes/markdown.md). El {@code Markdown}
 * está en la línea 8 del documento, sus propiedades en las siguientes y después {@code Content}:
 * sin propiedades, la primera línea del Markdown es la 10.
 */
class MarkdownBuilderTest {

    private static final DocumentReader READER = new DocumentReader();

    /** Un {@code Markdown} con estas propiedades y este contenido, traducido. */
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

    /** {@code Nombre: valor} de un nodo y de sus hijos, con dos espacios por nivel, y la línea. */
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
        // Un salto de línea simple es un espacio; una tilde sola no tacha.
        assertEquals(List.of("Paragraph: Uno dos tres ~sin~ tachar @17"), outline(t.elements().get(1)));
        Node text = ((InlineNode) t.elements().get(0)).getChildren().get(0);
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
    void anImageAloneInItsParagraphIsAnImage() {
        MarkdownBuilder.Translation t = translate("![Un *paisaje*](test/wide.png)", "", "![](logos/acme.png)",
                "", "Texto.");
        // El texto alternativo pierde su formato; sin él, la imagen es decorativa.
        assertEquals(List.of("Image: test/wide.png @15", "  Alt: Un paisaje @15"), outline(t.elements().get(0)));
        assertEquals(List.of("Image: logos/acme.png @17"), outline(t.elements().get(1)));
        assertEquals(List.of("Paragraph: Texto. @19"), outline(t.elements().get(2)));
    }

    @Test
    void plainTextHasNoRuns() {
        MarkdownBuilder.Translation t = translate("Sin formato.");
        assertTrue(t.runs().isEmpty());
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
                // El último marcador sirve para los niveles más profundos.
                "          Marker: dash @14",
                "          List item @19",
                "            Paragraph: c @19"), outline(t.elements().get(0)));
        assertEquals(List.of("List @21", "  Marker: lower-roman @15", "  Start: 3 @21", "  List item @21",
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
        MarkdownBuilder.Translation t = translate(List.of("Table columns: equal"),
                "| Mes | Total |", "|-----|------:|", "| Ene | **120** |", "| Feb |");
        assertEquals(List.of("Table @16",
                "  Column sizes: repeat(2, 1fr) @14",
                "  Head @16",
                "    Row @16",
                "      Cell: Mes @16",
                "      Cell: Total @16",
                "        Typography @16",
                "          Align: right @16",
                "  Body @18",
                "    Row @18",
                "      Cell: Ene @18",
                "      Cell: 120 @18",
                "        Typography @18",
                "          Align: right @18",
                "    Row @19",
                "      Cell: Feb @19",
                // GFM completa la fila con una celda vacía.
                "      Cell @19"), outline(t.elements().get(0)));
        InlineNode body = (InlineNode) ((InlineNode) t.elements().get(0)).getChild("body");
        Node cell = ((InlineNode) body.getChildren().get(0)).getChildren().get(1);
        assertEquals(List.of(run("120", "b")), t.runs().get(cell));
    }

    /**
     * Una fila con más celdas que la cabecera es un error, en la línea de la fila (referencia/componentes/markdown.md);
     * una barra escapada no separa celdas, y las de los extremos son opcionales.
     */
    @Test
    void rowsWithMoreCellsThanTheHeadAreAnError() {
        translate(List.of("Table columns: auto"), "A | B", "--|--", "1 \\| 2 | 3", "| x | y |");
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> translate(
                List.of("Table columns: auto"), "| A | B |", "|---|---|", "| 1 | 2 |", "3 | 4 | 5"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
        assertEquals(19, e.line());
    }

    @Test
    void autoColumns() {
        MarkdownBuilder.Translation t = translate(List.of("Table columns: auto"), "| A |", "|---|", "| 1 |");
        assertEquals("repeat(1, auto)", ((InlineNode) t.elements().get(0)).getChild("column-sizes").getText());
    }

    @Test
    void unusedPropertiesAreAllowed() {
        // Bullet markers, Number markers y Table columns solo son obligatorias si hay listas o tablas (K6).
        MarkdownBuilder.Translation t = translate(List.of("Bullet markers: bullet", "Table columns: auto"), "Hola.");
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
     * Un enlace es un tramo subrayado con su destino; el formato de dentro se conserva (referencia/comun/enlaces.md).
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
        MarkdownBuilder.Translation t = translate(List.of("Table columns: auto"), "# Ver [anexo](#anexo)", "",
                "| A | B |", "|---|---|", "| [uno](https://a.example) | dos |");
        assertEquals(List.of(run("Ver "), link("anexo", "u", "#anexo")),
                t.runs().get(t.elements().get(0).getChildren().get(0)));
        // Table: Columns, Head, Body; la primera celda de la primera fila del cuerpo.
        Node body = ((InlineNode) t.elements().get(1)).getChildren().get(2);
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
    void linkReferenceDefinitionsAndImagesInsideALinkStayUnsupported() {
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, assertThrows(InvalidModelException.class,
                () -> translate("[a][r]", "", "[r]: https://stxt.dev")).errorCode());
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, assertThrows(InvalidModelException.class,
                () -> translate("[![logo](test/wide.png)](https://stxt.dev) texto")).errorCode());
    }

    /** {@code {#id}} al final de un encabezado le da su {@code Id} y se quita del texto. */
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

    // --- Código ------------------------------------------------------------------------------------

    /** El código es un tramo de código; su familia, la de {@code Code font}, la recibe al construir su caja. */
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
                "    White space: preserve @17",
                "  Content: a  b\n  c @17"), outline(t.elements().get(1)));
        assertEquals(Set.of(t.elements().get(1)), t.codeBlocks());
    }

    @Test
    void anIndentedCodeBlockKeepsItsRelativeIndentation() {
        MarkdownBuilder.Translation t = translate("Antes.", "", "    uno", "      dos");
        InlineNode code = t.elements().get(1);
        assertEquals("uno\n  dos", ((InlineNode) code).getChild("content").getText());
        // El Content >> va en la línea anterior a la primera línea del código (la 17).
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
        MarkdownBuilder.Translation t = translate(List.of("Bullet markers: bullet", "Table columns: auto"),
                "| A |", "|---|", "| `x` |", "", "- uno", "", "  ```", "  y", "  ```");
        InlineNode cell = (InlineNode) ((InlineNode) ((InlineNode) t.elements().get(0).getChild("body")).getChildren()
                .get(0)).getChildren().get(0);
        assertTrue(t.runs().get(cell).get(0).format().code());
        InlineNode item = (InlineNode) t.elements().get(1).getChild("list-item");
        assertEquals("Text", item.getChildren().get(1).getName());
    }
}
