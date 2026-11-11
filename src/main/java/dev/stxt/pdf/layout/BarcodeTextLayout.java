package dev.stxt.pdf.layout;

import java.util.ArrayList;
import java.util.List;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.model.BarcodeText;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.tree.Box;

/**
 * The readable text under the bars of a {@code Barcode} (reference/components/barcode.md, {@code Readable text}): one
 * line at the bottom of its content box, as tall as the font of the box, with each piece of the text
 * ({@link BarcodeText.Label}) centered under its modules. The bars take what is left above it.
 *
 * <p>It does not check that the text fits: {@link ModuleSizes} does, once paginated.</p>
 */
public final class BarcodeTextLayout {

    private BarcodeTextLayout() {
    }

    /**
     * Height the text takes, in points: the ascent plus the descent of the font of {@code box} at its size; 0 if the
     * box has no readable text.
     */
    public static double height(Box box, TextMeasurer measurer) {
        Modules modules = box.modules();
        if (modules == null || modules.text() == null) {
            return 0;
        }
        VerticalMetrics metrics = measurer.vertical(box.style().typography().fontFace());
        return (metrics.ascent() + metrics.descent()) * box.style().typography().fontSize();
    }

    /**
     * The line of the text in a content box of {@code width} × {@code height} points, relative to it: one run per
     * piece, in order. Empty if the box has no readable text.
     */
    public static List<TextLine> lines(Box box, double width, double height, TextMeasurer measurer) {
        double band = height(box, measurer);
        if (band == 0) {
            return List.of();
        }
        Modules modules = box.modules();
        BarcodeText text = modules.text();
        FontFace font = box.style().typography().fontFace();
        double size = box.style().typography().fontSize();
        double module = width / modules.columns();
        List<TextLine.Run> runs = new ArrayList<>();
        for (BarcodeText.Label label : text.labels()) {
            double w = measurer.width(label.text(), font, size);
            double center = (label.column() + label.columns() / 2.0) * module;
            runs.add(new TextLine.Run(center - w / 2, w, label.text(), TextFormat.PLAIN));
        }
        return List.of(new TextLine(0, height - band, width, band, size, text.text(), 0, runs));
    }
}
