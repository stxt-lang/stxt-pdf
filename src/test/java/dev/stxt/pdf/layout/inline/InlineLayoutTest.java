package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.TextFormat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutRuns;
import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** With {@link FixedMetrics}, each character measures 5 pt with {@code Size: 10}. */
class InlineLayoutTest {

    private static final String TEXT = "aaaa bbbb cccc dddd eeee";

    /**
     * Lines of the text with a width of 100 and these properties in its {@code Typography} ({@code "Align: right"}).
     */
    private static List<TextLine> lines(String... typography) {
        List<String> doc = new ArrayList<>(List.of("\tText: " + TEXT, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100",
                "\t\tTypography:"));
        for (String p : typography) {
            doc.add("\t\t\t" + p);
        }
        return layOut(doc.toArray(String[]::new)).fragment("T").lines();
    }

    @Test
    void linesAndHeight() {
        LayoutFixture l = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100");
        Fragment f = l.fragment("T");
        assertEquals(2, f.lines().size());
        assertEquals(new TextLine(0, 0, 95, 12, 10, "aaaa bbbb cccc dddd"), f.lines().get(0));
        assertEquals(new TextLine(0, 12, 20, 12, 10, "eeee"), f.lines().get(1));
        assertEquals(24, f.height(), 1e-9);
    }

    @Test
    void lineBreakingBalancedIsInheritedAndUsesKnuthPlass() {
        // The example of LineBreakerTest.prettyMinimizesTheDemerits at scale 5: width 50.
        String text = "a bbb c ddddd e fff";
        LayoutFixture l = layOut("\tBlock:", "\t\tTypography:", "\t\t\tLine breaking: balanced",
                "\t\tText: " + text, "\t\t\tId: P", "\t\t\tBox:", "\t\t\t\tWidth: 50",
                "\t\tText: " + text, "\t\t\tId: G", "\t\t\tBox:", "\t\t\t\tWidth: 50", "\t\t\tTypography:",
                "\t\t\t\tLine breaking: simple");
        assertEquals(List.of("a bbb", "c ddddd e", "fff"),
                l.fragment("P").lines().stream().map(TextLine::text).toList());
        assertEquals(List.of("a bbb c", "ddddd e", "fff"),
                l.fragment("G").lines().stream().map(TextLine::text).toList());
    }

    @Test
    void justifyFillsAllLinesButTheLast() {
        // First line: 95 pt and 3 spaces, the leftover (5) is distributed among them.
        LayoutFixture l = layOut("\tText:", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100", "\t\tTypography:",
                "\t\t\tAlign: justify", "\t\tContent >>", "\t\t\t" + TEXT);
        List<TextLine> lines = l.fragment("T").lines();
        assertEquals(2, lines.size());
        assertEquals(new TextLine(0, 0, 100, 12, 10, "aaaa bbbb cccc dddd", 5.0 / 3), lines.get(0));
        assertEquals(new TextLine(0, 12, 20, 12, 10, "eeee", 0), lines.get(1));
    }

    @Test
    void paddingReducesTheLineWidth() {
        LayoutFixture l = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tPadding: 10");
        Fragment f = l.fragment("T");
        assertEquals(List.of("aaaa bbbb cccc", "dddd eeee"), f.lines().stream().map(TextLine::text).toList());
        assertEquals(44, f.height(), 1e-9);
    }

    @Test
    void alignment() {
        List<TextLine> right = lines("Align: right");
        assertEquals(5, right.get(0).x(), 1e-9);
        assertEquals(80, right.get(1).x(), 1e-9);
        List<TextLine> center = lines("Align: center");
        assertEquals(2.5, center.get(0).x(), 1e-9);
        assertEquals(40, center.get(1).x(), 1e-9);
    }

    @Test
    void lineHeightInPointsOrPercentage() {
        assertEquals(15, lines("Line height: 15").get(1).y(), 1e-9);
        assertEquals(15, lines("Line height: 150%").get(1).y(), 1e-9);
        assertEquals(28, lines("Size: 20", "Line height: 140%").get(1).height(), 1e-9);
    }

    @Test
    void aLineBreakInContentIsASpace() {
        LayoutFixture l = layOut("\tText:", "\t\tId: T", "\t\tContent >>", "\t\t\tuna dos", "\t\t\ttres");
        assertEquals(List.of("una dos tres"),
                l.fragment("T").lines().stream().map(TextLine::text).toList());
    }

