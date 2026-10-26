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
 * @param fragmentainerRemaining  espacio que queda en la página actual desde el borde superior de
 *                                la caja (puede ser negativo); vacío = sin fragmentación (modo
 *                                continuo, o una caja que no se parte, como un ítem de flex)
 * @param atFragmentainerStart    no hay nada antes de la caja en esta página: debe colocar algo
 *                                aunque no quepa (garantía de progreso)
 * @param contentHeight           se mide el alto del contenido: la caja ignora su {@code Height}
 *                                y su {@code Aspect ratio} (el mínimo automático de un ítem flex
 *                                en {@code column}, §5.7); no pasa a sus hijos
 */
public record ConstraintSpace(
        double availableWidth,
        OptionalDouble availableHeight,
        boolean fixedWidth,
        boolean fixedHeight,
        double percentBaseWidth,
        OptionalDouble percentBaseHeight,
        OptionalDouble fragmentainerRemaining,
        boolean atFragmentainerStart,
        boolean contentHeight) {

    public ConstraintSpace(double availableWidth, OptionalDouble availableHeight, boolean fixedWidth,
                           boolean fixedHeight, double percentBaseWidth, OptionalDouble percentBaseHeight,
                           OptionalDouble fragmentainerRemaining, boolean atFragmentainerStart) {
        this(availableWidth, availableHeight, fixedWidth, fixedHeight, percentBaseWidth, percentBaseHeight,
                fragmentainerRemaining, atFragmentainerStart, false);
    }

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

    /** El mismo espacio, para medir el alto del contenido sin el {@code Height} de la caja. */
    public ConstraintSpace measuringContentHeight() {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, fragmentainerRemaining, atFragmentainerStart, true);
    }

    /** El mismo espacio, con fragmentación aunque no la tuviera (los ítems que se parten en paralelo). */
    public ConstraintSpace fragmented(double remaining, boolean atStart) {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, OptionalDouble.of(remaining), atStart);
    }
}
