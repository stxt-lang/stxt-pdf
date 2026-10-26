package dev.stxt.boxes.layout.linea;

import dev.stxt.boxes.arbol.Estilo;
import dev.stxt.boxes.layout.IntrinsicSizes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineBreakerTest {

    // Factor 1 y tamaño 1: cada carácter mide 1 pt; un espacio también.
    private final TextMeasurer medidor = new MetricaFija(1);

    @Test
    void primerAjusteVoraz() {
        List<String> l = LineBreaker.partir("uno dos tres cuatro", 8, medidor, "F", 1);
        assertEquals(List.of("uno dos", "tres", "cuatro"), l);
    }

    @Test
    void saltosSimplesSonEspaciosYLineaEnBlancoSeparaParrafos() {
        List<String> l = LineBreaker.partir("uno\ndos\n\ntres", 100, medidor, "F", 1);
        assertEquals(List.of("uno dos", "tres"), l);
    }

    @Test
    void palabraMasAnchaQueLaLineaDesborda() {
        assertEquals(List.of("a", "larguísima", "b"), LineBreaker.partir("a larguísima b", 4, medidor, "F", 1));
    }

    @Test
    void intrinsecos() {
        IntrinsicSizes s = LineBreaker.intrinsecos("uno dos tres", medidor, "F", 1);
        assertEquals(4, s.minContent(), 1e-9);
        assertEquals(12, s.maxContent(), 1e-9);
        assertEquals(8, s.fitContent(8), 1e-9);
    }

    @Test
    void alineacion() {
        assertEquals(15, LineBreaker.desplazamiento(70, 100, Estilo.AlineacionTexto.CENTRO), 1e-9);
        assertEquals(30, LineBreaker.desplazamiento(70, 100, Estilo.AlineacionTexto.DERECHA), 1e-9);
    }
}
