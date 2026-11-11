package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

/**
 * The footnotes in the box tree (reference/components/footnote.md): the area of each section, the notes that hang
 * from it, the numbering of the calls and the label of each note.
 */
class FootnotesTest {

    private static final String HEADER = Docs.header(595, 842) + Docs.TYPOGRAPHY + "\t\t\t\tMarkup: markdown\n";

    private static BoxTreeBuilder.Result build(String... lines) {
        return new BoxTreeBuilder().build(Docs.read(HEADER, lines));
    }

    private static TextRun call(String number, String id) {
        return new TextRun(number, TextFormat.PLAIN.withFootnote(id));
    }

    private static TextRun label(String number) {
        return new TextRun(number, TextFormat.PLAIN.asLabel());
    }

    @Test
    void theNotesHangFromTheAreaOfTheSectionWhereverTheyAreWritten() {
        BoxTreeBuilder.Result r = build("\tSection:", "\t\tBlock:", "\t\t\tId: b", "\t\t\tParagraph: Uno[^a].",
                "\t\t\tFootnote:", "\t\t\t\tId: a", "\t\t\t\tParagraph: Nota.", "\t\tParagraph: Dos[^c] y tres[^d].",
                "\t\tFootnote:", "\t\t\tId: d", "\t\t\tParagraph: Cuarta.", "\t\tFootnote:", "\t\t\tId: c",
                "\t\t\tParagraph: Tercera.");
        Section section = r.sections().get(0);
        Box area = section.footnotes();
        assertSame(Element.FOOTNOTES, area.element());
        assertEquals(Position.FOOTNOTES, area.style().position());
        // A child of the section, after its content, out of its flow; the notes are its children in document order.
        assertSame(area, section.box().children().get(2));
        assertEquals(java.util.Arrays.asList("b", null), section.box().inFlowChildren().stream().map(Box::id).toList());
        assertEquals(List.of("a", "d", "c"), area.children().stream().map(Box::id).toList());
        assertTrue(section.hasFootnotes());
        // The block does not see its note.
        Box block = section.box().children().get(0);
        assertEquals(1, block.children().size());
        for (Box note : area.children()) {
            assertSame(area, note.parent());
            assertSame(Element.FOOTNOTE, note.element());
            assertEquals(Element.Role.NOTE, note.role());
        }
    }

    /** The calls are numbered in reading order, and each note starts with its number as a superscript and a space. */
    @Test
    void callsAreNumberedInOrderAndLabelTheirNotes() {
        BoxTreeBuilder.Result r = build("\tSection:", "\t\tFootnote:", "\t\t\tId: b", "\t\t\tParagraph: **Nota** b.",
                "\t\tParagraph: Uno[^a] y dos[^b].", "\t\tFootnote:", "\t\t\tId: a", "\t\t\tMarkdown:",
                "\t\t\t\tContent >>", "\t\t\t\t\tNota a.", "", "\t\t\t\t\tOtro párrafo.");
        Section section = r.sections().get(0);
        Box text = section.box().children().get(0);
        assertEquals("Uno1 y dos2.", text.text());
        assertEquals(List.of(run("Uno"), call("1", "a"), run(" y dos"), call("2", "b"), run(".")),
                text.formattedRuns());
        Box b = section.footnotes().children().get(0);
        Box a = section.footnotes().children().get(1);
        // The number goes before the first text of the note: in b, a Paragraph with runs; in a, the first paragraph
        // of its Markdown.
        Box bText = b.children().get(0);
        assertEquals("2 Nota b.", bText.text());
        assertEquals(List.of(label("2"), run(" "), run("Nota", "b"), run(" b.")), bText.formattedRuns());
        Box aText = a.children().get(0).children().get(0);
        assertEquals("1 Nota a.", aText.text());
        assertEquals(List.of(label("1"), run(" "), run("Nota a.")), aText.formattedRuns());
        assertNull(a.children().get(0).children().get(1).formattedRuns());
    }

