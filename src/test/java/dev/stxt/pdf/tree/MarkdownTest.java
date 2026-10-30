package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.model.WhiteSpace;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** El {@code Markdown} en el árbol de cajas (referencia/componentes/markdown.md). */
class MarkdownTest {

    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
            + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(HEADER + String.join("\n", lines))))).sections().get(0).box();
    }

    @Test
    void aBlockWithTheGeneratedElements() {
        Box root = build("\tMarkdown:", "\t\tId: M", "\t\tBullet markers: bullet", "\t\tTable columns: auto", "\t\tContent >>",
                "\t\t\t# Título", "", "\t\t\tUn **párrafo**.", "", "\t\t\t- ítem", "", "\t\t\t| A |", "\t\t\t|---|",
                "\t\t\t| *1* |");
        Box md = root.children().get(0);
        assertEquals(Element.MARKDOWN, md.element());
        assertEquals(Layout.BLOCK, md.style().layout());
        assertEquals(List.of(Element.HEADING_1, Element.PARAGRAPH, Element.LIST, Element.TABLE),
                md.children().stream().map(b -> b.element()).toList());
        Box paragraph = md.children().get(1);
        assertEquals("Un párrafo.", paragraph.text());
        assertEquals(List.of(run("Un "), run("párrafo", "b"), run(".")), paragraph.formattedRuns());
        // Sin formato, el texto no tiene tramos.
        assertNull(md.children().get(0).children().get(0).formattedRuns());
        Box cell = md.children().get(3).children().get(1);
        assertEquals(List.of(run("1", "i")), cell.formattedRuns());
        assertEquals(Marker.BULLET, md.children().get(2).style().list().marker());
    }

    @Test
    void stylesAndTypographyApplyToTheGeneratedElements() {
        Box root = build("\tStyles:", "\t\tFor element: Heading 1", "\t\t\tTypography:", "\t\t\t\tSize: 20",
                "\tMarkdown:", "\t\tTypography:", "\t\t\tColor: #336699", "\t\tContent >>", "\t\t\t# Título", "",
                "\t\t\tTexto.");
        Box md = root.children().get(0);
        Box title = md.children().get(0).children().get(0);
        assertEquals(20, title.style().typography().fontSize(), 1e-9);
        assertEquals("#336699", title.style().typography().color());
        assertEquals("#336699", md.children().get(1).style().typography().color());
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
        // En un ítem de lista, una celda, un Columns y un Item de Flex (K1).
        assertDoesNotThrow(() -> build("\tList:", "\t\tMarker: bullet", "\t\tList item:", "\t\t\tMarkdown:",
                "\t\t\t\tContent >>", "\t\t\t\t\tUno **dos**"));
        assertDoesNotThrow(() -> build("\tTable:", "\t\tColumn sizes: 100", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell:",
                "\t\t\t\t\tMarkdown:", "\t\t\t\t\t\tContent >>", "\t\t\t\t\t\t\tUno"));
        assertDoesNotThrow(() -> build("\tColumns:", "\t\tCount: 2", "\t\tMarkdown:", "\t\t\tContent >>",
                "\t\t\t\t# Uno", "", "\t\t\t\tTexto"));
        assertDoesNotThrow(() -> build("\tFlex:", "\t\tItem:", "\t\t\tMarkdown:", "\t\t\t\tContent >>",
                "\t\t\t\t\tUno"));
    }

    @Test
    void headingsCannotGoInAPageOverlay() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build("\tBlock:", "\tPage overlay:",
                "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\t# Pie"));
        assertEquals(ErrorCode.HEADING_IN_PAGE_OVERLAY, e.errorCode());
        assertEquals(17, e.line());
        assertDoesNotThrow(() -> build("\tBlock:", "\tPage overlay:", "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\tPie **{page}**"));
    }

    @Test
    void codeBlocksArePreformattedTextsInTheCodeFont() {
        Box root = build("\tMarkdown:", "\t\tTypography:", "\t\t\tCode font: IBM Plex Mono", "\t\tContent >>",
                "\t\t\tUsa `ls  -l` así.", "",
                "\t\t\t```", "\t\t\ta  b", "\t\t\t  c", "\t\t\t```");
        Box md = root.children().get(0);
        Box paragraph = md.children().get(0);
        assertEquals("Usa ls  -l así.", paragraph.text());
        assertEquals("IBM Plex Mono", paragraph.formattedRuns().get(1).format().family());
        assertEquals("IBM Plex Sans", paragraph.style().typography().font());
        Box code = md.children().get(1);
        assertEquals(Element.CODE_BLOCK, code.element());
        assertEquals("a  b\n  c", code.text());
        assertEquals(WhiteSpace.PRESERVE, code.style().typography().whiteSpace());
        assertEquals("IBM Plex Mono", code.style().typography().font());
    }

    /**
     * {@code Code font} es de {@code Typography}: se hereda de un antecesor, y {@code For element: Code block} cambia
     * la de los bloques sin tocar la del código en línea (referencia/componentes/markdown.md).
     */
    @Test
    void theCodeFontIsInheritedAndACodeBlockRuleChangesOnlyTheBlocks() {
        Box root = build("\tStyles:", "\t\tFor element: Code block", "\t\t\tTypography:", "\t\t\t\tCode font: IBM Plex Serif",
                "\tBlock:", "\t\tTypography:", "\t\t\tCode font: IBM Plex Mono",
                "\t\tMarkdown:", "\t\t\tContent >>", "\t\t\t\tUsa `ls`.", "", "\t\t\t\t```", "\t\t\t\tls -l", "\t\t\t\t```");
        Box md = root.children().get(0).children().get(0);
        assertEquals("IBM Plex Mono", md.children().get(0).formattedRuns().get(1).format().family());
        assertEquals("IBM Plex Serif", md.children().get(1).style().typography().font());
    }

    @Test
    void aCodeBlockWithoutCodeFontIsAnErrorOnItsLine() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> build("\tMarkdown:", "\t\tContent >>", "\t\t\tAntes.", "", "\t\t\t```", "\t\t\tx", "\t\t\t```"));
        assertEquals(ErrorCode.CODE_WITHOUT_FONT, e.errorCode());
        assertEquals(17, e.line());
    }

    /**
     * referencia/comun/estilos.md: la regla {@code For element: Code block} da estilo al código y no a los demás
     * textos, ni al revés.
     */
    @Test
    void aCodeBlockHasItsOwnElementRule() {
        Box root = build("\tStyles:", "\t\tFor element: Text", "\t\t\tTypography:", "\t\t\t\tColor: #FF0000",
                "\t\tFor element: Code block", "\t\t\tTypography:", "\t\t\t\tSize: 8", "\t\t\tBox:",
                "\t\t\t\tBackground: #EEEEEE", "\t\t\t\tPadding: 6",
                "\tMarkdown:", "\t\tTypography:", "\t\t\tCode font: IBM Plex Mono", "\t\tContent >>", "\t\t\t# Título", "",
                "\t\t\t```",
                "\t\t\ta  b", "\t\t\t```");
        Box md = root.children().get(0);
        Box title = md.children().get(0).children().get(0);
        Box code = md.children().get(1);
        // El Text del encabezado sigue alcanzándolo For element: Text; el bloque de código, solo Code block.
        assertEquals(Element.TEXT, title.element());
        assertEquals("#FF0000", title.style().typography().color());
        assertEquals(Element.CODE_BLOCK, code.element());
        assertEquals(8, code.style().typography().fontSize(), 1e-9);
        assertEquals(new Paint.Solid("#EEEEEE"), code.style().box().background());
        assertEquals(Sides.all(Value.pt(6)), code.style().box().padding());
        assertEquals(WhiteSpace.PRESERVE, code.style().typography().whiteSpace());
        assertEquals("IBM Plex Mono", code.style().typography().font());
        // Y el Text del encabezado no recibe lo del bloque de código.
        assertNull(title.style().box().background());
    }

    /** Un {@code Text} escrito a mano con {@code White space: pre} sigue siendo un {@code Text}. */
    @Test
    void aWrittenPreformattedTextIsNotACodeBlock() {
        Box root = build("\tText:", "\t\tTypography:", "\t\t\tWhite space: preserve", "\t\tContent >>", "\t\t\ta  b");
        assertEquals(Element.TEXT, root.children().get(0).element());
    }
}
