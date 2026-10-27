package dev.stxt.boxes.layout.flex;

import java.util.List;

import static dev.stxt.boxes.model.Tolerance.EPSILON;

/**
 * Resuelve los tamaños flexibles de una línea flex (módulo 5, paso 5; Flexbox §9.7).
 *
 * <p>Si sobra espacio se crece en proporción a {@code Grow}; si falta, se encoge en proporción a
 * {@code Shrink × Basis}. Los ítems que chocan con su mínimo o máximo se congelan y el reparto se
 * repite entre los demás.</p>
 */
public final class FlexResolver {

    private FlexResolver() {
    }

    /**
     * @param items     ítems de la línea, en orden
     * @param available espacio del eje principal ya descontados los huecos
     * @return tamaño principal final de cada ítem
     */
    public static double[] resolve(List<FlexItem> items, double available) {
        int n = items.size();
        double[] target = new double[n];
        boolean[] frozen = new boolean[n];

        double hypotheticalSum = 0;
        for (FlexItem it : items) {
            hypotheticalSum += it.hypothetical();
        }
        boolean growing = hypotheticalSum < available;

        // Ítems inflexibles: factor 0, o base fuera del sentido del reparto.
        for (int i = 0; i < n; i++) {
            FlexItem it = items.get(i);
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
                    FlexItem it = items.get(i);
                    factorSum += growing ? it.grow() : it.shrink();
                    scaledSum += it.shrink() * it.basis();
                }
            }
            if (!anyActive) {
                break;
            }

            double free = free(items, target, frozen, available);
            if (factorSum < 1) {
                // Factores que suman menos de 1: solo se reparte esa fracción del espacio libre.
                double fraction = initialFree * factorSum;
                if (Math.abs(fraction) < Math.abs(free)) {
                    free = fraction;
                }
            }

            for (int i = 0; i < n; i++) {
                if (frozen[i]) {
                    continue;
                }
                FlexItem it = items.get(i);
                if (growing) {
                    target[i] = it.basis() + (factorSum > 0 ? free * it.grow() / factorSum : 0);
                } else {
                    double scaled = it.shrink() * it.basis();
                    target[i] = it.basis() + (scaledSum > 0 ? free * scaled / scaledSum : 0);
                }
            }

            // Corregir violaciones de mínimo y máximo.
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

    private static double free(List<FlexItem> items, double[] target, boolean[] frozen, double available) {
        double used = 0;
        for (int i = 0; i < items.size(); i++) {
            used += frozen[i] ? target[i] : items.get(i).basis();
        }
        return available - used;
    }
}
