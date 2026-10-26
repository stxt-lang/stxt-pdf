package dev.stxt.boxes.layout.grid;

/**
 * Lo que aporta un ítem al dimensionado de pistas en un eje (módulo 6). Para columnas son sus
 * anchos min-content y max-content; para filas, su altura tras el layout con el ancho ya resuelto.
 *
 * @param pista      primera pista que ocupa (desde 0)
 * @param extension  número de pistas que ocupa
 */
public record AportacionItem(int pista, int extension, double minContent, double maxContent) {
}
