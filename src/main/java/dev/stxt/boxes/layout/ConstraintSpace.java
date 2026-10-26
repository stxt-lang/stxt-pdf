package dev.stxt.boxes.layout;

import dev.stxt.boxes.arbol.Medio;

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
    public static ConstraintSpace paraPagina(Medio medio) {
        double ancho = medio.areaUtil().ancho();
        OptionalDouble alto = medio.alturaFragmentainer();
        return new ConstraintSpace(ancho, alto, true, alto.isPresent(), ancho, alto, alto, true);
    }

    public boolean fragmentando() {
        return fragmentainerRemaining.isPresent();
    }

    public ConstraintSpace conRestante(double restante, boolean alInicio) {
        return new ConstraintSpace(availableWidth, availableHeight, fixedWidth, fixedHeight,
                percentBaseWidth, percentBaseHeight,
                fragmentando() ? OptionalDouble.of(restante) : OptionalDouble.empty(), alInicio);
    }
}
