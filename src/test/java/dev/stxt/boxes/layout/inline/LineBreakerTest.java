package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineBreakerTest {

    // Factor 1 y tamaño 1: cada carácter mide 1 pt; un espacio también.
    private final TextMeasurer measurer = new FixedMetrics(1);

    @Test
    void greedyFirstFit() {
        List<String> l = LineBreaker.breakLines("uno dos tres cuatro", 8, measurer, "F", 1);
        assertEquals(List.of("uno dos", "tres", "cuatro"), l);
    }

    @Test
    void singleBreaksAreSpacesAndBlankLineSeparatesParagraphs() {
        List<String> l = LineBreaker.breakLines("uno\ndos\n\ntres", 100, measurer, "F", 1);
        assertEquals(List.of("uno dos", "tres"), l);
    }

    @Test
    void wordWiderThanLineOverflows() {
        assertEquals(List.of("a", "larguísima", "b"), LineBreaker.breakLines("a larguísima b", 4, measurer, "F", 1));
    }

    @Test
    void intrinsics() {
        IntrinsicSizes s = LineBreaker.intrinsics("uno dos tres", measurer, "F", 1);
        assertEquals(4, s.minContent(), 1e-9);
        assertEquals(12, s.maxContent(), 1e-9);
        assertEquals(8, s.fitContent(8), 1e-9);
    }

    @Test
    void alignment() {
        assertEquals(15, LineBreaker.offset(70, 100, Style.TextAlign.CENTER), 1e-9);
        assertEquals(30, LineBreaker.offset(70, 100, Style.TextAlign.RIGHT), 1e-9);
        assertEquals(0, LineBreaker.offset(70, 100, Style.TextAlign.JUSTIFY), 1e-9);
    }

    @Test
    void justifySpreadsTheRemainderBetweenSpaces() {
        assertEquals(10, LineBreaker.wordSpacing("uno dos tres", 70, 90, Style.TextAlign.JUSTIFY, false), 1e-9);
    }

    @Test
    void justifyLeavesSomeLinesAlone() {
        // Última línea del párrafo, una sola palabra, línea que desborda y otra alineación.
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 70, 90, Style.TextAlign.JUSTIFY, true), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("palabra", 70, 90, Style.TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 100, 90, Style.TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 70, 90, Style.TextAlign.LEFT, false), 1e-9);
    }
}
