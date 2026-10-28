package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.exceptions.ParseException;
import dev.stxt.runtime.STXT;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.stxt.DocumentReader;
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
        InlineNode root = new DocumentReader().read(doc);
        return new BoxTreeBuilder().build(root);
    }

    private static String document(String... boxes) {
        return "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n"
                + String.join("\n", boxes);
    }

    @Test
    void textIsAnElementBetweenBoxes() {
        BoxTreeBuilder.Result r = build(document(
                "\tBlock:", "\t\tId: parent", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\t\tText: antes", "\t\tBlock:", "\t\t\tId: child"));
        Box parent = r.root().children.get(0);
        assertEquals(2, parent.children.size());
        assertFalse(parent.children.get(0).anonymous);
        assertEquals(Style.Layout.INLINE, parent.children.get(0).style.layout);
        assertEquals("antes", parent.children.get(0).text);
        assertEquals("child", parent.children.get(1).id);
    }

    @Test
    void elementsGiveLayoutAndPosition() {
        Box root = build(document(
                "\tText: hola", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\tFlex:", "\tGrid:", "\tRelative:", "\tAbsolute:", "\tFixed:")).root();
        assertEquals(List.of(Style.Layout.INLINE, Style.Layout.FLEX, Style.Layout.GRID, Style.Layout.BLOCK,
                Style.Layout.BLOCK, Style.Layout.BLOCK), root.children.stream().map(b -> b.style.layout).toList());
        assertEquals(List.of(Style.Position.STATIC, Style.Position.STATIC, Style.Position.STATIC,
                Style.Position.RELATIVE, Style.Position.ABSOLUTE, Style.Position.FIXED),
                root.children.stream().map(b -> b.style.position).toList());
        assertEquals("hola", root.children.get(0).text);
    }

    @Test
    void textInAContentBlock() {
        Box b = build(document("\tText:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\t\tContent >>", "\t\t\tuno", "\t\t\tdos")).root().children.get(0);
        assertEquals("uno\ndos", b.text);
        assertEquals(11, b.textLine);
        assertTrue(b.textBlock);
    }

    @Test
    void itemAndAreaPropertiesGoToTheirElement() {
        Box root = build(document(
                "\tFlex:", "\t\tItem:", "\t\t\tGrow: 2", "\t\t\tLayer: 1", "\t\t\tBlock:", "\t\t\t\tId: a", "\tGrid:", "\t\tColumns: 1fr 1fr", "\t\tArea: 1 2", "\t\t\tVertical: center", "\t\t\tBlock:", "\t\t\t\tId: b", "\t\tArea:", "\t\t\tBlock:", "\t\t\t\tId: c")).root();
        Box a = root.children.get(0).children.get(0);
        assertEquals("a", a.id);
        assertEquals(2, a.style.itemGrow, 1e-9);
        assertEquals(1, a.style.layer);
        Box b = root.children.get(1).children.get(0);
        assertEquals(new Cell(1, 2, 1, 1), b.style.itemArea);
        assertEquals(Style.Alignment.CENTER, b.style.itemVertical);
        assertEquals(15, b.style.line("item.area"));
        assertEquals(Cell.AUTO, root.children.get(1).children.get(1).style.itemArea);
    }

    @Test
    void onlyTextAndFragmentationPropertiesAreInherited() {
        Box child = build(document(
                "\tBlock:", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tWeight: bold", "\t\t\tStyle: italic",
                "\t\t\tSize: 14", "\t\t\tWidows: 3", "\t\tBox:", "\t\t\tPadding: 10",
                "\t\tText: x")).root().children.get(0).children.get(0);
        assertEquals(new FontFace("IBM Plex Sans", FontWeight.BOLD, FontStyle.ITALIC),
                child.style.fontFace());
        assertEquals(14, child.style.fontSize, 1e-9);
        assertEquals(3, child.style.widows);
        assertEquals(16.8, child.style.usedLineHeight(), 1e-9);
        assertEquals(Sides.all(Value.pt(0)), child.style.padding); // Padding no se hereda
    }

    @Test
    void rulesTheSchemaCannotExpress() {
        InvalidModelException single = assertThrows(InvalidModelException.class, () -> build(document(
                "\tGrid:", "\t\tArea:", "\t\t\tBlock:", "\t\t\tText: a")));
        assertEquals(ErrorCode.SINGLE_ELEMENT, single.code());
        assertEquals(10, single.line());
        InvalidModelException empty = assertThrows(InvalidModelException.class, () -> build(document(
                "\tFlex:", "\t\tItem:", "\t\t\tGrow: 1")));
        assertEquals(ErrorCode.SINGLE_ELEMENT, empty.code());
        assertEquals(8, empty.line());
        InvalidModelException noText = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText:", "\t\tTypography:", "\t\t\tSize: 10")));
        assertEquals(ErrorCode.TEXT_CONTENT, noText.code());
        InvalidModelException twice = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: a", "\t\tContent >>", "\t\t\tb")));
        assertEquals(ErrorCode.TEXT_CONTENT, twice.code());
        assertEquals(8, twice.line());
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tGrid:", "\t\tAreas >>", "\t\t\ta a", "\t\t\ta b", "\t\t\tb b")));
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tBlock:", "\t\tTypography:", "\t\t\tWidows: 0")));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tBlock:", "\t\tBox:", "\t\t\tWidth: 12,5")));
        // La línea es la de la propiedad, dentro del grupo, no la del grupo.
        assertEquals(9, e.line());
    }

    /** Sin pasar por DocumentReader, el builder valida contra el schema y da sus mismos errores. */
    @Test
    void unvalidatedDocumentsAreValidatedAgainstTheSchema() {
        InlineNode misplaced = (InlineNode) STXT.rawParser().parse(document(
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10", "\t\tOffset: 5")).get(0);
        ParseException e = assertThrows(ParseException.class, () -> new BoxTreeBuilder().build(misplaced));
        assertEquals("CHILD_NOT_DECLARED", e.getCode());
        assertEquals(10, e.getLine());

        InlineNode noMedia = (InlineNode) STXT.rawParser().parse(
                "Document (dev.stxt.boxes):\n\tBlock:\n\t\tId: a").get(0);
        assertEquals("TOO_FEW_CHILDREN",
                assertThrows(ParseException.class, () -> new BoxTreeBuilder().build(noMedia)).getCode());

        // Sin el namespace no se validaría nada: es un error de raíz.
        InlineNode noNamespace = (InlineNode) STXT.rawParser().parse(
                "Document:\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842").get(0);
        assertEquals(ErrorCode.DOCUMENT_ROOT,
                assertThrows(InvalidModelException.class, () -> new BoxTreeBuilder().build(noNamespace)).code());
    }

    @Test
    void anEmptyDocumentIsValid() {
        assertTrue(build(document()).root().children.isEmpty());
    }

    @Test
    void textNeedsAFont() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: hola", "\t\tTypography:", "\t\t\tSize: 12")));
        assertEquals(7, e.line());
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, e.code());
        assertEquals("línea 7: El texto necesita Font: declárala en el Typography del Text, de un antecesor "
                + "o de Styles (Element: Document)", e.getMessage());
        // También el texto de un ítem, sin importar que un hermano de su contenedor la declare.
        InvalidModelException item = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: a", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans", "\t\t\tSize: 10",
                "\tFlex:", "\t\tItem:", "\t\t\tText: b")));
        assertEquals(13, item.line());
        // Un elemento sin texto no la necesita.
        build(document("\tBlock:", "\t\tBox:", "\t\t\tHeight: 10"));
    }

    @Test
    void textNeedsAFontSize() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: hola", "\t\tTypography:", "\t\t\tFont: IBM Plex Sans")));
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT_SIZE, e.code());
        assertEquals(7, e.line());
    }

    @Test
    void fontPropertiesOfTheDocumentAreInherited() {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 12\n\t\t\t\tColor: #333\n\t\t\t\tLine height: 20\n"
                + "\tBlock:\n\t\tTypography:\n\t\t\tWeight: bold\n\t\tText: x";
        Box text = build(doc).root().children.get(0).children.get(0);
        assertEquals(new FontFace("IBM Plex Sans", FontWeight.BOLD, FontStyle.NORMAL), text.style.fontFace());
        assertEquals(12, text.style.fontSize, 1e-9);
        // Document admite todo Typography, no solo la fuente.
        assertEquals("#333", text.style.color);
        assertEquals(20, text.style.usedLineHeight(), 1e-9);
        // La línea de la declaración se hereda: los errores de fuentes la señalan.
        assertEquals(9, text.style.line("font"));
    }

    @Test
    void theSpecificationExampleIsValid() throws IOException {
        String doc = Files.readString(Path.of("test", "spec", "report.stxt"));
        BoxTreeBuilder.Result r = build(doc);
        assertEquals(515, r.media().pageArea().width(), 1e-9);
        assertEquals(762, r.media().pageArea().height(), 1e-9);
    }

    /** {@code Paper} y {@code Orientation} (§4.1): el tamaño en vertical, girado con {@code landscape}. */
    @Test
    void namedPaperSizes() {
        String head = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n";
        Media a4 = build(head + "\t\tPaper: A4\n\t\tMargin: 40\n").media();
        assertEquals(new Media(595, 842, new dev.stxt.boxes.model.Insets(40, 40, 40, 40)), a4);
        Media landscape = build(head + "\t\tPaper: A4\n\t\tOrientation: landscape\n").media();
        assertEquals(842, landscape.width(), 1e-9);
        assertEquals(595, landscape.height(), 1e-9);
        Media portrait = build(head + "\t\tPaper: Letter\n\t\tOrientation: portrait\n").media();
        assertEquals(612, portrait.width(), 1e-9);
        assertEquals(792, portrait.height(), 1e-9);
        for (Media.Paper paper : Media.Paper.values()) {
            String name = paper.name().length() == 2 ? paper.name()
                    : paper.name().charAt(0) + paper.name().substring(1).toLowerCase(java.util.Locale.ROOT);
            Media m = build(head + "\t\tPaper: " + name + "\n").media();
            assertTrue(m.width() < m.height(), name);
        }
    }

    @Test
    void paperExcludesWidthAndHeight() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(
                "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tHeight: 842\n\t\tPaper: A4\n"));
        assertEquals(ErrorCode.MEDIA_SIZE, e.code());
        assertEquals(4, e.line());
    }

    /** {@code Rotate} y {@code Rotate origin} (§4.2): el giro y la fracción de la caja; sin origen, el centro. */
    @Test
    void rotateAndItsOrigin() {
        Box root = build(document(
                "\tRelative:", "\t\tRotate: -8.5",
                "\tAbsolute:", "\t\tRotate: 90", "\t\tRotate origin: bottom right",
                "\tFixed:", "\t\tRotate: 360", "\t\tRotate origin: top",
                "\tImage: test/square.png", "\t\tRotate: -360",
                "\tRelative:")).root();
        assertEquals(new dev.stxt.boxes.model.Rotation(-8.5, 0.5, 0.5), root.children.get(0).style.rotation);
        assertEquals(new dev.stxt.boxes.model.Rotation(90, 1, 1), root.children.get(1).style.rotation);
        assertEquals(new dev.stxt.boxes.model.Rotation(360, 0.5, 0), root.children.get(2).style.rotation);
        assertEquals(-360, root.children.get(3).style.rotation.degrees(), 1e-9);
        assertEquals(null, root.children.get(4).style.rotation);
    }
}
