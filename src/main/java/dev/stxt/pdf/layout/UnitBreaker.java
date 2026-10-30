package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakRules;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Unit;
import dev.stxt.pdf.layout.position.FrameOffset;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Box;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Fragmentación de los contenedores cuyos ítems se agrupan en <em>unidades</em>
 * (referencia/comun/cortes.md): las líneas de un contenedor flex en {@code row}, los ítems de uno en
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
 *
 * <p>Una tabla repite su cabecera y su pie en cada fragmento ({@link Repeat}): van fuera de
 * las unidades, enteros, y las unidades se cortan en lo que dejan. Las filas de una tabla con aspecto o
 * {@code Id} son franjas ({@link Band}) que siguen a su unidad: entera, recortada al final de la página si su grupo
 * se parte, y en la continuación del grupo, la de su última unidad.</p>
 */
public final class UnitBreaker {

    private UnitBreaker() {
    }

    /**
     * Un ítem ya colocado en el layout sin fragmentar.
     *
     * @param box      el ítem
     * @param result   su layout
     * @param space    espacio de ese layout, para repetirlo con fragmentación si el ítem se parte
     * @param x        posición de su caja de borde, relativa a la caja de contenido del contenedor
     *                 (incluye el desplazamiento de {@code relative})
     * @param y        ídem
     * @param unit     unidad a la que pertenece
     * @param relative desplazamiento de {@code relative} incluido en {@code x} e {@code y}
     */
    public record Placed(Box box, LayoutResult result, ConstraintSpace space, double x, double y, int unit,
                         FrameOffset relative) {
    }

    /**
     * Las unidades del contenedor, con las posiciones del layout sin fragmentar.
     *
     * @param list       las unidades, en orden
     * @param extent     alto del contenido completo
     * @param splittable los ítems de una unidad que no cabe en una página se pueden partir en
     *                   paralelo
     */
    public record Units(List<Unit> list, double extent, boolean splittable) {

        public Units {
            list = List.copyOf(list);
        }

        public int size() {
            return list.size();
        }

        public Unit get(int index) {
            return list.get(index);
        }

        /**
         * Las unidades {@code [from, to)} con las posiciones desde {@code origin}; el contenido
         * acaba al final de la última. La tabla lo usa para cortar solo su cuerpo.
         */
        public Units slice(int from, int to, double origin) {
            List<Unit> slice = list.subList(from, to).stream().map(u -> u.shifted(origin)).toList();
            return new Units(slice, list.get(to - 1).end() - origin, splittable);
        }
    }

    /**
     * Un contenedor maquetado sin fragmentar, antes de cortarlo: lo producen flex y grid.
     *
     * @param model     modelo de caja del contenedor
     * @param placed    ítems colocados, en orden del árbol
     * @param outOfFlow cajas {@code absolute} y {@code fixed} del contenedor; van con el primer fragmento
     * @param units     las unidades en que se agrupan los ítems
     */
    public record Arranged(BoxModel model, List<Placed> placed, List<PendingOverlay> outOfFlow, Units units,
                           List<Band> bands) {

        public Arranged {
            bands = List.copyOf(bands);
        }

        /** Un contenedor sin franjas: los de flex y grid. */
        public Arranged(BoxModel model, List<Placed> placed, List<PendingOverlay> outOfFlow, Units units) {
            this(model, placed, outOfFlow, units, List.of());
        }
    }

    /**
     * La franja de una unidad: la fila de una tabla con {@code Id} o {@code Box}
     * (referencia/componentes/table.md#filas), que se pinta debajo de sus celdas con el alto de la unidad en cada
     * fragmento.
     *
     * @param box    la caja de la fila
     * @param unit   su unidad
     * @param x      su borde izquierdo, desde la caja de contenido del contenedor: el de la primera columna
     * @param width  su ancho: de la primera columna a la última
     * @param border su borde
     */
    public record Band(Box box, int unit, double x, double width, Insets border) {

        /**
         * El fragmento de la franja en {@code y}, con {@code height} de alto. Partida, con {@code Edges: open} (la
         * de su {@code Break}), el primer trozo no lleva borde inferior y los siguientes no llevan superior.
         */
        public Fragment fragment(double y, double height, boolean first, boolean last) {
            boolean clone = box.style().breaks().edges() == Edges.CLOSED;
            Insets sliced = clone ? border : new Insets(first ? border.top() : 0, border.right(),
                    last ? border.bottom() : 0, border.left());
            return new Fragment(box, x, y, width, height, Insets.ZERO, sliced, Insets.ZERO, List.of(), List.of(),
                    first, last);
        }

