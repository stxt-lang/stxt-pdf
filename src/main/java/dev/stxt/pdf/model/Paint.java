package dev.stxt.pdf.model;

import java.util.List;

/**
 * El relleno de un fondo (referencia/comun/caja.md): un color o un degradado lineal. Los colores
 * siguen siendo el texto del documento ({@link Colors}).
 */
public sealed interface Paint {

    /** Un color liso, con su opacidad si la escribe. */
    record Solid(String color) implements Paint {

        @Override
        public boolean isTranslucent() {
            return Colors.isTranslucent(color);
        }
    }

    /**
     * {@code linear(ángulo, color, color…)}: el ángulo en grados como en CSS (0 va hacia arriba,
     * 90 hacia la derecha, 180 hacia abajo), con los colores repartidos a partes iguales.
     */
    record Linear(double angle, List<String> stops) implements Paint {

        public Linear {
            stops = List.copyOf(stops);
        }

        /**
         * Los extremos de la línea del degradado en {@code box} (coordenadas absolutas): pasa por el
         * centro y mide lo que hace falta para que las esquinas tengan el color del primer y del
         * último color, como en CSS. Devuelve {@code {x1, y1, x2, y2}}.
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

    /** Si algún color del relleno deja ver lo que hay debajo. */
    boolean isTranslucent();
}
