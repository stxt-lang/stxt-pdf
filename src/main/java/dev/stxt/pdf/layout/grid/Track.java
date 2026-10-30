package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.model.Value;

/**
 * Pista de grid con su función mínima y máxima (referencia/componentes/grid.md). {@code auto} equivale a
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
