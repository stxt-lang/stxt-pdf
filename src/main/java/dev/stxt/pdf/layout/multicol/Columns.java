package dev.stxt.pdf.layout.multicol;

import static dev.stxt.pdf.model.Tolerance.EPSILON;

/**
 * Número y ancho de las columnas de un {@code Multicol} (referencia/componentes/multicol.md), con el
 * pseudoalgoritmo de CSS ({@code column-count} y {@code column-width}):
 *
 * <ul>
 *   <li>solo {@code Count}: ese número de columnas, que se reparten el ancho;</li>
 *   <li>solo {@code Column width}: tantas columnas de al menos ese ancho como quepan, y como
 *       mínimo una;</li>
 *   <li>los dos: las que quepan, sin pasar de {@code Count}.</li>
 * </ul>
 *
 * <p>Las columnas se ensanchan para llenar el ancho de contenido entre los huecos. Si los
 * huecos no dejan sitio, miden 0 y el contenido desborda.</p>
 *
 * @param count número de columnas
 * @param width ancho de cada columna
 * @param gap   hueco entre dos columnas
 */
record Columns(int count, double width, double gap) {

    /**
     * @param count     {@code Count}; {@code null} si no se declara
     * @param width     {@code Column width}; {@code null} si no se declara
     * @param gap       {@code Column gap}
     * @param available ancho de la caja de contenido del {@code Multicol}
     */
    static Columns of(Integer count, Double width, double gap, double available) {
        int n;
        if (width == null) {
            n = count;
        } else {
            n = Math.max(1, (int) Math.floor((available + gap + EPSILON) / (width + gap)));
            if (count != null) {
                n = Math.min(n, count);
            }
        }
        return new Columns(n, Math.max(0, (available - (n - 1) * gap) / n), gap);
    }

    /** Posición de la columna {@code column} (desde 0), desde la caja de contenido del {@code Multicol}. */
    double x(int column) {
        return column * (width + gap);
    }
}
