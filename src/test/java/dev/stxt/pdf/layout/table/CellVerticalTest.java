package dev.stxt.pdf.layout.table;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;

/**
 * {@code Vertical} of a {@code Cell} (reference/components/table.md): the cell stays stretched to its row and its
 * content goes down in what the row leaves over. Page area of 515 wide from (40, 40); the tables have two columns of
 * 100, the first with the cell that gives the row its height ({@code A}) and the second with the cell under test
 * ({@code K}), whose content is the block {@code C}.
 */
class CellVerticalTest {

    private static void assertRect(double x, double y, double w, double h, Rect r) {
        assertEquals(new Rect(x, y, w, h).toString(),
                new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height())).toString());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static String[] concat(String[]... parts) {
        return Arrays.stream(parts).flatMap(Arrays::stream).toArray(String[]::new);
    }

    private static String[] lines(String... lines) {
        return lines;
    }

    private static final String[] TABLE = {"\tTable:", "\t\tId: T", "\t\tColumn sizes: 100 100", "\t\tBody rows:"};

    /** A cell with a block as high as {@code height}: the one that gives the row its height. */
    private static String[] tall(String id, int height) {
        return lines("\t\t\t\tCell:", "\t\t\t\t\tId: " + id, "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: " + height);
    }

    /** The cell {@code K} with these properties and the block {@code C}, 30 high. */
    private static String[] cell(String... properties) {
        return concat(lines("\t\t\t\tCell:", "\t\t\t\t\tId: K"), properties,
                lines("\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: C", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30"));
    }

    private static LayoutFixture row(String... properties) {
        return layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100), cell(properties)));
    }

    @Test
    void theContentStartsAtTheTopByDefault() {
        LayoutFixture l = row();
        assertRect(140, 40, 100, 100, l.rect("K"));
        assertRect(140, 40, 100, 30, l.rect("C"));
        assertRect(140, 40, 100, 30, row("\t\t\t\t\tVertical: top").rect("C"));
    }

    @Test
    void centerPutsHalfOfWhatIsLeftOverAboveTheContent() {
        LayoutFixture l = row("\t\t\t\t\tVertical: center");
        // The cell stays stretched to its row: only its content moves.
        assertRect(140, 40, 100, 100, l.rect("K"));
        assertRect(140, 75, 100, 30, l.rect("C"));
    }

    @Test
    void endPutsTheContentAtTheBottom() {
        LayoutFixture l = row("\t\t\t\t\tVertical: bottom");
        assertRect(140, 40, 100, 100, l.rect("K"));
        assertRect(140, 110, 100, 30, l.rect("C"));
    }

    @Test
    void theContentMovesInsideTheContentBoxOfTheCell() {
        // Padding 5 and border 1 on each side leave a content box of 88: 58 left over.
        LayoutFixture l = row("\t\t\t\t\tVertical: bottom", "\t\t\t\t\tBox:", "\t\t\t\t\t\tPadding: 5",
                "\t\t\t\t\t\tBorder: 1");
        assertRect(140, 40, 100, 100, l.rect("K"));
        assertRect(146, 104, 88, 30, l.rect("C"));
    }

    @Test
    void theMarginsTheCellContainsCountAsContent() {
        // The margins of C stay inside the cell (10 above and 20 below): the content is 60 high, and 40 is left over.
        LayoutFixture l = layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100),
                lines("\t\t\t\tCell:", "\t\t\t\t\tId: K", "\t\t\t\t\tVertical: bottom",
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: C", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30",
                        "\t\t\t\t\t\t\tMargin: 10 0 20 0")));
        assertRect(140, 90, 100, 30, l.rect("C"));
    }

    @Test
    void allTheElementsOfTheCellMoveTogether() {
        LayoutFixture l = layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100),
                lines("\t\t\t\tCell:", "\t\t\t\t\tId: K", "\t\t\t\t\tVertical: center",
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: C", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30",
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: D", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 20")));
        assertRect(140, 65, 100, 30, l.rect("C"));
        assertRect(140, 95, 100, 20, l.rect("D"));
    }

    @Test
    void theLineOfACellWithAValueMovesToo() {
        LayoutFixture top = layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100),
                lines("\t\t\t\tCell: Total", "\t\t\t\t\tId: K")));
        LayoutFixture center = layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100),
                lines("\t\t\t\tCell: Total", "\t\t\t\t\tId: K", "\t\t\t\t\tVertical: center")));
        LayoutFixture end = layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100),
                lines("\t\t\t\tCell: Total", "\t\t\t\t\tId: K", "\t\t\t\t\tVertical: bottom")));
        Fragment k = top.fragment("K");
        double line = k.lines().get(0).height();
        assertEquals(100, k.height(), 1e-6);
        assertEquals(0, k.lines().get(0).y(), 1e-6);
        assertEquals((100 - line) / 2, center.fragment("K").lines().get(0).y(), 1e-6);
        assertEquals(100 - line, end.fragment("K").lines().get(0).y(), 1e-6);
        assertEquals(100, end.fragment("K").height(), 1e-6);
    }

    @Test
    void aCellThatSpansSeveralRowsUsesTheHeightOfAllOfThem() {
        // K takes up the two rows, of 40 and 60: its content of 30 is centered in 100.
        LayoutFixture l = layOut(concat(TABLE,
                lines("\t\t\tRow:"), tall("A", 40),
                lines("\t\t\t\tCell:", "\t\t\t\t\tId: K", "\t\t\t\t\tSpan: 2 1", "\t\t\t\t\tVertical: center",
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: C", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30"),
                lines("\t\t\tRow:"), tall("B", 60)));
        assertRect(140, 40, 100, 100, l.rect("K"));
        assertRect(140, 75, 100, 30, l.rect("C"));
    }

    @Test
    void aCellWithAHeightOfItsOwnHasNoSpecialCase() {
        LayoutFixture l = layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 20),
                lines("\t\t\t\tCell:", "\t\t\t\t\tId: K", "\t\t\t\t\tVertical: bottom", "\t\t\t\t\tBox:",
                        "\t\t\t\t\t\tHeight: 80",
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: C", "\t\t\t\t\t\tBox:", "\t\t\t\t\t\t\tHeight: 30")));
        assertRect(140, 40, 100, 80, l.rect("K"));
        assertRect(140, 90, 100, 30, l.rect("C"));
    }

    /** A cell with two blocks, {@code first} and {@code second} high, with these properties. */
    private static String[] two(String id, int first, int second, String... properties) {
        return concat(lines("\t\t\t\tCell:", "\t\t\t\t\tId: " + id), properties,
                lines("\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: " + id + "1", "\t\t\t\t\t\tBox:",
                        "\t\t\t\t\t\t\tHeight: " + first,
                        "\t\t\t\t\tBlock:", "\t\t\t\t\t\tId: " + id + "2", "\t\t\t\t\t\tBox:",
                        "\t\t\t\t\t\t\tHeight: " + second));
    }

    @Test
    void inASplitRowTheContentMovesInTheFragmentTheCellEndsIn() {
        // Page area of 100. A places 100 on page 1 and 60 on page 2; K, 100 and 30: on page 1 its content reaches the
        // break and does not move, and on page 2 the row is 60 high and its 30 go to the bottom.
        LayoutFixture l = layOutPaged(180, concat(TABLE, lines("\t\t\tRow:"), two("A", 100, 60),
                two("K", 100, 30, "\t\t\t\t\tVertical: bottom")));
        assertEquals(2, l.pages());
        assertRect(140, 40, 100, 100, l.rect(1, "K1"));
        assertRect(140, 40, 100, 60, l.rect(2, "K"));
        assertRect(140, 70, 100, 30, l.rect(2, "K2"));
    }

    @Test
    void inASplitRowTheContentThatEndedOnAPreviousPageStaysWhereItWas() {
        // K fits whole on page 1, but its row goes on with A: its content is not moved to another page, and what the
        // cell measures on page 2 is empty.
        LayoutFixture l = layOutPaged(180, concat(TABLE, lines("\t\t\tRow:"), two("A", 60, 60),
                cell("\t\t\t\t\tVertical: bottom")));
        assertEquals(2, l.pages());
        assertRect(140, 40, 100, 30, l.rect(1, "C"));
        l.assertAbsent(2, "C");
    }

    @Test
    void aRuleGivesItToTheCellsItReaches() {
        String[] content = concat(TABLE, lines("\t\t\tRow:"), tall("A", 100), cell());
        assertRect(140, 110, 100, 30, layOut(concat(lines("\tStyles:", "\t\tFor element: Cell",
                "\t\t\tVertical: bottom"), content)).rect("C"));
        assertRect(140, 110, 100, 30, layOut(concat(lines("\tStyles:", "\t\tFor element: Body cell",
                "\t\t\tVertical: bottom"), content)).rect("C"));
        assertRect(140, 75, 100, 30, layOut(concat(lines("\tStyles:", "\t\tFor column: 2",
                "\t\t\tVertical: center"), content)).rect("C"));
        // The style of the Row reaches its cells.
        assertRect(140, 75, 100, 30, layOut(concat(lines("\tStyles:", "\t\tFor style: total",
                "\t\t\tVertical: center"), TABLE, lines("\t\t\tRow:", "\t\t\t\tStyle: total"), tall("A", 100),
                cell())).rect("C"));
        // Inside the rule of a table, only its cells.
        assertRect(140, 110, 100, 30, layOut(concat(lines("\tStyles:", "\t\tFor element: Table",
                "\t\t\tFor element: Cell", "\t\t\t\tVertical: bottom"), content)).rect("C"));
    }

    @Test
    void whatTheCellDeclaresWinsOverTheRules() {
        LayoutFixture l = layOut(concat(lines("\tStyles:", "\t\tFor element: Cell", "\t\t\tVertical: bottom",
                "\t\tFor column: 2", "\t\t\tVertical: center"), TABLE, lines("\t\t\tRow:"), tall("A", 100),
                cell("\t\t\t\t\tVertical: top")));
        assertRect(140, 40, 100, 30, l.rect("C"));
        // And the column wins over the element.
        LayoutFixture rules = layOut(concat(lines("\tStyles:", "\t\tFor element: Cell", "\t\t\tVertical: bottom",
                "\t\tFor column: 2", "\t\t\tVertical: center"), TABLE, lines("\t\t\tRow:"), tall("A", 100), cell()));
        assertRect(140, 75, 100, 30, rules.rect("C"));
    }

    private static InvalidModelException error(String... lines) {
        return assertThrows(InvalidModelException.class, () -> layOut(lines));
    }

    @Test
    void startAndEndAndTheSidesOfTheOtherAxisAreNotValues() {
        for (String value : new String[]{"start", "end", "left", "right"}) {
            assertThrows(RuntimeException.class, () -> layOut(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100),
                    cell("\t\t\t\t\tVertical: " + value))), value);
        }
    }

    @Test
    void theValuesThatStretchOrDistributeAreNotAllowed() {
        for (String value : new String[]{"stretch", "justify", "around", "distribute"}) {
            InvalidModelException e = error(concat(TABLE, lines("\t\t\tRow:"), tall("A", 100),
                    cell("\t\t\t\t\tVertical: " + value)));
            assertEquals(ErrorCode.VALUE_NOT_ALLOWED.name(), e.code(), value);
            assertTrue(e.getMessage().contains("top, center or bottom"), e.getMessage());
        }
    }

    @Test
    void aRuleOfCellsChecksTheValueWhenItIsRead() {
        // No table in the document: the rule is checked anyway.
        for (String rule : new String[]{"For element: Cell", "For element: Header cell", "For column: 1"}) {
            InvalidModelException e = error("\tStyles:", "\t\t" + rule, "\t\t\tVertical: stretch", "\tText: a");
            assertEquals(ErrorCode.VALUE_NOT_ALLOWED.name(), e.code(), rule);
        }
    }

    @Test
    void aRuleOfAStyleChecksTheValueWhenItReachesACell() {
        // justify is valid in a Stack, so the rule is read; it is not in the cell that takes the style.
        InvalidModelException e = error(concat(lines("\tStyles:", "\t\tFor style: spread", "\t\t\tVertical: justify"),
                TABLE, lines("\t\t\tRow:"), tall("A", 100), cell("\t\t\t\t\tStyle: spread")));
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED.name(), e.code());
    }
}
