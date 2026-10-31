package dev.stxt.pdf.layout.stack;

import java.util.List;

import static dev.stxt.pdf.model.Tolerance.EPSILON;

/**
 * Resolves the flexible sizes of a stack line (reference/components/stack.md; Flexbox §9.7).
 *
 * <p>If there is space left over, items grow in proportion to {@code Grow}; if space is lacking, they shrink in
 * proportion to {@code Shrink × Base size}. The items that hit their minimum or maximum are frozen and the
 * distribution is repeated among the others.</p>
 */
public final class StackResolver {

    private StackResolver() {
    }

    /**
     * @param items     items of the line, in order
     * @param available space of the main axis with the gaps already subtracted
     * @return final main size of each item
     */
    public static double[] resolve(List<StackItem> items, double available) {
        int n = items.size();
        double[] target = new double[n];
        boolean[] frozen = new boolean[n];

        double hypotheticalSum = 0;
        for (StackItem it : items) {
            hypotheticalSum += it.hypothetical();
        }
        boolean growing = hypotheticalSum < available;

        // Inflexible items: factor 0, or base against the direction of the distribution.
        for (int i = 0; i < n; i++) {
            StackItem it = items.get(i);
            double factor = growing ? it.grow() : it.shrink();
            if (factor == 0
                    || (growing && it.basis() > it.hypothetical())
                    || (!growing && it.basis() < it.hypothetical())) {
                frozen[i] = true;
                target[i] = it.hypothetical();
            }
        }
        double initialFree = free(items, target, frozen, available);

        for (int round = 0; round <= n; round++) {
            boolean anyActive = false;
            double factorSum = 0;
            double scaledSum = 0;
            for (int i = 0; i < n; i++) {
                if (!frozen[i]) {
                    anyActive = true;
                    StackItem it = items.get(i);
                    factorSum += growing ? it.grow() : it.shrink();
                    scaledSum += it.scaledShrink();
                }
            }
            if (!anyActive) {
                break;
            }

            double free = free(items, target, frozen, available);
            if (factorSum < 1) {
                // Factors that add up to less than 1: only that fraction of the free space is distributed.
                double fraction = initialFree * factorSum;
                if (Math.abs(fraction) < Math.abs(free)) {
                    free = fraction;
                }
            }

            for (int i = 0; i < n; i++) {
                if (frozen[i]) {
                    continue;
                }
                StackItem it = items.get(i);
                if (growing) {
                    target[i] = it.basis() + (factorSum > 0 ? free * it.grow() / factorSum : 0);
                } else {
                    double scaled = it.scaledShrink();
                    target[i] = it.basis() + (scaledSum > 0 ? free * scaled / scaledSum : 0);
                }
            }

            // Fix minimum and maximum violations.
            double totalViolation = 0;
            double[] clamped = new double[n];
            for (int i = 0; i < n; i++) {
                if (!frozen[i]) {
                    clamped[i] = items.get(i).clamp(target[i]);
                    totalViolation += clamped[i] - target[i];
                }
            }
            for (int i = 0; i < n; i++) {
                if (frozen[i]) {
                    continue;
                }
                double violation = clamped[i] - target[i];
                boolean freeze = Math.abs(totalViolation) < EPSILON
                        || (totalViolation > 0 && violation > 0)
                        || (totalViolation < 0 && violation < 0);
                target[i] = clamped[i];
                if (freeze) {
                    frozen[i] = true;
                }
            }
        }
        return target;
    }

    private static double free(List<StackItem> items, double[] target, boolean[] frozen, double available) {
        double used = 0;
        for (int i = 0; i < items.size(); i++) {
            used += frozen[i] ? target[i] : items.get(i).basis();
        }
        return available - used;
    }
}
