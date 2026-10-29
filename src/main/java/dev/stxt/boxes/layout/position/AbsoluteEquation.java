package dev.stxt.boxes.layout.position;

import dev.stxt.boxes.layout.IntrinsicSizes;

import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

/**
 * Resuelve la posición y el tamaño de una caja {@code absolute} o {@code fixed} en un eje
 * (ESPECIFICACION.md §5.5). Siempre se cumple:
 *
 * <pre>inicio + margen inicio + tamaño + margen fin + fin = tamaño del containing block</pre>
 *
 * <p>Un {@link OptionalDouble} vacío representa {@code auto}. El tamaño es el de la caja de borde.</p>
 */
public final class AbsoluteEquation {

    /** Solución de la ecuación en un eje. */
    public record Solution(double start, double marginStart, double size, double marginEnd, double end) {
    }

    private AbsoluteEquation() {
    }

    /**
     * Eje horizontal. Un ancho {@code auto} que no se puede despejar se ajusta al contenido
     * (<em>shrink-to-fit</em>). Si está sobre-restringida, se ignora {@code right}.
     */
    public static Solution horizontal(OptionalDouble left, OptionalDouble width, OptionalDouble right,
                                      OptionalDouble marginLeft, OptionalDouble marginRight,
                                      double containingBlockWidth, double staticLeft,
                                      IntrinsicSizes content) {
        return solve(left, width, right, marginLeft, marginRight, containingBlockWidth,
                staticLeft, content::fitContent);
    }

    /**
     * Eje vertical. Un alto {@code auto} que no se puede despejar toma la altura del contenido.
     * Si está sobre-restringida, se ignora {@code bottom}.
     */
    public static Solution vertical(OptionalDouble top, OptionalDouble height, OptionalDouble bottom,
                                    OptionalDouble marginTop, OptionalDouble marginBottom,
                                    double containingBlockHeight, double staticTop,
                                    double contentHeight) {
        return solve(top, height, bottom, marginTop, marginBottom, containingBlockHeight,
                staticTop, available -> contentHeight);
    }

    private static Solution solve(OptionalDouble start, OptionalDouble size, OptionalDouble end,
                                  OptionalDouble mStart, OptionalDouble mEnd, double cb,
                                  double staticStart, DoubleUnaryOperator autoSize) {
        double ms = mStart.orElse(0);
        double me = mEnd.orElse(0);

        // 1. Todo auto: posición estática y tamaño automático.
        if (start.isEmpty() && size.isEmpty() && end.isEmpty()) {
            double s = staticStart;
            double t = autoSize.applyAsDouble(cb - s - ms - me);
            return new Solution(s, ms, t, me, cb - s - ms - t - me);
        }

        // 2. Nada auto: los márgenes auto absorben el sobrante; si no hay, se ignora el fin.
        if (start.isPresent() && size.isPresent() && end.isPresent()) {
            double s = start.getAsDouble();
            double t = size.getAsDouble();
            double e = end.getAsDouble();
            if (mStart.isEmpty() && mEnd.isEmpty()) {
                double remaining = cb - s - t - e;
                if (remaining >= 0) {
                    return new Solution(s, remaining / 2, t, remaining / 2, e);
                }
                return new Solution(s, 0, t, remaining, e);
            }
            if (mStart.isEmpty()) {
                return new Solution(s, cb - s - t - e - me, t, me, e);
            }
            if (mEnd.isEmpty()) {
                return new Solution(s, ms, t, cb - s - ms - t - e, e);
            }
            return new Solution(s, ms, t, me, cb - s - ms - t - me); // sobre-restringida
        }

        // 3. Algún auto (los márgenes auto valen 0).
        if (start.isEmpty() && size.isEmpty()) {
            double e = end.getAsDouble();
            double t = autoSize.applyAsDouble(cb - e - ms - me);
            return new Solution(cb - e - ms - me - t, ms, t, me, e);
        }
        if (start.isEmpty() && end.isEmpty()) {
            double s = staticStart;
            double t = size.getAsDouble();
            return new Solution(s, ms, t, me, cb - s - ms - t - me);
        }
        if (size.isEmpty() && end.isEmpty()) {
            double s = start.getAsDouble();
            double t = autoSize.applyAsDouble(cb - s - ms - me);
            return new Solution(s, ms, t, me, cb - s - ms - t - me);
        }
        if (start.isEmpty()) {
            double t = size.getAsDouble();
            double e = end.getAsDouble();
            return new Solution(cb - t - e - ms - me, ms, t, me, e);
        }
        if (size.isEmpty()) {
            double s = start.getAsDouble();
            double e = end.getAsDouble();
            double t = Math.max(0, cb - s - e - ms - me);
            return new Solution(s, ms, t, me, cb - s - ms - t - me);
        }
        double s = start.getAsDouble();
        double t = size.getAsDouble();
        return new Solution(s, ms, t, me, cb - s - ms - t - me);
    }
}
