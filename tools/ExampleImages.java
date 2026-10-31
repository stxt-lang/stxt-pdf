import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

/**
 * Generates the images of the examples of {@code test/examples/} in {@code resources/images/examples/}:
 * logos, synthetic photos, charts, product illustrations and signatures. Everything is drawn with
 * Java2D (no third-party files, so there are no rights to respect) and with the IBM Plex fonts
 * of {@code resources/fonts/}, and it is deterministic: the same Java version gives the same
 * result.
 *
 * <p>The images with text (the charts of the report, the figures of the manual and the seal of the certificate) are
 * drawn once per language of the examples, in a {@code es/} or {@code en/} subfolder. The logos keep their text in
 * every language: they are the brand of the companies.</p>
 *
 * <pre>
 * java tools/ExampleImages.java            # from the root of the repository
 * </pre>
 */
public class ExampleImages {

    static final Path FONTS = Path.of("resources", "fonts");
    static final Path OUT = Path.of("resources", "images", "examples");

    // ---- Common palette ---------------------------------------------------------------------
    static final Color WALNUT = new Color(0x3B2A1E);
    static final Color AMBER = new Color(0xB5651D);
    static final Color SAND = new Color(0xEFE4D2);
    static final Color INK = new Color(0x1F2A44);
    static final Color TEAL = new Color(0x1F7A7A);
    static final Color GREEN = new Color(0x4C8C4A);
    static final Color GREY = new Color(0x6B6B6B);