    /** {@code Footnote numbering: restart} starts at 1 again, with the {@code Footnote number format} of the section. */
    @Test
    void sectionsContinueOrRestartTheNumbering() {
        BoxTreeBuilder.Result r = build("\tSection:", "\t\tParagraph: Uno[^a] dos[^b].",
                "\t\tFootnote:", "\t\t\tId: a", "\t\t\tParagraph: A.", "\t\tFootnote:", "\t\t\tId: b", "\t\t\tParagraph: B.",
                "\tSection:", "\t\tFootnote number format: lower-roman", "\t\tParagraph: Tres[^c].",
                "\t\tFootnote:", "\t\t\tId: c", "\t\t\tParagraph: C.",
                "\tSection:", "\t\tFootnote numbering: restart", "\t\tFootnote number format: upper-alpha",
                "\t\tParagraph: Cuatro[^d].", "\t\tFootnote:", "\t\t\tId: d", "\t\t\tParagraph: D.");
        assertEquals("Uno1 dos2.", r.sections().get(0).box().children().get(0).text());
        assertEquals(Section.FootnoteNumbering.CONTINUE, r.sections().get(1).footnoteNumbering());
        assertEquals("Tresiii.", r.sections().get(1).box().children().get(0).text());
        assertEquals("iii C.", r.sections().get(1).footnotes().children().get(0).children().get(0).text());
        assertEquals(Section.FootnoteNumbering.RESTART, r.sections().get(2).footnoteNumbering());
        assertEquals("CuatroA.", r.sections().get(2).box().children().get(0).text());
        assertEquals("A D.", r.sections().get(2).footnotes().children().get(0).children().get(0).text());
    }

    /**
     * The area has the default rule and spacing, which {@code For element: Footnote area} changes, and the notes inherit
     * its typography; {@code For element: Footnote} styles each note.
     */
    @Test
    void theAreaHasADefaultLookThatTheRulesChange() {
        Section plain = build("\tSection:", "\t\tParagraph: Uno[^a].", "\t\tFootnote:", "\t\t\tId: a",
                "\t\t\tParagraph: A.").sections().get(0);
        Style s = plain.footnotes().style();
        assertEquals(Footnotes.DEFAULT_RULE, s.box().border().top().resolve(0).orElseThrow());
        assertEquals(Footnotes.DEFAULT_GAP, s.box().margin().top().resolve(0).orElseThrow());
        assertEquals(Footnotes.DEFAULT_PADDING, s.box().padding().top().resolve(0).orElseThrow());
        assertEquals(0, s.box().border().bottom().resolve(0).orElseThrow());
        assertEquals(10, plain.footnotes().children().get(0).style().typography().fontSize());

        Section styled = build("\t\tFor element: Footnote area", "\t\t\tTypography:", "\t\t\t\tSize: 8",
                "\t\t\tBox:", "\t\t\t\tBorder: 0", "\t\t\t\tMargin: 20 0 0 0", "\t\tFor element: Footnote",
                "\t\t\tBox:", "\t\t\t\tMargin: 0 0 3 0",
                "\tSection:", "\t\tParagraph: Uno[^a].", "\t\tFootnote:", "\t\t\tId: a",
                "\t\t\tParagraph: A.").sections().get(0);
        Style t = styled.footnotes().style();
        assertEquals(0, t.box().border().top().resolve(0).orElseThrow());
        assertEquals(20, t.box().margin().top().resolve(0).orElseThrow());
        Box note = styled.footnotes().children().get(0);
        assertEquals(8, note.style().typography().fontSize());
        assertEquals(8, note.children().get(0).style().typography().fontSize());
        assertEquals(3, note.style().box().margin().bottom().resolve(0).orElseThrow());
    }

    /** A note may be called several times: the later calls repeat its number, and the note is one. */
    @Test
    void aSecondCallRepeatsTheNumberOfItsNote() {
        Section section = build("\tSection:", "\t\tParagraph: Uno[^a], dos[^b] y otra vez[^a].",
                "\t\tParagraph: Tres[^c] y la primera[^a].",
                "\t\tFootnote:", "\t\t\tId: a", "\t\t\tParagraph: A.", "\t\tFootnote:", "\t\t\tId: b",
                "\t\t\tParagraph: B.", "\t\tFootnote:", "\t\t\tId: c", "\t\t\tParagraph: C.").sections().get(0);
        List<Box> texts = section.box().inFlowChildren();
        assertEquals(List.of(run("Uno"), call("1", "a"), run(", dos"), call("2", "b"), run(" y otra vez"),
                call("1", "a"), run(".")), texts.get(0).runs());
        assertEquals(List.of(run("Tres"), call("3", "c"), run(" y la primera"), call("1", "a"), run(".")),
                texts.get(1).runs());
        assertEquals(List.of(label("1"), run(" "), run("A.")), section.footnotes().children().get(0).children().get(0).runs());
        assertEquals(3, section.footnotes().children().size());
    }

