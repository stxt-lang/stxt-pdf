package dev.stxt.boxes.fragment;

/**
 * Una line box ya colocada, relativa a la caja de contenido de su fragmento.
 *
 * @param x      desplazamiento horizontal por la alineación
 * @param y      parte superior de la línea
 * @param width  ancho del texto de la línea
 * @param height altura de la línea ({@code Line height})
 * @param text   contenido de la línea
 */
public record TextLine(double x, double y, double width, double height, String text) {
}
