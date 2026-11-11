package dev.stxt.pdf.model;

import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The readable text under the bars of a one-dimensional {@code Barcode} (reference/components/barcode.md,
 * {@code Readable text}): its pieces, each one centered under some modules of the row, and the bars that go down
 * into the text, such as the guard bars of an EAN-13. It counts modules, like the drawing it belongs to
 * ({@link Modules}): the layout gives it its size.
 *
 * <p>It is immutable: the labels and the long bars are copied when it is created and when they are read.</p>
 *
 * @param labels   the pieces of the text, from left to right, at least one
 * @param longBars the columns of the row whose bars go down to the middle of the text
 */
public record BarcodeText(List<Label> labels, BitSet longBars) {

    /**
     * A piece of the text, centered under the modules from {@code column} to {@code column + columns}.
     *
     * @param text    what is painted, not empty
     * @param column  the first module it goes under, counting the quiet zone; negative if it starts before the
     *                drawing, which the layout rejects
     * @param columns how many modules it goes under, at least 1
     */
    public record Label(String text, int column, int columns) {

        public Label {
            if (text.isEmpty() || columns < 1) {
                throw new IllegalArgumentException("A label '" + text + "' under " + columns + " modules");
            }
        }
    }

    public BarcodeText {
        labels = List.copyOf(labels);
        if (labels.isEmpty()) {
            throw new IllegalArgumentException("A readable text without labels");
        }
        longBars = (BitSet) longBars.clone();
    }

    /** A text in one piece, centered under the {@code columns} modules of the drawing, with no long bars. */
    public static BarcodeText centered(String text, int columns) {
        return new BarcodeText(List.of(new Label(text, 0, columns)), new BitSet());
    }

    /** A copy of the columns with a long bar. */
    @Override
    public BitSet longBars() {
        return (BitSet) longBars.clone();
    }

    /** Whether the bar of {@code column} goes down into the text. */
    public boolean isLong(int column) {
        return column >= 0 && longBars.get(column);
    }

    /** The whole text, its pieces one after another. */
    public String text() {
        return labels.stream().map(Label::text).collect(Collectors.joining());
    }
}
