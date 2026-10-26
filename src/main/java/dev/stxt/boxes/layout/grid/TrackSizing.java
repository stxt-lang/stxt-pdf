package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.modelo.Valor;

import java.util.Comparator;
import java.util.List;

/**
 * Dimensionado de pistas (módulo 6; simplificación de Grid §12). La misma función sirve para
 * columnas y filas: solo cambian las aportaciones de los ítems.
 *
 * <ol>
 *   <li>Inicializar base (mínimo) y límite (máximo) de cada pista.</li>
 *   <li>Aportaciones intrínsecas: primero ítems de una pista, después los que ocupan varias.</li>
 *   <li>Maximizar: repartir el espacio libre hasta el límite de cada pista no flexible.</li>
 *   <li>Expandir fracciones, con bucle de congelación por min-content.</li>
 *   <li>Si no hay fracciones, estirar las pistas {@code auto} con el sobrante.</li>
 * </ol>
 *
 * <p>Simplificaciones: un ítem que ocupa varias pistas y alguna es flexible no aporta a las
 * pistas flexibles; {@code fit-content} se trata como {@code auto}.</p>
 */
public final class TrackSizing {

    private static final double EPS = 1e-9;

    private TrackSizing() {
    }

    /**
     * @param pistas     definiciones de las pistas (explícitas e implícitas)
     * @param items      aportaciones de los ítems colocados
     * @param disponible tamaño de la caja de contenido del contenedor en este eje;
     *                   {@link Double#POSITIVE_INFINITY} si es indefinido
     * @param hueco      separación entre pistas
     * @return tamaño de cada pista
     */
    public static double[] dimensionar(List<Pista> pistas, List<AportacionItem> items,
                                       double disponible, double hueco) {
        int n = pistas.size();
        double[] base = new double[n];
        double[] limite = new double[n];
        boolean finito = Double.isFinite(disponible);
        double espacio = finito ? disponible - hueco * Math.max(0, n - 1) : Double.POSITIVE_INFINITY;
        double referencia = finito ? espacio : 0;

        // 1. Inicializar.
        for (int i = 0; i < n; i++) {
            Pista p = pistas.get(i);
            base[i] = p.minIntrinseco() ? 0 : p.min().resolver(referencia).orElse(0);
            if (p.flexible()) {
                limite[i] = Double.POSITIVE_INFINITY;
            } else if (p.maxIntrinseco()) {
                limite[i] = 0;
            } else {
                limite[i] = p.max().resolver(referencia).orElse(0);
            }
        }

        // 2a. Ítems de una sola pista.
        for (AportacionItem it : items) {
            if (it.extension() != 1) {
                continue;
            }
            int i = it.pista();
            Pista p = pistas.get(i);
            if (p.min().esAuto() || p.min() == Valor.Palabra.MIN_CONTENT || p.min() == Valor.Palabra.FIT_CONTENT) {
                base[i] = Math.max(base[i], it.minContent());
            } else if (p.min() == Valor.Palabra.MAX_CONTENT) {
                base[i] = Math.max(base[i], it.maxContent());
            }
            if (!p.flexible()) {
                if (p.max() == Valor.Palabra.MIN_CONTENT) {
                    limite[i] = Math.max(limite[i], it.minContent());
                } else if (p.max().esAuto() || p.max() == Valor.Palabra.MAX_CONTENT
                        || p.max() == Valor.Palabra.FIT_CONTENT) {
                    limite[i] = Math.max(limite[i], it.maxContent());
                }
            }
        }

        // 2b. Ítems de varias pistas, de menor a mayor extensión.
        items.stream()
                .filter(it -> it.extension() > 1)
                .sorted(Comparator.comparingInt(AportacionItem::extension))
                .forEach(it -> repartirExceso(pistas, base, limite, it, hueco));

        for (int i = 0; i < n; i++) {
            if (!pistas.get(i).flexible() && limite[i] < base[i]) {
                limite[i] = base[i];
            }
        }

        if (!finito) {
            // Tamaño indefinido: las pistas no flexibles llegan a su límite y las fracciones
            // toman el mayor base/fr (simplificación de "find the size of an fr").
            double tamanoFr = 0;
            for (int i = 0; i < n; i++) {
                Pista p = pistas.get(i);
                if (p.flexible()) {
                    tamanoFr = Math.max(tamanoFr, base[i] / Math.max(p.fraccion(), 1));
                } else {
                    base[i] = limite[i];
                }
            }
            for (int i = 0; i < n; i++) {
                Pista p = pistas.get(i);
                if (p.flexible()) {
                    base[i] = Math.max(base[i], tamanoFr * p.fraccion());
                }
            }
            return base;
        }

        // 3. Maximizar.
        double libre = espacio - suma(base);
        boolean[] activo = new boolean[n];
        for (int i = 0; i < n; i++) {
            activo[i] = !pistas.get(i).flexible() && base[i] < limite[i];
        }
        while (libre > EPS && contar(activo) > 0) {
            double parte = libre / contar(activo);
            for (int i = 0; i < n; i++) {
                if (activo[i]) {
                    double d = Math.min(parte, limite[i] - base[i]);
                    base[i] += d;
                    libre -= d;
                    if (base[i] >= limite[i] - EPS) {
                        activo[i] = false;
                    }
                }
            }
        }

        // 4. Expandir fracciones.
        boolean hayFlexibles = pistas.stream().anyMatch(Pista::flexible);
        if (hayFlexibles) {
            boolean[] inflexible = new boolean[n];
            for (int vuelta = 0; vuelta <= n; vuelta++) {
                double fijo = 0;
                double sumaFr = 0;
                for (int i = 0; i < n; i++) {
                    if (!pistas.get(i).flexible() || inflexible[i]) {
                        fijo += base[i];
                    } else {
                        sumaFr += pistas.get(i).fraccion();
                    }
                }
                if (sumaFr == 0) {
                    break;
                }
                double tamanoFr = Math.max(0, espacio - fijo) / Math.max(sumaFr, 1);
                boolean recalcular = false;
                for (int i = 0; i < n; i++) {
                    Pista p = pistas.get(i);
                    if (p.flexible() && !inflexible[i] && base[i] > tamanoFr * p.fraccion() + EPS) {
                        inflexible[i] = true;
                        recalcular = true;
                    }
                }
                if (!recalcular) {
                    for (int i = 0; i < n; i++) {
                        Pista p = pistas.get(i);
                        if (p.flexible() && !inflexible[i]) {
                            base[i] = tamanoFr * p.fraccion();
                        }
                    }
                    break;
                }
            }
        } else {
            // 5. Estirar pistas auto.
            libre = espacio - suma(base);
            int auto = 0;
            for (Pista p : pistas) {
                if (p.max().esAuto()) {
                    auto++;
                }
            }
            if (libre > EPS && auto > 0) {
                for (int i = 0; i < n; i++) {
                    if (pistas.get(i).max().esAuto()) {
                        base[i] += libre / auto;
                    }
                }
            }
        }
        return base;
    }

