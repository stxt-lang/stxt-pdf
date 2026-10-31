package dev.stxt.pdf.model;

/**
 * Texto preformateado (referencia/comun/formato-del-texto.md): {@code false} colapsa el espacio en blanco; {@code true}
 * conserva los espacios y los saltos de línea y no parte las líneas; {@code wrap} también, pero parte las líneas
 * demasiado anchas entre palabras.
 */
public enum Preformatted {
    FALSE, TRUE, WRAP;

    /** Si conserva los espacios y los saltos de línea del texto. */
    public boolean enabled() {
        return this != FALSE;
    }
}
