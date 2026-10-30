package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.PageSetup;

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
 *                                la caja (puede ser negativo); vacío = sin fragmentación (una
 *                                caja que no se parte, como un ítem de flex)
 * @param atFragmentainerStart    no hay nada antes de la caja en esta página: debe colocar algo
 *                                aunque no quepa (garantía de progreso)
 * @param contentHeight           se mide el alto del contenido: la caja ignora su {@code Height}
 *                                y su {@code Aspect ratio} (el mínimo automático de un ítem flex
 *                                en {@code column}, referencia/componentes/flex.md); no pasa a sus hijos
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

    /** Espacio de la caja raíz al empezar una página: el área útil entera. */
    public static ConstraintSpace forPage(PageSetup pageSetup) {
        double width = pageSetup.pageArea().width();
        OptionalDouble height = OptionalDouble.of(pageSetup.pageArea().height());
        return new ConstraintSpace(width, height, true, true, width, height, height, true, false);
    }

    /**
     * Espacio de los hijos en flujo de una caja: su caja de contenido es a la vez el espacio
     * disponible y la base de los porcentajes, y nada impone el tamaño.
     */
    public static ConstraintSpace forChildren(double width, OptionalDouble height, OptionalDouble remaining,
                                              boolean atStart) {
        return new ConstraintSpace(width, height, false, false, width, height, remaining, atStart, false);
    }

    /**
     * Sin tamaños impuestos, con el alto indefinido y sin fragmentación: las sondas que resuelven
     * márgenes, borde, relleno y tamaños declarados ({@link BoxModel#resolve}).
     */
    public static ConstraintSpace free(double width, double baseWidth, OptionalDouble baseHeight, boolean atStart) {
        return new ConstraintSpace(width, OptionalDouble.empty(), false, false, baseWidth, baseHeight,
                OptionalDouble.empty(), atStart, false);
    }

    /**
     * El padre impone el ancho y, si lo hay, el alto, sin fragmentación: los ítems de flex y grid
     * y las cajas {@code absolute}.
     */
    public static ConstraintSpace fixed(double width, OptionalDouble height, double baseWidth,
                                        OptionalDouble baseHeight, boolean atStart) {
        return new ConstraintSpace(width, height, true, height.isPresent(), baseWidth, baseHeight,
                OptionalDouble.empty(), atStart, false);
    }

    public boolean fragmenting() {
        return fragmentainerRemaining.isPresent();
    }

    /**
     * El mismo espacio con otro resto de página. Solo tiene sentido si el espacio fragmenta:
     * para añadir la fragmentación está {@link #fragmented}.
     */
    public ConstraintSpace withRemaining(double remaining, boolean atStart) {
        if (!fragmenting()) {
            throw new IllegalStateException("withRemaining sobre un espacio sin fragmentación");
        }
        return fragmented(remaining, atStart);
    }

    /** El mismo espacio, para medir el alto del contenido sin el {@code Height} de la caja. */
    public ConstraintSpace measuringContentHeight() {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, fragmentainerRemaining, atFragmentainerStart, true);
    }

    /**
     * El mismo espacio, sin fragmentación: una caja que no se parte (un {@code Frame} girado,
     * referencia/comun/cortes.md).
     */
    public ConstraintSpace unfragmented() {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, OptionalDouble.empty(), atFragmentainerStart, contentHeight);
    }

    /** El mismo espacio, con fragmentación aunque no la tuviera (los ítems que se parten en paralelo). */
    public ConstraintSpace fragmented(double remaining, boolean atStart) {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight, OptionalDouble.of(remaining), atStart, contentHeight);
    }
}
