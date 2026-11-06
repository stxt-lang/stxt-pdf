package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Several separate styles documents (reference/common/styles.md): they are applied in the order they are given, so at
 * each priority level the last one wins, and the document wins over them all; an error says which one its line is in.
 */
class SeveralStylesTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = Docs.header(595, 842);
    private static final String BASE = Docs.STYLES;
    /** The first line of a styles document that does not bring the base typography. */
    private static final String STYLES = "Styles (dev.stxt.pdf):\n";

    /** The document (header and these lines) with these separate styles documents, in this order. */
    private static Box root(List<String> styles, String... lines) {
        InlineNode document = Docs.read(HEADER, lines);
        List<InlineNode> read = styles.stream().map(s -> READER.readStyles(s)).toList();
        return new BoxTreeBuilder().build(document, read).sections().get(0).box();
    }

    private static InvalidModelException error(List<String> styles, String... lines) {
        return assertThrows(InvalidModelException.class, () -> root(styles, lines));
    }

    private static String textRule(String... properties) {
        return "\tFor element: Text\n\t\tTypography:\n\t\t\t" + String.join("\n\t\t\t", properties) + "\n";
    }

    private static String rule(String selector, String... properties) {
        return "\t" + selector + "\n\t\tBox:\n\t\t\t" + String.join("\n\t\t\t", properties) + "\n";
    }

    @Test
    void theLastStylesDocumentWinsPropertyByProperty() {
        Box text = root(List.of(BASE + textRule("Size: 12", "Bold: true"), STYLES + textRule("Size: 14")),
                "\tText: hola").children().get(0);
        assertEquals(14, text.style().typography().fontSize(), 1e-9);
        // What the last one does not declare comes from the one before.
        assertEquals(FontWeight.BOLD, text.style().typography().fontWeight());
        // Each property keeps the line of its rule and its document.
        assertEquals(SourceLine.inStyles(4, 2, true), text.style().line("font-size"));
        assertEquals(SourceLine.inStyles(9, 1, true), text.style().line("font-weight"));
        // The base typography is inherited with its document.
        assertEquals(SourceLine.inStyles(4, 1, true), text.style().line("font"));
    }

    @Test
    void theOrderOfTheStylesDocumentsDecides() {
        Box text = root(List.of(STYLES + textRule("Size: 14"), BASE + textRule("Size: 12", "Bold: true")),
                "\tText: hola").children().get(0);
        assertEquals(12, text.style().typography().fontSize(), 1e-9);
    }

    @Test
    void theDocumentWinsOverEveryStylesDocument() {
        Box text = root(List.of(BASE + textRule("Size: 12", "Bold: true"), STYLES + textRule("Size: 14")),
                "\tStyles:", "\t\tFor element: Text", "\t\t\tTypography:", "\t\t\t\tSize: 16",
                "\tText: hola").children().get(0);
        assertEquals(16, text.style().typography().fontSize(), 1e-9);
        assertEquals(FontWeight.BOLD, text.style().typography().fontWeight());
        assertEquals(SourceLine.of(10), text.style().line("font-size"));
    }

    @Test
    void thePriorityLevelsStayTheSameAcrossTheStylesDocuments() {
        // A For style of the first document beats a For element of the second one.
        Box block = root(List.of(BASE + rule("For style: a", "Padding: 2"),
                STYLES + rule("For element: Block", "Padding: 1", "Margin: 1")),
                "\tBlock:", "\t\tStyle: a").children().get(0);
        assertEquals(Sides.all(Value.pt(2)), block.style().box().padding());
        assertEquals(Sides.all(Value.pt(1)), block.style().box().margin());
    }

    @Test
    void withinALevelTheLastStylesDocumentWins() {
        String first = rule("For style: a", "Padding: 2", "Margin: 2");
        String second = rule("For style: a", "Padding: 3");
        Box block = root(List.of(BASE + first, STYLES + second), "\tBlock:", "\t\tStyle: a").children().get(0);
        assertEquals(Sides.all(Value.pt(3)), block.style().box().padding());
        assertEquals(Sides.all(Value.pt(2)), block.style().box().margin());
        block = root(List.of(BASE + second, STYLES + first), "\tBlock:", "\t\tStyle: a").children().get(0);
        assertEquals(Sides.all(Value.pt(2)), block.style().box().padding());
    }

    @Test
    void tablePositionRulesGoInTheOrderOfTheStylesDocuments() {
        String[] table = {"\tTable:", "\t\tColumn sizes: *", "\t\tHeader rows:", "\t\t\tRow:", "\t\t\t\tCell: a",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: b"};
        String body = rule("For element: Body cell", "Padding: 3");
        String odd = rule("For element: Odd row cell", "Padding: 7");
        // In one source Odd row cell goes after Body cell; between two, the order of the documents goes first.
        Box cell = root(List.of(BASE + body + odd), table).children().get(0).children().get(1);
        assertEquals(Sides.all(Value.pt(7)), cell.style().box().padding());
        cell = root(List.of(BASE + odd, STYLES + body), table).children().get(0).children().get(1);
        assertEquals(Sides.all(Value.pt(3)), cell.style().box().padding());
        cell = root(List.of(BASE + body, STYLES + odd), table).children().get(0).children().get(1);
        assertEquals(Sides.all(Value.pt(7)), cell.style().box().padding());
    }

    @Test
    void theLastNamedColorWinsAlsoInTheRulesOfTheOthers() {
        String rules = "\tNamed color: brand #12315A\n" + textRule("Color: brand");
        String color = "\tNamed color: brand #A32D2D\n";
        Box text = root(List.of(BASE + rules, STYLES + color), "\tText: hola").children().get(0);
        assertEquals("#A32D2D", text.style().typography().color());
        text = root(List.of(BASE + color, STYLES + rules), "\tText: hola").children().get(0);
        assertEquals("#12315A", text.style().typography().color());
        // And the document wins over them all.
        text = root(List.of(BASE + rules, STYLES + color), "\tStyles:", "\t\tNamed color: brand #0F6E56",
                "\tText: hola").children().get(0);
        assertEquals("#0F6E56", text.style().typography().color());
    }

    @Test
    void aNameOfAnyStylesDocumentCanBeUsedInTheOthers() {
        Box text = root(List.of(BASE + textRule("Color: brand"), STYLES + "\tNamed color: brand #12315A\n"),
                "\tText: hola").children().get(0);
        assertEquals("#12315A", text.style().typography().color());
    }

    @Test
    void theSameRuleInTwoStylesDocumentsIsNotADuplicateButTwiceInOneIs() {
        String padding = rule("For element: Text", "Padding: 1");
        Box text = root(List.of(BASE + padding, STYLES + padding), "\tText: hola").children().get(0);
        assertEquals(Sides.all(Value.pt(1)), text.style().box().padding());

        InvalidModelException e = error(List.of(BASE, STYLES + padding + padding), "\tText: hola");
        assertEquals(ErrorCode.STYLE_DUPLICATE, e.errorCode());
        assertEquals(5, e.line());
        assertEquals(2, e.stylesDocument());
        assertEquals("line 5 of styles 2: Repeated rule: For element: Text (already on line 2)", e.getMessage());

        e = error(List.of(BASE, STYLES + "\tNamed color: brand #12315A\n\tNamed color: brand #A32D2D\n"), "\tText: hola");
        assertEquals(ErrorCode.COLOR_DUPLICATE, e.errorCode());
        assertEquals("line 3 of styles 2: Repeated Named color: brand (already on line 2)", e.getMessage());
    }

    @Test
    void anErrorSaysWhichStylesDocumentItsLineIsIn() {
        String bad = rule("For element: Text", "Padding: mucho");
        InvalidModelException e = error(List.of(BASE + bad, STYLES + textRule("Bold: true")), "\tText: hola");
        assertEquals(8, e.line());
        assertTrue(e.inStyles());
        assertEquals(1, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 8 of styles 1: "), e.getMessage());

        e = error(List.of(BASE, STYLES + bad), "\tText: hola");
        assertEquals(4, e.line());
        assertEquals(2, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 4 of styles 2: "), e.getMessage());
    }

    @Test
    void withASingleStylesDocumentTheMessageDoesNotNumberIt() {
        InvalidModelException e = error(List.of(BASE + rule("For element: Text", "Padding: mucho")), "\tText: hola");
        assertEquals(1, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 8 of the styles: "), e.getMessage());
    }

    @Test
    void anErrorOfTheDocumentIsOfNoStylesDocument() {
        InvalidModelException e = error(List.of(BASE, STYLES + textRule("Bold: true")),
                "\tText: hola", "\t\tBox:", "\t\t\tPadding: mucho");
        assertFalse(e.inStyles());
        assertEquals(0, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 10: "), e.getMessage());
    }

    @Test
    void aMessageNamesTheStylesDocumentOfEachLine() {
        InvalidModelException e = error(List.of(BASE + rule("For element: Block", "Min width: 100"),
                STYLES + rule("For element: Block", "Max width: 50")), "\tBlock:", "\t\tText: hola");
        assertEquals(ErrorCode.MIN_MAX_CONFLICT, e.errorCode());
        assertEquals("line 8 of styles 1: Min width is greater than Max width (line 4 of styles 2)", e.getMessage());
    }

    @Test
    void withoutStylesDocumentsTheDocumentCarriesItsOwn() {
        Box text = root(List.of(), "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:",
                "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 9", "\tText: hola").children().get(0);
        assertEquals(9, text.style().typography().fontSize(), 1e-9);
    }

    @Test
    void theRootOfEveryStylesDocumentMustBeStyles() {
        InlineNode document = Docs.read(HEADER, "\tText: a");
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> new BoxTreeBuilder().build(document, List.of(READER.readStyles(BASE), document)));
        assertEquals(ErrorCode.DOCUMENT_ROOT, e.errorCode());
        assertEquals(2, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 1 of styles 2: "), e.getMessage());
    }

    @Test
    void anErrorOfReadingAStylesDocumentSaysThePlaceItIsReadFor() {
        String unknown = STYLES + "\tFor element: Text\n\t\tColour: red\n";
        DocumentSyntaxException e = assertThrows(DocumentSyntaxException.class, () -> READER.readStyles(unknown, 2));
        assertEquals("NODE_NOT_DEFINED_IN_SCHEMA", e.code());
        assertEquals(2, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 3 of styles 2: "), e.getMessage());
        // The first one, and the one that is read without a place, are not numbered.
        e = assertThrows(DocumentSyntaxException.class, () -> READER.readStyles(unknown, 1));
        assertEquals(1, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 3 of the styles: "), e.getMessage());
        assertEquals(1, assertThrows(DocumentSyntaxException.class, () -> READER.readStyles(unknown)).stylesDocument());

        InvalidModelException root = assertThrows(InvalidModelException.class,
                () -> READER.readStyles(HEADER + "\tSection:\n\t\tText: a", 3));
        assertEquals(ErrorCode.DOCUMENT_ROOT, root.errorCode());
        assertEquals(3, root.stylesDocument());
        assertTrue(root.getMessage().startsWith("line 1 of styles 3: "), root.getMessage());

        assertThrows(IllegalArgumentException.class, () -> READER.readStyles(BASE, 0));
    }

    private static byte[] pdf(Converter converter, String... lines) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter.toPdf(Docs.read(HEADER, lines), out);
        return out.toByteArray();
    }

    @Test
    void eachCallToStylesAddsADocumentAndSplittingOneChangesNothing() throws IOException {
        String bold = textRule("Bold: true", "Size: 14");
        byte[] whole = pdf(Converter.builder().styles(BASE + bold).build(), "\tText: Hola");
        assertArrayEquals(whole, pdf(Converter.builder().styles(BASE).styles(STYLES + bold).build(), "\tText: Hola"));
        assertArrayEquals(whole, pdf(Converter.builder().styles(READER.readStyles(BASE))
                .styles(READER.readStyles(STYLES + bold)).build(), "\tText: Hola"));
        // The order of the calls is the order of the documents.
        byte[] small = pdf(Converter.builder().styles(BASE).styles(STYLES + bold).styles(STYLES + textRule("Size: 8"))
                .build(), "\tText: Hola");
        byte[] large = pdf(Converter.builder().styles(BASE).styles(STYLES + textRule("Size: 8")).styles(STYLES + bold)
                .build(), "\tText: Hola");
        assertArrayEquals(whole, large);
        assertFalse(java.util.Arrays.equals(whole, small));
    }

    @Test
    void theBuilderGivesEachStylesDocumentItsPlace() {
        Converter.Builder builder = Converter.builder().styles(BASE);
        StxtPdfException e = assertThrows(StxtPdfException.class,
                () -> builder.styles(STYLES + "\tFor element: Text\n\t\tColour: red\n"));
        assertEquals(2, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 3 of styles 2: "), e.getMessage());
        // The document that failed was not added: the next one takes its place.
        e = assertThrows(StxtPdfException.class, () -> builder.styles(STYLES + "\tText: a\n"));
        assertEquals(2, e.stylesDocument());
    }

    @Test
    void anErrorSeenWhenConvertingSaysWhichStylesDocument() {
        Converter converter = Converter.builder().styles(BASE).styles(STYLES + textRule("Font: Nada")).build();
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> converter.toPdf(Docs.read(HEADER, "\tText: hola"), new ByteArrayOutputStream()));
        assertEquals(ErrorCode.FONT_UNKNOWN, e.errorCode());
        assertEquals(4, e.line());
        assertEquals(2, e.stylesDocument());
        assertTrue(e.getMessage().startsWith("line 4 of styles 2: "), e.getMessage());
    }
}
