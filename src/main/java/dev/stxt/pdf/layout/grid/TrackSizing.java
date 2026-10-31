package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.layout.Sizing;
import dev.stxt.pdf.model.Value;

import java.util.Comparator;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Dimensionado de pistas (reference/components/grid.md; simplificación de Grid §12). La misma función sirve para
 * columnas y filas: solo cambian las aportaciones de los ítems.
 *
 * <ol>
 *   <li>Inicializar base (mínimo) y límite (máximo) de cada pista.</li>
 *   <li>Aportaciones intrínsecas: primero ítems de una pista, después los que ocupan varias sin
 *       cruzar ninguna fracción y, al final, los que cruzan alguna (solo crecen las fracciones).</li>
 *   <li>Maximizar: repartir el espacio libre hasta el límite de cada pista no flexible.</li>
 *   <li>Expandir fracciones, con bucle de congelación por el ancho más estrecho.</li>
 *   <li>Si no hay fracciones, estirar las pistas {@code auto} con el sobrante.</li>
 * </ol>
 *
 * <p>Simplificación: {@code fit} se trata como {@code auto}.</p>
 */
public final class TrackSizing {

    private TrackSizing() {
    }

    /**
     * @param declared  definiciones de las pistas (explícitas e implícitas)
     * @param items     aportaciones de los ítems colocados
     * @param available tamaño de la caja de contenido del contenedor en este eje;
     *                  {@link Double#POSITIVE_INFINITY} si es indefinido (los porcentajes de las
     *                  pistas se comportan entonces como {@code auto})
     * @param gap       separación entre pistas
     * @return tamaño de cada pista
     */
    public static double[] size(List<Track> declared, List<ItemContribution> items,
                                double available, double gap) {
        boolean finite = Double.isFinite(available);
        List<Track> tracks = finite ? declared : declared.stream().map(Track::withoutPercentBase).toList();
        int n = tracks.size();
        double[] base = new double[n];
        double[] limit = new double[n];
        double space = finite ? available - gap * Math.max(0, n - 1) : Double.POSITIVE_INFINITY;
        // Los porcentajes se refieren a la caja de contenido entera, huecos incluidos (reference/common/values.md).
        double reference = finite ? available : 0;

        // 1. Inicializar.
        for (int i = 0; i < n; i++) {
            Track t = tracks.get(i);
            base[i] = t.minIntrinsic() ? 0 : t.min().resolve(reference).orElse(0);
            if (t.flexible()) {
                limit[i] = Double.POSITIVE_INFINITY;
            } else if (t.maxIntrinsic()) {
                limit[i] = 0;
            } else {
                limit[i] = t.max().resolve(reference).orElse(0);
            }
        }

        // 2a. Ítems de una sola pista.
        for (ItemContribution it : items) {
            if (it.span() != 1) {
                continue;
            }
            int i = it.track();
            Track t = tracks.get(i);
            if (t.min().isAuto() || t.min() == Value.Keyword.NARROWEST || t.min() == Value.Keyword.FIT) {
                base[i] = Math.max(base[i], it.narrowest());
            } else if (t.min() == Value.Keyword.NATURAL) {
                base[i] = Math.max(base[i], it.natural());
            }
            if (!t.flexible()) {
                if (t.max() == Value.Keyword.NARROWEST) {
                    limit[i] = Math.max(limit[i], it.narrowest());
                } else if (t.max().isAuto() || t.max() == Value.Keyword.NATURAL
                        || t.max() == Value.Keyword.FIT) {
                    limit[i] = Math.max(limit[i], it.natural());
                }
            }
        }

        // 2b. Ítems de varias pistas, de menor a mayor extensión.
        items.stream()
                .filter(it -> it.span() > 1)
                .sorted(Comparator.comparingInt(ItemContribution::span))
                .forEach(it -> distributeExcess(tracks, base, limit, it, gap));

        // 2c. Ítems que cruzan alguna fracción, todos juntos.
        distributeToFlexible(tracks, base, items, gap);

        for (int i = 0; i < n; i++) {
            if (!tracks.get(i).flexible() && limit[i] < base[i]) {
                limit[i] = base[i];
            }
        }

        if (!finite) {
            // Tamaño indefinido: las pistas no flexibles llegan a su límite y las fracciones
            // toman el mayor base/peso (simplificación de "find the size of an fr").
            double frSize = 0;
            for (int i = 0; i < n; i++) {
                Track t = tracks.get(i);
                if (t.flexible()) {
                    frSize = Math.max(frSize, base[i] / Math.max(t.fraction(), 1));
                } else {
                    base[i] = limit[i];
                }
            }
            for (int i = 0; i < n; i++) {
                Track t = tracks.get(i);
                if (t.flexible()) {
                    base[i] = Math.max(base[i], frSize * t.fraction());
                }
            }
            return base;
        }

        // 3. Maximizar.
        double free = space - Sizing.sum(base);
        boolean[] active = new boolean[n];
        for (int i = 0; i < n; i++) {
            active[i] = !tracks.get(i).flexible() && base[i] < limit[i];
        }
        while (free > EPSILON && count(active) > 0) {
            double share = free / count(active);
            for (int i = 0; i < n; i++) {
                if (active[i]) {
                    double d = Math.min(share, limit[i] - base[i]);
                    base[i] += d;
                    free -= d;
                    if (base[i] >= limit[i] - EPSILON) {
                        active[i] = false;
                    }
                }
            }
        }

        // 4. Expandir fracciones.
        boolean anyFlexible = tracks.stream().anyMatch(Track::flexible);
        if (anyFlexible) {
            boolean[] inflexible = new boolean[n];
            for (int round = 0; round <= n; round++) {
                double fixed = 0;
                double frSum = 0;
                for (int i = 0; i < n; i++) {
                    if (!tracks.get(i).flexible() || inflexible[i]) {
                        fixed += base[i];
                    } else {
                        frSum += tracks.get(i).fraction();
                    }
                }
                if (frSum == 0) {
                    break;
                }
                double frSize = Math.max(0, space - fixed) / Math.max(frSum, 1);
                boolean recompute = false;
                for (int i = 0; i < n; i++) {
                    Track t = tracks.get(i);
                    if (t.flexible() && !inflexible[i] && !fits(base[i], frSize * t.fraction())) {
                        inflexible[i] = true;
                        recompute = true;
                    }
                }
                if (!recompute) {
                    for (int i = 0; i < n; i++) {
                        Track t = tracks.get(i);
                        if (t.flexible() && !inflexible[i]) {
                            base[i] = frSize * t.fraction();
                        }
                    }
                    break;
                }
            }
        } else {
            // 5. Estirar pistas auto.
            free = space - Sizing.sum(base);
            int autoTracks = 0;
            for (Track t : tracks) {
                if (t.max().isAuto()) {
                    autoTracks++;
                }
            }
            if (free > EPSILON && autoTracks > 0) {
                for (int i = 0; i < n; i++) {
                    if (tracks.get(i).max().isAuto()) {
                        base[i] += free / autoTracks;
                    }
                }
            }
        }
        return base;
    }

