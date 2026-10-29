package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
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

    private static final String HEADER = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
            + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(HEADER + String.join("\n", lines))))).sections().get(0).box();
    }

    @Test
    void aBlockWithTheGeneratedElements() {
        Box root = build("\tMarkdown:", "\t\tId: M", "\t\tBullet: disc", "\t\tTable columns: auto", "\t\tContent >>",
                "\t\t\t# Título", "", "\t\t\tUn **párrafo**.", "", "\t\t\t- ítem", "", "\t\t\t| A |", "\t\t\t|---|",
                "\t\t\t| *1* |");
        Box md = root.children().get(0);
        assertEquals(Element.MARKDOWN, md.element());
        assertEquals(Style.Layout.BLOCK, md.style().layout);
        assertEquals(List.of(Element.HEADING_1, Element.PARAGRAPH, Element.LIST, Element.TABLE),
                md.children().stream().map(b -> b.element()).toList());
        Box paragraph = md.children().get(1);
        assertEquals("Un párrafo.", paragraph.text());
        assertEquals(List.of(run("Un "), run("párrafo", "b"), run(".")), paragraph.formattedRuns());
        // Sin formato, el texto no tiene tramos.
        assertNull(md.children().get(0).children().get(0).formattedRuns());
        Box cell = md.children().get(3).children().get(1);
        assertEquals(List.of(run("1", "i")), cell.formattedRuns());
        assertEquals(Style.Marker.DISC, md.children().get(2).style().listMarker);
    }

    @Test
    void stylesAndTypographyApplyToTheGeneratedElements() {
        Box root = build("\tStyles:", "\t\tElement: Heading 1", "\t\t\tTypography:", "\t\t\t\tSize: 20",
                "\tMarkdown:", "\t\tTypography:", "\t\t\tColor: #336699", "\t\tContent >>", "\t\t\t# Título", "",
                "\t\t\tTexto.");
        Box md = root.children().get(0);
        Box title = md.children().get(0).children().get(0);
        assertEquals(20, title.style().fontSize, 1e-9);
        assertEquals("#336699", title.style().color);
        assertEquals("#336699", md.children().get(1).style().color);
    }

    @Test
    void headingsContinueTheHierarchyOfTheDocument() {
        assertDoesNotThrow(() -> build("\tHeading 1:", "\t\tText: Uno", "\tMarkdown:", "\t\tContent >>",
                "\t\t\t## Uno punto uno", "\tHeading 2:", "\t\tText: Uno punto dos"));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build("\tHeading 1:",
                "\t\tText: Uno", "\tMarkdown:", "\t\tContent >>", "\t\t\t### Salto"));
        assertEquals(ErrorCode.HEADING_LEVEL, e.errorCode());
        assertEquals(17, e.line());
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
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build("\tBlock:", "\tFixed:",
                "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\t# Pie"));
        assertEquals(ErrorCode.HEADING_IN_FIXED, e.errorCode());
        assertEquals(17, e.line());
        assertDoesNotThrow(() -> build("\tBlock:", "\tFixed:", "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\tPie **{page}**"));
    }

    @Test
    void codeBlocksArePreformattedTextsInTheCodeFont() {
        Box root = build("\tMarkdown:", "\t\tCode font: IBM Plex Mono", "\t\tContent >>", "\t\t\tUsa `ls  -l` así.", "",
                "\t\t\t```", "\t\t\ta  b", "\t\t\t  c", "\t\t\t```");
        Box md = root.children().get(0);
        Box paragraph = md.children().get(0);
        assertEquals("Usa ls  -l así.", paragraph.text());
        assertEquals("IBM Plex Mono", paragraph.formattedRuns().get(1).format().family());
        assertEquals("IBM Plex Sans", paragraph.style().font);
        Box code = md.children().get(1);
        assertEquals(Element.CODE_BLOCK, code.element());
        assertEquals("a  b\n  c", code.text());
        assertEquals(Style.WhiteSpace.PRE, code.style().whiteSpace);
        assertEquals("IBM Plex Mono", code.style().font);
    }

    /** §4.10: la regla {@code Element: Code block} da estilo al código y no a los demás textos, ni al revés. */
    @Test
    void aCodeBlockHasItsOwnElementRule() {
        Box root = build("\tStyles:", "\t\tElement: Text", "\t\t\tTypography:", "\t\t\t\tColor: #FF0000",
                "\t\tElement: Code block", "\t\t\tTypography:", "\t\t\t\tSize: 8", "\t\t\tBox:",
                "\t\t\t\tBackground: #EEEEEE", "\t\t\t\tPadding: 6",
                "\tMarkdown:", "\t\tCode font: IBM Plex Mono", "\t\tContent >>", "\t\t\t# Título", "", "\t\t\t```",
                "\t\t\ta  b", "\t\t\t```");
        Box md = root.children().get(0);
        Box title = md.children().get(0).children().get(0);
        Box code = md.children().get(1);
        // El Text del encabezado sigue alcanzándolo Element: Text; el bloque de código, solo Code block.
        assertEquals(Element.TEXT, title.element());
        assertEquals("#FF0000", title.style().color);
        assertEquals(Element.CODE_BLOCK, code.element());
        assertEquals(8, code.style().fontSize, 1e-9);
        assertEquals(new Paint.Solid("#EEEEEE"), code.style().background);
        assertEquals(Sides.all(Value.pt(6)), code.style().padding);
        assertEquals(Style.WhiteSpace.PRE, code.style().whiteSpace);
        assertEquals("IBM Plex Mono", code.style().font);
        // Y el Text del encabezado no recibe lo del bloque de código.
        assertNull(title.style().background);
    }

    /** Un {@code Text} escrito a mano con {@code White space: pre} sigue siendo un {@code Text}. */
    @Test
    void aWrittenPreformattedTextIsNotACodeBlock() {
        Box root = build("\tText:", "\t\tTypography:", "\t\t\tWhite space: pre", "\t\tContent >>", "\t\t\ta  b");
        assertEquals(Element.TEXT, root.children().get(0).element());
    }
}
