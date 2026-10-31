package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.TextCase;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Case}, {@code Letter spacing} and {@code Small caps} in the tree (reference/common/text-formatting.md).
 */
class TextCaseTest {

    private static String header(String language) {
        return "Document (dev.stxt.pdf):\n\tLanguage: " + language + "\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";
    }

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(header("es") + String.join("\n", lines))))).sections().get(0).box();
    }

    @Test
    void defaultsDoNothing() {
        Style s = build("\tText: Hola").children().get(0).style();
        assertEquals(TextCase.NONE, s.typography().textCase());
        assertEquals(0, s.typography().letterSpacing());
        assertFalse(s.typography().smallCaps());
    }

    @Test
    void propertiesAreReadAndInherited() {
        Box block = build("\tBlock:", "\t\tTypography:", "\t\t\tCase: capitalize", "\t\t\tLetter spacing: -0.5",
                "\t\t\tSmall caps: true", "\t\tText: Hola").children().get(0);
        Box text = block.children().get(0);
        assertEquals(TextCase.CAPITALIZE, text.style().typography().textCase());
        assertEquals(-0.5, text.style().typography().letterSpacing(), 1e-9);
        assertTrue(text.style().typography().smallCaps());
        assertEquals(15, block.style().line("case").number());
        assertEquals(16, block.style().line("letter-spacing").number());
        assertEquals(17, block.style().line("small-caps").number());
    }

    @Test
    void theyCanBeSetInTheDocumentAndInStylesRules() {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\t\t\t\tLetter spacing: 1\n"
                + "\tStyles:\n\t\tFor element: Heading 1\n\t\t\tTypography:\n\t\t\t\tCase: uppercase\n"
                + "\t\t\t\tSmall caps: true\n"
                + "\tHeading 1:\n\t\tText: Informe\n\tText: Hola\n";
        Box root = new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
        Style heading = root.children().get(0).children().get(0).style();
        assertEquals(TextCase.UPPERCASE, heading.typography().textCase());
        assertTrue(heading.typography().smallCaps());
        assertEquals(1, heading.typography().letterSpacing(), 1e-9);
        assertEquals(1, root.children().get(1).style().typography().letterSpacing(), 1e-9);
        assertEquals(TextCase.NONE, root.children().get(1).style().typography().textCase());
    }

    @Test
    void theLocaleIsTheOneOfLanguageAndIsInherited() {
        Box root = new BoxTreeBuilder().build(new DocumentReader()
                .read(Docs.sectioned(header("tr") + "\tBlock:\n\t\tText: iğne\n"))).sections().get(0).box();
        assertEquals(Locale.forLanguageTag("tr"), root.children().get(0).children().get(0).style().typography().locale());
        Box es = build("\tText: Hola");
        assertEquals(Locale.forLanguageTag("es"), es.children().get(0).style().typography().locale());
    }

    /**
     * {@code Language} in {@code Typography} changes the language of the box and of what is inside it
     * (reference/common/typography.md).
     */
    @Test
    void aBoxCanDeclareItsOwnLanguageAndItIsInherited() {
        Box root = build("\tBlock:", "\t\tTypography:", "\t\t\tLanguage: tr", "\t\tText: iğne", "\tText: Hola");
        Box turkish = root.children().get(0);
        assertEquals(Locale.forLanguageTag("tr"), turkish.style().typography().locale());
        assertEquals(Locale.forLanguageTag("tr"), turkish.children().get(0).style().typography().locale());
        assertEquals(Locale.forLanguageTag("es"), root.children().get(1).style().typography().locale());
        assertEquals(15, turkish.style().line("language").number());
    }

    /** Also in a {@code Styles} rule: a class for the words in another language. */
    @Test
    void aStylesRuleCanDeclareTheLanguage() {
        Box root = build("\tStyles:", "\t\tFor style: english", "\t\t\tTypography:", "\t\t\t\tLanguage: en-GB",
                "\tText: Hola", "\tText: Hello", "\t\tStyle: english");
        assertEquals(Locale.forLanguageTag("es"), root.children().get(0).style().typography().locale());
        assertEquals(Locale.forLanguageTag("en-GB"), root.children().get(1).style().typography().locale());
    }

    @Test
    void languageMustBeABcp47Tag() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> build("\tText: Hola", "\t\tTypography:", "\t\t\tLanguage: español"));
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.errorCode());
        assertEquals(15, e.line());
    }

    /**
     * The language of the document is the {@code Language} of {@code Document}; the rule
     * {@code For element: Document} does not change it.
     */
    @Test
    void elementDocumentCannotChangeTheLanguage() {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\t\t\t\tLanguage: en\n\tText: Hola\n";
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))));
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(12, e.line());
    }

    @Test
    void letterSpacingNeedsPoints() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> build("\tText: Hola", "\t\tTypography:", "\t\t\tLetter spacing: 10%"));
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.errorCode());
        assertEquals(15, e.line());
    }
}
