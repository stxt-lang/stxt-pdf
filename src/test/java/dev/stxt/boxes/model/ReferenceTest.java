package dev.stxt.boxes.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTest {

    @Test
    void findsPageAndNumberReferencesInOrder() {
        String text = "Ver {page of: wiring} y la {number of:clause-4}.";
        List<Reference> found = Reference.find(text);
        assertEquals(2, found.size());
        assertEquals(Reference.Kind.PAGE, found.get(0).kind());
        assertEquals("wiring", found.get(0).id());
        assertEquals("{page of: wiring}", text.substring(found.get(0).start(), found.get(0).end()));
        assertEquals(Reference.Kind.NUMBER, found.get(1).kind());
        assertEquals("clause-4", found.get(1).id());
    }

    @Test
    void theIdIsTrimmedAndMayBeEmpty() {
        assertEquals("a", Reference.find("{page of:    a   }").get(0).id());
        assertEquals("", Reference.find("{page of: }").get(0).id());
    }

    @Test
    void theMarkersOfPageNumbersAreNotReferences() {
        assertFalse(Reference.contains("Página {page} de {pages}"));
        assertFalse(Reference.contains("{page off: a}"));
        assertFalse(Reference.contains("{pages of: a}"));
        assertFalse(Reference.contains("{page of: a"));
        assertFalse(Reference.contains(null));
        assertTrue(Reference.contains("a {page of: a} b"));
    }

    @Test
    void atFindsOnlyAReferenceStartingThere() {
        String text = "x {number of: n}";
        assertNull(Reference.at(text, 0));
        assertNull(Reference.at(text, 1));
        assertEquals("n", Reference.at(text, 2).id());
        assertEquals(text.length(), Reference.at(text, 2).end());
    }

    @Test
    void replaceUsesTheValueAndKeepsWhatItCannotResolve() {
        String text = "p. {page of: a}, {number of: b}, p. {page of: zz}";
        String replaced = Reference.replace(text, (kind, id) -> switch (id) {
            case "a" -> "12";
            case "b" -> "IV";
            default -> null;
        });
        assertEquals("p. 12, IV, p. {page of: zz}", replaced);
        assertEquals("sin nada", Reference.replace("sin nada", (k, i) -> "x"));
    }
}
