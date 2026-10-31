package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.PageSetup;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;

/**
 * Salida SVG para depurar (hito 2): una página por fichero, en pt. Si {@code debug} está
 * activo, dibuja además el contorno de cada caja de borde.
 *
 * <p>No incrusta las fuentes: el visor usa las suyas, y la línea base es la aproximada
 * ({@link dev.stxt.pdf.model.VerticalMetrics#APPROXIMATE}). Las imágenes (reference/components/image.md) van
 * incrustadas ({@code data:}) si se le da la carpeta de recursos; si no, solo se ve su caja.</p>
 */
public final class SvgCanvas implements Canvas {

    private final StringBuilder svg = new StringBuilder();
    private final boolean debug;
    /** Número del próximo degradado o recorte, para dar nombre a sus {@code id}. */
    private int ids;
    /** Carpeta de las imágenes; {@code null} = no se pintan. */
    private final Resources resources;

    public SvgCanvas(boolean debug) {
        this(debug, null);
    }

    public SvgCanvas(boolean debug, Resources resources) {
        this.debug = debug;
        this.resources = resources;
    }

    /** SVG completo de una página, del tamaño del medio, sin imágenes. */
    public static String page(Page page, PageSetup pageSetup, boolean debug) {
        return page(page, pageSetup, debug, null);
    }