        /** La misma franja en otra unidad. */
        public Band inUnit(int unit) {
            return new Band(box, unit, x, width, border);
        }
    }

    /**
     * Ítems que se repiten enteros en cada fragmento, fuera de las unidades: la cabecera de una
     * tabla arriba y su pie debajo de las unidades colocadas (referencia/componentes/table.md). Nunca se
     * parten, y lo que queda de página para las unidades es el resto.
     *
     * @param head       ítems de la cabecera, con su posición desde la caja de contenido
     * @param headHeight alto de la cabecera, con el hueco que la separa de las unidades
     * @param foot       ítems del pie, con su posición desde el final de las unidades
     * @param footHeight alto del pie, con el hueco que lo separa de las unidades
     * @param headBands  franjas de las filas de la cabecera, colocadas como sus ítems
     * @param footBands  franjas de las filas del pie, colocadas como sus ítems
     */
    public record Repeat(List<Placed> head, double headHeight, List<Placed> foot, double footHeight,
                         List<Fragment> headBands, List<Fragment> footBands) {
        public static final Repeat NONE = new Repeat(List.of(), 0, List.of(), 0);

        /** Sin franjas. */
        public Repeat(List<Placed> head, double headHeight, List<Placed> foot, double footHeight) {
            this(head, headHeight, foot, footHeight, List.of(), List.of());
        }
    }

    /**
     * Fragmento del contenedor en lo que queda de página, o el contenedor entero sin
     * fragmentación.
     *
     * @param from     token del contenedor; {@code null} en su primer fragmento
     * @param arranged el contenedor maquetado sin fragmentar
     */
    public static LayoutResult fragment(LayoutContext context, Box box, ConstraintSpace space, BreakToken from,
                                        Arranged arranged) {
        return fragment(context, box, space, from, arranged, Repeat.NONE);
    }

    /** Como {@link #fragment(LayoutContext, Box, ConstraintSpace, BreakToken, Arranged)}, con ítems que se repiten en cada fragmento. */
    public static LayoutResult fragment(LayoutContext context, Box box, ConstraintSpace space, BreakToken from,
                                        Arranged arranged, Repeat repeat) {
        return new Run(context, box, space, (BreakToken.Container) from, arranged, repeat).layOut();
    }

    /** Un ítem en este fragmento: su fragmento ya colocado y sus cajas absolutas. */
    private record Piece(Fragment fragment, List<PendingOverlay> pending) {
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
        final LayoutContext context;
        final Box box;
        final BoxModel model;
        final ConstraintSpace space;
        final BreakToken.Container from;
        final boolean first;
        final List<Placed> placed;
        final List<PendingOverlay> outOfFlow;
        final Units units;
        final List<Band> bands;
        final Repeat repeat;
        final int n;
        /** Lo que queda de página para las unidades; sin fragmentación no hay límite. */
        final double remaining;
        /** Ítem de cada índice de {@code placed} colocado en este fragmento. */
        final Piece[] pieces;
        /** Fragmento de cada franja de {@code bands} en este fragmento. */
        final Fragment[] bandPieces;
        /** Primera unidad que queda por colocar. */
        int next;
        /**
         * Posición en el fragmento = posición sin fragmentar − origin. El primer fragmento conserva
         * el hueco inicial (alineación); los siguientes empiezan en su primera unidad o, si solo
         * queda el alto propio de la caja, al final del contenido.
         */
        double origin;
        /** Final de la continuación del grupo partido en la página anterior; 0 si no la hay. */
        double top;
        /** El fragmento empieza con la continuación de un grupo partido. */
        boolean continued;

        Run(LayoutContext context, Box box, ConstraintSpace space, BreakToken.Container from, Arranged arranged,
            Repeat repeat) {
            this.context = context;
            this.box = box;
            this.model = arranged.model();
            this.space = space;
            this.from = from;
            this.first = from == null;
            this.placed = arranged.placed();
            this.outOfFlow = arranged.outOfFlow();
            this.units = arranged.units();
            this.bands = arranged.bands();
            this.repeat = repeat;
            this.n = units.size();
            this.remaining = space.fragmenting()
                    ? Slice.contentRemaining(model, first, space) - repeat.headHeight() - repeat.footHeight()
                    : Double.POSITIVE_INFINITY;
            this.pieces = new Piece[placed.size()];
            this.bandPieces = new Fragment[bands.size()];
        }

