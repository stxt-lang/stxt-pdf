package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.TextAlign;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static dev.stxt.pdf.tree.ModelValidator.font;
import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.inline;
import static dev.stxt.pdf.tree.Nodes.is;
import static dev.stxt.pdf.tree.Nodes.label;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_AREA;
import static dev.stxt.pdf.tree.PropertyReader.box;
import static dev.stxt.pdf.tree.PropertyReader.fragmentation;
import static dev.stxt.pdf.tree.PropertyReader.typography;

/**
 * The rows of a {@code Table} (reference/components/table.md) as items of a grid with explicit placement, and the
 * text of a {@code Cell} with a value.
 */
final class TableBuilder {

    /** Row groups of a {@code Table}, in the order they go in. */
    private static final List<String> TABLE_GROUPS = List.of("header-rows", "body", "footer-rows");

    /** What the {@code Box} of a {@code Row} allows: its look (reference/components/table.md#rows). */
    private static final Set<String> ROW_LOOK = Set.of("background", "border", "border-color", "corner-radius");

    private TableBuilder() {
    }

    /**
     * Builds the box of a {@code Cell}, with its {@code Row} and its position in the table: the table does not know how
     * to build elements.
     */
    @FunctionalInterface
    interface Cells {
        Box build(InlineNode cell, InlineNode row, CellPosition position);
    }

    /** Rows and columns a {@code Cell} takes up, and the line of its {@code Span} (or of the cell, without one). */
    private record Span(int rows, int columns, int line) {
    }

    /**
     * The rows of a {@code Table} (reference/components/table.md) as items of a grid with explicit placement: each
     * {@code Cell} goes in the first free column of its row, counting the ones taken up by the {@code Span} of the rows
     * above. The groups go in the order {@code Header rows}, {@code Body}, {@code Footer rows}; each row fills exactly
     * the columns of {@code Column sizes}, and a {@code Span} does not go outside the table or its group. The rows of
     * {@code Header rows} and {@code Footer rows} are repeated on each page and do not allow {@code Break}.
     * {@code cells} builds the box of each cell, with the table as parent. Each cell carries its position
     * ({@link CellPosition}) for the rules of {@code Styles} and the {@code Column align} of its column. A row with
     * {@code Id} or {@code Box} also has its own box ({@link #row}).
     */
    static void table(BuildContext ctx, InlineNode n, Box table, Cells cells) {
        int count = table.style().grid().columnSizes().size();
        Columns columns = new Columns(count, columnAlign(n, count),
                child(n, "column-align").map(Node::getLine).orElse(0));
        int last = -1;
        int firstRow = 0;
        for (Node g : n.getChildren()) {
            int group = TABLE_GROUPS.indexOf(g.getCanonicalName());
            if (group < 0) {
                continue;
            }
            if (group < last) {
                throw error(g, ErrorCode.TABLE_ORDER, g.getName() + " goes after "
                        + label(TABLE_GROUPS.get(last))
                        + ": the groups of a Table go in the order Header rows, Body, Footer rows");
            }
            last = group;
            int rows = rows(ctx, g, group, firstRow, columns, table, cells);
            if (group == 0) {
                table.setHeaderRows(rows);
            } else if (group == 2) {
                table.setFooterRows(rows);
            }
            firstRow += rows;
        }
    }

    /**
     * The columns of the table: how many there are ({@code Column sizes}) and their {@code Column align}, with its line
     * (0 without one).
     */
    private record Columns(int count, List<TextAlign> aligns, int alignLine) {
    }

    /**
     * The rows of a group ({@code g}, the {@code group}-th of {@link #TABLE_GROUPS}), whose first is row
     * {@code firstRow} of the table (from 0). Builds their cells and returns how many rows it has.
     */
    private static int rows(BuildContext ctx, Node g, int group, int firstRow, Columns columns, Box table,
                            Cells cells) {
        List<InlineNode> rows = inline(g).getChildren().stream().map(Nodes::inline).toList();
        boolean[][] taken = new boolean[rows.size()][columns.count()];
        for (int r = 0; r < rows.size(); r++) {
            InlineNode row = rows.get(r);
            if (group != 1) {
                child(row, "break").ifPresent(b -> {
                    throw error(b, ErrorCode.TABLE_ROW_BREAK, "The rows of " + g.getName() + " are repeated on "
                            + "each page of the table and do not break: Break is not needed");
                });
            }
            row(ctx, row, table, firstRow + r + 1, columns.count());
            int c = 0;
            for (Node h : row.getChildren()) {
                if (!is(h, "cell")) {
                    continue;
                }
                InlineNode cell = inline(h);
                c = freeColumn(taken[r], c, cell);
                Span span = span(cell);
                occupy(taken, r, c, span, g.getName());
                TextAlign align = columns.aligns().isEmpty() ? null : columns.aligns().get(c);
                Box box = cells.build(cell, row, new CellPosition(group, r + 1, c + 1, align, columns.alignLine(),
                        child(row, "style").orElse(null), child(row, "id").orElse(null)));
                box.style().declared(ITEM_AREA, cell.getLine());
                box.style().item().setArea(new Cell(firstRow + r + 1, c + 1, span.rows(), span.columns()));
                table.addChild(box);
                c += span.columns();
            }
            for (int cc = 0; cc < columns.count(); cc++) {
                if (!taken[r][cc]) {
                    throw error(row, ErrorCode.TABLE_ROW_CELLS, "The row leaves column " + (cc + 1)
                            + " of " + columns.count() + " (Columns) free: a Cell is missing, empty if needed, or a Span");
                }
            }
        }
        return rows.size();
    }

