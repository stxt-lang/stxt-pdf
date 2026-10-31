package dev.stxt.pdf.tree;

import dev.stxt.Node;
import dev.stxt.pdf.model.TextAlign;

/**
 * Dónde está una {@code Cell} en su tabla (reference/components/table.md), para las reglas de
 * {@code Styles} que la nombran por su posición.
 *
 * @param group           grupo de filas: 0 {@code Header rows}, 1 {@code Body}, 2 {@code Footer rows}
 * @param row             fila dentro de su grupo, desde 1; la de una celda que ocupa varias es la primera
 * @param column          columna en que empieza, desde 1
 * @param columnAlign     {@code Column align} de su columna; {@code null} si la tabla no lo declara
 * @param columnAlignLine línea de ese {@code Column align}
 * @param rowClass        el {@code Style} de su {@code Row}, cuyas reglas {@code For style} se aplican a la celda;
 *                        {@code null} si la fila no lo declara
 * @param rowId           el {@code Id} de su {@code Row}, cuya regla {@code For id} se aplica a la celda; {@code null}
 *                        si la fila no lo declara
 */
record CellPosition(int group, int row, int column, TextAlign columnAlign, int columnAlignLine,
                    Node rowClass, Node rowId) {

    static final int HEADER = 0;
    static final int BODY = 1;
    static final int FOOTER = 2;
}
