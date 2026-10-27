package dev.stxt.boxes.fragment;

/**
 * Calidad de un corte (ESPECIFICACION.md §5.10): mayor es mejor. {@link BreakRules#best} se
 * queda con el último corte de mayor calidad, que es lo mismo que relajar las reglas en el orden
 * de la especificación.
 */
public final class BreakAppeal {

    /**
     * Corte forzado de página ({@code Before} o {@code After: page} de {@code Break}): mejor que
     * cualquier otro, y distinto de un corte perfecto para que un {@code Multicol} no lo tome por
     * el final de una columna y lo deje pasar hasta la página (§5.16).
     */
    public static final int FORCED_PAGE = 4;
    /** Corte que no incumple ninguna regla; también el forzado de columna. */
    public static final int PERFECT_BREAK = 3;
    /** Incumple viudas o huérfanas. */
    public static final int VIOLATES_WIDOWS = 2;
    /** Incumple {@code Before}/{@code After: avoid} de {@code Break}. */
    public static final int VIOLATES_AVOID_BETWEEN = 1;
    /** Incumple {@code Inside: avoid} de {@code Break} o corta a la fuerza. */
    public static final int FORCED = 0;

    private BreakAppeal() {
    }
}
