package dev.stxt.pdf.layout.stack;

/**
 * Datos de un ítem para el reparto en el eje principal (referencia/componentes/stack.md). Tamaños de caja de margen.
 *
 * @param basis   tamaño base ({@code Base size}, el tamaño declarado o el ancho natural)
 * @param grow    factor {@code Grow}
 * @param shrink  factor {@code Shrink}
 * @param min     mínimo; con {@code Min width: auto} es el tamaño de su contenido, sin pasar del
 *                tamaño declarado (referencia/componentes/stack.md)
 * @param max     máximo; {@link Double#POSITIVE_INFINITY} si no hay
 * @param margins los márgenes del ítem en el eje principal, incluidos en los demás tamaños
 */
public record StackItem(double basis, double grow, double shrink, double min, double max, double margins) {

    public static StackItem of(double basis, double grow, double shrink, double min) {
        return new StackItem(basis, grow, shrink, min, Double.POSITIVE_INFINITY, 0);
    }

    /**
     * Lo que pesa al encoger: {@code Shrink × Base size}, con la base sin los márgenes (referencia/componentes/stack.md).
     */
    double scaledShrink() {
        return shrink * (basis - margins);
    }

    double hypothetical() {
        return Math.max(min, Math.min(basis, max));
    }

    double clamp(double v) {
        return Math.max(min, Math.min(v, max));
    }
}
