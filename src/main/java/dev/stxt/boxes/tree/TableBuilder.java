package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;

import java.util.List;
import java.util.Optional;

import static dev.stxt.boxes.tree.ModelValidator.font;
import static dev.stxt.boxes.tree.Nodes.child;
import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.inline;
import static dev.stxt.boxes.tree.Nodes.is;
import static dev.stxt.boxes.tree.Nodes.label;
import static dev.stxt.boxes.tree.PropertyKey.ITEM_AREA;

/**
 * Las filas de una {@code Table} (ESPECIFICACION.md §5.15) como ítems de un grid con colocación
 * explícita, y el texto de una {@code Cell} con valor.
 */
final class TableBuilder {

    /** Grupos de filas de una {@code Table}, en el orden en que van. */
    private static final List<String> TABLE_GROUPS = List.of("head", "body", "foot");

    private TableBuilder() {
    }

    /** Construye la caja de una {@code Cell}, con su {@code Row}: la tabla no sabe construir elementos. */
    @FunctionalInterface
    interface Cells {
        Box build(InlineNode cell, InlineNode row);
    }

    /** Filas y columnas que ocupa una {@code Cell}, y la línea de su {@code Span} (o de la celda, sin él). */
    private record Span(int rows, int columns, int line) {
    }

    /**
     * Las filas de una {@code Table} (§5.15) como ítems de un grid con colocación explícita: cada
     * {@code Cell} va en la primera columna libre de su fila, contando las que ocupan los
     * {@code Span} de las filas de arriba. Los grupos van en el orden {@code Head}, {@code Body},
     * {@code Foot}; cada fila llena exactamente las columnas de {@code Columns}, y un
     * {@code Span} no sale de la tabla ni de su grupo. Las filas de {@code Head} y {@code Foot}
     * se repiten en cada página y no admiten {@code Break}. {@code cells} construye la caja de
     * cada celda, con la tabla como padre.
     */
    static void table(InlineNode n, Box table, Cells cells) {
        int columns = table.style.gridColumns.size();
        int last = -1;
        int firstRow = 0;
        for (Node g : n.getChildren()) {
            int group = TABLE_GROUPS.indexOf(g.getCanonicalName());
            if (group < 0) {
                continue;
            }
            if (group < last) {
                throw error(g, ErrorCode.TABLE_ORDER, g.getName() + " va después de "
                        + label(TABLE_GROUPS.get(last)) + ": los grupos de una Table van en el orden Head, Body, Foot");
            }
            last = group;
            List<InlineNode> rows = inline(g).getChildren().stream().map(Nodes::inline).toList();
            boolean[][] taken = new boolean[rows.size()][columns];
            for (int r = 0; r < rows.size(); r++) {
                InlineNode row = rows.get(r);
                if (group != 1) {
                    child(row, "break").ifPresent(b -> {
                        throw error(b, ErrorCode.TABLE_ROW_BREAK, "Las filas de " + g.getName() + " se repiten en "
                                + "cada página de la tabla y no se cortan: quita el Break");
                    });
                }
                int c = 0;
                for (Node h : row.getChildren()) {
                    if (!is(h, "cell")) {
                        continue;
                    }
                    InlineNode cell = inline(h);
                    while (c < columns && taken[r][c]) {
                        c++;
                    }
                    if (c == columns) {
                        throw error(cell, ErrorCode.TABLE_ROW_CELLS, "No queda columna libre para esta Cell: la tabla "
                                + "tiene " + columns + " (Columns), contando las que ocupan los Span de las filas de arriba");
                    }
                    Span span = span(cell);
                    if (c + span.columns() > columns) {
                        throw error(span.line(), ErrorCode.TABLE_SPAN, "La celda ocupa de la columna " + (c + 1)
                                + " a la " + (c + span.columns()) + ", y la tabla tiene " + columns + " (Columns)");
                    }
                    if (r + span.rows() > rows.size()) {
                        throw error(span.line(), ErrorCode.TABLE_SPAN, "La celda ocupa " + span.rows() + " filas y en su "
                                + g.getName() + " quedan " + (rows.size() - r) + ": un Span no pasa de un grupo a otro");
                    }
                    for (int rr = r; rr < r + span.rows(); rr++) {
                        for (int cc = c; cc < c + span.columns(); cc++) {
                            if (taken[rr][cc]) {
                                throw error(span.line(), ErrorCode.TABLE_SPAN, "La celda se solapa en la columna "
                                        + (cc + 1) + " con otra que ocupa varias filas");
                            }
                            taken[rr][cc] = true;
                        }
                    }
                    Box box = cells.build(cell, row);
                    box.style.declared(ITEM_AREA, cell.getLine());
                    box.style.itemArea = (firstRow + r + 1) + " " + (c + 1) + " " + span.rows() + " " + span.columns();
                    table.addChild(box);
                    c += span.columns();
                }
                for (int cc = 0; cc < columns; cc++) {
                    if (!taken[r][cc]) {
                        throw error(row, ErrorCode.TABLE_ROW_CELLS, "La fila deja libre la columna " + (cc + 1)
                                + " de " + columns + " (Columns): añade una Cell, vacía si hace falta, o usa Span");
                    }
                }
            }
            if (group == 0) {
                table.headRows = rows.size();
            } else if (group == 2) {
                table.footRows = rows.size();
            }
            firstRow += rows.size();
        }
    }

    /** El {@code Span} de una celda: {@code filas columnas}, dos enteros positivos; sin él, 1 1. */
    private static Span span(InlineNode cell) {
        Optional<Node> node = child(cell, "span");
        if (node.isEmpty()) {
            return new Span(1, 1, cell.getLine());
        }
        String text = node.get().getText().trim();
        String[] t = text.split("\\s+");
        if (t.length != 2 || !t[0].matches("[1-9]\\d*") || !t[1].matches("[1-9]\\d*")) {
            throw error(node.get(), ErrorCode.VALUE_SYNTAX, "Span espera 'filas columnas', dos enteros "
                    + "positivos: '" + text + "'");
        }
        return new Span(Integer.parseInt(t[0]), Integer.parseInt(t[1]), node.get().getLine());
    }

    /** El texto de una {@code Cell} con valor: una línea, sin elementos al lado. */
    static void cellText(InlineNode n, Box box) {
        n.getChildren().stream().filter(Nodes::isElement).findFirst().ifPresent(e -> {
            throw error(e, ErrorCode.CELL_CONTENT, "Una Cell tiene texto en el valor (línea " + n.getLine()
                    + ") o elementos, no las dos cosas: pon el texto en un Paragraph");
        });
        font(n, n, box);
        box.text = n.getText();
        box.textLine = n.getLine();
    }
}
