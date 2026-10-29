package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.Box;

/**
 * Caja {@code absolute} o {@code fixed} encontrada durante el layout en flujo, pendiente de
 * resolver cuando su containing block conozca su tamaño. Sube por los {@link LayoutResult}
 * hasta él; cada caja traslada la posición a su propia caja de contenido. La resuelve
 * {@link dev.stxt.pdf.layout.position.AbsoluteLayout}.
 *
 * @param box     la caja absoluta
 * @param staticX posición que habría tenido en flujo (para izquierda {@code auto}), relativa a
 *                la caja de contenido de la caja que devuelve el resultado
 * @param staticY ídem (para arriba {@code auto})
 */
public record PendingAbsolute(Box box, double staticX, double staticY) {

    public PendingAbsolute translated(double dx, double dy) {
        return new PendingAbsolute(box, staticX + dx, staticY + dy);
    }
}
