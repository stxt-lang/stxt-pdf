package dev.stxt.boxes.layout.linea;

import dev.stxt.boxes.arbol.Estilo;
import dev.stxt.boxes.layout.IntrinsicSizes;

import java.util.ArrayList;
import java.util.List;

/**
 * Reparte un texto en líneas con el algoritmo voraz (<em>first-fit</em>) de los navegadores
 * (módulo 2, ESPECIFICACION.md §5.4).
 *
 * <p>Tratamiento del espacio en blanco: los saltos de línea simples del bloque {@code Texto} y
 * las secuencias de espacios valen un espacio; una línea en blanco separa párrafos y fuerza un
 * salto. Una palabra más ancha que la línea ocupa una línea propia y desborda.</p>
 */
public final class LineBreaker {

    private LineBreaker() {
    }

    /** Líneas del texto para un ancho disponible. */
    public static List<String> partir(String texto, double disponible, TextMeasurer medidor,
                                      String fuente, double tamano) {
        List<String> lineas = new ArrayList<>();
        for (List<String> parrafo : parrafos(texto)) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo) {
                String candidata = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (actual.length() == 0 || medidor.ancho(candidata, fuente, tamano) <= disponible + 1e-9) {
                    actual.setLength(0);
                    actual.append(candidata);
                } else {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                    actual.append(palabra);
                }
            }
            if (actual.length() > 0) {
                lineas.add(actual.toString());
            }
        }
        return lineas;
    }

    /** min-content (palabra más ancha) y max-content (párrafo más largo sin partir). */
    public static IntrinsicSizes intrinsecos(String texto, TextMeasurer medidor, String fuente, double tamano) {
        double min = 0;
        double max = 0;
        for (List<String> parrafo : parrafos(texto)) {
            for (String palabra : parrafo) {
                min = Math.max(min, medidor.ancho(palabra, fuente, tamano));
            }
            max = Math.max(max, medidor.ancho(String.join(" ", parrafo), fuente, tamano));
        }
        return new IntrinsicSizes(min, max);
    }

    /** Desplazamiento horizontal de una línea según la alineación. */
    public static double desplazamiento(double anchoLinea, double disponible,
                                        Estilo.AlineacionTexto alinear) {
        double sobrante = Math.max(0, disponible - anchoLinea);
        return switch (alinear) {
            case IZQUIERDA, JUSTIFICADO -> 0;
            case DERECHA -> sobrante;
            case CENTRO -> sobrante / 2;
        };
    }

    private static List<List<String>> parrafos(String texto) {
        List<List<String>> resultado = new ArrayList<>();
        for (String bloque : texto.split("\\n\\s*\\n")) {
            String limpio = bloque.strip();
            if (!limpio.isEmpty()) {
                resultado.add(List.of(limpio.split("\\s+")));
            }
        }
        return resultado;
    }
}
