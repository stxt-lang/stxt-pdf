package dev.stxt.pdf.model;

/**
 * Cómo se escribe el número de un nivel de los encabezados ({@code Number style}, referencia/componentes/heading.md):
 * en cifras, en letras o en números romanos, como las numeraciones de las listas.
 */
public enum NumberStyle {
    DECIMAL, LOWER_ALPHA, UPPER_ALPHA, LOWER_ROMAN, UPPER_ROMAN;

    /** La numeración de lista que escribe igual los números. */
    public Marker marker() {
        return Marker.valueOf(name());
    }
}
