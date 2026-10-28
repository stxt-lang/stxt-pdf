package dev.stxt.boxes.tree;

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
        return "Document (dev.stxt.boxes):\n\tLanguage: " + language + "\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n";
    }

    private static Box build(String... lines) {
        return new BoxTreeBuilder().build(new DocumentReader().read(header("es") + String.join("\n", lines))).root();
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
        assertEquals(11, block.style.line("transform"));
        assertEquals(12, block.style.line("letter-spacing"));
        assertEquals(13, block.style.line("small-caps"));
    }

    @Test
    void theyCanBeSetInTheDocumentAndInStylesRules() {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n\t\tLetter spacing: 1\n"
                + "\tStyles:\n\t\tElement: Heading 1\n\t\t\tTypography:\n\t\t\t\tTransform: uppercase\n"
                + "\t\t\t\tSmall caps: true\n"
                + "\tHeading 1:\n\t\tText: Informe\n\tText: Hola\n";
        Box root = new BoxTreeBuilder().build(new DocumentReader().read(doc)).root();
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
                .read(header("tr") + "\tBlock:\n\t\tText: iğne\n")).root();
        assertEquals(Locale.forLanguageTag("tr"), root.children.get(0).children.get(0).style.locale);
        Box es = build("\tText: Hola");
        assertEquals(Locale.forLanguageTag("es"), es.children.get(0).style.locale);
    }

    @Test
    void letterSpacingNeedsPoints() {
        InvalidModelException e = assertThrows(InvalidModelException.class,
                () -> build("\tText: Hola", "\t\tTypography:", "\t\t\tLetter spacing: 10%"));
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.code());
        assertEquals(11, e.line());
    }
}
