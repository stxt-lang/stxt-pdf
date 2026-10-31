package dev.stxt.pdf.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dónde se cortó una caja y desde dónde hay que reanudar su layout en el siguiente
 * fragmentainer (reference/common/breaks.md). Un {@code null} en su lugar significa "la caja ha
 * terminado".
 *
 * <p>Son {@code record}: tienen {@code equals}, y por eso pueden formar parte de la clave de la
 * caché de layout y el paginador puede detectar que no hay progreso.</p>
 */
public sealed interface BreakToken {

    /**
     * La caja no cabe en lo que queda de página y no ha colocado nada: el padre debe cortar
     * antes de ella. Solo se devuelve fuera del principio de página (garantía de progreso) y
     * nunca se pasa como token de reanudación: la caja vuelve a empezar desde {@code null}.
     */
    BreakToken BEFORE = new Before();

    /** Ver {@link #BEFORE}. */
    record Before() implements BreakToken {
    }

    /**
     * Bloque: siguiente hijo a colocar y, si el corte fue dentro de él, su propio token.
     *
     * @param consumedHeight alto de caja de borde de los fragmentos anteriores de la caja (con
     *                       {@code Edges: closed}, de su caja de contenido); un
     *                       {@code Height} definido es el total de todos los fragmentos
     */
    record Block(int nextChild, BreakToken childToken, double consumedHeight) implements BreakToken {
    }

    /** Texto: siguiente línea a colocar y alto de los fragmentos anteriores (ver {@link Block}). */
    record Line(int nextLine, double consumedHeight) implements BreakToken {
    }

    /**
     * Stack o grid: siguiente línea o fila (en un stack vertical, siguiente ítem) y alto de los
     * fragmentos anteriores.
     *
     * @param itemTokens vacía si el corte es antes de {@code nextLine}. Si es dentro (una fila que
     *                   no cabe en una página y cuyos ítems se parten en paralelo), el token de
     *                   cada ítem de la fila en orden del árbol, con {@code null} en los que ya
     *                   han terminado
     */
    record Container(int nextLine, List<BreakToken> itemTokens, double consumedHeight) implements BreakToken {
        public Container {
            // List.copyOf no admite null.
            itemTokens = Collections.unmodifiableList(new ArrayList<>(itemTokens));
        }
    }

    /** Alto consumido por los fragmentos anteriores de la caja que devolvió el token. */
    static double consumed(BreakToken token) {
        // Java 17 no tiene patrones en switch.
        if (token instanceof Block b) {
            return b.consumedHeight();
        }
        if (token instanceof Line l) {
            return l.consumedHeight();
        }
        if (token instanceof Container c) {
            return c.consumedHeight();
        }
        return 0;
    }
}
