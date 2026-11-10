package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Markup;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.InlineImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.tree.TextRuns.link;
import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Markup: markdown}: inline formatting inside a {@code Text}, a {@code Paragraph} or a {@code Cell}
 * (reference/common/text-formatting.md).
 */
class InlineFormatTest {

    private static final String HEADER = Docs.header(595, 842) + Docs.TYPOGRAPHY;

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(Docs.read(HEADER, lines)).sections().get(0).box();
    }

    /** Like {@link #build}, with {@code Markup: markdown} in the {@code Typography} of the document. */
    private static Box buildInline(String... lines) {
        return new BoxTreeBuilder().build(Docs.read(HEADER + "\t\t\t\tMarkup: markdown\n", lines)).sections().get(0).box();
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
                "\t\tTypography:", "\t\t\tMarkup: markdown").children().get(0);
        assertEquals("Una negrita, cursiva, ++subrayado*, tachado y web.", text.text());
        assertEquals(List.of(run("Una "), run("negrita", "b"), run(", "), run("cursiva", "i"),
                run(", ++subrayado*, "), run("tachado", "s"), run(" y "),
                link("web", "u", "https://a.example"), run(".")), text.formattedRuns());
    }

    @Test
    void underlineIsTwoPlusSigns() {
        Box text = build("\tText: Un ++subrayado++ y **++ambos++**", "\t\tTypography:", "\t\t\tMarkup: markdown")
                .children().get(0);
        assertEquals(List.of(run("Un "), run("subrayado", "u"), run(" y "), run("ambos", "bu")), text.formattedRuns());
    }

    @Test
    void escapesGiveTheLiteralCharacter() {
        Box text = build("\tText: Dos \\*\\*estrellas\\*\\* y 3 \\* 4", "\t\tTypography:", "\t\t\tMarkup: markdown")
                .children().get(0);
        assertEquals("Dos **estrellas** y 3 * 4", text.text());
        assertNull(text.formattedRuns());
    }

    @Test
    void theContentBlockJoinsLinesWithASpace() {
        Box text = build("\tText:", "\t\tTypography:", "\t\t\tMarkup: markdown", "\t\tContent >>",
                "\t\t\tPrimera línea con **negrita**", "\t\t\tsigue *aquí*.").children().get(0);
        assertEquals("Primera línea con negrita sigue aquí.", text.text());
        assertEquals(List.of(run("Primera línea con "), run("negrita", "b"), run(" sigue "), run("aquí", "i"), run(".")),
                text.formattedRuns());
    }

    @Test
    void itIsInheritedAndNoneTurnsItOff() {
        Box root = buildInline("\tBlock:", "\t\tText: Uno **dos**",
                "\t\tText: Tres **cuatro**", "\t\t\tTypography:", "\t\t\t\tMarkup: none");
        Box block = root.children().get(0);
        assertEquals(Markup.MARKDOWN, block.style().typography().markup());
        assertEquals("Uno dos", block.children().get(0).text());
        assertEquals(List.of(run("Uno "), run("dos", "b")), block.children().get(0).formattedRuns());
        assertEquals("Tres **cuatro**", block.children().get(1).text());
        assertNull(block.children().get(1).formattedRuns());
    }

    @Test
    void aStylesRuleTurnsItOnForAnElement() {
        Box root = build("\tStyles:", "\t\tFor element: Paragraph", "\t\t\tTypography:", "\t\t\t\tMarkup: markdown",
                "\tParagraph: Un **párrafo**", "\tText: Un **texto**");
        assertEquals("Un párrafo", root.children().get(0).text());
        assertEquals(List.of(run("Un "), run("párrafo", "b")), root.children().get(0).formattedRuns());
        assertEquals("Un **texto**", root.children().get(1).text());
    }

    @Test
    void cellsAndHeadingTextsToo() {
        Box root = buildInline("\tHeading 1:", "\t\tText: Un *título*", "\tTable:",
                "\t\tColumn sizes: 100 100", "\t\tBody rows:", "\t\t\tRow:", "\t\t\t\tCell: Una **celda**", "\t\t\t\tCell: Otra");
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
        // What is escaped in a Markdown is literal: it is not interpreted again as bold.
        Box root = buildInline("\tMarkdown:", "\t\tContent >>",
                "\t\t\t\\*\\*x\\*\\* y **z**");
        Box paragraph = root.children().get(0).children().get(0);
        assertEquals("**x** y z", paragraph.text());
        assertEquals(List.of(run("**x** y "), run("z", "b")), paragraph.formattedRuns());
    }

    @Test
    void errorsPointToTheLineOfTheParagraph() {
        InvalidModelException image = assertThrows(InvalidModelException.class,
                () -> buildInline("\tText: Uno", "\tText: Ver ![logo](logo.png \"Logo\")"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, image.errorCode());
        assertEquals(15, image.line());
        // In a block, the line of the text, the first of the block.
        InvalidModelException block = assertThrows(InvalidModelException.class,
                () -> buildInline("\tText:", "\t\tContent >>", "\t\t\tUno", "\t\t\tver ![logo](logo.png \"Logo\")"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, block.errorCode());
        assertEquals(16, block.line());
    }

    /**
     * An image inside the text (reference/common/text-formatting.md): a run that is the image, with its path and its
     * alternative text, or decorative without it; inside a link, with the link.
     */
    @Test
    void anImageInTheTextIsARun() {
        Box text = buildInline("\tText: Ver ![el logo](test/square.png) y ![](test/check.svg) o "
                + "[![Web](test/shapes.svg)](https://stxt.dev)").children().get(0);
        assertEquals("Ver \uFFFC y \uFFFC o \uFFFC", text.text());
        List<TextRun> runs = text.runs();
        assertEquals(6, runs.size());
        assertEquals(new TextRun("Ver ", TextFormat.PLAIN), runs.get(0));
        assertEquals(new TextRun(TextRun.IMAGE, TextFormat.PLAIN.withImage(new InlineImage("test/square.png",
                "el logo"))), runs.get(1));
        assertEquals(new TextRun(" y ", TextFormat.PLAIN), runs.get(2));
        assertEquals(new TextRun(TextRun.IMAGE, TextFormat.PLAIN.withImage(new InlineImage("test/check.svg", null))),
                runs.get(3));
        assertTrue(runs.get(3).format().image().decorative());
        assertEquals(new TextRun(TextRun.IMAGE, new TextFormat(false, false, true, false).withLink("https://stxt.dev")
                .withImage(new InlineImage("test/shapes.svg", "Web"))), runs.get(5));
    }

    /** Two images one after the other are two runs, each one drawn where its run is. */
    @Test
    void twoImagesTogetherAreTwoRuns() {
        Box text = buildInline("\tText: ![a](test/square.png)![a](test/square.png)").children().get(0);
        assertEquals(2, text.runs().size());
        assertEquals(TextRun.IMAGE, text.runs().get(0).text());
        assertEquals(TextRun.IMAGE, text.runs().get(1).text());
    }

    /** What would start a block in a Markdown is text, without escaping it (reference/common/text-formatting.md). */
    @Test
    void blockSyntaxIsLiteralText() {
        Box root = buildInline("\tText: - 5 % de descuento", "\tText: 2024. Un buen año", "\tText: # 1 en ventas",
                "\tText: > 30 años", "\tText: ---", "\tText: 1) Primero",
                "\tText:", "\t\tContent >>", "\t\t\tHay que comprar:", "\t\t\t- **leche**", "\t\t\t===");
        List<String> texts = root.inFlowChildren().stream().map(Box::text).toList();
        assertEquals(List.of("- 5 % de descuento", "2024. Un buen año", "# 1 en ventas", "> 30 años", "---",
                "1) Primero", "Hay que comprar: - leche ==="), texts);
        assertEquals(List.of(run("Hay que comprar: - "), run("leche", "b"), run(" ===")),
                root.children().get(6).formattedRuns());
    }

    /** Inline code uses the family of {@code Code font}, which is inherited (reference/common/text-formatting.md). */
    @Test
    void inlineCodeUsesTheInheritedCodeFont() {
        Box root = buildInline("\tBlock:", "\t\tTypography:", "\t\t\tCode font: IBM Plex Mono",
                "\t\tText: Usa `mvn test` ya.");
        Box text = root.children().get(0).children().get(0);
        assertEquals("Usa mvn test ya.", text.text());
        assertEquals(List.of(run("Usa "), new TextRun("mvn test", TextFormat.PLAIN.asCode().withFamily("IBM Plex Mono")),
                run(" ya.")), text.formattedRuns());
        assertEquals("IBM Plex Sans", text.style().typography().font());
    }

    @Test
    void codeWithoutCodeFontIsAnErrorOnTheLineOfTheText() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> buildInline("\tText:", "\t\tContent >>", "\t\t\tUno", "\t\t\tusa `mvn`."));
        assertEquals(ErrorCode.CODE_WITHOUT_FONT, e.errorCode());
        assertEquals(16, e.line());
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
