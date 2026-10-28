package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.boxes.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** El {@code Markdown} en el árbol de cajas (ESPECIFICACION.md §5.17). */
class MarkdownTest {

    private static final String HEADER = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
            + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.mergeStyles(HEADER + String.join("\n", lines)))).root();
    }

    @Test
    void aBlockWithTheGeneratedElements() {
        Box root = build("\tMarkdown:", "\t\tId: M", "\t\tBullet: disc", "\t\tTable columns: auto", "\t\tContent >>",
                "\t\t\t# Título", "", "\t\t\tUn **párrafo**.", "", "\t\t\t- ítem", "", "\t\t\t| A |", "\t\t\t|---|",
                "\t\t\t| *1* |");
        Box md = root.children.get(0);
        assertEquals(Element.MARKDOWN, md.element);
        assertEquals(Style.Layout.BLOCK, md.style.layout);
        assertEquals(List.of(Element.HEADING_1, Element.PARAGRAPH, Element.LIST, Element.TABLE),
                md.children.stream().map(b -> b.element).toList());
        Box paragraph = md.children.get(1);
        assertEquals("Un párrafo.", paragraph.text);
        assertEquals(List.of(run("Un "), run("párrafo", "b"), run(".")), paragraph.runs);
        // Sin formato, el texto no tiene tramos.
        assertNull(md.children.get(0).children.get(0).runs);
        Box cell = md.children.get(3).children.get(1);
        assertEquals(List.of(run("1", "i")), cell.runs);
        assertEquals(Style.Marker.DISC, md.children.get(2).style.listMarker);
    }

    @Test
    void stylesAndTypographyApplyToTheGeneratedElements() {
        Box root = build("\tStyles:", "\t\tElement: Heading 1", "\t\t\tTypography:", "\t\t\t\tSize: 20",
                "\tMarkdown:", "\t\tTypography:", "\t\t\tColor: #336699", "\t\tContent >>", "\t\t\t# Título", "",
                "\t\t\tTexto.");
        Box md = root.children.get(0);
        Box title = md.children.get(0).children.get(0);
        assertEquals(20, title.style.fontSize, 1e-9);
        assertEquals("#336699", title.style.color);
        assertEquals("#336699", md.children.get(1).style.color);
    }

    @Test
    void headingsContinueTheHierarchyOfTheDocument() {
        assertDoesNotThrow(() -> build("\tHeading 1:", "\t\tText: Uno", "\tMarkdown:", "\t\tContent >>",
                "\t\t\t## Uno punto uno", "\tHeading 2:", "\t\tText: Uno punto dos"));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build("\tHeading 1:",
                "\t\tText: Uno", "\tMarkdown:", "\t\tContent >>", "\t\t\t### Salto"));
        assertEquals(ErrorCode.HEADING_LEVEL, e.code());
        assertEquals(15, e.line());
    }

    @Test
    void allowedWhereAFlowElementGoes() {
        // En un ítem de lista, una celda, un Multicol y un Item de Flex (K1).
        assertDoesNotThrow(() -> build("\tList:", "\t\tMarker: disc", "\t\tList item:", "\t\t\tMarkdown:",
                "\t\t\t\tContent >>", "\t\t\t\t\tUno **dos**"));
        assertDoesNotThrow(() -> build("\tTable:", "\t\tColumns: 100", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell:",
                "\t\t\t\t\tMarkdown:", "\t\t\t\t\t\tContent >>", "\t\t\t\t\t\t\tUno"));
        assertDoesNotThrow(() -> build("\tMulticol:", "\t\tCount: 2", "\t\tMarkdown:", "\t\t\tContent >>",
                "\t\t\t\t# Uno", "", "\t\t\t\tTexto"));
        assertDoesNotThrow(() -> build("\tFlex:", "\t\tItem:", "\t\t\tMarkdown:", "\t\t\t\tContent >>",
                "\t\t\t\t\tUno"));
    }

    @Test
    void headingsCannotGoInAFixed() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build("\tFixed:",
                "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\t# Pie"));
        assertEquals(ErrorCode.HEADING_IN_FIXED, e.code());
        assertEquals(14, e.line());
        assertDoesNotThrow(() -> build("\tFixed:", "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\tPie **{page}**"));
    }

    @Test
    void codeBlocksArePreformattedTextsInTheCodeFont() {
        Box root = build("\tMarkdown:", "\t\tCode font: IBM Plex Mono", "\t\tContent >>", "\t\t\tUsa `ls  -l` así.", "",
                "\t\t\t```", "\t\t\ta  b", "\t\t\t  c", "\t\t\t```");
        Box md = root.children.get(0);
        Box paragraph = md.children.get(0);
        assertEquals("Usa ls  -l así.", paragraph.text);
        assertEquals("IBM Plex Mono", paragraph.runs.get(1).format().family());
        assertEquals("IBM Plex Sans", paragraph.style.font);
        Box code = md.children.get(1);
        assertEquals(Element.TEXT, code.element);
        assertEquals("a  b\n  c", code.text);
        assertEquals(Style.WhiteSpace.PRE, code.style.whiteSpace);
        assertEquals("IBM Plex Mono", code.style.font);
    }
}
