package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Break;

/**
 * Una unidad de la fragmentación de un contenedor (referencia/comun/cortes.md): una línea de un
 * contenedor stack en {@code row}, un ítem de uno en {@code column} o una fila de una rejilla. Se
 * corta entre unidades, nunca dentro de una (salvo partiendo sus ítems en paralelo).
 *
 * @param start        principio de la unidad, desde la caja de contenido del contenedor
 * @param end          final de la unidad
 * @param appealBefore calidad del corte delante de la unidad ({@link BreakAppeal}); negativa si
 *                     ahí no se puede cortar (un ítem de grid que ocupa esta fila y la anterior).
 *                     La de la primera unidad no se usa
 * @param forcedBefore corte forzado delante de la unidad: {@code PAGE}, {@code COLUMN} o {@code AUTO}
 *                     si no hay ({@link BreakRules#forcedBetween(java.util.List, java.util.List)})
 */
public record Unit(double start, double end, int appealBefore, Break forcedBefore) {

    /** Si se puede cortar delante de la unidad. */
    public boolean breakableBefore() {
        return appealBefore >= 0;
    }

    /** La misma unidad con las posiciones desde {@code origin}. */
    public Unit shifted(double origin) {
        return new Unit(start - origin, end - origin, appealBefore, forcedBefore);
    }
}
