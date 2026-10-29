package dev.stxt.boxes.layout;

/**
 * Márgenes de los descendientes que tocan el borde superior o inferior de una caja y por eso
 * colapsan con los márgenes propios de la caja, fuera de ella (referencia/componentes/block.md: el margen
 * del hijo "sale" del padre). No incluyen los márgenes propios de la caja: los resuelve el padre,
 * que es quien la coloca.
 *
 * @param top              márgenes que salen por arriba (primer hijo, y los vacíos anteriores)
 * @param bottom           márgenes que salen por abajo (último hijo)
 * @param collapsesThrough la caja está vacía y sus márgenes superior e inferior colapsan entre sí;
 *                         entonces todo va en {@code top} y {@code bottom} está vacío
 */
public record AdjoiningMargins(MarginStrut top, MarginStrut bottom, boolean collapsesThrough) {

    /** Nada sale de la caja: contextos de formato propios, cajas con borde o relleno… */
    public static final AdjoiningMargins NONE = new AdjoiningMargins(MarginStrut.EMPTY, MarginStrut.EMPTY, false);

    /** Caja vacía que no aporta márgenes de descendientes, pero deja colapsar los suyos. */
    public static final AdjoiningMargins EMPTY_BOX = new AdjoiningMargins(MarginStrut.EMPTY, MarginStrut.EMPTY, true);
}
