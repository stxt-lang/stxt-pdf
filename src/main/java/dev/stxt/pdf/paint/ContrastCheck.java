package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.PropertyKey;
import dev.stxt.pdf.tree.SourceLine;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * The check of the contrast of text with its background (reference/common/pdf.md): a canvas that does not draw, but
 * records what is painted on each page, in the same order as the others ({@link StackingContext}), and for each text
 * run looks at what lies under its first letter. The contrast is that of the WCAG guidelines (level AA): at least
 * {@link #NORMAL}, or {@link #LARGE} for large text.
 *
 * <p>What lies underneath is the backgrounds of the boxes and the plain rectangles painted before the text, blended
 * with their opacity over the white of the page; for a gradient each color counts, and the worst one applies. An image
 * under the text, if nothing opaque covers it, is an unknown background: that text is not checked. Borders do not
 * count.</p>
 */
public final class ContrastCheck implements Canvas {

    /** The minimum contrast of normal text. */
    public static final double NORMAL = 4.5;
    /** The minimum contrast of large text: {@link #LARGE_SIZE} or more, or {@link #LARGE_BOLD_SIZE} in bold. */
    public static final double LARGE = 3;
    public static final double LARGE_SIZE = 18;
    public static final double LARGE_BOLD_SIZE = 14;

    private static final float[] WHITE = {1, 1, 1, 1};

    /** What is painted on the page, in order: the area in page coordinates and its colors ({@code null}, an image). */
    private record Layer(Shape area, List<float[]> colors) {
    }

    private final List<Layer> layers = new ArrayList<>();
    private final Deque<AffineTransform> turns = new ArrayDeque<>();
    private AffineTransform turn = new AffineTransform();
    /** The box whose text is being painted ({@link Tagging#beginText}); {@code null} outside a text. */
    private Box current;

    private ContrastCheck() {
    }

    /**
     * Checks the contrast of all the text of a laid-out document.
     *
     * @throws InvalidModelException {@link ErrorCode#CONTRAST_LOW} on the first text with too little contrast
     */
    public static void check(LaidOutDocument document) {
        for (Page page : document.pages()) {
            Rect area = page.pageSetup().pageArea();
            StackingContext.build(page.content(), page.pageOverlays(), area.x(), area.y()).paint(new ContrastCheck());
        }
    }

    @Override
    public void background(Rect r, String color) {
        add(r, List.of(Colors.rgba(color)));
    }

    @Override
    public void fill(Rect paddingBox, Radii radii, Paint paint) {
        List<String> colors = paint instanceof Paint.Linear linear ? linear.stops()
                : List.of(((Paint.Solid) paint).color());
        add(paddingBox, colors.stream().map(Colors::rgba).toList());
    }

    @Override
    public void image(Box box, Rect content, Radii radii) {
        add(content, null);
    }

    private void add(Rect r, List<float[]> colors) {
        Shape area = turn.createTransformedShape(new Rectangle2D.Double(r.x(), r.y(), r.width(), r.height()));
        layers.add(new Layer(area, colors));
    }

    @Override
    public void border(Rect r, Insets w, Radii radii, Sides<String> colors) {
    }

    @Override
    public void outline(Rect borderBox) {
    }

    @Override
    public void beginRotation(double degrees, double x, double y) {
        turns.push(turn);
        turn = new AffineTransform(turn);
        turn.rotate(Math.toRadians(degrees), x, y);
    }

    @Override
    public void endRotation() {
        turn = turns.pop();
    }

    /** A clip only removes what lies outside it: what lies under the text, which is inside, does not change. */
    @Override
    public void beginClip(Rect r, Radii radii) {
    }

    @Override
    public void endClip() {
    }

    @Override
    public VerticalMetrics vertical(FontFace font) {
        return VerticalMetrics.APPROXIMATE;
    }

    @Override
    public DecorationMetrics decoration(FontFace font) {
        return DecorationMetrics.APPROXIMATE;
    }

    @Override
    public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                     double wordSpacing, double letterSpacing) {
        if (current == null || text.isBlank()) {
            return;
        }
        // The middle of the first letter: above the baseline, where the underline does not reach.
        Point2D at = turn.transform(new Point2D.Double(x + fontSize / 4, baselineY - fontSize / 3), null);
        List<float[]> backdrops = backdrops(at);
        if (backdrops == null) {
            return;
        }
        boolean large = Tolerance.fits(LARGE_SIZE, fontSize)
                || font.weight().bold() && Tolerance.fits(LARGE_BOLD_SIZE, fontSize);
        double needed = large ? LARGE : NORMAL;
        float[] ink = Colors.rgba(color);
        for (float[] backdrop : backdrops) {
            float[] seen = over(ink, backdrop);
            double ratio = ratio(seen, backdrop);
            if (ratio < needed) {
                throw error(current, ratio, needed, seen, backdrop);
            }
        }
    }

    /**
     * The colors that may lie under the point, from the white of the page upwards; {@code null} if there is an image
     * that nothing opaque covers.
     */
    private List<float[]> backdrops(Point2D at) {
        List<float[]> below = List.of(WHITE);
        for (Layer layer : layers) {
            if (!layer.area().contains(at)) {
                continue;
            }
            if (layer.colors() == null) {
                below = null;
                continue;
            }
            List<float[]> next = new ArrayList<>();
            for (float[] color : layer.colors()) {
                if (color[3] >= 1) {
                    next.add(color);
                } else if (below != null) {
                    for (float[] b : below) {
                        next.add(over(color, b));
                    }
                }
            }
            // A translucent color over an image leaves the background unknown.
            below = next.size() < layer.colors().size() && below == null ? null : next;
        }
        return below;
    }

    /** {@code top} over the opaque color {@code bottom}: the color that is seen. */
    static float[] over(float[] top, float[] bottom) {
        float a = top[3];
        return new float[]{top[0] * a + bottom[0] * (1 - a), top[1] * a + bottom[1] * (1 - a),
                top[2] * a + bottom[2] * (1 - a), 1};
    }

    /** The contrast between two opaque colors, from 1 to 21. */
    static double ratio(float[] a, float[] b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** The relative luminance of an opaque color. */
    private static double luminance(float[] c) {
        return 0.2126 * linear(c[0]) + 0.7152 * linear(c[1]) + 0.0722 * linear(c[2]);
    }

    private static double linear(float v) {
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static InvalidModelException error(Box box, double ratio, double needed, float[] ink, float[] backdrop) {
        SourceLine line = box.style().isDeclared(PropertyKey.COLOR) ? box.style().line(PropertyKey.COLOR)
                : SourceLine.of(box.line());
        return new InvalidModelException(line, ErrorCode.CONTRAST_LOW, "The text of box " + box.describe()
                + " has a contrast of " + decimal(ratio) + ":1 with its background (" + hex(ink) + " on "
                + hex(backdrop) + ") and needs " + decimal(needed) + ":1 (3:1 for large text: Size of 18 or more, "
                + "or 14 in bold): its Color or the background has to be changed");
    }

    /** {@code 2.85}: rounded down, so that it never seems to reach the threshold. */
    private static String decimal(double v) {
        String s = String.format(Locale.ROOT, "%.2f", Math.floor(v * 100) / 100);
        if (s.endsWith(".00")) {
            return s.substring(0, s.length() - 3);
        }
        return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
    }

    private static String hex(float[] c) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", Math.round(c[0] * 255), Math.round(c[1] * 255),
                Math.round(c[2] * 255));
    }

    @Override
    public Tagging tagging() {
        return tagging;
    }

    /** The only thing of interest from tagging: which box the painted text belongs to. */
    private final Tagging tagging = new Tagging() {
        @Override
        public void beginText(Box box) {
            current = box;
        }

        @Override
        public void beginLinkText(Box box) {
            current = box;
        }

        @Override
        public void endText() {
            current = null;
        }

        @Override
        public void beginActualText(String text) {
        }

        @Override
        public void endActualText() {
        }

        @Override
        public void link(Box box, Rect area, String destination, String description) {
        }
    };
}
