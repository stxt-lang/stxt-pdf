package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.modelo.ValorParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TrackSizingTest {

    /** Contenido por columna del simulador del módulo 6: (min, max). */
    private static final double[][] CONTENIDO = {{60, 140}, {50, 90}, {80, 220}, {40, 120}};

    private static double[] columnas(String pistas, double disponible) {
        List<Pista> p = ValorParser.lista(pistas).stream().map(Pista::de).toList();
        List<AportacionItem> items = new ArrayList<>();
        for (int i = 0; i < p.size(); i++) {
            items.add(new AportacionItem(i, 1, CONTENIDO[i % 4][0], CONTENIDO[i % 4][1]));
        }
        return TrackSizing.dimensionar(p, items, disponible, 20);
    }

    @Test
    void fijaAutoYFraccion() {
        // 475 para pistas: 150 fija, auto llega a su max-content (90), 1fr recibe 235.
        assertArrayEquals(new double[]{150, 90, 235}, columnas("150 auto 1fr", 515), 1e-9);
    }

    @Test
    void fraccionNoBajaDeSuMinContent() {
        // 210 para pistas: 70 por fracción; la tercera (min 80) se congela.
        assertArrayEquals(new double[]{65, 65, 80}, columnas("1fr 1fr 1fr", 250), 1e-9);
    }

    @Test
    void sinFraccionesSeEstiranLasAuto() {
        // 495: base 140 + 90 = 230; sobran 265, 132.5 para cada una.
        assertArrayEquals(new double[]{272.5, 222.5}, columnas("auto auto", 515), 1e-9);
    }

    @Test
    void ejercicioModulo6() {
        List<Pista> p = ValorParser.lista("auto 1fr 1fr").stream().map(Pista::de).toList();
        List<AportacionItem> items = List.of(
                new AportacionItem(0, 1, 60, 100),
                new AportacionItem(1, 1, 40, 300),
                new AportacionItem(2, 1, 40, 300));
        assertArrayEquals(new double[]{100, 187.5, 187.5}, TrackSizing.dimensionar(p, items, 515, 20), 1e-9);
    }

    @Test
    void itemQueOcupaVariasPistas() {
        // Dos columnas auto vacías y un ítem de 2 columnas con min 100 y max 300:
        // el exceso se reparte entre ambas; el resto del espacio estira las auto.
        List<Pista> p = ValorParser.lista("auto auto").stream().map(Pista::de).toList();
        double[] r = TrackSizing.dimensionar(p, List.of(new AportacionItem(0, 2, 100, 300)), 1000, 0);
        assertArrayEquals(new double[]{500, 500}, r, 1e-9);
        double[] estrecho = TrackSizing.dimensionar(p, List.of(new AportacionItem(0, 2, 100, 300)), 120, 0);
        assertArrayEquals(new double[]{60, 60}, estrecho, 1e-9);
    }
}
