package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.ResourceException;
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
 * SVG output for debugging (milestone 2): one page per file, in pt. If {@code debug} is
 * on, it also draws the outline of each border box.
 *
 * <p>It does not embed the fonts: the viewer uses its own, and the baseline is the approximate one
 * ({@link dev.stxt.pdf.model.VerticalMetrics#APPROXIMATE}). Images (reference/components/image.md) are
 * embedded ({@code data:}) if it is given the resources folder; otherwise, only their box is visible.</p>
 */
public final class SvgCanvas implements Canvas {

    private final StringBuilder svg = new StringBuilder();
    private final boolean debug;
    /** Number of the next gradient or clip, to name their {@code id}. */
    private int ids;
    /** Folder of the images; {@code null} = they are not painted. */
    private final Resources resources;

    public SvgCanvas(boolean debug) {
        this(debug, null);
    }

    public SvgCanvas(boolean debug, Resources resources) {
        this.debug = debug;
        this.resources = resources;
    }

    /** Complete SVG of a page, of the size of the paper, without images. */
    public static String page(Page page, PageSetup pageSetup, boolean debug) {
        return page(page, pageSetup, debug, null);
    }

    /** Complete SVG of a page, of the size of the paper, with the images of {@code resources}. */
    public static String page(Page page, PageSetup pageSetup, boolean debug, Resources resources) {
        double height = pageSetup.height();
        SvgCanvas c = new SvgCanvas(debug, resources);
        Rect area = pageSetup.pageArea();
        StackingContext.build(page.content(), page.footnotes(), page.pageOverlays(), area.x(), area.y()).paint(c);
        return String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2fpt\" height=\"%.2fpt\" viewBox=\"0 0 %.2f %.2f\">\n"
                        + "<rect width=\"100%%\" height=\"100%%\" fill=\"#ffffff\"/>\n%s</svg>\n",
                pageSetup.width(), height, pageSetup.width(), height, c.svg);
    }

    /** A {@code <g>} group with the rotation, closed in {@link #endRotation()}. */
    @Override
    public void beginRotation(double degrees, double x, double y) {
        svg.append(String.format(Locale.ROOT, "<g transform=\"rotate(%s %.2f %.2f)\">\n", number(degrees), x, y));
    }

    @Override
    public void endRotation() {
        svg.append("</g>\n");
    }

    /** A {@code <g>} group with a {@code clipPath}, closed in {@link #endClip()}. */
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

    /** A number without extra zeros: {@code -8}, {@code 12.5}. */
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
     * <p>A {@code <rect>} if it is plain and square; otherwise, a path. The gradient is a
     * {@code linearGradient} in page coordinates, with the opacity of each color.</p>
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

    /** Defines the gradient and returns its {@code id}. */
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

    /** {@code fill="..."} of a color, with {@code fill-opacity} if it is not opaque. */
    private static String fillAttr(String color) {
        if (!Colors.isTranslucent(color) && color.length() <= 7) {
            return " fill=\"" + color + "\"";
        }
        float[] c = Colors.rgba(color);
        return String.format(Locale.ROOT, " fill=\"#%02x%02x%02x\" fill-opacity=\"%.3f\"",
                Math.round(c[0] * 255), Math.round(c[1] * 255), Math.round(c[2] * 255), c[3]);
    }

    /** The path ({@code d}) of the rectangle with its corners. */
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
     * <p>A ring (even-odd fill path) in the color of the sides; with different colors,
     * one per side, clipped to the trapezoid of that side ({@link Shapes#sidePolygon}).</p>
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

    /** The debugging SVG only draws: it neither marks the text nor creates links. */
    @Override
    public Tagging tagging() {
        return Tagging.NONE;
    }

    /** Without the font: the approximation 0.8/0.2 of the size. */
    @Override
    public VerticalMetrics vertical(FontFace font) {
        return VerticalMetrics.APPROXIMATE;
    }

    /** Without the font: the approximation of the lines. */
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
        // An SVG collapses spaces: preformatted text needs to keep them.
        if (text.contains("  ") || text.startsWith(" ") || text.endsWith(" ")) {
            spacing += " xml:space=\"preserve\"";
        }
        FontWeight w = font.weight();
        String weight = w.equals(FontWeight.NORMAL) ? ""
                : " font-weight=\"" + (w.equals(FontWeight.BOLD) ? "bold" : w.value()) + "\"";
        String style = font.style() == FontStyle.ITALIC ? " font-style=\"italic\"" : "";
        svg.append(String.format(Locale.ROOT,
                "<text x=\"%.2f\" y=\"%.2f\" font-family=\"%s\"%s%s font-size=\"%.2f\"%s%s%s>%s</text>\n",
                x, baselineY, escape(font.family()), weight, style, fontSize, fillAttr(color), spacing,
                kerning(text, font, fontSize), escape(text)));
    }

    /**
     * With {@code Kerning}, the adjustments of the pairs of the font (reference/common/typography.md) as {@code dx},
     * one shift per character, and without the kerning of the viewer, so that it is not added twice. Without the
     * resources folder, or without the font in it, nothing: the viewer paints with its own fonts.
     */
    private String kerning(String text, FontFace font, double fontSize) {
        if (!font.kerning() || resources == null) {
            return "";
        }
        int[] kerning;
        try {
            kerning = resources.fonts().find(font).kerning(text, font.ligatures());
        } catch (ResourceException e) {
            // A debugging SVG does not need the fonts: without the file there is no kerning to add.
            return "";
        }
        if (kerning == null) {
            return "";
        }
        StringBuilder dx = new StringBuilder();
        int last = 0;
        for (int i = 0; i < text.length(); i += Character.charCount(text.codePointAt(i))) {
            if (dx.length() > 0) {
                dx.append(' ');
            }
            dx.append(kerning[i] == 0 ? "0" : String.format(Locale.ROOT, "%.2f", kerning[i] / 1000.0 * fontSize));
            if (kerning[i] != 0) {
                last = dx.length();
            }
        }
        return " dx=\"" + dx.substring(0, last) + "\" style=\"font-kerning: none\"";
    }

    /**
     * {@inheritDoc}
     *
     * <p>A nested {@code <svg>} of the size of the content box clips the image, which goes
     * inside without keeping its proportion: {@link ImageFit} already gives the placement. A browser does not draw a
     * PDF inside an SVG: the page goes as a PNG, at 144 pixels per inch.</p>
     */
    @Override
    public void image(Box box, Rect content, Radii radii) {
        if (box.modules() != null) {
            modules(box, content, radii);
            return;
        }
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

    /**
     * The modules of a generated image, such as a {@code Barcode} (reference/components/barcode.md): a path with one
     * rectangle per run of dark modules of each row, in the {@code Color} of its {@code Typography}, clipped to the
     * content box with its corners.
     */
    private void modules(Box box, Rect content, Radii radii) {
        Modules m = box.modules();
        Rect target = ImageFit.place(m.fit(), content, m.naturalWidth(), m.naturalHeight());
        double w = target.width() / m.columns();
        double h = target.height() / m.rows();
        String id = "c" + ids++;
        svg.append("<clipPath id=\"").append(id).append("\"><path d=\"").append(path(content, radii))
                .append("\"/></clipPath>\n<path clip-path=\"url(#").append(id).append(")\" d=\"");
        for (int row = 0; row < m.rows(); row++) {
            for (int[] run : m.runs(row)) {
                svg.append(String.format(Locale.ROOT, "M%.2f %.2fh%.2fv%.2fh%.2fz", target.x() + run[0] * w,
                        target.y() + row * h, run[1] * w, h, -run[1] * w));
            }
        }
        svg.append("\"").append(fillAttr(box.style().typography().color())).append("/>\n");
    }

    /** Page {@code number} (from 1) of a PDF, drawn into a PNG. */
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
