package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Markers of lists (reference/components/list.md). With {@code FixedMetrics}, each character
 * measures 5 pt with {@code Size: 10}, so {@code "1. "} measures 15; the line measures 12, and the
 * baseline, with the 0.8/0.2 approximation, is 9 pt below its top.
 */
class ListLayoutTest {

    private static final String[] LIST_PADDING = {"\tStyles:", "\t\tFor element: List", "\t\t\tBox:",
            "\t\t\t\tPadding: 0 0 0 20"};

    private static String[] doc(String... lines) {
        String[] all = new String[LIST_PADDING.length + lines.length];
        System.arraycopy(LIST_PADDING, 0, all, 0, LIST_PADDING.length);
        System.arraycopy(lines, 0, all, LIST_PADDING.length, lines.length);
        return all;
    }

    private static void assertRect(Rect r, double x, double y, double w, double h) {
        assertEquals(x, r.x(), 1e-9, "x");
        assertEquals(y, r.y(), 1e-9, "y");
        assertEquals(w, r.width(), 1e-9, "ancho");
        assertEquals(h, r.height(), 1e-9, "alto");
    }

    @Test
    void markerHangsLeftOfTheItemOnItsFirstLine() {
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tId: A", "\t\t\tText: Uno",
                "\t\tList item:", "\t\t\tId: B", "\t\t\tText: Dos"));
        // The indent (20) is in the padding of the list: the items start at 60.
        assertRect(f.rect("A"), 60, 40, 495, 12);
        assertRect(f.marker("A"), 45, 40, 15, 12);
        assertRect(f.marker("B"), 45, 52, 15, 12);
    }

    @Test
    void markersOfDifferentWidthAlignRight() {
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: decimal", "\t\tStart: 9",
                "\t\tList item:", "\t\t\tId: A", "\t\t\tText: Nueve",
                "\t\tList item:", "\t\t\tId: B", "\t\t\tText: Diez"));
        assertRect(f.marker("A"), 45, 40, 15, 12);
        assertRect(f.marker("B"), 40, 52, 20, 12);
    }

    @Test
    void markerSharesTheBaselineOfABiggerFirstLine() {
        // Paragraph of 20 pt: line of 24 and baseline at 40 + 2 + 16 = 58. The marker, of
        // 10 pt, has its baseline 9 pt below its top: it starts at 49.
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: bullet",
                "\t\tList item:", "\t\t\tId: A", "\t\t\tParagraph: Grande",
                "\t\t\t\tTypography:", "\t\t\t\t\tSize: 20"));
        assertRect(f.marker("A"), 50, 49, 10, 12);
    }

    @Test
    void markerUsesTheTypographyOfTheItem() {
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: dash",
                "\t\tList item:", "\t\t\tId: A",
                "\t\t\tTypography:", "\t\t\t\tSize: 8", "\t\t\t\tLine height: 12",
                "\t\t\tText: Uno"));
        // "– " at 8 pt measures 8; with the line of 12, the baseline goes at 12/2 − 4 + 6.4 = 8.4 from
        // the top in both, so the marker starts at the height of the line.
        assertRect(f.marker("A"), 52, 40, 8, 12);
    }

    @Test
    void borderAndPaddingOfTheItemDoNotMoveTheMarker() {
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tId: A", "\t\t\tBox:", "\t\t\t\tBorder: 1", "\t\t\t\tPadding: 3",
                "\t\t\tText: Uno"));
        // Against the border box of the item; the first line starts at 40 + 1 + 3.
        assertRect(f.marker("A"), 45, 44, 15, 12);
    }

    @Test
    void nestedListMarkersShareTheFirstLine() {
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tId: A",
                "\t\t\tList:", "\t\t\t\tMarker: bullet",
                "\t\t\t\tList item:", "\t\t\t\t\tId: B", "\t\t\t\t\tText: Dentro"));
        assertRect(f.marker("A"), 45, 40, 15, 12);
        assertRect(f.rect("B"), 80, 40, 475, 12);
        assertRect(f.marker("B"), 70, 40, 10, 12);
    }

    @Test
    void firstLineIsFoundThroughBlocks() {
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: bullet",
                "\t\tList item:", "\t\t\tId: A",
                "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tPadding: 6 0 0 0",
                "\t\t\t\tText: Uno"));
        assertRect(f.marker("A"), 50, 46, 10, 12);
    }

    @Test
    void noneHasNoMarker() {
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: none",
                "\t\tList item:", "\t\t\tId: A", "\t\t\tText: Uno"));
        assertFalse(f.hasMarker(1, "A"));
    }

    @Test
    void markerMovesWithTheFirstLineToTheNextPage() {
        // Page area of 100: after the block of 90 no line of the item fits.
        LayoutFixture f = LayoutFixture.layOutPaged(180, doc("\tBlock:", "\t\tBox:", "\t\t\tHeight: 90",
                "\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tId: A", "\t\t\tText: Uno"));
        assertFalse(f.has(1, "A"));
        assertRect(f.marker(2, "A"), 45, 40, 15, 12);
    }

    @Test
    void aSplitItemHasItsMarkerOnlyInTheFirstFragment() {
        // Item of 100 − 20 = 80 pt: 16 characters per line, 3 words of 4; 15 words are
        // 5 lines. After the block of 60, 3 fit (orphans and widows 2).
        LayoutFixture f = LayoutFixture.layOutPaged(180, doc("\tBlock:", "\t\tBox:", "\t\t\tHeight: 60",
                "\tList:", "\t\tMarker: decimal", "\t\tBox:", "\t\t\tWidth: 100",
                "\t\tList item:", "\t\t\tId: A",
                "\t\t\tText: aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa aaaa"));
        assertRect(f.rect(1, "A"), 60, 100, 80, 36);
        assertRect(f.marker(1, "A"), 45, 100, 15, 12);
        assertTrue(f.has(2, "A"));
        assertFalse(f.hasMarker(2, "A"));
    }

    @Test
    void markerFollowsTheFirstLineInsideABorderedChild() {
        // The first line is in the content of a block with a border of 3 and padding of 5: at 40 + 8.
        LayoutFixture f = LayoutFixture.layOut(doc("\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tId: A", "\t\t\tBlock:", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 3",
                "\t\t\t\t\tPadding: 5", "\t\t\t\tText: Uno"));
        assertRect(f.marker("A"), 45, 48, 15, 12);
    }
}
