package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.RunStyle;
import dev.stxt.pdf.model.TextCase;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextPosition;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * Styled runs, {@code [text]{name}} (reference/common/text-formatting.md): a run with the properties of the
 * {@code For style} rule of that name that do not change the height of the line.
 */
class StyledRunsTest {

    /** The document up to the rules of {@code Styles}, which go next, at three tabs; 9 lines. */
    private static final String HEADER = """
            Document (dev.stxt.pdf):
            	Language: es
            	Title: Prueba
            	Page:
            		Width: 595
            		Height: 842
            	Styles:
            		For element: Document
            			Typography:
            				Font: IBM Plex Sans
            				Size: 10
            				Markup: markdown
            """;

    /**
     * The rules of {@code Styles} (from line 13), then the section with the elements (from line 14, two tabs more than
     * written).
     */
    private static Box build(String rules, String... elements) {
        StringBuilder doc = new StringBuilder(HEADER).append(rules).append("\tSection:\n");
        for (String e : elements) {
            doc.append("\t\t").append(e).append('\n');
        }
        return new BoxTreeBuilder().build(new DocumentReader().read(doc.toString())).sections().get(0).box();
    }

    private static Box text(String rules, String text) {
        return build(rules, "Text: " + text).children().get(0);
    }

    private static final String PRICE = """
            		For style: price
            			Typography:
            				Color: #B3261E
            				Bold: true
            """;

    @Test
    void theRunTakesTheValuesOfItsRule() {
        Box text = text(PRICE, "Total: [12,50 €]{price}.");
        assertEquals("Total: 12,50 €.", text.text());
        RunStyle price = new RunStyle("price", "#B3261E", null, null, FontWeight.BOLD, null, null, null, null, null);
        assertEquals(List.of(run("Total: "), new TextRun("12,50 €", TextFormat.PLAIN.withStyle(price)), run(".")),
                text.formattedRuns());
        // The box keeps the style with the line of each property.
        assertEquals(15, text.runStyle("price").line("color").number());
    }

    @Test
    void allTheAllowedProperties() {
        Box text = text("""
                		For style: all
                			Typography:
                				Font: IBM Plex Serif
                				Code font: IBM Plex Mono
                				Weight: 300
                				Italic: true
                				Color: #333333
                				Case: uppercase
                				Small caps: true
                				Letter spacing: 1.5
                """, "Uno [dos `tres`]{all}");
        RunStyle all = new RunStyle("all", "#333333", "IBM Plex Serif", "IBM Plex Mono", new FontWeight(300),
                FontStyle.ITALIC, 1.5, TextCase.UPPERCASE, true, null);
        TextFormat f = TextFormat.PLAIN.withStyle(all);
        assertEquals(List.of(run("Uno "), new TextRun("dos ", f.withFamily("IBM Plex Serif")),
                new TextRun("tres", f.asCode().withFamily("IBM Plex Mono"))), text.formattedRuns());
    }

    @Test
    void theMarksAddToTheStyleWhereverTheyAreWritten() {
        String light = """
                		For style: light
                			Typography:
                				Weight: 300
                """;
        Box inside = text(light, "[a **b**]{light}");
        Box outside = text(light, "**[b]{light}**");
        RunStyle style = new RunStyle("light", null, null, null, new FontWeight(300), null, null, null, null, null);
        TextFormat f = TextFormat.PLAIN.withStyle(style);
        assertEquals(List.of(new TextRun("a ", f), new TextRun("b", f.withMarks(true, false, false, false))),
                inside.formattedRuns());
        assertEquals(List.of(new TextRun("b", f.withMarks(true, false, false, false))), outside.formattedRuns());
    }

    @Test
    void aLinkCanHaveAStyledRun() {
        Box text = text(PRICE, "Ver [[precio]{price}](https://a.example)");
        TextRun price = text.formattedRuns().get(1);
        assertEquals("precio", price.text());
        assertEquals("https://a.example", price.format().link());
        assertTrue(price.format().underline());
        assertEquals("price", price.format().style().name());
    }

