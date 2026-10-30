package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code White space: pre} y {@code preserve-wrap} y {@code Tab size} (referencia/comun/formato-del-texto.md). Con
 * {@link FixedMetrics}, cada carácter mide 5 pt con {@code Size: 10}, y cada línea, 12.
 */
class PreformattedTextTest {

    private static final FontFace FONT = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    private static final TextFormat BOLD = new TextFormat(true, false, false, false);

    /** Un {@code Text} de 100 pt de ancho con este contenido (una línea por argumento) y estas propiedades de {@code Typography}. */
    private static List<String> text(int width, List<String> typography, String... content) {
        List<String> doc = new ArrayList<>(List.of("\tText:", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: " + width,
                "\t\tTypography:"));
        typography.forEach(p -> doc.add("\t\t\t" + p));
        doc.add("\t\tContent >>");
        Arrays.stream(content).forEach(l -> doc.add(l.isEmpty() ? "" : "\t\t\t" + l));
        return doc;
    }

    private static List<String> lines(int width, List<String> typography, String... content) {
        return layOut(text(width, typography, content).toArray(String[]::new)).fragment("T").lines().stream()
                .map(TextLine::text).toList();
    }

    @Test
    void spacesAndLineBreaksAreKept() {
        assertEquals(List.of("a  b", "  c   d", "e"), lines(100, List.of("White space: preserve"), "a  b", "  c   d", "e"));
    }

    @Test
    void withoutItSpacesCollapseAsAlways() {
        assertEquals(List.of("a b c d e"), lines(100, List.of(), "a  b", "  c   d", "e"));
    }

    @Test
    void blankLinesAreLinesAndDoNotSeparateParagraphs() {
        LayoutFixture l = layOut(text(100, List.of("White space: preserve"), "a", "", "b").toArray(String[]::new));
        assertEquals(List.of("a", "", "b"), l.fragment("T").lines().stream().map(TextLine::text).toList());
        assertEquals(36, l.rect("T").height(), 1e-9);
    }

    @Test
    void tabsGoToTheNextTabStop() {
        assertEquals(List.of("a   b", "abcd    e", "ab  c"),
                lines(100, List.of("White space: preserve"), "a\tb", "abcd\te", "ab\tc"));
    }

    @Test
    void tabSizeSetsTheTabStops() {
        assertEquals(List.of("a" + " ".repeat(7) + "b"),
                lines(100, List.of("White space: preserve", "Tab size: 8"), "a\tb"));
    }

    @Test
    void aLineWiderThanTheBoxIsNotBrokenWithPre() {
        List<LineBreaker.Line> lines = PreformattedText.lines(TextRun.plain("una línea ancha\nb"), 4, false, 30,
                new FixedMetrics(), FONT, 10, 0);
        assertEquals(List.of("una línea ancha", "b"), lines.stream().map(LineBreaker.Line::text).toList());
    }

    @Test
    void preWrapBreaksBetweenWords() {
        assertEquals(List.of("aaaa bbbb cccc dddd", "eeee", "ff"),
                lines(100, List.of("White space: preserve-wrap"), "aaaa bbbb cccc dddd eeee", "ff"));
    }

    @Test
    void preWrapKeepsTheIndentationAndDropsTheSpacesAtTheBreak() {
        assertEquals(List.of("  aaaa bbbb cccc", "dddd"),
                lines(80, List.of("White space: preserve-wrap"), "  aaaa bbbb cccc   dddd"));
    }

    @Test
    void preWrapKeepsTheSpacesInsideALineThatFits() {
        assertEquals(List.of("aa    bb"), lines(100, List.of("White space: preserve-wrap"), "aa    bb"));
    }

    @Test
    void preformattedLinesAreNeverJustified() {
        // Con pre las líneas caben en la caja; con pre-wrap la primera es una línea partida, que sin pre se justificaría.
        for (String mode : List.of("preserve", "preserve-wrap")) {
            List<TextLine> lines = layOut(text(mode.equals("preserve") ? 200 : 100, List.of("White space: " + mode,
                    "Align: justify"), "aaaa bbbb cccc dddd eeee", "ff gg").toArray(String[]::new)).fragment("T").lines();
            assertEquals(0, lines.get(0).wordSpacing(), 1e-9, mode);
            assertEquals(0, lines.get(0).x(), 1e-9, mode);
        }
    }

    @Test
    void eachLineIsAlignedOnItsOwn() {
        List<TextLine> lines = layOut(text(100, List.of("White space: preserve", "Align: right"), "ab", "abcd")
                .toArray(String[]::new)).fragment("T").lines();
        assertEquals(90, lines.get(0).x(), 1e-9);
        assertEquals(80, lines.get(1).x(), 1e-9);
    }

    @Test
    void whiteSpaceAndTabSizeAreInherited() {
        LayoutFixture l = layOut("\tBlock:", "\t\tTypography:", "\t\t\tWhite space: preserve", "\t\t\tTab size: 2",
                "\t\tText: a\tb", "\t\t\tId: A",
                "\t\tText: a    b", "\t\t\tId: B", "\t\t\tTypography:", "\t\t\t\tWhite space: normal");
        assertEquals("a b", l.fragment("A").lines().get(0).text());
        assertEquals("a b", l.fragment("B").lines().get(0).text());
    }

