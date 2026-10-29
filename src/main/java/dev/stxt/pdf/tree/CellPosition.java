package dev.stxt.pdf.tree;

import dev.stxt.Node;
import dev.stxt.pdf.model.TextAlign;

/**
 * Dónde está una {@code Cell} en su tabla (referencia/componentes/table.md), para las reglas de
 * {@code Styles} que la nombran por su posición.
 *
 * @param group           grupo de filas: 0 {@code Head}, 1 {@code Body}, 2 {@code Foot}
 * @param row             fila dentro de su grupo, desde 1; la de una celda que ocupa varias es la primera
 * @param column          columna en que empieza, desde 1
 * @param columnAlign     {@code Column align} de su columna; {@code null} si la tabla no lo declara
 * @param columnAlignLine línea de ese {@code Column align}
 * @param rowClass        el {@code Class} de su {@code Row}, cuyas reglas {@code For class} se aplican a la celda;
 *                        {@code null} si la fila no lo declara
 */
record CellPosition(int group, int row, int column, TextAlign columnAlign, int columnAlignLine,
                    Node rowClass) {

    static final int HEAD = 0;
    static final int BODY = 1;
    static final int FOOT = 2;
}
