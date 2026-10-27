package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineBreakerTest {

    // Factor 1 y tamaño 1: cada carácter mide 1 pt; un espacio también.
    private final TextMeasurer measurer = new FixedMetrics(1);
    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);

    @Test
    void greedyFirstFit() {
        List<String> l = LineBreaker.breakLines("uno dos tres cuatro", 8, measurer, F, 1);
        assertEquals(List.of("uno dos", "tres", "cuatro"), l);
    }

    @Test
    void prettyMinimizesTheDemerits() {
        // Ancho 10, espacios de 1 que se estiran hasta 0.5. El voraz deja 3 de sobrante en las
        // dos primeras líneas: badness 100 × (3/1)³ = 2700 y la máxima (3 / 0.5 = 6).
        // Knuth-Plass prefiere una línea con la máxima y otra con 100 × (1/1)³ = 100.
        String text = "a bbb c ddddd e fff";
        assertEquals(List.of("a bbb c", "ddddd e", "fff"),
                LineBreaker.breakParagraphs(text, 10, measurer, F, 1, Style.TextWrap.AUTO).get(0));
        assertEquals(List.of("a bbb", "c ddddd e", "fff"),
                LineBreaker.breakParagraphs(text, 10, measurer, F, 1, Style.TextWrap.PRETTY).get(0));
    }

    @Test
    void prettyKeepsParagraphsAndOverflowingWords() {
        assertEquals(List.of(List.of("a", "larguísima", "b"), List.of("uno", "dos")),
                LineBreaker.breakParagraphs("a larguísima b\n\nuno dos", 4, measurer, F, 1, Style.TextWrap.PRETTY));
        assertEquals(List.of(List.of("uno dos tres")),
                LineBreaker.breakParagraphs("uno dos tres", 100, measurer, F, 1, Style.TextWrap.PRETTY));
    }

    @Test
    void singleBreaksAreSpacesAndBlankLineSeparatesParagraphs() {
        List<String> l = LineBreaker.breakLines("uno\ndos\n\ntres", 100, measurer, F, 1);
        assertEquals(List.of("uno dos", "tres"), l);
    }

    @Test
    void wordWiderThanLineOverflows() {
        assertEquals(List.of("a", "larguísima", "b"), LineBreaker.breakLines("a larguísima b", 4, measurer, F, 1));
    }

    @Test
    void intrinsics() {
        IntrinsicSizes s = LineBreaker.intrinsics("uno dos tres", measurer, F, 1);
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

    @Test
    void placeMeasuresAlignsAndJustifies() {
        Style s = new Style();
        s.font = "F";
        s.fontSize = 1;
        s.textAlign = Style.TextAlign.JUSTIFY;
        // "uno dos tres" mide 12 en 20: 8 de sobrante entre 2 espacios, 4 por espacio.
        TextLine l = LineBreaker.place("uno dos tres", measurer, s, 20, true, 30, 12);
        assertEquals(new TextLine(0, 30, 20, 12, "uno dos tres", 4), l);
        // La última línea del párrafo no se justifica.
        assertEquals(new TextLine(0, 30, 12, 12, "uno dos tres", 0),
                LineBreaker.place("uno dos tres", measurer, s, 20, false, 30, 12));
        s.textAlign = Style.TextAlign.CENTER;
        assertEquals(new TextLine(4, 0, 12, 12, "uno dos tres", 0),
                LineBreaker.place("uno dos tres", measurer, s, 20, true, 0, 12));
    }
}
