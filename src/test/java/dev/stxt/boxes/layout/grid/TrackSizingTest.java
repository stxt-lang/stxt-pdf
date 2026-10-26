package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.model.ValueParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TrackSizingTest {

    /** Contenido por columna del simulador del módulo 6: (min, max). */
    private static final double[][] CONTENT = {{60, 140}, {50, 90}, {80, 220}, {40, 120}};

    private static double[] columns(String tracks, double available) {
        List<Track> t = ValueParser.list(tracks).stream().map(Track::of).toList();
        List<ItemContribution> items = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            items.add(new ItemContribution(i, 1, CONTENT[i % 4][0], CONTENT[i % 4][1]));
        }
        return TrackSizing.size(t, items, available, 20);
    }

    @Test
    void fixedAutoAndFraction() {
        // 475 para pistas: 150 fija, auto llega a su max-content (90), 1fr recibe 235.
        assertArrayEquals(new double[]{150, 90, 235}, columns("150 auto 1fr", 515), 1e-9);
    }

    @Test
    void fractionNeverBelowItsMinContent() {
        // 210 para pistas: 70 por fracción; la tercera (min 80) se congela.
        assertArrayEquals(new double[]{65, 65, 80}, columns("1fr 1fr 1fr", 250), 1e-9);
    }

    @Test
    void withoutFractionsAutoTracksStretch() {
        // 495: base 140 + 90 = 230; sobran 265, 132.5 para cada una.
        assertArrayEquals(new double[]{272.5, 222.5}, columns("auto auto", 515), 1e-9);
    }

    @Test
    void percentagesWithoutBaseAreAuto() {
        // Sin base, 50% y min(300, 50%) son auto (max-content 140 y 90); minmax(100, 50%)
        // pasa a minmax(100, auto) (max-content 220); 120 es fija.
        assertArrayEquals(new double[]{140, 90, 220, 120},
                columns("50% min(300, 50%) minmax(100, 50%) 120", Double.POSITIVE_INFINITY), 1e-9);
    }

    @Test
    void percentagesWithBase() {
        // 50% de 515 = 257.5 (los huecos no se descuentan); de los 495 para pistas, el resto
        // para la auto (estirada).
        assertArrayEquals(new double[]{257.5, 237.5}, columns("50% auto", 515), 1e-9);
    }

    @Test
    void percentagesIgnoreGaps() {
        // Como en CSS, 50% 50% con hueco desborda el hueco.
        assertArrayEquals(new double[]{257.5, 257.5}, columns("50% 50%", 515), 1e-9);
    }

    private static double[] spanning(String tracks, double available, ItemContribution... items) {
        List<Track> t = ValueParser.list(tracks).stream().map(Track::of).toList();
        return TrackSizing.size(t, List.of(items), available, 0);
    }

    @Test
    void itemCrossingFractionsGrowsThem() {
        // El ítem necesita 300 en dos fracciones: 150 cada una, aunque solo haya 200.
        assertArrayEquals(new double[]{150, 150},
                spanning("1fr 1fr", 200, new ItemContribution(0, 2, 300, 300)), 1e-9);
        assertArrayEquals(new double[]{150, 150},
                spanning("1fr 1fr", Double.POSITIVE_INFINITY, new ItemContribution(0, 2, 300, 300)), 1e-9);
    }

    @Test
    void itemCrossingFractionsInProportionToFr() {
        assertArrayEquals(new double[]{200, 100},
                spanning("2fr 1fr", 200, new ItemContribution(0, 2, 300, 300)), 1e-9);
        // Con fr que suman 0.75: 0.75 del exceso en proporción y el resto a partes iguales.
        assertArrayEquals(new double[]{187.5, 112.5},
                spanning("0.5fr 0.25fr", 200, new ItemContribution(0, 2, 300, 300)), 1e-9);
    }

    @Test
    void itemCrossingFractionsOnlyGrowsTheFractions() {
        // La auto se queda con su ítem (50); la fracción toma los 250 que faltan.
        assertArrayEquals(new double[]{50, 250}, spanning("auto 1fr", 200,
                new ItemContribution(0, 1, 50, 50), new ItemContribution(0, 2, 300, 300)), 1e-9);
    }

    @Test
    void module6Exercise() {
        List<Track> t = ValueParser.list("auto 1fr 1fr").stream().map(Track::of).toList();
        List<ItemContribution> items = List.of(
                new ItemContribution(0, 1, 60, 100),
                new ItemContribution(1, 1, 40, 300),
                new ItemContribution(2, 1, 40, 300));
        assertArrayEquals(new double[]{100, 187.5, 187.5}, TrackSizing.size(t, items, 515, 20), 1e-9);
    }

    @Test
    void itemSpanningSeveralTracks() {
        // Dos columnas auto vacías y un ítem de 2 columnas con min 100 y max 300:
        // el exceso se reparte entre ambas; el resto del espacio estira las auto.
        List<Track> t = ValueParser.list("auto auto").stream().map(Track::of).toList();
        double[] r = TrackSizing.size(t, List.of(new ItemContribution(0, 2, 100, 300)), 1000, 0);
        assertArrayEquals(new double[]{500, 500}, r, 1e-9);
        double[] narrow = TrackSizing.size(t, List.of(new ItemContribution(0, 2, 100, 300)), 120, 0);
        assertArrayEquals(new double[]{60, 60}, narrow, 1e-9);
    }
}
