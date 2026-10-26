package dev.stxt.boxes.layout;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.tree.Style;

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
     * Convierte un ancho o alto especificado en tamaño de caja de borde, según {@code Box sizing}.
     * Con {@code Box sizing: content}, el valor declarado es el del contenido y se le suman
     * relleno y borde.
     */
    public static double toBorderBox(double declared, Style.BoxSizing boxSizing, double paddingAndBorder) {
        return boxSizing == Style.BoxSizing.CONTENT ? declared + paddingAndBorder : declared;
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
}
