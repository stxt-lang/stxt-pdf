package dev.stxt.boxes.stxt;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.exceptions.ParseException;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentReaderTest {

    private final DocumentReader reader = new DocumentReader();

    private static String doc(String... boxLines) {
        StringBuilder sb = new StringBuilder(String.join("\n",
                "Document (dev.stxt.boxes):",
                "\tMedia:", "\t\tWidth: 595", "\t\tHeight: 842"));
        for (String l : boxLines) {
            sb.append('\n').append(l);
        }
        return sb.toString();
    }

    @Test
    void examplesValidateAgainstTheSchema() {
        for (String file : new String[] {
                "src/test/resources/examples/report.stxt",
                "src/test/resources/golden/01-block-flow/input.stxt",
                "src/test/resources/golden/02-flex-grow/input.stxt",
                "src/test/resources/golden/03-positioning/input.stxt",
                "src/test/resources/golden/04-grid/input.stxt",
                "src/test/resources/golden/05-report/input.stxt",
                "src/test/resources/golden/06-pagination/input.stxt",
                "src/test/resources/golden/07-parallel-clone/input.stxt",
                "src/test/resources/golden/08-styles/input.stxt",
                "src/test/resources/golden/09-headings/input.stxt",
                "src/test/resources/golden/10-lists/input.stxt"}) {
            InlineNode root = reader.read(Path.of(file));
            assertEquals("Document", root.getName(), file);
        }
    }

    /** Los documentos de ejemplo de la especificación son válidos y se maquetan sin errores. */
    @Test
    void specificationExamplesAreValid() throws IOException {
        // Con core.autocrlf, la copia de trabajo puede tener CRLF.
        String spec = Files.readString(Path.of("docs/ESPECIFICACION.md")).replace("\r\n", "\n");
        Matcher m = Pattern.compile("```\n(Document \\(dev\\.stxt\\.boxes\\):\n.*?)```", Pattern.DOTALL).matcher(spec);
        int examples = 0;
        while (m.find()) {
            new Engine(new PdfMetrics(new Resources())).layOutDocument(reader.read(m.group(1)));
            examples++;
        }
        assertEquals(2, examples);
    }

    @Test
    void nodesBlocksAndNamespace() {
        InlineNode root = reader.read(doc(
                "\tText:", "\t\tId: main", "\t\tContent >>", "\t\t\tPrimera línea: con dos puntos", "", "\t\t\t\tSangrada"));
        assertEquals("dev.stxt.boxes", root.getNamespace());
        InlineNode text = assertInstanceOf(InlineNode.class, root.getChild("Text"));
        assertEquals("dev.stxt.boxes", text.getNamespace());
        assertEquals("", text.getValue());
        assertEquals("main", text.getChild("Id").getText());
        Node content = text.getChild("Content");
        assertInstanceOf(TextNode.class, content);
        assertEquals("Primera línea: con dos puntos\n\n\tSangrada", content.getText());
        assertEquals(7, content.getLine());
    }

    @Test
    void schemaErrorsHaveCodeAndLine() {
        ParseException e = assertThrows(ParseException.class,
                () -> reader.read(doc("\tText: a", "\t\tTypography:", "\t\t\tAlign: middle")));
        assertEquals("INVALID_VALUE", e.getCode());
        assertEquals(7, e.getLine());

        ParseException noMedia = assertThrows(ParseException.class,
                () -> reader.read("Document (dev.stxt.boxes):\n\tBlock:\n\t\tId: a"));
        assertEquals("TOO_FEW_CHILDREN", noMedia.getCode());

        ParseException unknown = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBlock:", "\t\tColour: #fff")));
        assertEquals(6, unknown.getLine());

        // Una propiedad fuera de su elemento es un error de estructura.
        ParseException misplaced = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBlock:", "\t\tOffset: 10")));
        assertEquals("CHILD_NOT_DECLARED", misplaced.getCode());
        assertEquals(6, misplaced.getLine());
        ParseException value = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBlock: hola")));
        assertEquals(5, value.getLine());
    }

    @Test
    void syntaxErrorsWithLine() {
        ParseException e = assertThrows(ParseException.class, () -> reader.read(doc("\t\t\tBlock:")));
        assertEquals(5, e.getLine());
    }

    @Test
    void rootMustDeclareNamespace() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> reader.read("Document:\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842"));
        assertEquals(1, e.line());
        InvalidModelException twoRoots = assertThrows(InvalidModelException.class,
                () -> reader.read(doc("\tBlock:", "\t\tId: a") + "\nOther:"));
        assertEquals(7, twoRoots.line());
        // El schema admite cualquier elemento como raíz; el motor, solo Document.
        InvalidModelException block = assertThrows(InvalidModelException.class,
                () -> reader.read("Block (dev.stxt.boxes):\n\tId: a"));
        assertEquals(ErrorCode.DOCUMENT_ROOT, block.code());
    }
}
