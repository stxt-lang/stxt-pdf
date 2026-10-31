package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.layout.IntrinsicSizes;

import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

/**
 * Resolves the position and the size of an {@code Overlay} or {@code Page overlay} box on one axis
 * (reference/components/frame-and-overlays.md). It always holds that:
 *
 * <pre>start + start margin + size + end margin + end = size of the reference container</pre>
 *
 * <p>An empty {@link OptionalDouble} represents {@code auto}. The size is that of the border box.</p>
 */
public final class OverlayEquation {

    /** Solution of the equation on one axis. */
    public record Solution(double start, double marginStart, double size, double marginEnd, double end) {
    }

    private OverlayEquation() {
    }

    /**
     * Horizontal axis. An {@code auto} width that cannot be solved for fits the content
     * (<em>shrink-to-fit</em>). If it is over-constrained, {@code right} is ignored.
     */
    public static Solution horizontal(OptionalDouble left, OptionalDouble width, OptionalDouble right,
                                      OptionalDouble marginLeft, OptionalDouble marginRight,
                                      double containingBlockWidth, double staticLeft,
                                      IntrinsicSizes content) {
        return solve(left, width, right, marginLeft, marginRight, containingBlockWidth,
                staticLeft, content::fit);
    }

    /**
     * Vertical axis. An {@code auto} height that cannot be solved for takes the height of the content.
     * If it is over-constrained, {@code bottom} is ignored.
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

        // 1. All auto: static position and automatic size.
        if (start.isEmpty() && size.isEmpty() && end.isEmpty()) {
            double s = staticStart;
            double t = autoSize.applyAsDouble(cb - s - ms - me);
            return new Solution(s, ms, t, me, cb - s - ms - t - me);
        }

        // 2. Nothing auto: the auto margins absorb the leftover; if there are none, the end is ignored.
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
            return new Solution(s, ms, t, me, cb - s - ms - t - me); // over-constrained
        }

        // 3. Some auto (the auto margins are 0).
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
