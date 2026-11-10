package dev.stxt.pdf.layout.inline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Reference;

/** What the texts are worth between the passes of the pagination, with {@link FixedMetrics}: 5 pt per character. */
class TextValuesTest {

    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    private final TextValues values = new TextValues(new FixedMetrics(), new Engine());

    @Test
    void theMarkersMeasureAsTheProbeNumber() {
        assertEquals(10, values.measurer().width("{page}", F, 10), 1e-9);
        assertEquals(10, values.measurer().width("{pages}", F, 10), 1e-9);
        values.reservePageNumbers("000");
        assertEquals(15, values.measurer().width("{pages}", F, 10), 1e-9);
        // The real measurer measures what it is given.
        assertEquals(35, values.realMeasurer().width("{pages}", F, 10), 1e-9);
    }

    @Test
    void theReferencesAreWorthWhatIsAssumed() {
        assertNull(values.referenced(Reference.Kind.PAGE, "a"));
        values.assume(Map.of("a", "3"), Map.of("a", "1.2"));
        values.headings(Map.of("1", "Intro"));
        assertEquals("3", values.referenced(Reference.Kind.PAGE, "a"));
        assertEquals("1.2", values.referenced(Reference.Kind.NUMBER, "a"));
        assertEquals("Intro", values.referenced(Reference.Kind.HEADING, "1"));
        assertNull(values.referenced(Reference.Kind.HEADING, "2"));
    }

    @Test
    void resetReturnsToTheFirstProbeAndNoReferences() {
        values.reservePageNumbers("MMMM");
        values.assume(Map.of("a", "3"), Map.of());
        values.reset();
        assertEquals(10, values.measurer().width("{page}", F, 10), 1e-9);
        assertNull(values.referenced(Reference.Kind.PAGE, "a"));
    }
}