    /** SVG completo de una página, del tamaño del medio, con las imágenes de {@code resources}. */
    public static String page(Page page, PageSetup pageSetup, boolean debug, Resources resources) {
        double height = pageSetup.height();
        SvgCanvas c = new SvgCanvas(debug, resources);
        Rect area = pageSetup.pageArea();
        StackingContext.build(page.content(), page.fixed(), area.x(), area.y()).paint(c);
        return String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2fpt\" height=\"%.2fpt\" viewBox=\"0 0 %.2f %.2f\">\n"
                        + "<rect width=\"100%%\" height=\"100%%\" fill=\"#ffffff\"/>\n%s</svg>\n",
                pageSetup.width(), height, pageSetup.width(), height, c.svg);
    }

    /** Un grupo {@code <g>} con el giro, que se cierra en {@link #endRotation()}. */
    @Override
    public void beginRotation(double degrees, double x, double y) {
        svg.append(String.format(Locale.ROOT, "<g transform=\"rotate(%s %.2f %.2f)\">\n", number(degrees), x, y));
    }

    @Override
    public void endRotation() {
        svg.append("</g>\n");
    }

    /** Un grupo {@code <g>} con un {@code clipPath}, que se cierra en {@link #endClip()}. */
    @Override
    public void beginClip(Rect r, Radii radii) {
        String id = "c" + ids++;
        svg.append("<clipPath id=\"").append(id).append("\"><path d=\"").append(path(r, radii))
                .append("\"/></clipPath>\n<g clip-path=\"url(#").append(id).append(")\">\n");
    }

    @Override
    public void endClip() {
        svg.append("</g>\n");
    }

    /** Un número sin ceros de más: {@code -8}, {@code 12.5}. */
    private static String number(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    @Override
    public void background(Rect r, String color) {
        svg.append(String.format(Locale.ROOT, "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\"%s/>\n",
                r.x(), r.y(), r.width(), r.height(), fillAttr(color)));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un {@code <rect>} si es liso y cuadrado; si no, un camino. El degradado es un
     * {@code linearGradient} en coordenadas de la página, con la opacidad de cada color.</p>
     */
    @Override
    public void fill(Rect r, Radii radii, Paint paint) {
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        if (paint instanceof Paint.Solid solid && radii.isSquare()) {
            background(r, solid.color());
            return;
        }
        String attributes;
        if (paint instanceof Paint.Solid solid) {
            attributes = fillAttr(solid.color());
        } else {
            attributes = " fill=\"url(#" + gradient((Paint.Linear) paint, r) + ")\"";
        }
        svg.append("<path d=\"").append(path(r, radii)).append('"').append(attributes).append("/>\n");
    }

    /** Define el degradado y devuelve su {@code id}. */
    private String gradient(Paint.Linear linear, Rect box) {
        String id = "g" + ids++;
        double[] line = linear.line(box);
        svg.append(String.format(Locale.ROOT,
                "<defs><linearGradient id=\"%s\" gradientUnits=\"userSpaceOnUse\" x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\">",
                id, line[0], line[1], line[2], line[3]));
        int n = linear.stops().size();
        for (int i = 0; i < n; i++) {
            float[] c = Colors.rgba(linear.stops().get(i));
            svg.append(String.format(Locale.ROOT, "<stop offset=\"%.4f\" stop-color=\"#%02x%02x%02x\" stop-opacity=\"%.3f\"/>",
                    (double) i / (n - 1), Math.round(c[0] * 255), Math.round(c[1] * 255), Math.round(c[2] * 255), c[3]));
        }
        svg.append("</linearGradient></defs>\n");
        return id;
    }

    /** {@code fill="…"} de un color, con {@code fill-opacity} si no es opaco. */
    private static String fillAttr(String color) {
        if (!Colors.isTranslucent(color) && color.length() <= 7) {
            return " fill=\"" + color + "\"";
        }
        float[] c = Colors.rgba(color);
        return String.format(Locale.ROOT, " fill=\"#%02x%02x%02x\" fill-opacity=\"%.3f\"",
                Math.round(c[0] * 255), Math.round(c[1] * 255), Math.round(c[2] * 255), c[3]);
    }

    /** El camino ({@code d}) del rectángulo con sus esquinas. */
    private static String path(Rect r, Radii radii) {
        StringBuilder d = new StringBuilder();
        Shapes.roundedRect(pathWriter(d), r, radii);
        return d.toString().strip();
    }

    private static Shapes.Path pathWriter(StringBuilder d) {
        return new Shapes.Path() {
            @Override
            public void moveTo(double x, double y) {
                d.append(String.format(Locale.ROOT, "M%.2f %.2f ", x, y));
            }

            @Override
            public void lineTo(double x, double y) {
                d.append(String.format(Locale.ROOT, "L%.2f %.2f ", x, y));
            }

            @Override
            public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
                d.append(String.format(Locale.ROOT, "C%.2f %.2f %.2f %.2f %.2f %.2f ", x1, y1, x2, y2, x3, y3));
            }

            @Override
            public void close() {
                d.append("Z ");
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un anillo (camino de relleno par-impar) del color de los lados; con colores distintos,
     * uno por lado, recortado al trapecio de ese lado ({@link Shapes#sidePolygon}).</p>
     */
    @Override
    public void border(Rect r, Insets w, Radii radii, Sides<String> colors) {
        if (w.top() > 0 || w.right() > 0 || w.bottom() > 0 || w.left() > 0) {
            StringBuilder ring = new StringBuilder();
            Shapes.roundedRect(pathWriter(ring), r, radii);
            Rect inner = r.shrink(w);
            if (inner.width() > 0 && inner.height() > 0) {
                Shapes.roundedRect(pathWriter(ring), inner, radii.inset(w));
            }
            String d = ring.toString().strip();
            Shapes.Side[] sides = Shapes.Side.values();
            double[] widths = {w.top(), w.right(), w.bottom(), w.left()};
            String[] sideColors = {colors.top(), colors.right(), colors.bottom(), colors.left()};
            boolean uniform = true;
            String first = null;
            for (int i = 0; i < 4; i++) {
                if (widths[i] > 0) {
                    first = first == null ? sideColors[i] : first;
                    uniform &= first.equals(sideColors[i]);
                }
            }
            if (uniform) {
                svg.append("<path d=\"").append(d).append("\" fill-rule=\"evenodd\"").append(fillAttr(first)).append("/>\n");
            } else {
                for (int i = 0; i < 4; i++) {
                    if (widths[i] > 0) {
                        String id = "c" + ids++;
                        StringBuilder polygon = new StringBuilder();
                        Shapes.sidePolygon(pathWriter(polygon), sides[i], r, w, radii.inset(w));
                        svg.append("<clipPath id=\"").append(id).append("\"><path d=\"").append(polygon.toString().strip())
                                .append("\"/></clipPath>\n<path d=\"").append(d)
                                .append("\" fill-rule=\"evenodd\" clip-path=\"url(#").append(id).append(")\"")
                                .append(fillAttr(sideColors[i])).append("/>\n");
                    }
                }
            }
        }
    }

    /** El SVG de depuración solo dibuja: no marca el texto ni crea enlaces. */
    @Override
    public Tagging tagging() {
        return Tagging.NONE;
    }

    /** Sin la fuente: la aproximación 0,8/0,2 del tamaño. */
    @Override
    public VerticalMetrics vertical(FontFace font) {
        return VerticalMetrics.APPROXIMATE;
    }

    /** Sin la fuente: la aproximación de las rayas. */
    @Override
    public DecorationMetrics decoration(FontFace font) {
        return DecorationMetrics.APPROXIMATE;
    }

    @Override
    public boolean outlines() {
        return debug;
    }

    @Override
    public void outline(Rect r) {
        if (debug) {
            svg.append(String.format(Locale.ROOT,
                    "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"none\" stroke=\"#d33\" stroke-width=\"0.3\" stroke-dasharray=\"2 2\"/>\n",
                    r.x(), r.y(), r.width(), r.height()));
        }
    }

    @Override
    public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                     double wordSpacing, double letterSpacing) {
        String spacing = (wordSpacing == 0 ? "" : String.format(Locale.ROOT, " word-spacing=\"%.2f\"", wordSpacing))
                + (letterSpacing == 0 ? "" : String.format(Locale.ROOT, " letter-spacing=\"%.2f\"", letterSpacing));
        // Un SVG colapsa los espacios: el texto preformateado necesita conservarlos.
        if (text.contains("  ") || text.startsWith(" ") || text.endsWith(" ")) {
            spacing += " xml:space=\"preserve\"";
        }
        String weight = font.weight() == FontWeight.BOLD ? " font-weight=\"bold\"" : "";
        String style = font.style() == FontStyle.ITALIC ? " font-style=\"italic\"" : "";
        svg.append(String.format(Locale.ROOT,
                "<text x=\"%.2f\" y=\"%.2f\" font-family=\"%s\"%s%s font-size=\"%.2f\"%s%s>%s</text>\n",
                x, baselineY, escape(font.family()), weight, style, fontSize, fillAttr(color), spacing, escape(text)));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un {@code <svg>} anidado del tamaño de la caja de contenido recorta la imagen, que va
     * dentro sin conservar su proporción: la colocación ya la da {@link ImageFit}. Un navegador no dibuja un PDF dentro
     * de un SVG: la página va como PNG, a 144 píxeles por pulgada.</p>
     */
    @Override
    public void image(Box box, Rect content, Radii radii) {
        if (resources == null) {
            return;
        }
        ImageFile file = resources.images().find(box.image());
        int number = box.style().image().pdfPage();
        Rect target = ImageFit.place(box.style().image().fit(), content, file.width(number), file.height(number));
        String data;
        try {
            byte[] bytes = file.format() == ImageFile.Format.PDF ? pageAsPng(file, number)
                    : Files.readAllBytes(file.path());
            data = Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String mime = switch (file.format()) {
            case PNG, PDF -> "image/png";
            case JPEG -> "image/jpeg";
            case SVG -> "image/svg+xml";
        };
        boolean rounded = !radii.isSquare();
        if (rounded) {
            String id = "c" + ids++;
            svg.append("<clipPath id=\"").append(id).append("\"><path d=\"").append(path(content, radii))
                    .append("\"/></clipPath>\n<g clip-path=\"url(#").append(id).append(")\">");
        }
        svg.append(String.format(Locale.ROOT, "<svg x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\">"
                        + "<image x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" preserveAspectRatio=\"none\" "
                        + "href=\"data:%s;base64,%s\"/></svg>\n",
                content.x(), content.y(), content.width(), content.height(), target.x() - content.x(),
                target.y() - content.y(), target.width(), target.height(), mime, data));
        if (rounded) {
            svg.append("</g>\n");
        }
    }

    /** La página {@code number} (desde 1) de un PDF, dibujada en un PNG. */
    private static byte[] pageAsPng(ImageFile file, int number) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.path().toFile())) {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(number - 1, 144), "png", png);
            return png.toByteArray();
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