    /** The languages of the examples: Spanish is the original, English the mirror. */
    static final String[] LANGUAGES = {"es", "en"};

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        System.setProperty("java.awt.headless", "true");
        brand();
        letter();
        resume();
        report();
        newsletter();
        menu();
        contract();
        catalog();
        certificate();
        planner();
        for (String lang : LANGUAGES) {
            reportCharts(lang);
            manual(lang);
            seal(lang);
        }
        System.out.println("Images written to " + OUT);
    }

    // =========================================================================================
    // Utilities
    // =========================================================================================

    static Font font(String family, String variant, float size) {
        try {
            Path file = FONTS.resolve("IBM Plex " + family).resolve("IBMPlex" + family + "-" + variant + ".ttf");
            return Font.createFont(Font.TRUETYPE_FONT, file.toFile()).deriveFont(size);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * An SVG drawing that is written as it is drawn: just enough of Java2D (fill, stroke, gradient and
     * text) for the vector illustrations of the examples. The texts are {@code <text>} with the
     * families of {@code resources/fonts/}, which is the only thing the engine accepts (reference/components/image.md).
     */
    static final class Svg {
        final int w;
        final int h;
        private final StringBuilder defs = new StringBuilder();
        private final StringBuilder body = new StringBuilder();
        private int gradients;

        Svg(int w, int h) {
            this.w = w;
            this.h = h;
        }

        static String num(double v) {
            String s = String.format(Locale.ROOT, "%.2f", v);
            if (s.indexOf('.') >= 0) {
                s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            return s.equals("-0") ? "0" : s;
        }

        static String hex(Color c) {
            return String.format(Locale.ROOT, "#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }

        /** {@code fill="#RRGGBB"} and, if it is not opaque, its {@code fill-opacity}. */
        private static String paint(String property, Color c) {
            String out = " " + property + "=\"" + hex(c) + "\"";
            return c.getAlpha() == 255 ? out : out + " " + property + "-opacity=\"" + num(c.getAlpha() / 255.0) + "\"";
        }

        private static String shape(Shape sh) {
            if (sh instanceof Ellipse2D e) {
                return "<ellipse cx=\"" + num(e.getCenterX()) + "\" cy=\"" + num(e.getCenterY()) + "\" rx=\""
                        + num(e.getWidth() / 2) + "\" ry=\"" + num(e.getHeight() / 2) + "\"";
            }
            if (sh instanceof RoundRectangle2D r) {
                return "<rect x=\"" + num(r.getX()) + "\" y=\"" + num(r.getY()) + "\" width=\"" + num(r.getWidth())
                        + "\" height=\"" + num(r.getHeight()) + "\" rx=\"" + num(r.getArcWidth() / 2) + "\" ry=\""
                        + num(r.getArcHeight() / 2) + "\"";
            }
            if (sh instanceof Rectangle2D r) {
                return "<rect x=\"" + num(r.getX()) + "\" y=\"" + num(r.getY()) + "\" width=\"" + num(r.getWidth())
                        + "\" height=\"" + num(r.getHeight()) + "\"";
            }
            if (sh instanceof java.awt.geom.Line2D l) {
                return "<line x1=\"" + num(l.getX1()) + "\" y1=\"" + num(l.getY1()) + "\" x2=\"" + num(l.getX2())
                        + "\" y2=\"" + num(l.getY2()) + "\"";
            }
            StringBuilder d = new StringBuilder();
            double[] c = new double[6];
            for (java.awt.geom.PathIterator it = sh.getPathIterator(null); !it.isDone(); it.next()) {
                switch (it.currentSegment(c)) {
                    case java.awt.geom.PathIterator.SEG_MOVETO -> d.append('M').append(num(c[0])).append(' ').append(num(c[1]));
                    case java.awt.geom.PathIterator.SEG_LINETO -> d.append('L').append(num(c[0])).append(' ').append(num(c[1]));
                    case java.awt.geom.PathIterator.SEG_QUADTO -> d.append('Q').append(num(c[0])).append(' ').append(num(c[1]))
                            .append(' ').append(num(c[2])).append(' ').append(num(c[3]));
                    case java.awt.geom.PathIterator.SEG_CUBICTO -> d.append('C').append(num(c[0])).append(' ').append(num(c[1]))
                            .append(' ').append(num(c[2])).append(' ').append(num(c[3])).append(' ').append(num(c[4]))
                            .append(' ').append(num(c[5]));
                    default -> d.append('Z');
                }
            }
            return "<path d=\"" + d + "\"";
        }

        void fill(Color c, Shape sh) {
            body.append(shape(sh)).append(paint("fill", c)).append("/>\n");
        }

        void stroke(Color c, float width, Shape sh) {
            body.append(shape(sh)).append(" fill=\"none\"").append(paint("stroke", c)).append(" stroke-width=\"")
                    .append(num(width)).append("\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>\n");
        }

        /** Fills with a linear gradient between two colors (which can carry opacity). */
        void gradient(Shape sh, GradientPaint p) {
            String id = "g" + ++gradients;
            Point2D a = p.getPoint1();
            Point2D b = p.getPoint2();
            defs.append("<linearGradient id=\"").append(id).append("\" gradientUnits=\"userSpaceOnUse\" x1=\"")
                    .append(num(a.getX())).append("\" y1=\"").append(num(a.getY())).append("\" x2=\"")
                    .append(num(b.getX())).append("\" y2=\"").append(num(b.getY())).append("\">")
                    .append(stop(0, p.getColor1())).append(stop(1, p.getColor2())).append("</linearGradient>\n");
            body.append(shape(sh)).append(" fill=\"url(#").append(id).append(")\"/>\n");
        }

        private static String stop(int offset, Color c) {
            return "<stop offset=\"" + offset + "\"" + paint("stop-color", c).replace("stop-color-opacity", "stop-opacity")
                    + "/>";
        }

        void text(String s, Font f, Color c, float x, float y) {
            String name = f.getFontName();
            String weight = name.contains("Bold") ? " font-weight=\"bold\"" : "";
            String style = name.contains("Italic") ? " font-style=\"italic\"" : "";
            body.append("<text x=\"").append(num(x)).append("\" y=\"").append(num(y)).append("\" font-family=\"")
                    .append(f.getFamily()).append("\"").append(weight).append(style).append(" font-size=\"")
                    .append(num(f.getSize2D())).append("\"").append(paint("fill", c)).append(">")
                    .append(s.replace("&", "&amp;").replace("<", "&lt;")).append("</text>\n");
        }

        void save(String relative) throws IOException {
            Path file = OUT.resolve(relative);
            Files.createDirectories(file.getParent());
            String text = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + w + "\" height=\"" + h
                    + "\" viewBox=\"0 0 " + w + " " + h + "\">\n"
                    + (defs.length() == 0 ? "" : "<defs>\n" + defs + "</defs>\n") + body + "</svg>\n";
            Files.writeString(file, text, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    static Svg svg(int w, int h) {
        return new Svg(w, h);
    }

    /** For measuring texts: the metrics of a font do not depend on where it is drawn. */
    private static final Graphics2D SCRATCH = graphics(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));

    static void fill(Svg g, Color c, Shape s) {
        g.fill(c, s);
    }

    static void stroke(Svg g, Color c, float w, Shape s) {
        g.stroke(c, w, s);
    }

    static void text(Svg g, String s, Font f, Color c, float x, float y) {
        g.text(s, f, c, x, y);
    }

    static void centered(Svg g, String s, Font f, Color c, float cx, float y) {
        g.text(s, f, c, cx - width(SCRATCH, s, f) / 2, y);
    }

    static void rightAligned(Svg g, String s, Font f, Color c, float right, float y) {
        g.text(s, f, c, right - width(SCRATCH, s, f), y);
    }

    static BufferedImage image(int w, int h, boolean alpha) {
        return new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    }

    static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    static void fill(Graphics2D g, Color c, Shape s) {
        g.setColor(c);
        g.fill(s);
    }

    static void stroke(Graphics2D g, Color c, float w, Shape s) {
        g.setColor(c);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    static void text(Graphics2D g, String s, Font f, Color c, float x, float y) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    static float width(Graphics2D g, String s, Font f) {
        FontMetrics m = g.getFontMetrics(f);
        return (float) f.getStringBounds(s, m.getFontRenderContext()).getWidth();
    }

    static void centered(Graphics2D g, String s, Font f, Color c, float cx, float y) {
        text(g, s, f, c, cx - width(g, s, f) / 2, y);
    }

    static void rightAligned(Graphics2D g, String s, Font f, Color c, float right, float y) {
        text(g, s, f, c, right - width(g, s, f), y);
    }

    /** The text in Spanish or in English, according to {@code lang}. */
    static String tr(String lang, String es, String en) {
        return lang.equals("es") ? es : en;
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static void save(BufferedImage img, String relative) throws IOException {
        Path file = OUT.resolve(relative);
        Files.createDirectories(file.getParent());
        if (relative.endsWith(".jpg")) {
            ImageWriter w = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam p = w.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(0.9f);
            try (FileImageOutputStream out = new FileImageOutputStream(file.toFile())) {
                w.setOutput(out);
                w.write(null, new IIOImage(img, null, null), p);
            }
            w.dispose();
        } else {
            ImageIO.write(img, "png", file.toFile());
        }
    }

    static void verticalGradient(Graphics2D g, int w, int h, Color top, Color bottom) {
        g.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g.fillRect(0, 0, w, h);
    }

    /** A handwritten signature: a cursive stroke with loops that fades towards the end, and an underline. */
    static void signature(Svg g, Color ink, long seed) {
        Random r = new Random(seed);
        int w = g.w;
        int h = g.h;
        double loops = 3 + r.nextInt(5) + r.nextDouble();
        double loop = h * (0.11 + r.nextDouble() * 0.04);
        double lean = 0.2 + r.nextDouble() * 0.9;
        double slope = r.nextDouble() * 0.2 - 0.04;
        Path2D.Float p = new Path2D.Float();
        int n = 600;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double env = Math.pow(Math.sin(Math.PI * Math.pow(t, 0.6)), 0.8) * (1.15 - 0.5 * t);
            double phase = 2 * Math.PI * loops * t;
            double x = w * (0.07 + 0.78 * t) - loop * env * Math.sin(phase) + lean * loop * env * Math.cos(phase);
            double y = h * (0.62 - slope * t) - loop * 1.7 * env * Math.cos(phase);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        stroke(g, ink, h * 0.028f, p);
        Path2D.Float underline = new Path2D.Float();
        underline.moveTo(w * 0.16, h * 0.86);
        underline.curveTo(w * 0.4, h * 0.78, w * 0.66, h * 0.9, w * 0.9, h * 0.74);
        stroke(g, ink, h * 0.02f, underline);
    }

    /** A five-pointed star, drawn because the font does not have the character. */
    static Path2D.Float star(float cx, float cy, float outer, float inner) {
        Path2D.Float s = new Path2D.Float();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float rr = i % 2 == 0 ? outer : inner;
            float x = cx + (float) Math.cos(a) * rr;
            float y = cy + (float) Math.sin(a) * rr;
            if (i == 0) {
                s.moveTo(x, y);
            } else {
                s.lineTo(x, y);
            }
        }
        s.closePath();
        return s;
    }

    // =========================================================================================
    // Nogal brand (invoice, letter, catalog)
    // =========================================================================================

    /** The logo mark: the rings of a walnut trunk. */
    static void ringsMark(Svg g, float cx, float cy, float r, Color main, Color accent) {
        for (int i = 0; i < 5; i++) {
            float rr = r * (1 - i * 0.19f);
            float wobble = 1 + (i % 2 == 0 ? 0.04f : -0.03f);
            Ellipse2D.Float e = new Ellipse2D.Float(cx - rr * wobble, cy - rr, rr * 2 * wobble, rr * 2);
            stroke(g, i == 2 ? accent : main, r * 0.075f, e);
        }
        fill(g, accent, new Ellipse2D.Float(cx - r * 0.07f, cy - r * 0.07f, r * 0.14f, r * 0.14f));
    }

    static void brand() throws IOException {
        // Logo mark and name, horizontal.
        Svg g = svg(1200, 360);
        ringsMark(g, 170, 180, 150, WALNUT, AMBER);
        text(g, "Nogal", font("Serif", "Bold", 190), WALNUT, 360, 218);
        text(g, "TALLER DE MOBILIARIO", font("Sans", "Regular", 44), AMBER, 366, 290);
        g.save("brand/nogal.svg");

        // Only the logo mark, in white, for dark backgrounds.
        Svg mark = svg(400, 400);
        ringsMark(mark, 200, 200, 180, Color.WHITE, new Color(0xF3C27C));
        mark.save("brand/nogal-mark-white.svg");
    }

    // =========================================================================================
    // Letter: signature
    // =========================================================================================

    static void letter() throws IOException {
        Svg g = svg(800, 300);
        signature(g, new Color(0x1B2A6B), 11);
        g.save("letter/signature.svg");
    }

    // =========================================================================================
    // Resume: portrait
    // =========================================================================================

    static void resume() throws IOException {
        BufferedImage img = image(700, 700, false);
        Graphics2D g = graphics(img);
        verticalGradient(g, 700, 700, new Color(0xCFE6E4), new Color(0x7FB5B2));
        // Blurred window in the background.
        fill(g, alpha(Color.WHITE, 70), new RoundRectangle2D.Float(460, 90, 190, 260, 20, 20));
        fill(g, alpha(Color.WHITE, 50), new RoundRectangle2D.Float(60, 140, 120, 120, 120, 120));
        // Shoulders and sweater.
        Path2D.Float body = new Path2D.Float();
        body.moveTo(60, 700);
        body.curveTo(70, 560, 190, 520, 350, 520);
        body.curveTo(510, 520, 630, 560, 640, 700);
        body.closePath();
        fill(g, new Color(0x2B4C6F), body);
        // Neck and shirt collar.
        fill(g, new Color(0xD9A47E), new RoundRectangle2D.Float(300, 430, 100, 120, 40, 40));
        Path2D.Float collar = new Path2D.Float();
        collar.moveTo(290, 525);
        collar.lineTo(350, 600);
        collar.lineTo(410, 525);
        collar.lineTo(380, 520);
        collar.lineTo(350, 560);
        collar.lineTo(320, 520);
        collar.closePath();
        fill(g, new Color(0xF4F1EA), collar);
        // Hair behind, face and bangs.
        fill(g, new Color(0x3A2A22), new Ellipse2D.Float(222, 120, 256, 330));
        fill(g, new Color(0xE3B18C), new Ellipse2D.Float(250, 200, 200, 250));
        Path2D.Float fringe = new Path2D.Float();
        fringe.moveTo(238, 300);
        fringe.curveTo(240, 150, 460, 120, 468, 300);
        fringe.curveTo(420, 230, 330, 200, 238, 300);
        fringe.closePath();
        fill(g, new Color(0x3A2A22), fringe);
        // Features.
        fill(g, new Color(0x2A2220), new Ellipse2D.Float(300, 320, 22, 14));
        fill(g, new Color(0x2A2220), new Ellipse2D.Float(380, 320, 22, 14));
        stroke(g, new Color(0x2A2220), 5, new Arc2D.Float(300, 270, 40, 20, 20, 140, Arc2D.OPEN));
        stroke(g, new Color(0x2A2220), 5, new Arc2D.Float(362, 270, 40, 20, 20, 140, Arc2D.OPEN));
        stroke(g, new Color(0xB5655A), 6, new Arc2D.Float(315, 365, 70, 40, 200, 140, Arc2D.OPEN));
        stroke(g, alpha(new Color(0xB0805F), 160), 5, new Arc2D.Float(335, 330, 30, 40, 220, 100, Arc2D.OPEN));
        g.dispose();
        save(img, "resume/photo.jpg");
    }

    // =========================================================================================
    // Annual report: cover and charts
    // =========================================================================================

    static void windTurbine(Graphics2D g, float x, float base, float h, double angle, Color c) {
        stroke(g, c, h * 0.035f, new java.awt.geom.Line2D.Float(x, base, x, base - h));
        float hubY = base - h;
        for (int i = 0; i < 3; i++) {
            double a = angle + i * 2 * Math.PI / 3;
            stroke(g, c, h * 0.022f, new java.awt.geom.Line2D.Float(x, hubY,
                    x + (float) Math.cos(a) * h * 0.55f, hubY + (float) Math.sin(a) * h * 0.55f));
        }
        fill(g, c, new Ellipse2D.Float(x - h * 0.03f, hubY - h * 0.03f, h * 0.06f, h * 0.06f));
    }

    static void report() throws IOException {
        // Cover: a wind farm at sunset.
        int w = 1400;
        int h = 800;
        BufferedImage cover = image(w, h, false);
        Graphics2D g = graphics(cover);
        g.setPaint(new GradientPaint(0, 0, new Color(0x16335B), 0, h * 0.62f, new Color(0xF2A65A)));
        g.fillRect(0, 0, w, h);
        fill(g, alpha(new Color(0xFFE2A8), 230), new Ellipse2D.Float(930, 330, 190, 190));
        fill(g, alpha(new Color(0xFFE2A8), 60), new Ellipse2D.Float(870, 270, 310, 310));
        Random r = new Random(3);
        for (int i = 0; i < 70; i++) {
            fill(g, alpha(Color.WHITE, 40 + r.nextInt(120)),
                    new Ellipse2D.Float(r.nextInt(w), r.nextInt((int) (h * 0.3)), 2.4f, 2.4f));
        }
        int[] heights = {420, 360, 330};
        Color[] hills = {new Color(0x1E3A5F), new Color(0x17304F), new Color(0x0F2238)};
        float[] baseline = {560, 620, 700};
        for (int layer = 0; layer < 3; layer++) {
            Path2D.Float hill = new Path2D.Float();
            hill.moveTo(0, h);
            hill.lineTo(0, baseline[layer]);
            for (int x = 0; x <= w; x += 20) {
                hill.lineTo(x, baseline[layer] + Math.sin(x / (170.0 + layer * 60) + layer * 2) * (28 - layer * 5));
            }
            hill.lineTo(w, h);
            hill.closePath();
            fill(g, hills[layer], hill);
        }
        double[] turbineX = {180, 420, 700, 1180};
        float[] turbineH = {250, 190, 150, 210};
        for (int i = 0; i < turbineX.length; i++) {
            float base = (float) (580 + (i % 3) * 36);
            windTurbine(g, (float) turbineX[i], base, turbineH[i], 0.4 + i * 0.7, new Color(0xE9EEF5));
        }
        g.dispose();
        save(cover, "report/cover.jpg");
    }

    /** The three charts of the report, with their texts in {@code lang}. */
    static void reportCharts(String lang) throws IOException {
        // Monthly production: bars.
        double[] mwh = {112, 128, 171, 208, 251, 287, 306, 291, 232, 176, 124, 98};
        String[] months = lang.equals("es")
                ? new String[] {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"}
                : new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Svg bars = svg(1400, 640);
        float left = 90;
        float right = 1370;
        float top = 40;
        float bottom = 560;
        Font small = font("Sans", "Regular", 26);
        for (int v = 0; v <= 300; v += 100) {
            float y = bottom - (bottom - top) * v / 320f;
            stroke(bars, new Color(0xD9DDE3), 2, new java.awt.geom.Line2D.Float(left, y, right, y));
            rightAligned(bars, String.valueOf(v), small, GREY, left - 14, y + 9);
        }
        float slot = (right - left) / 12;
        for (int i = 0; i < 12; i++) {
            float bh = (float) ((bottom - top) * mwh[i] / 320);
            Color c = mwh[i] == 306 ? new Color(0xE07B1F) : new Color(0x1B6E8C);
            fill(bars, c, new RoundRectangle2D.Float(left + slot * i + slot * 0.18f, bottom - bh, slot * 0.64f, bh, 8, 8));
            centered(bars, months[i], small, GREY, left + slot * (i + 0.5f), bottom + 40);
        }
        bars.save("report/" + lang + "/chart-production.svg");

        // Customer mix: ring with legend.
        Svg ring = svg(1400, 640);
        String[] names = lang.equals("es")
                ? new String[] {"Hogares", "Comercios", "Industria", "Administración"}
                : new String[] {"Households", "Businesses", "Industry", "Public sector"};
        double[] parts = {46, 27, 19, 8};
        Color[] colors = {new Color(0x1B6E8C), new Color(0xE07B1F), new Color(0x4C8C4A), new Color(0x8A6FB3)};
        double angle = 90;
        for (int i = 0; i < parts.length; i++) {
            double sweep = -360 * parts[i] / 100;
            java.awt.geom.Area slice = new java.awt.geom.Area(
                    new Arc2D.Double(60, 40, 560, 560, angle, sweep - 0.01, Arc2D.PIE));
            slice.subtract(new java.awt.geom.Area(new Ellipse2D.Float(180, 160, 320, 320)));
            fill(ring, colors[i], slice);
            angle += sweep;
        }
        centered(ring, tr(lang, "8.412", "8,412"), font("Sans", "Bold", 64), INK, 340, 335);
        centered(ring, tr(lang, "clientes", "customers"), font("Sans", "Regular", 32), GREY, 340, 382);
        for (int i = 0; i < parts.length; i++) {
            float y = 150 + i * 110;
            fill(ring, colors[i], new RoundRectangle2D.Float(720, y - 34, 44, 44, 10, 10));
            text(ring, names[i], font("Sans", "Regular", 42), INK, 790, y);
            text(ring, (int) parts[i] + tr(lang, " %", "%"), font("Sans", "Bold", 42), colors[i], 1170, y);
        }
        ring.save("report/" + lang + "/chart-mix.svg");

        // Cumulative emission savings: line with area.
        Svg line = svg(1400, 640);
        double[] t = {0, 1.4, 3.1, 5.2, 8.0, 11.6, 15.9};
        String[] years = {"2019", "2020", "2021", "2022", "2023", "2024", "2025"};
        float lx = 100;
        float rx = 1330;
        float ty = 50;
        float by = 540;
        for (int v = 0; v <= 15; v += 5) {
            float y = by - (by - ty) * v / 17f;
            stroke(line, new Color(0xD9DDE3), 2, new java.awt.geom.Line2D.Float(lx, y, rx, y));
            rightAligned(line, String.valueOf(v), small, GREY, lx - 14, y + 9);
        }
        Path2D.Float area = new Path2D.Float();
        Path2D.Float curve = new Path2D.Float();
        for (int i = 0; i < t.length; i++) {
            float x = lx + (rx - lx) * i / (t.length - 1);
            float y = (float) (by - (by - ty) * t[i] / 17);
            if (i == 0) {
                area.moveTo(x, by);
                area.lineTo(x, y);
                curve.moveTo(x, y);
            } else {
                area.lineTo(x, y);
                curve.lineTo(x, y);
            }
            centered(line, years[i], small, GREY, x, by + 44);
        }
        area.lineTo(rx, by);
        area.closePath();
        line.gradient(area, new GradientPaint(0, ty, alpha(new Color(0x4C8C4A), 150), 0, by, alpha(new Color(0x4C8C4A), 10)));
        stroke(line, new Color(0x2F6B2E), 7, curve);
        for (int i = 0; i < t.length; i++) {
            float x = lx + (rx - lx) * i / (t.length - 1);
            float y = (float) (by - (by - ty) * t[i] / 17);
            fill(line, Color.WHITE, new Ellipse2D.Float(x - 11, y - 11, 22, 22));
            stroke(line, new Color(0x2F6B2E), 5, new Ellipse2D.Float(x - 11, y - 11, 22, 22));
        }
        line.save("report/" + lang + "/chart-savings.svg");
    }

    // =========================================================================================
    // Newsletter: two photos
    // =========================================================================================

    static void newsletter() throws IOException {
        // Market: colored awnings.
        BufferedImage m = image(1200, 780, false);
        Graphics2D g = graphics(m);
        verticalGradient(g, 1200, 780, new Color(0xBFDFF2), new Color(0xF6EBD3));
        fill(g, new Color(0xD8C3A0), new Rectangle2D.Float(0, 600, 1200, 180));
        Color[] awn = {new Color(0xD64545), new Color(0xF2B134), new Color(0x2E8B7A), new Color(0x3D6FB6)};
        for (int i = 0; i < 4; i++) {
            float x = 40 + i * 290;
            fill(g, new Color(0x7A5A3C), new Rectangle2D.Float(x + 20, 330, 12, 270));
            fill(g, new Color(0x7A5A3C), new Rectangle2D.Float(x + 228, 330, 12, 270));
            Path2D.Float top = new Path2D.Float();
            top.moveTo(x, 330);
            top.lineTo(x + 260, 330);
            top.lineTo(x + 290, 230);
            top.lineTo(x - 30, 230);
            top.closePath();
            fill(g, awn[i], top);
            for (int s = 0; s < 5; s++) {
                fill(g, alpha(Color.WHITE, 90), new Rectangle2D.Float(x - 30 + s * 64 + 32, 230, 32, 100));
            }
            fill(g, new Color(0xA9825A), new Rectangle2D.Float(x + 10, 470, 240, 20));
            Random r = new Random(i);
            for (int k = 0; k < 9; k++) {
                Color fruit = new Color[]{new Color(0xD64545), new Color(0xF28C1E), new Color(0x7DB343), new Color(0xF2D12E)}[r.nextInt(4)];
                fill(g, fruit, new Ellipse2D.Float(x + 24 + k * 24, 440 + r.nextInt(8), 24, 24));
            }
        }
        g.dispose();
        save(m, "newsletter/market.jpg");

        // Mountain trail.
        BufferedImage t = image(1200, 780, false);
        g = graphics(t);
        verticalGradient(g, 1200, 780, new Color(0x7FB6E6), new Color(0xE9F2F7));
        fill(g, alpha(Color.WHITE, 200), new Ellipse2D.Float(160, 100, 260, 70));
        fill(g, alpha(Color.WHITE, 170), new Ellipse2D.Float(260, 130, 220, 60));
        fill(g, alpha(Color.WHITE, 190), new Ellipse2D.Float(800, 150, 300, 70));
        Path2D.Float far = new Path2D.Float();
        far.moveTo(0, 520);
        far.lineTo(150, 330);
        far.lineTo(260, 430);
        far.lineTo(430, 220);
        far.lineTo(620, 450);
        far.lineTo(760, 310);
        far.lineTo(980, 500);
        far.lineTo(1200, 360);
        far.lineTo(1200, 780);
        far.lineTo(0, 780);
        far.closePath();
        fill(g, new Color(0x8FA6C4), far);
        Path2D.Float snow = new Path2D.Float();
        snow.moveTo(430, 220);
        snow.lineTo(370, 300);
        snow.lineTo(410, 290);
        snow.lineTo(440, 330);
        snow.lineTo(480, 285);
        snow.lineTo(520, 305);
        snow.closePath();
        fill(g, Color.WHITE, snow);
        Path2D.Float near = new Path2D.Float();
        near.moveTo(0, 620);
        near.curveTo(250, 470, 520, 600, 760, 560);
        near.curveTo(950, 530, 1100, 580, 1200, 540);
        near.lineTo(1200, 780);
        near.lineTo(0, 780);
        near.closePath();
        fill(g, new Color(0x4F8A4B), near);
        Path2D.Float trail = new Path2D.Float();
        trail.moveTo(520, 780);
        trail.curveTo(540, 700, 760, 700, 700, 640);
        trail.curveTo(650, 600, 600, 590, 640, 570);
        trail.lineTo(655, 572);
        trail.curveTo(630, 595, 690, 610, 735, 650);
        trail.curveTo(800, 710, 650, 720, 700, 780);
        trail.closePath();
        fill(g, new Color(0xD9C29B), trail);
        for (int i = 0; i < 9; i++) {
            float x = 60 + i * 130 + (i % 2) * 30;
            float y = 640 + (i % 3) * 30;
            fill(g, new Color(0x2F5E2E), new Rectangle2D.Float(x + 14, y, 8, 40));
            Path2D.Float tree = new Path2D.Float();
            tree.moveTo(x, y + 10);
            tree.lineTo(x + 18, y - 70);
            tree.lineTo(x + 36, y + 10);
            tree.closePath();
            fill(g, new Color(0x2F5E2E), tree);
        }
        g.dispose();
        save(t, "newsletter/trail.jpg");
    }

    // =========================================================================================
    // Menu: emblem and ornament
    // =========================================================================================

    static void menu() throws IOException {
        Svg g = svg(500, 500);
        Color navy = new Color(0x14365C);
        stroke(g, navy, 8, new Ellipse2D.Float(20, 20, 460, 460));
        stroke(g, navy, 3, new Ellipse2D.Float(40, 40, 420, 420));
        for (int i = 0; i < 3; i++) {
            Path2D.Float wave = new Path2D.Float();
            float y = 300 + i * 34;
            wave.moveTo(110, y);
            for (int k = 0; k < 4; k++) {
                wave.curveTo(110 + k * 70 + 18, y - 26, 110 + k * 70 + 52, y + 26, 110 + (k + 1) * 70, y);
            }
            stroke(g, i == 0 ? new Color(0x2B8AA6) : navy, 7, wave);
        }
        fill(g, new Color(0xE9A23B), new Ellipse2D.Float(210, 120, 80, 80));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            stroke(g, new Color(0xE9A23B), 5, new java.awt.geom.Line2D.Double(250 + Math.cos(a) * 54, 160 + Math.sin(a) * 54,
                    250 + Math.cos(a) * 74, 160 + Math.sin(a) * 74));
        }
        centered(g, "LA CALA", font("Serif", "Bold", 54), navy, 250, 262);
        g.save("menu/emblem.svg");

        // Ornament: rule with leaves.
        Svg o = svg(1200, 80);
        Color gold = new Color(0xA8782F);
        stroke(o, gold, 3, new java.awt.geom.Line2D.Float(40, 40, 520, 40));
        stroke(o, gold, 3, new java.awt.geom.Line2D.Float(680, 40, 1160, 40));
        for (int side = 0; side < 2; side++) {
            for (int k = 0; k < 3; k++) {
                float cx = 600 + (side == 0 ? -1 : 1) * (36 + k * 38);
                Path2D.Float leaf = new Path2D.Float();
                leaf.moveTo(cx, 40);
                leaf.curveTo(cx + (side == 0 ? -14 : 14), 10, cx + (side == 0 ? -34 : 34), 14, cx + (side == 0 ? -30 : 30), 40);
                leaf.curveTo(cx + (side == 0 ? -34 : 34), 66, cx + (side == 0 ? -14 : 14), 70, cx, 40);
                fill(o, gold, leaf);
            }
        }
        fill(o, gold, new Ellipse2D.Float(584, 24, 32, 32));
        o.save("menu/ornament.svg");
    }

    // =========================================================================================
    // Contract: logo of the studio
    // =========================================================================================

    static void contract() throws IOException {
        Svg g = svg(900, 240);
        Color violet = new Color(0x3F3D8F);
        fill(g, violet, new Ellipse2D.Float(30, 30, 180, 180));
        fill(g, new Color(0xFFD166), new Ellipse2D.Float(78, 78, 84, 84));
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5;
            stroke(g, new Color(0xFFD166), 6, new java.awt.geom.Line2D.Double(120 + Math.cos(a) * 52, 120 + Math.sin(a) * 52,
                    120 + Math.cos(a) * 72, 120 + Math.sin(a) * 72));
        }
        text(g, "Lumen", font("Sans", "Bold", 118), violet, 250, 148);
        text(g, "ESTUDIO DE DISEÑO", font("Sans", "Regular", 36), GREY, 254, 204);
        g.save("contract/lumen.svg");
    }

    // =========================================================================================
    // Manual: device, phone screen and wiring diagram
    // =========================================================================================

    /** The three figures of the manual, with their texts in {@code lang}. */
    static void manual(String lang) throws IOException {
        // Thermostat seen from the front.
        Svg d = svg(900, 900);
        fill(d, alpha(Color.BLACK, 30), new RoundRectangle2D.Float(110, 130, 700, 700, 120, 120));
        fill(d, new Color(0xF4F4F1), new RoundRectangle2D.Float(90, 100, 700, 700, 120, 120));
        stroke(d, new Color(0xD0D0CA), 4, new RoundRectangle2D.Float(90, 100, 700, 700, 120, 120));
        fill(d, new Color(0x222831), new Ellipse2D.Float(190, 200, 500, 500));
        stroke(d, new Color(0x5CC8C0), 18, new Arc2D.Float(215, 225, 450, 450, 225, -240, Arc2D.OPEN));
        stroke(d, new Color(0x2E3A46), 18, new Arc2D.Float(215, 225, 450, 450, -15, -30, Arc2D.OPEN));
        centered(d, tr(lang, "21,5°", "21.5°"), font("Sans", "Bold", 130), Color.WHITE, 440, 470);
        centered(d, tr(lang, "Modo Confort", "Comfort mode"), font("Sans", "Regular", 36), new Color(0x9FB3C2), 440, 535);
        fill(d, new Color(0xE9A23B), new Ellipse2D.Float(418, 580, 44, 44));
        centered(d, tr(lang, "Objetivo 22°", "Target 22°"), font("Sans", "Regular", 32), new Color(0x9FB3C2), 440, 660);
        fill(d, new Color(0x4C8C4A), new Ellipse2D.Float(660, 135, 26, 26));
        d.save("manual/" + lang + "/thermostat.svg");

        // Screen of the app.
        BufferedImage s = image(560, 1000, true);
        Graphics2D g = graphics(s);
        fill(g, new Color(0x1A1D23), new RoundRectangle2D.Float(10, 10, 540, 980, 70, 70));
        fill(g, new Color(0xF5F7FA), new RoundRectangle2D.Float(28, 28, 504, 944, 56, 56));
        fill(g, new Color(0x1F7A7A), new Rectangle2D.Float(28, 28, 504, 140));
        g.setClip(new RoundRectangle2D.Float(28, 28, 504, 944, 56, 56));
        fill(g, new Color(0x1F7A7A), new Rectangle2D.Float(28, 28, 504, 140));
        g.setClip(null);
        text(g, tr(lang, "Salón", "Living room"), font("Sans", "Bold", 48), Color.WHITE, 62, 110);
        rightAligned(g, "09:41", font("Sans", "Regular", 30), alpha(Color.WHITE, 220), 500, 62);
        stroke(g, new Color(0xD7DEE6), 22, new Arc2D.Float(110, 220, 340, 340, 225, -270, Arc2D.OPEN));
        stroke(g, new Color(0xE9A23B), 22, new Arc2D.Float(110, 220, 340, 340, 225, -170, Arc2D.OPEN));
        centered(g, tr(lang, "21,5°", "21.5°"), font("Sans", "Bold", 84), INK, 280, 410);
        centered(g, tr(lang, "Calentando", "Heating"), font("Sans", "Regular", 30), GREY, 280, 460);
        fill(g, Color.WHITE, new Ellipse2D.Float(130, 620, 120, 120));
        fill(g, Color.WHITE, new Ellipse2D.Float(310, 620, 120, 120));
        stroke(g, new Color(0xD7DEE6), 4, new Ellipse2D.Float(130, 620, 120, 120));
        stroke(g, new Color(0xD7DEE6), 4, new Ellipse2D.Float(310, 620, 120, 120));
        centered(g, "–", font("Sans", "Bold", 80), INK, 190, 705);
        centered(g, "+", font("Sans", "Bold", 80), INK, 370, 705);
        fill(g, new Color(0xFFFFFF), new RoundRectangle2D.Float(62, 800, 436, 110, 30, 30));
        stroke(g, new Color(0xD7DEE6), 3, new RoundRectangle2D.Float(62, 800, 436, 110, 30, 30));
        text(g, tr(lang, "Programa semanal", "Weekly schedule"), font("Sans", "Regular", 34), INK, 90, 866);
        fill(g, new Color(0x4C8C4A), new RoundRectangle2D.Float(400, 832, 70, 40, 40, 40));
        fill(g, Color.WHITE, new Ellipse2D.Float(436, 836, 32, 32));
        g.dispose();
        save(s, "manual/" + lang + "/app.png");

        // Wiring diagram: terminal block.
        Svg w = svg(1400, 560);
        fill(w, new Color(0xF4F4F1), new RoundRectangle2D.Float(40, 40, 520, 480, 40, 40));
        stroke(w, new Color(0xD0D0CA), 4, new RoundRectangle2D.Float(40, 40, 520, 480, 40, 40));
        text(w, tr(lang, "Termostato", "Thermostat"), font("Sans", "Bold", 40), INK, 80, 110);
        String[] term = {"L", "N", "C", "NO"};
        Color[] wire = {new Color(0x8B4A2B), new Color(0x2F6DB5), new Color(0x777777), new Color(0xD9A400)};
        String[] label = lang.equals("es")
                ? new String[] {"Fase (marrón)", "Neutro (azul)", "Común de la caldera", "Caldera: contacto NO"}
                : new String[] {"Live (brown)", "Neutral (blue)", "Boiler common", "Boiler: NO contact"};
        for (int i = 0; i < 4; i++) {
            float y = 160 + i * 88;
            fill(w, new Color(0x2E3A46), new RoundRectangle2D.Float(440, y, 120, 60, 12, 12));
            centered(w, term[i], font("Mono", "Bold", 34), Color.WHITE, 500, y + 42);
            stroke(w, wire[i], 12, new java.awt.geom.Line2D.Float(560, y + 30, 900, y + 30));
            fill(w, wire[i], new Ellipse2D.Float(890, y + 18, 24, 24));
            text(w, label[i], font("Sans", "Regular", 38), INK, 940, y + 43);
        }
        w.save("manual/" + lang + "/wiring.svg");
    }

    // =========================================================================================
    // Catalog: eight pieces of furniture
    // =========================================================================================

    static void catalog() throws IOException {
        Color wood = new Color(0x9A6B3F);
        Color darkWood = new Color(0x6E4727);
        Color[] bg = {new Color(0xF1E6D5), new Color(0xE3EBE3), new Color(0xE8E2EF), new Color(0xF3E1DC),
                new Color(0xDDE9EE), new Color(0xF2EBCF), new Color(0xE7E7E2), new Color(0xEBDDD0)};
        String[] names = {"silla", "mesa", "lampara", "estanteria", "taburete", "banco", "espejo", "escritorio"};
        for (int i = 0; i < names.length; i++) {
            BufferedImage img = image(800, 800, false);
            Graphics2D g = graphics(img);
            g.setPaint(new GradientPaint(0, 0, mix(bg[i], Color.WHITE, 0.35), 0, 800, bg[i]));
            g.fillRect(0, 0, 800, 800);
            fill(g, alpha(Color.BLACK, 22), new Rectangle2D.Float(0, 640, 800, 160));
            fill(g, alpha(Color.BLACK, 28), new Ellipse2D.Float(130, 620, 540, 50));
            switch (i) {
                case 0 -> { // chair
                    fill(g, wood, new RoundRectangle2D.Float(250, 120, 300, 40, 12, 12));
                    for (int k = 0; k < 4; k++) {
                        fill(g, wood, new Rectangle2D.Float(262 + k * 78, 150, 22, 270));
                    }
                    fill(g, darkWood, new RoundRectangle2D.Float(220, 400, 360, 50, 14, 14));
                    fill(g, wood, new Rectangle2D.Float(240, 440, 24, 200));
                    fill(g, wood, new Rectangle2D.Float(536, 440, 24, 200));
                    fill(g, darkWood, new Rectangle2D.Float(262, 500, 276, 14));
                }
                case 1 -> { // table
                    fill(g, darkWood, new RoundRectangle2D.Float(120, 300, 560, 56, 14, 14));
                    fill(g, wood, new RoundRectangle2D.Float(150, 340, 500, 36, 8, 8));
                    for (int x : new int[]{170, 600}) {
                        fill(g, wood, new Rectangle2D.Float(x, 376, 30, 270));
                    }
                    fill(g, darkWood, new Rectangle2D.Float(200, 520, 400, 16));
                }
                case 2 -> { // lamp
                    Path2D.Float shade = new Path2D.Float();
                    shade.moveTo(300, 150);
                    shade.lineTo(500, 150);
                    shade.lineTo(580, 330);
                    shade.lineTo(220, 330);
                    shade.closePath();
                    fill(g, new Color(0xF2C879), shade);
                    fill(g, alpha(new Color(0xFFF1C9), 120), new Ellipse2D.Float(180, 300, 440, 280));
                    fill(g, darkWood, new Rectangle2D.Float(388, 330, 24, 290));
                    fill(g, darkWood, new Ellipse2D.Float(290, 600, 220, 40));
                }
                case 3 -> { // bookcase
                    fill(g, darkWood, new Rectangle2D.Float(200, 100, 24, 540));
                    fill(g, darkWood, new Rectangle2D.Float(576, 100, 24, 540));
                    for (int k = 0; k < 5; k++) {
                        fill(g, wood, new Rectangle2D.Float(200, 100 + k * 134, 400, 20));
                    }
                    Color[] books = {new Color(0xB5473A), new Color(0x2E6C8E), new Color(0x4C8C4A), new Color(0xE0A33B), new Color(0x6A4C93)};
                    Random r = new Random(5);
                    for (int row = 0; row < 4; row++) {
                        float x = 232;
                        while (x < 560) {
                            float bw = 20 + r.nextInt(20);
                            float bh = 70 + r.nextInt(40);
                            fill(g, books[r.nextInt(books.length)], new Rectangle2D.Float(x, 100 + row * 134 + 114 - bh, bw, bh));
                            x += bw + 3;
                            if (r.nextInt(7) == 0) {
                                x += 40;
                            }
                        }
                    }
                }
                case 4 -> { // stool
                    fill(g, darkWood, new Ellipse2D.Float(250, 300, 300, 70));
                    fill(g, wood, new Ellipse2D.Float(250, 280, 300, 70));
                    Path2D.Float l1 = new Path2D.Float();
                    l1.moveTo(300, 340);
                    l1.lineTo(330, 340);
                    l1.lineTo(280, 640);
                    l1.lineTo(250, 640);
                    l1.closePath();
                    fill(g, wood, l1);
                    Path2D.Float l2 = new Path2D.Float();
                    l2.moveTo(470, 340);
                    l2.lineTo(500, 340);
                    l2.lineTo(550, 640);
                    l2.lineTo(520, 640);
                    l2.closePath();
                    fill(g, wood, l2);
                    fill(g, darkWood, new Rectangle2D.Float(290, 500, 220, 14));
                }
                case 5 -> { // bench
                    fill(g, wood, new RoundRectangle2D.Float(100, 340, 600, 50, 12, 12));
                    fill(g, darkWood, new Rectangle2D.Float(100, 380, 600, 14));
                    for (int x : new int[]{140, 630}) {
                        fill(g, wood, new Rectangle2D.Float(x, 394, 30, 250));
                    }
                    fill(g, darkWood, new Rectangle2D.Float(170, 520, 460, 14));
                    fill(g, new Color(0xB5473A), new RoundRectangle2D.Float(150, 300, 500, 46, 24, 24));
                }
                case 6 -> { // mirror
                    fill(g, darkWood, new Ellipse2D.Float(190, 100, 420, 520));
                    g.setPaint(new GradientPaint(220, 130, new Color(0xE6F1F5), 600, 600, new Color(0xA9C6D1)));
                    g.fill(new Ellipse2D.Float(220, 130, 360, 460));
                    stroke(g, alpha(Color.WHITE, 170), 12, new Arc2D.Float(260, 170, 220, 300, 110, 70, Arc2D.OPEN));
                    fill(g, darkWood, new Rectangle2D.Float(380, 590, 40, 50));
                }
                default -> { // desk
                    fill(g, wood, new RoundRectangle2D.Float(100, 290, 600, 44, 10, 10));
                    fill(g, darkWood, new Rectangle2D.Float(120, 330, 26, 310));
                    fill(g, darkWood, new Rectangle2D.Float(560, 330, 26, 310));
                    fill(g, wood, new Rectangle2D.Float(440, 334, 140, 220));
                    for (int k = 0; k < 3; k++) {
                        fill(g, darkWood, new RoundRectangle2D.Float(452, 346 + k * 70, 116, 58, 8, 8));
                        fill(g, new Color(0xE9D3A8), new Ellipse2D.Float(502, 368 + k * 70, 16, 16));
                    }
                    fill(g, new Color(0x2B3342), new RoundRectangle2D.Float(200, 170, 180, 120, 10, 10));
                    fill(g, new Color(0x9EC7E8), new Rectangle2D.Float(210, 180, 160, 96));
                    fill(g, new Color(0x2B3342), new Rectangle2D.Float(280, 290, 20, 10));
                }
            }
            g.dispose();
            save(img, "catalog/" + names[i] + ".jpg");
        }
    }

    // =========================================================================================
    // Certificate: seal and two signatures
    // =========================================================================================

    /** The seal of the certificate, with its text in {@code lang}. */
    static void seal(String lang) throws IOException {
        Svg g = svg(600, 760);
        Color gold = new Color(0xC9A24A);
        Color goldDark = new Color(0x8C6D23);
        Path2D.Float ribbonL = new Path2D.Float();
        ribbonL.moveTo(190, 400);
        ribbonL.lineTo(280, 400);
        ribbonL.lineTo(250, 740);
        ribbonL.lineTo(210, 690);
        ribbonL.lineTo(150, 720);
        ribbonL.closePath();
        fill(g, new Color(0x9E2B25), ribbonL);
        Path2D.Float ribbonR = new Path2D.Float();
        ribbonR.moveTo(410, 400);
        ribbonR.lineTo(320, 400);
        ribbonR.lineTo(350, 740);
        ribbonR.lineTo(390, 690);
        ribbonR.lineTo(450, 720);
        ribbonR.closePath();
        fill(g, new Color(0xB5372F), ribbonR);
        Path2D.Float scallop = new Path2D.Float();
        int n = 28;
        for (int i = 0; i <= n * 4; i++) {
            double a = i * Math.PI * 2 / (n * 4);
            double rr = 270 + (i % 4 < 2 ? 14 : -4);
            double x = 300 + Math.cos(a) * rr;
            double y = 300 + Math.sin(a) * rr;
            if (i == 0) {
                scallop.moveTo(x, y);
            } else {
                scallop.lineTo(x, y);
            }
        }
        scallop.closePath();
        g.gradient(scallop, new GradientPaint(40, 40, new Color(0xE9CB7A), 560, 560, goldDark));
        fill(g, gold, new Ellipse2D.Float(70, 70, 460, 460));
        stroke(g, goldDark, 6, new Ellipse2D.Float(90, 90, 420, 420));
        stroke(g, new Color(0xF3E2A8), 3, new Ellipse2D.Float(110, 110, 380, 380));
        fill(g, goldDark, star(300, 240, 105, 44));
        centered(g, tr(lang, "CERTIFICADO", "CERTIFICATE"), font("Serif", "Bold", 40), new Color(0x4F3B0F), 300, 400);
        centered(g, tr(lang, "DE EXCELENCIA", "OF EXCELLENCE"), font("Serif", "Bold", 32), new Color(0x4F3B0F), 300, 446);
        g.save("certificate/" + lang + "/seal.svg");
    }

    /** The two signatures of the certificate. */
    static void certificate() throws IOException {
        Svg s1 = svg(700, 240);
        signature(s1, new Color(0x14305E), 21);
        s1.save("certificate/signature-director.svg");
        Svg s2 = svg(700, 240);
        signature(s2, new Color(0x3A1F4D), 34);
        s2.save("certificate/signature-teacher.svg");
    }

    // =========================================================================================
    // Weekly planner: a header icon
    // =========================================================================================

    static void planner() throws IOException {
        BufferedImage img = image(240, 240, true);
        Graphics2D g = graphics(img);
        fill(g, new Color(0x2D6A4F), new RoundRectangle2D.Float(20, 30, 200, 190, 36, 36));
        fill(g, new Color(0xD8F3DC), new RoundRectangle2D.Float(34, 78, 172, 128, 20, 20));
        fill(g, new Color(0x2D6A4F), new RoundRectangle2D.Float(62, 10, 24, 52, 12, 12));
        fill(g, new Color(0x2D6A4F), new RoundRectangle2D.Float(154, 10, 24, 52, 12, 12));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                fill(g, (r + c) % 3 == 0 ? new Color(0xF4A261) : new Color(0x95D5B2),
                        new RoundRectangle2D.Float(50 + c * 38, 94 + r * 36, 28, 26, 6, 6));
            }
        }
        g.dispose();
        save(img, "planner/icon.png");
    }
}
