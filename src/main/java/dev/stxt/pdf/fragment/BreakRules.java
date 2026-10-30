package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.tree.Box;

import java.util.List;
import java.util.function.ToIntFunction;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Reglas de corte (referencia/comun/cortes.md): viudas y huérfanas, calidad de los cortes entre
 * cajas, cortes forzados y elección del corte. Son piezas puras; los algoritmos de layout las
 * usan para decidir dónde se parte cada caja.
 *
 * <p>La calidad de un corte es una de las constantes de {@link BreakAppeal}: mayor es mejor.
 * Se elige el <strong>último</strong> corte de mayor calidad, que es lo mismo que relajar las
 * reglas en el orden de la referencia (referencia/comun/cortes.md): primero viudas y huérfanas, después
 * {@code Before}/{@code After: avoid} y por último {@code Inside: avoid} (de {@code Break}).</p>
 */
public final class BreakRules {

    private BreakRules() {
    }

    /**
     * Cuántas líneas de un párrafo colocar en el espacio disponible.
     *
     * @param pending               líneas que quedan por colocar
     * @param capacity              líneas que caben en lo que queda de fragmentainer
     * @param widows                mínimo de líneas que deben pasar al siguiente
     * @param orphans               mínimo de líneas que deben quedar en este
     * @param atFragmentainerStart  si estamos al principio de una página (garantía de progreso)
     * @return líneas a colocar aquí; 0 significa "mover todo el párrafo al siguiente"
     */
    public static int linesThatFit(int pending, int capacity, int widows, int orphans,
                                   boolean atFragmentainerStart) {
        if (pending <= capacity) {
            return pending;
        }
        int k = capacity;
        if (pending - k < widows) {
            k = pending - widows;
        }
        if (k < orphans) {
            k = 0;
        }
        if (k <= 0 && atFragmentainerStart) {
            // Garantía de progreso: al principio de página siempre se coloca algo,
            // relajando viudas y huérfanas.
            k = Math.max(1, Math.min(capacity, pending));
        }
        return Math.max(k, 0);
    }

    /**
     * Líneas a colocar cuando ningún corte respeta viudas y huérfanas, pero el padre puede
     * preferir partir el párrafo a separar dos cajas con {@code avoid}: se respetan las viudas
     * si se puede (y se incumplen las huérfanas) y, si no, se llena el espacio.
     *
     * @return entre 1 y {@code capacity}; requiere {@code 0 < capacity < pending}
     */
    public static int relaxedLines(int pending, int capacity, int widows) {
        int keepingWidows = pending - widows;
        return keepingWidows >= 1 ? Math.min(capacity, keepingWidows) : capacity;
    }

    /**
     * Corte entre las líneas de un texto: las que caben desde {@code start}, respetando viudas y
     * huérfanas en cada párrafo. Entre dos párrafos siempre se puede cortar sin incumplir nada.
     * Si ningún corte respeta las reglas y no se ha colocado nada, se incumplen
     * ({@link #relaxedLines}) para que el padre pueda comparar ese corte con cortar antes de la
     * caja.
     *
     * @param paragraphEnds índice de la primera línea siguiente a cada párrafo, en orden
     * @param start         primera línea de este fragmento
     * @param capacity      líneas que caben en lo que queda de página
     * @return primera línea que pasa al fragmento siguiente (igual a {@code start} si no se
     *         coloca ninguna: cortar antes de la caja) y la calidad del corte
     */
    public static Cut cutLines(int[] paragraphEnds, int start, int capacity, int widows, int orphans,
                               boolean atFragmentainerStart) {
        int placed = 0;
        int paragraphStart = 0;
        for (int end : paragraphEnds) {
            int from = Math.max(paragraphStart, start);
            paragraphStart = end;
            if (end <= start) {
                continue;
            }
            int pending = end - from;
            int left = capacity - placed;
            if (pending <= left) {
                placed += pending;
                continue;
            }
            int k = linesThatFit(pending, left, widows, orphans, false);
            if (k > 0) {
                return new Cut(from + k, BreakAppeal.PERFECT_BREAK);
            }
            if (placed > 0) {
                // Todo el párrafo a la página siguiente: el corte queda entre dos párrafos.
                return new Cut(from, BreakAppeal.PERFECT_BREAK);
            }
            if (left > 0) {
                return new Cut(from + relaxedLines(pending, left, widows), BreakAppeal.VIOLATES_WIDOWS);
            }
            // No cabe ni una línea: al principio de página se coloca igualmente (garantía de progreso).
            return new Cut(atFragmentainerStart ? from + 1 : from, BreakAppeal.FORCED);
        }
        return new Cut(paragraphStart, BreakAppeal.PERFECT_BREAK);
    }

