package dev.stxt.boxes.modelo;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Valor <em>especificado</em> de una propiedad, tal como lo escribe el autor
 * (ESPECIFICACION.md §3). El layout lo convierte en un valor <em>usado</em>, en puntos,
 * con {@link #resolver(OptionalDouble)}.
 *
 * <p>Se conservan separados los valores especificados de los usados porque flex y grid
 * necesitan saber si un valor era {@code auto} en origen (módulo 1).</p>
 */
public sealed interface Valor {

    /** Longitud absoluta en puntos tipográficos. */
    record Puntos(double pt) implements Valor {}

    /** Porcentaje del containing block (o del tamaño de fuente en {@code Interlineado}). */
    record Porcentaje(double porcentaje) implements Valor {}

    /** Fracción de espacio libre en pistas de grid ({@code 1fr}). */
    record Fraccion(double fr) implements Valor {}

    /** {@code minmax(min, max)} en pistas de grid. */
    record MinMax(Valor min, Valor max) implements Valor {}

    /** Funciones {@code min()}, {@code max()} y {@code clamp()}. */
    record Funcion(TipoFuncion tipo, List<Valor> argumentos) implements Valor {}

    /** Palabras clave que dependen del contenido. */
    enum Palabra implements Valor { AUTO, MIN_CONTENT, MAX_CONTENT, FIT_CONTENT }

    enum TipoFuncion { MIN, MAX, CLAMP }

    Valor AUTO = Palabra.AUTO;

    static Valor pt(double puntos) {
        return new Puntos(puntos);
    }

    default boolean esAuto() {
        return this == Palabra.AUTO;
    }

    /** {@code min-content}, {@code max-content} o {@code fit-content}. */
    default boolean esIntrinseco() {
        return this instanceof Palabra p && p != Palabra.AUTO;
    }

    /**
     * Resuelve el valor a puntos.
     *
     * @param base tamaño de referencia (normalmente del containing block); vacío si es indefinido
     * @return los puntos, o vacío si el valor depende del contenido ({@code auto}, intrínsecos,
     *         {@code fr}) o es un porcentaje sobre una base indefinida (ESPECIFICACION.md §5.9)
     */
    default OptionalDouble resolver(OptionalDouble base) {
        if (this instanceof Puntos p) {
            return OptionalDouble.of(p.pt());
        }
        if (this instanceof Porcentaje p) {
            return base.isPresent()
                    ? OptionalDouble.of(base.getAsDouble() * p.porcentaje() / 100.0)
                    : OptionalDouble.empty();
        }
        if (this instanceof Funcion f) {
            double[] v = new double[f.argumentos().size()];
            for (int i = 0; i < v.length; i++) {
                OptionalDouble r = f.argumentos().get(i).resolver(base);
                if (r.isEmpty()) {
                    return OptionalDouble.empty();
                }
                v[i] = r.getAsDouble();
            }
            return switch (f.tipo()) {
                case MIN -> OptionalDouble.of(Arrays.stream(v).min().orElseThrow());
                case MAX -> OptionalDouble.of(Arrays.stream(v).max().orElseThrow());
                case CLAMP -> OptionalDouble.of(Math.max(v[0], Math.min(v[1], v[2])));
            };
        }
        return OptionalDouble.empty();
    }

    default OptionalDouble resolver(double base) {
        return resolver(OptionalDouble.of(base));
    }
}