    private static void distributeExcess(List<Track> tracks, double[] base, double[] limit,
                                         ItemContribution it, double gap) {
        int from = it.track();
        int to = Math.min(tracks.size(), from + it.span());
        double gaps = gap * (it.span() - 1);
        for (int i = from; i < to; i++) {
            if (tracks.get(i).flexible()) {
                return;
            }
        }
        double baseSum = gaps;
        int intrinsicMin = 0;
        int intrinsicMax = 0;
        for (int i = from; i < to; i++) {
            baseSum += base[i];
            if (tracks.get(i).minIntrinsic()) {
                intrinsicMin++;
            }
            if (tracks.get(i).maxIntrinsic()) {
                intrinsicMax++;
            }
        }
        double excess = it.narrowest() - baseSum;
        if (excess > 0 && intrinsicMin > 0) {
            for (int i = from; i < to; i++) {
                if (tracks.get(i).minIntrinsic()) {
                    base[i] += excess / intrinsicMin;
                }
            }
        }
        // Los límites, con las bases ya subidas (un límite nunca es menor que su base): lo que el ítem ya tiene
        // no se vuelve a dar.
        double limitSum = gaps;
        for (int i = from; i < to; i++) {
            limitSum += Math.max(limit[i], base[i]);
        }
        double limitExcess = it.natural() - limitSum;
        if (limitExcess > 0 && intrinsicMax > 0) {
            for (int i = from; i < to; i++) {
                if (tracks.get(i).maxIntrinsic()) {
                    limit[i] = Math.max(limit[i], base[i]) + limitExcess / intrinsicMax;
                }
            }
        }
    }

    /**
     * Ítems de varias pistas que cruzan alguna fracción (Grid §12.5, paso 4): lo que les falta
     * hasta su ancho más estrecho se reparte solo entre las fracciones con mínimo intrínseco que ocupan,
     * en proporción a su peso (si los pesos suman menos de 1, esa parte en proporción y
     * el resto a partes iguales). Se calcula con las bases de antes del paso, y cada pista crece
     * lo que pida el ítem que más le pida.
     */
    private static void distributeToFlexible(List<Track> tracks, double[] base, List<ItemContribution> items,
                                             double gap) {
        double[] planned = base.clone();
        for (ItemContribution it : items) {
            int from = it.track();
            int to = Math.min(tracks.size(), from + it.span());
            if (it.span() < 2 || tracks.subList(from, to).stream().noneMatch(Track::flexible)) {
                continue;
            }
            double excess = it.narrowest() - gap * (it.span() - 1);
            double frSum = 0;
            int count = 0;
            for (int i = from; i < to; i++) {
                excess -= base[i];
                Track t = tracks.get(i);
                if (t.flexible() && t.minIntrinsic()) {
                    frSum += t.fraction();
                    count++;
                }
            }
            if (excess <= EPSILON || count == 0) {
                continue;
            }
            for (int i = from; i < to; i++) {
                Track t = tracks.get(i);
                if (t.flexible() && t.minIntrinsic()) {
                    double share = frSum >= 1
                            ? excess * t.fraction() / frSum
                            : excess * t.fraction() + excess * (1 - frSum) / count;
                    planned[i] = Math.max(planned[i], base[i] + share);
                }
            }
        }
        System.arraycopy(planned, 0, base, 0, base.length);
    }

    private static int count(boolean[] v) {
        int c = 0;
        for (boolean b : v) {
            if (b) {
                c++;
            }
        }
        return c;
    }
}
