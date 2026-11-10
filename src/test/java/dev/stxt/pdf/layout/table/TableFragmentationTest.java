package dev.stxt.pdf.layout.table;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Repeated header and footer (reference/components/table.md). Pages of 180 pt with margins of 40: page
 * area of 515 × 100 from (40, 40). The tables have one column and cells of fixed height, and, unless the test says
 * otherwise, {@code Split rows: false}: a body row that does not fit moves whole, which is what these cases are about.
 */
class TableFragmentationTest {

    private static final double PAGE = 180;

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(), new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String[] table(String gap) {
        return concat(splitTable(gap), new String[]{"\t\tSplit rows: false"});
    }

    /** A table with {@code Split rows} by default. */
    private static String[] splitTable(String gap) {
        return gap == null
                ? new String[]{"\tTable:", "\t\tId: T", "\t\tColumn sizes: *"}
                : new String[]{"\tTable:", "\t\tId: T", "\t\tColumn sizes: *", "\t\tGap: " + gap};
    }

    private static String[] group(String name) {
        return new String[]{"\t\t" + name + ":"};
    }

    private static String[] row(String id, int height, String... rowLines) {
        String[] head = {"\t\t\tRow:"};
        String[] cell = {"\t\t\t\tCell:", "\t\t\t\t\tId: " + id, "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: " + height};
        return concat(head, rowLines, cell);
    }

    private static String[] concat(String[]... parts) {
        return Arrays.stream(parts).flatMap(Arrays::stream).toArray(String[]::new);
    }

    @Test
    void headerAndFooterRowsRepeatOnEveryPage() {
        // The header (20) and the footer (10) leave 70 for the body: two rows of 30 per page. The footer goes
        // after the last row placed.
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Header rows"), row("H", 20),
                group("Body rows"), row("B1", 30), row("B2", 30), row("B3", 30), row("B4", 30),
                group("Footer rows"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 90, l.rect(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(1, "H"));
        assertRect(40, 90, 515, 30, l.rect(1, "B2"));
        assertRect(40, 120, 515, 10, l.rect(1, "F"));
        l.assertAbsent(1, "B3");
        assertRect(40, 40, 515, 90, l.rect(2, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B3"));
        assertRect(40, 120, 515, 10, l.rect(2, "F"));
    }

    /**
     * The band of a row with {@code Id} or {@code Box} (reference/components/table.md#rows) goes from the first
     * column to the last, with the gap between them, and has the height of its row; the gap between rows belongs to
     * none.
     */
    @Test
    void theBandOfARowSpansItsColumnsAndItsHeight() {
        LayoutFixture l = LayoutFixture.layOut("\tTable:", "\t\tId: T", "\t\tColumn sizes: 100 100", "\t\tGap: 10",
                "\t\tBody rows:",
                "\t\t\tRow:", "\t\t\t\tId: R1", "\t\t\t\tCell:", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 20",
                "\t\t\t\tCell:",
                "\t\t\tRow:", "\t\t\t\tBox:", "\t\t\t\t\tBackground: #EEEEEE", "\t\t\t\tId: R2",
                "\t\t\t\tCell:", "\t\t\t\t\tId: C", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 30",
                "\t\t\t\tCell:");
        assertRect(40, 40, 210, 20, l.rect("R1"));
        assertRect(40, 70, 210, 30, l.rect("R2"));
        assertRect(40, 70, 100, 30, l.rect("C"));
        // It goes before its cells in the fragment of the table, to be painted underneath.
        assertEquals("R2", firstChildWithId(l, "R2", "C"));
    }

    /** The first of the two Ids that appears among the children of the fragment of the table. */
    private static String firstChildWithId(LayoutFixture l, String a, String b) {
        return l.fragment(1, "T").children().stream().map(f -> f.box().id())
                .filter(id -> a.equals(id) || b.equals(id)).findFirst().orElseThrow();
    }

    /** The bands of the header and of the footer repeat on each page, like their cells. */
    @Test
    void bandsOfHeaderAndFooterRowsRepeat() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Header rows"), row("H", 20, "\t\t\t\tId: HR"),
                group("Body rows"), row("B1", 30), row("B2", 30, "\t\t\t\tId: R2"), row("B3", 30, "\t\t\t\tId: R3"),
                row("B4", 30),
                group("Footer rows"), row("F", 10, "\t\t\t\tId: FR")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 20, l.rect(1, "HR"));
        assertRect(40, 90, 515, 30, l.rect(1, "R2"));
        assertRect(40, 120, 515, 10, l.rect(1, "FR"));
        l.assertAbsent(1, "R3");
        assertRect(40, 40, 515, 20, l.rect(2, "HR"));
        assertRect(40, 60, 515, 30, l.rect(2, "R3"));
        assertRect(40, 120, 515, 10, l.rect(2, "FR"));
    }

    /**
     * A row that does not fit on a page is split, and its band with it: up to the end of its cell on the first page,
     * which reaches the end of the page area, and from the top on the next one, without the border at the break (with
     * {@code Edges: slice}).
     */
    @Test
    void theBandOfASplitRowIsSplitToo() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null), group("Body rows"),
                new String[]{"\t\t\tRow:", "\t\t\t\tId: R", "\t\t\t\tBox:", "\t\t\t\t\tBorder: 1",
                        "\t\t\t\tCell:", "\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 60",
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 60"}));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "R"));
        assertEquals(0, l.fragment(1, "R").border().bottom());
        assertEquals(1, l.fragment(1, "R").border().top());
        // The cell puts 60 + 40 on the first page and the remaining 20 on the second.
        assertRect(40, 40, 515, 20, l.rect(2, "R"));
        assertEquals(0, l.fragment(2, "R").border().top());
        assertEquals(1, l.fragment(2, "R").border().bottom());
    }

