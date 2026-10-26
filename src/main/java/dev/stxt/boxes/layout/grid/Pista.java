package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.modelo.Valor;

/**
 * Pista de grid con su función mínima y máxima (módulo 6). {@code auto} equivale a
 * {@code minmax(auto, auto)} y {@code 1fr} a {@code minmax(auto, 1fr)}: una fracción nunca baja
 * del min-content de su contenido.
 */
public record Pista(Valor min, Valor max) {

    public static Pista de(Valor v) {
        if (v instanceof Valor.MinMax mm) {
            return new Pista(mm.min(), mm.max());
        }
        if (v instanceof Valor.Fraccion) {
            return new Pista(Valor.AUTO, v);
        }
        return new Pista(v, v);
    }

    public boolean flexible() {
        return max instanceof Valor.Fraccion;
    }

    public double fraccion() {
        return max instanceof Valor.Fraccion f ? f.fr() : 0;
    }

    /** La función mínima depende del contenido. */
    boolean minIntrinseco() {
        return min.esAuto() || min.esIntrinseco();
    }

    /** La función máxima depende del contenido. */
    boolean maxIntrinseco() {
        return max.esAuto() || max.esIntrinseco();
    }
}
