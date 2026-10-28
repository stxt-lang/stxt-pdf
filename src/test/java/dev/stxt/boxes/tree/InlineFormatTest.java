package dev.stxt.boxes.tree;

import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.boxes.tree.TextRuns.link;
import static dev.stxt.boxes.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** {@code Inline: markdown}: el formato dentro de la línea de un {@code Text}, un {@code Paragraph} o una {@code Cell} (§5.4). */
class InlineFormatTest {

    private static final String HEADER = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
            + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n";

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(HEADER + String.join("\n", lines))).root();
    }

    /** Como {@link #build}, con {@code Inline: markdown} en el {@code Typography} del documento. */
    private static Box buildInline(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader()
                .read(HEADER + "\t\tInline: markdown\n" + String.join("\n", lines))).root();
    }

    @Test
    void withoutItTheTextIsLiteral() {
        Box text = build("\tText: Una **negrita** literal").children.get(0);
        assertEquals("Una **negrita** literal", text.text);
        assertNull(text.runs);
    }

    @Test
    void theFourFormatsAndLinksBecomeRuns() {
        Box text = build("\tText: Una **negrita**, *cursiva*, ++subrayado*, ~~tachado~~ y [web](https://a.example).",
                "\t\tTypography:", "\t\t\tInline: markdown").children.get(0);
        assertEquals("Una negrita, cursiva, ++subrayado*, tachado y web.", text.text);
        assertEquals(List.of(run("Una "), run("negrita", "b"), run(", "), run("cursiva", "i"),
                run(", ++subrayado*, "), run("tachado", "s"), run(" y "),
                link("web", "u", "https://a.example"), run(".")), text.runs);
    }

    @Test
    void underlineIsTwoPlusSigns() {
        Box text = build("\tText: Un ++subrayado++ y **++ambos++**", "\t\tTypography:", "\t\t\tInline: markdown")
                .children.get(0);
        assertEquals(List.of(run("Un "), run("subrayado", "u"), run(" y "), run("ambos", "bu")), text.runs);
    }

    @Test
    void escapesGiveTheLiteralCharacter() {
        Box text = build("\tText: Dos \\*\\*estrellas\\*\\* y 3 \\* 4", "\t\tTypography:", "\t\t\tInline: markdown")
                .children.get(0);
        assertEquals("Dos **estrellas** y 3 * 4", text.text);
        assertNull(text.runs);
    }

    @Test
    void theContentBlockJoinsLinesWithASpaceAndKeepsParagraphs() {
        Box text = build("\tText:", "\t\tTypography:", "\t\t\tInline: markdown", "\t\tContent >>",
                "\t\t\tPrimera línea con **negrita**", "\t\t\tsigue aquí.", "", "", "\t\t\tOtro *párrafo*.").children.get(0);
        assertEquals("Primera línea con negrita sigue aquí.\n\nOtro párrafo.", text.text);
        assertEquals(List.of(run("Primera línea con "), run("negrita", "b"), run(" sigue aquí.\n\nOtro "),
                run("párrafo", "i"), run(".")), text.runs);
    }

    @Test
    void itIsInheritedAndNoneTurnsItOff() {
        Box root = buildInline("\tBlock:", "\t\tText: Uno **dos**",
                "\t\tText: Tres **cuatro**", "\t\t\tTypography:", "\t\t\t\tInline: none");
        Box block = root.children.get(0);
        assertEquals(Style.Inline.MARKDOWN, block.style.inline);
        assertEquals("Uno dos", block.children.get(0).text);
        assertEquals(List.of(run("Uno "), run("dos", "b")), block.children.get(0).runs);
        assertEquals("Tres **cuatro**", block.children.get(1).text);
        assertNull(block.children.get(1).runs);
    }

    @Test
    void aStylesRuleTurnsItOnForAnElement() {
        Box root = build("\tStyles:", "\t\tElement: Paragraph", "\t\t\tTypography:", "\t\t\t\tInline: markdown",
                "\tParagraph: Un **párrafo**", "\tText: Un **texto**");
        assertEquals("Un párrafo", root.children.get(0).text);
        assertEquals(List.of(run("Un "), run("párrafo", "b")), root.children.get(0).runs);
        assertEquals("Un **texto**", root.children.get(1).text);
    }

    @Test
    void cellsAndHeadingTextsToo() {
        Box root = buildInline("\tHeading 1:", "\t\tText: Un *título*", "\tTable:",
                "\t\tColumns: 100 100", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: Una **celda**", "\t\t\t\tCell: Otra");
        Box heading = root.children.get(0).children.get(0);
        assertEquals("Un título", heading.text);
        assertEquals(List.of(run("Un "), run("título", "i")), heading.runs);
        Box cell = root.children.get(1).children.get(0);
        assertEquals("Una celda", cell.text);
        assertEquals(List.of(run("Una "), run("celda", "b")), cell.runs);
        assertNull(root.children.get(1).children.get(1).runs);
    }

    @Test
    void insideAMarkdownTheTextIsNotInterpretedTwice() {
        // Lo escapado en un Markdown es literal: no se vuelve a interpretar como negrita.
        Box root = buildInline("\tMarkdown:", "\t\tContent >>",
                "\t\t\t\\*\\*x\\*\\* y **z**");
        Box paragraph = root.children.get(0).children.get(0);
        assertEquals("**x** y z", paragraph.text);
        assertEquals(List.of(run("**x** y "), run("z", "b")), paragraph.runs);
    }

    @Test
    void errorsPointToTheLineOfTheBlock() {
        InvalidModelException list = assertThrows(InvalidModelException.class, () -> buildInline("\tText: Uno", "\tText: - lista"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, list.code());
        assertEquals(11, list.line());
        InvalidModelException second = assertThrows(InvalidModelException.class, () -> buildInline("\tText:", "\t\tContent >>", "\t\t\tUno", "", "\t\t\t# Dos"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, second.code());
        assertEquals(14, second.line());
    }

    @Test
    void anInternalLinkToAnIdThatDoesNotExistIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> buildInline("\tText: Ver [aquí](#nada)"));
        assertEquals(ErrorCode.LINK_UNKNOWN, e.code());
        assertEquals(10, e.line());
    }

    @Test
    void aLinkSchemeThatIsNotAllowedIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> buildInline("\tText: Ver [aquí](javascript:alert(1))"));
        assertEquals(ErrorCode.LINK_SCHEME, e.code());
    }
}
