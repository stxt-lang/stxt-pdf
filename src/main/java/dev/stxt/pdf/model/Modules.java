package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The drawing of an image that the engine generates instead of reading it from a file, such as a {@code Barcode}
 * (reference/components/barcode.md): a matrix of modules, each one dark or light, with the quiet zone included as light
 * modules, and the natural size of a module. A one-dimensional barcode is a single row, whose modules are its bars and
 * spaces: it is stretched to its box, and can have a readable text under it ({@link BarcodeText}). A matrix of several
 * rows keeps the proportion of its modules ({@link #fit()}).
 *
 * <p>It is immutable: the dark modules are copied when it is created and when they are read.</p>
 *
 * @param columns      modules of each row, at least 1
 * @param rows         rows of modules, at least 1
 * @param dark         the dark modules, row by row: the module ({@code column}, {@code row}) is the bit
 *                     {@code row * columns + column}
 * @param moduleWidth  natural width of a module, in points
 * @param moduleHeight natural height of a module, in points
 * @param minModule    the narrowest module that can still be read, in points, at most {@code moduleWidth}; 0 if
 *                     there is no minimum. A drawing whose box leaves narrower modules is an error unless the caller
 *                     removes the check ({@link Check#BARCODE_SIZE})
 * @param text         the readable text under the bars of a single row ({@code Readable text}); {@code null} if
 *                     there is none. The layout adds its height to the natural one of the drawing and places it at
 *                     the bottom of the content box, where the bars leave it room ({@link #rects(Rect, double)})
 */
public record Modules(int columns, int rows, BitSet dark, double moduleWidth, double moduleHeight, double minModule,
                      BarcodeText text) {

    public Modules {
        if (columns < 1 || rows < 1 || moduleWidth <= 0 || moduleHeight <= 0) {
            throw new IllegalArgumentException("Modules of " + columns + " × " + rows + " of " + moduleWidth + " × "
                    + moduleHeight + " pt");
        }
        if (minModule < 0 || minModule > moduleWidth) {
            throw new IllegalArgumentException("A minimum module of " + minModule + " pt for modules of " + moduleWidth
                    + " pt");
        }
        if (dark.length() > columns * rows) {
            throw new IllegalArgumentException("A dark module outside the " + columns + " × " + rows + " matrix");
        }
        if (text != null && rows != 1) {
            throw new IllegalArgumentException("A readable text under a matrix of " + rows + " rows");
        }
        dark = (BitSet) dark.clone();
    }

    /** Without a readable text. */
    public Modules(int columns, int rows, BitSet dark, double moduleWidth, double moduleHeight, double minModule) {
        this(columns, rows, dark, moduleWidth, moduleHeight, minModule, null);
    }

    /** Without a minimum module or a readable text. */
    public Modules(int columns, int rows, BitSet dark, double moduleWidth, double moduleHeight) {
        this(columns, rows, dark, moduleWidth, moduleHeight, 0);
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
     * The width of a module once the drawing fills a content box of {@code width} × {@code height} points
     * ({@link #fit()}): stretched, the width divided among the columns; inside, the natural width scaled as much as
     * the box allows on both sides.
     */
    public double usedModule(double width, double height) {
        if (fit() == Fit.STRETCH) {
            return width / columns;
        }
        return moduleWidth * Math.min(width / naturalWidth(), height / naturalHeight());
    }

    /**
     * The smallest content box, width and height in points, in which the modules measure {@link #minModule()}: the
     * width alone if the drawing is stretched, whose height does not change its modules.
     */
    public double[] minimumSize() {
        double scale = minModule / moduleWidth;
        return new double[] {naturalWidth() * scale, naturalHeight() * scale};
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

    /**
     * The dark modules once the whole drawing takes {@code target}: one rectangle per run of each row ({@link #runs}),
     * row by row from the top and from left to right, in the coordinates of {@code target}.
     */
    public List<Rect> rects(Rect target) {
        double w = target.width() / columns;
        double h = target.height() / rows;
        List<Rect> rects = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int[] run : runs(row)) {
                rects.add(new Rect(target.x() + run[0] * w, target.y() + row * h, run[1] * w, h));
            }
        }
        return rects;
    }

    /**
     * The dark modules of a single row with a readable text under it, once the bars take {@code target}: as
     * {@link #rects(Rect)}, and the long bars of the text ({@link BarcodeText#isLong}) go {@code extension} points
     * further down. Without a text, {@link #rects(Rect)}.
     */
    public List<Rect> rects(Rect target, double extension) {
        if (text == null || extension <= 0) {
            return rects(target);
        }
        double w = target.width() / columns;
        List<Rect> rects = new ArrayList<>();
        for (int[] run : runs(0)) {
            // A run is split where its bars change height, so that each rectangle has one.
            int start = run[0];
            for (int c = run[0] + 1; c <= run[0] + run[1]; c++) {
                if (c == run[0] + run[1] || text.isLong(c) != text.isLong(start)) {
                    rects.add(new Rect(target.x() + start * w, target.y(), (c - start) * w,
                            target.height() + (text.isLong(start) ? extension : 0)));
                    start = c;
                }
            }
        }
        return rects;
    }
}
