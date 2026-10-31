package dev.stxt.pdf.tree;

/**
 * Una línea y el documento en el que está: el principal o el documento de estilos aparte ({@code Styles},
 * reference/common/styles.md). Es lo que guarda {@link Style} de cada propiedad declarada, porque una propiedad que
 * llega de una regla del documento de estilos tiene la línea de ese documento, y los errores que la señalan tienen que
 * decirlo.
 *
 * @param number   el número de línea, desde 1
 * @param inStyles si la línea es del documento de estilos aparte
 */
public record SourceLine(int number, boolean inStyles) {

    /** Una línea del documento principal. */
    public static SourceLine of(int number) {
        return new SourceLine(number, false);
    }

    /** Una línea del documento de estilos aparte. */
    public static SourceLine inStyles(int number) {
        return new SourceLine(number, true);
    }

    /** {@code línea 8} o {@code línea 8 de los estilos}, para los mensajes. */
    @Override
    public String toString() {
        return "línea " + number + (inStyles ? " de los estilos" : "");
    }
}
