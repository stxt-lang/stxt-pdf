package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;

import java.util.List;

/**
 * Salida inmutable del layout de una caja (módulos 7 y 8).
 *
 * @param fragment    el fragmento producido en este fragmentainer
 * @param breakToken  dónde se cortó; {@code null} si la caja ha terminado
 * @param breakAppeal calidad del corte elegido (mayor es mejor; ver {@link #PERFECT_BREAK})
 * @param pending     cajas absolutas cuyo containing block es un ancestro de esta caja
 */
public record LayoutResult(Fragment fragment, BreakToken breakToken, int breakAppeal,
                           List<PendingAbsolute> pending) {

    /** Corte que no incumple ninguna regla. */
    public static final int PERFECT_BREAK = 3;
    /** Incumple viudas o huérfanas. */
    public static final int VIOLATES_WIDOWS = 2;
    /** Incumple {@code Break before/after: avoid}. */
    public static final int VIOLATES_AVOID_BETWEEN = 1;
    /** Incumple {@code Break inside: avoid} o corta a la fuerza. */
    public static final int FORCED = 0;

    public LayoutResult {
        pending = List.copyOf(pending);
    }

    public static LayoutResult finished(Fragment fragment) {
        return new LayoutResult(fragment, null, PERFECT_BREAK, List.of());
    }

    public boolean isFinished() {
        return breakToken == null;
    }
}
