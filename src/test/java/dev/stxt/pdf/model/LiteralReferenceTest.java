package dev.stxt.pdf.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * The references that are text (reference/common/references.md): one inside another pair of braces is kept with
 * {@link Reference#LITERAL_BRACE} as its brace, {@link Reference#escape} writes a text so that it is shown as it is,
 * and {@link Reference#shown} gives the text that is read.
 */
class LiteralReferenceTest {

    private static final String B = String.valueOf(Reference.LITERAL_BRACE);

    @Test
    void aReferenceInsideAnotherPairOfBracesIsText() {
        assertEquals("Pág. " + B + "page} de " + B + "pages}", Reference.literal("Pág. {{page}} de {{pages}}"));
        assertEquals(B + "page of: a}, " + B + "number of: b} y " + B + "heading 2: first}",
                Reference.literal("{{page of: a}}, {{number of: b}} y {{heading 2: first}}"));
        // Nothing takes it for a reference or a page number any more.
        assertFalse(Reference.contains(Reference.literal("{{page of: a}}")));
        assertFalse(Reference.literal("{{page}}").contains("{page}"));
    }

    @Test
    void onlyOnePairIsRemoved() {
        assertEquals("{" + B + "page}}", Reference.literal("{{{page}}}"));
        assertEquals("{{page}}", Reference.shown(Reference.literal("{{{page}}}")));
        assertEquals("{{{page}}}", Reference.shown(Reference.literal("{{{{page}}}}")));
    }

    @Test
    void whatIsNotAReferenceInsideBracesIsLeftAsItIs() {
        for (String text : new String[] {"{{nombre}}", "{page}", "{{page}", "{page}}", "{{ page }}", "{{page of: a}",
                "sin llaves", "{{}}", ""}) {
            assertSame(text, Reference.literal(text), text);
        }
        assertNull(Reference.literal(null));
        // A reference next to a brace that is not its pair is still a reference.
        assertEquals("{{page}", Reference.literal("{{page}"));
        assertEquals("{" + B + "page}", Reference.literal("{{{page}}"));
    }

    @Test
    void escapeWritesATextThatIsShownAsItIs() {
        assertEquals("Pág. {{page}} de {{pages}}", Reference.escape("Pág. {page} de {pages}"));
        assertEquals("{{page of: a}} {{number of: b}} {{heading 1}}",
                Reference.escape("{page of: a} {number of: b} {heading 1}"));
        assertEquals("{nombre} y {{x}}", Reference.escape("{nombre} y {{x}}"));
        assertSame("sin llaves", Reference.escape("sin llaves"));
        for (String text : new String[] {"{page}", "{{page}}", "{{{page}}}", "{page}}", "{{page}", "{page}{pages}",
                "{{page of: a}}", "a {heading 3: first} b", "{ {page} }", "{nombre}", "{{nombre}}", "{page of: {page}}",
                "}{page}{"}) {
            assertEquals(text, Reference.shown(Reference.literal(Reference.escape(text))), text);
            assertFalse(Reference.contains(Reference.literal(Reference.escape(text))), text);
            assertFalse(Reference.literal(Reference.escape(text)).matches(".*\\{pages?}.*"), text);
        }
    }

    @Test
    void shownGivesTheBraceBack() {
        assertEquals("{page}", Reference.shown(B + "page}"));
        assertSame("{page}", Reference.shown("{page}"));
        assertNull(Reference.shown(null));
    }
}
