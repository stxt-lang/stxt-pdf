package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * El trozo de una caja que cae en un fragmentainer (referencia/comun/cortes.md). Una caja que se
 * parte entre páginas produce varios.
 *
 * @param box      caja de origen (sus propiedades de pintado se leen de su estilo)
 * @param x        posición de la caja de borde, relativa a la caja de contenido del padre; en una caja girada en
 *                 flujo, la del rectángulo que la envuelve ({@code turned})
 * @param y        ídem
 * @param width    ancho de la caja de borde; en una caja girada en flujo, el del rectángulo que la envuelve, que es lo
 *                 que ocupa
 * @param height   alto de la caja de borde <em>en este fragmento</em>; en una caja girada en flujo, el del rectángulo
 *                 que la envuelve
 * @param margin   márgenes usados de la caja en el layout que produjo el fragmento
 *                 ({@code auto} = 0, como en {@link dev.stxt.pdf.layout.BoxModel}): los
 *                 porcentajes dependen de la base de ese layout, que solo conoce el algoritmo
 * @param border   bordes pintados en este fragmento (con {@code Edges: open}, el
 *                 primero no tiene borde inferior y los siguientes no tienen superior)
 * @param padding  relleno de este fragmento, con el mismo criterio que {@code border}; la caja
 *                 de contenido del fragmento, a la que se refieren hijos y líneas, es la caja de
 *                 borde menos {@code border} y {@code padding}
 * @param children fragmentos hijos, en orden del árbol
 * @param lines    líneas de texto, si la caja contiene texto
 * @param first    es el primer fragmento de la caja
 * @param last     es el último fragmento de la caja
 * @param offsetX  desplazamiento de un {@code Frame} (referencia/componentes/frame-and-overlays.md), ya
 *                 sumado a {@code x}: su sitio en el flujo es {@code x - offsetX}. Lo pone el padre al colocarlo,
 *                 con la base de porcentajes de su layout; 0 en las demás cajas
 * @param offsetY  ídem en vertical
 * @param turned   en una caja girada en flujo (referencia/comun/capas-y-giro.md), su caja de borde sin girar, relativa
 *                 a ({@code x}, {@code y}) y centrada en el rectángulo que la envuelve; sus hijos y sus líneas se
 *                 refieren a ella. {@code null} en las demás cajas
 */
public record Fragment(Box box, double x, double y, double width, double height, Insets margin,
                       Insets border, Insets padding, List<Fragment> children, List<TextLine> lines,
                       boolean first, boolean last, double offsetX, double offsetY, Rect turned) {

    public Fragment {
        children = List.copyOf(children);
        lines = List.copyOf(lines);
    }

    /** Un fragmento que no es de una caja girada en flujo. */
    public Fragment(Box box, double x, double y, double width, double height, Insets margin, Insets border,
                    Insets padding, List<Fragment> children, List<TextLine> lines, boolean first, boolean last,
                    double offsetX, double offsetY) {
        this(box, x, y, width, height, margin, border, padding, children, lines, first, last, offsetX, offsetY,
                null);
    }

    /** Un fragmento sin desplazamiento de {@code Frame}: el que devuelve el layout de una caja. */
    public Fragment(Box box, double x, double y, double width, double height, Insets margin, Insets border,
                    Insets padding, List<Fragment> children, List<TextLine> lines, boolean first, boolean last) {
        this(box, x, y, width, height, margin, border, padding, children, lines, first, last, 0, 0);
    }

    /** Fragmento vacío de una caja que no cabe en lo que queda de página ({@link BreakToken.Before}). */
    public static Fragment empty(Box box) {
        return new Fragment(box, 0, 0, 0, 0, Insets.ZERO, Insets.ZERO, Insets.ZERO, List.of(), List.of(), false, false);
    }

    /**
     * La caja de borde de este fragmento si ({@code absX}, {@code absY}) es la posición del fragmento: el rectángulo
     * {@code x}, {@code y}, {@code width}, {@code height} o, en una caja girada en flujo, su caja sin girar.
     */
    public Rect borderBox(double absX, double absY) {
        return turned == null ? new Rect(absX, absY, width, height)
                : new Rect(absX + turned.x(), absY + turned.y(), turned.width(), turned.height());
    }

    /** Ancho de la caja de contenido de este fragmento: la de borde menos su borde y su relleno. */
    public double contentWidth() {
        return (turned == null ? width : turned.width()) - border.horizontal() - padding.horizontal();
    }

    /** Alto de la caja de contenido de este fragmento. */
    public double contentHeight() {
        return (turned == null ? height : turned.height()) - border.vertical() - padding.vertical();
    }

    /**
     * El fragmento de una caja girada en flujo: ocupa el rectángulo de {@code width} × {@code height} que envuelve su
     * caja de borde girada, que es {@code box} dentro de él.
     */
    public Fragment turned(Rect box, double width, double height) {
        return new Fragment(this.box, x, y, width, height, margin, border, padding, children, lines, first, last,
                offsetX, offsetY, box);
    }

    public Fragment moved(double newX, double newY) {
        return moved(newX, newY, offsetX, offsetY);
    }

    /**
     * El fragmento en ({@code newX}, {@code newY}), que ya incluye el desplazamiento de
     * {@code Frame} ({@code dx}, {@code dy}).
     */
    public Fragment moved(double newX, double newY, double dx, double dy) {
        return new Fragment(box, newX, newY, width, height, margin, border, padding, children, lines, first, last,
                dx, dy, turned);
    }

    /** El mismo fragmento con más hijos al final (las cajas absolutas que resuelve esta caja). */
    public Fragment withMoreChildren(List<Fragment> more) {
        List<Fragment> all = new ArrayList<>(children);
        all.addAll(more);
        return new Fragment(box, x, y, width, height, margin, border, padding, all, lines, first, last, offsetX,
                offsetY, turned);
    }
}