    /** {@code Footnote number format: symbols}: the asterisk, the dagger, the double dagger, the section sign, and doubled. */
    @Test
    void theNotesCanBeMarkedWithSymbols() {
        assertEquals(List.of("*", "\u2020", "\u2021", "\u00a7", "**", "\u2020\u2020", "\u2021\u2021", "\u00a7\u00a7", "***"),
                java.util.stream.IntStream.rangeClosed(1, 9)
                        .mapToObj(dev.stxt.pdf.model.FootnoteNumberFormat.SYMBOLS::format).toList());
        assertEquals("iv", dev.stxt.pdf.model.FootnoteNumberFormat.LOWER_ROMAN.format(4));
        Section section = build("\tSection:", "\t\tFootnote number format: symbols",
                "\t\tParagraph: Precio[^iva] y plazo[^plazo].",
                "\t\tFootnote:", "\t\t\tId: iva", "\t\t\tParagraph: IVA no incluido.",
                "\t\tFootnote:", "\t\t\tId: plazo", "\t\t\tParagraph: En días.").sections().get(0);
        assertEquals(List.of(run("Precio"), call("*", "iva"), run(" y plazo"), call("\u2020", "plazo"), run(".")),
                section.box().inFlowChildren().get(0).runs());
        assertEquals(List.of(label("\u2020"), run(" "), run("En días.")),
                section.footnotes().children().get(1).children().get(0).runs());
    }

    @Test
    void whatIsNotAllowed() {
        assertEquals(ErrorCode.FOOTNOTE_UNKNOWN, error("\tSection:", "\t\tParagraph: Uno[^a]."));
        assertEquals(ErrorCode.FOOTNOTE_NOT_CALLED, error("\tSection:", "\t\tParagraph: Uno.", "\t\tFootnote:",
                "\t\t\tId: a", "\t\t\tParagraph: A."));
        // A second call to a note in another section than the first one.
        assertEquals(ErrorCode.FOOTNOTE_UNKNOWN, error("\tSection:", "\t\tParagraph: Uno[^a].",
                "\t\tFootnote:", "\t\t\tId: a", "\t\t\tParagraph: A.", "\tSection:", "\t\tParagraph: Dos[^a]."));
        assertEquals(ErrorCode.FOOTNOTE_CALL_NOT_ALLOWED, error("\tSection:", "\t\tParagraph: Uno[^a].",
                "\t\tFootnote:", "\t\t\tId: a", "\t\t\tParagraph: A[^b].", "\t\tFootnote:", "\t\t\tId: b",
                "\t\t\tParagraph: B."));
        // The schema already keeps a Footnote out of a Footnote and of a Page overlay; inside a Block of theirs, the model.
        assertEquals(ErrorCode.FOOTNOTE_NOT_ALLOWED, error("\tSection:", "\t\tParagraph: Uno[^a].",
                "\t\tFootnote:", "\t\t\tId: a", "\t\t\tParagraph: A.", "\t\t\tBlock:", "\t\t\t\tFootnote:",
                "\t\t\t\t\tId: b", "\t\t\t\t\tParagraph: B."));
        assertEquals(ErrorCode.FOOTNOTE_CONTENT, error("\tSection:", "\t\tParagraph: Uno[^a].",
                "\t\tFootnote:", "\t\t\tId: a", "\t\t\tTable:", "\t\t\t\tColumn sizes: *", "\t\t\t\tBody rows:",
                "\t\t\t\t\tRow:", "\t\t\t\t\t\tCell: A."));
    }

    private static ErrorCode error(String... lines) {
        return assertThrows(InvalidModelException.class, () -> build(lines)).errorCode();
    }
}
