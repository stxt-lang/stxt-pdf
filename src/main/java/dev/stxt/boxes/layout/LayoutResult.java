package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;

import java.util.List;

/**
 * Salida inmutable del layout de una caja (módulos 7 y 8).
 *
 * @param fragment    el fragmento producido en este fragmentainer
 * @param breakToken  dónde se cortó; {@code null} si la caja ha terminado y
 *                    {@link BreakToken#BEFORE} si no cabe nada de ella (el padre corta antes)
 * @param breakAppeal calidad del corte elegido (mayor es mejor; ver {@link #PERFECT_BREAK})
 * @param pending     cajas absolutas cuyo containing block es un ancestro de esta caja, con la
 *                    posición estática relativa a la caja de contenido de esta caja
 * @param adjoining   márgenes de los descendientes que colapsan fuera de la caja (§5.3)
 */
public record LayoutResult(Fragment fragment, BreakToken breakToken, int breakAppeal,
                           List<PendingAbsolute> pending, AdjoiningMargins adjoining) {

    /** Corte que no incumple ninguna regla. */
    public static final int PERFECT_BREAK = 3;
    /** Incumple viudas o huérfanas. */
    public static final int VIOLATES_WIDOWS = 2;
    /** Incumple {@code Before}/{@code After: avoid} de {@code Break}. */
    public static final int VIOLATES_AVOID_BETWEEN = 1;
    /** Incumple {@code Inside: avoid} de {@code Break} o corta a la fuerza. */
    public static final int FORCED = 0;

    public LayoutResult {
        pending = List.copyOf(pending);
    }

    public static LayoutResult finished(Fragment fragment) {
        return finished(fragment, List.of(), AdjoiningMargins.NONE);
    }

    public static LayoutResult finished(Fragment fragment, List<PendingAbsolute> pending, AdjoiningMargins adjoining) {
        return new LayoutResult(fragment, null, PERFECT_BREAK, pending, adjoining);
    }

    public boolean isFinished() {
        return breakToken == null;
    }
}
