package dev.stxt.boxes.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Locale;

/**
 * El giro de una caja al pintarla (ESPECIFICACION.md §5.6): {@code Rotate}, en grados en el
 * sentido de las agujas del reloj, alrededor de {@code Rotate origin}, un punto de la caja de
 * borde dado como fracción de su ancho y de su alto ({@code center} es 0,5 0,5).
 *
 * <p>Las coordenadas son las de la página, con la y hacia abajo: un ángulo positivo de
 * {@link AffineTransform#getRotateInstance(double, double, double)} gira, a la vista, en el sentido
 * de las agujas del reloj.</p>
 *
 * @param degrees ángulo, de −360 a 360 y distinto de 0
 * @param originX fracción del ancho de la caja de borde donde está el centro del giro
 * @param originY fracción del alto
 */
public record Rotation(double degrees, double originX, double originY) {

    /** La fracción del origen por defecto, en los dos ejes: el centro de la caja de borde. */
    public static final double CENTER = 0.5;

    /**
     * Interpreta {@code Rotate origin}: una o dos palabras clave de {@code top}, {@code bottom},
     * {@code left}, {@code right} y {@code center}, en cualquier orden ({@code top left} y
     * {@code left top} son lo mismo); el eje que no se nombra es {@code center}.
     *
     * @return la fracción del ancho y la del alto
     * @throws IllegalArgumentException si no son palabras clave o dos señalan el mismo eje
     */
    public static double[] origin(String text) {
        String[] words = text.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length > 2 || words[0].isEmpty()) {
            throw new IllegalArgumentException("Rotate origin espera una o dos palabras clave (top, bottom, left, "
                    + "right, center): " + text.trim());
        }
        Double x = null;
        Double y = null;
        for (String w : words) {
            switch (w) {
                case "left", "right" -> {
                    if (x != null) {
                        throw new IllegalArgumentException("Rotate origin tiene dos posiciones horizontales: " + text.trim());
                    }
                    x = w.equals("left") ? 0.0 : 1.0;
                }
                case "top", "bottom" -> {
                    if (y != null) {
                        throw new IllegalArgumentException("Rotate origin tiene dos posiciones verticales: " + text.trim());
                    }
                    y = w.equals("top") ? 0.0 : 1.0;
                }
                case "center" -> {
                    // El eje que no se nombre queda en el centro.
                }
                default -> throw new IllegalArgumentException("Rotate origin no admite '" + w + "': usa top, bottom, "
                        + "left, right o center");
            }
        }
        return new double[] {x != null ? x : 0.5, y != null ? y : 0.5};
    }

    /** El giro de la caja de borde {@code r}, alrededor de su origen. */
    public AffineTransform transform(Rect r) {
        return AffineTransform.getRotateInstance(Math.toRadians(degrees), pivotX(r), pivotY(r));
    }

    /** La x del centro del giro de la caja de borde {@code r}. */
    public double pivotX(Rect r) {
        return r.x() + originX * r.width();
    }

    /** La y del centro del giro de la caja de borde {@code r}. */
    public double pivotY(Rect r) {
        return r.y() + originY * r.height();
    }

    /**
     * Las cuatro esquinas del rectángulo {@code r} después de aplicarle {@code t}, en el orden
     * arriba izquierda, arriba derecha, abajo derecha, abajo izquierda: x0, y0, x1, y1…
     */
    public static double[] corners(AffineTransform t, Rect r) {
        double[] points = {r.x(), r.y(), r.right(), r.y(), r.right(), r.bottom(), r.x(), r.bottom()};
        t.transform(points, 0, points, 0, 4);
        return points;
    }

    /** El rectángulo que envuelve {@code r} después de aplicarle {@code t}. */
    public static Rect envelope(AffineTransform t, Rect r) {
        double[] p = corners(t, r);
        double minX = Math.min(Math.min(p[0], p[2]), Math.min(p[4], p[6]));
        double maxX = Math.max(Math.max(p[0], p[2]), Math.max(p[4], p[6]));
        double minY = Math.min(Math.min(p[1], p[3]), Math.min(p[5], p[7]));
        double maxY = Math.max(Math.max(p[1], p[3]), Math.max(p[5], p[7]));
        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    /** El punto ({@code x}, {@code y}) después de aplicarle {@code t}. */
    public static Point2D point(AffineTransform t, double x, double y) {
        return t.transform(new Point2D.Double(x, y), null);
    }
}
