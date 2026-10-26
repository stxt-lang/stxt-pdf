package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Media;

import java.util.Locale;

/**
 * Salida SVG para depurar (hito 2): una página por fichero, en pt. Si {@code debug} está
 * activo, dibuja además el contorno de cada caja de borde.
 */
public final class SvgCanvas implements Canvas {

    private final StringBuilder svg = new StringBuilder();
    private final boolean debug;

    public SvgCanvas(boolean debug) {
        this.debug = debug;
    }

    /** SVG completo de una página. En modo continuo usa {@code contentHeight} como alto. */
    public static String page(Page page, Media media, double contentHeight, boolean debug) {
        double height = media.paged() ? media.height().getAsDouble() : contentHeight + media.margin().vertical();
        SvgCanvas c = new SvgCanvas(debug);
        Rect area = media.pageArea();
        StackingContext.build(page.content(), page.fixed(), area.x(), area.y()).paint(c);
        return String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2fpt\" height=\"%.2fpt\" viewBox=\"0 0 %.2f %.2f\">%n"
                        + "<rect width=\"100%%\" height=\"100%%\" fill=\"#ffffff\"/>%n%s</svg>%n",
                media.width(), height, media.width(), height, c.svg);
    }

    @Override
    public void background(Rect r, String color) {
        svg.append(String.format(Locale.ROOT, "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\"/>%n",
                r.x(), r.y(), r.width(), r.height(), color));
    }

    @Override
    public void border(Rect r, Insets w, String color) {
        if (w.top() > 0) {
            background(new Rect(r.x(), r.y(), r.width(), w.top()), color);
        }
        if (w.bottom() > 0) {
            background(new Rect(r.x(), r.bottom() - w.bottom(), r.width(), w.bottom()), color);
        }
        if (w.left() > 0) {
            background(new Rect(r.x(), r.y(), w.left(), r.height()), color);
        }
        if (w.right() > 0) {
            background(new Rect(r.right() - w.right(), r.y(), w.right(), r.height()), color);
        }
        if (debug) {
            svg.append(String.format(Locale.ROOT,
                    "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"none\" stroke=\"#d33\" stroke-width=\"0.3\" stroke-dasharray=\"2 2\"/>%n",
                    r.x(), r.y(), r.width(), r.height()));
        }
    }

    @Override
    public void text(double x, double baselineY, String text, String font, double fontSize, String color,
                     double wordSpacing) {
        String spacing = wordSpacing == 0 ? "" : String.format(Locale.ROOT, " word-spacing=\"%.2f\"", wordSpacing);
        svg.append(String.format(Locale.ROOT,
                "<text x=\"%.2f\" y=\"%.2f\" font-family=\"%s\" font-size=\"%.2f\" fill=\"%s\"%s>%s</text>%n",
                x, baselineY, escape(font), fontSize, color, spacing, escape(text)));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
