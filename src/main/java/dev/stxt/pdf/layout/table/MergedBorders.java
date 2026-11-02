package dev.stxt.pdf.layout.table;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Rule;
import dev.stxt.pdf.layout.UnitBreaker;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Stroke;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.same;

/**
 * The lines of the merged borders of a {@code Table} (reference/components/table.md#merged-borders) in one of its
 * fragments: the border between each two cells, drawn once, with the wider of their two sides ({@link Stroke#merge}).
 *
 * <p>The lines go on the lines of the grid, not on the edges of the cells: a line between two cells is centered on
 * the line between their columns or their rows, and those of the edge of the fragment (the outer border of the table,
 * and where a page cuts it) go inside it. At the cut between two rows the line is drawn on both pages, the same on
 * each; at a cut through a row, the side of each cell only if its {@code Edges} is {@code closed}. A cell that is not
 * in the fragment (it ended on a previous page) draws nothing.</p>
 *
 * <p>Where two lines meet, the horizontal one goes into the vertical one by half its width, and the lines are painted
 * from the thinnest to the widest, so that the widest covers the junction. Consecutive pieces of the same line are
 * joined into one rectangle.</p>
 */
final class MergedBorders {

    /** The cell that takes up each row and column of the table; the rows of the header, the body and the footer. */
    private final Box[][] at;
    private final List<Double> starts;
    private final List<Double> widths;
    private final double width;

    /**
     * @param table  the table, already built with {@code Merge borders}
     * @param starts start of each column, from the content box
     * @param widths width of each column
     */
    MergedBorders(Box table, List<Double> starts, List<Double> widths) {
        this.starts = starts;
        this.widths = widths;
        int columns = starts.size();
        this.width = starts.get(columns - 1) + widths.get(columns - 1);
        int rows = 0;
        List<Box> cells = table.children().stream().filter(c -> c.role() == Element.Role.CELL).toList();
        for (Box cell : cells) {
            Cell a = cell.style().item().area();
            rows = Math.max(rows, a.row() - 1 + a.rows());
        }
        this.at = new Box[rows][columns];
        for (Box cell : cells) {
            Cell a = cell.style().item().area();
            for (int r = a.row() - 1; r < a.row() - 1 + a.rows(); r++) {
                for (int c = a.column() - 1; c < a.column() - 1 + a.columns(); c++) {
                    at[r][c] = cell;
                }
            }
        }
    }

