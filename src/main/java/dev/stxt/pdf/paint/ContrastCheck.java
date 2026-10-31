package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontWeight;
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
 * La comprobación del contraste del texto con su fondo (referencia/comun/pdf.md): un lienzo que no dibuja,
 * sino que apunta lo que se pinta en cada página, en el mismo orden que los demás ({@link StackingContext}), y en cada
 * tramo de texto mira qué hay debajo de su primera letra. El contraste es el de las pautas WCAG (nivel AA): al menos
 * {@link #NORMAL}, o {@link #LARGE} en el texto grande.
 *
 * <p>Lo de debajo son los fondos de las cajas y los rectángulos lisos pintados antes que el texto, mezclados con su
 * opacidad sobre el blanco de la página; de un degradado cuenta cada color, y vale el peor. Una imagen debajo del
 * texto, si nada opaco la tapa, es un fondo desconocido: ese texto no se comprueba. Los bordes no cuentan.</p>
 */
public final class ContrastCheck implements Canvas {

    /** El contraste mínimo del texto normal. */
    public static final double NORMAL = 4.5;
    /** El contraste mínimo del texto grande: {@link #LARGE_SIZE} o más, o {@link #LARGE_BOLD_SIZE} en negrita. */
    public static final double LARGE = 3;
    public static final double LARGE_SIZE = 18;
    public static final double LARGE_BOLD_SIZE = 14;

    private static final float[] WHITE = {1, 1, 1, 1};

    /** Lo pintado en la página, en orden: la zona en coordenadas de la página y sus colores ({@code null}, imagen). */
    private record Layer(Shape area, List<float[]> colors) {
    }

    private final List<Layer> layers = new ArrayList<>();
    private final Deque<AffineTransform> turns = new ArrayDeque<>();
    private AffineTransform turn = new AffineTransform();
    /** La caja cuyo texto se pinta ahora ({@link Tagging#beginText}); {@code null} fuera de un texto. */
    private Box current;

    private ContrastCheck() {
    }

    /**
     * Comprueba el contraste de todo el texto de un documento maquetado.
     *
     * @throws InvalidModelException {@link ErrorCode#CONTRAST_LOW} en el primer texto con poco contraste
     */
    public static void check(LaidOutDocument document) {
        for (Page page : document.pages()) {
            Rect area = page.pageSetup().pageArea();
            StackingContext.build(page.content(), page.fixed(), area.x(), area.y()).paint(new ContrastCheck());
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

    /** Un recorte solo quita lo que queda fuera de él: lo que hay debajo del texto, que está dentro, no cambia. */
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
        // La mitad de la primera letra: por encima de la línea base, donde no llega el subrayado.
        Point2D at = turn.transform(new Point2D.Double(x + fontSize / 4, baselineY - fontSize / 3), null);
        List<float[]> backdrops = backdrops(at);
        if (backdrops == null) {
            return;
        }
        boolean large = Tolerance.fits(LARGE_SIZE, fontSize)
                || font.weight() == FontWeight.BOLD && Tolerance.fits(LARGE_BOLD_SIZE, fontSize);
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
     * Los colores que puede haber debajo del punto, del blanco de la página hacia arriba; {@code null} si hay una
     * imagen que nada opaco tapa.
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
            // Un color translúcido sobre una imagen deja el fondo sin conocer.
            below = next.size() < layer.colors().size() && below == null ? null : next;
        }
        return below;
    }

    /** {@code top} sobre el color opaco {@code bottom}: el color que se ve. */
    static float[] over(float[] top, float[] bottom) {
        float a = top[3];
        return new float[]{top[0] * a + bottom[0] * (1 - a), top[1] * a + bottom[1] * (1 - a),
                top[2] * a + bottom[2] * (1 - a), 1};
    }

    /** El contraste entre dos colores opacos, de 1 a 21. */
    static double ratio(float[] a, float[] b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** La luminancia relativa de un color opaco. */
    private static double luminance(float[] c) {
        return 0.2126 * linear(c[0]) + 0.7152 * linear(c[1]) + 0.0722 * linear(c[2]);
    }

    private static double linear(float v) {
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static InvalidModelException error(Box box, double ratio, double needed, float[] ink, float[] backdrop) {
        SourceLine line = box.style().isDeclared(PropertyKey.COLOR) ? box.style().line(PropertyKey.COLOR)
                : SourceLine.of(box.line());
        return new InvalidModelException(line, ErrorCode.CONTRAST_LOW, "El texto de la caja " + box.describe()
                + " tiene un contraste de " + decimal(ratio) + ":1 con su fondo (" + hex(ink) + " sobre "
                + hex(backdrop) + ") y necesita " + decimal(needed) + ":1 (3:1 en el texto grande: Size de 18 o más, "
                + "o de 14 en negrita): hay que cambiar su Color o el fondo");
    }

    /** {@code 2,85}: con coma y hacia abajo, para que nunca parezca que llega. */
    private static String decimal(double v) {
        String s = String.format(Locale.ROOT, "%.2f", Math.floor(v * 100) / 100).replace('.', ',');
        if (s.endsWith(",00")) {
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

    /** Lo único que interesa del etiquetado: de qué caja es el texto que se pinta. */
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
