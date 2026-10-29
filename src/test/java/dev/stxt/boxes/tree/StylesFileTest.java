package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.InlineNode;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.convert.Converter;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El documento de estilos aparte (ESPECIFICACION.md §4.10): sus reglas valen menos que las del
 * propio documento, que las mezcla propiedad a propiedad, y la tipografía base va en
 * {@code Element: Document}.
 */
class StylesFileTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n";
    private static final String BASE = "Styles (dev.stxt.boxes):\n\tElement: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    /** El documento (cabecera y estas líneas) con el documento de estilos aparte, o sin él si es {@code null}. */
    private static Box root(String styles, String... lines) {
        InlineNode document = READER.read(Docs.sectioned(HEADER + String.join("\n", lines)));
        InlineNode external = styles == null ? null : READER.readStyles(styles);
        return new BoxTreeBuilder().build(document, external).sections().get(0).box();
    }

    private static InvalidModelException error(String styles, String... lines) {
        return assertThrows(InvalidModelException.class, () -> root(styles, lines));
    }

    @Test
    void theBaseTypographyAndTheRulesCanComeFromTheStylesDocument() {
        Box text = root(BASE + "\tElement: Text\n\t\tTypography:\n\t\t\tWeight: bold\n", "\tText: hola").children().get(0);
        assertEquals("IBM Plex Sans", text.style().typography().font());
        assertEquals(10, text.style().typography().fontSize(), 1e-9);
        assertEquals(FontWeight.BOLD, text.style().typography().fontWeight());
    }

    @Test
    void theStylesDocumentIsOptional() {
        Box text = root(null, "\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans",
                "\t\t\t\tSize: 9", "\tText: hola").children().get(0);
        assertEquals(9, text.style().typography().fontSize(), 1e-9);
    }

    @Test
    void theDocumentWinsAndMergesWithTheStylesDocumentOneByOne() {
        Box text = root(BASE + "\tElement: Text\n\t\tTypography:\n\t\t\tSize: 12\n\t\t\tWeight: bold\n",
                "\tStyles:", "\t\tElement: Text", "\t\t\tTypography:", "\t\t\t\tSize: 14",
                "\tText: hola").children().get(0);
        assertEquals(14, text.style().typography().fontSize(), 1e-9);
        // Lo que el documento no declara lo pone el documento de estilos.
        assertEquals(FontWeight.BOLD, text.style().typography().fontWeight());
        // Cada propiedad guarda la línea de su regla, también en el documento aparte.
        assertEquals(10, text.style().line("font-size"));
        assertEquals(9, text.style().line("font-weight"));
    }

    @Test
    void theBaseTypographyOfTheDocumentWinsOverTheStylesDocument() {
        Box text = root(BASE, "\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tSize: 8",
                "\tText: hola").children().get(0);
        assertEquals(8, text.style().typography().fontSize(), 1e-9);
        assertEquals("IBM Plex Sans", text.style().typography().font());
    }

    @Test
    void thePriorityLevelsStayTheSameAcrossBothSources() {
        // Un For class del documento aparte gana a un Element del documento, y a su vez pierde ante For class del documento.
        String styles = BASE + "\tFor class: a\n\t\tBox:\n\t\t\tPadding: 2\n\t\t\tMargin: 2\n";
        Box root = root(styles,
                "\tStyles:", "\t\tElement: Block", "\t\t\tBox:", "\t\t\t\tPadding: 1", "\t\t\t\tMargin: 1",
                "\t\tFor class: a", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tBlock:", "\t\tClass: a");
        Box block = root.children().get(0);
        assertEquals(Sides.all(Value.pt(3)), block.style().box().padding());
        assertEquals(Sides.all(Value.pt(2)), block.style().box().margin());
    }

    @Test
    void anIdRuleOfTheStylesDocumentBeatsAClassRuleOfTheDocument() {
        String styles = BASE + "\tFor id: x\n\t\tBox:\n\t\t\tPadding: 5\n";
        Box block = root(styles, "\tStyles:", "\t\tFor class: a", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tBlock:", "\t\tId: x", "\t\tClass: a").children().get(0);
        assertEquals(Sides.all(Value.pt(5)), block.style().box().padding());
    }

    @Test
    void aClassCanHaveItsRuleOnlyInTheStylesDocument() {
        Box block = root(BASE + "\tFor class: a\n\t\tBox:\n\t\t\tPadding: 4\n", "\tBlock:", "\t\tClass: a").children().get(0);
        assertEquals(Sides.all(Value.pt(4)), block.style().box().padding());
    }

    @Test
    void rulesWithoutAnElementAreNeverAnError() {
        // Un documento de estilos sirve para varios documentos, y un elemento puede llegar después, de una plantilla.
        String styles = BASE + "\tFor id: nobody\n\t\tBox:\n\t\t\tPadding: 4\n\tFor class: unused\n\t\tBox:\n\t\t\tPadding: 1\n";
        assertNotNull(root(styles, "\tText: hola"));
        // Tampoco las del propio documento.
        assertNotNull(root(BASE, "\tStyles:", "\t\tFor id: nobody", "\t\t\tBox:", "\t\t\t\tPadding: 4",
                "\tText: hola"));
    }

    @Test
    void theSameRuleInBothSourcesIsNotADuplicate() {
        assertNotNull(root(BASE + "\tElement: Text\n\t\tBox:\n\t\t\tPadding: 1\n",
                "\tStyles:", "\t\tElement: Text", "\t\t\tBox:", "\t\t\t\tPadding: 2", "\tText: hola"));
    }

    @Test
    void aRepeatedRuleInsideTheStylesDocumentIsAnErrorThatSaysWhere() {
        InvalidModelException e = error(BASE + "\tElement: Text\n\t\tBox:\n\t\t\tPadding: 1\n\tElement: Text\n\t\tBox:\n\t\t\tPadding: 2\n",
                "\tText: hola");
        assertEquals(ErrorCode.STYLE_DUPLICATE, e.errorCode());
        assertEquals(9, e.line());
        assertTrue(e.getMessage().startsWith("línea 9 de los estilos: "), e.getMessage());
    }

    @Test
    void aBadValueInTheStylesDocumentPointsToItsLine() {
        InvalidModelException e = error(BASE + "\tElement: Text\n\t\tBox:\n\t\t\tPadding: mucho\n", "\tText: hola");
        assertEquals(8, e.line());
        assertTrue(e.getMessage().startsWith("línea 8 de los estilos: "), e.getMessage());
    }

    @Test
    void aBadValueInTheDocumentDoesNotMentionTheStylesDocument() {
        InvalidModelException e = error(BASE, "\tStyles:", "\t\tElement: Text", "\t\t\tBox:", "\t\t\t\tPadding: mucho",
                "\tText: hola");
        assertTrue(e.getMessage().startsWith("línea 10: "), e.getMessage());
    }

    @Test
    void anUnknownElementInTheStylesDocumentIsAnError() {
        InvalidModelException e = error(BASE + "\tElement: Nada\n\t\tBox:\n\t\t\tPadding: 1\n", "\tText: hola");
        assertEquals(ErrorCode.STYLE_UNKNOWN_ELEMENT, e.errorCode());
        assertTrue(e.getMessage().contains("de los estilos"), e.getMessage());
    }

    @Test
    void breakFromTheStylesDocumentCannotGoToAnAbsolute() {
        InvalidModelException e = error(BASE + "\tElement: Absolute\n\t\tBreak:\n\t\t\tAfter: avoid\n",
                "\tAbsolute:", "\t\tText: a");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(7, e.line());
        assertTrue(e.getMessage().startsWith("línea 7 de los estilos: "), e.getMessage());
    }

    @Test
    void elementDocumentOnlyAdmitsTypography() {
        // Dos Element: Document en el mismo origen son una regla repetida, como cualquier otra.
        InvalidModelException repeated = error(BASE + "\tElement: Document\n\t\tTypography:\n\t\t\tSize: 9\n", "\tText: hola");
        assertEquals(ErrorCode.STYLE_DUPLICATE, repeated.errorCode());
        InvalidModelException box = error("Styles (dev.stxt.boxes):\n\tElement: Document\n\t\tBox:\n\t\t\tPadding: 1\n",
                "\tText: hola");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, box.errorCode());
        assertEquals(3, box.line());
        InvalidModelException inDocument = error(null, "\tStyles:", "\t\tElement: Document", "\t\t\tBreak:", "\t\t\t\tAfter: avoid",
                "\tText: hola");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, inDocument.errorCode());
        assertEquals(9, inDocument.line());
        assertTrue(!inDocument.getMessage().contains("de los estilos"), inDocument.getMessage());
    }

    @Test
    void theDocumentNoLongerTakesATypography() {
        DocumentSyntaxException e = assertThrows(DocumentSyntaxException.class, () -> READER.read(Docs.sectioned(HEADER + "\tTypography:\n\t\tSize: 10\n\tText: a")));
        assertEquals("CHILD_NOT_DECLARED", e.code());
    }

    @Test
    void theRootOfTheStylesDocumentMustBeStyles() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> READER.readStyles(HEADER + "\tSection:\n\t\tText: a"));
        assertEquals(ErrorCode.DOCUMENT_ROOT, e.errorCode());
        assertThrows(InvalidModelException.class, () -> READER.readStyles(BASE + "Styles (dev.stxt.boxes):\n"));
        // Y no se pasa un Document como estilos.
        InlineNode document = READER.read(Docs.sectioned(HEADER + "\tText: a"));
        assertThrows(InvalidModelException.class, () -> new BoxTreeBuilder().build(document, document));
    }

    @Test
    void theStylesDocumentIsValidatedAgainstTheSchema() {
        DocumentSyntaxException e = assertThrows(DocumentSyntaxException.class,
                () -> READER.readStyles("Styles (dev.stxt.boxes):\n\tElement: Text\n\t\tColour: red\n"));
        assertEquals("NODE_NOT_DEFINED_IN_SCHEMA", e.code());
        // Un documento de estilos no tiene Media ni elementos.
        assertThrows(DocumentSyntaxException.class, () -> READER.readStyles("Styles (dev.stxt.boxes):\n\tText: a\n"));
    }

    @Test
    void tablePositionRulesOfTheStylesDocumentGoBeforeThoseOfTheDocument() {
        // Dentro del nivel de la posición, el documento aparte va antes: una Odd row cell suya pierde ante la Head cell del documento.
        String styles = BASE + "\tElement: Odd row cell\n\t\tBox:\n\t\t\tPadding: 7\n";
        Box table = root(styles, "\tStyles:", "\t\tElement: Head cell", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tTable:", "\t\tColumns: 1fr", "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: a",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: b").children().get(0);
        assertEquals(Sides.all(Value.pt(3)), table.children().get(0).style().box().padding());
        assertEquals(Sides.all(Value.pt(7)), table.children().get(1).style().box().padding());
    }

    @Test
    void theGeneratedEntriesOfAContentsTakeTheRulesOfTheStylesDocument() {
        String styles = BASE + "\tElement: Contents entry 1\n\t\tBox:\n\t\t\tPadding: 6\n";
        Box contents = root(styles, "\tHeading 1:", "\t\tText: Uno", "\tContents:").children().get(1);
        assertEquals(Sides.all(Value.pt(6)), contents.children().get(0).style().box().padding());
    }

    @Test
    void thePdfCanBeWrittenWithTheStylesDocument() throws IOException {
        InlineNode document = READER.read(Docs.sectioned(HEADER + "\tText: Hola"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().styles(BASE).build().toPdf(document, out);
        assertTrue(out.size() > 1000);
        // Sin la fuente base no hay texto que maquetar: el documento solo no basta.
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> Converter.create().toPdf(document, new ByteArrayOutputStream()));
        assertEquals(ErrorCode.TEXT_WITHOUT_FONT, e.errorCode());
    }
}
