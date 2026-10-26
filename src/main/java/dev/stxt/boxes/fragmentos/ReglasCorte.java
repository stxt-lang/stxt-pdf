package dev.stxt.boxes.fragmentos;

/**
 * Reglas tipográficas de corte (módulo 8, ESPECIFICACION.md §5.10).
 */
public final class ReglasCorte {

    private ReglasCorte() {
    }

    /**
     * Cuántas líneas de un párrafo colocar en el espacio disponible.
     *
     * @param pendientes           líneas que quedan por colocar
     * @param capacidad            líneas que caben en lo que queda de fragmentainer
     * @param viudas               mínimo de líneas que deben pasar al siguiente
     * @param huerfanas            mínimo de líneas que deben quedar en este
     * @param inicioFragmentainer  si estamos al principio de una página (garantía de progreso)
     * @return líneas a colocar aquí; 0 significa "mover todo el párrafo al siguiente"
     */
    public static int lineasQueCaben(int pendientes, int capacidad, int viudas, int huerfanas,
                                     boolean inicioFragmentainer) {
        if (pendientes <= capacidad) {
            return pendientes;
        }
        int k = capacidad;
        if (pendientes - k < viudas) {
            k = pendientes - viudas;
        }
        if (k < huerfanas) {
            k = 0;
        }
        if (k <= 0 && inicioFragmentainer) {
            // Garantía de progreso: al principio de página siempre se coloca algo,
            // relajando viudas y huérfanas.
            k = Math.max(1, Math.min(capacidad, pendientes));
        }
        return Math.max(k, 0);
    }

    /** Capacidad en líneas de un espacio vertical. */
    public static int capacidad(double espacio, double alturaLinea) {
        if (Double.isInfinite(espacio)) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor((espacio + 1e-9) / alturaLinea);
    }
}
