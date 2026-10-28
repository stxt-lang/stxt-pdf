package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Element;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.Media;
import dev.stxt.boxes.tree.Style;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static dev.stxt.boxes.layout.Messages.name;
import static dev.stxt.boxes.layout.Messages.pt;
import static dev.stxt.boxes.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.boxes.tree.PropertyKey.BORDER;
import static dev.stxt.boxes.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.boxes.tree.PropertyKey.GRID_ROWS;
import static dev.stxt.boxes.tree.PropertyKey.HEIGHT;
import static dev.stxt.boxes.tree.PropertyKey.LINE_HEIGHT;
import static dev.stxt.boxes.tree.PropertyKey.MAX_HEIGHT;
import static dev.stxt.boxes.tree.PropertyKey.MIN_WIDTH;
import static dev.stxt.boxes.tree.PropertyKey.OFFSET;
import static dev.stxt.boxes.tree.PropertyKey.PADDING;
import static dev.stxt.boxes.tree.PropertyKey.WIDTH;

/**
 * Comprueba, ya paginado, que nada desborda (ESPECIFICACION.md §5.12): cada línea de texto y
 * cada hijo en flujo (con sus márgenes horizontales) caben en la caja de contenido de su padre;
 * el flujo cabe en el área útil de cada página, y las cajas {@code absolute} y
 * {@code fixed} caben en la página. Se mira el resultado final, no las pasadas de medida.
 *
 * <p>El error señala al culpable más profundo: la palabra que no cabe en su línea, la caja más
 * interior que se sale, o el {@code Height} que se queda corto.</p>
 */
public final class Overflow {

    /**
     * Lo que una caja puede salirse sin que sea un error: 0,01 pt, la misma precisión con que se comparan
     * los volcados del §7. Es una tolerancia del modelo, más ancha que la de redondeo
     * ({@link dev.stxt.boxes.model.Tolerance#EPSILON}); ningún test depende hoy de la diferencia.
     */
    private static final double TOLERANCE = 0.01;

    private Overflow() {
    }

    /** @throws InvalidModelException con el primer desbordamiento que encuentra */
    public static void check(List<Page> pages, Media media) {
        Rect area = media.pageArea();
        Insets m = media.margin();
        // La página física, en coordenadas del área útil.
        Rect page = new Rect(-m.left(), -m.top(), media.width(), media.height());
        for (Page p : pages) {
            content(p.content(), 0, 0, area.width(), area.height(), true, page);
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
            if (x < page.x() - TOLERANCE || y < page.y() - TOLERANCE
                    || x + f.width() > page.right() + TOLERANCE || y + f.height() > page.bottom() + TOLERANCE) {
                Style s = f.box().style;
                throw new InvalidModelException(s.isDeclared(OFFSET) ? s.line(OFFSET) : f.box().line,
                        ErrorCode.BOX_OVERFLOW, "La caja " + name(f.box()) + " se sale de la página: revisa su "
                        + "Offset y su tamaño");
            }
        }
        double cx = x + f.border().left() + f.padding().left();
        double cy = y + f.border().top() + f.padding().top();
        content(f, cx, cy, f.contentWidth(), f.contentHeight(), false, page);
    }

    /**
     * Las líneas y los hijos de un fragmento, cuya caja de contenido empieza en ({@code cx},
     * {@code cy}) y mide {@code width} × {@code height}; {@code isPage}: es el flujo de una página.
     */
    private static void content(Fragment f, double cx, double cy, double width, double height, boolean isPage,
                                Rect page) {
        for (TextLine l : f.lines()) {
            if (l.x() < -TOLERANCE || l.x() + l.width() > width + TOLERANCE) {
                throw textOverflow(f.box(), l, width);
            }
            if (l.y() + l.height() > height + TOLERANCE) {
                throw tooTall(f, l.y() + l.height(), height, null);
            }
        }
        for (Fragment c : f.children()) {
            if (c.box().isMarker()) {
                // Cuelga fuera del ítem: se comprueba en la sangría de su lista (marker).
                fragment(c, cx, cy, page);
                continue;
            }
            if (f.box().element == Element.LIST) {
                marker(f, c);
            }
            if (!c.box().style.isOutOfFlow()) {
                // Las relative se comprueban en su sitio, sin el desplazamiento.
                double x = c.x() - c.offsetX();
                double y = c.y() - c.offsetY();
                Insets margin = c.margin();
                if (x - margin.left() < -TOLERANCE || x + c.width() + margin.right() > width + TOLERANCE) {
                    throw tooWide(c, cx + x, cy + y, cx, cx + width);
                }
                if (y + c.height() > height + TOLERANCE) {
                    if (isPage) {
                        throw pageOverflow(c, cx + x, cy + y, cy + height, height);
                    }
                    throw tooTall(f, y + c.height(), height, c);
                }
            }
            fragment(c, cx, cy, page);
        }
    }

