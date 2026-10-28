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
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.boxes):", "\tMedia:", "\t\tWidth: 595",
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
        assertEquals(List.of("Heading 1 @10", "  Text: Título grande @10"), outline(t.elements().get(0)));
        // Un salto de línea simple es un espacio; una tilde sola no tacha.
        assertEquals(List.of("Paragraph: Uno dos tres ~sin~ tachar @12"), outline(t.elements().get(1)));
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
        assertEquals(List.of("Image: test/wide.png @10", "  Alt: Un paisaje @10"), outline(t.elements().get(0)));
        assertEquals(List.of("Image: logos/acme.png @12"), outline(t.elements().get(1)));
        assertEquals(List.of("Paragraph: Texto. @14"), outline(t.elements().get(2)));
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
        assertEquals(List.of("List @12",
                "  Marker: disc @9",
                "  List item @12",
                "    Paragraph: a @12",
                "    List @13",
                "      Marker: dash @9",
                "      List item @13",
                "        Paragraph: b @13",
                "        List @14",
                // El último marcador sirve para los niveles más profundos.
                "          Marker: dash @9",
                "          List item @14",
                "            Paragraph: c @14"), outline(t.elements().get(0)));
        assertEquals(List.of("List @16", "  Marker: lower-roman @10", "  Start: 3 @16", "  List item @16",
                "    Paragraph: x @16"), outline(t.elements().get(1)));
    }

    @Test
    void anItemCanHaveSeveralParagraphs() {
        MarkdownBuilder.Translation t = translate(List.of("Numbering: decimal"), "1. uno", "", "   sigue");
        assertEquals(List.of("List @11", "  Marker: decimal @9", "  List item @11", "    Paragraph: uno @11",
                "    Paragraph: sigue @13"), outline(t.elements().get(0)));
    }

    @Test
    void tablesWithHeadAlignmentAndEmptyCells() {
        MarkdownBuilder.Translation t = translate(List.of("Table columns: equal"),
                "| Mes | Total |", "|-----|------:|", "| Ene | **120** |", "| Feb |");
        assertEquals(List.of("Table @11",
                "  Columns: repeat(2, 1fr) @9",
                "  Head @11",
                "    Row @11",
                "      Cell: Mes @11",
                "      Cell: Total @11",
                "        Typography @11",
                "          Align: right @11",
                "  Body @13",
                "    Row @13",
                "      Cell: Ene @13",
                "      Cell: 120 @13",
                "        Typography @13",
                "          Align: right @13",
                "    Row @14",
                "      Cell: Feb @14",
                // GFM completa la fila con una celda vacía.
                "      Cell @14"), outline(t.elements().get(0)));
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
}
