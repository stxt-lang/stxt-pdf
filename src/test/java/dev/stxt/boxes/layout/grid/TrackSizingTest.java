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
