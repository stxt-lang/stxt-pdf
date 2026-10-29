package dev.stxt.boxes.model;

/**
 * Tratamiento del espacio en blanco (§4.4, §5.4): {@code normal} lo colapsa; {@code pre} conserva
 * los espacios y los saltos de línea y no parte las líneas; {@code pre-wrap} también, pero parte
 * las líneas demasiado anchas entre palabras.
 */
public enum WhiteSpace {
    NORMAL, PRE, PRE_WRAP;

    /** Si conserva los espacios y los saltos de línea del texto. */
    public boolean preformatted() {
        return this != NORMAL;
    }
}
