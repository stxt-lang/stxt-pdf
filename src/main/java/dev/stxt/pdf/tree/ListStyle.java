package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Marker;

/**
 * Las propiedades de las listas (referencia/componentes/list.md): el {@code Marker} y el {@code Start} de una {@code
 * List} y el {@code Number} de un {@code List item}. No se heredan.
 */
public final class ListStyle extends StyleGroup {

    /** Marcador de los ítems de una {@code List}; {@code null} en el resto de cajas. */
    private Marker marker;
    /** Número del primer ítem de una {@code List}. */
    private int start = 1;
    /** Número que fija un {@code List item}; {@code null} = el siguiente al del ítem anterior. */
    private Integer number;

    ListStyle() {
    }

    public Marker marker() {
        return marker;
    }

    void setMarker(Marker v) {
        writable();
        marker = v;
    }

    public int start() {
        return start;
    }

    void setStart(int v) {
        writable();
        start = v;
    }

    public Integer number() {
        return number;
    }

    void setNumber(Integer v) {
        writable();
        number = v;
    }
}
