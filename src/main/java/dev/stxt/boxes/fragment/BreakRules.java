package dev.stxt.boxes.fragment;

/**
 * Reglas tipográficas de corte (módulo 8, ESPECIFICACION.md §5.10).
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

    /** Capacidad en líneas de un espacio vertical. */
    public static int capacity(double space, double lineHeight) {
        if (Double.isInfinite(space)) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor((space + 1e-9) / lineHeight);
    }
}
