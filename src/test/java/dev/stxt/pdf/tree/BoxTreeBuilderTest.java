package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.runtime.STXT;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTreeBuilderTest {

    private static BoxTreeBuilder.Result build(String doc) {
        InlineNode root = new DocumentReader().read(Docs.sectioned(doc));
        return new BoxTreeBuilder().build(root);
    }

    private static String document(String... boxes) {
        return "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n"
                + String.join("\n", boxes);
    }

    @Test
    void textIsAnElementBetweenBoxes() {
        BoxTreeBuilder.Result r = build(document(
                "\tBlock:", "\t\tId: parent", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\t\tText: antes", "\t\tBlock:", "\t\t\tId: child"));
        Box parent = r.sections().get(0).box().children().get(0);
        assertEquals(2, parent.children().size());
        assertFalse(parent.children().get(0).anonymous());
        assertEquals(Layout.INLINE, parent.children().get(0).style().layout());
        assertEquals("antes", parent.children().get(0).text());
        assertEquals("child", parent.children().get(1).id());
    }

    @Test
    void elementsGiveLayoutAndPosition() {
        Box root = build(document(
                "\tText: hola", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\tStack:", "\tGrid:", "\tFrame:", "\tOverlay:", "\tPage overlay:")).sections().get(0).box();
        assertEquals(List.of(Layout.INLINE, Layout.STACK, Layout.GRID, Layout.BLOCK,
                Layout.BLOCK, Layout.BLOCK), root.children().stream().map(b -> b.style().layout()).toList());
        assertEquals(List.of(Position.STATIC, Position.STATIC, Position.STATIC,
                Position.FRAME, Position.OVERLAY, Position.PAGE_OVERLAY),
                root.children().stream().map(b -> b.style().position()).toList());
        assertEquals("hola", root.children().get(0).text());
    }

    @Test
    void textInAContentBlock() {
        Box b = build(document("\tText:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\t\tContent >>", "\t\t\tuno", "\t\t\tdos")).sections().get(0).box().children().get(0);
        assertEquals("uno\ndos", b.text());
        assertEquals(13, b.textLine());
        assertTrue(b.textBlock());
    }

    @Test
    void itemAndAreaPropertiesGoToTheirElement() {
        Box root = build(document(
                "\tStack:", "\t\tItem:", "\t\t\tGrow: 2", "\t\t\tLayer: 1", "\t\t\tBlock:", "\t\t\t\tId: a", "\tGrid:", "\t\tColumn sizes: * *", "\t\tArea: 1 2", "\t\t\tVertical: center", "\t\t\tBlock:", "\t\t\t\tId: b", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: c")).sections().get(0).box();
        Box a = root.children().get(0).children().get(0);
        assertEquals("a", a.id());
        assertEquals(2, a.style().item().grow(), 1e-9);
        assertEquals(1, a.style().layer());
        Box b = root.children().get(1).children().get(0);
        assertEquals(new Cell(1, 2, 1, 1), b.style().item().area());
        assertEquals(Alignment.CENTER, b.style().item().vertical());
        assertEquals(17, b.style().line("item.area").number());
        assertEquals(Cell.AUTO, root.children().get(1).children().get(1).style().item().area());
    }

    @Test
    void onlyTextAndFragmentationPropertiesAreInherited() {
        Box child = build(document(
                "\tBlock:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tBold: true", "\t\t\tItalic: true",
                "\t\t\tSize: 14", "\t\t\tWidows: 3", "\t\tBox:", "\t\t\tPadding: 10",
                "\t\tText: x")).sections().get(0).box().children().get(0).children().get(0);
        assertEquals(new FontFace("IBM Plex Sans", FontWeight.BOLD, FontStyle.ITALIC),
                child.style().typography().fontFace());
        assertEquals(14, child.style().typography().fontSize(), 1e-9);
        assertEquals(3, child.style().typography().widows());
        assertEquals(16.8, child.style().typography().usedLineHeight(), 1e-9);
        assertEquals(Sides.all(Value.pt(0)), child.style().box().padding()); // Padding is not inherited
    }

    /**
     * {@code Weight} chooses the weight from 100 to 900 and {@code Bold} is another way of writing it: they are the
     * same property, so the later declaration wins, also between a rule and the element
     * (reference/common/typography.md).
     */
    @Test
    void weightAndBoldAreTheSameProperty() {
        Section section = build(document(
                "\tStyles:", "\t\tFor style: light", "\t\t\tTypography:", "\t\t\t\tWeight: 300",
                "\tBlock:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10", "\t\t\tWeight: 600",
                "\t\tText: inherited",
                "\t\tText: bold", "\t\t\tTypography:", "\t\t\t\tBold: true",
                "\t\tText: light", "\t\t\tStyle: light",
                "\t\tText: normal", "\t\t\tStyle: light", "\t\t\tTypography:", "\t\t\t\tBold: false"))
                .sections().get(0);
        List<Box> texts = section.box().children().get(0).children();
        assertEquals(List.of(600, 700, 300, 400),
                texts.stream().map(t -> t.style().typography().fontWeight().value()).toList());
    }

    @Test
    void weightGoesFrom100To900InStepsOf100() {
        for (String value : List.of("0", "50", "350", "1000")) {
            InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                    "\tText: x", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tWeight: " + value)));
            assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.errorCode(), value);
        }
        InvalidModelException conflict = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: x", "\t\tTypography:", "\t\t\tWeight: 700", "\t\t\tBold: true")));
        assertEquals(ErrorCode.WEIGHT_CONFLICT, conflict.errorCode());
    }

    @Test
    void rulesTheSchemaCannotExpress() {
        InvalidModelException single = assertThrows(InvalidModelException.class, () -> build(document(
                "\tGrid:", "\t\tArea:", "\t\t\tBlock:", "\t\t\tText: a")));
        assertEquals(ErrorCode.SINGLE_ELEMENT, single.errorCode());
        assertEquals(12, single.line());
        InvalidModelException empty = assertThrows(InvalidModelException.class, () -> build(document(
                "\tStack:", "\t\tItem:", "\t\t\tGrow: 1")));
        assertEquals(ErrorCode.SINGLE_ELEMENT, empty.errorCode());
        assertEquals(10, empty.line());
        InvalidModelException noText = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText:", "\t\tTypography:", "\t\t\tSize: 10")));
        assertEquals(ErrorCode.TEXT_CONTENT, noText.errorCode());
        InvalidModelException twice = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: a", "\t\tContent >>", "\t\t\tb")));
        assertEquals(ErrorCode.TEXT_CONTENT, twice.errorCode());
        assertEquals(10, twice.line());
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tGrid:", "\t\tAreas >>", "\t\t\ta a", "\t\t\ta b", "\t\t\tb b")));
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tBlock:", "\t\tTypography:", "\t\t\tWidows: 0")));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tBlock:", "\t\tBox:", "\t\t\tWidth: 12,5")));
        // The line is that of the property, inside the group, not that of the group.
        assertEquals(11, e.line());
    }

    /** Without going through DocumentReader, the builder validates against the schema and gives the same errors. */
    @Test
    void unvalidatedDocumentsAreValidatedAgainstTheSchema() {
        InlineNode misplaced = (InlineNode) STXT.rawParser().parse(Docs.sectioned(document(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10", "\t\tOffset: 5"))).get(0);
        DocumentSyntaxException e = assertThrows(DocumentSyntaxException.class, () -> new BoxTreeBuilder().build(misplaced));
        assertEquals("CHILD_NOT_DECLARED", e.code());
        assertEquals(12, e.line());

        InlineNode noPageSetup = (InlineNode) STXT.rawParser().parse(
                "Document (dev.stxt.pdf):\n\tSection:\n\t\tBlock:\n\t\t\tId: a").get(0);
        assertEquals("TOO_FEW_CHILDREN",
                assertThrows(DocumentSyntaxException.class, () -> new BoxTreeBuilder().build(noPageSetup)).code());

        // Without the namespace nothing would be validated: it is a root error.
        InlineNode noNamespace = (InlineNode) STXT.rawParser().parse(
                "Document:\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842").get(0);
        assertEquals(ErrorCode.DOCUMENT_ROOT,
                assertThrows(InvalidModelException.class, () -> new BoxTreeBuilder().build(noNamespace)).errorCode());
    }

    /**
     * A document needs at least one section (reference/components/document.md): loose content is not
     * allowed.
     */
    @Test
    void aDocumentNeedsASection() {
        DocumentSyntaxException none = assertThrows(DocumentSyntaxException.class, () -> build(document()));
        assertEquals("TOO_FEW_CHILDREN", none.code());
        DocumentSyntaxException loose = assertThrows(DocumentSyntaxException.class, () -> new DocumentReader().read(document("\tBlock:")));
        assertEquals("CHILD_NOT_DECLARED", loose.code());
    }

    @Test
    void textNeedsAFont() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: hola", "\t\tTypography:", "\t\t\tSize: 12")));
        assertEquals(9, e.line());
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, e.errorCode());
        assertEquals("line 9: The text needs Font: it is declared in the Typography of the Text, of an ancestor "
                + "or of Styles (For element: Document)", e.getMessage());
        // Also the text of an item, even if a sibling of its container declares it.
        InvalidModelException item = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: a", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\tStack:", "\t\tItem:", "\t\t\tText: b")));
        assertEquals(15, item.line());
        // An element without text does not need it.
        build(document("\tBlock:", "\t\tBox:", "\t\t\tHeight: 10"));
    }

    @Test
    void textNeedsAFontSize() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: hola", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans")));
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT_SIZE, e.errorCode());
        assertEquals(9, e.line());
    }

    @Test
    void fontPropertiesOfTheDocumentAreInherited() {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 12\n\t\t\t\tColor: #333\n\t\t\t\tLine height: 20\n"
                + "\tBlock:\n\t\tTypography:\n\t\t\tBold: true\n\t\tText: x";
        Box text = build(doc).sections().get(0).box().children().get(0).children().get(0);
        assertEquals(new FontFace("IBM Plex Sans", FontWeight.BOLD, FontStyle.NORMAL), text.style().typography().fontFace());
        assertEquals(12, text.style().typography().fontSize(), 1e-9);
        // Document allows all of Typography, not only the font.
        assertEquals("#333", text.style().typography().color());
        assertEquals(20, text.style().typography().usedLineHeight(), 1e-9);
        // The line of the declaration is inherited: font errors point to it.
        assertEquals(10, text.style().line("font").number());
    }

    @Test
    void theSpecificationExampleIsValid() throws IOException {
        String doc = Files.readString(Path.of("test", "spec", "report.stxt"));
        BoxTreeBuilder.Result r = build(doc);
        assertEquals(210 * 72 / 25.4 - 80, r.pageSetup().pageArea().width(), 1e-9);
        assertEquals(297 * 72 / 25.4 - 80, r.pageSetup().pageArea().height(), 1e-9);
    }

    /**
     * {@code Paper} and {@code Orientation} (reference/components/document.md): the size in portrait, rotated with
     * {@code landscape}.
     */
    @Test
    void namedPaperSizes() {
        String head = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n";
        String body = "\tSection:\n\t\tBlock:\n";
        PageSetup a4 = build(head + "\t\tPaper: A4\n\t\tMargin: 40\n" + body).pageSetup();
        // ISO 216 is exact in millimeters: A4 is 210 × 297 mm.
        assertEquals(595.28, a4.width(), 0.01);
        assertEquals(841.89, a4.height(), 0.01);
        assertEquals(new dev.stxt.pdf.model.Insets(40, 40, 40, 40), a4.margin());
        PageSetup landscape = build(head + "\t\tPaper: A4\n\t\tOrientation: landscape\n" + body).pageSetup();
        assertEquals(297 * 72 / 25.4, landscape.width(), 1e-9);
        assertEquals(210 * 72 / 25.4, landscape.height(), 1e-9);
        PageSetup portrait = build(head + "\t\tPaper: Letter\n\t\tOrientation: portrait\n" + body).pageSetup();
        assertEquals(612, portrait.width(), 1e-9);
        assertEquals(792, portrait.height(), 1e-9);
        for (PageSetup.Paper paper : PageSetup.Paper.values()) {
            String name = paper.name().length() == 2 ? paper.name()
                    : paper.name().charAt(0) + paper.name().substring(1).toLowerCase(java.util.Locale.ROOT);
            PageSetup m = build(head + "\t\tPaper: " + name + "\n" + body).pageSetup();
            assertTrue(m.width() < m.height(), name);
        }
    }

    /**
     * Every length allows millimeters (reference/common/values.md): the page, the box, the typography and the tracks.
     * Factors and degrees have no unit.
     */
    @Test
    void lengthsInMillimeters() {
        BoxTreeBuilder.Result r = build("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n"
                + "\t\tWidth: 210mm\n\t\tHeight: 297mm\n\t\tMargin: 20mm 15\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tWidth: 50mm\n\t\t\tMargin: -2mm 0\n\t\t\tCorner radius: 1mm\n"
                + "\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n\t\t\tSize: 3.5mm\n\t\t\tLetter spacing: -0.1mm\n"
                + "\t\tText: x\n"
                + "\tGrid:\n\t\tColumn sizes: 30mm between(20mm, *)\n\t\tGap: 5mm\n");
        assertEquals(595.28, r.pageSetup().width(), 0.01);
        assertEquals(841.89, r.pageSetup().height(), 0.01);
        assertEquals(56.69, r.pageSetup().margin().top(), 0.01);
        assertEquals(15, r.pageSetup().margin().left(), 1e-9);
        Box block = r.sections().get(0).box().children().get(0);
        assertEquals(Value.mm(50), block.style().box().width());
        assertEquals(Value.mm(-2), block.style().box().margin().top());
        assertEquals(9.92, block.style().typography().fontSize(), 0.01);
        assertEquals(-0.28, block.style().typography().letterSpacing(), 0.01);
        Box grid = r.sections().get(0).box().children().get(1);
        assertEquals(List.of(Value.mm(30), new Value.Between(Value.mm(20), new Value.Fraction(1))),
                grid.style().grid().columnSizes());

        for (String wrong : List.of("\t\t\tWidth: 50 mm", "\t\t\tWidth: 5cm", "\t\t\tPadding: 2pt")) {
            InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                    "\tBlock:", "\t\tBox:", wrong)));
            assertEquals(ErrorCode.VALUE_SYNTAX, e.errorCode(), wrong);
        }
        InvalidModelException grow = assertThrows(InvalidModelException.class, () -> build(document(
                "\tStack:", "\t\tItem:", "\t\t\tGrow: 2mm", "\t\t\tBlock:")));
        assertEquals(ErrorCode.VALUE_SYNTAX, grow.errorCode());
        // Rotate is a NUMBER in the schema: stxt-core rejects it before the engine reads it.
        assertThrows(DocumentSyntaxException.class, () -> build(document("\tFrame:", "\t\tRotate: 10mm")));
    }

    @Test
    void paperExcludesWidthAndHeight() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(
                "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tHeight: 842\n\t\tPaper: A4\n\tSection:\n\t\tBlock:\n"));
        assertEquals(ErrorCode.PAGE_SIZE, e.errorCode());
        assertEquals(5, e.line());
    }

    /**
     * {@code Rotate} and {@code Rotate origin} (reference/common/layers-and-rotation.md): the rotation and its origin,
     * with keywords or with lengths; without an origin, the center.
     */
    @Test
    void rotateAndItsOrigin() {
        Box root = build(document(
                "\tFrame:", "\t\tRotate: -8.5",
                "\tOverlay:", "\t\tRotate: 90", "\t\tRotate origin: bottom right",
                "\tPage overlay:", "\t\tRotate: 359.5", "\t\tRotate origin: top",
                "\tImage: test/square.png", "\t\tRotate: -359",
                "\tFrame:",
                "\tFrame:", "\t\tRotate: 30", "\t\tRotate origin: 10 25%")).sections().get(0).box();
        assertEquals(new dev.stxt.pdf.model.Rotation(-8.5, 0.5, 0.5), root.children().get(0).style().rotation());
        assertEquals(new dev.stxt.pdf.model.Rotation(90, 1, 1), root.children().get(1).style().rotation());
        assertEquals(new dev.stxt.pdf.model.Rotation(359.5, 0.5, 0), root.children().get(2).style().rotation());
        assertEquals(-359, root.children().get(3).style().rotation().degrees(), 1e-9);
        assertEquals(null, root.children().get(4).style().rotation());
        assertEquals(new dev.stxt.pdf.model.Rotation(30, dev.stxt.pdf.model.Value.pt(10),
                new dev.stxt.pdf.model.Value.Percentage(25)), root.children().get(5).style().rotation());
    }

    /**
     * What has no effect because of its place is only an error if the element itself writes it with an impossible
     * effect (reference/common/errors.md): {@code Isolate margins: false} in an item is the default value, a {@code
     * Break} that comes from a rule is not checked, and in the columns of a {@code Columns} breaks have an effect even
     * inside an {@code Overlay}.
     */
    @Test
    void breaksAndIsolateMarginsWithoutEffectOnlyFailWhenTheElementWritesThem() {
        build(document(
                "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans",
                "\t\t\t\tSize: 10", "\t\tFor element: Paragraph", "\t\t\tBreak:", "\t\t\t\tKeep with next: true",
                "\tStack:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tIsolate margins: false",
                "\tOverlay:", "\t\tParagraph: Sello",
                "\t\tColumns:", "\t\t\tCount: 2", "\t\t\tBlock:", "\t\t\t\tBreak:", "\t\t\t\t\tAfter: column",
                "\t\t\tBlock:"));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tPage overlay:", "\t\tBlock:", "\t\t\tBreak:", "\t\t\t\tBefore: page", "\tBlock:")));
        assertEquals(ErrorCode.BREAK_IN_UNBREAKABLE, e.errorCode());
    }
}
