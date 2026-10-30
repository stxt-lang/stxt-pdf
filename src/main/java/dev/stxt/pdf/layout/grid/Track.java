package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.model.Value;

/**
 * Pista de grid con su función mínima y máxima (referencia/componentes/grid.md). {@code auto} equivale a
 * {@code between(auto, auto)} y {@code *} a {@code between(auto, *)}: una fracción nunca baja
 * del min-content de su contenido.
 */
public record Track(Value min, Value max) {

    public static Track of(Value v) {
        if (v instanceof Value.Between b) {
            return new Track(b.min(), b.max());
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
        return max instanceof Value.Fraction f ? f.weight() : 0;
    }

    /**
     * La misma pista sin base para los porcentajes: los porcentajes se comportan como {@code auto}
     * (referencia/componentes/grid.md).
     */
    Track withoutPercentBase() {
        return new Track(definiteOrAuto(min), definiteOrAuto(max));
    }

    private static Value definiteOrAuto(Value v) {
        return v instanceof Value.Percentage ? Value.AUTO : v;
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
