package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.TextWrap;
import dev.stxt.pdf.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LineBreakerTest {

    // Factor 1 y tamaño 1: cada carácter mide 1 pt; un espacio también.
    private final TextMeasurer measurer = new FixedMetrics(1);
    private static final FontFace F = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    /** Como {@link #measurer}, pero cada carácter en negrita mide 2 pt. */
    private final TextMeasurer boldIsWider = (text, font, fontSize) ->
            text.codePointCount(0, text.length()) * fontSize * (font.weight() == FontWeight.BOLD ? 2 : 1);

    private static List<String> texts(List<LineBreaker.Line> lines) {
        return lines.stream().map(LineBreaker.Line::text).toList();
    }

    private static List<List<String>> paragraphs(List<List<LineBreaker.Line>> paragraphs) {
        return paragraphs.stream().map(LineBreakerTest::texts).toList();
    }

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
                LineBreaker.breakParagraphs(text, 10, measurer, F, 1, TextWrap.SIMPLE).get(0));
        assertEquals(List.of("a bbb", "c ddddd e", "fff"),
                LineBreaker.breakParagraphs(text, 10, measurer, F, 1, TextWrap.BALANCED).get(0));
    }

    @Test
    void prettyKeepsParagraphsAndOverflowingWords() {
        assertEquals(List.of(List.of("a", "larguísima", "b"), List.of("uno", "dos")),
                LineBreaker.breakParagraphs("a larguísima b\n\nuno dos", 4, measurer, F, 1, TextWrap.BALANCED));
        assertEquals(List.of(List.of("uno dos tres")),
                LineBreaker.breakParagraphs("uno dos tres", 100, measurer, F, 1, TextWrap.BALANCED));
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
        assertEquals(15, LineBreaker.offset(70, 100, TextAlign.CENTER), 1e-9);
        assertEquals(30, LineBreaker.offset(70, 100, TextAlign.RIGHT), 1e-9);
        assertEquals(0, LineBreaker.offset(70, 100, TextAlign.JUSTIFY), 1e-9);
    }

    @Test
    void justifySpreadsTheRemainderBetweenSpaces() {
        assertEquals(10, LineBreaker.wordSpacing("uno dos tres", 70, 90, TextAlign.JUSTIFY, false), 1e-9);
    }

    @Test
    void justifyLeavesSomeLinesAlone() {
        // Última línea del párrafo, una sola palabra, línea que desborda y otra alineación.
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 70, 90, TextAlign.JUSTIFY, true), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("palabra", 70, 90, TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 100, 90, TextAlign.JUSTIFY, false), 1e-9);
        assertEquals(0, LineBreaker.wordSpacing("uno dos", 70, 90, TextAlign.LEFT, false), 1e-9);
    }

    @Test
    void placeMeasuresAlignsAndJustifies() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "uno dos tres" mide 12 en 20: 8 de sobrante entre 2 espacios, 4 por espacio.
        TextLine l = LineBreaker.place("uno dos tres", measurer, s.style(), 20, true, 30, 12);
        assertEquals(new TextLine(0, 30, 20, 12, "uno dos tres", 4), l);
        // La última línea del párrafo no se justifica.
        assertEquals(new TextLine(0, 30, 12, 12, "uno dos tres", 0),
                LineBreaker.place("uno dos tres", measurer, s.style(), 20, false, 30, 12));
        s.textAlign(TextAlign.CENTER);
        assertEquals(new TextLine(4, 0, 12, 12, "uno dos tres", 0),
                LineBreaker.place("uno dos tres", measurer, s.style(), 20, true, 0, 12));
    }

    @Test
    void aWordCanHavePartsOfSeveralRuns() {
        // "uno ne**gri**ta dos": negrita es una sola palabra de tres partes.
        List<TextRun> runs = List.of(run("uno ne"), run("gri", "b"), run("ta dos"));
        List<LineBreaker.Word> words = LineBreaker.paragraphs(runs).get(0);
        assertEquals(3, words.size());
        assertEquals(List.of(run("ne"), run("gri", "b"), run("ta")), words.get(1).parts());
        // Cada parte con su fuente: ne (2) + gri (6) + ta (2).
        IntrinsicSizes s = LineBreaker.intrinsics(runs, boldIsWider, F, 1);
        assertEquals(10, s.minContent(), 1e-9);
        assertEquals(3 + 1 + 10 + 1 + 3, s.maxContent(), 1e-9);
    }

    @Test
    void greedyMeasuresEachRunWithItsFont() {
        // Sin formato, "aa bb" (5) cabe en 6; con "bb cc" en negrita, "aa bb" mide 7.
        List<TextRun> runs = List.of(run("aa "), run("bb cc", "b"));
        assertEquals(List.of("aa bb", "cc"),
                texts(LineBreaker.breakParagraphs(runs, 6, measurer, F, 1, TextWrap.SIMPLE).get(0)));
        List<LineBreaker.Line> lines = LineBreaker.breakParagraphs(runs, 6, boldIsWider, F, 1,
                TextWrap.SIMPLE).get(0);
        assertEquals(List.of("aa", "bb", "cc"), texts(lines));
        assertEquals(List.of(run("bb", "b")), lines.get(1).runs());
    }

    @Test
    void prettyMeasuresEachRunWithItsFont() {
        // "a bb c": con bb en negrita, la palabra mide 4 y no cabe con las otras en 6.
        List<TextRun> runs = List.of(run("a "), run("bb", "b"), run(" c"));
        assertEquals(List.of("a bb c"),
                texts(LineBreaker.breakParagraphs(runs, 6, measurer, F, 1, TextWrap.BALANCED).get(0)));
        assertEquals(2, LineBreaker.breakParagraphs(runs, 6, boldIsWider, F, 1, TextWrap.BALANCED)
                .get(0).size());
    }

    @Test
    void theSpaceTakesTheFormatOfItsFirstWhitespace() {
        TextFormat underline = new TextFormat(false, false, true, false);
        // Dentro de un tramo subrayado, el espacio también lo está.
        assertEquals(List.of(new TextRun("a b", underline)),
                LineBreaker.breakParagraphs(List.of(run("a b", "u")), 100, measurer, F, 1, TextWrap.SIMPLE)
                        .get(0).get(0).runs());
        // Entre dos tramos subrayados separados por un espacio sin formato, no.
        assertEquals(List.of(run("a", "u"), run(" "), run("b c", "u")),
                LineBreaker.breakParagraphs(List.of(run("a", "u"), run(" "), run("b c", "u")), 100, measurer, F, 1,
                        TextWrap.SIMPLE).get(0).get(0).runs());
        // Los espacios seguidos valen uno, con el formato del primero.
        assertEquals(List.of(run("a ", "u"), run("b")),
                LineBreaker.breakParagraphs(List.of(run("a  ", "u"), run("\n b")), 100, measurer, F, 1,
                        TextWrap.SIMPLE).get(0).get(0).runs());
    }

    @Test
    void aBlankLineAcrossRunsSeparatesParagraphs() {
        List<List<LineBreaker.Line>> p = LineBreaker.breakParagraphs(List.of(run("uno\n", "b"), run("\ndos")), 100,
                measurer, F, 1, TextWrap.SIMPLE);
        assertEquals(2, p.size());
        assertEquals(List.of(run("uno", "b")), p.get(0).get(0).runs());
        assertEquals(List.of(run("dos")), p.get(1).get(0).runs());
    }

    @Test
    void placePositionsEachRunAndSpreadsTheJustification() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.textAlign(TextAlign.JUSTIFY);
        // "uno " (4) + "dos" en negrita (6) + " tres" (5) = 15 en 20: 2,5 más por espacio.
        TextLine l = LineBreaker.place(List.of(run("uno "), run("dos", "b"), run(" tres")), boldIsWider, s.style(), 20,
                true, 0, 12);
        assertEquals(List.of(new TextLine.Run(0, 6.5, "uno ", TextFormat.PLAIN),
                        new TextLine.Run(6.5, 6, "dos", run("dos", "b").format()),
                        new TextLine.Run(12.5, 7.5, " tres", TextFormat.PLAIN)),
                l.runs());
        assertEquals(new TextLine(0, 0, 20, 12, "uno dos tres", 2.5, l.runs()), l);
    }

    // --- Interletraje y versalitas (referencia/comun/tipografia.md)
    // ----------------------------------------------------

    @Test
    void letterSpacingAddsToEveryCharacterButTheLastOfTheLine() {
        // «uno dos» mide 7; con 1 de interletraje, 7 + 6 = 13.
        assertEquals(13, LineBreaker.width(List.of(run("uno dos")), measurer, F, 1, 1), 1e-9);
        assertEquals(7, LineBreaker.width(List.of(run("uno dos")), measurer, F, 1, 0), 1e-9);
        assertEquals(new IntrinsicSizes(5, 13),
                LineBreaker.intrinsics(List.of(run("uno dos")), measurer, F, 1, 1));
    }

    @Test
    void letterSpacingChangesTheBreaks() {
        // Ancho 8: «uno dos» cabe (7), pero con interletraje 1 mide 13.
        assertEquals(List.of(List.of("uno dos")),
                paragraphs(LineBreaker.breakParagraphs(List.of(run("uno dos")), 8, measurer, F, 1, 0, TextWrap.SIMPLE)));
        assertEquals(List.of(List.of("uno", "dos")),
                paragraphs(LineBreaker.breakParagraphs(List.of(run("uno dos")), 8, measurer, F, 1, 1, TextWrap.SIMPLE)));
    }

    @Test
    void letterSpacingWorksWithKnuthPlass() {
        // Con interletraje 1 y ancho 9, entran dos palabras de dos letras (2+1 + 1+1 + 2... = 8) y no tres.
        List<List<LineBreaker.Line>> l = LineBreaker.breakParagraphs(List.of(run("ab cd ef gh")), 9, measurer, F, 1, 1,
                TextWrap.BALANCED);
        assertEquals(List.of(List.of("ab cd", "ef gh")), paragraphs(l));
    }

    @Test
    void smallCapsAreMeasuredAtTheirScale() {
        TextRun small = new TextRun("BC", TextFormat.PLAIN.withSmallCaps(true));
        // «A» mide 1 y «BC», 2 × 0,8.
        assertEquals(2.6, LineBreaker.width(List.of(run("A"), small), measurer, F, 1, 0), 1e-9);
        // Con interletraje, el de cada carácter es el mismo, también en las versalitas.
        assertEquals(2.6 + 2, LineBreaker.width(List.of(run("A"), small), measurer, F, 1, 1), 1e-9);
    }

    @Test
    void aPlacedLineDropsTheLastLetterSpacingAndAlignsWithIt() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.letterSpacing(2);
        s.textAlign(TextAlign.RIGHT);
        TextLine line = LineBreaker.place("ab", measurer, s.style(), 20, false, 0, 1);
        // a (1) + 2 + b (1) = 4, a la derecha de 20.
        assertEquals(4, line.width(), 1e-9);
        assertEquals(16, line.x(), 1e-9);
        assertEquals(4, line.runs().get(0).width(), 1e-9);
    }

    @Test
    void justifyDistributesWhatLetterSpacingLeaves() {
        StyleEditor s = new StyleEditor();
        s.font("F");
        s.fontSize(1);
        s.letterSpacing(1);
        s.textAlign(TextAlign.JUSTIFY);
        // «ab cd» mide 5 + 4 = 9; en 15 sobran 6 para un solo espacio.
        TextLine line = LineBreaker.place("ab cd", measurer, s.style(), 15, true, 0, 1);
        assertEquals(6, line.wordSpacing(), 1e-9);
        assertEquals(15, line.width(), 1e-9);
    }
}
