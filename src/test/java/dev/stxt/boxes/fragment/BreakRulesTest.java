package dev.stxt.boxes.fragment;

import dev.stxt.boxes.layout.LayoutResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BreakRulesTest {

    @Test
    void module8Exercise() {
        // Área útil 762, interlineado 15, párrafo de 53 líneas que empieza en y = 720.
        int capacity1 = BreakRules.capacity(762 - 720, 15);
        assertEquals(2, capacity1);
        int p1 = BreakRules.linesThatFit(53, capacity1, 2, 2, false);
        int p2 = BreakRules.linesThatFit(53 - p1, BreakRules.capacity(762, 15), 2, 2, true);
        int p3 = BreakRules.linesThatFit(53 - p1 - p2, BreakRules.capacity(762, 15), 2, 2, true);
        assertEquals(2, p1);
        assertEquals(49, p2);
        assertEquals(2, p3);
    }

    @Test
    void orphanMovesTheWholeParagraph() {
        assertEquals(0, BreakRules.linesThatFit(10, 1, 2, 2, false));
    }

    @Test
    void progressGuaranteeAtPageStart() {
        assertEquals(1, BreakRules.linesThatFit(10, 1, 2, 2, true));
    }

    @Test
    void continuousHasNoLimit() {
        assertEquals(Integer.MAX_VALUE, BreakRules.capacity(Double.POSITIVE_INFINITY, 15));
    }

    @Test
    void negativeSpaceHasNoCapacity() {
        assertEquals(0, BreakRules.capacity(-5, 15));
    }

    @Test
    void relaxedLinesKeepWidowsIfPossible() {
        assertEquals(1, BreakRules.relaxedLines(9, 1, 2));
        // 3 pendientes y 2 viudas: solo puede quedar 1 aquí.
        assertEquals(1, BreakRules.relaxedLines(3, 2, 2));
        // Con 2 pendientes cualquier corte deja una viuda: se llena el espacio.
        assertEquals(1, BreakRules.relaxedLines(2, 1, 2));
    }

    @Test
    void cutLinesInsideTheSecondParagraph() {
        assertEquals(new BreakRules.Cut(7, LayoutResult.PERFECT_BREAK),
                BreakRules.cutLines(new int[]{5, 10}, 0, 7, 2, 2, false));
    }

    @Test
    void cutLinesBetweenParagraphs() {
        // En el segundo párrafo solo cabría una línea huérfana.
        assertEquals(new BreakRules.Cut(5, LayoutResult.PERFECT_BREAK),
                BreakRules.cutLines(new int[]{5, 10}, 0, 6, 2, 2, false));
    }

    @Test
    void cutLinesRelaxesWhenNothingElseIsPlaced() {
        assertEquals(new BreakRules.Cut(1, LayoutResult.VIOLATES_WIDOWS),
                BreakRules.cutLines(new int[]{9}, 0, 1, 2, 2, false));
    }

    @Test
    void cutLinesWithoutRoom() {
        assertEquals(new BreakRules.Cut(0, LayoutResult.FORCED), BreakRules.cutLines(new int[]{3}, 0, 0, 2, 2, false));
        assertEquals(new BreakRules.Cut(1, LayoutResult.FORCED), BreakRules.cutLines(new int[]{3}, 0, 0, 2, 2, true));
    }

    @Test
    void cutLinesResumesInTheMiddle() {
        assertEquals(new BreakRules.Cut(9, LayoutResult.PERFECT_BREAK),
                BreakRules.cutLines(new int[]{9}, 7, 8, 2, 2, true));
    }

    @Test
    void bestIsTheLastOfTheHighestAppeal() {
        List<int[]> candidates = List.of(new int[]{1, 3}, new int[]{2, 1}, new int[]{3, 3}, new int[]{4, 2});
        assertEquals(3, BreakRules.best(candidates, c -> c[1])[0]);
        assertNull(BreakRules.best(List.<int[]>of(), c -> c[1]));
    }

    private static final double[] STARTS = {0, 50, 100};
    private static final double[] ENDS = {40, 90, 140};
    private static final boolean[] NOT_FORCED = {false, false, false};

    @Test
    void cutUnitsAtTheLastThatFits() {
        assertEquals(new BreakRules.Cut(2, LayoutResult.PERFECT_BREAK),
                BreakRules.cutUnits(STARTS, ENDS, 0, 100, new int[]{0, 3, 3}, NOT_FORCED, false));
    }

    @Test
    void cutUnitsPrefersAnEarlierBetterBreak() {
        assertEquals(new BreakRules.Cut(1, LayoutResult.PERFECT_BREAK),
                BreakRules.cutUnits(STARTS, ENDS, 0, 100, new int[]{0, 3, 1}, NOT_FORCED, false));
    }

    @Test
    void cutUnitsForced() {
        assertEquals(new BreakRules.Cut(1, LayoutResult.PERFECT_BREAK),
                BreakRules.cutUnits(STARTS, ENDS, 0, 1000, new int[]{0, 3, 3}, new boolean[]{false, true, false}, false));
    }

    @Test
    void cutUnitsSkipsUnbreakablePoints() {
        // Entre 1 y 2 no se puede cortar: 1 y 2 van juntas a la página siguiente.
        assertEquals(new BreakRules.Cut(1, LayoutResult.PERFECT_BREAK),
                BreakRules.cutUnits(STARTS, ENDS, 0, 100, new int[]{0, 3, -1}, NOT_FORCED, false));
    }

    @Test
    void cutUnitsOverflowOnlyAtThePageStart() {
        double[] starts = {0, 200};
        double[] ends = {150, 250};
        int[] appeal = {0, 3};
        boolean[] forced = {false, false};
        assertEquals(new BreakRules.Cut(0, LayoutResult.FORCED),
                BreakRules.cutUnits(starts, ends, 0, 100, appeal, forced, false));
        assertEquals(new BreakRules.Cut(1, LayoutResult.FORCED),
                BreakRules.cutUnits(starts, ends, 0, 100, appeal, forced, true));
    }

    @Test
    void cutUnitsResumesFromTheFirstUnitOfTheFragment() {
        // Desde la unidad 1, el espacio se mide desde su principio (50): caben 1 y 2.
        assertEquals(new BreakRules.Cut(3, LayoutResult.PERFECT_BREAK),
                BreakRules.cutUnits(STARTS, ENDS, 1, 100, new int[]{0, 3, 3}, NOT_FORCED, true));
    }
}