    private static void repartirExceso(List<Pista> pistas, double[] base, double[] limite,
                                       AportacionItem it, double hueco) {
        int desde = it.pista();
        int hasta = Math.min(pistas.size(), desde + it.extension());
        double huecos = hueco * (it.extension() - 1);
        for (int i = desde; i < hasta; i++) {
            if (pistas.get(i).flexible()) {
                return;
            }
        }
        double sumaBase = huecos;
        double sumaLimite = huecos;
        int intrinsecasMin = 0;
        int intrinsecasMax = 0;
        for (int i = desde; i < hasta; i++) {
            sumaBase += base[i];
            sumaLimite += Math.max(limite[i], base[i]);
            if (pistas.get(i).minIntrinseco()) {
                intrinsecasMin++;
            }
            if (pistas.get(i).maxIntrinseco()) {
                intrinsecasMax++;
            }
        }
        double exceso = it.minContent() - sumaBase;
        if (exceso > 0 && intrinsecasMin > 0) {
            for (int i = desde; i < hasta; i++) {
                if (pistas.get(i).minIntrinseco()) {
                    base[i] += exceso / intrinsecasMin;
                }
            }
        }
        double excesoLimite = it.maxContent() - sumaLimite;
        if (excesoLimite > 0 && intrinsecasMax > 0) {
            for (int i = desde; i < hasta; i++) {
                if (pistas.get(i).maxIntrinseco()) {
                    limite[i] = Math.max(limite[i], base[i]) + excesoLimite / intrinsecasMax;
                }
            }
        }
    }

    private static double suma(double[] v) {
        double s = 0;
        for (double x : v) {
            s += x;
        }
        return s;
    }

    private static int contar(boolean[] v) {
        int c = 0;
        for (boolean b : v) {
            if (b) {
                c++;
            }
        }
        return c;
    }
}
