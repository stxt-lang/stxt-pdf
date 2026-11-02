package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Stroke;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.Value;

import java.util.List;

import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.PropertyKey.CORNER_RADIUS;
import static dev.stxt.pdf.tree.PropertyKey.GRID_GAP;
import static dev.stxt.pdf.tree.PropertyKey.MERGE_BORDERS;

/**
 * The borders of the cells of a {@code Table} with {@code Merge borders: true}
 * (reference/components/table.md#merged-borders), once its cells are built.
 *
 * <p>Each cell keeps the border it declares ({@link Box#mergedBorder}), with which the layout draws the line between
 * each two cells: the wider of their two sides. Its {@code Border} becomes the room it leaves for those lines: on a
 * side between two cells, half of the widest line of that side against the cells next to it in the table; on a side
 * of the edge of the table, its own border, whole, because the outer line goes inside the table.</p>
 */
final class TableBorders {

    private TableBorders() {
    }

    /**
     * Merges the borders of the cells of {@code table} ({@code n}), of {@code rows} rows and {@code columns} columns. A
     * table with {@code Gap} or a cell with {@code Corner radius} is {@link ErrorCode#MERGE_BORDERS_CONFLICT}, also
     * when {@code Merge borders} comes from a rule; a table none of whose cells has a border is
     * {@link ErrorCode#MERGE_BORDERS_WITHOUT_EFFECT} only if the table declares it in its own {@code Box}: from a rule
     * it does nothing, like a rule that also reaches other boxes.
     */
    static void merge(InlineNode n, Box table, int rows, int columns) {
        Style s = table.style();
        if (!zero(s.grid().rowGap()) || !zero(s.grid().columnGap())) {
            throw error(s.line(GRID_GAP), ErrorCode.MERGE_BORDERS_CONFLICT, "The Gap of the table separates its cells, "
                    + "and with Merge borders (" + s.line(MERGE_BORDERS) + ") the border between two cells is a single "
                    + "line: the Gap is not possible with Merge borders, which Merge borders: false in the Box of the "
                    + "table turns off");
        }
        List<Box> cells = table.children().stream().filter(c -> c.role() == Element.Role.CELL).toList();
        Box[][] at = new Box[rows][columns];
        boolean any = false;
        for (Box cell : cells) {
            Cell a = cell.style().item().area();
            for (int r = a.row() - 1; r < a.row() - 1 + a.rows(); r++) {
                for (int c = a.column() - 1; c < a.column() - 1 + a.columns(); c++) {
                    at[r][c] = cell;
                }
            }
            Style cs = cell.style();
            if (cs.box().cornerRadius().asList().stream().anyMatch(v -> !zero(v))) {
                throw error(cs.line(CORNER_RADIUS), ErrorCode.MERGE_BORDERS_CONFLICT, "Corner radius rounds the "
                        + "corners of a cell, and with Merge borders the borders of the cells are the straight lines "
                        + "of the table (" + s.line(MERGE_BORDERS) + "): Corner radius is not possible in its "
                        + "cells");
            }
            Sides<Stroke> own = declared(cs);
            cell.setMergedBorder(own);
            any |= own.asList().stream().anyMatch(stroke -> !stroke.isNone());
        }
        if (!any) {
            child(n, "box").map(Nodes::inline).flatMap(b -> child(b, "merge-borders")).ifPresent(p -> {
                throw error(p, ErrorCode.MERGE_BORDERS_WITHOUT_EFFECT, "None of the cells of the table has a "
                        + "Border: Merge borders is not needed");
            });
        }
        for (Box cell : cells) {
            cell.style().box().setBorder(room(cell, at));
        }
    }

    /** The border the cell declares, side by side: its width and its color, or that of its text. */
    private static Sides<Stroke> declared(Style s) {
        Sides<Value> w = s.box().border();
        Sides<String> c = s.box().borderColor() != null ? s.box().borderColor() : Sides.all(null);
        return new Sides<>(stroke(w.top(), c.top(), s), stroke(w.right(), c.right(), s),
                stroke(w.bottom(), c.bottom(), s), stroke(w.left(), c.left(), s));
    }

    private static Stroke stroke(Value width, String color, Style s) {
        double w = width.resolve(0).orElse(0);
        return w <= Tolerance.EPSILON ? Stroke.NONE : new Stroke(w, color != null ? color : s.typography().color());
    }

    /**
     * The room the cell leaves for the lines of its sides: half of the widest line between it and the cells next to
     * it, or its own border on the edge of the table.
     */
    private static Sides<Value> room(Box cell, Box[][] at) {
        Cell a = cell.style().item().area();
        int top = a.row() - 1;
        int bottom = top + a.rows() - 1;
        int left = a.column() - 1;
        int right = left + a.columns() - 1;
        Sides<Stroke> own = cell.mergedBorder();
        double t = own.top().width();
        double b = own.bottom().width();
        if (top > 0) {
            t = 0;
            for (int c = left; c <= right; c++) {
                t = Math.max(t, Stroke.merge(at[top - 1][c].mergedBorder().bottom(), own.top()).width() / 2);
            }
        }
        if (bottom < at.length - 1) {
            b = 0;
            for (int c = left; c <= right; c++) {
                b = Math.max(b, Stroke.merge(own.bottom(), at[bottom + 1][c].mergedBorder().top()).width() / 2);
            }
        }
        double l = own.left().width();
        double r = own.right().width();
        if (left > 0) {
            l = 0;
            for (int row = top; row <= bottom; row++) {
                l = Math.max(l, Stroke.merge(at[row][left - 1].mergedBorder().right(), own.left()).width() / 2);
            }
        }
        if (right < at[0].length - 1) {
            r = 0;
            for (int row = top; row <= bottom; row++) {
                r = Math.max(r, Stroke.merge(own.right(), at[row][right + 1].mergedBorder().left()).width() / 2);
            }
        }
        return new Sides<>(Value.pt(t), Value.pt(r), Value.pt(b), Value.pt(l));
    }

    private static boolean zero(Value v) {
        return Tolerance.same(v.resolve(1).orElse(1), 0);
    }
}
