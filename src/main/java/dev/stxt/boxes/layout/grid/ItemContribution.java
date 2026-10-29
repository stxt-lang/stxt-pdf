package dev.stxt.boxes.layout.grid;

/**
 * Lo que aporta un ítem al dimensionado de pistas en un eje (ESPECIFICACION.md §5.8). Para columnas son sus
 * anchos min-content y max-content; para filas, su altura tras el layout con el ancho ya resuelto.
 *
 * @param track primera pista que ocupa (desde 0)
 * @param span  número de pistas que ocupa
 */
public record ItemContribution(int track, int span, double minContent, double maxContent) {
}
