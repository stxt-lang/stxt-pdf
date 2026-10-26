package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.position.RelativeOffset;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.Media;
import dev.stxt.boxes.tree.Style;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Comprueba, ya paginado, que nada desborda (ESPECIFICACION.md §5.12): cada línea de texto y
 * cada hijo en flujo (con sus márgenes horizontales) caben en la caja de contenido de su padre;
 * en modo paginado, el flujo cabe en el área útil de cada página, y las cajas {@code absolute} y
 * {@code fixed} caben en la página. Se mira el resultado final, no las pasadas de medida.
 *
 * <p>El error señala al culpable más profundo: la palabra que no cabe en su línea, la caja más
 * interior que se sale, o el {@code Height} que se queda corto.</p>
 */
public final class Overflow {

    private static final double EPSILON = 0.01;

    private Overflow() {
    }

    /** @throws InvalidModelException con el primer desbordamiento que encuentra */
    public static void check(List<Page> pages, Media media) {
        Rect area = media.pageArea();
        Insets m = media.margin();
        // La página física, en coordenadas del área útil.
        Rect page = new Rect(-m.left(), -m.top(), media.width(),
                media.paged() ? media.height().getAsDouble() : Double.POSITIVE_INFINITY);
        for (Page p : pages) {
            double height = media.paged() ? area.height() : Double.POSITIVE_INFINITY;
            content(p.content(), 0, 0, area.width(), height, media.paged(), page);
            for (Fragment f : p.fixed()) {
                fragment(f, 0, 0, page);
            }
        }
    }

    /** Un fragmento cuya caja de contenido del padre empieza en ({@code ox}, {@code oy}). */
    private static void fragment(Fragment f, double ox, double oy, Rect page) {
        double x = ox + f.x();
        double y = oy + f.y();
        if (f.box().style.isOutOfFlow()) {
            if (x < page.x() - EPSILON || y < page.y() - EPSILON
                    || x + f.width() > page.right() + EPSILON || y + f.height() > page.bottom() + EPSILON) {
                Style s = f.box().style;
                throw new InvalidModelException(s.isDeclared("offset") ? s.line("offset") : f.box().line,
                        ErrorCode.BOX_OVERFLOW, "La caja " + name(f.box()) + " se sale de la página: revisa su "
                        + "Offset y su tamaño");
            }
        }
        double cx = x + f.border().left() + f.padding().left();
        double cy = y + f.border().top() + f.padding().top();
        content(f, cx, cy, contentWidth(f), contentHeight(f), false, page);
    }

