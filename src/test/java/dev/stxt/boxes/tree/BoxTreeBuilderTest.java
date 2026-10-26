package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
        return "Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 595\n\t\tMargin: 40\n"
                + String.join("\n", boxes);
    }

    @Test
    void textMixedWithBoxesCreatesAnonymousBox() {
        BoxTreeBuilder.Result r = build(document(
                "\tBox:",
                "\t\tId: parent",
                "\t\tFont: IBM Plex Sans",
                "\t\tFont size: 10",
                "\t\tText: antes",
                "\t\tBox:",
                "\t\t\tId: child"));
        Box parent = r.root().children.get(0);
        assertEquals(2, parent.children.size());
        assertTrue(parent.children.get(0).anonymous);
        assertEquals(Style.Layout.INLINE, parent.children.get(0).style.layout);
        assertEquals("antes", parent.children.get(0).text);
        assertEquals("child", parent.children.get(1).id);
    }

    @Test
    void blockWithOnlyTextUsesInlineLayout() {
        Box b = build(document("\tBox:", "\t\tFont: IBM Plex Sans", "\t\tFont size: 10", "\t\tText: hola")).root().children.get(0);
        assertEquals(Style.Layout.INLINE, b.style.layout);
        assertFalse(b.anonymous);
        assertEquals("hola", b.text);
    }

    @Test
    void onlyTextAndFragmentationPropertiesAreInherited() {
        Box child = build(document(
                "\tBox:",
                "\t\tFont: IBM Plex Sans",
                "\t\tFont weight: bold",
                "\t\tFont style: italic",
                "\t\tFont size: 14",
                "\t\tWidows: 3",
                "\t\tPadding: 10",
                "\t\tBox:",
                "\t\t\tText: x")).root().children.get(0).children.get(0);
        assertEquals(new FontFace("IBM Plex Sans", Style.FontWeight.BOLD, Style.FontStyle.ITALIC),
                child.style.fontFace());
        assertEquals(14, child.style.fontSize, 1e-9);
        assertEquals(3, child.style.widows);
        assertEquals(16.8, child.style.usedLineHeight(), 1e-9);
        assertEquals(Sides.all(Value.pt(0)), child.style.padding); // Padding no se hereda
    }

    @Test
    void rulesTheTemplateCannotExpress() {
        assertThrows(InvalidModelException.class, () -> build(
                "Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: paged\n\t\tWidth: 595\n\tBox:\n\t\tHeight: 10"));
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tFlex:", "\t\t\tGap: 10")));
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tOffset: 10")));
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tAreas >>", "\t\t\t\ta a", "\t\t\t\ta b", "\t\t\t\tb b")));
        assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tWidows: 0")));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tWidth: 12,5")));
        assertEquals(7, e.line());
    }

    @Test
    void textNeedsAFont() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tFont size: 12", "\t\tText: hola")));
        assertEquals(8, e.line());
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, e.code());
        assertEquals("línea 8: El texto necesita Font: declárala en la caja, en una antecesora o en Document",
                e.getMessage());
        // También el texto que va a una caja anónima, y sin importar que una hermana la declare.
        InvalidModelException anonymous = assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tFont: IBM Plex Sans", "\t\tFont size: 10", "\t\tText: a",
                "\tBox:", "\t\tLayout: flex", "\t\tText: b")));
        assertEquals(12, anonymous.line());
        // Una caja sin texto no la necesita.
        build(document("\tBox:", "\t\tHeight: 10"));
    }

    @Test
    void textNeedsAFontSize() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> build(document(
                "\tBox:", "\t\tFont: IBM Plex Sans", "\t\tText: hola")));
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT_SIZE, e.code());
        assertEquals(8, e.line());
    }

    @Test
    void fontPropertiesOfTheDocumentAreInherited() {
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 595\n"
                + "\tFont: IBM Plex Sans\n\tFont size: 12\n\tBox:\n\t\tFont weight: bold\n\t\tBox:\n\t\t\tText: x";
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
