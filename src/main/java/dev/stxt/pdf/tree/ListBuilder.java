package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;

import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.NUMBER;
import static dev.stxt.pdf.tree.PropertyKey.START;

/**
 * Listas (referencia/componentes/list.md): numeración de los {@code List item}, la caja generada de su
 * marcador y las reglas de {@code Start} y {@code Number}.
 */
final class ListBuilder {

    private ListBuilder() {
    }

    /**
     * Número de un {@code List item} (referencia/componentes/list.md): el de su {@code Number} o el siguiente al del
     * ítem anterior, empezando por el {@code Start} de la lista. En una lista sin numeración, 0.
     */
    static int itemNumber(BuildContext ctx, InlineNode n, Style s, Box list) {
        Marker marker = list.style().list().marker();
        if (!marker.numbered()) {
            if (s.isDeclared(NUMBER)) {
                throw error(s.line(NUMBER), ErrorCode.LIST_NUMBER_NOT_ALLOWED, "Number no tiene efecto: la lista "
                        + "(línea " + list.line() + ") tiene Marker: " + markerName(marker) + ", que no numera sus ítems");
            }
            return 0;
        }
        List<Box> previous = list.children();
        int number = s.list().number() != null ? s.list().number()
                : previous.isEmpty() ? list.style().list().start()
                : ctx.itemNumbers.get(previous.get(previous.size() - 1)) + 1;
        if (!ListMarkers.inRange(marker, number)) {
            throw error(s.list().number() != null ? s.line(NUMBER) : SourceLine.of(n.getLine()),
                    ErrorCode.LIST_NUMBER_RANGE,
                    "El número " + number + " no se puede escribir con Marker: " + markerName(marker)
                            + ", que numera " + ListMarkers.range(marker));
        }
        return number;
    }

    /**
     * La caja generada del marcador de un {@code List item} (referencia/componentes/list.md): texto con la tipografía
     * del ítem, que necesita, como cualquier texto, {@code Font} y {@code Size}.
     */
    static Box marker(InlineNode n, Box item, String text) {
        if (item.style().typography().font() == null) {
            throw error(n, ErrorCode.TEXT_WITHOUT_FONT, "El marcador del List item necesita Font: se declara en "
                    + "el Typography del List item, de un antecesor o de Styles (For element: Document)");
        }
        if (!item.style().isDeclared(FONT_SIZE)) {
            throw error(n, ErrorCode.TEXT_WITHOUT_FONT_SIZE, "El marcador del List item necesita Size: se declara "
                    + "en el Typography del List item, de un antecesor o de Styles (For element: Document)");
        }
        Style s = Style.inheriting(item.style());
        s.setLayout(Layout.INLINE);
        Box marker = new Box(null, Element.MARKER, s, false, n.getLine());
        marker.setText(text, n.getLine(), false);
        return marker;
    }

    /**
     * Comprueba que el marcador de un {@code List item} tiene una línea con la que alinearse
     * (referencia/componentes/list.md): bajando por el primer hijo en flujo de cada bloque se llega a un texto.
     */
    static void markerLine(Box item) {
        Box b = item;
        while (b.style().layout() != Layout.INLINE) {
            String problem = null;
            if (b.style().layout() == Layout.STACK || b.style().layout() == Layout.GRID) {
                problem = "empieza con un " + b.element().label();
            } else if (b.isImage()) {
                problem = "empieza con una Image";
            } else if (b.inFlowChildren().isEmpty()) {
                problem = b == item ? "no tiene contenido en flujo" : "empieza con un " + b.element().label() + " vacío";
            }
            if (problem != null) {
                throw error(b.line(), ErrorCode.LIST_ITEM_WITHOUT_LINE, "El marcador del List item de la línea "
                        + item.line() + " se alinea con su primera línea, y el ítem " + problem
                        + ": tiene que empezar con un texto, o llevar Marker: none");
            }
            b = b.inFlowChildren().get(0);
        }
    }

    /** {@code Start} solo numera, y en el rango de la numeración. */
    static void validateList(Style s) {
        if (!s.isDeclared(START)) {
            return;
        }
        if (!s.list().marker().numbered()) {
            throw error(s.line(START), ErrorCode.LIST_NUMBER_NOT_ALLOWED, "Start no tiene efecto con Marker: "
                    + markerName(s.list().marker()) + ", que no numera los ítems");
        }
        if (!ListMarkers.inRange(s.list().marker(), s.list().start())) {
            throw error(s.line(START), ErrorCode.LIST_NUMBER_RANGE, "Start: " + s.list().start() + " no se puede "
                    + "escribir con Marker: " + markerName(s.list().marker()) + ", que numera "
                    + ListMarkers.range(s.list().marker()));
        }
    }

    /** Un marcador como se escribe: {@code LOWER_ALPHA} → {@code lower-alpha}. */
    private static String markerName(Marker marker) {
        return marker.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
