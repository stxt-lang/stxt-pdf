package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Drop caps (reference/components/text.md). Page area of 515 from (40, 40); lines of 12 (Size 10), 5 pt per
 * character and capitals of 0.7 of the size ({@link FixedMetrics}): a drop cap of 3 lines spans 2 × 12 + 7 = 31 and
 * is set at 31 / 0.7 = 44.29; its letter measures 22.14 and takes a space of the paragraph (5) more.
 */
class DropCapLayoutTest {

    private static String words(int n) {
        return "X" + "aaaa ".repeat(n).strip().substring(1);
    }

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    @Test
    void theDropCapSpansItsLinesAndShortensThem() {
        LayoutFixture l = layOut("\tParagraph: " + words(100), "\t\tId: P", "\t\tDrop cap: 3");
        List<TextLine> lines = l.fragment("P").lines();
        TextLine drop = lines.get(0);
        assertEquals("X", drop.text());
        assertEquals(31 / 0.7, drop.fontSize(), 1e-9);
        assertEquals(0.5 * 31 / 0.7, drop.width(), 1e-9);
        assertEquals(0, drop.x(), 1e-9);
        // Its baseline is that of the third line: 24 + (12 − 10) / 2 + 8 = 33, which at its size is 19.29 below its top.
        double size = 31 / 0.7;
        assertEquals(33 - ((12 - size) / 2 + 0.8 * size), drop.y(), 1e-9);
        // The first three lines start after it; the fourth has the whole width.
        double taken = 0.5 * size + 5;
        assertEquals(taken, lines.get(1).left(), 1e-9);
        assertEquals(taken, lines.get(3).left(), 1e-9);
        assertEquals(0, lines.get(4).left(), 1e-9);
        // The first word lost its letter: "aaa".
        assertEquals("aaa", lines.get(1).text().split(" ")[0]);
        // 515 − 27.14 = 487.86: 19 words fit (465 with the first one of 15, 470 after it); then 20.
        assertEquals(19, lines.get(1).text().split(" ").length);
        assertEquals(19, lines.get(3).text().split(" ").length);
        assertEquals(20, lines.get(4).text().split(" ").length);
    }

    @Test
    void aShortParagraphIsAsTallAsItsDropCap() {
        LayoutFixture l = layOut("\tParagraph: " + words(3), "\t\tId: P", "\t\tDrop cap: 2",
                "\tParagraph: " + words(3), "\t\tId: Q");
        assertRect(40, 40, 515, 24, l.rect("P"));
        assertRect(40, 64, 515, 12, l.rect("Q"));
        assertEquals(2, l.fragment("P").lines().size());
    }

    @Test
    void theDropCapAndItsLinesMoveWholeToTheNextPage() {
        // Page area of 100: the Block leaves 20, one line, and the drop cap spans two.
        LayoutFixture l = layOutPaged(180, "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 80",
                "\tParagraph: " + words(50), "\t\tId: P", "\t\tDrop cap: 2");
        assertEquals(2, l.pages());
        assertFalse(l.has(1, "P"));
        assertRect(40, 40, 515, 36, l.rect(2, "P"));
        assertEquals("X", l.fragment(2, "P").lines().get(0).text());
    }

    @Test
    void theDropCapGoesAfterASideBox() {
        LayoutFixture l = layOut("\tImage: test/square.png", "\t\tSide: left", "\t\tBox:", "\t\t\tWidth: 60",
                "\t\t\tHeight: 60", "\t\t\tMargin: 0 10 0 0",
                "\tParagraph: " + words(60), "\t\tId: P", "\t\tDrop cap: 2");
        List<TextLine> lines = l.fragment("P").lines();
        double size = 19 / 0.7;
        assertEquals(70, lines.get(0).x(), 1e-9);
        assertEquals(70 + 0.5 * size + 5, lines.get(1).left(), 1e-9);
        assertEquals(70, lines.get(3).left(), 1e-9);
    }
}
