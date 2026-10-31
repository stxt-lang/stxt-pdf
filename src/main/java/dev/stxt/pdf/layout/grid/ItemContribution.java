package dev.stxt.pdf.layout.grid;

/**
 * Lo que aporta un ítem al dimensionado de pistas en un eje (reference/components/grid.md). Para columnas son sus
 * anchos más estrecho y natural; para filas, su altura tras el layout con el ancho ya resuelto.
 *
 * @param track primera pista que ocupa (desde 0)
 * @param span  número de pistas que ocupa
 */
public record ItemContribution(int track, int span, double narrowest, double natural) {
}
