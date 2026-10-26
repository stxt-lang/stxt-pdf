package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakRules;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.tree.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmentación de los contenedores cuyos ítems se agrupan en <em>unidades</em>
 * (ESPECIFICACION.md §5.10): las líneas de un contenedor flex en {@code row}, los ítems de uno en
 * {@code column} y las filas de una rejilla.
 *
 * <p>El contenedor hace su layout completo sin fragmentar y después {@link #fragment} se queda
 * con las unidades que caben en la página ({@link BreakRules#cutUnits}), desplazadas para que el
 * fragmento empiece en la primera. Una unidad que no cabe en lo que queda de página pasa entera
 * a la siguiente.</p>
 *
 * <p>Si una unidad tampoco cabe al principio de una página, sus ítems se parten <strong>en
 * paralelo</strong>: cada uno se maqueta con fragmentación desde su posición, y la unidad
 * continúa en la página siguiente con los ítems que no han terminado, todos desde arriba y sin
 * su margen superior; mide lo que el más largo de ellos y las unidades siguientes van detrás.
 * Las unidades entre las que no se puede cortar (las filas que ocupa un mismo ítem) se parten
 * juntas, como un <em>grupo</em>. La calidad del corte es la peor de los ítems que se parten.</p>
 */
public final class UnitBreaker {

    private static final double EPSILON = 1e-9;

    private UnitBreaker() {
    }

    /**
     * Un ítem ya colocado en el layout sin fragmentar.
     *
     * @param box    el ítem
     * @param result su layout
     * @param space  espacio de ese layout, para repetirlo con fragmentación si el ítem se parte
     * @param x      posición de su caja de borde, relativa a la caja de contenido del contenedor
     *               (incluye el desplazamiento de {@code relative})
     * @param y      ídem
     * @param unit   unidad a la que pertenece
     */
    public record Placed(Box box, LayoutResult result, ConstraintSpace space, double x, double y, int unit) {
    }

    /**
     * Las unidades del contenedor, con las posiciones del layout sin fragmentar.
     *
     * @param starts     principio de cada unidad, desde la caja de contenido
     * @param ends       final de cada unidad
     * @param appeal     calidad del corte delante de cada unidad; negativa si no se puede cortar ahí
     * @param forced     hay un corte forzado delante de cada unidad
     * @param extent     alto del contenido completo
     * @param splittable los ítems de una unidad que no cabe en una página se pueden partir en
     *                   paralelo
     */
    public record Units(double[] starts, double[] ends, int[] appeal, boolean[] forced, double extent,
                        boolean splittable) {
    }

    /**
     * Fragmento del contenedor en lo que queda de página, o el contenedor entero sin
     * fragmentación.
     *
     * @param from      token del contenedor; {@code null} en su primer fragmento
     * @param placed    ítems colocados, en orden del árbol
     * @param outOfFlow cajas {@code absolute} y {@code fixed} del contenedor; van con el primer fragmento
     */
    public static LayoutResult fragment(Engine engine, Box box, BoxModel model, ConstraintSpace space,
                                        BreakToken from, List<Placed> placed, List<PendingAbsolute> outOfFlow,
                                        Units units) {
        return new Run(engine, box, model, space, (BreakToken.Container) from, placed, units).layOut(outOfFlow);
    }

    /** Un ítem en este fragmento: su fragmento ya colocado y sus cajas absolutas. */
    private record Piece(Fragment fragment, List<PendingAbsolute> pending) {
    }

    /**
     * Los ítems de un grupo partidos en paralelo.
     *
     * @param pieces índice en {@code placed} → ítem colocado en esta página
     * @param tokens token de cada ítem del grupo, en orden; {@code null} si ha terminado
     * @param bottom final del más largo, desde la caja de contenido del fragmento
     * @param broken algún ítem continúa en la página siguiente
     */
    private record Split(Piece[] pieces, List<BreakToken> tokens, int appeal, double bottom, boolean broken) {
    }

    /** Un fragmento de un contenedor. */
    private static final class Run {
        final Engine engine;
        final Box box;
        final BoxModel model;
        final ConstraintSpace space;
        final BreakToken.Container from;
        final boolean first;
        final List<Placed> placed;
        final Units units;
        final int n;
        /** Lo que queda de página para el contenido; sin fragmentación no hay límite. */
        final double remaining;
        /** Ítem de cada índice de {@code placed} colocado en este fragmento. */
        final Piece[] pieces;

        Run(Engine engine, Box box, BoxModel model, ConstraintSpace space, BreakToken.Container from,
            List<Placed> placed, Units units) {
            this.engine = engine;
            this.box = box;
            this.model = model;
            this.space = space;
            this.from = from;
            this.first = from == null;
            this.placed = placed;
            this.units = units;
            this.n = units.starts().length;
            this.remaining = space.fragmenting()
                    ? Slice.contentRemaining(model, first, space) : Double.POSITIVE_INFINITY;
            this.pieces = new Piece[placed.size()];
        }

        LayoutResult layOut(List<PendingAbsolute> outOfFlow) {
            int next = first ? 0 : from.nextLine();
            // Posición en el fragmento = posición sin fragmentar − origin. El primer fragmento
            // conserva el hueco inicial (alineación); los siguientes empiezan en su primera unidad
            // o, si solo queda el alto propio de la caja, al final del contenido.
            double origin = first ? 0 : next < n ? units.starts()[next] : units.extent();
            double top = 0;
            boolean continued = false;
            if (!first && !from.itemTokens().isEmpty()) {
                // El grupo partido en la página anterior continúa desde arriba.
                int end = groupEnd(next);
                Split s = split(next, end, from.itemTokens(), 0);
                if (s == null) {
                    return before();
                }
                commit(s);
                if (s.broken()) {
                    return result(s.bottom(), false, next, s.tokens(), s.appeal(), outOfFlow);
                }
                // Las unidades siguientes, detrás del ítem más largo.
                origin = bottom(next, end) - s.bottom();
                top = s.bottom();
                next = end;
                continued = true;
            }

            int end = n;
            int appeal = LayoutResult.PERFECT_BREAK;
            if (space.fragmenting() && next < n) {
                boolean mustPlace = space.atFragmentainerStart() && !continued;
                int groupEnd = groupEnd(next);
                if (mustPlace && units.splittable() && bottom(next, groupEnd) - origin > remaining + EPSILON) {
                    // No cabe ni al principio de página: sus ítems se parten en paralelo.
                    Split s = split(next, groupEnd, null, origin);
                    if (s != null && s.broken()) {
                        commit(s);
                        return result(s.bottom(), false, next, s.tokens(), s.appeal(), outOfFlow);
                    }
                }
                BreakRules.Cut cut = BreakRules.cutUnits(units.starts(), units.ends(), next,
                        remaining - (units.starts()[next] - origin), units.appeal(), units.forced(), mustPlace);
                if (cut.end() == next) {
                    if (!continued) {
                        return before();
                    }
                    // Solo cabe la continuación del grupo: se corta detrás de ella.
                    return result(top, false, next, List.of(), units.appeal()[next], outOfFlow);
                }
                end = cut.end();
                appeal = cut.appeal();
            }
            for (int k = 0; k < placed.size(); k++) {
                Placed p = placed.get(k);
                if (p.unit() >= next && p.unit() < end) {
                    pieces[k] = piece(p, p.result(), p.y() - origin);
                }
            }
            boolean contentFinished = end == n;
            double contentHeight = contentFinished
                    ? Math.max(top, units.extent() - origin)
                    : bottom(next, end) - origin;
            return result(contentHeight, contentFinished, end, List.of(), appeal, outOfFlow);
        }

        /**
         * Parte en paralelo los ítems de las unidades {@code [start, end)}; {@code null} si alguno
         * no puede colocar nada.
         *
         * @param tokens token de cada ítem para reanudarlos desde arriba; {@code null} si el grupo
         *               empieza en esta página, cada ítem en su posición
         * @param origin ver {@link #layOut}; solo si el grupo empieza en esta página
         */
        Split split(int start, int end, List<BreakToken> tokens, double origin) {
            Piece[] split = new Piece[placed.size()];
            List<BreakToken> next = new ArrayList<>();
            int appeal = LayoutResult.PERFECT_BREAK;
            double bottom = 0;
            boolean broken = false;
            int j = 0;
            for (int k = 0; k < placed.size(); k++) {
                Placed p = placed.get(k);
                if (p.unit() < start || p.unit() >= end) {
                    continue;
                }
                BreakToken token = tokens == null ? null : tokens.get(j);
                j++;
                if (tokens != null && token == null) {
                    // Terminó en una página anterior.
                    next.add(null);
                    continue;
                }
                double y = tokens == null ? p.y() - origin : 0;
                LayoutResult r = engine.layout(p.box(),
                        p.space().fragmented(remaining - y, space.atFragmentainerStart()), token);
                if (r.breakToken() instanceof BreakToken.Before) {
                    return null;
                }
                split[k] = piece(p, r, y);
                next.add(r.breakToken());
                if (!r.isFinished()) {
                    broken = true;
                    appeal = Math.min(appeal, BreakRules.inside(p.box(), r.breakAppeal()));
                }
                bottom = Math.max(bottom, y + r.fragment().height());
            }
            return new Split(split, next, appeal, bottom, broken);
        }

        void commit(Split s) {
            for (int k = 0; k < pieces.length; k++) {
                if (s.pieces()[k] != null) {
                    pieces[k] = s.pieces()[k];
                }
            }
        }

        /** Coloca el fragmento de un ítem en {@code y} del fragmento del contenedor. */
        Piece piece(Placed p, LayoutResult r, double y) {
            Fragment f = r.fragment();
            p.box().x = p.x();
            p.box().y = y;
            double dx = p.x() + f.border().left() + f.padding().left();
            double dy = y + f.border().top() + f.padding().top();
            return new Piece(f.moved(p.x(), y), r.pending().stream().map(q -> q.translated(dx, dy)).toList());
        }

        /** Primera unidad detrás del grupo que empieza en {@code start}: la siguiente delante de la que se puede cortar. */
        int groupEnd(int start) {
            int end = start + 1;
            while (end < n && units.appeal()[end] < 0) {
                end++;
            }
            return end;
        }

        /** Final de la unidad más baja de {@code [start, end)} en el layout sin fragmentar. */
        double bottom(int start, int end) {
            double bottom = units.starts()[start];
            for (int u = start; u < end; u++) {
                bottom = Math.max(bottom, units.ends()[u]);
            }
            return bottom;
        }

        /** Nada del contenedor cabe en lo que queda de página: el padre debe cortar antes de él. */
        LayoutResult before() {
            return new LayoutResult(Fragment.empty(box), BreakToken.BEFORE, LayoutResult.PERFECT_BREAK,
                    List.of(), AdjoiningMargins.NONE);
        }

        /**
         * El fragmento con los ítems colocados.
         *
         * @param nextLine   unidad por la que sigue el contenedor si no termina
         * @param itemTokens tokens de los ítems de un grupo partido en paralelo, o vacía
         */
        LayoutResult result(double contentHeight, boolean contentFinished, int nextLine, List<BreakToken> itemTokens,
                            int appeal, List<PendingAbsolute> outOfFlow) {
            List<Fragment> fragments = new ArrayList<>();
            List<PendingAbsolute> pending = new ArrayList<>(first ? outOfFlow : List.of());
            for (Piece piece : pieces) {
                if (piece != null) {
                    fragments.add(piece.fragment());
                    pending.addAll(piece.pending());
                }
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
            BreakToken token = new BreakToken.Container(nextLine, itemTokens, slice.consumed());
            return new LayoutResult(fragment, token, contentFinished ? LayoutResult.PERFECT_BREAK : appeal,
                    pending, AdjoiningMargins.NONE);
        }
    }
}
