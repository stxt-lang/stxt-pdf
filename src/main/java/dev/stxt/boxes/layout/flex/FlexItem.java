package dev.stxt.boxes.layout.flex;

/**
 * Datos de un ítem para el reparto en el eje principal (módulo 5). Tamaños de caja de margen.
 *
 * @param basis  tamaño base ({@code Basis}, el tamaño declarado o el max-content)
 * @param grow   factor {@code Grow}
 * @param shrink factor {@code Shrink}
 * @param min    mínimo; con {@code Min width: auto} es el tamaño de su contenido, sin pasar del
 *               tamaño declarado (§5.7)
 * @param max    máximo; {@link Double#POSITIVE_INFINITY} si no hay
 */
public record FlexItem(double basis, double grow, double shrink, double min, double max) {

    public static FlexItem of(double basis, double grow, double shrink, double min) {
        return new FlexItem(basis, grow, shrink, min, Double.POSITIVE_INFINITY);
    }

    double hypothetical() {
        return Math.max(min, Math.min(basis, max));
    }

    double clamp(double v) {
        return Math.max(min, Math.min(v, max));
    }
}