        LayoutResult layOut() {
            next = first ? 0 : from.nextLine();
            origin = first ? 0 : next < n ? units.get(next).start() : units.extent();
            if (!first && !from.itemTokens().isEmpty()) {
                LayoutResult r = continueGroup();
                if (r != null) {
                    return r;
                }
            }
            if (!space.fragmenting() || next == n) {
                return place(n, BreakAppeal.PERFECT_BREAK);
            }
            return cut();
        }

        /**
         * El grupo partido en la página anterior continúa desde arriba, y las unidades siguientes
         * van detrás del ítem más largo. El resultado si el fragmento acaba con el grupo (o no cabe
         * nada); {@code null} si sigue con las unidades siguientes.
         */
        LayoutResult continueGroup() {
            int end = groupEnd(next);
            Split s = split(next, end, from.itemTokens(), 0);
            if (s == null) {
                return LayoutResult.before(box);
            }
            commit(s);
            // La continuación del grupo lleva la franja de su última unidad.
            for (int b = 0; b < bands.size(); b++) {
                if (bands.get(b).unit() == end - 1) {
                    bandPieces[b] = bands.get(b).fragment(repeat.headHeight(), s.bottom(), false, !s.broken());
                }
            }
            if (s.broken()) {
                return result(s.bottom(), false, next, s.tokens(), s.appeal());
            }
            origin = bottom(next, end) - s.bottom();
            top = s.bottom();
            next = end;
            continued = true;
            return null;
        }

        /**
         * Las unidades desde {@code next} que caben en lo que queda de página. Si el primer grupo
         * no cabe ni al principio de página, sus ítems se parten en paralelo.
         */
        LayoutResult cut() {
            boolean mustPlace = space.atFragmentainerStart() && !continued;
            int groupEnd = groupEnd(next);
            if (mustPlace && units.splittable() && !fits(bottom(next, groupEnd) - origin, remaining)) {
                Split s = split(next, groupEnd, null, origin);
                if (s != null && s.broken()) {
                    commit(s);
                    splitBands(next, groupEnd, s.bottom());
                    return result(s.bottom(), false, next, s.tokens(), s.appeal());
                }
            }
            BreakRules.Cut cut = BreakRules.cutUnits(units.list(), next,
                    remaining - (units.get(next).start() - origin), mustPlace);
            if (cut.end() == next) {
                // Solo cabe la continuación del grupo, si la hay: se corta detrás de ella.
                return continued
                        ? result(top, false, next, List.of(), units.get(next).appealBefore())
                        : LayoutResult.before(box);
            }
            return place(cut.end(), cut.appeal());
        }

        /** Coloca enteras las unidades {@code [next, end)} y cierra el fragmento. */
        LayoutResult place(int end, int appeal) {
            for (int k = 0; k < placed.size(); k++) {
                Placed p = placed.get(k);
                if (p.unit() >= next && p.unit() < end) {
                    pieces[k] = piece(p, p.result(), p.y() - origin + repeat.headHeight());
                }
            }
            for (int b = 0; b < bands.size(); b++) {
                int u = bands.get(b).unit();
                if (u >= next && u < end) {
                    Unit unit = units.get(u);
                    bandPieces[b] = bands.get(b).fragment(unit.start() - origin + repeat.headHeight(),
                            unit.end() - unit.start(), true, true);
                }
            }
            boolean contentFinished = end == n;
            double contentHeight = contentFinished
                    ? Math.max(top, units.extent() - origin)
                    : bottom(next, end) - origin;
            return result(contentHeight, contentFinished, end, List.of(), appeal);
        }

        /**
         * Parte en paralelo los ítems de las unidades {@code [start, end)}; {@code null} si alguno
         * no puede colocar nada.
         *
         * @param tokens token de cada ítem para reanudarlos desde arriba; {@code null} si el grupo
         *               empieza en esta página, cada ítem en su posición
         * @param origin ver {@link #origin}; solo si el grupo empieza en esta página
         */
        Split split(int start, int end, List<BreakToken> tokens, double origin) {
            Piece[] split = new Piece[placed.size()];
            List<BreakToken> next = new ArrayList<>();
            int appeal = BreakAppeal.PERFECT_BREAK;
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
                LayoutResult r = context.layout(p.box(),
                        p.space().fragmented(remaining - y, space.atFragmentainerStart()), token);
                if (r.breakToken() instanceof BreakToken.Before) {
                    return null;
                }
                split[k] = piece(p, r, y + repeat.headHeight());
                next.add(r.breakToken());
                if (!r.isFinished()) {
                    broken = true;
                    appeal = Math.min(appeal, BreakRules.inside(p.box(), r.breakAppeal()));
                }
                bottom = Math.max(bottom, y + r.fragment().height());
            }
            return new Split(split, next, appeal, bottom, broken);
        }

