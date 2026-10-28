package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code Transform}, {@code Letter spacing} y {@code Small caps} en el árbol (§4.4). */
class TextCaseTest {

    private static String header(String language) {
        return "Document (dev.stxt.boxes):\n\tLanguage: " + language + "\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";
    }

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(header("es") + String.join("\n", lines))))).sections().get(0).box();
    }

    @Test
    void defaultsDoNothing() {
        Style s = build("\tText: Hola").children.get(0).style;
        assertEquals(Style.Transform.NONE, s.transform);
        assertEquals(0, s.letterSpacing);
        assertFalse(s.smallCaps);
    }

    @Test
    void propertiesAreReadAndInherited() {
        Box block = build("\tBlock:", "\t\tTypography:", "\t\t\tTransform: capitalize", "\t\t\tLetter spacing: -0.5",
                "\t\t\tSmall caps: true", "\t\tText: Hola").children.get(0);
        Box text = block.children.get(0);
        assertEquals(Style.Transform.CAPITALIZE, text.style.transform);
        assertEquals(-0.5, text.style.letterSpacing, 1e-9);
        assertTrue(text.style.smallCaps);
        assertEquals(15, block.style.line("transform"));
        assertEquals(16, block.style.line("letter-spacing"));
        assertEquals(17, block.style.line("small-caps"));
    }

    @Test
    void theyCanBeSetInTheDocumentAndInStylesRules() {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\t\t\t\tLetter spacing: 1\n"
                + "\tStyles:\n\t\tElement: Heading 1\n\t\t\tTypography:\n\t\t\t\tTransform: uppercase\n"
                + "\t\t\t\tSmall caps: true\n"
                + "\tHeading 1:\n\t\tText: Informe\n\tText: Hola\n";
        Box root = new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
        Style heading = root.children.get(0).children.get(0).style;
        assertEquals(Style.Transform.UPPERCASE, heading.transform);
        assertTrue(heading.smallCaps);
        assertEquals(1, heading.letterSpacing, 1e-9);
        assertEquals(1, root.children.get(1).style.letterSpacing, 1e-9);
        assertEquals(Style.Transform.NONE, root.children.get(1).style.transform);
    }

    @Test
    void theLocaleIsTheOneOfLanguageAndIsInherited() {
        Box root = new BoxTreeBuilder().build(new DocumentReader()
                .read(Docs.sectioned(header("tr") + "\tBlock:\n\t\tText: iğne\n"))).sections().get(0).box();
        assertEquals(Locale.forLanguageTag("tr"), root.children.get(0).children.get(0).style.locale);
        Box es = build("\tText: Hola");
        assertEquals(Locale.forLanguageTag("es"), es.children.get(0).style.locale);
    }

    /** {@code Language} en {@code Typography} cambia el idioma de la caja y de lo que hay dentro (§4.4). */
    @Test
    void aBoxCanDeclareItsOwnLanguageAndItIsInherited() {
        Box root = build("\tBlock:", "\t\tTypography:", "\t\t\tLanguage: tr", "\t\tText: iğne", "\tText: Hola");
        Box turkish = root.children.get(0);
        assertEquals(Locale.forLanguageTag("tr"), turkish.style.locale);
        assertEquals(Locale.forLanguageTag("tr"), turkish.children.get(0).style.locale);
        assertEquals(Locale.forLanguageTag("es"), root.children.get(1).style.locale);
        assertEquals(15, turkish.style.line("language"));
    }

    /** También en una regla de {@code Styles}: una clase para las palabras de otro idioma. */
    @Test
    void aStylesRuleCanDeclareTheLanguage() {
        Box root = build("\tStyles:", "\t\tFor class: english", "\t\t\tTypography:", "\t\t\t\tLanguage: en-GB",
                "\tText: Hola", "\tText: Hello", "\t\tClass: english");
        assertEquals(Locale.forLanguageTag("es"), root.children.get(0).style.locale);
        assertEquals(Locale.forLanguageTag("en-GB"), root.children.get(1).style.locale);
    }

    @Test
    void languageMustBeABcp47Tag() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> build("\tText: Hola", "\t\tTypography:", "\t\t\tLanguage: español"));
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.errorCode());
        assertEquals(15, e.line());
    }

    /** El idioma del documento es el {@code Language} de {@code Document}; la regla {@code Element: Document} no lo cambia. */
    @Test
    void elementDocumentCannotChangeTheLanguage() {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
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