    /**
     * Las líneas y los hijos de un fragmento, cuya caja de contenido empieza en ({@code cx},
     * {@code cy}) y mide {@code width} × {@code height}; {@code isPage}: es el flujo de una página.
     */
    private static void content(Fragment f, double cx, double cy, double width, double height, boolean isPage,
                                Rect page) {
        for (TextLine l : f.lines()) {
            if (l.x() < -EPSILON || l.x() + l.width() > width + EPSILON) {
                throw textOverflow(f.box(), l, width);
            }
            if (l.y() + l.height() > height + EPSILON) {
                throw tooTall(f, l.y() + l.height(), height, null);
            }
        }
        for (Fragment c : f.children()) {
            if (!c.box().style.isOutOfFlow()) {
                // Las relative se comprueban en su sitio, sin el desplazamiento.
                RelativeOffset o = RelativeOffset.of(c.box().style, width,
                        Double.isFinite(height) ? OptionalDouble.of(height) : OptionalDouble.empty());
                double x = c.x() - o.dx();
                double y = c.y() - o.dy();
                Insets margin = c.box().margin;
                if (x - margin.left() < -EPSILON || x + c.width() + margin.right() > width + EPSILON) {
                    throw tooWide(c, cx + x, cy + y, cx, cx + width);
                }
                if (y + c.height() > height + EPSILON) {
                    if (isPage) {
                        throw pageOverflow(c, cx + x, cy + y, cy + height);
                    }
                    throw tooTall(f, y + c.height(), height, c);
                }
            }
            fragment(c, cx, cy, page);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Culpables
    // ------------------------------------------------------------------------------------------

    /** Un hijo se sale por los lados: el culpable es lo más interior que se sale. */
    private static InvalidModelException tooWide(Fragment c, double x, double y, double left, double right) {
        while (true) {
            double cx = x + c.border().left() + c.padding().left();
            double cy = y + c.border().top() + c.padding().top();
            // Una palabra que no cabe en su propia caja es la culpable; si cabe, lo es la caja.
            for (TextLine l : c.lines()) {
                if (l.x() < -EPSILON || l.x() + l.width() > contentWidth(c) + EPSILON) {
                    throw textOverflow(c.box(), l, contentWidth(c));
                }
            }
            Fragment inner = null;
            for (Fragment d : c.children()) {
                double dx = cx + d.x();
                if (!d.box().style.isOutOfFlow() && (dx < left - EPSILON || dx + d.width() > right + EPSILON)) {
                    inner = d;
                    x = dx;
                    y = cy + d.y();
                    break;
                }
            }
            if (inner == null) {
                Style s = c.box().style;
                String property = s.isDeclared("width") ? "width" : s.isDeclared("min-width") ? "min-width" : null;
                return new InvalidModelException(property != null ? s.line(property) : c.box().line,
                        ErrorCode.BOX_OVERFLOW, "La caja " + name(c.box()) + " no cabe a lo ancho: mide "
                        + pt(c.width()) + " y el contenido de su contenedor mide " + pt(right - left)
                        + "; reduce su ancho o el de su contenido");
            }
            c = inner;
        }
    }

    /** El contenido de una caja pasa de su alto: el culpable es su {@code Height} (o su máximo). */
    private static InvalidModelException tooTall(Fragment container, double needed, double height, Fragment child) {
        Box box = container.box();
        Style s = box.style;
        String property = s.isDeclared("height") ? "height" : s.isDeclared("max-height") ? "max-height" : null;
        String what = child == null ? "su texto" : "la caja " + name(child.box());
        return new InvalidModelException(property != null ? s.line(property) : box.line, ErrorCode.BOX_OVERFLOW,
                "El contenido de la caja " + name(box) + " no cabe en su alto: " + what + " llega a "
                        + pt(needed) + " y la caja deja " + pt(height));
    }

    /** Algo del flujo no cabe en la página y no se ha podido partir: el culpable es lo más interior que pasa del final. */
    private static InvalidModelException pageOverflow(Fragment c, double x, double y, double bottom) {
        while (true) {
            double cx = x + c.border().left() + c.padding().left();
            double cy = y + c.border().top() + c.padding().top();
            Fragment inner = null;
            for (Fragment d : c.children()) {
                if (!d.box().style.isOutOfFlow() && cy + d.y() + d.height() > bottom + EPSILON) {
                    inner = d;
                    x = cx + d.x();
                    y = cy + d.y();
                    break;
                }
            }
            if (inner == null) {
                Style s = c.box().style;
                int line = s.isDeclared("height") ? s.line("height")
                        : !c.lines().isEmpty() && s.isDeclared("line-height") ? s.line("line-height") : c.box().line;
                return new InvalidModelException(line, ErrorCode.PAGE_OVERFLOW, "La caja " + name(c.box())
                        + " no cabe en la página y no se puede partir: mide " + pt(c.height())
                        + " de alto y llega " + pt(y + c.height() - bottom) + " más allá del área útil");
            }
            c = inner;
        }
    }

    /** Una línea más ancha que su caja: una palabra que no cabe. Señala la línea del documento de esa palabra. */
    private static InvalidModelException textOverflow(Box box, TextLine l, double width) {
        String word = Arrays.stream(l.text().split(" ")).max(Comparator.comparingInt(String::length)).orElse(l.text());
        int index = box.text == null ? -1 : box.text.indexOf(word);
        int line = index >= 0 ? box.textLineAt(index) : box.textLine;
        return new InvalidModelException(line, ErrorCode.TEXT_OVERFLOW, "La palabra '" + word + "' no cabe en "
                + "la línea: la línea mide " + pt(l.width()) + " y la caja deja " + pt(width)
                + "; ensancha la caja o reduce el Font size");
    }

    private static double contentWidth(Fragment f) {
        return f.width() - f.border().horizontal() - f.padding().horizontal();
    }

    private static double contentHeight(Fragment f) {
        return f.height() - f.border().vertical() - f.padding().vertical();
    }

    /** {@code (Id main, línea 12)} o {@code de la línea 12}. */
    private static String name(Box box) {
        return box.id != null ? box.id + " (línea " + box.line + ")" : "de la línea " + box.line;
    }

    private static String pt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.replaceAll("\\.?0+$", "");
    }
}
