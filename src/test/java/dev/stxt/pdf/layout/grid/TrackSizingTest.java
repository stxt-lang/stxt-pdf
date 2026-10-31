package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.model.ValueParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TrackSizingTest {

    /** Narrowest width and natural width of the content of each column. */
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
        // 475 for tracks: 150 fixed, auto reaches its natural width (90), * receives 235.
        assertArrayEquals(new double[]{150, 90, 235}, columns("150 auto *", 515), 1e-9);
    }

    @Test
    void fractionNeverBelowItsNarrowest() {
        // 210 for tracks: 70 per fraction; the third (min 80) freezes.
        assertArrayEquals(new double[]{65, 65, 80}, columns("* * *", 250), 1e-9);
    }

    @Test
    void withoutFractionsAutoTracksStretch() {
        // 495: base 140 + 90 = 230; 265 left over, 132.5 for each one.
        assertArrayEquals(new double[]{272.5, 222.5}, columns("auto auto", 515), 1e-9);
    }

    @Test
    void percentagesWithoutBaseAreAuto() {
        // Without a base, 50% is auto (natural width 140, like the auto next to it, 90) and between(100, 50%)
        // becomes between(100, auto) (natural width 220); 120 is fixed.
        assertArrayEquals(new double[]{140, 90, 220, 120},
                columns("50% auto between(100, 50%) 120", Double.POSITIVE_INFINITY), 1e-9);
    }

    @Test
    void percentagesWithBase() {
        // 50% of 515 = 257.5 (the gaps are not subtracted); of the 495 for tracks, the rest
        // for the auto one (stretched).
        assertArrayEquals(new double[]{257.5, 237.5}, columns("50% auto", 515), 1e-9);
    }

    @Test
    void percentagesIgnoreGaps() {
        // The percentages do not discount the gap: 50% 50% with a gap overflows by the gap.
        assertArrayEquals(new double[]{257.5, 257.5}, columns("50% 50%", 515), 1e-9);
    }

    private static double[] spanning(String tracks, double available, ItemContribution... items) {
        List<Track> t = ValueParser.list(tracks).stream().map(Track::of).toList();
        return TrackSizing.size(t, List.of(items), available, 0);
    }

    @Test
    void itemCrossingFractionsGrowsThem() {
        // The item needs 300 in two fractions: 150 each, even though there are only 200.
        assertArrayEquals(new double[]{150, 150},
                spanning("* *", 200, new ItemContribution(0, 2, 300, 300)), 1e-9);
        assertArrayEquals(new double[]{150, 150},
                spanning("* *", Double.POSITIVE_INFINITY, new ItemContribution(0, 2, 300, 300)), 1e-9);
    }

    @Test
    void itemCrossingFractionsInProportionToFr() {
        assertArrayEquals(new double[]{200, 100},
                spanning("2* *", 200, new ItemContribution(0, 2, 300, 300)), 1e-9);
        // With weights that add up to 0.75: 0.75 of the excess in proportion and the rest in equal parts.
        assertArrayEquals(new double[]{187.5, 112.5},
                spanning("0.5* 0.25*", 200, new ItemContribution(0, 2, 300, 300)), 1e-9);
    }

    @Test
    void itemCrossingFractionsOnlyGrowsTheFractions() {
        // The auto one keeps its item (50); the fraction takes the 250 that are missing.
        assertArrayEquals(new double[]{50, 250}, spanning("auto *", 200,
                new ItemContribution(0, 1, 50, 50), new ItemContribution(0, 2, 300, 300)), 1e-9);
    }

    @Test
    void module6Exercise() {
        List<Track> t = ValueParser.list("auto * *").stream().map(Track::of).toList();
        List<ItemContribution> items = List.of(
                new ItemContribution(0, 1, 60, 100),
                new ItemContribution(1, 1, 40, 300),
                new ItemContribution(2, 1, 40, 300));
        assertArrayEquals(new double[]{100, 187.5, 187.5}, TrackSizing.size(t, items, 515, 20), 1e-9);
    }

    @Test
    void itemSpanningSeveralTracks() {
        // Two empty auto columns and an item of 2 columns with min 100 and max 300:
        // the excess is distributed between both; the rest of the space stretches the auto ones.
        List<Track> t = ValueParser.list("auto auto").stream().map(Track::of).toList();
        double[] r = TrackSizing.size(t, List.of(new ItemContribution(0, 2, 100, 300)), 1000, 0);
        assertArrayEquals(new double[]{500, 500}, r, 1e-9);
        double[] narrow = TrackSizing.size(t, List.of(new ItemContribution(0, 2, 100, 300)), 120, 0);
        assertArrayEquals(new double[]{60, 60}, narrow, 1e-9);
    }

    /**
     * An item over a fixed track and an auto one, with a gap: the excess of its narrowest width goes only to the auto
     * one, and its natural width is counted with the bases already raised (reference/components/grid.md):
     * the area of the item measures its natural width (50 + 10 + 340 = 400), no more. Before, the auto one reached 480
     * (mutation testing).
     */
    @Test
    void itemSpanningAFixedAndAnAutoTrack() {
        List<Track> t = ValueParser.list("50 auto *").stream().map(Track::of).toList();
        assertArrayEquals(new double[]{50, 340, 590},
                TrackSizing.size(t, List.of(new ItemContribution(0, 2, 200, 400)), 1000, 10), 1e-9);
        List<Track> intrinsic = ValueParser.list("50 auto").stream().map(Track::of).toList();
        assertArrayEquals(new double[]{50, 340}, TrackSizing.size(intrinsic,
                List.of(new ItemContribution(0, 2, 200, 400)), Double.POSITIVE_INFINITY, 10), 1e-9);
    }

    /** The gaps that the item spans count as its own, and the excess is split in equal parts among the auto ones. */
    @Test
    void itemSpanningThreeTracksCountsItsGaps() {
        List<Track> t = ValueParser.list("50 auto auto").stream().map(Track::of).toList();
        // 300 − 50 − 2 × 10 = 230, 115 per auto track.
        assertArrayEquals(new double[]{50, 115, 115}, TrackSizing.size(t,
                List.of(new ItemContribution(0, 3, 300, 300)), Double.POSITIVE_INFINITY, 10), 1e-9);
    }

    /**
     * An item of two tracks over one that already has an item of its own: the missing narrowest width is distributed
     * between the two, and the missing natural width is added to what they already have (237.5 + 62.5 = 300).
     */
    @Test
    void itemSpanningTracksThatAlreadyHaveContent() {
        assertArrayEquals(new double[]{237.5, 62.5}, spanning("auto auto", Double.POSITIVE_INFINITY,
                new ItemContribution(0, 1, 50, 200), new ItemContribution(0, 2, 100, 300)), 1e-9);
    }

    /** between(50, 200) grows up to its maximum before the fraction, which keeps the rest. */
    @Test
    void aFixedMaximumIsALimitToGrowTo() {
        assertArrayEquals(new double[]{200, 295}, spanning("between(50, 200) *", 495), 1e-9);
    }

    /** Without a width, the fractions measure in proportion: * and 2* with content of 100 are 100 and 200. */
    @Test
    void fractionsOfAnIndefiniteSizeKeepTheirProportion() {
        assertArrayEquals(new double[]{100, 200}, spanning("* 2*", Double.POSITIVE_INFINITY,
                new ItemContribution(0, 1, 100, 100), new ItemContribution(1, 1, 100, 100)), 1e-9);
    }

    /** An item that spans three fractions with gaps of 10: it lacks 300 − 20, 93.33 per fraction. */
    @Test
    void itemCrossingFractionsCountsItsGaps() {
        List<Track> t = ValueParser.list("* * *").stream().map(Track::of).toList();
        double third = 280.0 / 3;
        assertArrayEquals(new double[]{third, third, third},
                TrackSizing.size(t, List.of(new ItemContribution(0, 3, 300, 300)), 200, 10), 1e-9);
    }
}
