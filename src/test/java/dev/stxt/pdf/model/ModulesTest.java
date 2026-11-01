package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The matrix of modules of a generated image (reference/components/barcode.md). */
class ModulesTest {

    /** A matrix of 5 × 2: row 0 is ##.## and row 1 is .###. */
    private static Modules sample() {
        BitSet dark = new BitSet();
        for (int i : new int[] {0, 1, 3, 4, 6, 7, 8}) {
            dark.set(i);
        }
        return new Modules(5, 2, dark, 2, 3);
    }

    @Test
    void readsTheModulesRowByRow() {
        Modules m = sample();
        assertTrue(m.isDark(0, 0));
        assertFalse(m.isDark(2, 0));
        assertFalse(m.isDark(0, 1));
        assertTrue(m.isDark(3, 1));
        // Outside the matrix, light.
        assertFalse(m.isDark(5, 0));
        assertFalse(m.isDark(-1, 0));
        assertFalse(m.isDark(0, 2));
    }

    @Test
    void runsOfDarkModulesPerRow() {
        Modules m = sample();
        List<int[]> first = m.runs(0);
        assertEquals(2, first.size());
        assertArrayEquals(new int[] {0, 2}, first.get(0));
        assertArrayEquals(new int[] {3, 2}, first.get(1));
        List<int[]> second = m.runs(1);
        assertEquals(1, second.size());
        assertArrayEquals(new int[] {1, 3}, second.get(0));
    }

    @Test
    void aRunEndsAtTheEndOfItsRow() {
        BitSet dark = new BitSet();
        dark.set(2, 6);
        Modules m = new Modules(4, 2, dark, 1, 1);
        assertArrayEquals(new int[] {2, 2}, m.runs(0).get(0));
        assertArrayEquals(new int[] {0, 2}, m.runs(1).get(0));
    }

    @Test
    void naturalSizeAndFit() {
        Modules m = sample();
        assertEquals(10, m.naturalWidth());
        assertEquals(6, m.naturalHeight());
        assertEquals(Fit.INSIDE, m.fit());
        assertEquals(Fit.STRETCH, new Modules(5, 1, new BitSet(), 1, 20).fit());
    }

    @Test
    void isImmutable() {
        BitSet dark = new BitSet();
        Modules m = new Modules(2, 1, dark, 1, 1);
        dark.set(0);
        assertFalse(m.isDark(0, 0));
        m.dark().set(1);
        assertFalse(m.isDark(1, 0));
    }

    @Test
    void rejectsAnEmptyMatrixOrAModuleOutsideIt() {
        assertThrows(IllegalArgumentException.class, () -> new Modules(0, 1, new BitSet(), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Modules(1, 1, new BitSet(), 0, 1));
        BitSet outside = new BitSet();
        outside.set(4);
        assertThrows(IllegalArgumentException.class, () -> new Modules(2, 2, outside, 1, 1));
    }
}
