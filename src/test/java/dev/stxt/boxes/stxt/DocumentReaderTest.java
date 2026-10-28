package dev.stxt.boxes.stxt;

import dev.stxt.boxes.Docs;
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
                "Document (dev.stxt.boxes):", "\tLanguage: es", "\tTitle: Prueba",
                "\tMedia:", "\t\tWidth: 595", "\t\tHeight: 842"));
        for (String l : boxLines) {
            sb.append('\n').append(l);
        }
        return Docs.sectioned(sb.toString());
    }

    @Test
    void examplesValidateAgainstTheSchema() {
        for (String file : new String[] {
                "test/spec/report.stxt",
                "test/golden/01-block-flow/input.stxt",
                "test/golden/02-flex-grow/input.stxt",
                "test/golden/03-positioning/input.stxt",
                "test/golden/04-grid/input.stxt",
                "test/golden/05-report/input.stxt",
                "test/golden/06-pagination/input.stxt",
                "test/golden/07-parallel-clone/input.stxt",
                "test/golden/08-styles/input.stxt",
                "test/golden/09-headings/input.stxt",
                "test/golden/10-lists/input.stxt",
                "test/golden/11-tables/input.stxt",
                "test/golden/12-multicol/input.stxt",
                "test/golden/13-markdown/input.stxt",
                "test/golden/14-images/input.stxt",
                "test/golden/15-contents/input.stxt",
                "test/golden/16-links/input.stxt",
                "test/golden/17-page-types/input.stxt",
                "test/golden/18-table-styles/input.stxt",
                "test/golden/19-inline/input.stxt",
                "test/golden/20-typography/input.stxt", "test/golden/21-box-looks/input.stxt",
                "test/golden/22-preformatted/input.stxt", "test/golden/23-rotation/input.stxt",
                "test/golden/24-svg/input.stxt", "test/golden/25-sections/input.stxt",
                "test/golden/26-heading-numbers/input.stxt",
                "test/golden/27-code-block/input.stxt"}) {
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
        assertEquals(4, examples);
    }

    @Test
    void nodesBlocksAndNamespace() {
        InlineNode root = reader.read(doc(
                "\tText:", "\t\tId: main", "\t\tContent >>", "\t\t\tPrimera línea: con dos puntos", "", "\t\t\t\tSangrada"));
        assertEquals("dev.stxt.boxes", root.getNamespace());
        InlineNode section = assertInstanceOf(InlineNode.class, root.getChild("Section"));
        InlineNode text = assertInstanceOf(InlineNode.class, section.getChild("Text"));
        assertEquals("dev.stxt.boxes", text.getNamespace());
        assertEquals("", text.getValue());
        assertEquals("main", text.getChild("Id").getText());
        Node content = text.getChild("Content");
        assertInstanceOf(TextNode.class, content);
        assertEquals("Primera línea: con dos puntos\n\n\tSangrada", content.getText());
        assertEquals(10, content.getLine());
    }

    @Test
    void schemaErrorsHaveCodeAndLine() {
        ParseException e = assertThrows(ParseException.class,
                () -> reader.read(doc("\tText: a", "\t\tTypography:", "\t\t\tAlign: middle")));
        assertEquals("INVALID_VALUE", e.getCode());
        assertEquals(10, e.getLine());

        ParseException noMedia = assertThrows(ParseException.class,
                () -> reader.read("Document (dev.stxt.boxes):\n\tSection:\n\t\tBlock:\n\t\t\tId: a"));
        assertEquals("TOO_FEW_CHILDREN", noMedia.getCode());

        ParseException unknown = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBlock:", "\t\tColour: #fff")));
        assertEquals(9, unknown.getLine());

        // Una propiedad fuera de su elemento es un error de estructura.
        ParseException misplaced = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBlock:", "\t\tOffset: 10")));
        assertEquals("CHILD_NOT_DECLARED", misplaced.getCode());
        assertEquals(9, misplaced.getLine());
        ParseException value = assertThrows(ParseException.class,
                () -> reader.read(doc("\tBlock: hola")));
        assertEquals(8, value.getLine());
    }

    @Test
    void tablesValidateAgainstTheSchema() {
        InlineNode root = reader.read(doc(
                "\tTable:", "\t\tId: t", "\t\tColumns: 120 1fr 1fr", "\t\tGap: 2",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tTypography:", "\t\t\t\t\tWeight: bold",
                "\t\t\t\tCell: Concepto", "\t\t\t\tCell: T1", "\t\t\t\tCell: T2",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tBreak:", "\t\t\t\t\tBefore: page",
                "\t\t\t\tCell: Ventas", "\t\t\t\tCell:", "\t\t\t\t\tParagraph: 100", "\t\t\t\tCell:",
                "\t\tFoot:", "\t\t\tRow:", "\t\t\t\tCell: Total", "\t\t\t\t\tSpan: 1 3",
                "\t\t\t\t\tBox:", "\t\t\t\t\t\tPadding: 4"));
        Node cell = root;
        for (String name : new String[] {"Section", "Table", "Foot", "Row", "Cell"}) {
            cell = assertInstanceOf(InlineNode.class, cell).getChild(name);
        }
        assertEquals("1 3", assertInstanceOf(InlineNode.class, cell).getChild("Span").getText());

        // Columns y Body son obligatorios; las filas van en su grupo y Span solo en Cell.
        assertEquals("TOO_FEW_CHILDREN", assertThrows(ParseException.class, () -> reader.read(doc(
                "\tTable:", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a"))).getCode());
        assertEquals("TOO_FEW_CHILDREN", assertThrows(ParseException.class, () -> reader.read(doc(
                "\tTable:", "\t\tColumns: 1fr", "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: a"))).getCode());
        ParseException row = assertThrows(ParseException.class, () -> reader.read(doc(
                "\tTable:", "\t\tColumns: 1fr", "\t\tRow:", "\t\t\tCell: a", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a")));
        assertEquals("CHILD_NOT_DECLARED", row.getCode());
        assertEquals(10, row.getLine());
        assertEquals("CHILD_NOT_DECLARED", assertThrows(ParseException.class, () -> reader.read(doc(
                "\tGrid:", "\t\tArea:", "\t\t\tSpan: 1 2", "\t\t\tBlock:"))).getCode());
    }

    @Test
    void syntaxErrorsWithLine() {
        ParseException e = assertThrows(ParseException.class, () -> reader.read(doc("\t\t\tBlock:")));
        assertEquals(7, e.getLine());
    }

    @Test
    void rootMustDeclareNamespace() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> reader.read("Document:\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842"));
        assertEquals(1, e.line());
        InvalidModelException twoRoots = assertThrows(InvalidModelException.class,
                () -> reader.read(doc("\tBlock:", "\t\tId: a") + "\nOther:"));
        assertEquals(10, twoRoots.line());
        // El schema admite cualquier elemento como raíz; el motor, solo Document.
        InvalidModelException block = assertThrows(InvalidModelException.class,
                () -> reader.read("Block (dev.stxt.boxes):\n\tId: a"));
        assertEquals(ErrorCode.DOCUMENT_ROOT, block.code());
    }
}