    /** Capacidad en líneas de un espacio vertical. */
    public static int capacity(double space, double lineHeight) {
        if (Double.isInfinite(space)) {
            return Integer.MAX_VALUE;
        }
        if (space <= 0) {
            return 0;
        }
        return (int) Math.floor((space + EPSILON) / lineHeight);
    }

    // --- Cortes entre cajas -----------------------------------------------------------------------

    /** Calidad del corte entre dos cajas hermanas en flujo: {@code avoid} en cualquiera lo empeora. */
    public static int between(Box before, Box after) {
        return before.style().breaks().after() == Break.AVOID || after.style().breaks().before() == Break.AVOID
                ? BreakAppeal.VIOLATES_AVOID_BETWEEN : BreakAppeal.PERFECT_BREAK;
    }

    /**
     * El corte forzado entre dos cajas hermanas en flujo: {@code PAGE}, {@code COLUMN} (solo
     * dentro de un {@code Columns}) o {@code AUTO} si no hay ninguno. Si hay de los dos, gana el
     * de página.
     */
    public static Break forcedBetween(Box before, Box after) {
        return stronger(forcedAfter(before), forcedBefore(after));
    }

    /**
     * El corte forzado antes de la caja ({@code Before} de {@code Break}) o, si es un bloque, de
     * su primer hijo en flujo: como en CSS, el corte forzado del primer hijo se propaga al padre,
     * porque los dos empiezan en el mismo punto. De un {@code Columns} solo sale el de página:
     * el de columna se queda en su primera columna.
     *
     * @return {@code PAGE}, {@code COLUMN} o {@code AUTO}
     */
    public static Break forcedBefore(Box box) {
        Break own = forced(box.style().breaks().before());
        List<Box> children = propagates(box) ? box.inFlowChildren() : List.of();
        return children.isEmpty() ? own : stronger(own, fromInside(box, forcedBefore(children.get(0))));
    }

    /** El corte forzado después de la caja o, si es un bloque, de su último hijo en flujo (ver {@link #forcedBefore}). */
    public static Break forcedAfter(Box box) {
        Break own = forced(box.style().breaks().after());
        List<Box> children = propagates(box) ? box.inFlowChildren() : List.of();
        return children.isEmpty() ? own
                : stronger(own, fromInside(box, forcedAfter(children.get(children.size() - 1))));
    }

    /** Si hay corte forzado: {@code PAGE} o {@code COLUMN}. */
    public static boolean isForced(Break forced) {
        return forced == Break.PAGE || forced == Break.COLUMN;
    }

    /**
     * Calidad de un corte forzado: {@link BreakAppeal#FORCED_PAGE} el de página, que debe salir
     * de las columnas, y {@link BreakAppeal#PERFECT_BREAK} el de columna.
     */
    public static int forcedAppeal(Break forced) {
        return forced == Break.PAGE ? BreakAppeal.FORCED_PAGE : BreakAppeal.PERFECT_BREAK;
    }

    /** Los cortes forzados de un bloque y de un {@code Columns} son también los de sus hijos de los extremos. */
    private static boolean propagates(Box box) {
        return box.style().layout() == Layout.BLOCK || box.style().layout() == Layout.COLUMNS;
    }

    /** El corte forzado de un hijo de los extremos que sale de la caja: de un {@code Columns}, solo el de página. */
    private static Break fromInside(Box box, Break forced) {
        return box.style().layout() == Layout.COLUMNS && forced == Break.COLUMN ? Break.AUTO : forced;
    }

    private static Break forced(Break b) {
        return isForced(b) ? b : Break.AUTO;
    }

