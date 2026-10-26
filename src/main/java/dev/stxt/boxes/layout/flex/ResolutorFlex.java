package dev.stxt.boxes.layout.flex;

import java.util.List;

/**
 * Resuelve los tamaños flexibles de una línea flex (módulo 5, paso 5; Flexbox §9.7).
 *
 * <p>Si sobra espacio se crece en proporción a {@code Crecer}; si falta, se encoge en proporción a
 * {@code Encoger × Base}. Los ítems que chocan con su mínimo o máximo se congelan y el reparto se
 * repite entre los demás.</p>
 */
public final class ResolutorFlex {

    private ResolutorFlex() {
    }

    /**
     * @param items      ítems de la línea, en orden
     * @param disponible espacio del eje principal ya descontados los huecos
     * @return tamaño principal final de cada ítem
     */
    public static double[] resolver(List<ItemFlex> items, double disponible) {
        int n = items.size();
        double[] objetivo = new double[n];
        boolean[] congelado = new boolean[n];

        double sumaHipoteticos = 0;
        for (ItemFlex it : items) {
            sumaHipoteticos += it.hipotetico();
        }
        boolean creciendo = sumaHipoteticos < disponible;

        // Ítems inflexibles: factor 0, o base fuera del sentido del reparto.
        for (int i = 0; i < n; i++) {
            ItemFlex it = items.get(i);
            double factor = creciendo ? it.crecer() : it.encoger();
            if (factor == 0
                    || (creciendo && it.base() > it.hipotetico())
                    || (!creciendo && it.base() < it.hipotetico())) {
                congelado[i] = true;
                objetivo[i] = it.hipotetico();
            }
        }
        double libreInicial = libre(items, objetivo, congelado, disponible);

        for (int vuelta = 0; vuelta <= n; vuelta++) {
            boolean hayActivos = false;
            double sumaFactores = 0;
            double sumaEscalados = 0;
            for (int i = 0; i < n; i++) {
                if (!congelado[i]) {
                    hayActivos = true;
                    ItemFlex it = items.get(i);
                    sumaFactores += creciendo ? it.crecer() : it.encoger();
                    sumaEscalados += it.encoger() * it.base();
                }
            }
            if (!hayActivos) {
                break;
            }

            double libre = libre(items, objetivo, congelado, disponible);
            if (sumaFactores < 1) {
                // Factores que suman menos de 1: solo se reparte esa fracción del espacio libre.
                double fraccion = libreInicial * sumaFactores;
                if (Math.abs(fraccion) < Math.abs(libre)) {
                    libre = fraccion;
                }
            }

            for (int i = 0; i < n; i++) {
                if (congelado[i]) {
                    continue;
                }
                ItemFlex it = items.get(i);
                if (creciendo) {
                    objetivo[i] = it.base() + (sumaFactores > 0 ? libre * it.crecer() / sumaFactores : 0);
                } else {
                    double escalado = it.encoger() * it.base();
                    objetivo[i] = it.base() + (sumaEscalados > 0 ? libre * escalado / sumaEscalados : 0);
                }
            }

            // Corregir violaciones de mínimo y máximo.
            double violacionTotal = 0;
            double[] limitado = new double[n];
            for (int i = 0; i < n; i++) {
                if (!congelado[i]) {
                    limitado[i] = items.get(i).limitar(objetivo[i]);
                    violacionTotal += limitado[i] - objetivo[i];
                }
            }
            for (int i = 0; i < n; i++) {
                if (congelado[i]) {
                    continue;
                }
                double violacion = limitado[i] - objetivo[i];
                boolean congelar = Math.abs(violacionTotal) < 1e-9
                        || (violacionTotal > 0 && violacion > 0)
                        || (violacionTotal < 0 && violacion < 0);
                objetivo[i] = limitado[i];
                if (congelar) {
                    congelado[i] = true;
                }
            }
        }
        return objetivo;
    }

    private static double libre(List<ItemFlex> items, double[] objetivo, boolean[] congelado, double disponible) {
        double usado = 0;
        for (int i = 0; i < items.size(); i++) {
            usado += congelado[i] ? objetivo[i] : items.get(i).base();
        }
        return disponible - usado;
    }
}
