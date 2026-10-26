package dev.stxt.boxes.stxt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinimalStxtReaderTest {

    private final MinimalStxtReader reader = new MinimalStxtReader();

    @Test
    void nodesBlocksAndNamespace() {
        String doc = String.join("\n",
                "# comentario",
                "Document (dev.stxt.boxes):",
                "\tBox:",
                "\t\tId: main",
                "\t\tText >>",
                "\t\t\tPrimera línea: con dos puntos",
                "",
                "\t\t\t\tSangrada",
                "\tBox:",
                "\t\tId: other");
        StxtNode root = reader.read(doc, "test");
        assertEquals("dev.stxt.boxes", root.namespace());
        assertEquals(2, root.children("Box").size());
        StxtNode box = root.children("Box").get(0);
        assertEquals("dev.stxt.boxes", box.namespace());
        assertEquals("main", box.contentOf("Id").orElseThrow());
        assertEquals("Primera línea: con dos puntos\n\n\tSangrada", box.contentOf("Text").orElseThrow());
        assertNull(box.child("Text").orElseThrow().value());
        assertEquals(5, box.child("Text").orElseThrow().line());
    }

    @Test
    void spacesInMultiplesOfFour() {
        StxtNode root = reader.read("A:\n    B: 1\n        C: 2", "p");
        assertEquals("2", root.child("B").orElseThrow().contentOf("C").orElseThrow());
        assertThrows(StxtSyntaxException.class, () -> reader.read("A:\n   B: 1", "p"));
    }

    @Test
    void errorsWithLine() {
        StxtSyntaxException e = assertThrows(StxtSyntaxException.class, () -> reader.read("A:\n\t\tB: 1", "doc.stxt"));
        assertEquals(2, e.line());
        assertThrows(StxtSyntaxException.class, () -> reader.read("A:\n\tsin dos puntos", "p"));
        assertThrows(StxtSyntaxException.class, () -> reader.read("A: 1\nB: 2", "p"));
    }
}
