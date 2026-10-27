package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTreeBuilderTest {

    private static BoxTreeBuilder.Result build(String doc) {
        InlineNode root = new DocumentReader().read(doc);
        return new BoxTreeBuilder().build(root);
    }

    private static String document(String... boxes) {
        return "Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 595\n\t\tMargin: 40\n"
                + String.join("\n", boxes);
    }

    @Test
    void textIsAnElementBetweenBoxes() {
        BoxTreeBuilder.Result r = build(document(
                "\tBlock:", "\t\tId: parent", "\t\tFont: IBM Plex Sans", "\t\tFont size: 10",
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
                "\tText: hola", "\t\tFont: IBM Plex Sans", "\t\tFont size: 10",
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
        Box b = build(document("\tText:", "\t\tFont: IBM Plex Sans", "\t\tFont size: 10",
                "\t\tContent >>", "\t\t\tuno", "\t\t\tdos")).root().children.get(0);
        assertEquals("uno\ndos", b.text);
        assertEquals(9, b.textLine);
        assertTrue(b.textBlock);
    }

    @Test
    void itemAndCellPropertiesGoToTheirElement() {
        Box root = build(document(
                "\tFlex:", "\t\tItem:", "\t\t\tGrow: 2", "\t\t\tLayer: 1", "\t\t\tBlock:", "\t\t\t\tId: a",
                "\tGrid:", "\t\tColumns: 1fr 1fr", "\t\tCell: 1 2", "\t\t\tVertical: center", "\t\t\tBlock:", "\t\t\t\tId: b",
                "\t\tCell:", "\t\t\tBlock:", "\t\t\t\tId: c")).root();
        Box a = root.children.get(0).children.get(0);
        assertEquals("a", a.id);
        assertEquals(2, a.style.itemGrow, 1e-9);
        assertEquals(1, a.style.layer);
        Box b = root.children.get(1).children.get(0);
        assertEquals("1 2", b.style.itemCell);
        assertEquals(Style.Alignment.CENTER, b.style.itemVertical);
        assertEquals(14, b.style.line("item.cell"));
        assertNull(root.children.get(1).children.get(1).style.itemCell);
    }

    @Test
    void onlyTextAndFragmentationPropertiesAreInherited() {
        Box child = build(document(
                "\tBlock:", "\t\tFont: IBM Plex Sans", "\t\tFont weight: bold", "\t\tFont style: italic",
                "\t\tFont size: 14", "\t\tWidows: 3", "\t\tPadding: 10",
                "\t\tText: x")).root().children.get(0).children.get(0);
        assertEquals(new FontFace("IBM Plex Sans", Style.FontWeight.BOLD, Style.FontStyle.ITALIC),
                child.style.fontFace());
        assertEquals(14, child.style.fontSize, 1e-9);
        assertEquals(3, child.style.widows);
        assertEquals(16.8, child.style.usedLineHeight(), 1e-9);
        assertEquals(Sides.all(Value.pt(0)), child.style.padding); // Padding no se hereda
    }

    @Test
    void rulesTheSchemaCannotExpress() {
        assertThrows(InvalidModelException.class, () -> build(
                "Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: paged\n\t\tWidth: 595\n\tBlock:\n\t\tHeight: 10"));
        InvalidModelException single = assertThrows(InvalidModelException.class, () -> build(document(
                "\tGrid:", "\t\tCell:", "\t\t\tBlock:", "\t\t\tText: a")));
        assertEquals(ErrorCode.SINGLE_ELEMENT, single.code());
        assertEquals(9, single.line());
        InvalidModelException empty = assertThrows(InvalidModelException.class, () -> build(document(
                "\tFlex:", "\t\tItem:", "\t\t\tGrow: 1")));
        assertEquals(ErrorCode.SINGLE_ELEMENT, empty.code());
        assertEquals(7, empty.line());
        InvalidModelException noText = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText:", "\t\tFont size: 10")));
        assertEquals(ErrorCode.TEXT_CONTENT, noText.code());
        InvalidModelException twice = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: a", "\t\tContent >>", "\t\t\tb")));
        assertEquals(ErrorCode.TEXT_CONTENT, twice.code());
        assertEquals(7, twice.line());
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tGrid:", "\t\tAreas >>", "\t\t\ta a", "\t\t\ta b", "\t\t\tb b")));
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tBlock:", "\t\tWidows: 0")));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tBlock:", "\t\tWidth: 12,5")));
        assertEquals(7, e.line());
    }

    @Test
    void anEmptyDocumentIsValid() {
        assertTrue(build(document()).root().children.isEmpty());
    }

    @Test
    void textNeedsAFont() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: hola", "\t\tFont size: 12")));
        assertEquals(6, e.line());
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, e.code());
        assertEquals("línea 6: El texto necesita Font: declárala en el Text, en un antecesor o en Document",
                e.getMessage());
        // También el texto de un ítem, sin importar que un hermano de su contenedor la declare.
        InvalidModelException item = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: a", "\t\tFont: IBM Plex Sans", "\t\tFont size: 10",
                "\tFlex:", "\t\tItem:", "\t\t\tText: b")));
        assertEquals(11, item.line());
        // Un elemento sin texto no la necesita.
        build(document("\tBlock:", "\t\tHeight: 10"));
    }

    @Test
    void textNeedsAFontSize() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tText: hola", "\t\tFont: IBM Plex Sans")));
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT_SIZE, e.code());
        assertEquals(6, e.line());
    }

    @Test
    void fontPropertiesOfTheDocumentAreInherited() {
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 595\n"
                + "\tFont: IBM Plex Sans\n\tFont size: 12\n\tBlock:\n\t\tFont weight: bold\n\t\tText: x";
        Box text = build(doc).root().children.get(0).children.get(0);
        assertEquals(new FontFace("IBM Plex Sans", Style.FontWeight.BOLD, Style.FontStyle.NORMAL), text.style.fontFace());
        assertEquals(12, text.style.fontSize, 1e-9);
        // La línea de la declaración se hereda: los errores de fuentes la señalan.
        assertEquals(5, text.style.line("font"));
    }

    @Test
    void theSpecificationExampleIsValid() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/examples/report.stxt")) {
            String doc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            BoxTreeBuilder.Result r = build(doc);
            assertTrue(r.media().paged());
            assertEquals(515, r.media().pageArea().width(), 1e-9);
            assertEquals(762, r.media().pageArea().height(), 1e-9);
        }
    }
}