    private static Break stronger(Break a, Break b) {
        if (a == Break.PAGE || b == Break.PAGE) {
            return Break.PAGE;
        }
        return a == Break.COLUMN || b == Break.COLUMN ? Break.COLUMN : Break.AUTO;
    }

    /**
     * Calidad del corte entre dos grupos de cajas, como dos líneas flex o dos filas de una
     * rejilla: el peor de todos los pares.
     *
     * @param before cajas que acaban antes del corte
     * @param after  cajas que empiezan después
     */
    public static int between(List<Box> before, List<Box> after) {
        int appeal = BreakAppeal.PERFECT_BREAK;
        for (Box b : before) {
            for (Box a : after) {
                appeal = Math.min(appeal, between(b, a));
            }
        }
        return appeal;
    }

    /** El corte forzado entre dos grupos de cajas (ver {@link #between(List, List)} y {@link #forcedBetween(Box, Box)}). */
    public static Break forcedBetween(List<Box> before, List<Box> after) {
        Break forced = Break.AUTO;
        for (Box b : before) {
            forced = stronger(forced, forcedAfter(b));
        }
        for (Box a : after) {
            forced = stronger(forced, forcedBefore(a));
        }
        return forced;
    }

    /** Calidad de un corte dentro de una caja: {@code Inside: avoid} lo deja en {@link BreakAppeal#FORCED}. */
    public static int inside(Box box, int appeal) {
        return box.style().breaks().avoidInside() ? BreakAppeal.FORCED : appeal;
    }

    // --- Elección del corte -----------------------------------------------------------------------

    /**
     * El último de los cortes de mayor calidad; {@code null} si no hay ninguno.
     *
     * @param candidates cortes posibles, en orden
     * @param appeal     calidad de cada corte (constantes de {@link BreakAppeal})
     */
    public static <T> T best(List<T> candidates, ToIntFunction<T> appeal) {
        T best = null;
        for (T c : candidates) {
            if (best == null || appeal.applyAsInt(c) >= appeal.applyAsInt(best)) {
                best = c;
            }
        }
        return best;
    }

    /**
     * Resultado de {@link #cutUnits}.
     *
     * @param end    primera unidad que pasa al siguiente fragmento; igual al número de unidades si
     *               caben todas, e igual a la primera si no cabe ninguna (cortar antes de la caja)
     * @param appeal calidad del corte
     */
    public record Cut(int end, int appeal) {
    }

    /**
     * Corte entre unidades que no se parten ({@link Unit}). Las posiciones son las del layout sin
     * fragmentar, desde el principio de la caja de contenido.
     *
     * @param units     las unidades del contenedor
     * @param first     primera unidad de este fragmento
     * @param space     alto disponible desde el principio de la unidad {@code first}
     * @param mustPlace garantía de progreso: hay que colocar al menos una unidad aunque desborde
     */
    public static Cut cutUnits(List<Unit> units, int first, double space, boolean mustPlace) {
        int n = units.size();
        if (first >= n) {
            return new Cut(n, BreakAppeal.PERFECT_BREAK);
        }
        double origin = units.get(first).start();
        int best = -1;
        boolean overflowing = false;
        for (int i = first; i < n; i++) {
            Unit u = units.get(i);
            if (i > first) {
                if (isForced(u.forcedBefore())) {
                    return new Cut(i, forcedAppeal(u.forcedBefore()));
                }
                if (u.breakableBefore()) {
                    if (overflowing) {
                        // Se ha colocado a la fuerza lo que no se podía separar: se corta en cuanto se puede.
                        return new Cut(i, BreakAppeal.FORCED);
                    }
                    if (best < 0 || u.appealBefore() >= units.get(best).appealBefore()) {
                        best = i;
                    }
                }
            }
            if (!overflowing && !fits(u.end() - origin, space)) {
                if (best >= 0) {
                    return new Cut(best, units.get(best).appealBefore());
                }
                if (!mustPlace) {
                    return new Cut(first, BreakAppeal.FORCED);
                }
                overflowing = true;
            }
        }
        return new Cut(n, BreakAppeal.PERFECT_BREAK);
    }
}
