package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.TextRun;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.boxes.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La traducción de {@code Markdown} a nodos STXT (ESPECIFICACION.md §5.17). El {@code Markdown}
 * está en la línea 8 del documento, sus propiedades en las siguientes y después {@code Content}:
 * sin propiedades, la primera línea del Markdown es la 10.
 */
class MarkdownBuilderTest {

    private static final DocumentReader READER = new DocumentReader();

    /** Un {@code Markdown} con estas propiedades y este contenido, traducido. */
    private static MarkdownBuilder.Translation translate(List<String> properties, String... content) {
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.boxes):", "\tLanguage: es", "\tMedia:", "\t\tWidth: 595",
                "\t\tHeight: 842", "\tTypography:", "\t\tFont: IBM Plex Sans", "\t\tSize: 10", "\tMarkdown:"));
        properties.forEach(p -> lines.add("\t\t" + p));
        lines.add("\t\tContent >>");
        for (String c : content) {
            lines.add(c.isEmpty() ? "" : "\t\t\t" + c);
        }
        InlineNode doc = READER.read(String.join("\n", lines));
        return MarkdownBuilder.translate((InlineNode) doc.getChild("markdown"));
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
        assertEquals(List.of("Heading 1 @11", "  Text: Título grande @11"), outline(t.elements().get(0)));
        // Un salto de línea simple es un espacio; una tilde sola no tacha.
        assertEquals(List.of("Paragraph: Uno dos tres ~sin~ tachar @13"), outline(t.elements().get(1)));
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
        assertEquals(List.of("Image: test/wide.png @11", "  Alt: Un paisaje @11"), outline(t.elements().get(0)));
        assertEquals(List.of("Image: logos/acme.png @13"), outline(t.elements().get(1)));
        assertEquals(List.of("Paragraph: Texto. @15"), outline(t.elements().get(2)));
    }

    @Test
    void plainTextHasNoRuns() {
        MarkdownBuilder.Translation t = translate("Sin formato.");
        assertTrue(t.runs().isEmpty());
    }

    @Test
    void listsTakeTheirMarkerFromTheLevel() {
        MarkdownBuilder.Translation t = translate(List.of("Bullet: disc dash", "Numbering: lower-roman"),
                "- a", "  - b", "    - c", "", "3. x");
        assertEquals(List.of("List @13",
                "  Marker: disc @10",
                "  List item @13",
                "    Paragraph: a @13",
                "    List @14",
                "      Marker: dash @10",
                "      List item @14",
                "        Paragraph: b @14",
                "        List @15",
                // El último marcador sirve para los niveles más profundos.
                "          Marker: dash @10",
                "          List item @15",
                "            Paragraph: c @15"), outline(t.elements().get(0)));
        assertEquals(List.of("List @17", "  Marker: lower-roman @11", "  Start: 3 @17", "  List item @17",
                "    Paragraph: x @17"), outline(t.elements().get(1)));
    }

    @Test
    void anItemCanHaveSeveralParagraphs() {
        MarkdownBuilder.Translation t = translate(List.of("Numbering: decimal"), "1. uno", "", "   sigue");
        assertEquals(List.of("List @12", "  Marker: decimal @10", "  List item @12", "    Paragraph: uno @12",
                "    Paragraph: sigue @14"), outline(t.elements().get(0)));
    }

    @Test
    void tablesWithHeadAlignmentAndEmptyCells() {
        MarkdownBuilder.Translation t = translate(List.of("Table columns: equal"),
                "| Mes | Total |", "|-----|------:|", "| Ene | **120** |", "| Feb |");
        assertEquals(List.of("Table @12",
                "  Columns: repeat(2, 1fr) @10",
                "  Head @12",
                "    Row @12",
                "      Cell: Mes @12",
                "      Cell: Total @12",
                "        Typography @12",
                "          Align: right @12",
                "  Body @14",
                "    Row @14",
                "      Cell: Ene @14",
                "      Cell: 120 @14",
                "        Typography @14",
                "          Align: right @14",
                "    Row @15",
                "      Cell: Feb @15",
                // GFM completa la fila con una celda vacía.
                "      Cell @15"), outline(t.elements().get(0)));
        InlineNode body = (InlineNode) ((InlineNode) t.elements().get(0)).getChild("body");
        Node cell = ((InlineNode) body.getChildren().get(0)).getChildren().get(1);
        assertEquals(List.of(run("120", "b")), t.runs().get(cell));
    }

    @Test
    void autoColumns() {
        MarkdownBuilder.Translation t = translate(List.of("Table columns: auto"), "| A |", "|---|", "| 1 |");
        assertEquals("repeat(1, auto)", ((InlineNode) t.elements().get(0)).getChild("columns").getText());
    }

    @Test
    void unusedPropertiesAreAllowed() {
        // Bullet, Numbering y Table columns solo son obligatorias si hay listas o tablas (K6).
        MarkdownBuilder.Translation t = translate(List.of("Bullet: disc", "Table columns: auto"), "Hola.");
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

    /** Un enlace es un tramo subrayado con su destino; el formato de dentro se conserva (§5.20). */
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
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, title.code());
        assertEquals(11, title.line());
        InvalidModelException scheme = assertThrows(InvalidModelException.class,
                () -> translate("", "Un [enlace](javascript:alert(1))."));
        assertEquals(ErrorCode.LINK_SCHEME, scheme.code());
        assertEquals(12, scheme.line());
    }

    @Test
    void linkReferenceDefinitionsAndImagesInsideALinkStayUnsupported() {
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, assertThrows(InvalidModelException.class,
                () -> translate("[a][r]", "", "[r]: https://stxt.dev")).code());
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, assertThrows(InvalidModelException.class,
                () -> translate("[![logo](test/wide.png)](https://stxt.dev) texto")).code());
    }

    private static TextRun link(String text, String format, String destination) {
        return dev.stxt.boxes.tree.TextRuns.link(text, format, destination);
    }
}