        /**
         * Las franjas de las unidades {@code [start, end)} de un grupo que se parte en esta página y acaba en {@code
         * bottom}: cada una desde su principio, recortada en {@code bottom}.
         */
        void splitBands(int start, int end, double bottom) {
            for (int b = 0; b < bands.size(); b++) {
                int u = bands.get(b).unit();
                if (u < start || u >= end) {
                    continue;
                }
                double top = units.get(u).start() - origin;
                double unitBottom = units.get(u).end() - origin;
                if (fits(bottom, top)) {
                    continue;
                }
                bandPieces[b] = bands.get(b).fragment(top + repeat.headHeight(), Math.min(unitBottom, bottom) - top,
                        true, fits(unitBottom, bottom));
            }
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
            double dx = p.x() + f.border().left() + f.padding().left();
            double dy = y + f.border().top() + f.padding().top();
            return new Piece(f.moved(p.x(), y, p.relative().dx(), p.relative().dy()), r.pending().stream().map(q -> q.translated(dx, dy)).toList());
        }

        /**
         * Un ítem que se repite: su fragmento va en todos, pero sus cajas absolutas solo en el
         * primero, como las del contenedor.
         */
        void repeated(Piece piece, List<Fragment> fragments, List<PendingOverlay> pending) {
            fragments.add(piece.fragment());
            if (first) {
                pending.addAll(piece.pending());
            }
        }

        /** Primera unidad detrás del grupo que empieza en {@code start}: la siguiente delante de la que se puede cortar. */
        int groupEnd(int start) {
            int end = start + 1;
            while (end < n && !units.get(end).breakableBefore()) {
                end++;
            }
            return end;
        }

        /** Final de la unidad más baja de {@code [start, end)} en el layout sin fragmentar. */
        double bottom(int start, int end) {
            double bottom = units.get(start).start();
            for (int u = start; u < end; u++) {
                bottom = Math.max(bottom, units.get(u).end());
            }
            return bottom;
        }

        /**
         * El fragmento con los ítems colocados.
         *
         * @param nextLine   unidad por la que sigue el contenedor si no termina
         * @param itemTokens tokens de los ítems de un grupo partido en paralelo, o vacía
         */
        LayoutResult result(double contentHeight, boolean contentFinished, int nextLine, List<BreakToken> itemTokens,
                            int appeal) {
            List<Fragment> fragments = new ArrayList<>();
            List<PendingOverlay> pending = new ArrayList<>(first ? outOfFlow : List.of());
            double units = Math.max(0, contentHeight);
            // Las franjas de las filas, delante de las celdas de su grupo para pintarse debajo.
            fragments.addAll(repeat.headBands());
            for (Placed p : repeat.head()) {
                repeated(piece(p, p.result(), p.y()), fragments, pending);
            }
            for (Fragment band : bandPieces) {
                if (band != null) {
                    fragments.add(band);
                }
            }
            for (Piece piece : pieces) {
                if (piece != null) {
                    fragments.add(piece.fragment());
                    pending.addAll(piece.pending());
                }
            }
            for (Fragment band : repeat.footBands()) {
                fragments.add(band.moved(band.x(), repeat.headHeight() + units + band.y()));
            }
            for (Placed p : repeat.foot()) {
                repeated(piece(p, p.result(), repeat.headHeight() + units + p.y()), fragments, pending);
            }
            Slice slice = Slice.of(model, first, BreakToken.consumed(from),
                    repeat.headHeight() + units + repeat.footHeight(), contentFinished, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), fragments, List.of(), first, slice.finished());
            // Los márgenes de los ítems nunca colapsan con los del contenedor (referencia/componentes/block.md).
            if (slice.finished()) {
                return LayoutResult.finished(fragment, pending, AdjoiningMargins.NONE);
            }
            BreakToken token = new BreakToken.Container(nextLine, itemTokens, slice.consumed());
            return new LayoutResult(fragment, token, contentFinished ? BreakAppeal.PERFECT_BREAK : appeal,
                    pending, AdjoiningMargins.NONE);
        }
    }
}