    @Test
    void gapsSeparateHeaderAndFooterRowsFromTheBody() {
        // With a gap of 5, the header takes up 25 and the footer 15: 60 remain, and B1 (30) and B2 (which ends
        // at 65) do not fit together.
        LayoutFixture l = layOutPaged(PAGE, concat(table("5"),
                group("Header rows"), row("H", 20),
                group("Body rows"), row("B1", 30), row("B2", 30),
                group("Footer rows"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 65, 515, 30, l.rect(1, "B1"));
        assertRect(40, 100, 515, 10, l.rect(1, "F"));
        assertRect(40, 40, 515, 70, l.rect(1, "T"));
        assertRect(40, 65, 515, 30, l.rect(2, "B2"));
        assertRect(40, 100, 515, 10, l.rect(2, "F"));
    }

    @Test
    void tableThatDoesNotFitMovesWithItsHeaderRows() {
        // After X (50), the header, the first row and the footer (60) do not fit: the whole table moves
        // to page 2, without leaving the header alone.
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tBlock:", "\t\tId: X", "\t\tBox:", "\t\t\tHeight: 50"}, table(null),
                group("Header rows"), row("H", 20), group("Body rows"), row("B1", 30), group("Footer rows"), row("F", 10)));
        assertEquals(2, l.pages());
        l.assertAbsent(1, "T");
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B1"));
    }

    @Test
    void tallBodyRowBreaksBetweenHeaderAndFooterRows() {
        // B1 (100) does not fit in the 70 left by the header and the footer: it is split, 70 + 30, and the
        // header and the footer repeat around each part.
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Header rows"), row("H", 20), group("Body rows"), row("B1", 100), group("Footer rows"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 60, 515, 70, l.rect(1, "B1"));
        assertRect(40, 130, 515, 10, l.rect(1, "F"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B1"));
        assertRect(40, 90, 515, 10, l.rect(2, "F"));
    }

    @Test
    void rowBreakForcesAPageWithTheHeadRepeated() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Header rows"), row("H", 20),
                group("Body rows"), row("B1", 30), row("B2", 30, "\t\t\t\tBreak:", "\t\t\t\t\tBefore: page")));
        assertEquals(2, l.pages());
        l.assertAbsent(1, "B2");
        assertRect(40, 40, 515, 50, l.rect(1, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 30, l.rect(2, "B2"));
    }

    @Test
    void withoutHeaderAndFooterRowsItBreaksLikeAGrid() {
        LayoutFixture l = layOutPaged(PAGE, concat(table(null),
                group("Body rows"), row("B1", 40), row("B2", 40), row("B3", 40)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "T"));
        assertRect(40, 40, 515, 40, l.rect(2, "B3"));
    }

