package dev.stxt.pdf.layout.table;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Rule;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.LineStyle;

/**
 * The lines of the merged borders of a table across pages (reference/components/table.md#merged-borders). Pages of
 * 180 pt with margins of 40: page area of 515 × 100 from (40, 40). The tables have one column of 515, and their cells
 * {@code Border: 1} and blocks of fixed height.
 */
class MergedBordersTest {

    private static final double PAGE = 180;
    private static final String[] BORDER = {"\tStyles:", "\t\tFor element: Cell", "\t\t\tBox:", "\t\t\t\tBorder: 1"};

    private static String[] concat(String[]... parts) {
        return Arrays.stream(parts).flatMap(Arrays::stream).toArray(String[]::new);
    }

    private static String[] table(String... properties) {
        String[] head = {"\tTable:", "\t\tId: T", "\t\tColumn sizes: *", "\t\tBox:", "\t\t\tMerge borders: true"};
        return concat(head, Arrays.stream(properties).map(p -> "\t\t" + p).toArray(String[]::new));
    }

    /** A row with a cell that holds blocks of those heights. */
    private static String[] row(String id, String[] rowLines, int... blocks) {
        String[] lines = concat(new String[]{"\t\t\tRow:"}, rowLines, new String[]{"\t\t\t\tCell:", "\t\t\t\t\tId: " + id});
        for (int h : blocks) {
            lines = concat(lines, new String[]{"\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: " + h});
        }
        return lines;
    }

    private static String[] row(String id, int height) {
        return row(id, new String[0], height);
    }

    /**
     * The horizontal lines of the fragment of the table, as {@code y height}, from its content box, in the order they
     * are painted: from the thinnest to the widest.
     */
    private static List<String> horizontal(LayoutFixture l, int page) {
        Fragment t = l.fragment(page, "T");
        return t.rules().stream().filter(r -> r.width() > r.height()).map(MergedBordersTest::yAndHeight).toList();
    }

    private static String yAndHeight(Rule r) {
        return format(r.y()) + " " + format(r.height());
    }

