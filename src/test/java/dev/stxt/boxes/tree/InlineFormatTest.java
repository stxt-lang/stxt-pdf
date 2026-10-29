package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.model.Inline;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.boxes.tree.TextRuns.link;
import static dev.stxt.boxes.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code Inline: markdown}: el formato dentro de la línea de un {@code Text}, un {@code Paragraph} o una {@code Cell}
 * (referencia/comun/formato-del-texto.md).
 */
class InlineFormatTest {

    private static final String HEADER = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
            + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(HEADER + String.join("\n", lines))))).sections().get(0).box();
    }

    /** Como {@link #build}, con {@code Inline: markdown} en el {@code Typography} del documento. */
    private static Box buildInline(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader()
                .read(Docs.sectioned(Docs.mergeStyles(HEADER + "\t\t\t\tInline: markdown\n" + String.join("\n", lines))))).sections().get(0).box();
    }

    @Test
    void withoutItTheTextIsLiteral() {
        Box text = build("\tText: Una **negrita** literal").children().get(0);
        assertEquals("Una **negrita** literal", text.text());
        assertNull(text.formattedRuns());
    }

    @Test
    void theFourFormatsAndLinksBecomeRuns() {
        Box text = build("\tText: Una **negrita**, *cursiva*, ++subrayado*, ~~tachado~~ y [web](https://a.example).",
                "\t\tTypography:", "\t\t\tInline: markdown").children().get(0);
        assertEquals("Una negrita, cursiva, ++subrayado*, tachado y web.", text.text());
        assertEquals(List.of(run("Una "), run("negrita", "b"), run(", "), run("cursiva", "i"),
                run(", ++subrayado*, "), run("tachado", "s"), run(" y "),
                link("web", "u", "https://a.example"), run(".")), text.formattedRuns());
    }

    @Test
    void underlineIsTwoPlusSigns() {
        Box text = build("\tText: Un ++subrayado++ y **++ambos++**", "\t\tTypography:", "\t\t\tInline: markdown")
                .children().get(0);
        assertEquals(List.of(run("Un "), run("subrayado", "u"), run(" y "), run("ambos", "bu")), text.formattedRuns());
    }

    @Test
    void escapesGiveTheLiteralCharacter() {
        Box text = build("\tText: Dos \\*\\*estrellas\\*\\* y 3 \\* 4", "\t\tTypography:", "\t\t\tInline: markdown")
                .children().get(0);
        assertEquals("Dos **estrellas** y 3 * 4", text.text());
        assertNull(text.formattedRuns());
    }

    @Test
    void theContentBlockJoinsLinesWithASpaceAndKeepsParagraphs() {
        Box text = build("\tText:", "\t\tTypography:", "\t\t\tInline: markdown", "\t\tContent >>",
                "\t\t\tPrimera línea con **negrita**", "\t\t\tsigue aquí.", "", "", "\t\t\tOtro *párrafo*.").children().get(0);
        assertEquals("Primera línea con negrita sigue aquí.\n\nOtro párrafo.", text.text());
        assertEquals(List.of(run("Primera línea con "), run("negrita", "b"), run(" sigue aquí.\n\nOtro "),
                run("párrafo", "i"), run(".")), text.formattedRuns());
    }

    @Test
    void itIsInheritedAndNoneTurnsItOff() {
        Box root = buildInline("\tBlock:", "\t\tText: Uno **dos**",
                "\t\tText: Tres **cuatro**", "\t\t\tTypography:", "\t\t\t\tInline: none");
        Box block = root.children().get(0);
        assertEquals(Inline.MARKDOWN, block.style().typography().inline());
        assertEquals("Uno dos", block.children().get(0).text());
        assertEquals(List.of(run("Uno "), run("dos", "b")), block.children().get(0).formattedRuns());
        assertEquals("Tres **cuatro**", block.children().get(1).text());
        assertNull(block.children().get(1).formattedRuns());
    }

    @Test
    void aStylesRuleTurnsItOnForAnElement() {
        Box root = build("\tStyles:", "\t\tElement: Paragraph", "\t\t\tTypography:", "\t\t\t\tInline: markdown",
                "\tParagraph: Un **párrafo**", "\tText: Un **texto**");
        assertEquals("Un párrafo", root.children().get(0).text());
        assertEquals(List.of(run("Un "), run("párrafo", "b")), root.children().get(0).formattedRuns());
        assertEquals("Un **texto**", root.children().get(1).text());
    }

    @Test
    void cellsAndHeadingTextsToo() {
        Box root = buildInline("\tHeading 1:", "\t\tText: Un *título*", "\tTable:",
                "\t\tColumns: 100 100", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: Una **celda**", "\t\t\t\tCell: Otra");
        Box heading = root.children().get(0).children().get(0);
        assertEquals("Un título", heading.text());
        assertEquals(List.of(run("Un "), run("título", "i")), heading.formattedRuns());
        Box cell = root.children().get(1).children().get(0);
        assertEquals("Una celda", cell.text());
        assertEquals(List.of(run("Una "), run("celda", "b")), cell.formattedRuns());
        assertNull(root.children().get(1).children().get(1).formattedRuns());
    }

    @Test
    void insideAMarkdownTheTextIsNotInterpretedTwice() {
        // Lo escapado en un Markdown es literal: no se vuelve a interpretar como negrita.
        Box root = buildInline("\tMarkdown:", "\t\tContent >>",
                "\t\t\t\\*\\*x\\*\\* y **z**");
        Box paragraph = root.children().get(0).children().get(0);
        assertEquals("**x** y z", paragraph.text());
        assertEquals(List.of(run("**x** y "), run("z", "b")), paragraph.formattedRuns());
    }

    @Test
    void errorsPointToTheLineOfTheBlock() {
        InvalidModelException list = assertThrows(InvalidModelException.class, () -> buildInline("\tText: Uno", "\tText: - lista"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, list.errorCode());
        assertEquals(15, list.line());
        InvalidModelException second = assertThrows(InvalidModelException.class, () -> buildInline("\tText:", "\t\tContent >>", "\t\t\tUno", "", "\t\t\t# Dos"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, second.errorCode());
        assertEquals(18, second.line());
    }

    @Test
    void anInternalLinkToAnIdThatDoesNotExistIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> buildInline("\tText: Ver [aquí](#nada)"));
        assertEquals(ErrorCode.LINK_UNKNOWN, e.errorCode());
        assertEquals(14, e.line());
    }

    @Test
    void aLinkSchemeThatIsNotAllowedIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> buildInline("\tText: Ver [aquí](javascript:alert(1))"));
        assertEquals(ErrorCode.LINK_SCHEME, e.errorCode());
    }
}