    /**
     * El marcador de un ítem cabe en su sangría (§5.14): el relleno izquierdo de la lista más el
     * margen izquierdo del ítem.
     */
    private static void marker(Fragment list, Fragment item) {
        for (Fragment m : item.children()) {
            if (!m.box().isMarker()) {
                continue;
            }
            double room = item.x() + list.padding().left();
            if (m.width() > room + TOLERANCE) {
                throw new InvalidModelException(item.box().line, ErrorCode.LIST_MARKER_OVERFLOW, "El marcador '"
                        + m.box().text.strip() + "' del List item " + name(item.box()) + " mide " + pt(m.width())
                        + " con su espacio, y la sangría deja " + pt(room) + ": aumenta el Padding izquierdo de "
                        + "la lista o el margen izquierdo del ítem");
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Culpables
    // ------------------------------------------------------------------------------------------

    /**
     * Un hijo se sale por los lados: el culpable es lo más interior que se sale. Si la caja
     * declara su ancho ({@code Width} o {@code Min width}), sus hijos que solo la llenan no son
     * culpables: solo se baja a un hijo que se sale también de ella.
     */
    private static InvalidModelException tooWide(Fragment c, double x, double y, double left, double right) {
        while (true) {
            double cx = x + c.border().left() + c.padding().left();
            double cy = y + c.border().top() + c.padding().top();
            // Una palabra que no cabe en su propia caja es la culpable; si cabe, lo es la caja.
            for (TextLine l : c.lines()) {
                if (l.x() < -TOLERANCE || l.x() + l.width() > c.contentWidth() + TOLERANCE) {
                    throw textOverflow(c.box(), l, c.contentWidth());
                }
            }
            Style s = c.box().style;
            boolean sized = s.isDeclared(WIDTH) || s.isDeclared(MIN_WIDTH);
            Fragment inner = null;
            for (Fragment d : c.children()) {
                if (d.box().style.isOutOfFlow() || d.box().isMarker()) {
                    continue;
                }
                double dx = cx + d.x() - d.offsetX();
                boolean outside = dx < left - TOLERANCE || dx + d.width() > right + TOLERANCE;
                boolean outsideParent = dx - d.margin().left() < cx - TOLERANCE
                        || dx + d.width() + d.margin().right() > cx + c.contentWidth() + TOLERANCE;
                if (outside && (!sized || outsideParent)) {
                    inner = d;
                    x = dx;
                    y = cy + d.y() - d.offsetY();
                    break;
                }
            }
            if (inner == null) {
                String property = s.isDeclared(WIDTH) ? WIDTH : s.isDeclared(MIN_WIDTH) ? MIN_WIDTH : null;
                return new InvalidModelException(property != null ? s.line(property) : c.box().line,
                        ErrorCode.BOX_OVERFLOW, "La caja " + name(c.box()) + " no cabe a lo ancho: mide "
                        + pt(c.width()) + " y el contenido de su contenedor mide " + pt(right - left)
                        + "; reduce su ancho o el de su contenido");
            }
            c = inner;
        }
    }

    /**
     * El contenido de una caja pasa de su alto: el culpable es lo que lo fija, por este orden, su
     * {@code Height}, su {@code Max height}, su {@code Aspect ratio} o las filas de un grid
     * ({@code Rows}; si fueran {@code auto}, crecerían con su contenido).
     */
    private static InvalidModelException tooTall(Fragment container, double needed, double height, Fragment child) {
        Box box = container.box();
        Style s = box.style;
        String property = Stream.of(HEIGHT, MAX_HEIGHT, ASPECT_RATIO, GRID_ROWS)
                .filter(s::isDeclared).findFirst().orElse(null);
        String what = child == null ? "su texto" : "la caja " + name(child.box());
        return new InvalidModelException(property != null ? s.line(property) : box.line, ErrorCode.BOX_OVERFLOW,
                "El contenido de la caja " + name(box) + " no cabe en su alto: " + what + " llega a "
                        + pt(needed) + " y la caja deja " + pt(height));
    }

    /**
     * Algo del flujo no cabe en la página y no se ha podido partir: el culpable es lo más interior
     * que pasa del final, y en él, lo que no se puede partir: su borde y su relleno si ya no caben
     * en la página ({@code pageHeight}), su {@code Line height} (o su {@code Size}) si tiene texto,
     * o su {@code Height} (en una imagen sin él, su {@code Width}).
     */
    private static InvalidModelException pageOverflow(Fragment c, double x, double y, double bottom,
                                                      double pageHeight) {
        while (true) {
            double cx = x + c.border().left() + c.padding().left();
            double cy = y + c.border().top() + c.padding().top();
            Fragment inner = null;
            for (Fragment d : c.children()) {
                double dy = cy + d.y() - d.offsetY();
                if (!d.box().style.isOutOfFlow() && !d.box().isMarker() && dy + d.height() > bottom + TOLERANCE) {
                    inner = d;
                    x = cx + d.x() - d.offsetX();
                    y = dy;
                    break;
                }
            }
            if (inner == null) {
                Style s = c.box().style;
                String property;
                if (c.border().vertical() + c.padding().vertical() > pageHeight + TOLERANCE) {
                    property = c.padding().vertical() >= c.border().vertical() ? PADDING : BORDER;
                } else if (!c.lines().isEmpty()) {
                    property = s.isDeclared(LINE_HEIGHT) ? LINE_HEIGHT : FONT_SIZE;
                } else if (c.box().isImage() && !s.isDeclared(HEIGHT)) {
                    // El alto de una imagen sale de su ancho.
                    property = WIDTH;
                } else {
                    property = HEIGHT;
                }
                int line = s.isDeclared(property) ? s.line(property) : c.box().line;
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
        // No está en el texto si la ha cambiado el layout, como un número de página.
        int index = box.text.indexOf(word);
        int line = index >= 0 ? box.textLineAt(index) : box.textLine;
        return new InvalidModelException(line, ErrorCode.TEXT_OVERFLOW, "La palabra '" + word + "' no cabe en "
                + "la línea: la línea mide " + pt(l.width()) + " y la caja deja " + pt(width)
                + "; ensancha la caja o reduce el Size del texto");
    }
}
