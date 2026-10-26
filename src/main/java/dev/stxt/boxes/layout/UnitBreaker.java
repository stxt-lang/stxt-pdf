package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakRules;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.tree.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmentación de los contenedores cuyos ítems se agrupan en <em>unidades</em> que no se
 * parten (ESPECIFICACION.md §5.10): las líneas de un contenedor flex en {@code row}, los ítems de
 * uno en {@code column} y las filas de una rejilla.
 *
 * <p>El contenedor hace su layout completo sin fragmentar y después {@link #fragment} se queda
 * con las unidades que caben en la página ({@link BreakRules#cutUnits}), desplazadas para que el
 * fragmento empiece en la primera. Una unidad más alta que la página se coloca al principio de
 * página y desborda.</p>
 */
public final class UnitBreaker {

    // TODO(hito 7): fragmentación en paralelo de los ítems de una unidad que no cabe en una página.

    private UnitBreaker() {
    }

    /**
     * Un ítem ya colocado en el layout sin fragmentar.
     *
     * @param box    el ítem
     * @param result su layout
     * @param x      posición de su caja de borde, relativa a la caja de contenido del contenedor
     *               (incluye el desplazamiento de {@code relative})
     * @param y      ídem
     * @param unit   unidad a la que pertenece
     */
    public record Placed(Box box, LayoutResult result, double x, double y, int unit) {
    }

    /**
     * Las unidades del contenedor, con las posiciones del layout sin fragmentar.
     *
     * @param starts principio de cada unidad, desde la caja de contenido
     * @param ends   final de cada unidad
     * @param appeal calidad del corte delante de cada unidad; negativa si no se puede cortar ahí
     * @param forced hay un corte forzado delante de cada unidad
     * @param extent alto del contenido completo
     */
    public record Units(double[] starts, double[] ends, int[] appeal, boolean[] forced, double extent) {
    }

    /**
     * Fragmento del contenedor en lo que queda de página, o el contenedor entero sin
     * fragmentación.
     *
     * @param from      token del contenedor; {@code null} en su primer fragmento
     * @param placed    ítems colocados, en orden del árbol
     * @param outOfFlow cajas {@code absolute} y {@code fixed} del contenedor; van con el primer fragmento
     */
    public static LayoutResult fragment(Box box, BoxModel model, ConstraintSpace space, BreakToken from,
                                        List<Placed> placed, List<PendingAbsolute> outOfFlow, Units units) {
        boolean first = from == null;
        int start = first ? 0 : ((BreakToken.Container) from).nextLine();
        int n = units.starts().length;
        int end = n;
        int appeal = LayoutResult.PERFECT_BREAK;
        // El primer fragmento conserva el hueco inicial (alineación); los siguientes empiezan en su
        // primera unidad o, si solo queda el alto propio de la caja, al final del contenido.
        double origin = first ? 0 : start < n ? units.starts()[start] : units.extent();
        if (space.fragmenting() && n > 0) {
            double remaining = space.fragmentainerRemaining().getAsDouble() - Slice.top(model, first);
            double fromStart = start < n ? remaining - (units.starts()[start] - origin) : remaining;
            BreakRules.Cut cut = BreakRules.cutUnits(units.starts(), units.ends(), start, fromStart,
                    units.appeal(), units.forced(), space.atFragmentainerStart());
            if (cut.end() == start && start < n) {
                return new LayoutResult(Fragment.empty(box), BreakToken.BEFORE, LayoutResult.PERFECT_BREAK,
                        List.of(), AdjoiningMargins.NONE);
            }
            end = cut.end();
            appeal = cut.appeal();
        }
        boolean contentFinished = end == n;
        double contentHeight = contentFinished ? units.extent() - origin : units.ends()[end - 1] - origin;

        List<Fragment> fragments = new ArrayList<>();
        List<PendingAbsolute> pending = new ArrayList<>(first ? outOfFlow : List.of());
        for (Placed p : placed) {
            if (p.unit() < start || p.unit() >= end) {
                continue;
            }
            Fragment f = p.result().fragment();
            double y = p.y() - origin;
            p.box().x = p.x();
            p.box().y = y;
            fragments.add(f.moved(p.x(), y));
            double dx = p.x() + f.border().left() + f.padding().left();
            double dy = y + f.border().top() + f.padding().top();
            p.result().pending().forEach(q -> pending.add(q.translated(dx, dy)));
        }

        Slice slice = Slice.of(model, first, BreakToken.consumed(from), Math.max(0, contentHeight),
                contentFinished, space);
        box.height = slice.height();
        Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), slice.border(),
                slice.padding(), fragments, List.of(), first, slice.finished());
        // Los márgenes de los ítems nunca colapsan con los del contenedor (§5.3).
        if (slice.finished()) {
            return LayoutResult.finished(fragment, pending, AdjoiningMargins.NONE);
        }
        BreakToken token = new BreakToken.Container(end, List.of(), slice.consumed());
        return new LayoutResult(fragment, token, contentFinished ? LayoutResult.PERFECT_BREAK : appeal,
                pending, AdjoiningMargins.NONE);
    }
}
