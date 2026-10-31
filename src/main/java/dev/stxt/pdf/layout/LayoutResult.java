package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.tree.Box;

import java.util.List;

/**
 * Salida inmutable del layout de una caja (módulos 7 y 8).
 *
 * @param fragment    el fragmento producido en este fragmentainer
 * @param breakToken  dónde se cortó; {@code null} si la caja ha terminado y
 *                    {@link BreakToken#BEFORE} si no cabe nada de ella (el padre corta antes)
 * @param breakAppeal calidad del corte elegido (una de las constantes de {@link BreakAppeal})
 * @param pending     cajas absolutas cuyo containing block es un ancestro de esta caja, con la
 *                    posición estática relativa a la caja de contenido de esta caja
 * @param adjoining   márgenes de los descendientes que se fusionan fuera de la caja (reference/components/block.md)
 */
public record LayoutResult(Fragment fragment, BreakToken breakToken, int breakAppeal,
                           List<PendingOverlay> pending, AdjoiningMargins adjoining) {

    public LayoutResult {
        pending = List.copyOf(pending);
    }

    /** Nada de la caja cabe en lo que queda de página: el padre debe cortar antes de ella. */
    public static LayoutResult before(Box box) {
        return new LayoutResult(Fragment.empty(box), BreakToken.BEFORE, BreakAppeal.PERFECT_BREAK,
                List.of(), AdjoiningMargins.NONE);
    }

    public static LayoutResult finished(Fragment fragment) {
        return finished(fragment, List.of(), AdjoiningMargins.NONE);
    }

    public static LayoutResult finished(Fragment fragment, List<PendingOverlay> pending, AdjoiningMargins adjoining) {
        return new LayoutResult(fragment, null, BreakAppeal.PERFECT_BREAK, pending, adjoining);
    }

    public boolean isFinished() {
        return breakToken == null;
    }
}
