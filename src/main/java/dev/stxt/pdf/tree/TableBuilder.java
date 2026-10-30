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
 * Las filas de una {@code Table} (referencia/componentes/table.md) como ítems de un grid con colocación
 * explícita, y el texto de una {@code Cell} con valor.
 */
final class TableBuilder {

    /** Grupos de filas de una {@code Table}, en el orden en que van. */
    private static final List<String> TABLE_GROUPS = List.of("head", "body", "foot");

    /** Lo que admite el {@code Box} de una {@code Row}: su aspecto (referencia/componentes/table.md#filas). */
    private static final Set<String> ROW_LOOK = Set.of("background", "border", "border-color", "radius");

    private TableBuilder() {
    }

    /**
     * Construye la caja de una {@code Cell}, con su {@code Row} y su posición en la tabla: la tabla
     * no sabe construir elementos.
     */
    @FunctionalInterface
    interface Cells {
        Box build(InlineNode cell, InlineNode row, CellPosition position);
    }

    /** Filas y columnas que ocupa una {@code Cell}, y la línea de su {@code Span} (o de la celda, sin él). */
    private record Span(int rows, int columns, int line) {
    }

    /**
     * Las filas de una {@code Table} (referencia/componentes/table.md) como ítems de un grid con colocación explícita:
     * cada {@code Cell} va en la primera columna libre de su fila, contando las que ocupan los {@code Span} de las
     * filas de arriba. Los grupos van en el orden {@code Head}, {@code Body}, {@code Foot}; cada fila llena exactamente
     * las columnas de {@code Columns}, y un {@code Span} no sale de la tabla ni de su grupo. Las filas de {@code Head}
     * y {@code Foot} se repiten en cada página y no admiten {@code Break}. {@code cells} construye la caja de cada
     * celda, con la tabla como padre. Cada celda lleva su posición ({@link CellPosition}) para las reglas de {@code
     * Styles} y el {@code Column align} de su columna. Una fila con {@code Id} o {@code Box} tiene además su caja
     * ({@link #row}).
     */
    static void table(BuildContext ctx, InlineNode n, Box table, Cells cells) {
        int count = table.style().grid().columns().size();
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
                throw error(g, ErrorCode.TABLE_ORDER, g.getName() + " va después de "
                        + label(TABLE_GROUPS.get(last)) + ": los grupos de una Table van en el orden Head, Body, Foot");
            }
            last = group;
            int rows = rows(ctx, g, group, firstRow, columns, table, cells);
            if (group == 0) {
                table.setHeadRows(rows);
            } else if (group == 2) {
                table.setFootRows(rows);
            }
            firstRow += rows;
        }
    }

    /** Las columnas de la tabla: cuántas hay ({@code Columns}) y su {@code Column align}, con su línea (0 sin él). */
    private record Columns(int count, List<TextAlign> aligns, int alignLine) {
    }

    /**
     * Las filas de un grupo ({@code g}, el {@code group}-ésimo de {@link #TABLE_GROUPS}), cuya primera es la fila
     * {@code firstRow} de la tabla (desde 0). Construye sus celdas y devuelve cuántas filas tiene.
     */
    private static int rows(BuildContext ctx, Node g, int group, int firstRow, Columns columns, Box table,
                            Cells cells) {
        List<InlineNode> rows = inline(g).getChildren().stream().map(Nodes::inline).toList();
        boolean[][] taken = new boolean[rows.size()][columns.count()];
        for (int r = 0; r < rows.size(); r++) {
            InlineNode row = rows.get(r);
            if (group != 1) {
                child(row, "break").ifPresent(b -> {
                    throw error(b, ErrorCode.TABLE_ROW_BREAK, "Las filas de " + g.getName() + " se repiten en "
                            + "cada página de la tabla y no se cortan: quita el Break");
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
                        child(row, "class").orElse(null), child(row, "id").orElse(null)));
                box.style().declared(ITEM_AREA, cell.getLine());
                box.style().item().setArea(new Cell(firstRow + r + 1, c + 1, span.rows(), span.columns()));
                table.addChild(box);
                c += span.columns();
            }
            for (int cc = 0; cc < columns.count(); cc++) {
                if (!taken[r][cc]) {
                    throw error(row, ErrorCode.TABLE_ROW_CELLS, "La fila deja libre la columna " + (cc + 1)
                            + " de " + columns.count() + " (Columns): añade una Cell, vacía si hace falta, o usa Span");
                }
            }
        }
        return rows.size();
    }

    /**
     * La caja de una {@code Row} con {@code Id} o {@code Box} (referencia/componentes/table.md#filas): la franja de la
     * fila {@code tableRow} de la tabla (desde 1), de la primera columna a la última, que se pinta debajo de sus
     * celdas. Su {@code Box} solo admite el aspecto ({@code ROW_BOX}); lee también su {@code Typography} (el color de
     * su borde por defecto) y su {@code Break} (su {@code Decoration}). Sin {@code Id} ni {@code Box}, la fila no tiene
     * caja.
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
                    throw error(p, ErrorCode.ROW_BOX, p.getName() + " no tiene efecto en una Row, que no es una caja "
                            + "del layout: su Box solo admite Background, Border, Border color y Radius; dáselo a sus "
                            + "celdas");
                }
            }
            box(b, s);
        });
        Box box = new Box(id.map(ctx::id).orElse(null), Element.ROW, s, false, row.getLine());
        s.item().setArea(new Cell(tableRow, 1, 1, columns));
        table.addRow(box);
    }

    /** La primera columna libre de la fila desde {@code from}, contando las que ocupan los {@code Span} de arriba. */
    private static int freeColumn(boolean[] taken, int from, InlineNode cell) {
        int c = from;
        while (c < taken.length && taken[c]) {
            c++;
        }
        if (c == taken.length) {
            throw error(cell, ErrorCode.TABLE_ROW_CELLS, "No queda columna libre para esta Cell: la tabla "
                    + "tiene " + taken.length + " (Columns), contando las que ocupan los Span de las filas de arriba");
        }
        return c;
    }

    /**
     * Marca como ocupadas las filas y columnas de la celda que empieza en la fila {@code r} y la columna {@code c}
     * de su grupo ({@code group}, su nombre): un {@code Span} no sale de la tabla ni del grupo, ni se solapa.
     */
    private static void occupy(boolean[][] taken, int r, int c, Span span, String group) {
        int columns = taken[r].length;
        if (c + span.columns() > columns) {
            throw error(span.line(), ErrorCode.TABLE_SPAN, "La celda ocupa de la columna " + (c + 1)
                    + " a la " + (c + span.columns()) + ", y la tabla tiene " + columns + " (Columns)");
        }
        if (r + span.rows() > taken.length) {
            throw error(span.line(), ErrorCode.TABLE_SPAN, "La celda ocupa " + span.rows() + " filas y en su "
                    + group + " quedan " + (taken.length - r) + ": un Span no pasa de un grupo a otro");
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
    }

    /**
     * El {@code Column align} de una {@code Table}: una alineación por columna, en el orden de
     * {@code Columns}. Vacío si no lo declara.
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
                throw error(node.get(), ErrorCode.VALUE_NOT_ALLOWED, "Valor no permitido en Column align: " + word
                        + " (left, center, right o justify)");
            }
        }
        if (aligns.size() != columns) {
            throw error(node.get(), ErrorCode.TABLE_COLUMN_ALIGN, "Column align da " + aligns.size()
                    + " alineaciones y la tabla tiene " + columns + " columnas (Columns): escribe una por columna");
        }
        return aligns;
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
        box.setText(n.getText(), n.getLine(), false);
    }
}
