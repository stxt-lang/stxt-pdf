package dev.stxt.boxes.stxt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StxtCoreReaderTest {

    private final StxtCoreReader reader = new StxtCoreReader();

    private static String doc(String... boxLines) {
        StringBuilder sb = new StringBuilder(String.join("\n",
                "Document (dev.stxt.boxes):",
                "\tMedia:",
                "\t\tType: continuous",
                "\t\tWidth: 595"));
        for (String l : boxLines) {
            sb.append('\n').append(l);
        }
        return sb.toString();
    }

    @Test
    void sameTreeAsMinimalReader() throws IOException {
        MinimalStxtReader minimal = new MinimalStxtReader();
        for (String file : new String[] {
                "src/test/resources/examples/report.stxt",
                "src/test/resources/golden/01-block-flow/input.stxt",
                "src/test/resources/golden/02-flex-grow/input.stxt"}) {
            Path path = Path.of(file);
            assertEquals(minimal.read(path), reader.read(path), file);
        }
    }

    @Test
    void nodesBlocksAndNamespace() {
        StxtNode root = reader.read(doc(
                "\tBox:",
                "\t\tId: main",
                "\t\tText >>",
                "\t\t\tPrimera línea: con dos puntos",
                "",
                "\t\t\t\tSangrada"), "test");
        assertEquals("dev.stxt.boxes", root.namespace());
        StxtNode box = root.child("Box").orElseThrow();
        assertEquals("dev.stxt.boxes", box.namespace());
        assertEquals("", box.value());
        assertEquals("main", box.contentOf("Id").orElseThrow());
        StxtNode text = box.child("Text").orElseThrow();
        assertEquals("Primera línea: con dos puntos\n\n\tSangrada", text.block());
        assertNull(text.value());
        assertEquals(7, text.line());
    }

    @Test
    void templateErrorsKeepCodeAndLine() {
        StxtSyntaxException e = assertThrows(StxtSyntaxException.class,
                () -> reader.read(doc("\tBox:", "\t\tPosition: sticky"), "doc.stxt"));
        assertEquals("INVALID_VALUE", e.code());
        assertEquals(6, e.line());
        assertEquals("doc.stxt", e.source());

        StxtSyntaxException noMedia = assertThrows(StxtSyntaxException.class,
                () -> reader.read("Document (dev.stxt.boxes):\n\tBox:\n\t\tId: a", "p"));
        assertEquals("TOO_FEW_CHILDREN", noMedia.code());

        StxtSyntaxException unknown = assertThrows(StxtSyntaxException.class,
                () -> reader.read(doc("\tBox:", "\t\tColour: #fff"), "p"));
        assertEquals(6, unknown.line());
    }

    @Test
    void syntaxErrorsWithLine() {
        StxtSyntaxException e = assertThrows(StxtSyntaxException.class,
                () -> reader.read(doc("\t\t\tBox:"), "p"));
        assertEquals(5, e.line());
    }

    @Test
    void rootMustDeclareNamespace() {
        StxtSyntaxException e = assertThrows(StxtSyntaxException.class,
                () -> reader.read("Document:\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 595", "p"));
        assertNull(e.code());
        assertEquals(1, e.line());
        assertThrows(StxtSyntaxException.class, () -> reader.read(doc() + "\nOther (dev.stxt.boxes):", "p"));
    }
}
