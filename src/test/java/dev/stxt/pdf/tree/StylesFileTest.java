package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.InlineNode;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El documento de estilos aparte (referencia/comun/estilos.md): sus reglas valen menos que las del
 * propio documento, que las mezcla propiedad a propiedad, y la tipografía base va en
 * {@code For element: Document}.
 */
class StylesFileTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
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
        Box text = root(BASE + "\tFor element: Text\n\t\tTypography:\n\t\t\tBold: true\n", "\tText: hola").children().get(0);
        assertEquals("IBM Plex Sans", text.style().typography().font());
        assertEquals(10, text.style().typography().fontSize(), 1e-9);
        assertEquals(FontWeight.BOLD, text.style().typography().fontWeight());
    }

    @Test
    void theStylesDocumentIsOptional() {
        Box text = root(null, "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans",
                "\t\t\t\tSize: 9", "\tText: hola").children().get(0);
        assertEquals(9, text.style().typography().fontSize(), 1e-9);
    }

    @Test
    void theDocumentWinsAndMergesWithTheStylesDocumentOneByOne() {
        Box text = root(BASE + "\tFor element: Text\n\t\tTypography:\n\t\t\tSize: 12\n\t\t\tBold: true\n",
                "\tStyles:", "\t\tFor element: Text", "\t\t\tTypography:", "\t\t\t\tSize: 14",
                "\tText: hola").children().get(0);
        assertEquals(14, text.style().typography().fontSize(), 1e-9);
        // Lo que el documento no declara lo pone el documento de estilos.
        assertEquals(FontWeight.BOLD, text.style().typography().fontWeight());
        // Cada propiedad guarda la línea de su regla, también en el documento aparte.
        assertEquals(SourceLine.of(10), text.style().line("font-size"));
        assertEquals(SourceLine.inStyles(9), text.style().line("font-weight"));
    }

    @Test
    void theBaseTypographyOfTheDocumentWinsOverTheStylesDocument() {
        Box text = root(BASE, "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tSize: 8",
                "\tText: hola").children().get(0);
        assertEquals(8, text.style().typography().fontSize(), 1e-9);
        assertEquals("IBM Plex Sans", text.style().typography().font());
    }

    @Test
    void thePriorityLevelsStayTheSameAcrossBothSources() {
        // Un For style del documento aparte gana a un For element del documento, y a su vez pierde ante For style del
        // documento.
        String styles = BASE + "\tFor style: a\n\t\tBox:\n\t\t\tPadding: 2\n\t\t\tMargin: 2\n";
        Box root = root(styles,
                "\tStyles:", "\t\tFor element: Block", "\t\t\tBox:", "\t\t\t\tPadding: 1", "\t\t\t\tMargin: 1",
                "\t\tFor style: a", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tBlock:", "\t\tStyle: a");
        Box block = root.children().get(0);
        assertEquals(Sides.all(Value.pt(3)), block.style().box().padding());
        assertEquals(Sides.all(Value.pt(2)), block.style().box().margin());
    }

    @Test
    void anIdRuleOfTheStylesDocumentBeatsAClassRuleOfTheDocument() {
        String styles = BASE + "\tFor id: x\n\t\tBox:\n\t\t\tPadding: 5\n";
        Box block = root(styles, "\tStyles:", "\t\tFor style: a", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tBlock:", "\t\tId: x", "\t\tStyle: a").children().get(0);
        assertEquals(Sides.all(Value.pt(5)), block.style().box().padding());
    }

    @Test
    void aClassCanHaveItsRuleOnlyInTheStylesDocument() {
        Box block = root(BASE + "\tFor style: a\n\t\tBox:\n\t\t\tPadding: 4\n", "\tBlock:", "\t\tStyle: a").children().get(0);
        assertEquals(Sides.all(Value.pt(4)), block.style().box().padding());
    }

    @Test
    void rulesWithoutAnElementAreNeverAnError() {
        // Un documento de estilos sirve para varios documentos, y un elemento puede llegar después, de una plantilla.
        String styles = BASE + "\tFor id: nobody\n\t\tBox:\n\t\t\tPadding: 4\n\tFor style: unused\n\t\tBox:\n\t\t\tPadding: 1\n";
        assertNotNull(root(styles, "\tText: hola"));
        // Tampoco las del propio documento.
        assertNotNull(root(BASE, "\tStyles:", "\t\tFor id: nobody", "\t\t\tBox:", "\t\t\t\tPadding: 4",
                "\tText: hola"));
    }

    @Test
    void theSameRuleInBothSourcesIsNotADuplicate() {
        assertNotNull(root(BASE + "\tFor element: Text\n\t\tBox:\n\t\t\tPadding: 1\n",
                "\tStyles:", "\t\tFor element: Text", "\t\t\tBox:", "\t\t\t\tPadding: 2", "\tText: hola"));
    }

    @Test
    void aRepeatedRuleInsideTheStylesDocumentIsAnErrorThatSaysWhere() {
        InvalidModelException e = error(BASE + "\tFor element: Text\n\t\tBox:\n\t\t\tPadding: 1\n\tFor element: Text\n\t\tBox:\n\t\t\tPadding: 2\n",
                "\tText: hola");
        assertEquals(ErrorCode.STYLE_DUPLICATE, e.errorCode());
        assertEquals(9, e.line());
        assertTrue(e.getMessage().startsWith("línea 9 de los estilos: "), e.getMessage());
    }

    @Test
    void aBadValueInTheStylesDocumentPointsToItsLine() {
        InvalidModelException e = error(BASE + "\tFor element: Text\n\t\tBox:\n\t\t\tPadding: mucho\n", "\tText: hola");
        assertEquals(8, e.line());
        assertTrue(e.getMessage().startsWith("línea 8 de los estilos: "), e.getMessage());
    }

    @Test
    void aBadValueInTheDocumentDoesNotMentionTheStylesDocument() {
        InvalidModelException e = error(BASE, "\tStyles:", "\t\tFor element: Text", "\t\t\tBox:", "\t\t\t\tPadding: mucho",
                "\tText: hola");
        assertTrue(e.getMessage().startsWith("línea 10: "), e.getMessage());
    }

    @Test
    void aRuleCheckedAfterBuildingTheTreePointsToTheStylesDocument() {
        InvalidModelException e = error(BASE + "\tFor element: Block\n\t\tBox:\n\t\t\tWidth: 10\n\t\t\tHeight: 10\n"
                + "\t\t\tAspect ratio: 2\n", "\tBlock:", "\t\tText: hola");
        assertEquals(ErrorCode.ASPECT_RATIO_CONFLICT, e.errorCode());
        assertEquals(10, e.line());
        assertTrue(e.inStyles());
        assertTrue(e.getMessage().startsWith("línea 10 de los estilos: "), e.getMessage());
    }

    @Test
    void aMessageNamesTheDocumentOfEachLine() {
        // Min width llega de los estilos y Max width es del documento: cada línea dice de dónde es.
        InvalidModelException e = error(BASE + "\tFor element: Block\n\t\tBox:\n\t\t\tMin width: 100\n",
                "\tBlock:", "\t\tBox:", "\t\t\tMax width: 50", "\t\tText: hola");
        assertEquals(ErrorCode.MIN_MAX_CONFLICT, e.errorCode());
        assertEquals("línea 8 de los estilos: Min width es mayor que Max width (línea 10)", e.getMessage());
        // Y al revés.
        e = error(BASE + "\tFor element: Block\n\t\tBox:\n\t\t\tMax width: 50\n",
                "\tBlock:", "\t\tBox:", "\t\t\tMin width: 100", "\t\tText: hola");
        assertFalse(e.inStyles());
        assertEquals("línea 10: Min width es mayor que Max width (línea 8 de los estilos)", e.getMessage());
    }

    @Test
    void aDeclarationOfTheDocumentAfterARuleOfTheStylesDocumentIsOfTheDocument() {
        Box block = root(BASE + "\tFor element: Block\n\t\tBox:\n\t\t\tPadding: 1\n",
                "\tBlock:", "\t\tBox:", "\t\t\tMargin: 2", "\t\tText: hola").children().get(0);
        assertEquals(SourceLine.inStyles(8), block.style().line("padding"));
        assertEquals(SourceLine.of(10), block.style().line("margin"));
        // La tipografía se hereda con su documento.
        assertEquals(SourceLine.inStyles(4), block.children().get(0).style().line("font"));
    }

    @Test
    void anUnknownElementInTheStylesDocumentIsAnError() {
        InvalidModelException e = error(BASE + "\tFor element: Nada\n\t\tBox:\n\t\t\tPadding: 1\n", "\tText: hola");
        assertEquals(ErrorCode.STYLE_UNKNOWN_ELEMENT, e.errorCode());
        assertTrue(e.getMessage().contains("de los estilos"), e.getMessage());
    }

    @Test
    void breakFromTheStylesDocumentCannotGoToAnAbsolute() {
        InvalidModelException e = error(BASE + "\tFor element: Overlay\n\t\tBreak:\n\t\t\tAfter: avoid\n",
                "\tOverlay:", "\t\tText: a");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(7, e.line());
        assertTrue(e.getMessage().startsWith("línea 7 de los estilos: "), e.getMessage());
    }

    @Test
    void elementDocumentOnlyAdmitsTypography() {
        // Dos For element: Document en el mismo origen son una regla repetida, como cualquier otra.
        InvalidModelException repeated = error(BASE + "\tFor element: Document\n\t\tTypography:\n\t\t\tSize: 9\n", "\tText: hola");
        assertEquals(ErrorCode.STYLE_DUPLICATE, repeated.errorCode());
        InvalidModelException box = error("Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tBox:\n\t\t\tPadding: 1\n",
                "\tText: hola");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, box.errorCode());
        assertEquals(3, box.line());
        InvalidModelException inDocument = error(null, "\tStyles:", "\t\tFor element: Document", "\t\t\tBreak:", "\t\t\t\tAfter: avoid",
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
        assertTrue(e.inStyles());
        assertThrows(InvalidModelException.class, () -> READER.readStyles(BASE + "Styles (dev.stxt.pdf):\n"));
        // Y no se pasa un Document como estilos.
        InlineNode document = READER.read(Docs.sectioned(HEADER + "\tText: a"));
        assertThrows(InvalidModelException.class, () -> new BoxTreeBuilder().build(document, document));
    }

    @Test
    void theStylesDocumentIsValidatedAgainstTheSchema() {
        DocumentSyntaxException e = assertThrows(DocumentSyntaxException.class,
                () -> READER.readStyles("Styles (dev.stxt.pdf):\n\tFor element: Text\n\t\tColour: red\n"));
        assertEquals("NODE_NOT_DEFINED_IN_SCHEMA", e.code());
        assertTrue(e.inStyles());
        assertTrue(e.getMessage().startsWith("línea 3 de los estilos: "), e.getMessage());
        // Un documento de estilos no tiene Page ni elementos.
        assertThrows(DocumentSyntaxException.class, () -> READER.readStyles("Styles (dev.stxt.pdf):\n\tText: a\n"));
    }

    @Test
    void tablePositionRulesOfTheStylesDocumentGoBeforeThoseOfTheDocument() {
        // Dentro del nivel de la posición, el documento aparte va antes: una Odd row cell suya pierde ante la Head cell
        // del documento.
        String styles = BASE + "\tFor element: Odd row cell\n\t\tBox:\n\t\t\tPadding: 7\n";
        Box table = root(styles, "\tStyles:", "\t\tFor element: Head cell", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\tTable:", "\t\tColumn sizes: 1fr", "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: a",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: b").children().get(0);
        assertEquals(Sides.all(Value.pt(3)), table.children().get(0).style().box().padding());
        assertEquals(Sides.all(Value.pt(7)), table.children().get(1).style().box().padding());
    }

    @Test
    void theGeneratedEntriesOfATableOfContentsTakeTheRulesOfTheStylesDocument() {
        String styles = BASE + "\tFor element: TOC 1\n\t\tBox:\n\t\t\tPadding: 6\n";
        Box contents = root(styles, "\tHeading 1:", "\t\tText: Uno", "\tTable of contents:").children().get(1);
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

    /** El error de convertir el documento (cabecera y estas líneas) con el documento de estilos aparte. */
    private static InvalidModelException conversionError(String styles, String... lines) {
        InlineNode document = READER.read(Docs.sectioned(HEADER + String.join("\n", lines)));
        Converter converter = Converter.builder().styles(styles).build();
        return assertThrows(InvalidModelException.class, () -> converter.toPdf(document, new ByteArrayOutputStream()));
    }

    @Test
    void anUnknownFontOfTheStylesDocumentPointsToItsLine() {
        InvalidModelException e = conversionError("Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n"
                + "\t\t\tFont: Nada\n\t\t\tSize: 10\n", "\tText: hola");
        assertEquals(ErrorCode.FONT_UNKNOWN, e.errorCode());
        assertEquals(4, e.line());
        assertTrue(e.inStyles());
        assertTrue(e.getMessage().startsWith("línea 4 de los estilos: "), e.getMessage());
    }

    @Test
    void anOverflowCausedByTheStylesDocumentPointsToItsLine() {
        // La línea del error es la del Width, en los estilos; la caja se nombra por su línea en el documento.
        InvalidModelException e = conversionError(BASE + "\tFor element: Block\n\t\tBox:\n\t\t\tWidth: 2000\n",
                "\tBlock:", "\t\tText: hola");
        assertEquals(ErrorCode.BOX_OVERFLOW, e.errorCode());
        assertTrue(e.inStyles());
        assertTrue(e.getMessage().startsWith("línea 8 de los estilos: La caja de la línea 8 no cabe a lo ancho"),
                e.getMessage());
    }
}
