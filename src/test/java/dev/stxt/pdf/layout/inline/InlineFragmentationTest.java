package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.TextFormat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPagedRuns;
import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pages of 180 pt with margins of 40: page area of 515 × 100 from (40, 40). With
 * {@code Width: 30} each word of 5 letters (25 pt) takes up a line of 12 pt: 8 fit per page.
 */
class InlineFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String words(int n) {
        return String.join(" ", java.util.Collections.nCopies(n, "aaaaa"));
    }

    private static int lines(LayoutFixture l, int page, String id) {
        return l.fragment(page, id).lines().size();
    }

    @Test
    void paragraphFillsThePage() {
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(10), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(2, l.pages());
        assertRect(40, 40, 30, 96, l.rect(1, "P"));
        assertRect(40, 40, 30, 24, l.rect(2, "P"));
        assertEquals(0, l.fragment(2, "P").lines().get(0).y());
    }

    @Test
    void widowsTakeALineToTheNextPage() {
        // 8 + 1 would leave a widow: 7 + 2.
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(9), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(7, lines(l, 1, "P"));
        assertEquals(2, lines(l, 2, "P"));
    }

    @Test
    void orphanMovesTheWholeParagraph() {
        // Only one line fits: it would be an orphan, and breaking before the paragraph violates nothing.
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 80",
                "\tText: " + words(9), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(3, l.pages());
        assertFalse(l.has(1, "P"));
        assertEquals(7, lines(l, 2, "P"));
        assertEquals(2, lines(l, 3, "P"));
    }

    @Test
    void orphansAreRelaxedBeforeKeeps() {
        // Breaking before P would violate the Keep with next of T; leaving an orphan is less serious
        // (reference/common/breaks.md).
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: T", "\t\tBox:", "\t\t\tHeight: 80", "\t\tBreak:", "\t\t\tKeep with next: true",
                "\tText: " + words(9), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(2, l.pages());
        assertRect(40, 120, 30, 12, l.rect(1, "P"));
        assertRect(40, 40, 30, 96, l.rect(2, "P"));
    }

    @Test
    void breakBetweenParagraphs() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 30",
                "\tBlock:", "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30", "\t\tParagraph: " + words(5),
                "\t\tParagraph: " + words(5));
        assertEquals(2, l.pages());
        assertRect(40, 70, 30, 60, l.rect(1, "P"));
        assertRect(40, 40, 30, 60, l.rect(2, "P"));
    }

    // A line taller than the page cannot be split: it is an error (error catalog).

    @Test
    void runsContinueOnTheNextPage() {
        // The ninth word, in bold, opens page 2.
        LayoutFixture l = layOutPagedRuns(PAGE,
                Map.of("P", List.of(run(words(8) + " "), run("aaaaa", "b"), run(" aaaaa"))),
                "\tText: " + words(10), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30");
        assertEquals(8, lines(l, 1, "P"));
        List<TextLine> lines = l.fragment(2, "P").lines();
        assertEquals(List.of(new TextLine.Run(0, 25, "aaaaa", run("aaaaa", "b").format())), lines.get(0).runs());
        assertEquals(List.of(new TextLine.Run(0, 25, "aaaaa", TextFormat.PLAIN)), lines.get(1).runs());
    }

    /**
     * A text with Height: 150 reaches the end of page 1 (100), but its 8 lines only consume 96: on page 2 it has
     * 54 left for the 2 lines that follow (reference/common/breaks.md).
     */
    @Test
    void theHeightOfATextIsTheTotal() {
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(10), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30",
                "\t\t\tHeight: 150");
        assertEquals(2, l.pages());
        assertRect(40, 40, 30, 100, l.rect(1, "P"));
        assertRect(40, 40, 30, 54, l.rect(2, "P"));
    }

    /**
     * Height equal to the content (20 lines, 240) on pages of 100: each page has 8 lines (96) and the gap of 4
     * at the end is not taken from the text, so the last 4 fit on page 3. It used to be BOX_OVERFLOW: the box
     * consumed 100 per page and had 40 left for 48 of lines.
     */
    @Test
    void aHeightEqualToTheContentIsNotAnOverflowWhenItBreaks() {
        LayoutFixture l = layOutPaged(PAGE, "\tText: " + words(20), "\t\tId: P", "\t\tBox:", "\t\t\tWidth: 30",
                "\t\t\tHeight: 240",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 240",
                "\t\tText: " + words(20), "\t\t\tId: Q", "\t\t\tBox:", "\t\t\t\tWidth: 30");
        assertRect(40, 40, 30, 100, l.rect(1, "P"));
        assertRect(40, 40, 30, 100, l.rect(2, "P"));
        assertRect(40, 40, 30, 48, l.rect(3, "P"));
        assertEquals(4, lines(l, 3, "P"));
        // A Block with the same height and the text inside, after it (it starts on page 3): it does not overflow
        // either, and its 20 lines are in its fragments.
        int total = 0;
        for (int page = 3; page <= l.pages(); page++) {
            total += l.has(page, "Q") ? lines(l, page, "Q") : 0;
        }
        assertEquals(20, total);
    }
}
