package dev.stxt.boxes.fragment;

import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Reglas de corte (ESPECIFICACION.md §5.10): viudas y huérfanas, calidad de los cortes entre
 * cajas, cortes forzados y elección del corte. Son piezas puras; los algoritmos de layout las
 * usan para decidir dónde se parte cada caja.
 *
 * <p>La calidad de un corte es una de las constantes de {@link LayoutResult}: mayor es mejor.
 * Se elige el <strong>último</strong> corte de mayor calidad, que es lo mismo que relajar las
 * reglas en el orden de la especificación: primero viudas y huérfanas, después
 * {@code Before}/{@code After: avoid} y por último {@code Inside: avoid} (de {@code Break}).</p>
 */
public final class BreakRules {

    private static final double EPSILON = 1e-9;

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
                return new Cut(from + k, LayoutResult.PERFECT_BREAK);
            }
            if (placed > 0) {
                // Todo el párrafo a la página siguiente: el corte queda entre dos párrafos.
                return new Cut(from, LayoutResult.PERFECT_BREAK);
            }
            if (left > 0) {
                return new Cut(from + relaxedLines(pending, left, widows), LayoutResult.VIOLATES_WIDOWS);
            }
            // No cabe ni una línea: al principio de página se coloca igualmente (garantía de progreso).
            return new Cut(atFragmentainerStart ? from + 1 : from, LayoutResult.FORCED);
        }
        return new Cut(paragraphStart, LayoutResult.PERFECT_BREAK);
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
        return before.style.breakAfter == Style.Break.AVOID || after.style.breakBefore == Style.Break.AVOID
                ? LayoutResult.VIOLATES_AVOID_BETWEEN : LayoutResult.PERFECT_BREAK;
    }

    /** Hay un corte forzado entre dos cajas hermanas en flujo. */
    public static boolean forcedBetween(Box before, Box after) {
        return forcedAfter(before) || forcedBefore(after);
    }

    /**
     * {@code Before: page} (de {@code Break}) de la caja o, si es un bloque, de su primer hijo en flujo:
     * como en CSS, el corte forzado del primer hijo se propaga al padre, porque los dos empiezan
     * en el mismo punto.
     */
    public static boolean forcedBefore(Box box) {
        if (box.style.breakBefore == Style.Break.PAGE) {
            return true;
        }
        List<Box> children = box.style.layout == Style.Layout.BLOCK ? box.inFlowChildren() : List.of();
        return !children.isEmpty() && forcedBefore(children.get(0));
    }

    /** {@code After: page} (de {@code Break}) de la caja o, si es un bloque, de su último hijo en flujo. */
    public static boolean forcedAfter(Box box) {
        if (box.style.breakAfter == Style.Break.PAGE) {
            return true;
        }
        List<Box> children = box.style.layout == Style.Layout.BLOCK ? box.inFlowChildren() : List.of();
        return !children.isEmpty() && forcedAfter(children.get(children.size() - 1));
    }

    /**
     * Calidad del corte entre dos grupos de cajas, como dos líneas flex o dos filas de una
     * rejilla: el peor de todos los pares.
     *
     * @param before cajas que acaban antes del corte
     * @param after  cajas que empiezan después
     */
    public static int between(List<Box> before, List<Box> after) {
        int appeal = LayoutResult.PERFECT_BREAK;
        for (Box b : before) {
            for (Box a : after) {
                appeal = Math.min(appeal, between(b, a));
            }
        }
        return appeal;
    }

    /** Hay un corte forzado entre dos grupos de cajas (ver {@link #between(List, List)}). */
    public static boolean forcedBetween(List<Box> before, List<Box> after) {
        return before.stream().anyMatch(BreakRules::forcedAfter) || after.stream().anyMatch(BreakRules::forcedBefore);
    }

    /** Calidad de un corte dentro de una caja: {@code Inside: avoid} lo deja en {@link LayoutResult#FORCED}. */
    public static int inside(Box box, int appeal) {
        return box.style.avoidBreakInside ? LayoutResult.FORCED : appeal;
    }

    // --- Elección del corte -----------------------------------------------------------------------

    /**
     * El último de los cortes de mayor calidad; {@code null} si no hay ninguno.
     *
     * @param candidates cortes posibles, en orden
     * @param appeal     calidad de cada corte (constantes de {@link LayoutResult})
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
     * Corte entre unidades que no se parten: las líneas de un contenedor flex en {@code row}, los
     * ítems de uno en {@code column} o las filas de una rejilla. Las posiciones son las del layout
     * sin fragmentar, desde el principio de la caja de contenido.
     *
     * @param starts    posición de cada unidad
     * @param ends      final de cada unidad
     * @param first     primera unidad de este fragmento
     * @param space     alto disponible desde el principio de la unidad {@code first}
     * @param appeal    calidad del corte delante de cada unidad; negativa si ahí no se puede
     *                  cortar (un ítem de grid que ocupa las dos filas)
     * @param forced    hay un corte forzado delante de cada unidad
     * @param mustPlace garantía de progreso: hay que colocar al menos una unidad aunque desborde
     */
    public static Cut cutUnits(double[] starts, double[] ends, int first, double space,
                               int[] appeal, boolean[] forced, boolean mustPlace) {
        int n = starts.length;
        if (first >= n) {
            return new Cut(n, LayoutResult.PERFECT_BREAK);
        }
        double origin = starts[first];
        int best = -1;
        boolean overflowing = false;
        for (int i = first; i < n; i++) {
            if (i > first) {
                if (forced[i]) {
                    return new Cut(i, LayoutResult.PERFECT_BREAK);
                }
                if (appeal[i] >= 0) {
                    if (overflowing) {
                        // Se ha colocado a la fuerza lo que no se podía separar: se corta en cuanto se puede.
                        return new Cut(i, LayoutResult.FORCED);
                    }
                    if (best < 0 || appeal[i] >= appeal[best]) {
                        best = i;
                    }
                }
            }
            if (!overflowing && ends[i] - origin > space + EPSILON) {
                if (best >= 0) {
                    return new Cut(best, appeal[best]);
                }
                if (!mustPlace) {
                    return new Cut(first, LayoutResult.FORCED);
                }
                overflowing = true;
            }
        }
        return new Cut(n, LayoutResult.PERFECT_BREAK);
    }
}
