package dev.stxt.boxes.layout;

import dev.stxt.boxes.arbol.Box;

/**
 * Caja {@code absoluta} encontrada durante el layout en flujo, pendiente de resolver cuando su
 * containing block conozca su tamaño (módulo 4).
 *
 * @param caja      la caja absoluta
 * @param xEstatica posición que habría tenido en flujo (para izquierda {@code auto})
 * @param yEstatica ídem (para arriba {@code auto})
 */
public record PendienteAbsoluta(Box caja, double xEstatica, double yEstatica) {
}