    @Test
    void widthOfAnEmptyLineIsZeroEvenWithLetterSpacing() {
        List<TextLine> lines = layOut(text(100, List.of("White space: preserve", "Letter spacing: 2"), "ab", "", "ab")
                .toArray(String[]::new)).fragment("T").lines();
        assertEquals(0, lines.get(1).width(), 1e-9);
        assertEquals(12, lines.get(0).width(), 1e-9);
    }

    @Test
    void transformStillApplies() {
        assertEquals(List.of("A  B"), lines(100, List.of("White space: preserve", "Case: uppercase"), "a  b"));
    }

    @Test
    void anItemWithoutWidthIsAsWideAsItsWidestLine() {
        LayoutFixture l = layOut("\tFlex:", "\t\tItem:", "\t\t\tText:", "\t\t\t\tId: T",
                "\t\t\t\tTypography:", "\t\t\t\t\tWhite space: preserve",
                "\t\t\t\tContent >>", "\t\t\t\t\tab", "\t\t\t\t\tabcdef  ");
        // El espacio final de una línea de un bloque lo quita el parser: abcdef mide 30.
        assertEquals(30, l.rect("T").width(), 1e-9);
    }

    @Test
    void theWholeBlockIsOneParagraphForWidowsAndOrphans() {
        // 100 pt de área útil: caben 8 líneas. Con 9, la última sola sería una viuda: pasan 2.
        String[] nine = new String[9];
        Arrays.fill(nine, "x");
        LayoutFixture l = layOutPaged(180, text(100, List.of("White space: preserve"), nine).toArray(String[]::new));
        assertEquals(2, l.pages());
        assertEquals(7, l.fragment(1, "T").lines().size());
        assertEquals(2, l.fragment(2, "T").lines().size());
    }

    // --- Piezas puras ------------------------------------------------------------------------------

    private static List<List<TextRun>> source(int tabSize, TextRun... runs) {
        return PreformattedText.sourceLines(List.of(runs), tabSize);
    }

    @Test
    void aFinalLineBreakDoesNotAddAnEmptyLine() {
        assertEquals(1, source(4, new TextRun("a\n", TextFormat.PLAIN)).size());
        assertEquals(2, source(4, new TextRun("a\n\n", TextFormat.PLAIN)).size());
    }

    @Test
    void formatSurvivesTheLineBreaksAndTheColumnsCountAcrossRuns() {
        List<List<TextRun>> lines = source(4, new TextRun("ab", TextFormat.PLAIN), new TextRun("\tc\nd", BOLD),
                new TextRun("\te", TextFormat.PLAIN));
        assertEquals(2, lines.size());
        assertEquals(List.of(new TextRun("ab", TextFormat.PLAIN), new TextRun("  c", BOLD)), lines.get(0));
        assertEquals(List.of(new TextRun("d", BOLD), new TextRun("   e", TextFormat.PLAIN)), lines.get(1));
    }

    @Test
    void carriageReturnsAreIgnored() {
        assertEquals(List.of(List.of(new TextRun("a", TextFormat.PLAIN)), List.of(new TextRun("b", TextFormat.PLAIN))),
                source(4, new TextRun("a\r\nb", TextFormat.PLAIN)));
    }

    @Test
    void preWrapIntrinsicsAreTheWidestWordAndTheWidestLine() {
        List<TextRun> runs = TextRun.plain("ab cdef\ngh");
        IntrinsicSizes wrap = PreformattedText.intrinsics(runs, 4, true, new FixedMetrics(), FONT, 10, 0);
        assertEquals(20, wrap.minContent(), 1e-9);
        assertEquals(35, wrap.maxContent(), 1e-9);
        IntrinsicSizes pre = PreformattedText.intrinsics(runs, 4, false, new FixedMetrics(), FONT, 10, 0);
        assertEquals(35, pre.minContent(), 1e-9);
        assertTrue(pre.maxContent() >= pre.minContent());
    }

    @Test
    void severalTabsInALine() {
        // «a» llega a la columna 4, «bb» a la 6 y el segundo tabulador a la 8.
        assertEquals(List.of("a   bb  c"), lines(100, List.of("White space: preserve"), "a\tbb\tc"));
    }

    @Test
    void minContentOfPreAndPreWrap() {
        // pre no parte: su min-content es la línea entera (45); pre-wrap, la palabra más larga (25).
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tWidth: min-content",
                "\t\tText: aaa bbbbb", "\t\t\tTypography:", "\t\t\t\tWhite space: preserve",
                "\tBlock:", "\t\tId: W", "\t\tBox:", "\t\t\tWidth: min-content",
                "\t\tText: aaa bbbbb", "\t\t\tTypography:", "\t\t\t\tWhite space: preserve-wrap");
        assertEquals(45, l.rect("P").width(), 1e-9);
        assertEquals(25, l.rect("W").width(), 1e-9);
    }
}
