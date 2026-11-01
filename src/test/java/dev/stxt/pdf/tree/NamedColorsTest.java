package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The colors with a name of {@code Styles} (reference/common/styles.md): {@code Named color} declares a name once, and
 * it is written wherever a color goes, in the rules and in the elements; the document overrides the names of the
 * separate styles document.
 */
class NamedColorsTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    /** The document (header and these lines) with the separate styles document, or without it if {@code null}. */
    private static Box root(String styles, String... lines) {
        InlineNode document = READER.read(Docs.sectioned(HEADER + String.join("\n", lines)));
        InlineNode external = styles == null ? null : READER.readStyles(styles);
        return new BoxTreeBuilder().build(document, external).sections().get(0).box();
    }

    private static InvalidModelException error(String styles, String... lines) {
        return assertThrows(InvalidModelException.class, () -> root(styles, lines));
    }

    @Test
    void aNameIsWrittenWhereverAColorGoes() {
        Box root = root(BASE,
                "\tStyles:", "\t\tNamed color: brand #12315A", "\t\tNamed color: light #EEF3F8",
                "\tBlock:", "\t\tBox:", "\t\t\tBackground: light", "\t\t\tBorder: 1", "\t\t\tBorder color: brand #000 brand",
                "\t\tTypography:", "\t\t\tColor: brand",
                "\tBlock:", "\t\tBox:", "\t\t\tBackground: linear(180, light, #FFF, brand)",
                "\tColumns:", "\t\tCount: 2", "\t\tGap: 10", "\t\tSeparator: 1", "\t\tSeparator color: brand", "\t\tText: hola");
        Box block = root.children().get(0);
        assertEquals(new Paint.Solid("#EEF3F8"), block.style().box().background());
        assertEquals(Sides.shorthand(List.of("#12315A", "#000", "#12315A")), block.style().box().borderColor());
        assertEquals("#12315A", block.style().typography().color());
        assertEquals(new Paint.Linear(180, List.of("#EEF3F8", "#FFF", "#12315A")),
                root.children().get(1).style().box().background());
        assertEquals("#12315A", root.children().get(2).style().columns().separatorColor());
    }

    @Test
    void aRuleCanUseANameDeclaredAfterIt() {
        Box text = root(BASE, "\tStyles:", "\t\tFor element: Text", "\t\t\tTypography:", "\t\t\t\tColor: brand",
                "\t\tNamed color: brand #12315A", "\tText: hola").children().get(0);
        assertEquals("#12315A", text.style().typography().color());
    }

    @Test
    void theDocumentUsesTheNamesOfTheStylesDocument() {
        Box text = root(BASE + "\tNamed color: brand #12315A\n", "\tText: hola", "\t\tTypography:",
                "\t\t\tColor: brand").children().get(0);
        assertEquals("#12315A", text.style().typography().color());
    }

    @Test
    void theDocumentOverridesANameAlsoInTheRulesOfTheStylesDocument() {
        String styles = BASE + "\tNamed color: brand #12315A\n\tFor element: Text\n\t\tTypography:\n\t\t\tColor: brand\n";
        Box text = root(styles, "\tStyles:", "\t\tNamed color: brand #A32D2D", "\tText: hola").children().get(0);
        assertEquals("#A32D2D", text.style().typography().color());
        // The property keeps the line of the rule that writes the name, not that of the Named color.
        assertEquals(SourceLine.inStyles(9), text.style().line(PropertyKey.COLOR));
    }

    @Test
    void theSameNameInBothSourcesIsNotADuplicate() {
        assertNotNull(root(BASE + "\tNamed color: brand #12315A\n", "\tStyles:", "\t\tNamed color: brand #12315A",
                "\tText: hola"));
    }

    @Test
    void aNameThatIsNotUsedIsNotAnError() {
        assertNotNull(root(BASE + "\tNamed color: unused #12315A\n", "\tText: hola"));
    }

    @Test
    void aRepeatedNameInOneSourceIsAnError() {
        InvalidModelException e = error(BASE, "\tStyles:", "\t\tNamed color: brand #12315A",
                "\t\tNamed color: brand #A32D2D", "\tText: hola");
        assertEquals(ErrorCode.COLOR_DUPLICATE, e.errorCode());
        assertEquals("line 9: Repeated Named color: brand (already on line 8)", e.getMessage());

        e = error(BASE + "\tNamed color: brand #12315A\n\tNamed color: brand #A32D2D\n", "\tText: hola");
        assertEquals(ErrorCode.COLOR_DUPLICATE, e.errorCode());
        assertEquals("line 7 of the styles: Repeated Named color: brand (already on line 6)", e.getMessage());
    }

    @Test
    void anUnknownNameIsAnErrorOnTheLineOfTheProperty() {
        InvalidModelException e = error(BASE, "\tText: hola", "\t\tTypography:", "\t\t\tColor: brand");
        assertEquals(ErrorCode.COLOR_UNKNOWN, e.errorCode());
        assertEquals("line 10: Unknown color: brand (no Named color in Styles declares it)", e.getMessage());

        e = error(BASE, "\tBlock:", "\t\tBox:", "\t\t\tBackground: linear(90, #FFF, brand)");
        assertEquals(ErrorCode.COLOR_UNKNOWN, e.errorCode());

        // In a rule of the separate styles document, even if it applies to no element.
        e = error(BASE + "\tFor style: unused\n\t\tBox:\n\t\t\tBorder color: #000 brand\n", "\tText: hola");
        assertEquals(ErrorCode.COLOR_UNKNOWN, e.errorCode());
        assertTrue(e.getMessage().startsWith("line 8 of the styles: "), e.getMessage());
    }

    @Test
    void aColorThatIsNeitherHexadecimalNorANameIsASyntaxError() {
        InvalidModelException e = error(BASE, "\tStyles:", "\t\tNamed color: brand #12315A",
                "\tText: hola", "\t\tTypography:", "\t\t\tColor: Brand");
        assertEquals(ErrorCode.VALUE_SYNTAX, e.errorCode());
        assertEquals("line 12: Invalid color (it is written #RGB, #RGBA, #RRGGBB, #RRGGBBAA or with the name of a "
                + "Named color): Brand", e.getMessage());
    }

    @Test
    void theDefinitionIsANameAndAHexadecimalColor() {
        for (String definition : List.of("brand", "brand #12315A #000", "Brand #12315A", "1st #12315A", "dark--gray #333",
                "brand -#12315A", "brand other")) {
            InvalidModelException e = error(BASE, "\tStyles:", "\t\tNamed color: " + definition, "\tText: hola");
            assertEquals(ErrorCode.VALUE_SYNTAX, e.errorCode(), definition);
            assertTrue(e.getMessage().startsWith("line 8: "), e.getMessage());
        }
        InvalidModelException e = error(BASE, "\tStyles:", "\t\tNamed color: linear #12315A", "\tText: hola");
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.errorCode());

        e = error(BASE + "\tNamed color: Brand #12315A\n", "\tText: hola");
        assertTrue(e.getMessage().startsWith("line 6 of the styles: "), e.getMessage());
    }

    @Test
    void namesWithHyphensAndDigits() {
        Box text = root(BASE, "\tStyles:", "\t\tNamed color: gray-2 #777", "\tText: hola", "\t\tTypography:",
                "\t\t\tColor: gray-2").children().get(0);
        assertEquals("#777", text.style().typography().color());
    }
}
