package dev.stxt.boxes.layout.flex;

/**
 * Datos de un ítem para el reparto en el eje principal (módulo 5). Tamaños de caja de margen.
 *
 * @param base    tamaño base ({@code Base}, el tamaño declarado o el max-content)
 * @param crecer  factor {@code Crecer}
 * @param encoger factor {@code Encoger}
 * @param min     mínimo; con {@code Min ancho: auto} es el min-content del ítem
 * @param max     máximo; {@link Double#POSITIVE_INFINITY} si no hay
 */
public record ItemFlex(double base, double crecer, double encoger, double min, double max) {

    public static ItemFlex de(double base, double crecer, double encoger, double min) {
        return new ItemFlex(base, crecer, encoger, min, Double.POSITIVE_INFINITY);
    }

    double hipotetico() {
        return Math.max(min, Math.min(base, max));
    }

    double limitar(double v) {
        return Math.max(min, Math.min(v, max));
    }
}
