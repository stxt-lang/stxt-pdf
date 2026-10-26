package dev.stxt.boxes.fragmentos;

/**
 * Una line box ya colocada, relativa a la caja de contenido de su fragmento.
 *
 * @param x     desplazamiento horizontal por la alineación
 * @param y     parte superior de la línea
 * @param ancho ancho del texto de la línea
 * @param alto  altura de la línea ({@code Interlineado})
 * @param texto contenido de la línea
 */
public record LineaTexto(double x, double y, double ancho, double alto, String texto) {
}
