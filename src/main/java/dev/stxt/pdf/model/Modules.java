package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The drawing of an image that the engine generates instead of reading it from a file, such as a {@code Barcode}
 * (reference/components/barcode.md): a matrix of modules, each one dark or light, with the quiet zone included as light
 * modules, and the natural size of a module. A one-dimensional barcode is a single row, whose modules are its bars and
 * spaces: it is stretched to its box. A matrix of several rows keeps its modules square ({@link #fit()}).
 *
 * <p>It is immutable: the dark modules are copied when it is created and when they are read.</p>
 *
 * @param columns      modules of each row, at least 1
 * @param rows         rows of modules, at least 1
 * @param dark         the dark modules, row by row: the module ({@code column}, {@code row}) is the bit
 *                     {@code row * columns + column}
 * @param moduleWidth  natural width of a module, in points
 * @param moduleHeight natural height of a module, in points
 */
public record Modules(int columns, int rows, BitSet dark, double moduleWidth, double moduleHeight) {

    public Modules {
        if (columns < 1 || rows < 1 || moduleWidth <= 0 || moduleHeight <= 0) {
            throw new IllegalArgumentException("Modules of " + columns + " × " + rows + " of " + moduleWidth + " × "
                    + moduleHeight + " pt");
        }
        if (dark.length() > columns * rows) {
            throw new IllegalArgumentException("A dark module outside the " + columns + " × " + rows + " matrix");
        }
        dark = (BitSet) dark.clone();
    }

    /** A copy of the dark modules. */
    @Override
    public BitSet dark() {
        return (BitSet) dark.clone();
    }

    /** Whether the module ({@code column}, {@code row}) is dark; outside the matrix, it is not. */
    public boolean isDark(int column, int row) {
        return column >= 0 && column < columns && row >= 0 && row < rows && dark.get(row * columns + column);
    }

    /** Natural width of the drawing, in points: its size if no other is declared. */
    public double naturalWidth() {
        return columns * moduleWidth;
    }

    /** Natural height of the drawing, in points. */
    public double naturalHeight() {
        return rows * moduleHeight;
    }

    /**
     * How the drawing fills its content box: a single row is stretched to it, so its bars take the whole height and
     * all its modules have the same width; a matrix of several rows fits inside it, whole and centered, with square
     * modules.
     */
    public Fit fit() {
        return rows == 1 ? Fit.STRETCH : Fit.INSIDE;
    }

    /**
     * The dark modules of a row as runs of consecutive modules, from left to right: each one is the column where it
     * starts and the number of modules. They are painted as one rectangle each.
     */
    public List<int[]> runs(int row) {
        List<int[]> runs = new ArrayList<>();
        int base = row * columns;
        int start = dark.nextSetBit(base);
        while (start >= 0 && start < base + columns) {
            int end = Math.min(dark.nextClearBit(start), base + columns);
            runs.add(new int[] {start - base, end - start});
            start = dark.nextSetBit(end);
        }
        return runs;
    }
}