    /**
     * The lines of a fragment.
     *
     * @param rows     the rows it shows, in order, each with its row of the table as {@code unit} (the header, the body
     *                 and the footer)
     * @param children the fragments of its cells (and of its bands, which are ignored)
     */
    List<Rule> rules(List<UnitBreaker.UnitPiece> rows, List<Fragment> children) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Box, Fragment> fragments = new IdentityHashMap<>();
        for (Fragment f : children) {
            fragments.put(f.box(), f);
        }
        int columns = starts.size();
        int m = rows.size();
        List<Rule> vertical = new ArrayList<>();
        double[][] verticalWidths = new double[m][columns + 1];
        for (int i = 0; i < m; i++) {
            UnitBreaker.UnitPiece row = rows.get(i);
            for (int j = 0; j <= columns; j++) {
                Box left = j > 0 ? at[row.unit()][j - 1] : null;
                Box right = j < columns ? at[row.unit()][j] : null;
                if (left == right) {
                    continue;
                }
                Stroke s = Stroke.merge(left != null && fragments.containsKey(left) ? left.mergedBorder().right()
                        : Stroke.NONE, right != null && fragments.containsKey(right) ? right.mergedBorder().left()
                        : Stroke.NONE);
                if (s.isNone()) {
                    continue;
                }
                verticalWidths[i][j] = s.width();
                double x = j == 0 ? 0 : j == columns ? width - s.width() : line(j) - s.width() / 2;
                vertical.add(new Rule(x, row.top(), s.width(), row.height(), s.color()));
            }
        }
        List<Rule> horizontal = new ArrayList<>();
        for (int i = 0; i <= m; i++) {
            UnitBreaker.UnitPiece above = i > 0 ? rows.get(i - 1) : null;
            UnitBreaker.UnitPiece below = i < m ? rows.get(i) : null;
            for (int c = 0; c < columns; c++) {
                Box a = above != null ? at[above.unit()][c] : null;
                Box b = below != null ? at[below.unit()][c] : null;
                if (a == b) {
                    continue;
                }
                Stroke s;
                double y;
                if (above == null) {
                    // The top edge of the fragment: the line goes inside it.
                    s = Stroke.merge(sideBefore(below, b, c, fragments), top(b, fragments));
                    y = below.top();
                } else if (below == null) {
                    s = Stroke.merge(bottom(a, fragments), sideAfter(above, a, c, fragments));
                    y = above.top() + above.height() - s.width();
                } else {
                    s = Stroke.merge(bottom(a, fragments), top(b, fragments));
                    y = below.top() - s.width() / 2;
                }
                if (s.isNone()) {
                    continue;
                }
                double startInto = Math.max(i > 0 ? verticalWidths[i - 1][c] : 0, i < m ? verticalWidths[i][c] : 0);
                double endInto = Math.max(i > 0 ? verticalWidths[i - 1][c + 1] : 0,
                        i < m ? verticalWidths[i][c + 1] : 0);
                double x0 = c == 0 ? 0 : starts.get(c) - startInto / 2;
                double x1 = c == columns - 1 ? width : starts.get(c) + widths.get(c) + endInto / 2;
                horizontal.add(new Rule(x0, y, x1 - x0, s.width(), s.color()));
            }
        }
        List<Rule> all = new ArrayList<>(joinVertical(vertical));
        all.addAll(joinHorizontal(horizontal));
        // The widest last: it covers the junctions. The sort is stable: with the same width, vertical first.
        all.sort(Comparator.comparingDouble(r -> Math.min(r.width(), r.height())));
        return all;
    }

    /** Position of the line between column {@code j - 1} and column {@code j}. */
    private double line(int j) {
        return starts.get(j);
    }

    /**
     * The top side of {@code cell} in the fragment: its own if it starts in it; if it comes from the previous page,
     * only if its {@code Edges} is {@code closed}; nothing if it is not in it.
     */
    private static Stroke top(Box cell, Map<Box, Fragment> fragments) {
        Fragment f = fragments.get(cell);
        if (f == null) {
            return Stroke.NONE;
        }
        return f.first() || closed(cell) ? cell.mergedBorder().top() : Stroke.NONE;
    }

    /** The bottom side of {@code cell} in the fragment, with the criterion of {@link #top}. */
    private static Stroke bottom(Box cell, Map<Box, Fragment> fragments) {
        Fragment f = fragments.get(cell);
        if (f == null) {
            return Stroke.NONE;
        }
        return f.last() || closed(cell) ? cell.mergedBorder().bottom() : Stroke.NONE;
    }

    /**
     * At the top of the fragment, the bottom side of the cell of the row above in the table, on the previous page, if
     * {@code row} and {@code cell} start here: the line between the two rows is drawn on both pages.
     */
    private Stroke sideBefore(UnitBreaker.UnitPiece row, Box cell, int column, Map<Box, Fragment> fragments) {
        Fragment f = fragments.get(cell);
        if (!row.first() || row.unit() == 0 || f == null || !f.first()
                || cell.style().item().area().row() - 1 != row.unit()) {
            return Stroke.NONE;
        }
        return at[row.unit() - 1][column].mergedBorder().bottom();
    }

    /** At the bottom of the fragment, the top side of the cell of the row below in the table, on the next page. */
    private Stroke sideAfter(UnitBreaker.UnitPiece row, Box cell, int column, Map<Box, Fragment> fragments) {
        Fragment f = fragments.get(cell);
        Cell a = cell.style().item().area();
        if (!row.last() || row.unit() == at.length - 1 || f == null || !f.last()
                || a.row() - 1 + a.rows() - 1 != row.unit()) {
            return Stroke.NONE;
        }
        return at[row.unit() + 1][column].mergedBorder().top();
    }

    private static boolean closed(Box cell) {
        return cell.style().breaks().edges() == Edges.CLOSED;
    }

    /** Joins the vertical pieces of the same line that touch: same position, width and color. */
    private static List<Rule> joinVertical(List<Rule> rules) {
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingDouble(Rule::x).thenComparingDouble(Rule::y));
        List<Rule> joined = new ArrayList<>();
        for (Rule r : sorted) {
            Rule last = joined.isEmpty() ? null : joined.get(joined.size() - 1);
            if (last != null && same(last.x(), r.x()) && same(last.width(), r.width()) && last.color().equals(r.color())
                    && r.y() <= last.y() + last.height() + EPSILON) {
                double end = Math.max(last.y() + last.height(), r.y() + r.height());
                joined.set(joined.size() - 1, new Rule(last.x(), last.y(), last.width(), end - last.y(), last.color()));
            } else {
                joined.add(r);
            }
        }
        return joined;
    }

    /** Joins the horizontal pieces of the same line that touch or overlap. */
    private static List<Rule> joinHorizontal(List<Rule> rules) {
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingDouble(Rule::y).thenComparingDouble(Rule::x));
        List<Rule> joined = new ArrayList<>();
        for (Rule r : sorted) {
            Rule last = joined.isEmpty() ? null : joined.get(joined.size() - 1);
            if (last != null && same(last.y(), r.y()) && same(last.height(), r.height())
                    && last.color().equals(r.color()) && r.x() <= last.x() + last.width() + EPSILON) {
                double end = Math.max(last.x() + last.width(), r.x() + r.width());
                joined.set(joined.size() - 1, new Rule(last.x(), last.y(), end - last.x(), last.height(), last.color()));
            } else {
                joined.add(r);
            }
        }
        return joined;
    }
}
