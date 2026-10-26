package dev.stxt.boxes.layout;

import dev.stxt.boxes.tree.Box;

/**
 * Caja {@code absolute} encontrada durante el layout en flujo, pendiente de resolver cuando su
 * containing block conozca su tamaño (módulo 4).
 *
 * @param box     la caja absoluta
 * @param staticX posición que habría tenido en flujo (para izquierda {@code auto})
 * @param staticY ídem (para arriba {@code auto})
 */
public record PendingAbsolute(Box box, double staticX, double staticY) {
}
