package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;

import java.util.List;
import java.util.Locale;

import static dev.stxt.boxes.tree.Nodes.error;

/**
 * Listas (ESPECIFICACION.md §5.14): numeración de los {@code List item}, la caja generada de su
 * marcador y las reglas de {@code Start} y {@code Number}.
 */
final class ListBuilder {

    private ListBuilder() {
    }

    /**
     * Número de un {@code List item} (§5.14): el de su {@code Number} o el siguiente al del ítem
     * anterior, empezando por el {@code Start} de la lista. En una lista sin numeración, 0.
     */
    static int itemNumber(BuildContext ctx, InlineNode n, Style s, Box list) {
        Style.Marker marker = list.style.listMarker;
        if (!marker.numbered()) {
            if (s.isDeclared("number")) {
                throw error(s.line("number"), ErrorCode.LIST_NUMBER_NOT_ALLOWED, "Number no tiene efecto: la lista "
                        + "(línea " + list.line + ") tiene Marker: " + markerName(marker) + ", que no numera sus ítems");
            }
            return 0;
        }
        List<Box> previous = list.children;
        int number = s.listNumber != null ? s.listNumber
                : previous.isEmpty() ? list.style.listStart
                : ctx.itemNumbers.get(previous.get(previous.size() - 1)) + 1;
        if (!ListMarkers.inRange(marker, number)) {
            throw error(s.listNumber != null ? s.line("number") : n.getLine(), ErrorCode.LIST_NUMBER_RANGE,
                    "El número " + number + " no se puede escribir con Marker: " + markerName(marker)
                            + ", que numera " + ListMarkers.range(marker));
        }
        return number;
    }

    /**
     * La caja generada del marcador de un {@code List item} (§5.14): texto con la tipografía del
     * ítem, que necesita, como cualquier texto, {@code Font} y {@code Size}.
     */
    static Box marker(InlineNode n, Box item, String text) {
        if (item.style.font == null) {
            throw error(n, ErrorCode.TEXT_WITHOUT_FONT, "El marcador del List item necesita Font: declárala en "
                    + "el Typography del List item, de un antecesor o de Document");
        }
        if (!item.style.isDeclared("font-size")) {
            throw error(n, ErrorCode.TEXT_WITHOUT_FONT_SIZE, "El marcador del List item necesita Size: decláralo "
                    + "en el Typography del List item, de un antecesor o de Document");
        }
        Style s = Style.inheriting(item.style);
        s.layout = Style.Layout.INLINE;
        Box marker = new Box(null, Element.MARKER, s, false, n.getLine());
        marker.parent = item;
        marker.text = text;
        marker.textLine = n.getLine();
        return marker;
    }

    /**
     * Comprueba que el marcador de un {@code List item} tiene una línea con la que alinearse
     * (§5.14): bajando por el primer hijo en flujo de cada bloque se llega a un texto.
     */
    static void markerLine(Box item) {
        Box b = item;
        while (b.style.layout != Style.Layout.INLINE) {
            String problem = null;
            if (b.style.layout == Style.Layout.FLEX || b.style.layout == Style.Layout.GRID) {
                problem = "empieza con un " + b.element.label();
            } else if (b.inFlowChildren().isEmpty()) {
                problem = b == item ? "no tiene contenido en flujo" : "empieza con un " + b.element.label() + " vacío";
            }
            if (problem != null) {
                throw error(b.line, ErrorCode.LIST_ITEM_WITHOUT_LINE, "El marcador del List item de la línea "
                        + item.line + " se alinea con su primera línea, y el ítem " + problem
                        + ": empieza con un texto, o usa Marker: none");
            }
            b = b.inFlowChildren().get(0);
        }
    }

    /** {@code Start} solo numera, y en el rango de la numeración. */
    static void validateList(Style s) {
        if (!s.isDeclared("start")) {
            return;
        }
        if (!s.listMarker.numbered()) {
            throw error(s.line("start"), ErrorCode.LIST_NUMBER_NOT_ALLOWED, "Start no tiene efecto con Marker: "
                    + markerName(s.listMarker) + ", que no numera los ítems");
        }
        if (!ListMarkers.inRange(s.listMarker, s.listStart)) {
            throw error(s.line("start"), ErrorCode.LIST_NUMBER_RANGE, "Start: " + s.listStart + " no se puede "
                    + "escribir con Marker: " + markerName(s.listMarker) + ", que numera "
                    + ListMarkers.range(s.listMarker));
        }
    }

    /** Un marcador como se escribe: {@code LOWER_ALPHA} → {@code lower-alpha}. */
    private static String markerName(Style.Marker marker) {
        return marker.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
