package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.model.Value;

/**
 * Pista de grid con su función mínima y máxima (módulo 6). {@code auto} equivale a
 * {@code minmax(auto, auto)} y {@code 1fr} a {@code minmax(auto, 1fr)}: una fracción nunca baja
 * del min-content de su contenido.
 */
public record Track(Value min, Value max) {

    public static Track of(Value v) {
        if (v instanceof Value.MinMax mm) {
            return new Track(mm.min(), mm.max());
        }
        if (v instanceof Value.Fraction) {
            return new Track(Value.AUTO, v);
        }
        return new Track(v, v);
    }

    public boolean flexible() {
        return max instanceof Value.Fraction;
    }

    public double fraction() {
        return max instanceof Value.Fraction f ? f.fr() : 0;
    }

    /** La función mínima depende del contenido. */
    boolean minIntrinsic() {
        return min.isAuto() || min.isIntrinsic();
    }

    /** La función máxima depende del contenido. */
    boolean maxIntrinsic() {
        return max.isAuto() || max.isIntrinsic();
    }
}
