package dev.stxt.pdf.model;

/**
 * Tratamiento del espacio en blanco (referencia/comun/formato-del-texto.md): {@code normal} lo colapsa; {@code
 * preserve} conserva los espacios y los saltos de línea y no parte las líneas; {@code preserve-wrap} también, pero
 * parte las líneas demasiado anchas entre palabras.
 */
public enum WhiteSpace {
    NORMAL, PRESERVE, PRESERVE_WRAP;

    /** Si conserva los espacios y los saltos de línea del texto. */
    public boolean preformatted() {
        return this != NORMAL;
    }
}