    /**
     * The box of a {@code Row} with {@code Id} or {@code Box} (reference/components/table.md#rows): the band of row
     * {@code tableRow} of the table (from 1), from the first column to the last, which is painted below its cells. Its
     * {@code Box} only allows the look ({@code ROW_BOX}); it also reads its {@code Typography} (the default color of
     * its border) and its {@code Break} (its {@code Edges}). Without {@code Id} or {@code Box}, the row has no box.
     */
    private static void row(BuildContext ctx, InlineNode row, Box table, int tableRow, int columns) {
        Optional<Node> id = child(row, "id");
        Optional<Node> look = child(row, "box");
        if (id.isEmpty() && look.isEmpty()) {
            return;
        }
        Style s = Style.inheriting(table.style());
        s.setLayout(Layout.BLOCK);
        s.setPosition(Position.STATIC);
        child(row, "typography").map(Nodes::inline).ifPresent(t -> typography(t, s));
        child(row, "break").map(Nodes::inline).ifPresent(b -> fragmentation(b, s));
        look.map(Nodes::inline).ifPresent(b -> {
            for (Node p : b.getChildren()) {
                if (!ROW_LOOK.contains(p.getCanonicalName())) {
                    throw error(p, ErrorCode.ROW_BOX, p.getName() + " has no effect on a Row, which is not a layout "
                            + "box: its Box only allows Background, Border, Border color and Corner radius; it goes on its "
                            + "cells");
                }
            }
            box(b, s);
        });
        Box box = new Box(id.map(ctx::id).orElse(null), Element.ROW, s, false, row.getLine());
        s.item().setArea(new Cell(tableRow, 1, 1, columns));
        table.addRow(box);
    }

    /** The first free column of the row from {@code from}, counting the ones taken up by the {@code Span} above. */
    private static int freeColumn(boolean[] taken, int from, InlineNode cell) {
        int c = from;
        while (c < taken.length && taken[c]) {
            c++;
        }
        if (c == taken.length) {
            throw error(cell, ErrorCode.TABLE_ROW_CELLS, "No free column is left for this Cell: the table "
                    + "has " + taken.length + " (Columns), counting the ones taken up by the Span of the rows above");
        }
        return c;
    }

    /**
     * Marks as taken the rows and columns of the cell that begins at row {@code r} and column {@code c} of its group
     * ({@code group}, its name): a {@code Span} does not go outside the table or the group, and does not overlap.
     */
    private static void occupy(boolean[][] taken, int r, int c, Span span, String group) {
        int columns = taken[r].length;
        if (c + span.columns() > columns) {
            throw error(span.line(), ErrorCode.TABLE_SPAN, "The cell takes up from column " + (c + 1)
                    + " to " + (c + span.columns()) + ", and the table has " + columns + " (Columns)");
        }
        if (r + span.rows() > taken.length) {
            throw error(span.line(), ErrorCode.TABLE_SPAN, "The cell takes up " + span.rows() + " rows and its "
                    + group + " has " + (taken.length - r) + " left: a Span does not cross from one group to another");
        }
        for (int rr = r; rr < r + span.rows(); rr++) {
            for (int cc = c; cc < c + span.columns(); cc++) {
                if (taken[rr][cc]) {
                    throw error(span.line(), ErrorCode.TABLE_SPAN, "The cell overlaps in column "
                            + (cc + 1) + " with another one that takes up several rows");
                }
                taken[rr][cc] = true;
            }
        }
    }

    /**
     * The {@code Column align} of a {@code Table}: one alignment per column, in the order of {@code Column sizes}.
     * Empty if it does not declare it.
     */
    private static List<TextAlign> columnAlign(InlineNode table, int columns) {
        Optional<Node> node = child(table, "column-align");
        if (node.isEmpty()) {
            return List.of();
        }
        String[] t = node.get().getText().trim().split("\\s+");
        List<TextAlign> aligns = new ArrayList<>();
        for (String word : t) {
            try {
                aligns.add(TextAlign.valueOf(word.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw error(node.get(), ErrorCode.VALUE_NOT_ALLOWED, "Value not allowed in Column align: " + word
                        + " (left, center, right or justify)");
            }
        }
        if (aligns.size() != columns) {
            throw error(node.get(), ErrorCode.TABLE_COLUMN_ALIGN, "Column align gives " + aligns.size()
                    + " alignments and the table has " + columns + " columns (Columns): one goes per column");
        }
        return aligns;
    }

    /** The {@code Span} of a cell: {@code rows columns}, two positive integers; without it, 1 1. */
    private static Span span(InlineNode cell) {
        Optional<Node> node = child(cell, "span");
        if (node.isEmpty()) {
            return new Span(1, 1, cell.getLine());
        }
        String text = node.get().getText().trim();
        String[] t = text.split("\\s+");
        if (t.length != 2 || !t[0].matches("[1-9]\\d*") || !t[1].matches("[1-9]\\d*")) {
            throw error(node.get(), ErrorCode.VALUE_SYNTAX, "Span expects 'rows columns', two positive "
                    + "integers: '" + text + "'");
        }
        return new Span(Integer.parseInt(t[0]), Integer.parseInt(t[1]), node.get().getLine());
    }

    /** The text of a {@code Cell} with a value: one line, with no elements beside it. */
    static void cellText(InlineNode n, Box box) {
        n.getChildren().stream().filter(Nodes::isElement).findFirst().ifPresent(e -> {
            throw error(e, ErrorCode.CELL_CONTENT, "A Cell has text in the value (line " + n.getLine()
                    + ") or elements, not both: the text goes in a Paragraph");
        });
        font(n, n, box);
        box.setText(n.getText(), n.getLine(), false);
    }
}
