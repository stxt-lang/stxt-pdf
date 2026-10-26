package dev.stxt.boxes.arbol;

import dev.stxt.boxes.modelo.Insets;
import dev.stxt.boxes.modelo.Rect;

import java.util.OptionalDouble;

/**
 * El medio de salida (ESPECIFICACION.md §4.1). Define el containing block inicial: el área útil.
 *
 * @param tipo   continuo (una franja de altura infinita) o paginado (una secuencia de páginas)
 * @param ancho  ancho de la página o de la franja, en pt
 * @param alto   alto de la página; obligatorio en modo paginado
 * @param margen márgenes de página, en pt
 */
public record Medio(Tipo tipo, double ancho, OptionalDouble alto, Insets margen) {

    public enum Tipo { CONTINUO, PAGINADO }

    public boolean paginado() {
        return tipo == Tipo.PAGINADO;
    }

    /** Área útil: el containing block inicial y el de las cajas {@code fija}. */
    public Rect areaUtil() {
        double util = paginado()
                ? alto.orElseThrow() - margen.vertical()
                : Double.POSITIVE_INFINITY;
        return new Rect(margen.izquierda(), margen.arriba(), ancho - margen.horizontal(), util);
    }

    /** Altura de cada fragmentainer; vacía en modo continuo. */
    public OptionalDouble alturaFragmentainer() {
        return paginado() ? OptionalDouble.of(areaUtil().alto()) : OptionalDouble.empty();
    }
}
