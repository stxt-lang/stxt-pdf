package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.layout.Sizing;
import dev.stxt.pdf.model.Value;

import java.util.Comparator;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Track sizing (reference/components/grid.md; simplification of Grid §12). The same function serves for
 * columns and rows: only the contributions of the items change.
 *
 * <ol>
 *   <li>Initialize base (minimum) and limit (maximum) of each track.</li>
 *   <li>Intrinsic contributions: first items of one track, then the ones that span several without
 *       crossing any fraction and, last, the ones that cross one (only the fractions grow).</li>
 *   <li>Maximize: distribute the free space up to the limit of each non-flexible track.</li>
 *   <li>Expand fractions, with a freezing loop for the narrowest width.</li>
 *   <li>If there are no fractions, stretch the {@code auto} tracks with the leftover space.</li>
 * </ol>
 *
 * <p>Simplification: {@code fit} is treated as {@code auto}.</p>
 */
public final class TrackSizing {

    private TrackSizing() {
    }

    /**
     * @param declared  definitions of the tracks (explicit and implicit)
     * @param items     contributions of the placed items
     * @param available size of the content box of the container on this axis;
     *                  {@link Double#POSITIVE_INFINITY} if indefinite (the percentages of the
     *                  tracks then behave as {@code auto})
     * @param gap       spacing between tracks
     * @return size of each track
     */
    public static double[] size(List<Track> declared, List<ItemContribution> items,
                                double available, double gap) {
        boolean finite = Double.isFinite(available);
        List<Track> tracks = finite ? declared : declared.stream().map(Track::withoutPercentBase).toList();
        int n = tracks.size();
        double[] base = new double[n];
        double[] limit = new double[n];
        double space = finite ? available - gap * Math.max(0, n - 1) : Double.POSITIVE_INFINITY;
        // Percentages refer to the whole content box, gaps included (reference/common/values.md).
        double reference = finite ? available : 0;

        // 1. Initialize.
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

        // 2a. Items of a single track.
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

        // 2b. Items of several tracks, from smallest to largest span.
        items.stream()
                .filter(it -> it.span() > 1)
                .sorted(Comparator.comparingInt(ItemContribution::span))
                .forEach(it -> distributeExcess(tracks, base, limit, it, gap));

        // 2c. Items that cross some fraction, all together.
        distributeToFlexible(tracks, base, items, gap);

        for (int i = 0; i < n; i++) {
            if (!tracks.get(i).flexible() && limit[i] < base[i]) {
                limit[i] = base[i];
            }
        }

        if (!finite) {
            // Indefinite size: the non-flexible tracks reach their limit and the fractions
            // take the largest base/weight (simplification of "find the size of an fr").
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

        // 3. Maximize.
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

        // 4. Expand fractions.
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
            // 5. Stretch auto tracks.
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
        // The limits, with the bases already raised (a limit is never smaller than its base): what the item already
        // has is not given again.
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
     * Items of several tracks that cross some fraction (Grid §12.5, step 4): what they lack
     * to reach their narrowest width is distributed only among the fractions with an intrinsic minimum that they span,
     * in proportion to their weight (if the weights add up to less than 1, that part in proportion and
     * the rest in equal parts). It is computed with the bases from before the step, and each track grows
     * by what the item asking the most of it asks for.
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
