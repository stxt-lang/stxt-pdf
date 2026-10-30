package dev.stxt.pdf.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * El giro de una caja al pintarla (referencia/comun/capas-y-giro.md): {@code Rotate}, en grados en el
 * sentido de las agujas del reloj, alrededor de {@code Rotate origin}, un punto de la caja de
 * borde medido desde su esquina de arriba a la izquierda, en puntos o en porcentaje de su ancho y de su alto
 * ({@code center} es 50 % 50 %).
 *
 * <p>Las coordenadas son las de la página, con la y hacia abajo: un ángulo positivo de
 * {@link AffineTransform#getRotateInstance(double, double, double)} gira, a la vista, en el sentido
 * de las agujas del reloj.</p>
 *
 * @param degrees ángulo, de −360 a 360 y distinto de 0
 * @param originX distancia desde el borde izquierdo de la caja de borde hasta el centro del giro: puntos o porcentaje
 *                de su ancho
 * @param originY distancia desde el borde de arriba: puntos o porcentaje de su alto
 */
public record Rotation(double degrees, Value originX, Value originY) {

    /** El origen por defecto, en los dos ejes: el centro de la caja de borde. */
    public static final Value CENTER = new Value.Percentage(50);

    private static final Set<String> KEYWORDS = Set.of("top", "bottom", "left", "right", "center");

    /** Un giro con el origen en fracciones del ancho y del alto de la caja de borde ({@code 0.5} es el centro). */
    public Rotation(double degrees, double fractionX, double fractionY) {
        this(degrees, new Value.Percentage(fractionX * 100), new Value.Percentage(fractionY * 100));
    }

    /**
     * Interpreta {@code Rotate origin}: una o dos palabras clave de {@code top}, {@code bottom},
     * {@code left}, {@code right} y {@code center}, en cualquier orden ({@code top left} y
     * {@code left top} son lo mismo; el eje que no se nombra es {@code center}), o dos medidas, la horizontal y la
     * vertical, en puntos o en porcentaje ({@code 20 50%}).
     *
     * @return el origen horizontal y el vertical
     * @throws IllegalArgumentException si no son palabras clave ni dos medidas, si mezcla las dos formas o si dos
     *                                  palabras señalan el mismo eje
     */
    public static Value[] origin(String text) {
        String[] words = text.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length > 2 || words[0].isEmpty()) {
            throw new IllegalArgumentException("Rotate origin espera una o dos palabras clave (top, bottom, left, "
                    + "right, center) o dos medidas: " + text.trim());
        }
        long keywords = Arrays.stream(words).filter(KEYWORDS::contains).count();
        if (keywords == 0) {
            return measures(words, text);
        }
        if (keywords < words.length) {
            throw new IllegalArgumentException("Rotate origin son palabras clave o dos medidas, sin mezclarlas: "
                    + text.trim());
        }
        Double x = null;
        Double y = null;
        for (String w : words) {
            switch (w) {
                case "left", "right" -> {
                    if (x != null) {
                        throw new IllegalArgumentException("Rotate origin tiene dos posiciones horizontales: " + text.trim());
                    }
                    x = w.equals("left") ? 0.0 : 100.0;
                }
                case "top", "bottom" -> {
                    if (y != null) {
                        throw new IllegalArgumentException("Rotate origin tiene dos posiciones verticales: " + text.trim());
                    }
                    y = w.equals("top") ? 0.0 : 100.0;
                }
                case "center" -> {
                    // El eje que no se nombre queda en el centro.
                }
                default -> throw new IllegalStateException(w);
            }
        }
        return new Value[] {x != null ? new Value.Percentage(x) : CENTER, y != null ? new Value.Percentage(y) : CENTER};
    }

    /** Dos medidas, en puntos o en porcentaje. */
    private static Value[] measures(String[] words, String text) {
        if (words.length != 2) {
            throw new IllegalArgumentException("Rotate origin con medidas necesita dos, la horizontal y la vertical: "
                    + text.trim());
        }
        Value[] origin = new Value[2];
        for (int i = 0; i < 2; i++) {
            Value v;
            try {
                v = ValueParser.value(words[i]);
            } catch (IllegalArgumentException ex) {
                v = null;
            }
            if (!(v instanceof Value.Points) && !(v instanceof Value.Percentage)) {
                throw new IllegalArgumentException("Rotate origin no admite '" + words[i] + "': usa top, bottom, left, "
                        + "right o center, o dos medidas en puntos o en porcentaje");
            }
            origin[i] = v;
        }
        return origin;
    }

    /** El giro de la caja de borde {@code r}, alrededor de su origen. */
    public AffineTransform transform(Rect r) {
        return AffineTransform.getRotateInstance(Math.toRadians(degrees), pivotX(r), pivotY(r));
    }

    /** La x del centro del giro de la caja de borde {@code r}. */
    public double pivotX(Rect r) {
        return r.x() + originX.resolve(r.width()).orElseThrow();
    }

    /** La y del centro del giro de la caja de borde {@code r}. */
    public double pivotY(Rect r) {
        return r.y() + originY.resolve(r.height()).orElseThrow();
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
