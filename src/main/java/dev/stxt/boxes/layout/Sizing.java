package dev.stxt.boxes.layout;

import dev.stxt.boxes.model.Alignment;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Value;

import java.util.OptionalDouble;

/**
 * Reglas comunes de dimensionado (módulos 1 y 7, ESPECIFICACION.md §5.9).
 */
public final class Sizing {

    private Sizing() {
    }

    /** Aplica primero el máximo y después el mínimo: si chocan, gana el mínimo. */
    public static double constrain(double provisional, double min, OptionalDouble max) {
        double v = max.isPresent() ? Math.min(provisional, max.getAsDouble()) : provisional;
        return Math.max(v, min);
    }

    /**
     * Ancho de caja de borde de una caja en flujo de bloque con {@code Width: auto}: ocupa el
     * containing block menos sus márgenes horizontales.
     */
    public static double autoWidthInFlow(double containingBlockWidth, Insets margin) {
        return Math.max(0, containingBlockWidth - margin.horizontal());
    }

    /**
     * Márgenes horizontales de una caja en flujo de bloque con ancho conocido: si ambos son
     * {@code auto}, el sobrante se reparte y la caja queda centrada; si solo uno es {@code auto},
     * absorbe el sobrante.
     *
     * @return {izquierdo, derecho}
     */
    public static double[] horizontalMargins(double containingBlockWidth, double borderBoxWidth,
                                             Value marginLeft, Value marginRight) {
        double left = marginLeft.resolve(containingBlockWidth).orElse(0);
        double right = marginRight.resolve(containingBlockWidth).orElse(0);
        double remaining = containingBlockWidth - borderBoxWidth - left - right;
        if (marginLeft.isAuto() && marginRight.isAuto()) {
            double each = Math.max(0, remaining) / 2;
            return new double[]{each, each};
        }
        if (marginLeft.isAuto()) {
            return new double[]{Math.max(0, remaining), right};
        }
        if (marginRight.isAuto()) {
            return new double[]{left, Math.max(0, remaining)};
        }
        return new double[]{left, right};
    }

    /**
     * Desplazamiento de una caja de tamaño {@code size} alineada en un espacio {@code space}
     * (§5.7, §5.8). Con {@code stretch} la caja ya ocupa el espacio y empieza al principio.
     */
    public static double align(Alignment alignment, double space, double size) {
        return switch (alignment) {
            case START, STRETCH -> 0;
            case END -> space - size;
            case CENTER -> (space - size) / 2;
        };
    }

    public static double sum(double[] v) {
        double r = 0;
        for (double x : v) {
            r += x;
        }
        return r;
    }
}
