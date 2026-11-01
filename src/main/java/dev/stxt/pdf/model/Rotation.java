package dev.stxt.pdf.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * The rotation of a box when it is painted (reference/common/layers-and-rotation.md): {@code Rotate}, in degrees
 * clockwise, around {@code Rotate origin}, a point of the border box measured from its top left corner, in points or
 * as a percentage of its width and its height ({@code center} is 50% 50%).
 *
 * <p>The coordinates are those of the page, with y pointing down: a positive angle of
 * {@link AffineTransform#getRotateInstance(double, double, double)} rotates, as seen, clockwise.</p>
 *
 * @param degrees angle, from −360 to 360 and other than 0
 * @param originX distance from the left edge of the border box to the center of the rotation: points or percentage
 *                of its width
 * @param originY distance from the top edge: points or percentage of its height
 */
public record Rotation(double degrees, Value originX, Value originY) {

    /** The default origin, on both axes: the center of the border box. */
    public static final Value CENTER = new Value.Percentage(50);

    private static final Set<String> KEYWORDS = Set.of("top", "bottom", "left", "right", "center");

    /** A rotation with the origin in fractions of the width and height of the border box ({@code 0.5}: center). */
    public Rotation(double degrees, double fractionX, double fractionY) {
        this(degrees, new Value.Percentage(fractionX * 100), new Value.Percentage(fractionY * 100));
    }

    /**
     * Parses {@code Rotate origin}: one or two keywords out of {@code top}, {@code bottom},
     * {@code left}, {@code right} and {@code center}, in any order ({@code top left} and
     * {@code left top} are the same; the axis that is not named is {@code center}), or two measurements, the horizontal
     * and the vertical one, in points or as a percentage ({@code 20 50%}).
     *
     * @return the horizontal and the vertical origin
     * @throws IllegalArgumentException if they are neither keywords nor two measurements, if both forms are mixed or if
     *                                  two words point to the same axis
     */
    public static Value[] origin(String text) {
        String[] words = text.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length > 2 || words[0].isEmpty()) {
            throw new IllegalArgumentException("Rotate origin expects one or two keywords (top, bottom, left, "
                    + "right, center) or two measurements: " + text.trim());
        }
        long keywords = Arrays.stream(words).filter(KEYWORDS::contains).count();
        if (keywords == 0) {
            return measures(words, text);
        }
        if (keywords < words.length) {
            throw new IllegalArgumentException("Rotate origin is keywords or two measurements, not a mix of both: "
                    + text.trim());
        }
        Double x = null;
        Double y = null;
        for (String w : words) {
            switch (w) {
                case "left", "right" -> {
                    if (x != null) {
                        throw new IllegalArgumentException("Rotate origin has two horizontal positions: " + text.trim());
                    }
                    x = w.equals("left") ? 0.0 : 100.0;
                }
                case "top", "bottom" -> {
                    if (y != null) {
                        throw new IllegalArgumentException("Rotate origin has two vertical positions: " + text.trim());
                    }
                    y = w.equals("top") ? 0.0 : 100.0;
                }
                case "center" -> {
                    // The axis that is not named stays at the center.
                }
                default -> throw new IllegalStateException(w);
            }
        }
        return new Value[] {x != null ? new Value.Percentage(x) : CENTER, y != null ? new Value.Percentage(y) : CENTER};
    }

    /** Two measurements, in points or as a percentage. */
    private static Value[] measures(String[] words, String text) {
        if (words.length != 2) {
            throw new IllegalArgumentException("Rotate origin needs two measurements, horizontal and vertical: "
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
                throw new IllegalArgumentException("Rotate origin does not allow '" + words[i] + "': it allows top, bottom, "
                        + "left, right or center, or two lengths or percentages");
            }
            origin[i] = v;
        }
        return origin;
    }

    /** The rotation of the border box {@code r}, around its origin. */
    public AffineTransform transform(Rect r) {
        return AffineTransform.getRotateInstance(Math.toRadians(degrees), pivotX(r), pivotY(r));
    }

    /** The x of the center of the rotation of the border box {@code r}. */
    public double pivotX(Rect r) {
        return r.x() + originX.resolve(r.width()).orElseThrow();
    }

    /** The y of the center of the rotation of the border box {@code r}. */
    public double pivotY(Rect r) {
        return r.y() + originY.resolve(r.height()).orElseThrow();
    }

    /**
     * The four corners of the rectangle {@code r} after applying {@code t} to it, in the order
     * top left, top right, bottom right, bottom left: x0, y0, x1, y1...
     */
    public static double[] corners(AffineTransform t, Rect r) {
        double[] points = {r.x(), r.y(), r.right(), r.y(), r.right(), r.bottom(), r.x(), r.bottom()};
        t.transform(points, 0, points, 0, 4);
        return points;
    }

    /** The rectangle that encloses {@code r} after applying {@code t} to it. */
    public static Rect envelope(AffineTransform t, Rect r) {
        double[] p = corners(t, r);
        double minX = Math.min(Math.min(p[0], p[2]), Math.min(p[4], p[6]));
        double maxX = Math.max(Math.max(p[0], p[2]), Math.max(p[4], p[6]));
        double minY = Math.min(Math.min(p[1], p[3]), Math.min(p[5], p[7]));
        double maxY = Math.max(Math.max(p[1], p[3]), Math.max(p[5], p[7]));
        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    /** The point ({@code x}, {@code y}) after applying {@code t} to it. */
    public static Point2D point(AffineTransform t, double x, double y) {
        return t.transform(new Point2D.Double(x, y), null);
    }
}
