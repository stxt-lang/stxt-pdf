package dev.stxt.pdf.tree;

import dev.stxt.Node;
import dev.stxt.pdf.model.TextAlign;

/**
 * Where a {@code Cell} is in its table (reference/components/table.md), for the {@code Styles} rules that name it
 * by its position.
 *
 * @param group           group of rows: 0 {@code Header rows}, 1 {@code Body}, 2 {@code Footer rows}
 * @param row             row within its group, from 1; that of a cell spanning several is the first
 * @param column          column where it starts, from 1
 * @param columnAlign     {@code Column align} of its column; {@code null} if the table does not declare it
 * @param columnAlignLine line of that {@code Column align}
 * @param rowClass        the {@code Style} of its {@code Row}, whose {@code For style} rules apply to the cell;
 *                        {@code null} if the row does not declare it
 * @param rowId           the {@code Id} of its {@code Row}, whose {@code For id} rule applies to the cell; {@code null}
 *                        if the row does not declare it
 */
record CellPosition(int group, int row, int column, TextAlign columnAlign, int columnAlignLine,
                    Node rowClass, Node rowId) {

    static final int HEADER = 0;
    static final int BODY = 1;
    static final int FOOTER = 2;
}