    @Test
    void aStyledRunCanHaveALink() {
        Box text = text(PRICE, "[Ver [precio](https://a.example)]{price}");
        assertEquals("Ver precio", text.text());
        assertEquals("price", text.formattedRuns().get(0).format().style().name());
        TextRun link = text.formattedRuns().get(1);
        assertEquals("https://a.example", link.format().link());
        assertEquals("price", link.format().style().name());
    }

    @Test
    void codeAndEscapedBracketsAreLiteral() {
        Box text = build(PRICE, "Text: `[a]{price}` y \\[b\\]{price}", "\tTypography:",
                "\t\tCode font: IBM Plex Mono").children().get(0);
        assertEquals("[a]{price} y [b]{price}", text.text());
        assertTrue(text.formattedRuns().stream().allMatch(r -> r.format().style() == null));
    }

    @Test
    void inACodeBlockOfAMarkdownTheTextIsKept() {
        Box root = build(PRICE, "Markdown:", "\tTypography:", "\t\tCode font: IBM Plex Mono", "\tContent >>",
                "\t\t~~~", "\t\t[a]{price} y [b]{price}", "\t\t~~~");
        assertEquals("[a]{price} y [b]{price}", root.children().get(0).children().get(0).text());
    }

    @Test
    void aStyledRunInsideAnotherIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> text(PRICE, "[a [b]{price} c]{price}"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
    }