    private static String format(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /**
     * The header (20, with a bottom border of 2) repeats: on each page the line under it is 2, the widest against the
     * first row of the body on that page. The body rows measure 1 + 30 + 0.5, 0.5 + 30 + 0.5 and 0.5 + 30 + 1, and the
     * header 1 + 20 + 1: on page 1, the header and two rows (84.5), with the line between the second and the third
     * row inside at the bottom; on page 2 the third row goes under the header, and the line between them is the
     * header's, 2 wide, centered on 22.
     */
    @Test
    void theLineUnderARepeatedHeaderIsTheWidestOnEveryPage() {
        LayoutFixture l = layOutPaged(PAGE, concat(BORDER, table("Split rows: false"),
                new String[]{"\t\tHeader rows:"},
                concat(new String[]{"\t\t\tRow:", "\t\t\t\tCell:", "\t\t\t\t\tId: H", "\t\t\t\t\tBox:",
                        "\t\t\t\t\t\tBorder: 1 1 2 1", "\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 20"}),
                new String[]{"\t\tBody rows:"}, row("B1", 30), row("B2", 30), row("B3", 30)));
        assertEquals(2, l.pages());
        assertEquals(84.5, l.rect(1, "T").height(), 1e-9);
        assertEquals(List.of("0 1", "53 1", "83.5 1", "21 2"), horizontal(l, 1));
        assertEquals(53.5, l.rect(2, "T").height(), 1e-9);
        assertEquals(List.of("0 1", "52.5 1", "21 2"), horizontal(l, 2));
        // The vertical lines go along the whole fragment, inside it, as one rectangle each.
        List<Rule> vertical = l.fragment(1, "T").rules().stream().filter(r -> r.height() > r.width()).toList();
        assertEquals(List.of(new Rule(0, 0, 1, 84.5, "#000000"), new Rule(514, 0, 1, 84.5, "#000000")), vertical);
    }

    /**
     * A row split across pages: with {@code Edges: open} (by default) the cut through its cell has no line; with
     * {@code Edges: closed}, the cell's side is drawn at the cut on both pages.
     */
    @Test
    void aSplitRowHasALineAtTheCutOnlyWithClosedEdges() {
        LayoutFixture open = layOutPaged(PAGE, concat(BORDER, table(), new String[]{"\t\tBody rows:"},
                row("C", new String[0], 30, 30, 30, 30, 30)));
        assertEquals(2, open.pages());
        assertEquals(List.of("0 1"), horizontal(open, 1));
        double end = open.rect(2, "T").height();
        assertEquals(List.of(format(end - 1) + " 1"), horizontal(open, 2));

        LayoutFixture closed = layOutPaged(PAGE, concat(BORDER, table(), new String[]{"\t\tBody rows:"},
                row("C", new String[]{"\t\t\t\tBreak:", "\t\t\t\t\tEdges: closed"}, 30, 30, 30, 30, 30)));
        assertEquals(2, closed.pages());
        assertEquals(List.of("0 1", format(closed.rect(1, "T").height() - 1) + " 1"), horizontal(closed, 1));
        assertEquals(List.of("0 1", format(closed.rect(2, "T").height() - 1) + " 1"), horizontal(closed, 2));
    }

    // Cases that were missing according to mutation testing.

    /**
     * A table cut between two rows: the line between them is drawn on both pages, inside the fragment. Rows of
     * 1 + 30 + 0.5, 0.5 + 30 + 0.5 and 0.5 + 30 + 0.5 fill page 1 (93.5, with the bottom side of the fourth row's cell
     * as the line at the bottom), and the fourth, 0.5 + 30 + 1, starts page 2 with the third row's bottom side as the
     * line at the top.
     */
    @Test
    void aTableCutBetweenRowsDrawsTheLineOnBothPages() {
        LayoutFixture l = layOutPaged(PAGE, concat(BORDER, table("Split rows: false"), new String[]{"\t\tBody rows:"},
                row("R1", 30), row("R2", 30), row("R3", 30), row("R4", 30)));
        assertEquals(2, l.pages());
        assertEquals(93.5, l.rect(1, "T").height(), 1e-9);
        assertEquals(List.of("0 1", "31 1", "62 1", "92.5 1"), horizontal(l, 1));
        assertEquals(31.5, l.rect(2, "T").height(), 1e-9);
        assertEquals(List.of("0 1", "30.5 1"), horizontal(l, 2));
    }

    /** Numbering the pages ({@code {page}} in a cell) rebuilds the fragments of the table and keeps its lines. */
    @Test
    void theLinesSurviveThePageNumbering() {
        LayoutFixture l = layOut(concat(BORDER, table(), new String[]{"\t\tBody rows:", "\t\t\tRow:",
                "\t\t\t\tCell: p. {page}"}));
        // One cell: its four sides, each a rule.
        assertEquals(4, l.fragment("T").rules().size());
    }

    /** The pieces of a horizontal line that touch across the columns are one rule, as wide as the table. */
    @Test
    void theHorizontalPiecesOfTheColumnsAreJoined() {
        String[] cell = {"\t\t\t\tCell:", "\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30"};
        LayoutFixture l = layOut(concat(BORDER, new String[]{"\tTable:", "\t\tId: T", "\t\tColumn sizes: * *",
                        "\t\tBox:", "\t\t\tMerge borders: true", "\t\tBody rows:"},
                new String[]{"\t\t\tRow:"}, cell, cell, new String[]{"\t\t\tRow:"}, cell, cell));
        List<Rule> horizontal = l.fragment("T").rules().stream().filter(r -> r.width() > r.height()).toList();
        assertEquals(List.of(new Rule(0, 0, 515, 1, "#000000"), new Rule(0, 31, 515, 1, "#000000"),
                new Rule(0, 62, 515, 1, "#000000")), horizontal);
        List<Rule> vertical = l.fragment("T").rules().stream().filter(r -> r.height() > r.width()).toList();
        assertEquals(List.of(new Rule(0, 0, 1, 63, "#000000"), new Rule(257, 0, 1, 63, "#000000"),
                new Rule(514, 0, 1, 63, "#000000")), vertical);
    }

    /**
     * Border style: the line between two cells is drawn like the wider of their two borders. The header has a solid
     * bottom border of 2 and the body a dashed border of 1: the line between them is solid, and the other lines of
     * the body, dashed. A dashed piece and a solid one of the same line are not joined.
     */
    @Test
    void theLineBetweenTwoCellsTakesTheStyleOfTheWiderBorder() {
        String[] cell = {"\t\t\t\tCell:", "\t\t\t\t\tBlock:", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30"};
        LayoutFixture l = layOut(concat(new String[]{"\tStyles:", "\t\tFor element: Cell", "\t\t\tBox:",
                        "\t\t\t\tBorder: 1", "\t\t\t\tBorder style: dashed", "\t\tFor element: Header cell",
                        "\t\t\tBox:", "\t\t\t\tBorder: 1 1 2 1", "\t\t\t\tBorder style: solid",
                        "\tTable:", "\t\tId: T", "\t\tColumn sizes: *", "\t\tBox:", "\t\t\tMerge borders: true",
                        "\t\tHeader rows:", "\t\t\tRow:"}, cell, new String[]{"\t\tBody rows:", "\t\t\tRow:"}, cell));
        List<Rule> horizontal = l.fragment("T").rules().stream().filter(r -> !r.vertical()).toList();
        assertEquals(List.of(new Rule(0, 0, 515, 1, "#000000", LineStyle.SOLID),
                new Rule(0, 63, 515, 1, "#000000", LineStyle.DASHED),
                new Rule(0, 31, 515, 2, "#000000", LineStyle.SOLID)), horizontal);
        List<Rule> left = l.fragment("T").rules().stream().filter(r -> r.vertical() && r.x() == 0).toList();
        assertEquals(List.of(new Rule(0, 0, 1, 32, "#000000", LineStyle.SOLID),
                new Rule(0, 32, 1, 32, "#000000", LineStyle.DASHED)), left);
    }
}
