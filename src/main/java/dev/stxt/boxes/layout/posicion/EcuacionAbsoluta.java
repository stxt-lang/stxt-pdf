package dev.stxt.boxes.layout.posicion;

import dev.stxt.boxes.layout.IntrinsicSizes;

import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

/**
 * Resuelve la posición y el tamaño de una caja {@code absoluta} o {@code fija} en un eje
 * (módulo 4, ESPECIFICACION.md §5.5). Siempre se cumple:
 *
 * <pre>inicio + margen inicio + tamaño + margen fin + fin = tamaño del containing block</pre>
 *
 * <p>Un {@link OptionalDouble} vacío representa {@code auto}. El tamaño es el de la caja de borde.</p>
 */
public final class EcuacionAbsoluta {

    /** Solución de la ecuación en un eje. */
    public record Solucion(double inicio, double margenInicio, double tamano, double margenFin, double fin) {
    }

    private EcuacionAbsoluta() {
    }

    /**
     * Eje horizontal. Un ancho {@code auto} que no se puede despejar se ajusta al contenido
     * (<em>shrink-to-fit</em>). Si está sobre-restringida, se ignora {@code derecha}.
     */
    public static Solucion horizontal(OptionalDouble izquierda, OptionalDouble ancho, OptionalDouble derecha,
                                      OptionalDouble margenIzq, OptionalDouble margenDer,
                                      double anchoContainingBlock, double izquierdaEstatica,
                                      IntrinsicSizes contenido) {
        return resolver(izquierda, ancho, derecha, margenIzq, margenDer, anchoContainingBlock,
                izquierdaEstatica, contenido::fitContent);
    }

    /**
     * Eje vertical. Un alto {@code auto} que no se puede despejar toma la altura del contenido.
     * Si está sobre-restringida, se ignora {@code abajo}.
     */
    public static Solucion vertical(OptionalDouble arriba, OptionalDouble alto, OptionalDouble abajo,
                                    OptionalDouble margenArriba, OptionalDouble margenAbajo,
                                    double altoContainingBlock, double arribaEstatica,
                                    double altoContenido) {
        return resolver(arriba, alto, abajo, margenArriba, margenAbajo, altoContainingBlock,
                arribaEstatica, disponible -> altoContenido);
    }

    private static Solucion resolver(OptionalDouble inicio, OptionalDouble tamano, OptionalDouble fin,
                                     OptionalDouble mInicio, OptionalDouble mFin, double cb,
                                     double inicioEstatico, DoubleUnaryOperator tamanoAuto) {
        double mi = mInicio.orElse(0);
        double mf = mFin.orElse(0);

        // 1. Todo auto: posición estática y tamaño automático.
        if (inicio.isEmpty() && tamano.isEmpty() && fin.isEmpty()) {
            double i = inicioEstatico;
            double t = tamanoAuto.applyAsDouble(cb - i - mi - mf);
            return new Solucion(i, mi, t, mf, cb - i - mi - t - mf);
        }

        // 2. Nada auto: los márgenes auto absorben el sobrante; si no hay, se ignora el fin.
        if (inicio.isPresent() && tamano.isPresent() && fin.isPresent()) {
            double i = inicio.getAsDouble();
            double t = tamano.getAsDouble();
            double f = fin.getAsDouble();
            if (mInicio.isEmpty() && mFin.isEmpty()) {
                double sobrante = cb - i - t - f;
                if (sobrante >= 0) {
                    return new Solucion(i, sobrante / 2, t, sobrante / 2, f);
                }
                return new Solucion(i, 0, t, sobrante, f);
            }
            if (mInicio.isEmpty()) {
                return new Solucion(i, cb - i - t - f - mf, t, mf, f);
            }
            if (mFin.isEmpty()) {
                return new Solucion(i, mi, t, cb - i - mi - t - f, f);
            }
            return new Solucion(i, mi, t, mf, cb - i - mi - t - mf); // sobre-restringida
        }

        // 3. Algún auto (los márgenes auto valen 0).
        if (inicio.isEmpty() && tamano.isEmpty()) {
            double f = fin.getAsDouble();
            double t = tamanoAuto.applyAsDouble(cb - f - mi - mf);
            return new Solucion(cb - f - mi - mf - t, mi, t, mf, f);
        }
        if (inicio.isEmpty() && fin.isEmpty()) {
            double i = inicioEstatico;
            double t = tamano.getAsDouble();
            return new Solucion(i, mi, t, mf, cb - i - mi - t - mf);
        }
        if (tamano.isEmpty() && fin.isEmpty()) {
            double i = inicio.getAsDouble();
            double t = tamanoAuto.applyAsDouble(cb - i - mi - mf);
            return new Solucion(i, mi, t, mf, cb - i - mi - t - mf);
        }
        if (inicio.isEmpty()) {
            double t = tamano.getAsDouble();
            double f = fin.getAsDouble();
            return new Solucion(cb - t - f - mi - mf, mi, t, mf, f);
        }
        if (tamano.isEmpty()) {
            double i = inicio.getAsDouble();
            double f = fin.getAsDouble();
            double t = Math.max(0, cb - i - f - mi - mf);
            return new Solucion(i, mi, t, mf, cb - i - mi - t - mf);
        }
        double i = inicio.getAsDouble();
        double t = tamano.getAsDouble();
        return new Solucion(i, mi, t, mf, cb - i - mi - t - mf);
    }
}