    @Test
    void aStyledRunThatCrossesTwoCellsIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(PRICE, "Markdown:",
                "\tTable column sizes: auto", "\tContent >>", "\t\t| A | B |", "\t\t|---|---|", "\t\t| [a | b]{price} |"));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
        // The line of the row.
        assertEquals(23, e.line());
    }

    @Test
    void aBlankLineEndsTheBrackets() {
        Box root = build(PRICE, "Markdown:", "\tContent >>", "\t\t[a", "", "\t\tb]{price}").children().get(0);
        assertEquals("[a", root.children().get(0).text());
        assertEquals("b]{price}", root.children().get(1).text());
    }

    @Test
    void inAMarkdownToo() {
        Box root = build(PRICE, "Markdown:", "\tContent >>", "\t\tUn [precio]{price} y **otro**.");
        Box paragraph = root.children().get(0).children().get(0);
        assertEquals("Un precio y otro.", paragraph.text());
        assertEquals("#B3261E", paragraph.formattedRuns().get(1).format().color("#000000"));
    }

    @Test
    void whatIsNotAStyleNameStaysAsText() {
        // A reference, braces with spaces, a link before the braces or a space between them.
        Box text = text(PRICE, "[ver]{} [a]{b c} [d](https://a.example){price} [e] {price}");
        assertEquals("[ver]{} [a]{b c} d{price} [e] {price}", text.text());
    }

    @Test
    void withoutMarkupTheBracketsAreText() {
        Box text = build(PRICE, "Text: [12,50 €]{price}", "\tTypography:", "\t\tMarkup: none").children().get(0);
        assertEquals("[12,50 €]{price}", text.text());
        assertNull(text.formattedRuns());
    }

    @Test
    void aStyleWithoutRuleIsAnErrorOnTheLineOfTheText() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> build("", "Text: Uno", "Text:", "\tContent >>", "\t\tdos", "\t\t[tres]{nada}"));
        assertEquals(ErrorCode.STYLE_WITHOUT_RULE, e.errorCode());
        // Like every error of a text with Markup: markdown, the line where the text begins.
        assertEquals(17, e.line());
    }

    @Test
    void aPropertyThatChangesTheLineIsAnErrorOnItsLine() {
        InvalidModelException size = assertThrows(InvalidModelException.class, () -> text("""
                		For style: big
                			Typography:
                				Color: #333333
                				Size: 14
                """, "Uno [dos]{big}"));
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED_IN_RUN, size.errorCode());
        assertEquals(16, size.line());
        InvalidModelException box = assertThrows(InvalidModelException.class, () -> text("""
                		For style: boxed
                			Box:
                				Padding: 2
                """, "Uno [dos]{boxed}"));
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED_IN_RUN, box.errorCode());
        assertEquals(14, box.line());
    }

    @Test
    void theSameRuleOnABoxIsNotAnError() {
        Box block = build("""
                		For style: big
                			Typography:
                				Size: 14
                """, "Text: Uno", "\tStyle: big").children().get(0);
        assertEquals(14, block.style().typography().fontSize());
    }

    private static final String SCRIPTS = """
            		For style: sup
            			Typography:
            				Position: superscript
            		For style: sub
            			Typography:
            				Position: subscript
            				Color: #333333
            """;

    @Test
    void positionMakesTheRunASuperscriptOrASubscript() {
        Box text = text(SCRIPTS, "10 m[2]{sup} de CO[2]{sub}");
        assertEquals("10 m2 de CO2", text.text());
        RunStyle sup = new RunStyle("sup", null, null, null, null, null, null, null, null, TextPosition.SUPERSCRIPT);
        RunStyle sub = new RunStyle("sub", "#333333", null, null, null, null, null, null, null,
                TextPosition.SUBSCRIPT);
        assertEquals(List.of(run("10 m"), new TextRun("2", TextFormat.PLAIN.withStyle(sup)), run(" de CO"),
                new TextRun("2", TextFormat.PLAIN.withStyle(sub))), text.formattedRuns());
        assertEquals(15, text.runStyle("sup").line("text-position").number());
        // The box is not a superscript: Position is not inherited by it.
        assertEquals(TextPosition.NORMAL, text.style().typography().position());
    }

    @Test
    void positionInABoxIsAnErrorOnItsLine() {
        // In the Typography of an element.
        InvalidModelException own = assertThrows(InvalidModelException.class,
                () -> build("", "Text: Uno", "\tTypography:", "\t\tPosition: superscript"));
        assertEquals(ErrorCode.POSITION_OUTSIDE_RUN, own.errorCode());
        assertEquals(16, own.line());
        // By the rule of a styled run given to a box with Style: the line of the property in the rule.
        InvalidModelException styled = assertThrows(InvalidModelException.class,
                () -> build(SCRIPTS, "Text: Uno", "\tStyle: sup"));
        assertEquals(ErrorCode.POSITION_OUTSIDE_RUN, styled.errorCode());
        assertEquals(15, styled.line());
        // In a rule that only reaches boxes, when the rule is read, also with normal.
        InvalidModelException element = assertThrows(InvalidModelException.class, () -> build("""
                		For element: Paragraph
                			Typography:
                				Position: normal
                """, "Text: Uno"));
        assertEquals(ErrorCode.POSITION_OUTSIDE_RUN, element.errorCode());
        assertEquals(15, element.line());
        InvalidModelException document = assertThrows(InvalidModelException.class,
                () -> new BoxTreeBuilder().build(new DocumentReader().read(
                        HEADER.replace("Markup: markdown", "Position: subscript") + "\tSection:\n\t\tText: Uno\n")));
        assertEquals(ErrorCode.POSITION_OUTSIDE_RUN, document.errorCode());
    }

    /** A nested rule takes the superscript away from the runs inside the elements of the outer one. */
    @Test
    void aNestedRuleCanSetThePositionBackToNormal() {
        Box section = build(SCRIPTS + """
                		For style: flat
                			For style: sup
                				Typography:
                					Position: normal
                """, "Text: a[1]{sup}", "Text: b[2]{sup}", "\tStyle: flat");
        assertEquals(TextPosition.SUPERSCRIPT, section.children().get(0).formattedRuns().get(1).format().style().position());
        TextFormat inside = section.children().get(1).formattedRuns().get(1).format();
        assertEquals(TextPosition.NORMAL, inside.style().position());
        assertEquals(1, inside.scale());
        // In a For id, which only reaches a box, it is an error when the rule is read.
        InvalidModelException id = assertThrows(InvalidModelException.class, () -> build("""
                		For id: total
                			Typography:
                				Position: superscript
                """, "Text: Uno"));
        assertEquals(ErrorCode.POSITION_OUTSIDE_RUN, id.errorCode());
        assertEquals(15, id.line());
    }
}
