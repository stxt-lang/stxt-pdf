package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragmentos.BreakToken;
import dev.stxt.boxes.fragmentos.Fragment;

import java.util.List;

/**
 * Salida inmutable del layout de una caja (módulos 7 y 8).
 *
 * @param fragment    el fragmento producido en este fragmentainer
 * @param breakToken  dónde se cortó; {@code null} si la caja ha terminado
 * @param breakAppeal calidad del corte elegido (mayor es mejor; ver {@link #CORTE_PERFECTO})
 * @param pendientes  cajas absolutas cuyo containing block es un ancestro de esta caja
 */
public record LayoutResult(Fragment fragment, BreakToken breakToken, int breakAppeal,
                           List<PendienteAbsoluta> pendientes) {

    /** Corte que no incumple ninguna regla. */
    public static final int CORTE_PERFECTO = 3;
    /** Incumple viudas o huérfanas. */
    public static final int INCUMPLE_VIUDAS = 2;
    /** Incumple {@code Corte antes/despues: evitar}. */
    public static final int INCUMPLE_EVITAR_ENTRE = 1;
    /** Incumple {@code Corte dentro: evitar} o corta a la fuerza. */
    public static final int FORZADO = 0;

    public LayoutResult {
        pendientes = List.copyOf(pendientes);
    }

    public static LayoutResult terminado(Fragment fragment) {
        return new LayoutResult(fragment, null, CORTE_PERFECTO, List.of());
    }

    public boolean terminado() {
        return breakToken == null;
    }
}
