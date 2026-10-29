package dev.stxt.boxes.model;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Valor <em>especificado</em> de una propiedad, tal como lo escribe el autor
 * (ESPECIFICACION.md §3). El layout lo convierte en un valor <em>usado</em>, en puntos,
 * con {@link #resolve(OptionalDouble)}.
 *
 * <p>Se conservan separados los valores especificados de los usados porque flex y grid
 * necesitan saber si un valor era {@code auto} en origen (ESPECIFICACION.md §3).</p>
 */
public sealed interface Value {

    /** Longitud absoluta en puntos tipográficos. */
    record Points(double pt) implements Value {}

    /** Porcentaje del containing block (o del tamaño de fuente en {@code Line height}). */
    record Percentage(double percent) implements Value {}

    /** Fracción de espacio libre en pistas de grid ({@code 1fr}). */
    record Fraction(double fr) implements Value {}

    /** {@code minmax(min, max)} en pistas de grid. */
    record MinMax(Value min, Value max) implements Value {}

    /** Funciones {@code min()}, {@code max()} y {@code clamp()}. */
    record Function(FunctionType type, List<Value> arguments) implements Value {}

    /** Palabras clave que dependen del contenido. */
    enum Keyword implements Value { AUTO, MIN_CONTENT, MAX_CONTENT, FIT_CONTENT }

    enum FunctionType { MIN, MAX, CLAMP }

    Value AUTO = Keyword.AUTO;

    static Value pt(double points) {
        return new Points(points);
    }

    default boolean isAuto() {
        return this == Keyword.AUTO;
    }

    /** {@code min-content}, {@code max-content} o {@code fit-content}. */
    default boolean isIntrinsic() {
        return this instanceof Keyword k && k != Keyword.AUTO;
    }

    /**
     * Resuelve el valor a puntos.
     *
     * @param base tamaño de referencia (normalmente del containing block); vacío si es indefinido
     * @return los puntos, o vacío si el valor depende del contenido ({@code auto}, intrínsecos,
     *         {@code fr}) o es un porcentaje sobre una base indefinida (ESPECIFICACION.md §5.9)
     */
    default OptionalDouble resolve(OptionalDouble base) {
        if (this instanceof Points p) {
            return OptionalDouble.of(p.pt());
        }
        if (this instanceof Percentage p) {
            return base.isPresent()
                    ? OptionalDouble.of(base.getAsDouble() * p.percent() / 100.0)
                    : OptionalDouble.empty();
        }
        if (this instanceof Function f) {
            double[] v = new double[f.arguments().size()];
            for (int i = 0; i < v.length; i++) {
                OptionalDouble r = f.arguments().get(i).resolve(base);
                if (r.isEmpty()) {
                    return OptionalDouble.empty();
                }
                v[i] = r.getAsDouble();
            }
            return switch (f.type()) {
                case MIN -> OptionalDouble.of(Arrays.stream(v).min().orElseThrow());
                case MAX -> OptionalDouble.of(Arrays.stream(v).max().orElseThrow());
                case CLAMP -> OptionalDouble.of(Math.max(v[0], Math.min(v[1], v[2])));
            };
        }
        return OptionalDouble.empty();
    }

    default OptionalDouble resolve(double base) {
        return resolve(OptionalDouble.of(base));
    }
}