    @Test
    void bodyRowThatDoesNotFitIsSplitWithTheFooterAfterIt() {
        // Split rows by default: B3 (60 to 90 of the 70 the header and the footer leave) is split, 10 + 20, and the
        // footer goes after each part.
        LayoutFixture l = layOutPaged(PAGE, concat(splitTable(null),
                group("Header rows"), row("H", 20),
                group("Body rows"), row("B1", 30), row("B2", 30), row("B3", 30),
                group("Footer rows"), row("F", 10)));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 100, l.rect(1, "T"));
        assertRect(40, 120, 515, 10, l.rect(1, "B3"));
        assertRect(40, 130, 515, 10, l.rect(1, "F"));
        assertRect(40, 40, 515, 50, l.rect(2, "T"));
        assertRect(40, 40, 515, 20, l.rect(2, "H"));
        assertRect(40, 60, 515, 20, l.rect(2, "B3"));
        assertRect(40, 80, 515, 10, l.rect(2, "F"));
    }

    @Test
    void keepTogetherOnARowMovesItWhole() {
        // The Break of a Row is that of its cells: B3 cannot be split, and the break before it is better.
        LayoutFixture l = layOutPaged(PAGE, concat(splitTable(null),
                group("Body rows"), row("B1", 40), row("B2", 40),
                row("B3", 40, "\t\t\t\tBreak:", "\t\t\t\t\tKeep together: true")));
        assertEquals(2, l.pages());
        assertRect(40, 40, 515, 80, l.rect(1, "T"));
        l.assertAbsent(1, "B3");
        assertRect(40, 40, 515, 40, l.rect(2, "B3"));
    }

    /** A body row with one cell, without a height of its own, that holds {@code children}. */
    private static String[] contentRow(String[]... children) {
        return concat(new String[]{"\t\t\tRow:", "\t\t\t\tCell:", "\t\t\t\t\tId: B"}, concat(children));
    }

    private static String[] child(String id, int height, String... lines) {
        return concat(new String[]{"\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: " + id, "\t\t\t\t\t\tBox:",
                "\t\t\t\t\t\t\tHeight: " + height}, lines);
    }

    @Test
    void splitRowWhoseLastMarginDoesNotFitGoesOnToTheNextPage() {
        // The header (20) and the footer (10) leave 70 for the body. The cell, stretched to its row, holds C (130)
        // with a bottom margin of 15: 70 on page 1 and, on page 2, the 60 left fit but the margin does not. C is
        // broken inside with the margin reserved, 55, and the row ends on page 3 with 5 and the margin. The footer
        // stays inside every page.
        LayoutFixture l = layOutPaged(PAGE, concat(splitTable(null),
                group("Header rows"), row("H", 20),
                group("Body rows"), contentRow(child("C", 130, "\t\t\t\t\t\t\tMargin: 0 0 15 0")),
                group("Footer rows"), row("F", 10)));
        assertEquals(3, l.pages());
        assertRect(40, 60, 515, 70, l.rect(1, "C"));
        assertRect(40, 130, 515, 10, l.rect(1, "F"));
        assertRect(40, 60, 515, 70, l.rect(2, "B"));
        assertRect(40, 60, 515, 55, l.rect(2, "C"));
        assertRect(40, 130, 515, 10, l.rect(2, "F"));
        assertRect(40, 60, 515, 20, l.rect(3, "B"));
        assertRect(40, 60, 515, 5, l.rect(3, "C"));
        assertRect(40, 80, 515, 10, l.rect(3, "F"));
        assertRect(40, 40, 515, 50, l.rect(3, "T"));
    }

    @Test
    void splitRowBreaksBeforeTheLastChildWhoseMarginDoesNotFit() {
        // A (80) leaves 10 for page 2, where C (50) fits after it but its bottom margin (15) does not: the cell
        // contains that margin although the row gives it its height, so the break goes before C.
        LayoutFixture l = layOutPaged(PAGE, concat(splitTable(null),
                group("Header rows"), row("H", 20),
                group("Body rows"), contentRow(child("A", 80), child("C", 50, "\t\t\t\t\t\t\tMargin: 0 0 15 0",
                        "\t\t\t\t\t\tBreak:", "\t\t\t\t\t\t\tKeep together: true")),
                group("Footer rows"), row("F", 10)));
        assertEquals(3, l.pages());
        assertRect(40, 60, 515, 70, l.rect(1, "A"));
        assertRect(40, 60, 515, 10, l.rect(2, "A"));
        assertRect(40, 60, 515, 70, l.rect(2, "B"));
        l.assertAbsent(2, "C");
        assertRect(40, 130, 515, 10, l.rect(2, "F"));
        assertRect(40, 60, 515, 50, l.rect(3, "C"));
        assertRect(40, 60, 515, 65, l.rect(3, "B"));
        assertRect(40, 125, 515, 10, l.rect(3, "F"));
    }
}
