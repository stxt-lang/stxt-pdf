package dev.stxt.boxes.layout;

import dev.stxt.boxes.tree.Media;

import java.util.OptionalDouble;

/**
 * Entrada inmutable del layout de una caja (módulos 7 y 8). Al ser un {@code record}, sirve como
 * parte de la clave de {@link LayoutCache}.
 *
 * @param availableWidth          ancho disponible; siempre conocido
 * @param availableHeight         alto disponible; vacío = indefinido
 * @param fixedWidth              el padre (flex o grid) impone el ancho exacto
 * @param fixedHeight             el padre impone el alto exacto (por ejemplo, al estirar)
 * @param percentBaseWidth        base para porcentajes horizontales
 * @param percentBaseHeight       base para porcentajes verticales; vacío = indefinida
 * @param fragmentainerRemaining  espacio que queda en la página actual; vacío = modo continuo
 * @param atFragmentainerStart    la caja empieza al principio de una página (garantía de progreso)
 */
public record ConstraintSpace(
        double availableWidth,
        OptionalDouble availableHeight,
        boolean fixedWidth,
        boolean fixedHeight,
        double percentBaseWidth,
        OptionalDouble percentBaseHeight,
        OptionalDouble fragmentainerRemaining,
        boolean atFragmentainerStart) {

    /** Espacio de la caja raíz al empezar una página (o el documento, en modo continuo). */
    public static ConstraintSpace forPage(Media media) {
        double width = media.pageArea().width();
        OptionalDouble height = media.fragmentainerHeight();
        return new ConstraintSpace(width, height, true, height.isPresent(), width, height, height, true);
    }

    public boolean fragmenting() {
        return fragmentainerRemaining.isPresent();
    }

    public ConstraintSpace withRemaining(double remaining, boolean atStart) {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight,
                fragmenting() ? OptionalDouble.of(remaining) : OptionalDouble.empty(), atStart);
    }
}
