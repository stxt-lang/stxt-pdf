package dev.stxt.boxes.stxt;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.exceptions.ParseException;
import dev.stxt.boxes.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentReaderTest {

    private final DocumentReader reader = new DocumentReader();

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
    void examplesValidateAgainstTheTemplate() {
        for (String file : new String[] {
                "src/test/resources/examples/report.stxt",
                "src/test/resources/golden/01-block-flow/input.stxt",
                "src/test/resources/golden/02-flex-grow/input.stxt",
                "src/test/resources/golden/03-positioning/input.stxt",
                "src/test/resources/golden/04-grid/input.stxt",
                "src/test/resources/golden/05-report/input.stxt",
                "src/test/resources/golden/06-pagination/input.stxt",
                "src/test/resources/golden/07-parallel-clone/input.stxt"}) {
            InlineNode root = reader.read(Path.of(file));
            assertEquals("Document", root.getName(), file);
        }
    }

    @Test
    void nodesBlocksAndNamespace() {
        InlineNode root = reader.read(doc(
                "\tBox:",
                "\t\tId: main",
                "\t\tText >>",
                "\t\t\tPrimera línea: con dos puntos",
                "",
                "\t\t\t\tSangrada"));
        assertEquals("dev.stxt.boxes", root.getNamespace());
        InlineNode box = assertInstanceOf(InlineNode.class, root.getChild("Box"));
        assertEquals("dev.stxt.boxes", box.getNamespace());
        assertEquals("", box.getValue());
        assertEquals("main", box.getChild("Id").getText());
        Node text = box.getChild("Text");
        assertInstanceOf(TextNode.class, text);
        assertEquals("Primera línea: con dos puntos\n\n\tSangrada", text.getText());
        assertEquals(7, text.getLine());
    }

    @Test
    void templateErrorsHaveCodeAndLine() {
        ParseException e = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBox:", "\t\tPosition: sticky")));
        assertEquals("INVALID_VALUE", e.getCode());
        assertEquals(6, e.getLine());

        ParseException noMedia = assertThrows(ParseException.class,
                () -> reader.read("Document (dev.stxt.boxes):\n\tBox:\n\t\tId: a"));
        assertEquals("TOO_FEW_CHILDREN", noMedia.getCode());

        ParseException unknown = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBox:", "\t\tColour: #fff")));
        assertEquals(6, unknown.getLine());
    }

    @Test
    void syntaxErrorsWithLine() {
        ParseException e = assertThrows(ParseException.class, () -> reader.read(doc("\t\t\tBox:")));
        assertEquals(5, e.getLine());
    }

    @Test
    void rootMustDeclareNamespace() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> reader.read("Document:\n\tMedia:\n\t\tType: continuous\n\t\tWidth: 595"));
        assertEquals(1, e.line());
        InvalidModelException twoRoots = assertThrows(InvalidModelException.class,
                () -> reader.read(doc("\tBox:", "\t\tId: a") + "\nOther:"));
        assertEquals(7, twoRoots.line());
    }
}
