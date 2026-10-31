package dev.stxt.pdf.model;

import java.util.List;

/**
 * The fill of a background (reference/common/box.md): a color or a linear gradient. The colors
 * are still the text of the document ({@link Colors}).
 */
public sealed interface Paint {

    /** A solid color, with its opacity if it is written. */
    record Solid(String color) implements Paint {

        @Override
        public boolean isTranslucent() {
            return Colors.isTranslucent(color);
        }
    }

    /**
     * {@code linear(angle, color, color...)}: the angle in degrees as in CSS (0 goes up,
     * 90 to the right, 180 down), with the colors spread evenly.
     */
    record Linear(double angle, List<String> stops) implements Paint {

        public Linear {
            stops = List.copyOf(stops);
        }

        /**
         * The ends of the gradient line in {@code box} (absolute coordinates): it goes through the
         * center and is as long as needed for the corners to have the first and the last color,
         * as in CSS. Returns {@code {x1, y1, x2, y2}}.
         */
        public double[] line(Rect box) {
            double rad = Math.toRadians(angle);
            double dx = Math.sin(rad);
            double dy = -Math.cos(rad);
            double half = (Math.abs(box.width() * dx) + Math.abs(box.height() * dy)) / 2;
            double cx = box.x() + box.width() / 2;
            double cy = box.y() + box.height() / 2;
            return new double[]{cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half};
        }

        @Override
        public boolean isTranslucent() {
            return stops.stream().anyMatch(Colors::isTranslucent);
        }
    }

    /** Whether some color of the fill lets what is underneath show through. */
    boolean isTranslucent();
}
