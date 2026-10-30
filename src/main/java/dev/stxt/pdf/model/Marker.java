package dev.stxt.pdf.model;

/** Marcador de los ítems de una lista: un símbolo, ninguno o una numeración. */
public enum Marker {
    BULLET, DASH, NONE, DECIMAL, LOWER_ALPHA, UPPER_ALPHA, LOWER_ROMAN, UPPER_ROMAN;

    /** Si numera los ítems: solo entonces tienen efecto {@code Start} y {@code Number}. */
    public boolean numbered() {
        return compareTo(DECIMAL) >= 0;
    }
}