    @Test
    void textMixedWithBoxesGoesInAnAnonymousBox() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P",
                "\t\tText: hola", "\t\tBlock:", "\t\t\tId: C", "\t\t\tBox:", "\t\t\t\tHeight: 10");
        assertEquals(12, l.rect("C").y() - 40, 1e-9);
        assertEquals(22, l.rect("P").height(), 1e-9);
    }

    @Test
    void runsArePlacedOneAfterTheOther() {
        // "uno " (20) + "dos" (15) + " tres" (25) = 60, centered in 100.
        TextLine l = layOutRuns(Map.of("T", List.of(run("uno "), run("dos", "b"), run(" tres"))),
                "\tText: uno dos tres", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100", "\t\tTypography:",
                "\t\t\tAlign: center").fragment("T").lines().get(0);
        assertEquals(20, l.x(), 1e-9);
        assertEquals(List.of(new TextLine.Run(0, 20, "uno ", TextFormat.PLAIN),
                new TextLine.Run(20, 15, "dos", run("dos", "b").format()),
                new TextLine.Run(35, 25, " tres", TextFormat.PLAIN)), l.runs());
    }

    @Test
    void pageNumbersKeepTheirRun() {
        TextLine l = layOutRuns(Map.of("T", List.of(run("Página "), run("{page}", "b"), run(" de {pages}"))),
                "\tText: Página {page} de {pages}", "\t\tId: T").fragment("T").lines().get(0);
        assertEquals("Página 1 de 1", l.text());
        assertEquals(List.of(new TextLine.Run(0, 35, "Página ", TextFormat.PLAIN),
                new TextLine.Run(35, 5, "1", run("1", "b").format()),
                new TextLine.Run(40, 25, " de 1", TextFormat.PLAIN)), l.runs());
    }

    @Test
    void theLastLineOfAParagraphIsNotJustifiedEvenWithSeveralWords() {
        // "aa bb" (25) is justified to 30; "cc dd", the last one, is not.
        LayoutFixture l = layOut("\tText:", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 30", "\t\tTypography:",
                "\t\t\tAlign: justify", "\t\tContent >>", "\t\t\taa bb cc dd");
        List<TextLine> lines = l.fragment("T").lines();
        assertEquals(List.of("aa bb", "cc dd"), lines.stream().map(TextLine::text).toList());
        assertEquals(5, lines.get(0).wordSpacing(), 1e-9);
        assertEquals(0, lines.get(1).wordSpacing(), 1e-9);
    }

    // Overflow: shrink (reference/common/overflow.md): the Size goes down in steps of 0.5 until the text fits.

    @Test
    void shrinkReducesTheSizeUntilTheWidestWordFits() {
        // Twelve characters: 60 pt at Size 10, 51 at 8.5 and 48 at 8, the first size at which they fit in 50.
        Fragment f = layOut("\tText: aaaaaaaaaaaa", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tOverflow: shrink")
                .fragment("T");
        assertEquals(1, f.lines().size());
        TextLine l = f.lines().get(0);
        assertEquals(8, l.fontSize(), 1e-9);
        assertEquals(48, l.width(), 1e-9);
        // The default line height (120%) follows the size.
        assertEquals(9.6, l.height(), 1e-9);
        assertEquals(9.6, f.height(), 1e-9);
    }

    @Test
    void shrinkReducesTheSizeUntilTheLinesFitTheHeight() {
        // Two lines (24 pt) at Size 10 do not fit in 20; at 8 the 24 characters fit in one line (96) of 9.6.
        Fragment f = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tHeight: 20",
                "\t\t\tOverflow: shrink").fragment("T");
        assertEquals(List.of(TEXT), f.lines().stream().map(TextLine::text).toList());
        assertEquals(8, f.lines().get(0).fontSize(), 1e-9);
        assertEquals(20, f.height(), 1e-9);
    }

    @Test
    void shrinkKeepsALineHeightInPoints() {
        // Line height: 14 is a length and does not follow the size: the text needs one line of 14, which it has at
        // Size 6 (24 characters, 72 pt).
        Fragment f = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 72", "\t\t\tHeight: 14",
                "\t\t\tOverflow: shrink", "\t\tTypography:", "\t\t\tLine height: 14").fragment("T");
        assertEquals(1, f.lines().size());
        assertEquals(6, f.lines().get(0).fontSize(), 1e-9);
        assertEquals(14, f.lines().get(0).height(), 1e-9);
    }

    @Test
    void shrinkTriesMinSizeAsTheLastStep() {
        // Ten characters in 39.5: 40 at Size 8 does not fit, and the next step, 7.5, is below Min size 7.9, which is
        // tried instead: 39.5.
        Fragment f = layOut("\tText: aaaaaaaaaa", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 39.5", "\t\t\tOverflow: shrink",
                "\t\tTypography:", "\t\t\tMin size: 7.9").fragment("T");
        assertEquals(7.9, f.lines().get(0).fontSize(), 1e-9);
    }

    @Test
    void aTextThatFitsIsNotShrunk() {
        Fragment f = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tOverflow: shrink")
                .fragment("T");
        assertEquals(2, f.lines().size());
        assertEquals(10, f.lines().get(0).fontSize(), 1e-9);
    }

    @Test
    void shrinkAlsoFitsAMaxHeight() {
        // Without Height the box grows with its text, but Max height: 20 limits it: as with Height 20.
        Fragment f = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tMax height: 20",
                "\t\t\tOverflow: shrink").fragment("T");
        assertEquals(1, f.lines().size());
        assertEquals(8, f.lines().get(0).fontSize(), 1e-9);
        assertEquals(9.6, f.height(), 1e-9);
    }
}
