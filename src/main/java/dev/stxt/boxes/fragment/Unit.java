package dev.stxt.boxes.fragment;

/**
 * Una unidad de la fragmentación de un contenedor (ESPECIFICACION.md §5.10): una línea de un
 * contenedor flex en {@code row}, un ítem de uno en {@code column} o una fila de una rejilla. Se
 * corta entre unidades, nunca dentro de una (salvo partiendo sus ítems en paralelo).
 *
 * @param start        principio de la unidad, desde la caja de contenido del contenedor
 * @param end          final de la unidad
 * @param appealBefore calidad del corte delante de la unidad ({@link BreakAppeal}); negativa si
 *                     ahí no se puede cortar (un ítem de grid que ocupa esta fila y la anterior).
 *                     La de la primera unidad no se usa
 * @param forcedBefore hay un corte forzado delante de la unidad
 */
public record Unit(double start, double end, int appealBefore, boolean forcedBefore) {

    /** Si se puede cortar delante de la unidad. */
    public boolean breakableBefore() {
        return appealBefore >= 0;
    }

    /** La misma unidad con las posiciones desde {@code origin}. */
    public Unit shifted(double origin) {
        return new Unit(start - origin, end - origin, appealBefore, forcedBefore);
    }
}
