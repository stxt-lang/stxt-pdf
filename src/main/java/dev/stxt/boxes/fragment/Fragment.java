package dev.stxt.boxes.fragment;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * El trozo de una caja que cae en un fragmentainer (ESPECIFICACION.md §5.10). En modo continuo
 * cada caja produce un único fragmento; en modo paginado puede producir varios.
 *
 * @param box      caja de origen (sus propiedades de pintado se leen de su estilo)
 * @param x        posición de la caja de borde, relativa a la caja de contenido del padre
 * @param y        ídem
 * @param width    ancho de la caja de borde
 * @param height   alto de la caja de borde <em>en este fragmento</em>
 * @param border   bordes pintados en este fragmento (con {@code Decoration: slice}, el
 *                 primero no tiene borde inferior y los siguientes no tienen superior)
 * @param padding  relleno de este fragmento, con el mismo criterio que {@code border}; la caja
 *                 de contenido del fragmento, a la que se refieren hijos y líneas, es la caja de
 *                 borde menos {@code border} y {@code padding}
 * @param children fragmentos hijos, en orden del árbol
 * @param lines    líneas de texto, si la caja contiene texto
 * @param first    es el primer fragmento de la caja
 * @param last     es el último fragmento de la caja
 */
public record Fragment(Box box, double x, double y, double width, double height, Insets border,
                       Insets padding, List<Fragment> children, List<TextLine> lines,
                       boolean first, boolean last) {

    public Fragment {
        children = List.copyOf(children);
        lines = List.copyOf(lines);
    }

    /** Fragmento único (modo continuo o caja que no se ha partido). */
    public static Fragment complete(Box box, double x, double y, double width, double height,
                                    List<Fragment> children, List<TextLine> lines) {
        return new Fragment(box, x, y, width, height, box.border, box.padding, children, lines, true, true);
    }

    /** Fragmento vacío de una caja que no cabe en lo que queda de página ({@link BreakToken.Before}). */
    public static Fragment empty(Box box) {
        return new Fragment(box, 0, 0, 0, 0, Insets.ZERO, Insets.ZERO, List.of(), List.of(), false, false);
    }

    public Fragment moved(double newX, double newY) {
        return new Fragment(box, newX, newY, width, height, border, padding, children, lines, first, last);
    }

    /** El mismo fragmento con más hijos al final (las cajas absolutas que resuelve esta caja). */
    public Fragment withMoreChildren(List<Fragment> more) {
        List<Fragment> all = new ArrayList<>(children);
        all.addAll(more);
        return new Fragment(box, x, y, width, height, border, padding, all, lines, first, last);
    }
}
