package dev.stxt.boxes.layout;

import dev.stxt.boxes.arbol.Estilo;
import dev.stxt.boxes.modelo.Insets;
import dev.stxt.boxes.modelo.Valor;

import java.util.OptionalDouble;

/**
 * Reglas comunes de dimensionado (módulos 1 y 7, ESPECIFICACION.md §5.9).
 */
public final class Dimensiones {

    private Dimensiones() {
    }

    /** Aplica primero el máximo y después el mínimo: si chocan, gana el mínimo. */
    public static double limitar(double provisional, double minimo, OptionalDouble maximo) {
        double v = maximo.isPresent() ? Math.min(provisional, maximo.getAsDouble()) : provisional;
        return Math.max(v, minimo);
    }

    /**
     * Convierte un ancho o alto especificado en tamaño de caja de borde, según {@code Medida}.
     * Con {@code Medida: contenido}, el valor declarado es el del contenido y se le suman
     * relleno y borde.
     */
    public static double aCajaDeBorde(double declarado, Estilo.Medida medida, double rellenoYBorde) {
        return medida == Estilo.Medida.CONTENIDO ? declarado + rellenoYBorde : declarado;
    }

    /**
     * Ancho de caja de borde de una caja en flujo de bloque con {@code Ancho: auto}: ocupa el
     * containing block menos sus márgenes horizontales.
     */
    public static double anchoAutoEnFlujo(double anchoContainingBlock, Insets margen) {
        return Math.max(0, anchoContainingBlock - margen.horizontal());
    }

    /**
     * Márgenes horizontales de una caja en flujo de bloque con ancho conocido: si ambos son
     * {@code auto}, el sobrante se reparte y la caja queda centrada; si solo uno es {@code auto},
     * absorbe el sobrante.
     *
     * @return {izquierdo, derecho}
     */
    public static double[] margenesHorizontales(double anchoContainingBlock, double anchoBorde,
                                                Valor margenIzq, Valor margenDer) {
        double izq = margenIzq.resolver(anchoContainingBlock).orElse(0);
        double der = margenDer.resolver(anchoContainingBlock).orElse(0);
        double sobrante = anchoContainingBlock - anchoBorde - izq - der;
        if (margenIzq.esAuto() && margenDer.esAuto()) {
            double cada = Math.max(0, sobrante) / 2;
            return new double[]{cada, cada};
        }
        if (margenIzq.esAuto()) {
            return new double[]{Math.max(0, sobrante), der};
        }
        if (margenDer.esAuto()) {
            return new double[]{izq, Math.max(0, sobrante)};
        }
        return new double[]{izq, der};
    }
}
